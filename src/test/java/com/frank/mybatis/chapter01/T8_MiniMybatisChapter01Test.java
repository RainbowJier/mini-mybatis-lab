package com.frank.mybatis.chapter01;

import com.frank.mybatis.chapter01.mapper.UserMapper;
import com.frank.mybatis.fixture.TUser;
import com.frank.mybatis.session.Configuration;
import com.frank.mybatis.session.DefaultSqlSessionFactory;
import com.frank.mybatis.session.SqlSession;
import com.frank.mybatis.session.SqlSessionFactory;
import com.frank.mybatis.support.MySQLDatabaseSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class T8_MiniMybatisChapter01Test {

    private SqlSessionFactory factory;

    @BeforeEach
    void setUp() {
        DataSource ds = MySQLDatabaseSupport.newDataSource();
        Configuration c = new Configuration(ds);
        c.addMapper(UserMapper.class);
        factory = new DefaultSqlSessionFactory(c);
    }

    @Test
    void crudAndCommit() {
        try (SqlSession s = factory.openSession()) {
            UserMapper m = s.getMapper(UserMapper.class);
            assertEquals(1, m.insert(1L, "Frank", 25));
            assertEquals(1, m.insert(2L, "Alice", null));
            assertEquals(1, m.rename(1L, "Frank Updated"));
            assertEquals("Frank Updated", m.findById(1L).getUserName());
            assertNull(m.findById(2L).getAge());
            List<TUser> users = m.findByMinimumAge(18);
            assertEquals(1, users.size());
            assertEquals(1, m.deleteById(2L));
            s.commit();
        }
        try (SqlSession s = factory.openSession()) {
            UserMapper m = s.getMapper(UserMapper.class);
            assertEquals("Frank Updated", m.findById(1L).getUserName());
            assertNull(m.findById(2L));
        }
    }

    @Test
    void rollbackHidesWriteFromNextSession() {
        try (SqlSession s = factory.openSession()) {
            assertEquals(1, s.getMapper(UserMapper.class).insert(3L, "Rollback", 20));
            s.rollback();
        }
        try (SqlSession s = factory.openSession()) {
            assertNull(s.getMapper(UserMapper.class).findById(3L));
        }
    }

    @Test
    void closedSessionRejectsMapperCall() {
        SqlSession s = factory.openSession();
        UserMapper m = s.getMapper(UserMapper.class);
        s.close();
        assertThrows(IllegalStateException.class, () -> m.findById(1L));
    }

}
