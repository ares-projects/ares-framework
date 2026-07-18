package io.github.aresprojects.runtime.aws;

import com.amazonaws.services.lambda.runtime.Context;
import io.github.aresprojects.runtime.InvocationContext;
import java.util.Objects;

/** Adapts the AWS Lambda context to the Ares invocation context. */
public final class AwsInvocationContext implements InvocationContext {

    private final Context delegate;

    /** Creates an adapter for an AWS Lambda context. */
    public AwsInvocationContext(Context delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    }

    @Override
    public String requestId() {
        return delegate.getAwsRequestId();
    }

    @Override
    public String functionName() {
        return delegate.getFunctionName();
    }

    @Override
    public String functionVersion() {
        return delegate.getFunctionVersion();
    }

    @Override
    public String logGroupName() {
        return delegate.getLogGroupName();
    }

    @Override
    public String logStreamName() {
        return delegate.getLogStreamName();
    }

    @Override
    public int remainingTimeInMillis() {
        return delegate.getRemainingTimeInMillis();
    }
}
