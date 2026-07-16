package io.github.aresprojects.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

class LambdaHandlerTest {

    @Test
    void declaresTypeTargetAndClassRetention() {
        Target target = LambdaHandler.class.getAnnotation(Target.class);
        Retention retention = LambdaHandler.class.getAnnotation(Retention.class);

        assertTrue(java.util.Arrays.asList(target.value()).contains(java.lang.annotation.ElementType.TYPE));
        assertEquals(RetentionPolicy.CLASS, retention.value());
    }
}
