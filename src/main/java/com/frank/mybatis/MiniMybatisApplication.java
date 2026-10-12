package com.frank.mybatis;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Mini-MyBatis 实验项目的启动入口。
 * <p>
 * 连接信息统一来自 classpath 上的 db.properties，schema.sql 使用
 * IF NOT EXISTS，保证可以对同一个 MySQL 库重复执行。
 */
public final class MiniMybatisApplication {

    private MiniMybatisApplication() {
    }

    public static void main(String[] args) {
        Properties db = loadDbProperties();
        String url = db.getProperty("jdbc.url");
        try (Connection connection = DriverManager.getConnection(
                url, db.getProperty("jdbc.username"), db.getProperty("jdbc.password"))) {
            initializeDatabase(connection);
            System.out.println("mini-mybatis-lab started");
            System.out.println("database initialized: " + url);
            System.out.println("table initialized: T_USER");
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to connect database: " + url, exception);
        }
    }

    private static Properties loadDbProperties() {
        try (InputStream in = MiniMybatisApplication.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties was not found on the classpath");
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load db.properties", exception);
        }
    }

    private static void initializeDatabase(Connection connection) {
        try (InputStream script = loadSchemaScript();
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
        String sql = "SELECT COUNT(*) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = 't_user'";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            if (resultSet.getInt(1) != 1) {
                throw new SQLException("t_user was not created");
            }
        }
    }
}
