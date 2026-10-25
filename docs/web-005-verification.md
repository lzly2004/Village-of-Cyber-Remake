# WEB-005：启动、注册锁与 CO 中断验收

实施基线：2da1254；本轮只执行已批准 WEB-005，不提前接入新的游戏操作端点。

## 重复 review 处置

用户再次提供 e3d0a5e / f979eca / 8f290ea 的相同报告，并未新增针对 2da1254 的审查结论。

| 事项 | 核验依据 | 当前结论与归属 |
|---|---|---|
| C1：声称未知请求字段被忽略 | 2da1254 已用真实 HTTP 证明创建/写入均拒绝未知字段且无副作用，本轮保留并复跑此测试 | 排除；不按重复报告反向修改正确配置 |
| C3：null / NONE | v1 草案已有显式区别，序列化专项已有实际 JSON 断言 | 已完成，不重复改动 |
| C4：末轮重试风险 | CORE-TIE-001 仍为 PROPOSED，未获得规则变更批准 | 保持候选，不改核心/随机消费；49 步不重排 |
| skill 停用与其余已验证声明 | 既有核验记录与现行审查规范均一致 | 维持结论；不将本轮说成已收到对 WEB-004 的新 review |

## 实现

- CLI：省略端口为 7070；只接收一个十进制 1—65535 端口。参数错误给用法、进程退出 2；监听失败提示检查端口/权限、清理服务并退出 1；程序化 start(0) 保留。没有生产依赖变更。
- 注册：全局锁内检查 closed 与 `sessions.size() + pendingCreations`、预留名额；UUID、factory、startGame 均在锁外。成功注册与撤销预留在同一临界区完成，不出现注册/预留双计数空窗。
- 失败：factory 或 startGame 抛出 RuntimeException/Error 时归还预留，已获得的候选会话关闭。关闭服务与初始化竞争时，候选局不得事后入库，必须关闭；close 清空注册表并幂等拒绝后续创建，同一服务器对象不重启。
- 释放与关服：只在全局锁内摘除/复制会话；GameSession.close 在全局锁外执行，避免等待某局锁时阻塞其他会话注册查找。单局操作依然由 GameSession 自身串行处理，未改变游戏规则。
- HTTP 回归：七配役固定种子原策略保留，显式统计中断；通常村 4202 必须至少一次 voteCompleted=false，中断业务阶段/日期/AFTERNOON_DIALOGUE 均断言，保留每次响应与重复 GET 的窗口比较及序列化安全门禁。
- 浏览器：新增仅在 test 源集中存在的固定种子启动器与 Gradle 验证任务，Playwright 连接真实 HTTP/核心；不模拟 voteCompleted 响应、不向生产开放种子，不改变生产随机行为。显式验证中断提示、同窗 GET、刷新及事件展示前后操作许可。

## 测试设计与边界

1. CountDownLatch 阻塞第二个 factory，保留一个已注册局：阻塞期间 GET/PUT/DELETE 均必须在请求超时前完成；预留占满时新建 503；释放旧局后替代创建成功；恢复慢创建后仍不超限。断言顺序由 latch 控制，不用 sleep 猜测时序。
2. factory 内部错误（500）与候选已关闭导致 startGame 拒绝（409）分别验证名额回收，随后容量为 1 的合法创建仍成功。
3. 通过反射调用私有 create、在未启动 Jetty 的实例上构造关闭竞争，避免 graceful stop 等待网络请求掩盖注册竞态；验证候选 CLOSED、后续创建/启动拒绝及重复关闭。此项是注册层生命周期测试，不谎称覆盖所有网络停机时序。
4. CLI 测试实际占用回环端口，调用与 main 共用的 run，断言返回码；捕获诊断验证用法/占用提示及无异常堆栈。main 仅将非零状态转成进程退出；未以此声称运行过所有 OS 权限失败场景。
5. 关闭标记阻止后续入库，不强制中断正在构造的线程；若核心初始化永久不返回，预留会一直占用到进程退出。初始化时间上限、TTL、完整负载和日志脱敏仍归 WEB-039/041/044/047，未在本轮扩大范围。

## 最终验证

环境沿用 Java 21、Gradle 8.8、Node 22、临时 Chromium 与 README 中 Ubuntu 平台覆盖配置。

- 定向 `gradle cleanTest test --tests com.vocr.web.GuestGameServerTest --tests com.vocr.web.GuestGameServerLifecycleTest --console=plain` 通过；最终完整执行 `gradle clean check verifyRuntimeAssets fatJar --console=plain`，438 项测试零失败，包括新增生命周期 5 项、HTTP 11 项、序列化 13 项及七配役黄金轨迹。未刷新黄金摘要，560 条概率和运行时资源校验通过。
- 最终生命周期用例将 factory 内部失败明确为 RuntimeException/500，startGame 状态拒绝为 409，两者均释放容量；上述最终完整 clean 已包含该测试版本。
- `npm run test:e2e`：8 项真实浏览器测试通过（1.7m），其中固定种子 CO 中断用例实际通过；通过 VOCR_WEB_SERVER_COMMAND 指向 `gradle runWebTestServer --args=17071 --console=plain`。没有对生产投票结果做浏览器拦截/伪造。
- Fat JAR SHA-256 `9d1c93f0d1b0bb0114333874728cd2b5364bd6c7e0f0e50314c001144fd39f2d`，与先前桌面产物一致；实际检查归档无 Web/测试启动器、Javalin/Jetty，web 编译输出也无 Seeded 启动器。生产 Web 类路径仍只消费 main/web 输出。
- 差异空白、文档链接和 49 步顺序/状态检查通过；未提交用户原有 Game.java 空白差异、批量换行或其他未跟踪文件。完整验证使用当前工作区，非假称隔离的历史快照。
