# 手写 MyBatis 实施路线

这份路线不是概念清单，而是按“创建哪个文件 → 写什么 → 依赖谁 → 如何验收”排列的施工顺序。当前只有启动类和数据库表，**第 01 章的框架代码全部待实现**。

## 0. 当前已有文件

```text
pom.xml
src/main/java/com/frank/mybatis/MiniMybatisApplication.java
src/main/resources/schema.sql
```

启动类只初始化 H2 和 `t_user`，不属于 MyBatis 核心。运行：

```bash
mvn compile exec:java
```

## 1. 第 01 章：固定注解 SQL + Mapper 代理

最终调用链：

```text
UserMapper.findById(1L)
→ MapperProxy
→ Configuration 查 MappedStatement
→ SqlSession
→ Executor
→ PreparedStatement
→ ResultSetHandler
→ User
```

### 1.1 测试基础设施（先写）

创建：

```text
src/test/java/com/frank/mybatis/fixture/User.java
src/test/java/com/frank/mybatis/support/H2DatabaseSupport.java
src/test/java/com/frank/mybatis/chapter01/JdbcBaselineTest.java
```

包名与职责：

| 文件 | package | 内容 |
| --- | --- | --- |
| `User.java` | `com.frank.mybatis.fixture` | `id`、`userName`、`age`、无参构造器、getter/setter |
| `H2DatabaseSupport.java` | `com.frank.mybatis.support` | 创建唯一 H2 DataSource，执行 `schema.sql` |
| `JdbcBaselineTest.java` | `com.frank.mybatis.chapter01` | 不使用自定义框架，验证 JDBC CRUD、NULL、资源关闭和事务 |

先让 JDBC 基线测试通过，再开始写框架。测试实体和测试工具放 `src/test`，不要放主代码包。

### 1.2 SQL 注解（无前置依赖）

创建：

```text
src/main/java/com/frank/mybatis/annotations/Select.java
src/main/java/com/frank/mybatis/annotations/Insert.java
src/main/java/com/frank/mybatis/annotations/Update.java
src/main/java/com/frank/mybatis/annotations/Delete.java
src/main/java/com/frank/mybatis/annotations/Param.java
```

统一 package：

```java
package com.frank.mybatis.annotations;
```

每个 SQL 注解都是 `@Retention(RUNTIME)` + `@Target(METHOD)` + `String value()`；`Param` 是 `@Target(PARAMETER)` + `String value()`。

不要在这一阶段创建 MapperProxy 或数据库代码。

### 1.3 映射模型（依赖 annotations）

创建：

```text
src/main/java/com/frank/mybatis/mapping/SqlCommandType.java
src/main/java/com/frank/mybatis/mapping/PreparedSql.java
src/main/java/com/frank/mybatis/mapping/MappedStatement.java
src/main/java/com/frank/mybatis/mapping/SqlTemplateParser.java
```

统一 package：

```java
package com.frank.mybatis.mapping;
```

职责：

- `SqlCommandType`：`SELECT`、`INSERT`、`UPDATE`、`DELETE`；
- `PreparedSql`：保存 JDBC SQL 和参数名顺序，例如 `#{id}` → `?`、`[id]`；
- `MappedStatement`：保存 statement id、原始 SQL、命令类型、返回类型和 PreparedSql；
- `SqlTemplateParser`：只解析简单 `#{name}`，暂不支持 XML、`${}` 和动态 SQL。

建议先给 `SqlTemplateParser` 写纯单元测试：单参数、多参数、重复参数、空白参数名、非法占位符。

### 1.4 参数与结果处理（依赖 mapping）

创建：

```text
src/main/java/com/frank/mybatis/executor/ParameterHandler.java
src/main/java/com/frank/mybatis/executor/ResultSetHandler.java
```

统一 package：

```java
package com.frank.mybatis.executor;
```

`ParameterHandler`：读取 `@Param`，建立名称到值的 Map，再按 SQL 中的参数顺序调用 `PreparedStatement#setObject`。区分“参数不存在”和“参数值为 null”。

`ResultSetHandler`：支持具体 POJO 和 `List<POJO>`；使用 `getColumnLabel`；将 `user_name` 转成 `userName`；SQL NULL 映射为包装类型 null；单对象查询多行时抛异常。

这一阶段可以先用 Mockito 或 H2 写测试，不需要先有 Executor。

### 1.5 执行器（依赖参数/结果处理）

创建：

```text
src/main/java/com/frank/mybatis/executor/Executor.java
src/main/java/com/frank/mybatis/executor/SimpleExecutor.java
```

`Executor` 定义 query、queryList、update 等最小操作；`SimpleExecutor` 负责：

1. 接收 Connection 和 MappedStatement；
2. 创建 PreparedStatement；
3. 交给 ParameterHandler 绑定；
4. 按命令类型执行 query/update；
5. 查询交给 ResultSetHandler；
6. 关闭 ResultSet 和 PreparedStatement。

不要在 Executor 中 commit、rollback 或 close Connection。

### 1.6 事务和 Session（依赖 Executor）

创建：

```text
src/main/java/com/frank/mybatis/transaction/Transaction.java
src/main/java/com/frank/mybatis/transaction/JdbcTransaction.java
src/main/java/com/frank/mybatis/session/SqlSession.java
src/main/java/com/frank/mybatis/session/DefaultSqlSession.java
src/main/java/com/frank/mybatis/session/SqlSessionFactory.java
src/main/java/com/frank/mybatis/session/DefaultSqlSessionFactory.java
```

`JdbcTransaction` 独占一个 Connection，提供 `getConnection`、`commit`、`rollback`、`close`。

`DefaultSqlSession` 负责：

- 根据 statement id 查语句；
- 委托 Executor；
- 提供 `selectOne`、`selectList`、`insert`、`update`、`delete`；
- 暴露 commit/rollback/close。

`DefaultSqlSessionFactory.openSession()` 每次获取新连接。Session 可以拥有 Mapper 代理，但不能跨线程共享。

### 1.7 Mapper 绑定和配置（依赖 Session、mapping）

创建：

```text
src/main/java/com/frank/mybatis/binding/MapperProxy.java
src/main/java/com/frank/mybatis/binding/MapperProxyFactory.java
src/main/java/com/frank/mybatis/binding/MapperRegistry.java
src/main/java/com/frank/mybatis/session/Configuration.java
src/main/java/com/frank/mybatis/builder/MapperAnnotationBuilder.java
```

职责：

- `MapperProxy`：实现 `InvocationHandler`，只负责把接口调用转成 statement id 和 Session 调用；
- `MapperProxyFactory`：创建指定 Mapper 接口的 JDK 代理；
- `MapperRegistry`：按 Mapper 类型注册和获取代理工厂；
- `Configuration`：保存 DataSource、`Map<String, MappedStatement>` 和 MapperRegistry；
- `MapperAnnotationBuilder`：扫描一个接口的方法，解析 SQL 注解并生成 id：

```text
Mapper 接口全限定名 + "." + 方法名
```

建议先不支持 Mapper 继承、default 方法和重载方法，遇到时明确抛错。

### 1.8 端到端测试（最后写）

创建：

```text
src/test/java/com/frank/mybatis/chapter01/UserMapper.java
src/test/java/com/frank/mybatis/chapter01/MiniMybatisChapter01Test.java
```

`UserMapper` 是测试业务接口，放 `src/test`，不是框架代码。测试至少覆盖：

- 注解扫描和 Mapper 注册；
- JDK 动态代理调用；
- insert、单查、列表、update、delete；
- `user_name → userName`；
- NULL 保持为 null；
- SQL 参数顺序与 Java 方法参数顺序不同；
- commit 后新 Session 可见；
- rollback 后新 Session 不可见；
- 多行 selectOne 报错；
- 重复注册、未注册方法、缺少参数报错；
- Session close 后拒绝操作。

## 2. 第 02 章：XML、注解与 statement id

第 01 章完成后再新增：

```text
src/main/java/com/frank/mybatis/builder/XMLConfigBuilder.java
src/main/java/com/frank/mybatis/builder/XMLMapperBuilder.java
src/main/java/com/frank/mybatis/builder/MapperAnnotationBuilder.java
src/main/java/com/frank/mybatis/mapping/Configuration.java
src/main/java/com/frank/mybatis/mapping/MappedStatement.java
```

XML 解析只放 `builder`；解析结果统一进入 `Configuration` 的 statement id Map；运行期代理不重新读取 XML。DOM 解析必须禁用 DOCTYPE 和外部实体。

## 3. 第 03 章：动态 SQL 与丰富参数

新增：

```text
src/main/java/com/frank/mybatis/scripting/SqlNode.java
src/main/java/com/frank/mybatis/scripting/IfSqlNode.java
src/main/java/com/frank/mybatis/scripting/WhereSqlNode.java
src/main/java/com/frank/mybatis/scripting/ForEachSqlNode.java
src/main/java/com/frank/mybatis/mapping/SqlSource.java
src/main/java/com/frank/mybatis/mapping/BoundSql.java
src/main/java/com/frank/mybatis/mapping/ParameterMapping.java
src/main/java/com/frank/mybatis/type/TypeHandler.java
src/main/java/com/frank/mybatis/type/TypeHandlerRegistry.java
```

动态 SQL 节点放 `scripting`，不要塞进 Executor；参数模型放 `mapping`，类型转换放 `type`；`${}` 必须白名单化，不能拼接用户输入。

## 4. 第 04 章：缓存、插件与嵌套映射

新增：

```text
src/main/java/com/frank/mybatis/cache/Cache.java
src/main/java/com/frank/mybatis/cache/PerpetualCache.java
src/main/java/com/frank/mybatis/cache/CacheKey.java
src/main/java/com/frank/mybatis/cache/TransactionalCache.java
src/main/java/com/frank/mybatis/plugin/Interceptor.java
src/main/java/com/frank/mybatis/plugin/Invocation.java
src/main/java/com/frank/mybatis/plugin/Plugin.java
src/main/java/com/frank/mybatis/mapping/ResultMap.java
src/main/java/com/frank/mybatis/mapping/ResultMapping.java
```

缓存只放 `cache`，拦截器只放 `plugin`，ResultMap 模型放 `mapping`。先做一级缓存，再做提交后的二级缓存；先完成一对一，再完成一对多和 JOIN 去重。

## 5. 第 05 章：类型、事务与生态

新增：

```text
src/main/java/com/frank/mybatis/type/JdbcType.java
src/main/java/com/frank/mybatis/type/EnumTypeHandler.java
src/main/java/com/frank/mybatis/type/LocalDateTimeTypeHandler.java
src/main/java/com/frank/mybatis/executor/BatchExecutor.java
src/main/java/com/frank/mybatis/transaction/TransactionFactory.java
src/main/java/com/frank/mybatis/transaction/JdbcTransactionFactory.java
```

Spring 适配单独放在：

```text
src/main/java/com/frank/mybatis/spring/
```

等核心 Session、Transaction 和 DataSource 边界稳定后再接入 Spring，不要一开始用 Spring Boot 自动配置替代自己的实现。

## 6. 每个检查点的命令

```bash
mvn clean test
mvn package
```

如果已经创建 `MiniMybatisApplication` 以外的 Demo，再执行：

```bash
mvn compile exec:java
```

空目录中的 `.gitkeep` 只是为了让 Git 保存目录，不是待实现类。