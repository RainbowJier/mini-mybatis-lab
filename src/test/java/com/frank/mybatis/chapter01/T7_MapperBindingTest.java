package com.frank.mybatis.chapter01;

import com.frank.mybatis.chapter01.mapper.UserMapper;
import com.frank.mybatis.fixture.TUser;
import com.frank.mybatis.session.Configuration;
import com.frank.mybatis.session.DefaultSqlSessionFactory;
import com.frank.mybatis.session.SqlSession;
import com.frank.mybatis.support.MySQLDatabaseSupport;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class T7_MapperBindingTest {

    @Test
    void registrationRejectsDuplicatesAndUnknownStatements() {
        Configuration c = new Configuration(MySQLDatabaseSupport.newDataSource());
        c.addMapper(UserMapper.class);
        assertThrows(IllegalArgumentException.class, () -> c.addMapper(UserMapper.class));
        assertThrows(IllegalArgumentException.class, () -> c.addMapper(TUser.class));
        assertThrows(IllegalArgumentException.class, () -> c.getMappedStatement("missing.id"));
        var statement = c.getMappedStatement(UserMapper.class.getName() + ".rename");
        assertEquals(List.of("name", "id"), statement.preparedSql().parameterNames());
    }


    @Test
    void proxyObjectMethodsDoNotExecuteSql() {
        DataSource ds = MySQLDatabaseSupport.newDataSource();
        Configuration c = new Configuration(ds);
        c.addMapper(UserMapper.class);
        try (SqlSession s = new DefaultSqlSessionFactory(c).openSession()) {
            UserMapper first = s.getMapper(UserMapper.class);
            UserMapper second = s.getMapper(UserMapper.class);
            assertEquals(first, first);
            assertNotEquals(first, second);
            assertNotEquals(null, first);
            assertEquals(System.identityHashCode(first), first.hashCode());
            assertTrue(first.toString().contains(UserMapper.class.getName()));
        }
    }
}
