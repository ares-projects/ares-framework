package io.github.aresprojects.processor.model;

/** Immutable processor model for a validated handle method. */
public record HandlerMethodModel(
        String methodName, String inputTypeName, String outputTypeName, boolean acceptsContext, boolean returnsVoid) {}
