package com.frank.mybatis.mapping;

import lombok.Data;

import java.util.List;

@Data
public final class PreparedSql {

    private final String sql;
    private final List<String> parameterNames;

    public PreparedSql(String sql, List<String> parameterNames) {
        if (sql == null || sql.isBlank()) throw new IllegalStateException("SQL is empty");

        // defensive copy, keep immutable, the parameterNames is a snapshot of values at construction time.
        this.sql = sql;
        this.parameterNames = List.copyOf(parameterNames);
    }
}
