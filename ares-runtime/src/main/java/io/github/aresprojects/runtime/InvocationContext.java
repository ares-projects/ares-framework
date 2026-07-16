package io.github.aresprojects.runtime;

/** A framework-neutral view of the AWS Lambda invocation context. */
public interface InvocationContext {

    /** Returns the AWS request identifier. */
    String requestId();

    /** Returns the deployed function name. */
    String functionName();

    /** Returns the deployed function version. */
    String functionVersion();

    /** Returns the CloudWatch log group name. */
    String logGroupName();

    /** Returns the CloudWatch log stream name. */
    String logStreamName();

    /** Returns the remaining invocation time in milliseconds. */
    int remainingTimeInMillis();
}
