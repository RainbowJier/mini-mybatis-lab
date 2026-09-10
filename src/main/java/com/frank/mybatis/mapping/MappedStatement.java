package com.frank.mybatis.mapping;

import java.util.Objects;


public record MappedStatement(String id, SqlCommandType commandType, String rawSql,
                              PreparedSql preparedSql, Class<?> resultType,
                              boolean returnsMany) {
    public MappedStatement {
        Objects.requireNonNull(id);
        Objects.requireNonNull(commandType);
        Objects.requireNonNull(rawSql);
        Objects.requireNonNull(preparedSql);
        Objects.requireNonNull(resultType);
    }
}
