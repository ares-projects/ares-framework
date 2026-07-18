package com.example.hello;

import io.github.aresprojects.annotation.LambdaHandler;
import io.github.aresprojects.runtime.InvocationContext;

/** Handles the hello Lambda example. */
@LambdaHandler("hello")
public final class HelloHandler {

    /** Creates the example handler. */
    public HelloHandler() {}

    /** Returns a greeting containing the current request identifier. */
    public HelloResponse handle(HelloRequest request, InvocationContext context) {
        return new HelloResponse("Hello, " + request.name(), context.requestId());
    }
}
