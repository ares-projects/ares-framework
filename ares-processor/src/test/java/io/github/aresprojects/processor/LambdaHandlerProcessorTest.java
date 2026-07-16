package io.github.aresprojects.processor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

class LambdaHandlerProcessorTest {

    @Test
    void generatesContextAwareResponseAdapterAndSortedManifest() throws Exception {
        Compilation compilation = compile(source("sample.ZHandler", """
                                package sample;
                                import io.github.aresprojects.annotation.LambdaHandler;
                                import io.github.aresprojects.runtime.InvocationContext;
                                @LambdaHandler("z-handler")
                                public final class ZHandler {
                                    public ZHandler() {}
                                    public String handle(String input, InvocationContext context) {
                                        return input + context.requestId();
                                    }
                                }
                                """), source("sample.AHandler", """
                                package sample;
                                import io.github.aresprojects.annotation.LambdaHandler;
                                @LambdaHandler("a-handler")
                                public final class AHandler {
                                    public AHandler() {}
                                    public String handle(String input) { return input; }
                                }
                                """));

        assertTrue(compilation.success(), diagnostics(compilation));
        String generated =
                Files.readString(compilation.generated().resolve("sample/ares/generated/ZHandlerAresAdapter.java"));
        assertTrue(generated.contains("new AwsInvocationContext(context)"));
        assertTrue(generated.contains("private final sample.ZHandler handler;"));
        String manifest = Files.readString(compilation.output().resolve("META-INF/ares/handlers.json"));
        assertTrue(manifest.indexOf("a-handler") < manifest.indexOf("z-handler"));
        assertTrue(manifest.contains("\"schemaVersion\": 1"));
    }

    @Test
    void generatedAdapterExecutesAndReusesHandler() throws Exception {
        Compilation compilation = compile(source("sample.CountingHandler", """
                                package sample;
                                import io.github.aresprojects.annotation.LambdaHandler;
                                @LambdaHandler("counting")
                                public final class CountingHandler {
                                    private static int instances;
                                    public CountingHandler() { instances++; }
                                    public String handle(String input) { return input + instances; }
                                }
                                """));

        assertTrue(compilation.success(), diagnostics(compilation));
        try (URLClassLoader loader =
                new URLClassLoader(new URL[] {compilation.output().toUri().toURL()})) {
            Class<?> adapterType = Class.forName("sample.ares.generated.CountingHandlerAresAdapter", true, loader);
            @SuppressWarnings("unchecked")
            RequestHandler<String, String> adapter = (RequestHandler<String, String>)
                    adapterType.getConstructor().newInstance();
            assertTrue("a1".equals(adapter.handleRequest("a", new StubContext())));
            assertTrue("b1".equals(adapter.handleRequest("b", new StubContext())));
        }
    }

    @Test
    void generatesVoidAdapterThatReturnsNull() throws Exception {
        Compilation compilation = compile(source("sample.AuditHandler", """
                                package sample;
                                import io.github.aresprojects.annotation.LambdaHandler;
                                @LambdaHandler("audit-event")
                                public final class AuditHandler {
                                    public AuditHandler() {}
                                    public void handle(String input) {}
                                }
                                """));

        assertTrue(compilation.success(), diagnostics(compilation));
        assertTrue(
                Files.readString(compilation.generated().resolve("sample/ares/generated/AuditHandlerAresAdapter.java"))
                        .contains("implements RequestHandler<java.lang.String, java.lang.Void>"));
    }

    @Test
    void rejectsInvalidDefinitionsWithStableCodes() throws Exception {
        List<InvalidCase> cases = List.of(
                new InvalidCase("InterfaceHandler", "interface", "ARES001"),
                new InvalidCase("PackageHandler", "package", "ARES002"),
                new InvalidCase("AbstractHandler", "abstract", "ARES003"),
                new InvalidCase("NoConstructorHandler", "noConstructor", "ARES004"),
                new InvalidCase("NoMethodHandler", "noMethod", "ARES005"),
                new InvalidCase("StaticHandler", "staticMethod", "ARES006"),
                new InvalidCase("BadParametersHandler", "badParameters", "ARES007"),
                new InvalidCase("InvalidNameHandler", "invalidName", "ARES009"),
                new InvalidCase("PrivateInputHandler", "privateInput", "ARES010"),
                new InvalidCase("PrivateOutputHandler", "privateOutput", "ARES011"),
                new InvalidCase("GenericHandler", "generic", "ARES012"),
                new InvalidCase("AsyncHandler", "async", "ARES013"),
                new InvalidCase("InnerHandler", "inner", "ARES014"),
                new InvalidCase("BadContextHandler", "badContext", "ARES015"));

        for (InvalidCase invalidCase : cases) {
            Compilation compilation = compile(source("sample." + invalidCase.typeName(), invalidCase.source()));
            assertFalse(compilation.success(), invalidCase.typeName() + " unexpectedly compiled");
            assertTrue(
                    diagnostics(compilation).contains(invalidCase.code()),
                    invalidCase.typeName() + " diagnostics: " + diagnostics(compilation));
            assertFalse(Files.exists(compilation.output().resolve("META-INF/ares/handlers.json")));
        }
    }

    @Test
    void rejectsDuplicateNames() throws Exception {
        Compilation compilation = compile(
                source("sample.FirstHandler", handlerSource("FirstHandler", "duplicate")),
                source("sample.SecondHandler", handlerSource("SecondHandler", "duplicate")));

        assertFalse(compilation.success());
        assertTrue(diagnostics(compilation).contains("ARES008"));
    }

    private Compilation compile(Source... sources) throws IOException {
        Path root = Files.createTempDirectory("ares-processor-test");
        Path output = root.resolve("classes");
        Path generated = root.resolve("generated");
        Files.createDirectories(output);
        Files.createDirectories(generated);
        List<Path> sourceFiles = new ArrayList<>();
        for (Source source : sources) {
            Path file = root.resolve(source.className().replace('.', '/') + ".java");
            Files.createDirectories(Objects.requireNonNull(file.getParent()));
            Files.writeString(file, source.contents());
            sourceFiles.add(file);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromFiles(
                    sourceFiles.stream().map(Path::toFile).toList());
            String classpath = System.getProperty("java.class.path");
            List<String> options = List.of(
                    "-classpath",
                    classpath,
                    "-processorpath",
                    classpath,
                    "-processor",
                    LambdaHandlerProcessor.class.getName(),
                    "-d",
                    output.toString(),
                    "-s",
                    generated.toString());
            boolean success = compiler.getTask(null, fileManager, diagnostics, options, null, units)
                    .call();
            return new Compilation(root, output, generated, success, diagnostics.getDiagnostics());
        }
    }

    private String diagnostics(Compilation compilation) {
        return compilation.diagnostics().stream()
                .map(diagnostic -> diagnostic.getMessage(null))
                .reduce("", (a, b) -> a + b);
    }

    private static Source source(String className, String contents) {
        return new Source(className, contents);
    }

    private static String handlerSource(String className, String name) {
        return String.join(
                        System.lineSeparator(),
                        "package sample;",
                        "import io.github.aresprojects.annotation.LambdaHandler;",
                        "@LambdaHandler(\"%s\")",
                        "public final class %s {",
                        "    public %s() {}",
                        "    public String handle(String input) { return input; }",
                        "}")
                .formatted(name, className, className);
    }

    private record Source(String className, String contents) {}

    private record Compilation(
            Path root,
            Path output,
            Path generated,
            boolean success,
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {}

    private record InvalidCase(String typeName, String variant, String code) {

        private String source() {
            String prefix = "package sample;\n" + "import io.github.aresprojects.annotation.LambdaHandler;\n";
            return switch (variant) {
                case "interface" -> prefix + "@LambdaHandler(\"interface\") public interface " + typeName + " {}";
                case "package" -> handlerSource(typeName, "package").replace("public final", "final");
                case "abstract" -> handlerSource(typeName, "abstract").replace("public final", "public abstract");
                case "noConstructor" ->
                    prefix
                            + "@LambdaHandler(\"no-constructor\") public final class "
                            + typeName
                            + " { private "
                            + typeName
                            + "() {} public String handle(String input) { return input; } }";
                case "noMethod" ->
                    prefix
                            + "@LambdaHandler(\"no-method\") public final class "
                            + typeName
                            + " { public "
                            + typeName
                            + "() {} }";
                case "staticMethod" ->
                    handlerSource(typeName, "static").replace("public String handle", "public static String handle");
                case "badParameters" ->
                    handlerSource(typeName, "bad-parameters")
                            .replace("String input", "String first, String second, String third");
                case "invalidName" -> handlerSource(typeName, "Invalid_Name");
                case "privateInput" ->
                    prefix
                            + "class Hidden {} @LambdaHandler(\"private-input\") public final class "
                            + typeName
                            + " { public "
                            + typeName
                            + "() {} public String handle(Hidden input) { return input.toString(); } }";
                case "privateOutput" ->
                    prefix
                            + "class Hidden {} @LambdaHandler(\"private-output\") public final class "
                            + typeName
                            + " { public "
                            + typeName
                            + "() {} public Hidden handle(String input) { return new Hidden(); } }";
                case "generic" ->
                    handlerSource(typeName, "generic")
                            .replace("public String handle(String input)", "public <T> T handle(T input)");
                case "async" ->
                    handlerSource(typeName, "async")
                            .replace(
                                    "public String handle(String input)",
                                    "public java.util.concurrent.CompletableFuture<String> handle(String input)");
                case "inner" ->
                    prefix
                            + "public final class "
                            + typeName
                            + " { @LambdaHandler(\"inner\") public final class NestedHandler { "
                            + "public NestedHandler() {} public String handle(String input) { "
                            + "return input; } } }";
                case "badContext" ->
                    handlerSource(typeName, "bad-context").replace("String input", "String input, String context");
                default -> throw new IllegalArgumentException(variant);
            };
        }
    }

    private static final class StubContext implements Context {

        @Override
        public String getAwsRequestId() {
            return "request";
        }

        @Override
        public String getLogGroupName() {
            return "group";
        }

        @Override
        public String getLogStreamName() {
            return "stream";
        }

        @Override
        public String getFunctionName() {
            return "function";
        }

        @Override
        public String getFunctionVersion() {
            return "version";
        }

        @Override
        public String getInvokedFunctionArn() {
            return "arn";
        }

        @Override
        public CognitoIdentity getIdentity() {
            return null;
        }

        @Override
        public ClientContext getClientContext() {
            return null;
        }

        @Override
        public int getRemainingTimeInMillis() {
            return 100;
        }

        @Override
        public int getMemoryLimitInMB() {
            return 128;
        }

        @Override
        public LambdaLogger getLogger() {
            return new LambdaLogger() {
                @Override
                public void log(String message) {}

                @Override
                public void log(byte[] message) {}
            };
        }
    }
}
