package com.frank.mybatis.session;

import java.util.List;
import java.util.Map;

public interface SqlSession extends AutoCloseable {

    /**
     * @param sessionId  session id
     * @param parameter  parameters mapping values.
     * @param resultType result type.
     */
    <T> T selectOne(String sessionId, Map<String, Object> parameter, Class<T> resultType);

    <T> List<T> selectList(String sessionId, Map<String, Object> parameter, Class<T> resultType);

    int insert(String sessionId, Map<String, Object> parameter);

    int update(String sessionId, Map<String, Object> parameter);

    int delete(String sessionId, Map<String, Object> parameter);

    /**
     * calling mapper defined by user.
     */
    <T> T getMapper(Class<T> mapperType);

    void commit();

    void rollback();

    void close();
}
