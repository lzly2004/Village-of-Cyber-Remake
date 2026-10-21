# 游客 HTTP API 原型（TASK-010）

本文只描述当前可调用的原型。首发全部核心操作、兼容与恢复约束见
[Web v1 契约草案](web-api-v1-draft.md)；其中明确标记的待实现端点目前不可调用。

使用 Java 21，在项目根目录运行：

```sh
./gradlew runWeb
# Windows: gradlew.bat runWeb
# 改端口: ./gradlew runWeb --args="7071"
```

默认地址为 `http://127.0.0.1:7070`，仅监听回环网卡。独立 `web` 源集锁定
`io.javalin:javalin:7.2.3`，按 [Javalin 官方文档](https://javalin.io/documentation)
使用配置期路由；桌面运行时和 Fat JAR 不包含 Javalin/Jetty。

CLI 允许省略端口（7070），或提供一个 1—65535 的十进制端口；非法参数打印用法并退出 2，
端口占用等启动失败打印监听检查提示并退出 1，不裸抛堆栈。程序化 `start(0)` 仅为测试分配临时端口保留。

此切片是供后续浏览器客户端调用的本机 API。每局拥有独立的随机源、事件窗口、
revision 和内存战绩实例，不访问桌面战绩 JSON。进程重启后对局丢失，暂不保存 Replay，
也没有账号与公网认证。会话 UUID 作为临时访问凭据，持有者可读取和操作该局。
当前已注册会话与正在初始化的预留合计最多 100 局，调用 DELETE 或停止服务释放；尚无超时清理。
初始化在全局注册锁外执行，容量检查/预留及注册交接保持原子；初始化失败归还名额。
close 是终止操作：关闭后拒绝新建，未完成初始化的候选局不得再入库，完成后关闭；同一实例不支持重启。

应用错误通过现有 DebugLogger 记录；目前未引入 SLF4J 日志提供者，启动时会出现
Javalin/SLF4J 的提示，框架内部日志使用 NOP。正式运行监控与框架日志接入仍待后续完善。

## 路由

所有请求体必须是 `application/json`，上限 16 KiB。响应设置 `Cache-Control: no-store`。
配役、职业、事件和阶段均使用 Java 枚举的名称字符串，玩家编号为局内正整数。

| 方法与路径 | 请求 | 成功响应 |
|---|---|---|
| POST `/api/v1/games` | `{"configuration":"jianyi"}` | 201，`{sessionId, update}`；Location 为该局读取路径 |
| GET `/api/v1/games/{id}?eventCursor=0` | 无 | 200，`GameUpdate`；省略游标时从 0 读取 |
| PUT `/api/v1/games/{id}/vote-target` | `{"expectedRevision":1,"target":3,"selected":true}` | 200，选择后的 `GameUpdate` |
| POST `/api/v1/games/{id}/votes` | `{"expectedRevision":2,"method":"DESIGNATED","candidates":[3]}` | 200，`{voteCompleted, update}` |
| DELETE `/api/v1/games/{id}` | 无 | 204，释放该局；随后读取返回 404 |

配役可取 `jianyi/tongchang/yaoohu/kuangxin/beide/maoyou/daxing`。
每个成功写命令使 revision 递增；读取与展示事件不递增。选择多名目标时逐个 PUT，
取消目标使用 `selected:false`。投票的 candidates 必须严格等于最新更新中
`legalVoteActions` 的 DESIGNATED 候补列表，并保留顺序。

投票被回避 CO 中断时，`voteCompleted:false`，revision 仍递增，产生的对话带
`AFTERNOON_DIALOGUE` 阶段。客户端先展示对话，再从最新合法动作重新提交。
公开共有者的回避遵循桌面规则，客户端可取消其目标后改选其他玩家。
首批 API 只开放指定投票及其目标选择。

## 更新契约

`GameUpdate` 直接复用应用层不可变安全快照，包含：

- `revision/phase/view`：状态版本、业务阶段、玩家公开状态与胜负；
- `selections/legalVoteActions/claimInquiry/legalTargetSelections`：当前选择与合法候选；
- `voteHistory/revealedRoles`：权威票型，以及仅在 FINISHED 阶段出现的真实身份揭晓；
- `eventCursor/nextEventCursor/events/presentedEvents`：半开窗口
  `[eventCursor,nextEventCursor)` 与同顺序的原始事件、展示阶段投影。

游标应使用上一次成功处理后的 `nextEventCursor`。重复读取同一游标可重复取得事件，
不会消耗其他客户端或桌面展示队列。命令响应只包含本次命令发布的事件窗口，
刷新恢复可 GET 当前状态并按已保存游标补读。
运行中响应不序列化核心上下文、真实职业、怀疑度、随机源或录像器。

## 错误响应

格式为 `{"code":"…","message":"…","currentRevision":null}`；
旧版本错误额外返回实际的 `currentRevision`。

| HTTP 状态 | 含义 |
|---|---|
| 400 / INVALID_REQUEST | JSON、配役、缺失字段、目标、候补或游标无效 |
| 404 / GAME_NOT_FOUND | 会话不存在或已释放 |
| 409 / STALE_REVISION | 版本已过期；先重新读取状态，再决定是否提交 |
| 409 / INVALID_STATE | 对局结束或关闭，命令不再合法 |
| 413 / HTTP_ERROR | 请求体超过 16 KiB |
| 415 / JSON_REQUIRED | 请求体的 Content-Type 不支持 |
| 503 / SESSION_LIMIT | 会话数量已满，需要释放旧会话 |
| 500 / INTERNAL_ERROR | 内部错误；详细原因仅写服务端日志 |

服务端不会自动重放写命令。相同版本的并发请求最多一个进入核心。

## 快速请求

```sh
curl -i -H 'Content-Type: application/json' \
  -d '{"configuration":"jianyi"}' http://127.0.0.1:7070/api/v1/games
# 从响应取 sessionId，再读取：
curl 'http://127.0.0.1:7070/api/v1/games/SESSION_ID?eventCursor=0'
```

自动验收运行 `./gradlew test --tests com.vocr.web.GuestGameServerTest`。
测试使用仅测试可见的种子注入，逐次比较七种配役 HTTP/直连完整对局的 JSON 更新、
CO 中断、投票结果与终局，并覆盖游客隔离、并发旧版本拒绝和容量释放。
生产 HTTP 不提供种子注入，也未更改 Replay 元数据。
