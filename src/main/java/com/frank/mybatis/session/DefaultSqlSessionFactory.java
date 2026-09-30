package com.frank.mybatis.session;

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
