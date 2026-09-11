package com.frank.mybatis.chapter01;

import com.frank.mybatis.support.H2DatabaseSupport;
import com.frank.mybatis.transaction.JdbcTransaction;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class T5_JdbcTransactionTest {

    @Test
    void commitPersistsAndRollbackDiscards() throws SQLException {
        DataSource ds = H2DatabaseSupport.newDataSource();
        JdbcTransaction transaction = new JdbcTransaction(ds);
        Connection connection = transaction.getConnection();

        try (transaction; Statement s = connection.createStatement()) {
            assertFalse(connection.getAutoCommit());
            s.executeUpdate("insert into t_user values(1,'Committed',20)");
            transaction.commit();

            s.executeUpdate("insert into t_user values(2,'Rolled back',20)");
            transaction.rollback();
        }

        assertTrue(connection.isClosed());
        assertDoesNotThrow(transaction::close);
        assertThrows(IllegalStateException.class, transaction::getConnection);
        assertThrows(IllegalStateException.class, transaction::commit);

        // Here get a new Connection object, don't use the same connection from transaction.
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            ResultSet resultSet = s.executeQuery("select * from t_user order by id");
            assertTrue(resultSet.next());
        }
    }
}
