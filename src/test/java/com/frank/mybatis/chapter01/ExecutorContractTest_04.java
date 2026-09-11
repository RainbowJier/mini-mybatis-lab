package com.frank.mybatis.chapter01;

import com.frank.mybatis.executor.Executor;
import com.frank.mybatis.executor.SimpleExecutor;
import com.frank.mybatis.fixture.TUser;
import com.frank.mybatis.mapping.MappedStatement;
import com.frank.mybatis.mapping.SqlCommandType;
import com.frank.mybatis.mapping.SqlTemplateParser;
import com.frank.mybatis.support.H2DatabaseSupport;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ExecutorContractTest_04 {

    private MappedStatement statement(String sql, SqlCommandType command) {
        return new MappedStatement("test.statement", command, sql,
                SqlTemplateParser.parse(sql), TUser.class, false);
    }

    @Test
    void bindsBySqlOrderAndKeepsConnectionOpen() throws SQLException {
        try (Connection c = H2DatabaseSupport.newDataSource().getConnection()) {
            c.setAutoCommit(false);
            Executor executor = new SimpleExecutor(c);

            String insertSql = "INSERT INTO t_user (id, user_name, age) VALUES (#{id},#{userName},#{age})";
            MappedStatement insert = statement(insertSql, SqlCommandType.INSERT);
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("id", 1L);
            values.put("userName", "Frank");
            values.put("age", null);
            assertEquals(1, executor.update(insert, values));

            String selectSql = "select id,user_name,age from t_user where id=#{id}";
            MappedStatement query = statement(selectSql, SqlCommandType.SELECT);
            TUser user = executor.queryOne(query, Map.of("id", 1L), TUser.class);
            assertEquals("Frank", user.getUserName());
            assertNull(user.getAge());

            assertFalse(c.isClosed());
            c.rollback();
        }
    }


    @Test
    void selectOneRejectsMultipleRows() throws SQLException {
        try (Connection c = H2DatabaseSupport.newDataSource().getConnection();
             Statement s = c.createStatement()) {

            String insertSql = "insert into t_user values(2,'A',20),(3,'B',21)";
            s.executeUpdate(insertSql);

            Executor executor = new SimpleExecutor(c);
            String selectSql = "select id,user_name,age from t_user";
            MappedStatement statement = statement(selectSql, SqlCommandType.SELECT);
            List<TUser> tUsers = executor.queryList(statement, Map.of(), TUser.class);
            System.out.println(tUsers);
            assertEquals(2, tUsers.size());
        }
    }

}
