package com.frank.mybatis;

import com.frank.mybatis.mapping.PreparedSql;
import com.frank.mybatis.mapping.SqlTemplateParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SqlTemplateParserTest_03 {

    @Test
    void preservesSqlOrderAndRepeatedNames() {
        PreparedSql sql = SqlTemplateParser.parse("select * from t_user where id=#{ id } and age=#{age} or id=#{id}");

        assertEquals("select * from t_user where id=? and age=? or id=?", sql.sql());
        assertEquals(List.of("id", "age", "id"), sql.parameterNames());
    }

    @Test
    void rejectsUnsupportedAndMalformedTemplates() {
        for (String raw : List.of(" ", "select ${column}", "select #{user.id}", "select #{id")) {
            assertThrows(IllegalArgumentException.class, () -> SqlTemplateParser.parse(raw), raw);
        }
        assertThrows(IllegalArgumentException.class, () -> SqlTemplateParser.parse(null));
    }

    @Test
    void parameterNamesAreDefensivelyCopied() {
        var names = new ArrayList<>(List.of("id"));
        PreparedSql sql = new PreparedSql("select ?", names);
        names.clear();
        assertEquals(List.of("id"), sql.parameterNames());
        assertThrows(UnsupportedOperationException.class, () -> sql.parameterNames().clear());
    }


}
