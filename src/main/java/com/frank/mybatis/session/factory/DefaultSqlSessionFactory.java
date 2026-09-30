package com.frank.mybatis.session.factory;

import com.frank.mybatis.session.Configuration;
import com.frank.mybatis.session.DefaultSqlSession;
import com.frank.mybatis.session.SqlSession;
import com.frank.mybatis.session.SqlSessionFactory;
import com.frank.mybatis.transaction.JdbcTransaction;

public class DefaultSqlSessionFactory implements SqlSessionFactory {

    private final Configuration configuration;

    public DefaultSqlSessionFactory(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public SqlSession openSession() {
        return new DefaultSqlSession(configuration, new JdbcTransaction(configuration.getDataSource()));

    }

}
