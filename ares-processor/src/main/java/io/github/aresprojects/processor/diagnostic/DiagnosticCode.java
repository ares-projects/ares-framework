package io.github.aresprojects.processor.diagnostic;

/** Stable diagnostic identifiers emitted by the Ares annotation processor. */
public enum DiagnosticCode {
    ARES001("@LambdaHandler may only be applied to a class"),
    ARES002("Lambda handler class must be public"),
    ARES003("Lambda handler class must be concrete"),
    ARES004("Lambda handler class must declare an accessible public no-argument constructor"),
    ARES005("Lambda handler class must declare exactly one supported handle method"),
    ARES006("Handle method must be public and non-static"),
    ARES007("Handle method must accept one input parameter and an optional InvocationContext"),
    ARES008("Duplicate Lambda handler name"),
    ARES009("Invalid Lambda handler name"),
    ARES010("Input type is not accessible from generated code"),
    ARES011("Output type is not accessible from generated code"),
    ARES012("Generic handler methods are not supported"),
    ARES013("Asynchronous handler return types are not supported"),
    ARES014("Non-static inner handler classes are not supported"),
    ARES015("The optional second parameter must be InvocationContext");

    private final String message;

    DiagnosticCode(String message) {
        this.message = message;
    }

    /** Returns the stable code and default message. */
    public String message() {
        return name() + ": " + message;
    }
}
