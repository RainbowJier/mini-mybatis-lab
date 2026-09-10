package com.frank.mybatis;

import com.frank.mybatis.support.H2DatabaseSupport;
import lombok.Data;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

class JdbcBaselineTest {

    @Test
    void bindQueryNullAndRollback() throws Exception {
        DataSource ds = H2DatabaseSupport.newDataSource();
        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            
            try (PreparedStatement p = c.prepareStatement("INSERT INTO t_user (id, user_name,age) VALUES (?, ?,?)")) {
                p.setLong(1, 1L);
                p.setString(2, "frank");
                p.setNull(3, Types.INTEGER);
                assertEquals(1, p.executeUpdate());
            }

            try (PreparedStatement p = c.prepareStatement("select age from t_user where id=?")) {
                p.setLong(1, 1L);
                try (ResultSet r = p.executeQuery()) {
                    assertTrue(r.next());
                    assertNull(r.getObject(1));
                }
            }
            c.rollback();
        }

        try (Connection c = ds.getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("select count(*) from t_user")) {
            assertTrue(r.next());
            assertEquals(0, r.getInt(1));
        }
    }

}
