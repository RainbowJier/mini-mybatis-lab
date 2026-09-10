package com.frank.mybatis.mapping;

import java.util.List;

public record PreparedSql(String sql, List<String> parameterNames) {
    public PreparedSql {
        if (sql == null || sql.isBlank()) throw new IllegalStateException("SQL is empty");
        // defensive copy, keep immutable, the parameterNames is a snapshot of values at construction time.
        parameterNames = List.copyOf(parameterNames);
    }
}
