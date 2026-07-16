package io.github.aresprojects.processor.validation;

import io.github.aresprojects.annotation.LambdaHandler;
import io.github.aresprojects.processor.diagnostic.DiagnosticCode;
import io.github.aresprojects.processor.model.HandlerMethodModel;
import io.github.aresprojects.processor.model.HandlerModel;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/** Validates annotated handler classes and builds immutable processor models. */
public final class HandlerValidator {

    private static final Pattern HANDLER_NAME = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");
    private static final String CONTEXT_NAME = "io.github.aresprojects.runtime.InvocationContext";
    private static final String COMPLETABLE_FUTURE_NAME = "java.util.concurrent.CompletableFuture";

    private final ProcessingEnvironment environment;
    private boolean hasErrors;

    /** Creates a validator using the compiler services for one processing run. */
    public HandlerValidator(ProcessingEnvironment environment) {
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
    }

    /** Validates one annotated class, emitting a stable diagnostic when invalid. */
    public Optional<HandlerModel> validate(TypeElement handler, LambdaHandler annotation) {
        validateHandlerDefinition(handler, annotation);
        if (hasErrors) {
            return Optional.empty();
        }
        if (!hasPublicNoArgConstructor(handler)) {
            error(DiagnosticCode.ARES004, handler);
            return Optional.empty();
        }

        List<ExecutableElement> methods = ElementFilter.methodsIn(handler.getEnclosedElements()).stream()
                .filter(method -> method.getSimpleName().contentEquals("handle"))
                .toList();
        if (methods.isEmpty()) {
            error(DiagnosticCode.ARES005, handler);
            return Optional.empty();
        }

        List<HandlerMethodModel> supported = methods.stream()
                .map(this::validateMethod)
                .flatMap(Optional::stream)
                .toList();
        if (supported.size() != 1 || supported.size() != methods.size()) {
            error(DiagnosticCode.ARES005, handler);
            return Optional.empty();
        }

        HandlerMethodModel method = supported.getFirst();
        String packageName = packageName(handler);
        String sourceName = handler.getQualifiedName().toString();
        String adapterSimpleName = handler.getSimpleName() + "AresAdapter";
        String adapterPackage = packageName.isBlank() ? "ares.generated" : packageName + ".ares.generated";
        String adapterName = adapterPackage + "." + adapterSimpleName;
        return Optional.of(new HandlerModel(
                annotation.value(),
                sourceName,
                adapterName,
                adapterName + "::handleRequest",
                method.inputTypeName(),
                method.outputTypeName(),
                method.acceptsContext(),
                method.returnsVoid()));
    }

    private void validateHandlerDefinition(TypeElement handler, LambdaHandler annotation) {
        if (handler.getKind() != ElementKind.CLASS) {
            error(DiagnosticCode.ARES001, handler);
        }
        if (!handler.getModifiers().contains(Modifier.PUBLIC)) {
            error(DiagnosticCode.ARES002, handler);
        }
        if (handler.getModifiers().contains(Modifier.ABSTRACT)) {
            error(DiagnosticCode.ARES003, handler);
        }
        if (handler.getNestingKind() != NestingKind.TOP_LEVEL
                && !handler.getModifiers().contains(Modifier.STATIC)) {
            error(DiagnosticCode.ARES014, handler);
        }
        if (!HANDLER_NAME.matcher(annotation.value()).matches()) {
            error(DiagnosticCode.ARES009, handler);
        }
    }

    private Optional<HandlerMethodModel> validateMethod(ExecutableElement method) {
        Optional<DiagnosticCode> declarationError = methodDeclarationError(method);
        if (declarationError.isPresent()) {
            error(declarationError.orElseThrow(), method);
            return Optional.empty();
        }
        List<? extends VariableElement> parameters = method.getParameters();
        Optional<Boolean> contextResult = validateParameters(parameters, method);
        if (contextResult.isEmpty()) {
            return Optional.empty();
        }
        boolean acceptsContext = contextResult.orElseThrow();
        TypeMirror inputType = parameters.getFirst().asType();
        TypeMirror outputType = method.getReturnType();
        if (!isAccessible(inputType) || inputType.getKind() == TypeKind.VOID) {
            error(DiagnosticCode.ARES010, parameters.getFirst());
            return Optional.empty();
        }
        if (outputType.getKind() != TypeKind.VOID && !isAccessible(outputType)) {
            error(DiagnosticCode.ARES011, method);
            return Optional.empty();
        }
        if (isCompletableFuture(outputType)) {
            error(DiagnosticCode.ARES013, method);
            return Optional.empty();
        }
        boolean returnsVoid = outputType.getKind() == TypeKind.VOID;
        return Optional.of(new HandlerMethodModel(
                method.getSimpleName().toString(),
                inputType.toString(),
                returnsVoid ? "java.lang.Void" : outputType.toString(),
                acceptsContext,
                returnsVoid));
    }

    private Optional<DiagnosticCode> methodDeclarationError(ExecutableElement method) {
        if (!method.getModifiers().contains(Modifier.PUBLIC)
                || method.getModifiers().contains(Modifier.STATIC)) {
            return Optional.of(DiagnosticCode.ARES006);
        }
        if (!method.getTypeParameters().isEmpty()) {
            return Optional.of(DiagnosticCode.ARES012);
        }
        return Optional.empty();
    }

    private Optional<Boolean> validateParameters(List<? extends VariableElement> parameters, ExecutableElement method) {
        if (parameters.size() < 1 || parameters.size() > 2) {
            error(DiagnosticCode.ARES007, method);
            return Optional.empty();
        }
        boolean acceptsContext = parameters.size() == 2;
        if (acceptsContext && !isInvocationContext(parameters.get(1).asType())) {
            error(DiagnosticCode.ARES015, method);
            return Optional.empty();
        }
        return Optional.of(acceptsContext);
    }

    private boolean hasPublicNoArgConstructor(TypeElement handler) {
        return ElementFilter.constructorsIn(handler.getEnclosedElements()).stream()
                .anyMatch(constructor -> constructor.getParameters().isEmpty()
                        && constructor.getModifiers().contains(Modifier.PUBLIC));
    }

    private boolean isInvocationContext(TypeMirror type) {
        return environment.getTypeUtils().erasure(type).toString().equals(CONTEXT_NAME);
    }

    private boolean isCompletableFuture(TypeMirror type) {
        return type.getKind() == TypeKind.DECLARED
                && environment.getTypeUtils().erasure(type).toString().equals(COMPLETABLE_FUTURE_NAME);
    }

    private boolean isAccessible(TypeMirror type) {
        if (type.getKind().isPrimitive() || type.getKind() == TypeKind.VOID) {
            return true;
        }
        if (type.getKind() == TypeKind.ARRAY) {
            return isAccessible(((ArrayType) type).getComponentType());
        }
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        Element element = ((DeclaredType) type).asElement();
        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            return false;
        }
        Element enclosing = element.getEnclosingElement();
        return !(enclosing instanceof TypeElement) || isAccessible(((TypeElement) enclosing).asType());
    }

    private String packageName(TypeElement handler) {
        PackageElement packageElement = environment.getElementUtils().getPackageOf(handler);
        return packageElement.getQualifiedName().toString().toLowerCase(Locale.ROOT);
    }

    private void error(DiagnosticCode code, Element element) {
        hasErrors = true;
        environment.getMessager().printMessage(Diagnostic.Kind.ERROR, code.message(), element);
    }

    /** Returns whether this validator emitted any errors. */
    public boolean hasErrors() {
        return hasErrors;
    }
}
