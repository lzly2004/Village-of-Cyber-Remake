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
| POST `/api/v1/games/{id}/claims/by-role` | `{"expectedRevision":1,"role":"zhan"}` | 200，询问后的 `GameUpdate` |
| POST `/api/v1/games/{id}/claims/by-players` | `{"expectedRevision":1,"players":[9,1]}` | 200，询问后的 `GameUpdate` |
| PUT `/api/v1/games/{id}/avoidance` | `{"expectedRevision":1,"enabled":false}` | 200，设置后的 `GameUpdate` |
| DELETE `/api/v1/games/{id}` | 无 | 204，释放该局；随后读取返回 404 |

配役可取 `jianyi/tongchang/yaoohu/kuangxin/beide/maoyou/daxing`。
每个成功写命令使 revision 递增；读取与展示事件不递增。选择多名目标时逐个 PUT，
取消目标使用 `selected:false`。投票的 candidates 必须严格等于最新更新中
`legalVoteActions` 中所选 method 的候补列表，并保留顺序。支持 `DESIGNATED/FREE/GREY`；
自由投票并非任意自选候补，灰区也不得由客户端重新计算。灰区候补耗尽时没有 GREY 合法动作，
客户端隐藏对应提交入口；服务端拒绝不可用方式、候补子集/乱序/死亡目标，失败不改变状态。

投票被回避 CO 中断时，`voteCompleted:false`，revision 仍递增，产生的对话带
`AFTERNOON_DIALOGUE` 阶段。客户端先展示对话，再从最新合法动作重新提交。
公开共有者的回避遵循桌面规则，客户端可取消其目标后改选其他玩家。
WEB-009 已开放自由/灰区方式，复用既有投票命令与 revision/游标/异常恢复，不增加新路由或协议字段。
网页三种方式均在事件读完、状态确定且无在途请求时提交；冲突只同步，失响应先同步而不自动重放投票。
普通指定回避的开关语义不扩展到自由/灰区投票，实际执行完全沿用核心规则。
WEB-006 已开放按职业询问 CO：role 必须取自最新
`claimInquiry.roles`（运行中为 zhan/ling/lie/gong/mao），询问成功 revision 加 1，
即使没有新发言也返回权威更新；新事件沿用应用层展示阶段。缺失/null/未知或不可询问的职业返回 400，
旧版本返回 409 且不重放，终局拒绝询问。浏览器读完待确认事件后才允许发起询问；
刷新按保存游标恢复公开 CO 状态与未读发言，不持久化本地“已询问职业”镜像。

WEB-007 已开放按玩家询问：players 必须非空、无重复/无 null，全部来自最新 `claimInquiry.players`，
请求顺序交由既有应用命令处理，不在 HTTP 层重排。一次单选或批量询问只使 revision 加 1；
非法/死亡/越界目标返回 400，旧版本与终局返回对应 409，失败前置校验不改变状态。
网页多选是尚未发送的临时草稿，不修改投票目标、不进入 sessionStorage；取消勾选不发请求。
新权威快照、同步或刷新会清理草稿；已执行询问的公开状态与未读事件仍按游标恢复，绝不自动重新询问。

WEB-008 已开放回避设置：enabled 表示设为开启/关闭，不是 toggle；新局默认 true，
每个合法设置（含设置同值）revision 加 1，不产生新事件，值由 selections.avoidanceEnabled 权威返回。
浏览器等待服务响应后才改变显示，刷新从服务恢复，不在本地另存偏好；409 不重放，失响应时先同步。
该开关按既有核心规则影响指定投票的普通回避 CO，关闭后共有者仍可触发回避，并非禁用一切 CO。
终局拒绝设置，其他错误沿用既有错误表；未改变核心规则或开关作用范围。

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
