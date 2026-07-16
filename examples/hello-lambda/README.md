# Hello Lambda example

This example uses a plain Java class without implementing an AWS Lambda interface.

```java
@LambdaHandler("hello")
public final class HelloHandler {
    public HelloResponse handle(HelloRequest request, InvocationContext context) {
        return new HelloResponse("Hello, " + request.name(), context.requestId());
    }
}
```

The Ares processor generates this AWS Lambda handler:

```text
com.example.hello.ares.generated.HelloHandlerAresAdapter::handleRequest
```

Handler instances are reused across invocations. Do not store invocation-specific mutable state in handler instance fields.
