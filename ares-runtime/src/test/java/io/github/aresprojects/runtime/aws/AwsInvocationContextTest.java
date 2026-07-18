package io.github.aresprojects.runtime.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import io.github.aresprojects.runtime.exception.HandlerInitializationException;
import org.junit.jupiter.api.Test;

class AwsInvocationContextTest {

    @Test
    void delegatesEveryContextValue() {
        Context context = new StubContext();
        AwsInvocationContext adapted = new AwsInvocationContext(context);

        assertEquals("request", adapted.requestId());
        assertEquals("function", adapted.functionName());
        assertEquals("version", adapted.functionVersion());
        assertEquals("group", adapted.logGroupName());
        assertEquals("stream", adapted.logStreamName());
        assertEquals(123, adapted.remainingTimeInMillis());
    }

    @Test
    void rejectsNullDelegate() {
        assertThrows(NullPointerException.class, () -> new AwsInvocationContext(null));
    }

    @Test
    void preservesInitializationCause() {
        RuntimeException cause = new RuntimeException("cause");
        HandlerInitializationException exception = new HandlerInitializationException("message", cause);

        assertEquals("message", exception.getMessage());
        assertEquals(cause, exception.getCause());
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
            return 123;
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
