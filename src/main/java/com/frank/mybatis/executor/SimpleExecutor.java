package com.frank.mybatis.executor;

import com.frank.mybatis.mapping.MappedStatement;
import com.frank.mybatis.mapping.SqlCommandType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


public class SimpleExecutor implements Executor {

    private final Connection connection;
    private final ResultSetHandler resultSetHandler = new ResultSetHandler();

    public SimpleExecutor(Connection connection) {
        this.connection = connection;
    }

    @Override
    public <T> T queryOne(MappedStatement s, Map<String, Object> values, Class<T> type) {
        List<T> rows = queryList(s, values, type);
        if (rows.isEmpty()) return null;
        if (rows.size() > 1) throw new IllegalStateException("Result has more than one row");
        return rows.get(0);
    }

    @Override
    public <T> List<T> queryList(MappedStatement s, Map<String, Object> values, Class<T> type) {
        require(s, SqlCommandType.SELECT);
        try (PreparedStatement ps = connection.prepareStatement(s.preparedSql().sql())) {
            ParameterHandler.bind(ps, s.preparedSql(), values);
            try (ResultSet rs = ps.executeQuery()) {
                return resultSetHandler.handle(rs, type);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Query failed", e);
        }
    }

    @Override
    public int update(MappedStatement s, Map<String, Object> values) {
        if (s.commandType() == SqlCommandType.SELECT) {
            throw new IllegalArgumentException("Invalid command type: " + s.commandType());
        }

        try (PreparedStatement ps = connection.prepareStatement(s.preparedSql().sql())) {
            ParameterHandler.bind(ps, s.preparedSql(), values);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Update failed", e);
        }

    }

    private void require(MappedStatement s, SqlCommandType expected) {
        if (s.commandType() != expected) throw new IllegalArgumentException("Invalid command type: " + s.commandType());
    }
}
