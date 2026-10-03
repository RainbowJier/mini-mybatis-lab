package com.frank.mybatis.binding;

import com.frank.mybatis.executor.ParameterHandler;
import com.frank.mybatis.mapping.MappedStatement;
import com.frank.mybatis.session.Configuration;
import com.frank.mybatis.session.SqlSession;
import org.h2.engine.Session;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

public final class MapperProxy implements InvocationHandler {

    private final SqlSession session;

    private final Configuration configuration;

    private final Class<?> mapperType;

    public MapperProxy(SqlSession session, Configuration configuration, Class<?> mapperType) {
        this.session = session;
        this.configuration = configuration;
        this.mapperType = mapperType;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "toString" -> "MapperProxy(" + mapperType.getName() + ")";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> throw new IllegalStateException("不支持 Object 方法");
            };
        }

        String sessionId = mapperType.getName() + ":" + method.getName();
        MappedStatement mappedStatement = configuration.getMappedStatement(sessionId);
        Map<String, Object> p = ParameterHandler.resolve(method, args);
        return switch (mappedStatement.commandType()) {
            case SELECT ->
                    mappedStatement.returnsMany() ? session.selectList(sessionId, p, mappedStatement.resultType())
                            : session.selectOne(sessionId, p, mappedStatement.resultType());
            case INSERT -> session.insert(sessionId, p);
            case UPDATE -> session.update(sessionId, p);
            case DELETE -> session.delete(sessionId, p);
        };

    }
}
