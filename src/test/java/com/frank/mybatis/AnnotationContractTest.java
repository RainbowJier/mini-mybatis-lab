package com.frank.mybatis;

import com.frank.mybatis.annotations.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnnotationContractTest {

    @Test
    void sqlAnnotationsAreRuntimeMethodAnnotations() {
        for (Class<?> type : List.of(Select.class, Insert.class, Update.class, Delete.class)) {

            assertEquals(RetentionPolicy.RUNTIME, type.getAnnotation(Retention.class).value());

            assertArrayEquals(new ElementType[]{ElementType.METHOD}, type.getAnnotation(Target.class).value());
        }

        assertEquals(RetentionPolicy.RUNTIME, Param.class.getAnnotation(Retention.class).value());

        assertArrayEquals(new ElementType[]{ElementType.PARAMETER}, Param.class.getAnnotation(Target.class).value());
    }
}
