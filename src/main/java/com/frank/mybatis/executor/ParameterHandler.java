package com.frank.mybatis.executor;

import com.frank.mybatis.annotations.Param;
import com.frank.mybatis.mapping.PreparedSql;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ParameterHandler {
    private ParameterHandler() {
    }

    public static Map<String, Object> resolve(Method method, Object[] args) {
        Parameter[] ps = method.getParameters();
        Object[] actual = args == null ? new Object[0] : args;
        if (ps.length != actual.length) throw new IllegalArgumentException("Parameter count not match");

        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i < ps.length; i++) {
            Param p = ps[i].getAnnotation(Param.class);

            // if no Param annotation, use default name "arg" + index.
            String name = p == null ? "arg" + i : p.value();

            if (name.isBlank() || values.containsKey(name)) {
                throw new IllegalArgumentException("Invalid parameter name: " + name);
            }

            values.put(name, actual[i]);
        }

        return values;
    }

    /**
     * binding parameter values.
     */
    public static void bind(PreparedStatement ps, PreparedSql sql, Map<String, Object> values) throws SQLException {
        for (int i = 0; i < sql.parameterNames().size(); i++) {
            String name = sql.parameterNames().get(i);
            if (!values.containsKey(name)) throw new IllegalArgumentException("Parameter name not found: " + name);

            ps.setObject(i + 1, values.get(name));
        }
    }

}
