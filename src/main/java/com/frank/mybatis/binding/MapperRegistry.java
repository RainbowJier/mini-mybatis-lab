package com.frank.mybatis.binding;

import com.frank.mybatis.session.Configuration;
import com.frank.mybatis.session.SqlSession;

import java.util.HashMap;
import java.util.Map;

public final class MapperRegistry {
    private final Configuration configuration;
    private final Map<Class<?>, MapperProxyFactory<?>> regedit = new HashMap<>();

    public MapperRegistry(Configuration c) {
        this.configuration = c;
    }

    public <T> void addMapper(Class<T> mapperType) {
        if (!mapperType.isInterface()) {
            throw new IllegalArgumentException("Mapper must be interface.");
        }
        if (regedit.containsKey(mapperType)) {
            throw new IllegalArgumentException("Mapper already registered.");
        }

        // Parse mapper interface
        // scan the annotations of each method, prase sql and validate parameters and the return type.
        MapperAnnotationBuilder mapperAnnotationBuilder = new MapperAnnotationBuilder(configuration, mapperType);
        mapperAnnotationBuilder.parse();

        // 校验通过后，才为该 Mapper 创建代理工厂并注册，供后续 getMapper 生成动态代理实例。
        // Create and register the mapper proxy factory,
        // so that the subsequent getMapper operation can generate a dynamic proxy instance.
        regedit.put(mapperType, new MapperProxyFactory<>(mapperType, configuration));
    }


    public <T> T getMapper(Class<T> mapperType, SqlSession sqlSession) {
        // get Mapper factory from regedit.
        MapperProxyFactory<?> factory = regedit.get(mapperType);
        if (factory == null) {
            throw new IllegalArgumentException("Mapper not registered.");
        }

        // Cast to MapperProxyFactory<T> to avoid class cast exception
        MapperProxyFactory<T> typedFactory = (MapperProxyFactory<T>) factory;
        return typedFactory.newInstance(sqlSession);
    }

}
