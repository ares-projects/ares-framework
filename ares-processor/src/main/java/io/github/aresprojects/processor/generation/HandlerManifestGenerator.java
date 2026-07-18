package io.github.aresprojects.processor.generation;

import io.github.aresprojects.processor.model.HandlerModel;
import java.io.IOException;
import java.io.Writer;
import java.util.Comparator;
import java.util.List;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;

/**
 * Generates deterministic version-one Ares handler metadata without adding a JSON runtime dependency.
 *
 * <p>The manifest has a small, fixed schema, so writing it directly keeps the annotation processor
 * dependency-free and makes its output stable across serializer versions.
 */
public final class HandlerManifestGenerator {

    private final Filer filer;

    /** Creates a manifest generator using the compiler filer. */
    public HandlerManifestGenerator(ProcessingEnvironment environment) {
        this.filer = environment.getFiler();
    }

    /** Writes the sorted handler manifest after processing is complete. */
    public void generate(List<HandlerModel> models) throws IOException {
        var resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/ares/handlers.json");
        try (Writer writer = resource.openWriter()) {
            List<HandlerModel> sorted = models.stream()
                    .sorted(Comparator.comparing(HandlerModel::name))
                    .toList();
            writer.write("{\n  \"schemaVersion\": 1,\n  \"handlers\": [");
            for (int index = 0; index < sorted.size(); index++) {
                if (index > 0) {
                    writer.write(",");
                }
                writer.write("\n");
                writeEntry(writer, sorted.get(index));
            }
            if (!sorted.isEmpty()) {
                writer.write("\n  ");
            }
            writer.write("]\n}\n");
        }
    }

    private void writeEntry(Writer writer, HandlerModel model) throws IOException {
        writer.write("    {\n");
        writeField(writer, "name", model.name(), true, true);
        writeField(writer, "sourceClass", model.sourceClassName(), true, true);
        writeField(writer, "adapterClass", model.adapterClassName(), true, true);
        writeField(writer, "awsHandler", model.awsHandler(), true, true);
        writeField(writer, "inputType", model.inputTypeName(), true, true);
        writeField(writer, "outputType", model.outputTypeName(), true, true);
        writeField(writer, "acceptsContext", Boolean.toString(model.acceptsContext()), false, true);
        writeField(writer, "returnsVoid", Boolean.toString(model.returnsVoid()), false, false);
        writer.write("\n    }");
    }

    private void writeField(Writer writer, String name, String value, boolean quoted, boolean comma)
            throws IOException {
        writer.write("      \"");
        writer.write(name);
        writer.write("\": ");
        if (quoted) {
            writer.write("\"");
            writer.write(escapeJson(value));
            writer.write("\"");
        } else {
            writer.write(value);
        }
        writer.write(comma ? ",\n" : "\n");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
