package com.frank.mybatis.executor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

public class ResultSetHandler {

    /**
     * 返回 User 列表。
     * <p>
     * 例：DB 返回的 result set
     * <pre>
     *    id    |   full_name
     *    1     |   frank
     *    2     |   pi
     * </pre>
     * <p>
     * step1: fieldsOf(User)  ==> { "id" -> Field.id, "fullName" -> Field.fullName }
     * step2: rs.getMetaData() -> meta（3 行缓存在 result set 中）
     * step3: 游标定位到第 1 行 (rs.next()==true)
     * - i=1: meta.getColumnLabel(1)="id"       -> fields["id"]=Field.id
     * rs.getObject(1)=1                    -> Field.id  = 1      -> target "1"
     * - i=2: meta.getColumnLabel(2)="full_name"  -> key() 转驼峰 = "fullName"
     * fields["fullName"]=Field.fullName
     * rs.getObject(2)="frank"              -> Field.fullName = "frank" -> target "frank"
     * result.add(target)  ==> [User(id=1, fullName=frank)]
     * step4: 游标定位到第 2 行 (rs.next()==true)，同上
     * result ==> [User(id=1, fullName=frank), User(id=2, fullName=pi)]
     * step5: rs.next()==false，循环结束
     *
     * @param rs   游标将被移动到末尾
     * @param type app 里的映射类型，这里为 User
     * @return 映射好的对象列表 buffer，无数据时为空列表
     */
    public <T> List<T> handle(ResultSet rs, Class<T> type) throws SQLException {
        Map<String, Field> fields = fieldsOf(type);
        ResultSetMetaData meta = rs.getMetaData();
        List<T> result = new ArrayList<>();

        while (rs.next()) {
            try {
                Constructor<T> c = type.getDeclaredConstructor();
                c.setAccessible(true);
                T target = c.newInstance();

                // JDBC 的列下标从 1 开始（ResultSetMetaData / ResultSet 均是）
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String label = meta.getColumnLabel(i);
                    Field f = fields.get(key(label));
                    if (f == null) {
                        throw new IllegalStateException("There is no field for column " + label);
                    }

                    Object value = rs.getObject(i);
                    if (value == null && f.getType().isPrimitive()) {
                        throw new IllegalStateException("Null value for primitive type " + f.getType());
                    }

                    f.set(target, value);
                }
                result.add(target);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }


    /**
     * 例：type=User
     * <pre>
     *     class User {
     *         private Long   id;       // 非 static：保留       -> result["id"]
     *         private String fullName; // 非 static：保留       -> result["fullName"]
     *         public static final int MAX_AGE = 150;   // static final：保留（常量）
     *         public static String version;            // static 且非 final：跳过
     *     }
     * </pre>
     * 返回 { "id"->Field.id, "fullName"->Field.fullName }（version 不在其中）
     * <p>
     * 该 map 供 handle() 按列名查找字段；若 type 一个可映射字段都没有，抛异常。
     */
    private Map<String, Field> fieldsOf(Class<?> type) {
        Map<String, Field> result = new HashMap<>();
        for (Field f : type.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || Modifier.isFinal(f.getModifiers())) {
                f.setAccessible((true));
                result.put(key(f.getName()), f);
            }
        }

        if (result.isEmpty()) throw new IllegalArgumentException("No fields found in " + type.getName());
        return result;
    }

    /**
     * 转驼峰，且大小写不敏感。
     * <p>
     * 规则：遇到 '_' 或“小写→大写”的驼峰边界时断开；第一段全小写，后续每段首字母大写。
     * 例：
     * key("full_name") ==> "fullName"
     * key("FULL_NAME") ==> "fullName"
     * key("fullName")  ==> "fullName"
     * key("ID")        ==> "id"
     * key("userID")    ==> "userId"
     * <p>
     * 这样 H2 返回的大写列名（ID / USER_NAME / AGE）与 Java 驼峰字段名
     * （id / userName / age）会归一化到同一个 key。
     */
    private String key(String value) {
        StringBuilder camel = new StringBuilder(value.length());
        boolean capitalizeNext = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '_') {
                capitalizeNext = true;
            } else if (i > 0 && Character.isLowerCase(value.charAt(i - 1)) && Character.isUpperCase(c)) {
                // 已是驼峰（如 full_name 之外的 fullName / userID），保留其大写字母
                camel.append(c);
            } else if (capitalizeNext) {
                camel.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                camel.append(Character.toLowerCase(c));
            }
        }
        return camel.toString();
    }
}