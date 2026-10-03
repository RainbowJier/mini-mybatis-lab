package com.frank.mybatis.binding;

import com.frank.mybatis.session.Configuration;
import com.frank.mybatis.session.SqlSession;

import java.lang.reflect.Proxy;

public final class MapperProxyFactory<T> {

    private final Class<T> mapperType;

    private final Configuration configuration;

    public MapperProxyFactory(Class<T> mapperType, Configuration configuration) {
        this.mapperType = mapperType;
        this.configuration = configuration;
    }

    /**
     * 为 mapper 接口生成 JDK 动态代理实例。
     *
     * mapper 接口没有任何实现类；这里在运行期动态合成一个实现了 mapperType 的代理对象，
     * 调用方拿到它之后调用任何方法（如 userMapper.selectById(1)），调用都会被转发到
     * {@link MapperProxy#invoke}，由它根据 Configuration 中的 MappedStatement 委托 SqlSession 执行 SQL。
     */
    public T newInstance(SqlSession session) {
        Object proxy = Proxy.newProxyInstance(
                // 参数一：类加载器——用 mapper 接口自身的加载器定义代理类，确保可见性一致
                mapperType.getClassLoader(),
                // 参数二：代理类要实现的接口列表——就是 mapper 接口本身，因此代理对象可直接当作 T 使用
                new Class<?>[]{mapperType},
                // 参数三：调用处理器——真正干活的地方，所有方法调用都进入 MapperProxy.invoke()
                new MapperProxy(session, configuration, mapperType)
        );

        // 等价于 (T) proxy，但类型安全，无需 @SuppressWarnings("unchecked")
        return mapperType.cast(proxy);
    }

}
