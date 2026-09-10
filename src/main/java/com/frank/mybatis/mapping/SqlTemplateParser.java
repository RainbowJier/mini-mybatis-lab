package com.frank.mybatis.mapping;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This parser only accepts simple parameter names and reject ${} and illegal #{} symbols.
 */
public final class SqlTemplateParser {

    private static final Pattern TOKEN = Pattern.compile(
            "#\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*}");

    private SqlTemplateParser() {
    }

    public static PreparedSql parse(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("SQL is empty");
        if (raw.contains("${")) throw new IllegalArgumentException("Illegal SQL: " + raw);

        Matcher m = TOKEN.matcher(raw);
        StringBuffer sql = new StringBuffer();
        List<String> names = new ArrayList<>();

        while (m.find()) {
            names.add(m.group(1));
            m.appendReplacement(sql, "?");
        }
        m.appendTail(sql);

        if (sql.indexOf("#{") >= 0) throw new IllegalArgumentException("非法占位符");

        return new PreparedSql(sql.toString(), names);
    }


}
