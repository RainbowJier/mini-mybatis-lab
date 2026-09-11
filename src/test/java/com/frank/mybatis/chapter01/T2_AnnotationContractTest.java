package com.frank.mybatis.chapter01;

import com.frank.mybatis.annotations.*;
import org.junit.jupiter.api.Test;

import java.lang.annotation.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class T2_AnnotationContractTest {

    @Test
    void sqlAnnotationsAreRuntimeMethodAnnotations() {
        List<Class<?>> annotations = List.of(Select.class, Insert.class, Update.class, Delete.class);
        for (Class<?> type : annotations) {
            assertEquals(RetentionPolicy.RUNTIME, type.getAnnotation(Retention.class).value());
            assertArrayEquals(new ElementType[]{ElementType.METHOD}, type.getAnnotation(Target.class).value());
        }

        assertEquals(RetentionPolicy.RUNTIME, Param.class.getAnnotation(Retention.class).value());
        assertArrayEquals(new ElementType[]{ElementType.PARAMETER}, Param.class.getAnnotation(Target.class).value());
    }
}
