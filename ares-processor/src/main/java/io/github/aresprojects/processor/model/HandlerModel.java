package io.github.aresprojects.processor.model;

/** Immutable processor model for one validated handler. */
public record HandlerModel(
        String name,
        String sourceClassName,
        String adapterClassName,
        String awsHandler,
        String inputTypeName,
        String outputTypeName,
        boolean acceptsContext,
        boolean returnsVoid) {}
