# Village of Cyber Remake 双端与 AI 兼容架构规划

> 决策日期：2026-09-13
> 首次规划基线：`a7a4ced`（`main`）
> 当前实施基线：`4b5b1ce`（阶段 2 的主要应用契约已落地，阶段 3 的单局 `GameSession` 已完成）
> 实施更新：2026-10-19，单局 `GameSession` 会话容器建立
> 状态：阶段 3 进行中，尚未引入 Web 或 AI 运行时

## 一、结论

项目可以同时支持“下载到本地游玩”和“打开网页游玩”，也可以让规则 AI、本地大模型和云端大模型共用同一套决策入口。但这不是把现有 Swing 程序直接转换成网页；可持续方案是保留一份 Java 权威规则核心，在它外面建立平台无关的对局应用层，再分别连接桌面和 Web 适配器。

难度判断：

- 在应用门面完成后，做一个能开始对局并完成一次操作的网页垂直切片：中等，现有核心可以复用。
- 在此之前把业务流程从 Swing 驱动中剥离到应用层：高，是当前双端改造中最难且最容易被低估的一步。现有 UI 不只是渲染，还直接发起开始、CO、处刑等业务操作并消费事件推进流程。
- 做到当前桌面版的完整网页功能与视觉基本一致：中高，主要成本在重新实现当前 5,251 行 Swing UI、资源管线和会话恢复；这与前述业务流程剥离是两个不同工作量来源。
- 同时发布本地版与网页端：可行。迁移期保留 Swing；长期可以让本地包启动 `127.0.0.1` 服务并打开与线上相同的 Web UI，从而只维护一套新界面。
- 将 Java 核心直接编译进浏览器：只适合实验，不作为主路线。TeaVM 能把 Java 字节码编译成 JavaScript/WebAssembly，但使用自己的 Java 类库子集；当前核心仍包含 Jackson、文件配置和持久化边界，迁移成本和兼容风险高于把 Java 留在服务端。

## 二、目标架构

```text
                         ┌─ Swing 适配器（迁移期桌面版）
                         │
玩家命令 ──> GameApplication / GameSessionFacade <── HTTP API ── Vue Web UI
                         │                              │
                         │                              ├─ 线上：远程 Javalin 服务
                         │                              └─ 本地：127.0.0.1 Javalin 服务
                         ▼
                每局独立 GameSession
                  ├─ 权威规则 core/domain
                  ├─ 独立 GameRandom
                  ├─ 独立事件与 Replay 记录
                  └─ DecisionPolicyRouter
                       ├─ RuleDecisionPolicy
                       └─ LlmDecisionPolicy
                            └─ Local / Remote LlmClient

持久化端口 ──> 本地 JSON 适配器 / 服务端数据库适配器
资源端口   ──> 本地 classpath / Web 静态资源与 CDN
```

必须保持的单向关系是：

```text
平台适配器 -> 应用层 -> 权威规则核心
AI 适配器  -> 决策端口
持久化实现 -> 持久化端口
```

规则核心不得反向引用 Swing、HTTP、Javalin、Vue、数据库或具体模型客户端。

## 三、双端如何共存

### 3.1 迁移期

保留现有 Swing 桌面版，同时开发 Web UI。两者调用同一个应用层，但使用方式不同：

- Swing 在进程内直接调用 `GameSessionFacade`；
- 浏览器通过 HTTP 调用 Javalin 适配器；
- 两端接收相同语义的 `GameUpdate`，但由各自界面渲染；
- 规则、合法候选项、死亡、胜负和 Replay 只在 Java 权威核心中计算。

迁移期会暂时维护两套 UI，这是控制风险的代价。不能一开始删除 Swing，也不能要求 Web 首版一次达到全部功能。

### 3.2 长期本地发行

网页端稳定后，可以增加“本地 Web 包”：

1. 打包 Java 服务、Web 静态文件、配置和资源；
2. 启动器把 Javalin 绑定到 `127.0.0.1` 的随机空闲端口；
3. 启动成功后打开系统浏览器；
4. 对局与本地模型都只在玩家电脑运行；
5. 停止启动器时关闭本地服务和模型 sidecar。

这使下载版和线上版共享同一套 Vue 页面及 HTTP 协议。Swing 是否最终停用，应等 Web 达成功能与体验对等后再决定，当前不预设删除日期。

### 3.3 线上发行

线上 Web 使用相同的 Javalin 适配器，但每位用户拥有独立 `GameSession`。第一版可以是游客会话和内存存储；只有确认需要账号、跨设备续局、可信战绩后才接认证与数据库。

浏览器永远不能收到完整 `GameContextView`。远程服务只能返回按玩家身份白名单构造的 `PlayerViewSnapshot`；实际身份、其他人的私有结果、内部怀疑度和非人计划必须留在服务端。

## 四、平台无关应用契约

现有 `MainLogicInterface` 和 `GameContextView` 是 Swing 时代的进程内接口：前者按 UI 操作命名，后者同时暴露真实身份与四类写操作。它们不应直接成为网络协议。

目标应用层只需要少量稳定概念：

```text
GameId / SessionId
GameCommand           玩家请求做什么
LegalAction           当前允许做什么
PlayerViewSnapshot    当前玩家允许看到什么
PublicGameEvent       本轮发生了什么公开事件
GameUpdate            revision + view + selections + legalActions + events
```

命令可以逐步使用密封类型表达，例如开始对局、询问 CO、提交投票和安排技能对象。每个命令必须携带 `gameId`、调用者身份和客户端已见的 `revision`；服务端再次校验合法性，防止重复提交、旧页面提交和越权目标。

`PlayerViewSnapshot` 必须从空对象开始白名单式构造，不能先复制 `GameContext` 再删除秘密。桌面结算页、调试工具或 Replay 所需的全知视图应使用另一个明确标记的内部/结算投影，不能让普通玩家视图带一个 `showSecrets` 布尔值。

### 4.1 业务流程状态与表现状态的边界

迁移开始前，`MainLogic.start(...)`、`shokei(...)` 和 `askCo(...)` 的生产调用点均位于 Swing UI，选择面板还直接写入投票与技能预约状态。截至 `1c863df`，这些入口已经全部改由 `GameApplication` 承接，选择面板也不再读取真实身份来展开“全体指定”；`4b5b1ce` 又把 `GameApplication` 收进每局独立的 `GameSession`，Swing 只持有会话。但 `DialogueDayHandler` 仍按桌面语义逐项消费事件，多个 Handler 仍依靠 `UI.currentScene` 和 Swing `Timer` 推进画面。因而不能把浏览器化理解成只重写渲染层，否则 Vue 会被迫复制一份流程判断。

但也不能把整个 `UI.currentScene` 状态机原样搬到服务端。目标边界是：

- 应用层 / `GameSession` 持有业务阶段、当前可接受命令、合法候选项、待消费的领域事件、命令结果、`revision` 与幂等状态；
- Swing / Vue 持有当前面板、动画进度、打字机效果、过场计时、音效/BGM 和尚未提交的本地选择；
- 核心层继续唯一决定规则合法性、投票、死亡、胜负和技能结算，前端只提交命令并渲染权威结果。

对现有 UI 内计算也要按职责处理，不能以“位于 `ui` 包”作为迁移依据：

- 原 `GreyListCalculator` 已迁为应用层 `VoteCandidateProjector`，自由、灰区和指定投票均由应用层返回权威候选项，Swing 不再独立推导；
- `VoteResultRenderer` 当前会从已记录票型重新统计平票，以决定各轮结果的展示推进；权威多轮投票本身已经在核心完成。未来应直接渲染应用层给出的 `VoteOutcome` 与轮次序列，避免前端重复推导领域结论；
- `VoteInfoRenderer` 一类纯文本/布局转换可以保留在前端，但只能消费为该界面准备的安全投影，不能依赖包含秘密与写操作的 `GameContextView`。

因此，阶段 2 的目标不是“UI 中零计算”，而是“UI 中零权威规则决策、零业务阶段推进、零直接状态写入”。

### 4.2 当前资源边界

桌面资源名称已经按来源完成第一轮收口：固定界面图片由 `UiImage` 表达，按状态组合生成的图片名由 `ImageNames` 统一构造，事件立绘继续以 `event_image_mapping.json` 为数据目录，固定音效与 BGM 分别由 `SoundEffect`、`BgmTrack` 表达。业务与 UI 代码不得直接出现 `.png` 或 `.wav` 文件名字面量，该约束已有源码门禁；固定图片、事件映射和音频目录也分别有存在性、大小写与 classpath 校验。

`Resources.getImage(String)` 仍需保留给动态命名和数据驱动立绘，不能误删成只有枚举参数的接口。固定 `UiImage` 已用 `EnumMap` 缓存；字符串入口可能覆盖大量角色与事件立绘，当前故意不做无上限缓存。只有测量证明重复解码形成实际瓶颈后，才引入有容量上限且可观测命中率的缓存。

这项收口只解决代码如何引用资源，没有解决网页发布的体积、格式、分包、授权与 CDN 策略。Web 资源清单可以从现有语义目录和 JSON 映射生成，但仍须经过后续资源管线阶段。

## 五、AI 决策的统一调控

“哪些环节启用 AI”只允许在一个权威配置对象中决定，不能在狼咬、占卜、CO 等类里散落多个布尔字段。建议使用白名单配置：新增加的决策类型默认走规则，不会因为漏配而意外调用模型。

概念配置如下：

```json
{
  "enabled": true,
  "llmEnabledDecisions": [
    "DIVINATION_TARGET",
    "FAKE_DIVINATION",
    "WOLF_BITE"
  ],
  "socialMode": "RULES",
  "provider": "LOCAL",
  "timeoutMillis": 8000,
  "maxRetries": 1
}
```

开发时改变一个 `EnumSet<DecisionType>` 或配置数组即可切换接入范围；发布版可以由设置界面生成同一配置。`enabled=false` 或空白名单等价于完整规则模式。

需要把两个维度分开：

- `llmEnabledDecisions` 决定哪些行动由模型选择；
- `provider` 决定模型运行在本地、远端还是自动选择。

`DecisionPolicyRouter` 的固定流程为：

```text
取得该决策的 RuleDecisionPolicy
    ↓
不在 AI 白名单：直接执行规则策略
    ↓
在白名单：向 LlmDecisionPolicy 提交已脱敏 DecisionRequest
    ↓
合法结果：返回 choiceId
超时、异常、空结果、越界或重试耗尽：执行原规则策略
```

Replay 只记录最终被引擎接受的选择、决策类型、策略来源和必要版本信息；回放时不得重新调用模型。

### 5.1 不同平台的模型位置

| 运行方式 | 规则 AI | 本地模型 | 云端模型 |
|---|---|---|---|
| Swing 桌面版 | 进程内 | `llama.cpp` sidecar / localhost | 桌面端安全配置 API |
| 本地 Web 包 | 本地 Javalin 进程内 | 同机 sidecar | 本地服务代调 |
| 线上网页端 | 服务端进程内 | 服务器部署模型（可选） | 服务端代调 |

浏览器前端不得保存开发者的云模型密钥。浏览器直接连接玩家电脑上的模型服务、WebGPU 浏览器推理或用户自带 API 可以作为高级实验功能，但不进入第一版默认路线。

## 六、推荐技术选择

### 6.1 后端：Javalin 作为首个垂直切片候选

第一版优先评估 Javalin，而不是直接引入 Spring Boot：

- 当前只是一个回合制游戏服务，不需要完整企业应用生态；
- 项目已有 Gradle、Java 21 和 Jackson；
- Javalin 7 要求 Java 17+，使用嵌入式 Jetty，并内置 HTTP 路由、静态 SPA、会话和 WebSocket 能力；
- 同一个可执行 JAR 既能部署到服务器，也能绑定 localhost 服务本地 Web 包。

Spring Boot 仍是账号、数据库迁移、复杂监控或团队规模扩大后的备选。正式试点时再锁定依赖版本，不在规划阶段提前加入构建。

### 6.2 前端：Vue 3 + TypeScript + Vite

项目界面以固定舞台、状态面板、按钮和文本为主，优先使用 DOM/CSS；首版维持 1280×720 逻辑坐标并整体等比缩放，不同时承担手机响应式重做。

选择 Vue 3 + TypeScript + Vite 作为默认候选：Vue 官方脚手架直接提供 Vite、TypeScript、Vitest 和 Playwright 选项；本项目不需要 SEO 或服务端渲染，因此暂不引入 Nuxt。也不在首版引入 Pinia，只有跨页面共享状态确实复杂后再增加。

### 6.3 通信：HTTP 优先，SSE 可选

当前是低频、回合制交互，开始对局、询问 CO、投票和技能安排都适合普通 HTTP 请求/响应。模型调用如果需要显示进度，可增加轮询或单向 SSE；实时多人、双向高频事件或明确的长连接需求出现后再启用 WebSocket。

### 6.4 不作为主路线的方案

- TeaVM：保留为“纯核心能否编译到浏览器”的实验，不承担 Swing UI 或首版正式发布。
- CheerpJ/Webswing：可做快速试玩和兼容性 PoC，但不解决现代 Web UI、资源体积、授权与长期运维问题。
- 规则核心重写为 TypeScript：会形成 Java/TypeScript 双规则源，除非未来决定彻底停止 Java 版，否则不采用。

## 七、当前真实阻塞点

截至基线，已有条件和阻塞点如下：

- `core/domain` 共 60 个文件、7,471 行，且已被架构测试禁止依赖 Swing；规则复用基础良好。
- 原有 `ConstNum` 静态随机状态已被单局 `GameRandom` 取代；生产代码中的旧随机入口和静态随机状态均已清零，两局交错完整对局测试已证明随机轨迹互不污染。
- `MainLogic` 仍直接读取文件配置；`ExecutionManager` 直接更新 `GameRecordManager`；`GameRecorder` 同时负责对局轨迹与战绩元数据，应用/持久化边界尚未完全分离。
- Swing UI 对旧进程内视图仍有大量读取；`start`、`askCo`、`shokei` 以及 `CharacterSelectionPanel` 的四类状态写入已经全部通过 `GameSession`，但事件消费和部分流程推进仍在 Handler 中。不能一次机械替换，应按一个完整用户流程逐步改用应用门面。
- `GameContextView` 暴露实际身份并允许写入预约状态，不能作为网络 DTO。
- 当前仍有 591.41 MiB WAV 和 127.56 MiB PNG。网页端必须转码、分包、缓存和延迟加载；不能把当前 690.52 MiB Fat JAR 当网页资源包。
- 音乐、图片和字体的网页公开传播及转码授权是独立发布门槛；即使 Web 代码完成，授权台账未确认也不能公开上线相关资源。

## 八、实施顺序与退出门槛

### 阶段 1：每局隔离，不接 Web/AI

状态：**已完成**。

1. 已把 `ConstNum` 的静态可变状态替换为每局拥有的 `GameRandom`；
2. 每局独立持有 Java 随机数生成器、随机序列录制与摘要；
3. 已增加两局按天交错推进的完整对局测试，证明不同种子和调用顺序不会相互污染；
4. 七种配役既有完整轨迹、随机校验和、规则结果和 Replay 行为均保持不变。

这项底座同时服务于服务器并发、AI 离线批量评测和普通测试并行。下一步进入阶段 2，但仍只先做一个最小应用门面垂直切片，不一次性替换 Swing 的全部调用。

### 阶段 2：应用门面与安全视图

这是双端代码改造中难度最高的阶段。先做可回退的垂直切片，再扩大覆盖范围：

当前进度：已建立不依赖 Swing/HTTP 的 `GameApplication`。桌面端开局通过 `StartGameCommand` 并接收无秘密的 `StartedGame` 摘要；按职业/玩家询问 CO 分别通过两种不可变命令；自由、灰区和指定投票统一通过 `ExecuteVoteCommand` 与语义化 `VotingMethod`，应用层保留“回避 CO 打断投票”的既有返回语义。目标选择分为“为全部相关行动者指定”和“为某个已 CO 行动者指定”两种命令，应用层根据真实身份、CO 和存活状态展开底层预约，Swing 不再直接写四类状态或读取真实身份名单。领域事件已迁入单局 `GameEventBuffer`；每份更新用 `[eventCursor, nextEventCursor)` 明确标识所带事件窗口，桌面待展示后缀不再冒充完整历史，命令结果只带该命令同步发布的事件，Web 则可按自有游标非破坏、可重复地续读。`GameUpdate` 现已包含白名单构造的 `PlayerViewSnapshot`，并把核心返回应用层后的稳定业务状态明确为 `DAY_DECISION` 或 `FINISHED`。自由、灰区和指定投票由 `LegalVoteAction` 返回候选；CO 询问由 `ClaimInquiryOptions` 返回可询问职业与存活玩家；三类预约由 `LegalTargetSelection` 返回存活目标和公开 CO 行动者。普通视图始终不含真实身份；仅在 `FINISHED` 阶段通过独立 `RevealedPlayerRole` 列表一一揭晓，投影器在对局结束前会主动拒绝调用。`SelectionStateSnapshot` 额外白名单投影指定投票、匿名潜伏技能目标、公开 CO 行动者目标和回避开关；它不暴露真实行动者身份。回避设置是带 revision 的应用命令，投票只使用应用层权威值；Swing 已删除对应本地镜像，并从同一快照恢复选择面板和摘要。`GameApplication` 现以 0 为初始 revision，在同一实例内对每个成功接受的写命令严格递增且不因新开局归零；所有写命令携带客户端已见版本，校验与核心调用在同一串行临界区内完成，过期或重复请求在进入核心前被拒绝。`GameUpdate`、`StartedGame` 与 Swing 均使用同一版本契约；只读查询、事件续读和桌面展示队列操作不推进 revision。`4b5b1ce` 进一步把 `GameApplication` 收为包内实现，由每局独立的 `GameSession` 持有；桌面端开局、CO、投票、目标预约、回避和事件读取统一经会话进入，会话状态为 `NEW/RUNNING/FINISHED/CLOSED`，关闭后拒绝命令与读取。阶段 2 尚未正式退出：`VoteResultRenderer` 等展示推导仍在 UI，`DialogueDayHandler` 等事件消费也仍按桌面语义推进。

TASK-007 更新：上述关于 `VoteResultRenderer` 仍在 Swing 推导权威结论的描述已经过时。`GameUpdate` 现已公开投影投票方式、候补、逐轮票型、最高票、同票与最终处刑者，当前结果和历史浏览只渲染该快照，并有架构门禁禁止恢复旧读取。阶段 2 仍未正式退出，因为 `DialogueDayHandler` 等事件消费仍按桌面语义推进，其他结果展示也需逐条核实权威边界。

1. 只定义业务阶段，不复制 `UI.Scene`；先覆盖“开始对局 → 读取事件 → 完成一种玩家操作”；
2. 定义 `GameCommand/GameUpdate/LegalAction/PlayerViewSnapshot` 的最小集合，令更新同时携带合法候选项、业务结果和 `revision`；
3. 让 Swing 的这一条流程改用门面，证明接口不依赖 HTTP；
4. 将灰名单、投票结果等权威推导收敛到核心/应用层，UI 只渲染结果；
5. 补未授权身份不可见、非法目标拒绝、旧 revision 拒绝和重复命令测试；
6. 逐条迁移其余 Swing 业务入口，保持表现层过场与动画不变。

阶段 2 完成时必须同时满足：

- `ui` 不再直接调用 `MainLogic.start(...)`、`shokei(...)` 或 `askCo(...)`，玩家业务操作均通过 `GameCommand`；
- `ui` 不再通过 `GameContextView` 的四个 setter 写入投票和技能预约状态；
- 合法动作、候选项、投票结果和下一业务阶段来自 `GameUpdate`，UI 不独立重算权威结论；
- 表现专用的场景、计时器、动画、音频和文本布局仍可留在 UI，不以“零 UI 计算”为目标；
- 无 Swing、无 `JFrame` 的测试可以只通过应用门面走完整局；相同种子和命令序列与既有核心轨迹、Replay 结果一致。

### 阶段 3：持久化端口与会话容器

状态：**进行中**。单局 `GameSession` 已完成（`4b5b1ce`）；持久化端口尚未迁移。

1. 已建立每局独立 `GameSession` 与生命周期管理；会话内命令、事件游标、revision 和读取串行；
2. 桌面端每局创建新会话，并继续共用 `data/game_record.json` 以保持战绩页累计；并发 Web 会话必须由组合根注入隔离战绩路径；
3. 将战绩更新和 Replay 保存从核心流程改为应用层端口（尚未开始）；
4. 本地继续使用 JSON 适配器；
5. 暂不引入数据库和账号系统。

阶段 1～3 是 Web 与 AI 共用的基础。完成后，下面的 Web 垂直切片与 AI 垂直切片可以按实际优先级交换顺序或分别推进，任何一方都不得成为另一方的编译依赖。AI 文档中的离线模型延迟/质量实验不修改生产代码，可以在基础重构期间并行进行。

### 阶段 4：Web 垂直切片

1. 新增独立 `web-server` 与 `web-client` 构建，不把 HTTP 注解放入核心；
2. Javalin 提供创建游客局、读取更新、提交一个命令的 HTTP API；
3. Vue 页面只实现配役选择、事件展示和一种交互；
4. 同一固定种子下，Swing 直连与 HTTP 路径产生相同领域轨迹；
5. 测量 API、会话内存、冷启动和最小资源下载量后再决定扩展。

### 阶段 5：AI 路由骨架与真占垂直切片

1. 建立 `DecisionType`、`DecisionRoutingConfig` 和 `DecisionPolicyRouter`；
2. 默认配置为空白名单，确认规则轨迹不变；
3. 只接真占目标一种 LLM 决策；
4. 同一决策配置在 Swing、本地 Web 和线上服务中使用；
5. 验证视角泄漏、非法输出、超时、取消、重试、降级、延迟和 Replay。

### 阶段 6：双端对等与发布

1. 按页面逐步迁移游戏、Replay、战绩和设置；
2. 在批量处理前先用 BGM、立绘、背景和图标代表样本完成转码实验，测量首屏、首局和累计下载预算；
3. 建立资源来源、公开传播、转码、署名和商业使用条件的授权台账；授权未确认的资源不得进入公开 Web 发布包；
4. 建立资源转码、分包、内容哈希、缓存和延迟加载清单；
5. 线上先支持游客，账号/数据库按产品需求后置；
6. 增加本地 Web 启动器与进程生命周期管理；
7. Web 达成功能、视觉、键鼠操作和异常恢复对等后，再决定 Swing 的长期状态。

任何阶段只要无法满足既有 `clean check`、七配役轨迹、Replay 兼容或规则模式零模型调用，就停止扩展并回到上一个稳定点。

总体难度按当前证据排序为：业务流程脱离 Swing 最高，资源管线与授权确认其次，持久化与在线会话再次；应用边界稳定后的 Web API 垂直切片反而较直接。AI 的白名单路由骨架本身较轻，但真实模型的语义质量、推理延迟、分发体积与成本仍是独立难题，不能笼统称为“最容易”。

### 8.1 距离网页端完成还有多少工作

必须区分“网页上能玩一个纵向切片”和“完整替代当前桌面版”。截至 `4b5b1ce`，规则复用、每局随机隔离、明确的应用事件窗口、最小安全玩家视图、终局身份揭晓、稳定业务阶段、投票/CO/预约合法候选、可靠 revision、当前选择投影、开局/CO/目标预约/投票四类应用用例，以及每局独立 `GameSession` 均已落地。最新完整构建共有 395 项测试通过，桌面 Fat JAR 构建成功并实际启动。资源仍有 1,838 个图片文件、32 个 WAV，Fat JAR 约 690 MiB。这些数据说明 Java 规则底座可复用，但持久化端口、Web UI 和资源发布仍是主体工作。

按当前每批都保持桌面版可运行、可回退的粒度估计：

| 目标 | 尚需完成的主要内容 | 预计剩余批次 |
|---|---|---:|
| 最小可玩 Web 垂直切片 | 会话容器已完成；增加 Javalin 创建/读取/提交 API；建立最小 Vue 页面 | 约 1～3 批 |
| 桌面功能基本对等 | 完整业务阶段与结果投影；全部游戏场景、Replay、战绩、帮助、设置和音频控制的 Web 实现；刷新恢复和错误处理 | 总计约 20～35 批 |
| 可公开发布 | 资源转码、分包、缓存与 CDN；素材授权台账；部署、安全、监控、备份、浏览器兼容和端到端测试 | 包含在上项后段，且受产品决策影响最大 |

这里的“批”是一个有独立契约、测试和桌面回归的聚焦提交，不是等时工作单位，各阶段也会交叉，不能把表内数字机械相加。若仅要求电脑浏览器、游客模式、固定 1280×720 舞台和粗略视觉，可接近区间下限；若要求账号、跨设备续局、手机适配、视觉逐像素对等或可信排行，会超过上限。

从“完整功能与基本视觉对等、可公开部署”的总目标看，当前仍约完成 25%～35%，剩余约 65%～75% 的工程量；revision 与选择状态投影已经缩短最小垂直切片的关键路径，但相对完整 Web UI、资源发布和部署工作的总量，还不足以把宏观比例上调一个档位。这是根据已迁移契约、现有 UI/资源规模和未完成交付面做出的工程估算，不是代码行比例或精确工期。以单人持续开发粗略看仍是数月级工作，而不是再加一个 HTTP 接口就结束。

接下来的最短关键路径是：以已落地的单局 `GameSession` 为共同入口，进入 Javalin/Vue 最小垂直切片；持久化端口可以在切片前后按需推进，但并发会话必须使用隔离存储。`GameSession` 已不再是候选任务。

## 九、已经确定与仍待确定的事项

已经确定：

- 长期支持本地下载和网页访问两种运行方式；
- 共享 Java 权威规则核心，不维护两套规则实现；
- 浏览器不接触完整权威状态或开发者模型密钥；
- AI 按决策类型白名单启用，并始终保留规则兜底；
- 第一版 Web 使用 HTTP，Javalin 与 Vue 作为默认垂直切片候选；
- 先做每局隔离，再做 Web/AI 适配。

仍需在对应阶段前决定：

- 是否必须注册账号，还是默认游客即可游玩；
- 是否需要跨设备战绩、Replay 与进行中对局恢复；
- 是否需要可信排行或防止玩家查看本局秘密；
- 第一版是否支持手机触屏；
- 主要部署地区、同时在线人数和预算；
- Swing 在 Web 对等后是长期保留、维护模式还是停止更新；
- 全部音画和字体是否允许网页公开分发及转码。

## 十、官方资料

- Javalin 文档：<https://javalin.io/documentation>
- Spring Boot WebSocket 文档：<https://docs.spring.io/spring-boot/reference/messaging/websockets.html>
- Vue 快速开始：<https://vuejs.org/guide/quick-start>
- Vue TypeScript 指南：<https://vuejs.org/guide/typescript/overview>
- TeaVM 总览：<https://www.teavm.org/>
- TeaVM Gradle 插件：<https://www.teavm.org/docs/tooling/gradle.html>
- TeaVM Java 类库说明：<https://teavm.org/docs/runtime/java-classes.html>
- Server-Sent Events：<https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events>
- IndexedDB：<https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API>
