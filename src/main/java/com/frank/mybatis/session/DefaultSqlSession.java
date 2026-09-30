package com.frank.mybatis.session;

import com.frank.mybatis.executor.Executor;
import com.frank.mybatis.executor.SimpleExecutor;
import com.frank.mybatis.mapping.MappedStatement;
import com.frank.mybatis.mapping.SqlCommandType;
import com.frank.mybatis.transaction.Transaction;

import java.util.List;
import java.util.Map;

public final class DefaultSqlSession implements SqlSession {

    private final Configuration configuration;

    private final Transaction transaction;

    private final Executor executor;

    private boolean closed;


    public DefaultSqlSession(Configuration c, Transaction t) {
        this.configuration = c;
        this.transaction = t;
        this.executor = new SimpleExecutor(t.getConnection());
    }

    @Override
    public <T> T selectOne(String sessionId, Map<String, Object> parameter, Class<T> resultType) {
        return executor.queryOne(statement(sessionId, SqlCommandType.SELECT), parameter, resultType);

    }

    @Override
    public <T> List<T> selectList(String sessionId, Map<String, Object> parameter, Class<T> resultType) {
        return executor.queryList(statement(sessionId, SqlCommandType.SELECT), parameter, resultType);
    }

    @Override
    public int insert(String sessionId, Map<String, Object> parameter) {
        return executor.update(statement(sessionId, SqlCommandType.INSERT), parameter);
    }

    @Override
    public int update(String sessionId, Map<String, Object> parameter) {
        return executor.update(statement(sessionId, SqlCommandType.UPDATE), parameter);
    }

    @Override
    public int delete(String sessionId, Map<String, Object> parameter) {
        return executor.update(statement(sessionId, SqlCommandType.DELETE), parameter);
    }

    @Override
    public <T> T getMapper(Class<T> mapperType) {
        return null;
    }


    public void commit() {
        requireOpen();
        transaction.commit();
    }

    public void rollback() {
        requireOpen();
        transaction.rollback();
    }

    public void close() {
        if (!closed) {
            closed = true;
            try {
                transaction.rollback();
            } finally {
                transaction.close();
            }
        }
    }


    private MappedStatement statement(String sessionId, SqlCommandType commandType) {
        requireOpen();

        MappedStatement m = configuration.getMappedStatement(sessionId);
        if (m.commandType() != commandType) {
            throw new IllegalArgumentException("Command type mismatch. Expected: " + m.commandType() + ", but was: " + commandType);

        }

        return m;
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("SqlSession is closed.");
    }
}
