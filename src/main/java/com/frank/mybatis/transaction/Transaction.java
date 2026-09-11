package com.frank.mybatis.transaction;

import java.sql.Connection;

public interface Transaction {
    Connection getConnection();

    void commit();

    void rollback();

    void close();
}
