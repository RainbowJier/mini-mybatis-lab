package com.frank.mybatis;

import org.h2.jdbcx.JdbcDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Mini-MyBatis 实验项目的启动入口。
 */
public final class MiniMybatisApplication {

    private static final String DATABASE_URL =
            "jdbc:h2:mem:mini_mybatis;DB_CLOSE_DELAY=-1";

    private MiniMybatisApplication() {
    }

    public static void main(String[] args) {
        JdbcDataSource dataSource = createDataSource();
        initializeDatabase(dataSource);
        System.out.println("mini-mybatis-lab started");
        System.out.println("database initialized: " + DATABASE_URL);
        System.out.println("table initialized: T_USER");
    }

    private static JdbcDataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(DATABASE_URL);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private static void initializeDatabase(JdbcDataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             InputStream script = loadSchemaScript();
             Statement statement = connection.createStatement()) {

            statement.execute(new String(script.readAllBytes(), StandardCharsets.UTF_8));

            verifyTable(connection);
        } catch (IOException | SQLException exception) {
            throw new IllegalStateException("Failed to initialize database from schema.sql", exception);
        }
    }

    private static InputStream loadSchemaScript() {
        InputStream script = MiniMybatisApplication.class.getClassLoader().getResourceAsStream("schema.sql");
        if (script == null) {
            throw new IllegalStateException("schema.sql was not found on the classpath");
        }
        return script;
    }

    private static void verifyTable(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'T_USER'";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            if (resultSet.getInt(1) != 1) {
                throw new SQLException("T_USER was not created");
            }
        }
    }
}
