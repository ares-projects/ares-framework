package io.github.aresprojects.processor;

import io.github.aresprojects.annotation.LambdaHandler;
import io.github.aresprojects.processor.diagnostic.DiagnosticCode;
import io.github.aresprojects.processor.generation.HandlerAdapterGenerator;
import io.github.aresprojects.processor.generation.HandlerManifestGenerator;
import io.github.aresprojects.processor.model.HandlerModel;
import io.github.aresprojects.processor.validation.HandlerValidator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

/** Discovers, validates, and generates artifacts for Ares Lambda handlers. */
@SupportedAnnotationTypes("io.github.aresprojects.annotation.LambdaHandler")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public final class LambdaHandlerProcessor extends AbstractProcessor {

    private final Map<String, HandlerModel> models = new LinkedHashMap<>();
    private boolean hasErrors;
    private boolean generated;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        if (roundEnvironment.processingOver()) {
            generateArtifacts();
            return false;
        }
        HandlerValidator validator = new HandlerValidator(processingEnv);
        for (Element element : roundEnvironment.getElementsAnnotatedWith(LambdaHandler.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                error(DiagnosticCode.ARES001, element);
                continue;
            }
            LambdaHandler annotation = typeElement.getAnnotation(LambdaHandler.class);
            Optional<HandlerModel> model = validator.validate(typeElement, annotation);
            hasErrors |= validator.hasErrors();
            model.ifPresent(this::addModel);
        }
        return true;
    }

    private void addModel(HandlerModel model) {
        HandlerModel previous = models.putIfAbsent(model.name(), model);
        if (previous != null) {
            hasErrors = true;
            error(DiagnosticCode.ARES008, model.name());
            models.remove(model.name());
        }
    }

    private void generateArtifacts() {
        if (generated || hasErrors) {
            return;
        }
        generated = true;
        try {
            HandlerAdapterGenerator adapterGenerator = new HandlerAdapterGenerator(processingEnv);
            for (HandlerModel model : new ArrayList<>(models.values())
                    .stream().sorted(Comparator.comparing(HandlerModel::name)).toList()) {
                adapterGenerator.generate(model);
            }
            new HandlerManifestGenerator(processingEnv).generate(new ArrayList<>(models.values()));
        } catch (IOException exception) {
            hasErrors = true;
            processingEnv
                    .getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Ares generation failed: " + exception.getMessage());
        }
    }

    private void error(DiagnosticCode code, Element element) {
        hasErrors = true;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, code.message(), element);
    }

    private void error(DiagnosticCode code, String name) {
        hasErrors = true;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, code.message() + " '" + name + "'");
    }
}
