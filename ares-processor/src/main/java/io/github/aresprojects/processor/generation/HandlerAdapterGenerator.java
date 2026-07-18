package io.github.aresprojects.processor.generation;

import io.github.aresprojects.processor.model.HandlerModel;
import java.io.IOException;
import java.io.Writer;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;

/** Generates reflection-free AWS Lambda adapters for validated handlers. */
public final class HandlerAdapterGenerator {

    private final Filer filer;

    /** Creates a generator using the compiler filer. */
    public HandlerAdapterGenerator(ProcessingEnvironment environment) {
        this.filer = environment.getFiler();
    }

    /** Writes one adapter source file. */
    public void generate(HandlerModel model) throws IOException {
        int separator = model.adapterClassName().lastIndexOf('.');
        String packageName = model.adapterClassName().substring(0, separator);
        String adapterSimpleName = model.adapterClassName().substring(separator + 1);
        String sourceClassName = model.sourceClassName();
        StringBuilder source = new StringBuilder();
        source.append("package ").append(packageName).append(";\n\n");
        source.append("import com.amazonaws.services.lambda.runtime.Context;\n");
        source.append("import com.amazonaws.services.lambda.runtime.RequestHandler;\n");
        source.append("import io.github.aresprojects.runtime.aws.AwsInvocationContext;\n");
        source.append("import io.github.aresprojects.runtime.exception.HandlerInitializationException;\n\n");
        source.append("public final class ")
                .append(adapterSimpleName)
                .append(" implements RequestHandler<")
                .append(model.inputTypeName())
                .append(", ")
                .append(model.outputTypeName())
                .append("> {\n");
        source.append("    private final ").append(sourceClassName).append(" handler;\n\n");
        source.append("    public ").append(adapterSimpleName).append("() {\n");
        source.append("        try {\n");
        source.append("            this.handler = new ").append(sourceClassName).append("();\n");
        source.append("        } catch (RuntimeException exception) {\n");
        source.append("            throw new HandlerInitializationException(\n");
        source.append("                    \"Failed to initialize Lambda handler '")
                .append(escapeJava(model.name()))
                .append("'\",\n");
        source.append("                    exception);\n");
        source.append("        }\n");
        source.append("    }\n\n");
        source.append("    @Override\n");
        source.append("    public ").append(model.outputTypeName()).append(" handleRequest(\n");
        source.append("            ").append(model.inputTypeName()).append(" input, Context context) {\n");
        if (model.returnsVoid()) {
            source.append("        handler.handle(").append(invocation(model)).append(");\n");
            source.append("        return null;\n");
        } else {
            source.append("        return handler.handle(")
                    .append(invocation(model))
                    .append(");\n");
        }
        source.append("    }\n");
        source.append("}\n");

        JavaFileObject file = filer.createSourceFile(model.adapterClassName());
        try (Writer writer = file.openWriter()) {
            writer.write(source.toString());
        }
    }

    private String invocation(HandlerModel model) {
        if (model.acceptsContext()) {
            return "input, new AwsInvocationContext(context)";
        }
        return "input";
    }

    private String escapeJava(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
