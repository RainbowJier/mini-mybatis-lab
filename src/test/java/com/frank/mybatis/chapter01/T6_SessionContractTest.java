package com.frank.mybatis.chapter01;

import com.frank.mybatis.fixture.TUser;
import com.frank.mybatis.mapping.*;
import com.frank.mybatis.session.*;
import com.frank.mybatis.session.DefaultSqlSessionFactory;
import com.frank.mybatis.support.H2DatabaseSupport;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class T6_SessionContractTest {
    private SqlSessionFactory factory() {
        Configuration c = new Configuration(H2DatabaseSupport.newDataSource());
        c.addMappedStatement(stmt("chapter01.session.insert",
                "insert into t_user(id,user_name,age) values(#{id},#{name},#{age})",
                SqlCommandType.INSERT));
        c.addMappedStatement(stmt("chapter01.session.findById",
                "select id,user_name,age from t_user where id=#{id}",
                SqlCommandType.SELECT));
        return new DefaultSqlSessionFactory(c);
    }

    private MappedStatement stmt(String id, String sql, SqlCommandType command) {
        return new MappedStatement(id, command, sql, SqlTemplateParser.parse(sql), TUser.class, false);
    }

    @Test
    void closeRollsBackUncommittedWork() {
        SqlSessionFactory f = factory();
        try (SqlSession writer = f.openSession()) {
            assertEquals(1, writer.insert("chapter01.session.insert",
                    Map.of("id", 1L, "name", "Pending", "age", 20)));
        }
        try (SqlSession reader = f.openSession()) {
            assertNull(reader.selectOne("chapter01.session.findById", Map.of("id", 1L), TUser.class));
        }
    }

    @Test
    void commitPersistsAcrossSessions() {
        SqlSessionFactory f = factory();
        try (SqlSession writer = f.openSession()) {
            writer.insert("chapter01.session.insert", Map.of("id", 2L, "name", "Kept", "age", 21));
            writer.commit();
        }
        try (SqlSession reader = f.openSession()) {
            TUser u = reader.selectOne("chapter01.session.findById", Map.of("id", 2L), TUser.class);
            assertEquals("Kept", u.getUserName());
        }
    }

    @Test
    void concurrentSessionsDoNotSeeUncommittedRows() {
        SqlSessionFactory f = factory();
        try (SqlSession writer = f.openSession(); SqlSession observer = f.openSession()) {
            writer.insert("chapter01.session.insert", Map.of("id", 3L, "name", "Hidden", "age", 22));
            assertNotNull(writer.selectOne("chapter01.session.findById", Map.of("id", 3L), TUser.class));
            assertNull(observer.selectOne("chapter01.session.findById", Map.of("id", 3L), TUser.class));
        }
    }

    @Test
    void closedSessionRejectsFurtherCalls() {
        SqlSession s = factory().openSession();
        s.close();
        assertThrows(IllegalStateException.class,
                () -> s.selectOne("chapter01.session.findById", Map.of("id", 1L), TUser.class));
        assertThrows(IllegalStateException.class, s::commit);
    }
}
