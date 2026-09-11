# AGENTS.md — mini-mybatis-lab

## 项目定位

从零手写 MyBatis 的 Java 17 学习项目（非生产级 ORM），按章节逐个检查点推进。改代码前先读：

- `docs/roadmap.md` — 章节施工顺序、每章创建哪些文件、验收标准
- `README.md` — 包结构规则、常见放错位置的反例、调用链设计

## 常用命令

```bash
mvn test                          # 全部测试（每完成一个检查点必须跑）
mvn test -Dtest=ExecutorContractTest_04   # 只跑单个测试类
mvn clean test && mvn package     # 完整验证
mvn compile exec:java             # 运行 MiniMybatisApplication：初始化 H2 内存库 t_user
```

Java 17、UTF-8；surefire 配置了 `failIfNoTests=false`。构建通过只是底线，章节完成的真正标准是对应测试覆盖行为边界。

## 架构与包边界

| 包 | 只放 | 不要放 |
| --- | --- | --- |
| `annotations` | `@Select/@Insert/@Update/@Delete/@Param` 纯注解 | 执行逻辑、代理 |
| `mapping` | `MappedStatement`、`PreparedSql`、`SqlCommandType`、`SqlTemplateParser` 等不可变元数据与 SQL 解析 | 连接管理、JDBC 执行 |
| `executor` | `Executor`、`SimpleExecutor`、`ParameterHandler`（静态工具）、`ResultSetHandler` | 代理、事务提交 |
| `fixture` / `support` | 测试实体 `TUser`、`H2DatabaseSupport`（当前在 src/main，见下方差异说明） | 框架代码 |
| 后续章节新增 | `binding`(动态代理)、`session`、`builder`、`transaction`、`type`、`cache`、`plugin`、`scripting` | 不要塞进已有包 |

资源所有权（必须保持）：

- `SimpleExecutor` 关闭 ResultSet / PreparedStatement，**绝不 commit**，不拥有 Connection 生命周期
- `SqlSession` 负责获取/关闭 Connection 和 commit/rollback；每次 `openSession()` 拿独立连接
- `SqlTemplateParser` 只支持简单 `#{name}` 并**显式拒绝 `${}`**；`${}`、`<if>`、`<foreach>`、OGNL、XML 属于后续章节，不要提前实现

## 代码约定

- 实体用 Lombok（`@Data` / `@NoArgsConstructor` / `@AllArgsConstructor`，见 `TUser`）
- 映射元数据用不可变类型（record 或 final 字段），配置完成后被多个 Session 只读共享
- 异常统一非受检：`IllegalArgumentException` / `IllegalStateException` 包装 `SQLException`
- 测试类命名 `<主题>Test_NN`（NN = 章节号，如 `JdbcBaselineTest_01`），目前都放在 `com.frank.mybatis` 测试根包
- 提交信息用中文 conventional commit，如 `feat(executor): 实现...`

## 已知坑

- **README/roadmap 与实际代码有偏差**：文档建议 fixture/support 放 `src/test`、测试放 `chapterNN` 包；实际代码把它们放在 `src/main/java/com/frank/mybatis/fixture|support`，测试放在根包。新代码**跟随现有实际布局**，不要机械照搬文档目录。
- `schema.sql` 在 `src/main/resources` 和 `src/test/resources` 各有一份（当前内容相同）；`H2DatabaseSupport` 从 classpath 读取，测试运行时 test 版本优先生效——改表结构两份都要同步。
- H2 是内存库且每次测试用随机库名（`DB_CLOSE_DELAY=-1`），没有 data.sql；测试数据在测试内自行插入。
- 仓库根目录有个未跟踪的 `nul` 文件（Windows 保留名导致的意外产物），是垃圾文件，忽略即可，勿引用。
- 开发环境为 Windows + Git Bash。
