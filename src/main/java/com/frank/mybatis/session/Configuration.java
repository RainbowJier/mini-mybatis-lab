package com.frank.mybatis.session;

import com.frank.mybatis.binding.MapperRegistry;
import com.frank.mybatis.mapping.MappedStatement;
import lombok.Data;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Data
public final class Configuration {

    private final DataSource dataSource;

    private final Map<String, MappedStatement> statements = new HashMap<>();

    private final MapperRegistry mapperRegistry = new MapperRegistry(this);

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

    public <T> void addMapper(Class<T> type) {
        mapperRegistry.addMapper(type);
    }

}
