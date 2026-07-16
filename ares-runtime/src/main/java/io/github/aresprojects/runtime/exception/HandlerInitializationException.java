package io.github.aresprojects.runtime.exception;

/** Indicates that a generated adapter could not initialize its handler. */
public final class HandlerInitializationException extends RuntimeException {

    /** Creates an initialization exception with its original cause. */
    public HandlerInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
