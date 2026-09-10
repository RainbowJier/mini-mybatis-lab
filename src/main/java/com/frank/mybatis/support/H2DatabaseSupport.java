package com.frank.mybatis.support;

import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.RunScript;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.util.UUID;

public class H2DatabaseSupport {

    private H2DatabaseSupport() {
    }

    public static DataSource newDataSource() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:chapter01_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");

        try (Connection c = ds.getConnection();
             InputStream in = H2DatabaseSupport.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IllegalStateException("schema.sql not found");
            }
            try (Reader reader = new InputStreamReader(in)) {
                RunScript.execute(c, reader);
            }
            return ds;
        } catch (Exception e) {
            throw new IllegalStateException("初始化 H2 失败", e);
        }

    }


}
