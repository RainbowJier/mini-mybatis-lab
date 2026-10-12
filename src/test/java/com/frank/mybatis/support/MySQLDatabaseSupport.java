package com.frank.mybatis.support;

import com.mysql.cj.jdbc.MysqlDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * MySQL 测试基础设施：所有测试共享 127.0.0.1:3307 上的 mini-mybatis-lab 库。
 * <p>
 * H2 时代靠“随机内存库名”做到用例间隔离；换成共享的 MySQL 后改由
 * {@link #newDataSource()} 每次调用时重建 t_user，保证每个测试方法
 * 拿到的都是干净的表结构。
 */
public final class MySQLDatabaseSupport {

    private MySQLDatabaseSupport() {
    }

    public static DataSource newDataSource() {
        MysqlDataSource ds = new MysqlDataSource();
        Properties db = loadDbProperties();
        ds.setURL(db.getProperty("jdbc.url"));
        ds.setUser(db.getProperty("jdbc.username"));
        ds.setPassword(db.getProperty("jdbc.password"));

        try (Connection c = ds.getConnection();
             Statement statement = c.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS t_user");
            statement.execute(loadSchemaScript());
            return ds;
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("初始化 MySQL 失败", e);
        }
    }

    private static Properties loadDbProperties() {
        try (InputStream in = MySQLDatabaseSupport.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found");
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load db.properties", e);
        }
    }

    private static String loadSchemaScript() throws IOException {
        try (InputStream in = MySQLDatabaseSupport.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
