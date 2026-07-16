package io.github.aresprojects.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Declares a plain Java class as an Ares Lambda handler. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface LambdaHandler {

    /** Returns the logical name used in the generated handler manifest. */
    String value();
}
