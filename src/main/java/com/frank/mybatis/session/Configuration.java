package com.frank.mybatis.session;

import com.frank.mybatis.mapping.MappedStatement;
import lombok.Getter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public final class Configuration {

    @Getter
    private final DataSource dataSource;

    private final Map<String, MappedStatement> statements = new HashMap<>();

    public Configuration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void addMappedStatement(MappedStatement m) {
        if (statements.putIfAbsent(m.sessionId(), m) != null) {
            throw new IllegalArgumentException("Duplicate statement id: " + m.sessionId());
        }
    }

    public MappedStatement getMappedStatement(String sessionId) {
        MappedStatement m = statements.get(sessionId);
        if (m == null) {
            throw new IllegalArgumentException("unknown statement id: " + sessionId);
        }

        return m;
    }

}
