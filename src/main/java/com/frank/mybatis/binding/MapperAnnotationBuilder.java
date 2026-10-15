package com.frank.mybatis.binding;


import com.frank.mybatis.annotations.Delete;
import com.frank.mybatis.annotations.Insert;
import com.frank.mybatis.annotations.Select;
import com.frank.mybatis.annotations.Update;
import com.frank.mybatis.common.SessionId;
import com.frank.mybatis.executor.ParameterHandler;
import com.frank.mybatis.mapping.MappedStatement;
import com.frank.mybatis.mapping.PreparedSql;
import com.frank.mybatis.mapping.SqlCommandType;
import com.frank.mybatis.mapping.SqlTemplateParser;
import com.frank.mybatis.session.Configuration;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 唯一注解、SQL 解析、参数名称和返回形状检查
 */
public final class MapperAnnotationBuilder {

    private final Configuration configuration;

    private final Class<?> mapperType;

    public MapperAnnotationBuilder(Configuration configuration, Class<?> mapperType) {
        this.configuration = configuration;
        this.mapperType = mapperType;
    }

    public void parse() {
        Set<String> names = new HashSet<>();

        // get declared methods from mapper interface.
        Method[] declaredMethods = mapperType.getDeclaredMethods();

        // no support for default,static,overload method.
        for (Method m : declaredMethods) {
            if (m.isDefault() || Modifier.isStatic(m.getModifiers()) || !names.add(m.getName())) {
                throw new IllegalArgumentException("Invalid method: " + m);
            }

            parseMethod(m);
        }
    }

    private void parseMethod(Method m) {
        SqlCommandType commandType = commandOf(m);

        // extract raw SQL string from annotation by type.
        // such as: @Select("select * from t_user where name = #{name}")
        // raw SQL: "select * from t_user where name = #{name}"
        String rawSql = switch (commandType) {
            case SELECT -> m.getAnnotation(Select.class).value();
            case INSERT -> m.getAnnotation(Insert.class).value();
            case UPDATE -> m.getAnnotation(Update.class).value();
            case DELETE -> m.getAnnotation(Delete.class).value();
        };

        // parse SQL, get preparedSql and parameter names list.
        // " #{name}" -> preparedSql="name = ?", parameterNames=["name"]
        PreparedSql preparedSql = SqlTemplateParser.parse(rawSql);

        // parse method parameters, get declared parameter map.
        // such as: method findByName(@Param("name") String name)
        // declared: {"name": null}
        Map<String, Object> declared = ParameterHandler.resolve(m, new Object[m.getParameterCount()]);

        // check parameter is valid.
        for (String name : preparedSql.parameterNames()) {
            if (!declared.containsKey(name)) throw new IllegalArgumentException("SQL 参数未声明: " + name);
        }

        boolean many = false;
        Class<?> resultType = null;

        // check return type is valid.
        if (commandType == SqlCommandType.SELECT) {
            if (m.getReturnType() == List.class) {
                // getGenericReturnType() gets type with generic, e.g. List<TUser>.
                Type t = m.getGenericReturnType();
                // two conditions must be met:
                // 1. Parametric type (List<T> instead of bare List)
                // 2. First generic argument is a concrete class (e.g. List<TUser> instead of List<?> or List<T> or List<List<...>>)
                if (!(t instanceof ParameterizedType p) || !(p.getActualTypeArguments()[0] instanceof Class<?>)) {
                    throw new IllegalArgumentException("List must be parameterized with concrete class type.");
                }
                // get TUser class.
                resultType = (Class<?>) p.getActualTypeArguments()[0];
                many = true;
            } else {
                resultType = m.getReturnType();
                // 必须是具体类才能 new 出来并填充字段，故拒绝：
                // result type must be a concrete class.
                // - interface (cannot be instantiated)
                // - Object (too generic, don't know what to map it to)
                if (resultType.isInterface() || resultType == Object.class) {
                    throw new IllegalArgumentException("The query must return a concrete class.");
                }
            }
        } else {
            if (m.getReturnType() != int.class) {
                throw new IllegalArgumentException("Result type must be int.");
            }
            resultType = Void.class;
        }

        String sessionId = SessionId.of(mapperType, m.getName());
        configuration.addMappedStatement(new MappedStatement(sessionId, commandType, rawSql, preparedSql, resultType, many));
    }

    private SqlCommandType commandOf(Method m) {
        int count = (m.isAnnotationPresent(Select.class) ? 1 : 0)
                + (m.isAnnotationPresent(Insert.class) ? 1 : 0)
                + (m.isAnnotationPresent(Update.class) ? 1 : 0)
                + (m.isAnnotationPresent(Delete.class) ? 1 : 0);
        if (count != 1) throw new IllegalArgumentException("必须且只能有一个 SQL 注解: " + m);
        if (m.isAnnotationPresent(Select.class)) return SqlCommandType.SELECT;
        if (m.isAnnotationPresent(Insert.class)) return SqlCommandType.INSERT;
        if (m.isAnnotationPresent(Update.class)) return SqlCommandType.UPDATE;
        return SqlCommandType.DELETE;
    }

}
