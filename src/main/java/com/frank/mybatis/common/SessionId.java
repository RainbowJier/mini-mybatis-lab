package com.frank.mybatis.common;

public class SessionId {
    private static final char SEPARATOR = ':';

    private SessionId() {
    }

    public static String of(Class<?> mapperType, String methodName) {
        return mapperType.getName() + SEPARATOR + methodName;
    }
}
