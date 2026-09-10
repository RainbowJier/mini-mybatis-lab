# mini-mybatis-lab

这是一个**从零手写 MyBatis** 的 Java 17 学习项目。目标不是马上做出一个生产级 ORM，而是沿着真实框架的调用链，一步一步把 JDBC、Mapper、动态代理、SQL 解析、结果映射、缓存和事务实现出来。

> 重要：目前项目只准备了 Maven、启动类和数据库表结构。MyBatis 核心代码、Mapper、业务 Demo 和测试，都需要你按 `docs/roadmap.md` 自己创建。

## 一、当前已经有什么

### 1. Maven 配置

文件：`pom.xml`

- Java 17
- UTF-8
- H2 2.3.232：运行时数据库
- JUnit Jupiter 5.10.2：后续测试使用
- `maven-compiler-plugin`
- `maven-surefire-plugin`
- `exec-maven-plugin`

### 2. 启动类

文件：`src/main/java/com/frank/mybatis/MiniMybatisApplication.java`

当前启动类只负责：

1. 创建 H2 内存数据库连接；
2. 读取 `src/main/resources/schema.sql`；
3. 创建 `t_user` 表；
4. 检查表是否创建成功；
5. 输出初始化结果。

它**不是 MyBatis 启动类**，目前不会创建 `Configuration`、`SqlSessionFactory` 或 Mapper。

### 3. 数据库表

文件：`src/main/resources/schema.sql`

```sql
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY,
    user_name VARCHAR(100) NOT NULL,
    age INTEGER
);
```

当前只保留表结构，没有 `data.sql`。测试数据由你在测试初始化或测试方法中自行插入。

## 二、如何导入和启动

### IDEA

1. 使用 IDEA 打开 `D:\Projects\mini-mybatis-lab`；
2. 识别并导入 `pom.xml`；
3. Project SDK 选择 JDK 17；
4. Maven Runner JRE 选择 JDK 17；
5. 等待 Maven 依赖下载完成。

### 初始化数据库

在项目根目录执行：

```bash
mvn compile exec:java
```

看到下面的输出就表示数据库初始化成功：

```text
mini-mybatis-lab started
database initialized: jdbc:h2:mem:mini_mybatis;DB_CLOSE_DELAY=-1
table initialized: T_USER
```

这是 H2 内存数据库。进程退出后，数据会销毁；`DB_CLOSE_DELAY=-1` 只保证同一个 JVM 存活期间数据库不因连接关闭而消失。

### 基础构建

```bash
mvn test       # 当前还没有测试，后续每完成一个阶段就执行
mvn package    # 编译并打包
```

## 三、先记住：目录和包是一一对应的

Java 文件的 `package` 必须和目录一致。例如：

```text
src/main/java/com/frank/mybatis/session/SqlSession.java
```

文件第一行必须是：

```java
package com.frank.mybatis.session;
```

不要把所有类都放在 `com.frank.mybatis` 根包，也不要把测试夹具和框架源码混在一起。

### 主代码包放什么

| 包 | 只放这些内容 | 不要放什么 |
| --- | --- | --- |
| `annotations` | `@Select`、`@Insert`、`@Update`、`@Delete`、`@Param` | 代理、SQL 执行代码 |
| `mapping` | `MappedStatement`、`BoundSql`、`ResultMap` 等数据模型 | 连接管理 |
| `builder` | XML/注解配置解析器 | JDBC 执行 |
| `binding` | `MapperRegistry`、`MapperProxy`、方法绑定 | 结果集映射 |
| `session` | `Configuration`、`SqlSession`、`SqlSessionFactory` | 具体 SQL 执行细节 |
| `executor` | `Executor`、StatementHandler、参数/结果处理 | Mapper 注解定义 |
| `type` | `TypeHandler`、`JdbcType`、类型注册表 | 事务提交 |
| `transaction` | `Transaction`、JDBC 事务实现 | SQL 模板解析 |
| `cache` | `Cache`、`CacheKey`、一级/二级缓存 | Mapper 代理 |
| `plugin` | `Interceptor`、`Invocation`、插件代理 | 数据库表模型 |

### 测试代码放什么

测试相关类统一放在：

```text
src/test/java/com/frank/mybatis/
```

推荐的测试包：

```text
com.frank.mybatis.fixture     # User、Blog 等测试实体
com.frank.mybatis.support     # H2DatabaseSupport 等测试工具
com.frank.mybatis.chapter01   # 第一章测试和 UserMapper
```

测试实体 `User` 放 `fixture`，不要放进主代码的 `mapping` 包；`mapping` 是框架代码，不是业务实体包。

## 四、第 01 章：严格按这个顺序创建文件

不要一次创建几十个空类。每完成一个检查点，就先运行测试，再进入下一个检查点。

### Checkpoint 0：数据库测试工具

先创建：

```text
src/test/java/com/frank/mybatis/support/H2DatabaseSupport.java
```

包名：

```java
package com.frank.mybatis.support;
```

职责：

- 创建唯一名称的 H2 内存数据库；
- 获取 `JdbcDataSource`；
- 执行 `schema.sql`；
- 交给测试一个可用的 `DataSource`。

这一层只负责测试环境，不属于 MyBatis 框架。先写一个小测试，确认 `t_user` 可以创建和查询。

### Checkpoint 1：原生 JDBC 基线

创建：

```text
src/test/java/com/frank/mybatis/fixture/User.java
src/test/java/com/frank/mybatis/chapter01/JdbcBaselineTest.java
```

`User` 放在：

```java
package com.frank.mybatis.fixture;
```

至少包含：

```java
private Long id;
private String userName;
private Integer age;
```

以及 public 无参构造器、getter 和 setter。

`JdbcBaselineTest` 放在：

```java
package com.frank.mybatis.chapter01;
```

先不用任何自定义框架，直接完成：

1. `Connection`；
2. `PreparedStatement`；
3. `setLong`、`setString`、`setObject`；
4. `executeUpdate`；
5. `executeQuery`；
6. `ResultSet` 遍历；
7. `try-with-resources` 关闭资源；
8. commit 和 rollback。

这一步的目的是确认你理解后面要封装的重复代码。

### Checkpoint 2：SQL 注解

创建在：

```text
src/main/java/com/frank/mybatis/annotations/Select.java
src/main/java/com/frank/mybatis/annotations/Insert.java
src/main/java/com/frank/mybatis/annotations/Update.java
src/main/java/com/frank/mybatis/annotations/Delete.java
src/main/java/com/frank/mybatis/annotations/Param.java
```

包名统一是：

```java
package com.frank.mybatis.annotations;
```

规则：

- 四个 SQL 注解使用 `@Target(ElementType.METHOD)`；
- `@Param` 使用 `@Target(ElementType.PARAMETER)`；
- 全部使用 `@Retention(RetentionPolicy.RUNTIME)`；
- 每个 SQL 注解提供 `String value()`；
- `Param` 也提供 `String value()`。

此时不要写动态代理，不要写 XML，不要把注解放进 `mapping` 包。

### Checkpoint 3：最小映射模型

创建在：

```text
src/main/java/com/frank/mybatis/mapping/SqlCommandType.java
src/main/java/com/frank/mybatis/mapping/MappedStatement.java
src/main/java/com/frank/mybatis/mapping/PreparedSql.java
```

包名：

```java
package com.frank.mybatis.mapping;
```

职责：

- `SqlCommandType`：`SELECT`、`INSERT`、`UPDATE`、`DELETE`；
- `PreparedSql`：保存已经把 `#{id}` 转成 `?` 的 SQL 和参数名顺序；
- `MappedStatement`：保存 statement id、原始 SQL、命令类型、返回类型和 `PreparedSql`。

建议使用不可变类（`record` 或 final 字段），因为配置完成后这些元数据会被多个 Session 只读使用。

### Checkpoint 4：固定 SQL 与参数处理

创建在：

```text
src/main/java/com/frank/mybatis/executor/ParameterHandler.java
src/main/java/com/frank/mybatis/mapping/SqlTemplateParser.java
```

包名分别是：

```java
package com.frank.mybatis.executor;
package com.frank.mybatis.mapping;
```

`SqlTemplateParser` 只做一件事：

```text
select ... where id = #{id}
↓
select ... where id = ?
```

同时保存：

```text
[id]
```

`ParameterHandler` 再根据 `@Param("id")` 找到真正的参数值，并按 SQL 中出现的顺序绑定到 `PreparedStatement`。

第 01 章只支持简单 `#{name}`，不要在这里实现：

- `${}`；
- `<if>`；
- `<foreach>`；
- OGNL；
- XML。

这些属于后续章节。

### Checkpoint 5：结果集处理和 Executor

创建在：

```text
src/main/java/com/frank/mybatis/executor/ResultSetHandler.java
src/main/java/com/frank/mybatis/executor/Executor.java
src/main/java/com/frank/mybatis/executor/SimpleExecutor.java
```

包名：

```java
package com.frank.mybatis.executor;
```

职责分工：

- `ResultSetHandler`：把 `ResultSet` 转成 POJO 或 `List<POJO>`；
- `Executor`：定义 query/update 接口；
- `SimpleExecutor`：创建 `PreparedStatement`、调用参数处理器、执行 SQL、调用结果处理器。

资源边界必须保持：

```text
SimpleExecutor 关闭 ResultSet 和 PreparedStatement
SqlSession      关闭 Connection
SqlSession      决定 commit / rollback
```

Executor 不要提交事务，也不要关闭不属于它的 Connection。

### Checkpoint 6：事务和 Session

创建在：

```text
src/main/java/com/frank/mybatis/transaction/Transaction.java
src/main/java/com/frank/mybatis/transaction/JdbcTransaction.java
src/main/java/com/frank/mybatis/session/SqlSession.java
src/main/java/com/frank/mybatis/session/DefaultSqlSession.java
src/main/java/com/frank/mybatis/session/SqlSessionFactory.java
src/main/java/com/frank/mybatis/session/DefaultSqlSessionFactory.java
```

包名分别是：

```java
package com.frank.mybatis.transaction;
package com.frank.mybatis.session;
```

`Transaction` 管连接和事务动作；`SqlSession` 对外提供：

- `selectOne`；
- `selectList`；
- `insert`；
- `update`；
- `delete`；
- `commit`；
- `rollback`；
- `close`。

每次 `openSession()` 都要拿到独立 Connection。不要让全局 Configuration 持有 Connection，也不要让多个 Session 共享事务状态。

### Checkpoint 7：Mapper 注册和动态代理

创建在：

```text
src/main/java/com/frank/mybatis/binding/MapperProxy.java
src/main/java/com/frank/mybatis/binding/MapperProxyFactory.java
src/main/java/com/frank/mybatis/binding/MapperRegistry.java
src/main/java/com/frank/mybatis/session/Configuration.java
src/main/java/com/frank/mybatis/builder/MapperAnnotationBuilder.java
```

包名分别是：

```java
package com.frank.mybatis.binding;
package com.frank.mybatis.session;
package com.frank.mybatis.builder;
```

调用链应该变成：

```text
UserMapper.findById(1L)
→ MapperProxy.invoke
→ 根据接口全限定名 + 方法名生成 statement id
→ Configuration 找 MappedStatement
→ SqlSession 执行
→ Executor 执行 JDBC
```

这里才创建测试 Mapper：

```text
src/test/java/com/frank/mybatis/chapter01/UserMapper.java
```

包名：

```java
package com.frank.mybatis.chapter01;
```

`UserMapper` 是测试用的业务接口，不要放入 `src/main/java` 的 `binding` 包。

### Checkpoint 8：端到端测试

最后创建：

```text
src/test/java/com/frank/mybatis/chapter01/MiniMybatisChapter01Test.java
```

至少验证：

- 注解 Mapper 可以注册；
- 动态代理可以获得；
- INSERT 可以执行；
- SELECT 单对象可以映射；
- SELECT 列表可以映射；
- `user_name` 可以映射到 `userName`；
- SQL NULL 可以保留为 `null`；
- UPDATE / DELETE 返回影响行数；
- 参数顺序与 SQL 占位符顺序不同仍然正确；
- commit 后新 Session 可见；
- rollback 后新 Session 不可见；
- Session close 会释放连接；
- 关闭后的 Session 不允许继续执行。

## 五、后续章节文件规划

### 第 02 章：XML、注解与 statement id

新增或调整：

```text
src/main/java/com/frank/mybatis/builder/XMLConfigBuilder.java
src/main/java/com/frank/mybatis/builder/XMLMapperBuilder.java
src/main/java/com/frank/mybatis/mapping/Configuration.java
src/main/java/com/frank/mybatis/mapping/MappedStatement.java
src/main/java/com/frank/mybatis/binding/MapperMethod.java
```

重点是把 `Method` Key 改成：

```text
com.example.UserMapper.findById
```

并让 XML 和注解最终都注册为同一种 `MappedStatement`。

### 第 03 章：动态 SQL 与丰富参数

新增：

```text
src/main/java/com/frank/mybatis/mapping/SqlSource.java
src/main/java/com/frank/mybatis/mapping/StaticSqlSource.java
src/main/java/com/frank/mybatis/mapping/DynamicSqlSource.java
src/main/java/com/frank/mybatis/mapping/BoundSql.java
src/main/java/com/frank/mybatis/mapping/ParameterMapping.java
src/main/java/com/frank/mybatis/scripting/SqlNode.java
src/main/java/com/frank/mybatis/scripting/IfSqlNode.java
src/main/java/com/frank/mybatis/scripting/WhereSqlNode.java
src/main/java/com/frank/mybatis/scripting/ForEachSqlNode.java
src/main/java/com/frank/mybatis/type/TypeHandler.java
src/main/java/com/frank/mybatis/type/TypeHandlerRegistry.java
```

需要新增 `scripting` 包；它负责 SQL 节点树，不要把动态 SQL 代码塞进 `executor`。

### 第 04 章：缓存、插件与嵌套映射

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

缓存放 `cache`，拦截器放 `plugin`，结果映射模型放 `mapping`，不要混在 `session` 中。

### 第 05 章：类型、事务与生态

新增：

```text
src/main/java/com/frank/mybatis/type/JdbcType.java
src/main/java/com/frank/mybatis/type/EnumTypeHandler.java
src/main/java/com/frank/mybatis/type/LocalDateTimeTypeHandler.java
src/main/java/com/frank/mybatis/executor/BatchExecutor.java
src/main/java/com/frank/mybatis/transaction/TransactionFactory.java
src/main/java/com/frank/mybatis/transaction/JdbcTransactionFactory.java
```

Spring 适配代码建议单独放：

```text
src/main/java/com/frank/mybatis/spring/
```

但要等核心事务接口稳定后再添加，不要一开始把 Spring Boot 自动配置混进核心包。

## 六、常见放错位置的问题

### 把 User 放进 mapping

错误：

```text
src/main/java/com/frank/mybatis/mapping/User.java
```

正确：

```text
src/test/java/com/frank/mybatis/fixture/User.java
```

`User` 是测试业务对象，不是框架元数据。

### 把 MapperProxy 放进 executor

错误：

```text
com.frank.mybatis.executor.MapperProxy
```

正确：

```text
com.frank.mybatis.binding.MapperProxy
```

代理负责方法调用路由，Executor 负责 JDBC 执行，两者边界不能混。

### 让 Executor 提交事务

错误：

```text
executor.execute(...); // 内部 commit
```

正确：

```text
mapper.update(...);
session.commit();
```

Executor 不决定业务事务边界。

### 把 XML 解析放进 MapperProxy

错误：每次调用 Mapper 方法时读取 XML 文件。

正确：启动或配置阶段由 `builder` 解析 XML，生成 `MappedStatement`；运行期 MapperProxy 只根据 statement id 查元数据。

### 把测试类放进 main

错误：

```text
src/main/java/.../UserMapper.java
```

正确：

```text
src/test/java/.../UserMapper.java
```

除非某个接口本身就是框架公开 API，否则练习用的实体、Mapper、Demo 和测试都放 `src/test`。

## 七、每完成一个阶段都执行

```bash
mvn clean test
mvn package
```

如果你添加了启动 Demo，再执行：

```bash
mvn compile exec:java
```

构建成功只说明 Java 代码能编译；真正的阶段完成标准是对应测试覆盖了该阶段的行为和失败边界。