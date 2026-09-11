package com.frank.mybatis.transaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTransaction implements Transaction, AutoCloseable {
    private final Connection connection;
    private boolean closed;

    public JdbcTransaction(DataSource ds) {
        try {
            connection = ds.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to get datasource connection.");
        }
    }

    @Override
    public Connection getConnection() {
        requireOpen();
        return connection;
    }

    @Override
    public void commit() {
        requireOpen();
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to commit transaction.", e);
        }
        // execute("Failed to commit transaction.", connection::commit);
    }

    @Override
    public void rollback() {
        requireOpen();
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rollback transaction.", e);
        }

        // execute("Failed to rollback transaction.", connection::rollback);
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                connection.close();
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to close connection.", e);
            }
        }

    }

    private void execute(String message, SqlAction action) {
        requireOpen();
        try {
            action.run();
        } catch (SQLException e) {
            throw new IllegalStateException(message, e);
        }
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Transaction is closed.");
        }
    }

    @FunctionalInterface
    private interface SqlAction {
        void run() throws SQLException;
    }
}
