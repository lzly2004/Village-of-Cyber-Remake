# Web v1 核心操作契约草案（WEB-003）

本文件冻结首发核心操作的接口方向、既有字段语义、兼容及恢复规则；**不是已实现 API 清单**。当前可调用接口仍见 [游客 HTTP 原型](guest-http-api.md)。以 2038d98 时的 GuestGameServer、GameSession、GameApplication 与不可变 record 为实现基线，不改本轮代码。

## 1. 范围与兼容规则

- 基础路径 `/api/v1/games`；同源 JSON API，成功 JSON 为 UTF-8，响应 `Cache-Control: no-store`。当前服务仅绑定回环；公网 HTTPS、同源托管和安全限额由 WEB-039/041/043 完成。
- 创建返回的 UUID 是本局访问凭据，不是账号；不放入分析系统、业务日志、错误链或 Replay 文件。路径、Location、请求/响应体也在脱敏范围，不能只遮请求头。
- 请求体限 16 KiB；有请求体的路由使用 `application/json`（允许 charset 参数）；GET/DELETE 不需 JSON 请求体或 Content-Type。生产接口不接受种子、核心上下文或实际身份注入。
- 请求端使用 JSON 原生整数/布尔值、非 null 必填字段、精确大小写枚举；不得依赖 Jackson 可能存在的标量强制转换。额外请求字段不属于扩展渠道，当前 Jackson 2.17.1 解析器默认启用 FAIL_ON_UNKNOWN_PROPERTIES，会拒绝未知字段；WEB-004 已用真实 HTTP 创建/写入请求锁定此行为，不需修改默认配置。
- 同一 v1 只允许新增端点/可选响应字段，且不得改变既有字段含义、枚举既有成员、排序、错误恢复类别或默认值。旧客户端忽略未知响应字段；新增可选字段缺失时保持原功能，不能假装已有新能力。
- 新枚举值会影响既有消费者，不能一概按“增量”放行；须有兼容兜底和消费者测试，否则用 v2。删除/重命名字段、改变事件窗口语义或扩大原有错误含义同理。
- 草案端点首次落地时可在其任务内定稿并同步文档；已实现端点不得借“草案”名义破坏兼容。尚待信息审计的字段不伪装成已冻结白名单。

WEB-014例外：用户明确批准AUDIT-SEC-001，在v1正式上线前直接修订`deathReason`类型与语义，不保留泄漏旧字段。旧原型消费者须按下述阶段分类读取；当前Vue未使用玩家deathReason字段，旧客户端可忽略新增publicInformation；WEB-015已接入其公开报告、怀疑摘要与死亡历史，缺投影明确显示未支持。该例外不授权后续任意破坏兼容。

## 2. 核心操作目录

路径中的 `{id}` 为规范小写随机UUID v4本局凭据；未知/畸形/过期/释放统一404 GAME_NOT_FOUND，详见[生命周期](web-guest-lifecycle.md)。`R` 表示 `expectedRevision`（非负整数）；所有写命令都从最近权威 update 取 R，不使用本地自增猜测。以下 JSON 形状是字段说明，不是可复制的带占位符 JSON。

| 方法 / 相对路径 | 请求字段 | 成功体与状态 | 状态/对应应用命令 | 任务 |
|---|---|---|---|---|
| POST 空路径 | configuration | 201 `{sessionId, update}`，Location 指向读取地址 | 已实现；StartGameCommand 的初始 revision 由服务端置 0 | 已有 |
| GET `/{id}?eventCursor=C` | 无；省略 C 默认为 0 | 200 GameUpdate | 已实现；currentUpdate(C) | 已有 |
| PUT `/{id}/vote-target` | expectedRevision, target, selected | 200 GameUpdate | 已实现；SetAllTargetSelectionCommand，type 固定 VOTE | 已有 |
| POST `/{id}/votes` | expectedRevision, method, candidates | 200 `{voteCompleted, update}` | DESIGNATED/FREE/GREY 已实现；ExecuteVoteCommand | 009 |
| DELETE `/{id}` | 无 | 204，无响应体 | 已实现；移除并 close，无 revision；重复删除返回 404 | 已有 |
| POST `/{id}/claims/by-role` | expectedRevision, role | 200 GameUpdate | 已实现；AskClaimByRoleCommand | 006 |
| POST `/{id}/claims/by-players` | expectedRevision, players | 200 GameUpdate | 已实现；AskClaimByPlayersCommand | 007 |
| PUT `/{id}/avoidance` | expectedRevision, enabled | 200 GameUpdate | 已实现；SetAvoidanceCommand | 008 |
| PUT `/{id}/target-selections` | expectedRevision, type, target, selected | 200 GameUpdate | DIVINATION/GUARD 已实现；SetAllTargetSelectionCommand；VOTE 保持原路径 | 010、011 |
| PUT `/{id}/claimed-target-selections` | expectedRevision, type, claimedPlayer, target, selected | 200 GameUpdate | 已实现；SetClaimedRoleTargetSelectionCommand，type 只开放 DIVINATION/GUARD | 012 |

### 参数与动作约束

- configuration：`jianyi/tongchang/yaoohu/kuangxin/beide/maoyou/daxing`；NONE、缺失、null 或未知值无效。玩家编号为局内从 1 开始的正整数，不是角色枚举序号。
- role：只提交最新 `claimInquiry.roles` 中的成员；当前运行中投影固定为 zhan/ling/lie/gong/mao，终局为空，不能自行按配役删改。players：非空、无重复/无 null，全部来自 `claimInquiry.players`；保留请求顺序交给应用层，不由 HTTP 层重新排序。
- selected/enabled 为布尔值；false 分别表示取消目标或关闭回避。选择命令是设值而非 toggle；重复设置同值也按当前应用行为成功递增 revision，不能因此自动重试旧版本。
- target 必须来自对应 `legalTargetSelections[type].targets`；claimedPlayer 还须来自该类型的 claimedActors。不能凭真实身份筛出“实际技能持有者”；全体技能目标由应用层按既有规则应用，不由客户端分发多条个人命令模拟。
- method：`DESIGNATED/FREE/GREY`；candidates 必须与最新 `legalVoteActions` 中该 method 的列表**逐项同序相等**，非空且无重复。客户端不能自行重算“灰区”，也不能把自由投票理解为任意手选候补。WEB-009 已开放全部三种方式；灰区候补耗尽则无 GREY 动作，不得提交空列表或用 FREE 候补冒充。
- 所有游戏命令仅作用于 RUNNING 局；终局没有可提交动作，但仍允许读取。`phase` 是业务快照阶段，不代表客户端已读完事件，不能将 `DAY_DECISION` 当作跳过事件展示的许可。

## 3. 更新与公开数据白名单

以下是既有序列化形状；字段名取 record 分量，不把 Java 便捷方法误列为 JSON 属性。数组不返回 null；明确可空的角色事件参数例外。枚举使用名称字符串，不使用 ordinal。

| 对象 | 字段 / 形状 | 约束 |
|---|---|---|
| GameUpdate | revision, phase, view, selections, legalVoteActions, claimInquiry, legalTargetSelections, voteHistory, revealedRoles, publicInformation, eventCursor, nextEventCursor, events, presentedEvents | phase 为 DAY_DECISION 或 FINISHED |
| view | configuration, gameDay, aliveCount, deathCount, result, players | 计数和人数一致；游戏日期以权威投影为准，不从事件游标推算 |
| view.players[] | playerNumber, character, alive, deathReason, deathDay, claimedRole, claimedRoleOrder, comingOutDay | character 是外观标识，claimedRole 仅为公开声称，不是实际身份 |
| selections | avoidanceEnabled, voteTargets, divination, guard | 当前日权威状态，客户端不维护跨日镜像 |
| divination / guard | hiddenTargets, claimedActors | hiddenTargets 是匿名潜伏预约目标，不包含潜伏行动者身份 |
| selections 的 claimedActors[] | playerNumber, claimedRoleOrder, targets | targets 是该公开 CO 行动者的预约 |
| legalVoteActions[] | method, candidates | 仅列当前可提交方式，终局为空 |
| claimInquiry | roles, players | 终局两数组均为空；available() 不是约定 JSON 字段 |
| legalTargetSelections[] | type, targets, claimedActors | claimedActors[] 为 `{playerNumber, claimedRoleOrder}`，不是选择状态中的 targets 对象 |
| voteHistory[] | day, method, candidates, rounds | 按日递增，每日一条完整已完成投票；CO 中断不伪造完成票型 |
| rounds[] | round, lines, maximumVotes, leaders | 轮次从 1 连续递增；非最终轮为平票，最终轮唯一领先者 |
| lines[] | playerNumber, receivedVotes, targetPlayerNumber | 全部沿用权威公开票型，不由浏览器另算替代 |
| revealedRoles[] | playerNumber, actualRole | DAY_DECISION 必须为空；FINISHED 与 players 数量、编号顺序一一对应 |
| events[] | eventname, ch1, ch2, ch3 | ch1/ch2/ch3 可为 null 或枚举字符串 "NONE"，见下文；不得通过 toString() 解析角色或事件 |
| presentedEvents[] | event, stage | 与 events 等长同序，event 内容相等；展示采用服务端 stage |

stage 为 `NIGHT_RESULT/DAY_DIALOGUE/AFTERNOON_DIALOGUE/EXECUTION/SETTLEMENT`。业务可能已处于 FINISHED 而客户端仍需按顺序展示前置事件，不应只看到结算快照就吞掉未读对话。角色/结果/死亡原因/外观的全部现有枚举名称以对应源码为准；日文展示目录由 WEB-018 建立，不修改 wire 名称实现翻译。

事件角色参数的 JSON `null` 与字符串 `"NONE"` 不相等：前者是未提供参数（Event 简写构造器将省略参数置 null），后者是显式的 CharacterEnglishName.NONE 枚举占位，均不对应可绘制的具体角色。客户端展示应同时处理两者；不能擅自将 wire 值互相改写，也不从此区别推断新的游戏规则。WEB-004 已验证两者的实际序列化区别。

### WEB-014公开信息与死因值级契约

逐字段来源见[公开信息审计](web-public-information-audit.md)。`view.players[].deathReason`不再采用核心whyDie类型，而是PublicDeathReason：

- DAY_DECISION：仅`NONE / EXECUTION / DAY_CAT_CURSE / DAY_PURSUED / NIGHT_DEATH`。对应存活、处刑、白天猫咒、白天后追和四类夜死归一；白天原因不得被吞并。
- FINISHED：`NONE / chuxing / daymaozhou / dayhouzhui / beiyao / nightmaozhou / nighthouzhui / zhousha`，保留原始精确值（用户批准的终局选择），而非桌面终局文字对猫咒/后追的合并粒度。此时全身份已揭晓；客户端仍须读完前置事件再展示终局。
- 运行中完整响应任意嵌套位置不得出现四类精确夜死字面量；没有rawDeathReason等兼容后门。核心whyDie、游戏规则和持久化不改。

本版始终返回非null `publicInformation`；数组均非null不可变。旧原型缺此字段表示“不支持”，不能视为“报告为空”。结构：

| 对象 | 字段 / 形状 | 公开条件 |
|---|---|---|
| publicInformation | skillReports, suspicion, deathHistory | 完整最新公开状态，与事件游标无关 |
| skillReports[] | playerNumber, claimedRole, claimedRoleOrder, exposed, results | 运行中当前公开zhan/ling/lie，包括死亡者；exposed仅是该报告行公开破绽，不代表真实职业 |
| results[] | skillDay, targetPlayerNumber, color | 占卜从1、灵能/护卫从2，严格小于当前日及死亡日；不按CO日起点裁掉桌面可见旧历史；原始0不生成项 |
| suspicion | available, rows | 仅运行中gameDay>2为true，否则false且rows为空 |
| rows[] | playerNumber, targets, comparisons | 仅存活观察者，最多3个非零目标，原次序；comparisons数量=max(0,targets数量-1) |
| deathHistory[] | day, executions, nightDeaths | 日1到当前日-1，逐日递增；每项可为空，空nightDeaths表示平和 |
| executions[] | playerNumber, cause, prefixPlayers | cause仅EXECUTION/DAY_CAT_CURSE/DAY_PURSUED；按桌面狐/猫处刑组合规则；prefixPlayers至多一个桌面已显示的前缀玩家，不是额外角色索引 |

`color`仅WHITE/BLACK/NOT_APPLICABLE；护卫只能NOT_APPLICABLE，不传护卫成功判断。报告不包括未CO真实技能者，不返回原始编码。
`comparisons`为CLOSE(`=`)、SLIGHTLY_GREATER(`≧`)、GREATER(`>`)、MUCH_GREATER(`≫`)，内部相邻差依次<=2、<=5、<=10、其余，不发分数/差值/矩阵，不重新排名。CLOSE不等于数学意义分数相等。
`nightDeaths`仅玩家编号，不带夜死子类；历史保留桌面公开分组，即使终局也不把该列表改成私有原因表。终局skillReports为空、suspicion不可用；精确死因只取终局玩家视图。

刷新同游标读取保持公开状态不变；游标只裁事件，不能裁公开信息。进程重启不恢复会话：旧id为404，客户端清凭据，新局不是旧局持久化。HTTP测试用固定种子验证新进程新局的一致性，不承诺生产随机新局相同。

运行中禁止返回 GameContextView、实际职业、核心怀疑度、随机源、非人计划、完整 recorder 或未经审计的私有技能结果。`claimedRole`、匿名预约与终局揭晓不等于允许扩大秘密公开。WEB-004 先为现有形状建立持续正/负门禁；后续新增字段必须显式更新白名单及条件测试，不能用宽泛豁免绕过。

## 4. revision、游标与展示恢复

1. 开局成功后的 revision 为 1；随后每个成功应用写命令加 1，包括返回 voteCompleted=false 的投票以及重复设置同值；GET、确认展示和失败的前置校验不增加。运行时/存储/序列化意外不承诺回滚，按“不确定写入”恢复。
2. 同一会话串行校验 expectedRevision 并执行核心；相同旧版本的并发写最多一个被接受，其他返回 STALE_REVISION。不同会话隔离；DELETE 与已取得会话引用的在途操作可能竞争，不能假定 DELETE 可撤销已经成功的写。
3. 游标 C 是已确认展示的事件前缀长度，合法范围 `[0, 当前发布总数]`。响应半开窗口 `[eventCursor, nextEventCursor)` 满足长度等于 events/presentedEvents 数量；GET 从 C 到当前末尾。C 等于末尾返回空事件，但仍返回完整最新状态。
4. 命令响应仅含该命令新发布窗口，可能为空；状态、合法动作等仍是命令后的完整快照。若窗口起点晚于本地已确认游标，先 GET 补读，不能直接将游标跳到命令窗口起点。
5. 同游标重读不会消费事件或影响其他页面；在无新写入时窗口及状态稳定。若期间有新命令，重读可以返回更长后缀及更新状态，**不是历史快照查询**。revision 与事件游标不能互换，一条命令不一定发布一条事件。
6. 前端只在事件实际确认后推进本地游标；不能收到 nextEventCursor 就把整窗标记已读。同标签页 sessionStorage 保存 `{sessionId,eventCursor}`，刷新 GET 权威状态；不信任旧缓存中的合法候补或 phase。
7. voteCompleted=false 仍是成功写入，先接受新 revision 和 AFTERNOON_DIALOGUE 等事件，读完后由用户重新决定投票，不自动重复原 votes 请求。true 也不保证终局，以 update.phase/result 为准。
8. 当前无后台自主推进/SSE，nextEvent 只确认本地事件成立。WEB-040 需验证多页面引起的旧状态；若未来增加后台写入，必须明确拉取/订阅策略与冲突测试，不能直接沿用单页面假设。

## 5. 错误码与客户端恢复表

错误体保留 `{code, message, currentRevision}`；STALE_REVISION 给出实际 currentRevision，其余通常为 null。当前响应序列化失败兜底可能省略 currentRevision，客户端按可选读取。message 仅为提示，不参与控制流；原型提示当前不全是日文，首发文案任务再统一。网关/网络不保证返回此 JSON 结构。

| 状态 / code | 服务语义 | 客户端动作 |
|---|---|---|
| 400 INVALID_REQUEST | JSON/字段、配役、目标或候补前置校验拒绝；见下方 GET 窄语义 | 显示错误，不重放；必要时用户触发同步 |
| 404 GAME_NOT_FOUND | 未找到会话（包括释放、过期、重启丢失） | 清当前会话凭据/游标并回开局；不清浏览器 Replay/累计战绩 |
| 409 STALE_REVISION | 本命令未通过 revision 检查 | GET 同步并展示最新事件；不自动重放。不得仅更新本地 revision 后静默再试 |
| 409 INVALID_STATE | 已结束/已关闭等会话前置状态拒绝 | 同步确认终局/失效，停止旧操作；未经过版本检查时 currentRevision 不保证存在 |
| 413 HTTP_ERROR | 当前框架请求体上限拒绝 | 缩减/纠正请求；同样的大请求不自动重试 |
| 415 JSON_REQUIRED | 请求体媒体类型拒绝 | 纠正请求 Content-Type |
| 503 SESSION_LIMIT | **本服务**创建前容量拒绝，未新建对局 | 确定性拒绝，不置“操作可能完成”的 uncertain；提示稍后手动再试/释放可控旧会话 |
| 500 INTERNAL_ERROR | 核心、结果写入、响应序列化等未知阶段失败 | 写请求可能已有副作用；禁止自动重放，已有 sessionId 时先同步确认 |
| 429（待 WEB-041 定稿 code） | 将来限流的扩展点，当前未实现 | 仅在明确为执行前限流且有稳定错误码/测试时按确定拒绝；不凭任意 429 页面推断 |
| 无法解析/未知 code、网络中断、超时或代理 5xx | 无可信业务拒绝证据 | 写入结果不确定；与 503 SESSION_LIMIT 区分，不按所有 5xx 一刀切 |

确定性拒绝仅指本请求未执行核心命令，不表示没有其他页面的并发写。框架状态与业务 code 需组合判断；不把任意 404 HTML 当作失效会话，也不把任意 503 当 SESSION_LIMIT。

### GET 400 的窄恢复约束（F2）

现有 GET `/{id}` 的 400 INVALID_REQUEST 只用于 eventCursor 无法解析为 long、为负数或超过该局发布总数。v1 保持这一窄语义；客户端确认方法/路由及 code 后，可清游标并**至多补读一次** C=0，仍失败则保留错误，不无限循环。
未来此 GET 若新增非游标验证，不能复用上述组合；应先设计可区分错误与客户端兼容迁移，必要时升版。其他路由的 400、GET 的非业务错误或不明响应不得触发重置游标。

### 不确定请求的恢复（F1 及刷新）

- 已有 id 的写操作：锁住后续决策，GET 当前游标同步。同步成功才解除不确定标记；读取到新 revision 仅表明状态变化，不证明一定是自己的请求，不能据此自动补发。同步失败继续阻止写入，404 按失效处理。
- 纯 GET 失败不会自行修改服务器；保留原游标与未确认写的状态，不把“读失败”描述成“本次读取可能执行了投票”。
- 创建响应丢失：客户端可能没有 id，无法发现/恢复刚创建的局；不自动重试，不承诺 exactly-once 创建。提示不确定并允许用户主动重开；孤儿局由WEB-039回收：空闲30分钟/最长24小时，创建前补扫与每60秒定时扫描；不驱逐有效局，见[生命周期](web-guest-lifecycle.md)。
- DELETE 成功为 204；DELETE 响应丢失后 GET 若返回 GAME_NOT_FOUND 即确认失效；仍存在则由用户决定是否再次释放。当前 DELETE 无版本保护，是明确结束本局的操作，不按普通游戏命令规则伪造 R。
- 当前不提供请求幂等键、后台重试或事件消费 ACK 接口；若后续需要，须独立设计，不谎称 revision 能保证跨断网 exactly-once。

## 6. Replay / 本地战绩与接口边界

首发没有云端用户、战绩 CRUD 或 Replay 文件仓库 API。本地累计战绩以终局权威投影为输入，由 WEB-036 做本地结算标识/幂等，不能把 sessionId 当永久用户身份。不要为此从服务器导出完整核心状态。

Replay 不能只靠现有公开 GameUpdate 重建；旧录像包含的数据必须由 WEB-030/031 的兼容契约与隔离端口处理。WEB-032已实现只读路径 GET `/{id}/replay?eventCursor=N`，只允许终局且游标恰为发布末尾；运行中导出全部秘密仍禁止，详见[本地Replay库](web-replay-library.md)。WEB-030 明确可导出生命周期、文件封装/旧格式迁移、容量和安全白名单，WEB-031 定稿读取适配；必须测试跨游客隔离、与并发写的一致性以及失效会话行为。无论是否保留此路径，不得悄悄将本地保存改为云端存档。

技能公开投影与 Replay 详细 schema 的延后是明确依赖，不是声称已完全冻结未知规则；分别在 WEB-014 和 WEB-030 定稿并补机器可验收样本，再进入消费任务。

## 7. 实现差距与持续契约验收

WEB-003仅交付文档；后续已实现项目见上方当前契约与各轮验收记录，不能把下面的历史文档验证当作本轮运行证据：

| 任务 | 必交证据 |
|---|---|
| WEB-004 | 现有 GameUpdate 的结构白名单与运行中秘密负例，覆盖嵌套事件/选择；终局揭晓只在条件满足时允许；新增投影持续门禁 |
| WEB-005 | CLI/创建锁不影响上述容量拒绝语义，容量预留/失败释放原子；HTTP 与浏览器显式断言 CO 中断与窗口重读 |
| WEB-006—012 | 每条新端点正例、缺失/null/越界/重复/不合法候补、旧 revision 无副作用、结束局拒绝、刷新恢复；保留原 vote-target 与已实现响应形状 |
| WEB-014—016 | 审计结论→安全 DTO→HTTP→浏览器一致；票型所有轮次；不将 computed Java 方法当 JSON 字段 |
| WEB-017 | 固定七配役种子、直连/HTTP/网页对等；每个已落地操作可复现，F1/F2 恢复用例确有执行证据 |
| WEB-030/031/035/036 | Replay 生命周期与跨端样本、本地战绩幂等；无账户/云端存储，失败不覆盖正常本地数据 |
| WEB-039—041 | 确定/不确定错误分类，503 SESSION_LIMIT、GET 窄 400 单次补读、非游标/非业务 400 不重置、未知 5xx/失响应不重放、多页面冲突、过期/释放及凭据脱敏 |

F1/F2 的前端当前仍按 status 区间和任意 GET 400 处理；这是已登记的原型差距，不声称本轮文档已经修复。统一恢复分类在 WEB-040 落地，WEB-017 先验证截至该轮已实现的路径，若实际功能依赖此差距则提前报告并按授权调整，不能仅通过修改文案宣布已兼容。

文档定向检查：每条目录操作可映射到应用命令或明确的待新增端口；JSON 字段与 record 分量一致；已实现路由保持可识别；所有延期都有任务归属。机器可执行 HTTP/浏览器契约测试随相关实现提交，不以本轮未执行的构建或未来测试充当当前通过证据。

本轮实际文档验证：Node 22 临时只读检查从源码提取 15 个 record 的分量名，与第 3 节字段表逐项同序比对通过；现有 5 条路由/草案 10 条核心路由计数、6 份变更文档的本地链接与行尾空白、49 步顺序及 WEB-003 DONE / WEB-004 READY 状态检查通过。首次检查脚本误将草案路由数设为 11，核对后修正预期为 10 并重跑通过；不是接口缺失。上述检查不执行服务、不证明 Jackson 运行时行为；未重跑 Java/浏览器构建。
