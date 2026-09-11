package com.frank.mybatis.executor;

import com.frank.mybatis.mapping.MappedStatement;

import java.util.List;
import java.util.Map;

public interface Executor {
    <T> T queryOne(MappedStatement s, Map<String, Object> p, Class<T> type);

    <T> List<T> queryList(MappedStatement s, Map<String, Object> p, Class<T> type);

    int update(MappedStatement s, Map<String, Object> p);
}
