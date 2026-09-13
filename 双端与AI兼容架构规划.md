# Village of Cyber Remake 双端与 AI 兼容架构规划

> 决策日期：2026-09-13
> 代码基线：`a7a4ced`（`main`）
> 状态：架构方向已确认，尚未引入 Web 或 AI 运行时

## 一、结论

项目可以同时支持“下载到本地游玩”和“打开网页游玩”，也可以让规则 AI、本地大模型和云端大模型共用同一套决策入口。但这不是把现有 Swing 程序直接转换成网页；可持续方案是保留一份 Java 权威规则核心，在它外面建立平台无关的对局应用层，再分别连接桌面和 Web 适配器。

难度判断：

- 做一个能开始对局并完成一次操作的网页垂直切片：中等，现有核心可以复用。
- 做到当前桌面版的完整网页功能与视觉基本一致：中高，主要成本在重新实现 5,362 行 Swing UI、资源管线和会话恢复。
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
                  ├─ 独立 RandomSource
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
GameUpdate            revision + view + legalActions + events
```

命令可以逐步使用密封类型表达，例如开始对局、询问 CO、提交投票和安排技能对象。每个命令必须携带 `gameId`、调用者身份和客户端已见的 `revision`；服务端再次校验合法性，防止重复提交、旧页面提交和越权目标。

`PlayerViewSnapshot` 必须从空对象开始白名单式构造，不能先复制 `GameContext` 再删除秘密。桌面结算页、调试工具或 Replay 所需的全知视图应使用另一个明确标记的内部/结算投影，不能让普通玩家视图带一个 `showSecrets` 布尔值。

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

- `core/domain` 共 60 个文件、7,438 行，且已被架构测试禁止依赖 Swing；规则复用基础良好。
- `ConstNum` 仍有 27 个静态随机调用分布在 10 个核心文件中，不能安全支撑并发对局。
- `MainLogic` 仍直接读取文件配置；`ExecutionManager` 直接更新 `GameRecordManager`；`GameRecorder` 同时负责对局轨迹与战绩元数据，应用/持久化边界尚未完全分离。
- Swing UI 对 `MainLogicInterface/GameContextView` 有约 209 个引用点，不能一次机械替换；应按一个完整用户流程逐步改用应用门面。
- `GameContextView` 暴露实际身份并允许写入预约状态，不能作为网络 DTO。
- 当前仍有 591.41 MiB WAV 和 127.56 MiB PNG。网页端必须转码、分包、缓存和延迟加载；不能把当前 690.52 MiB Fat JAR 当网页资源包。
- 音乐、图片和字体的网页公开传播及转码授权仍可能成为上线硬阻塞。

## 八、实施顺序与退出门槛

### 阶段 1：每局隔离，不接 Web/AI

1. 把 `ConstNum` 的可变随机状态替换为每局注入的 `RandomSource`；
2. 每局持有独立随机序列记录与摘要；
3. 增加并行双局测试，证明不同种子和调用顺序不会相互污染；
4. 保持七配役轨迹与规则模式完全不变。

这是下一轮最值得做的重构。它现在同时服务于服务器并发、AI 离线批量评测和普通测试并行，不再是为假想需求预建结构。

### 阶段 2：应用门面与安全视图

1. 先覆盖“开始对局 → 读取事件 → 完成一种玩家操作”的垂直流程；
2. 定义 `GameCommand/GameUpdate/LegalAction/PlayerViewSnapshot` 的最小集合；
3. 让 Swing 这一条流程改用门面，证明接口不依赖 HTTP；
4. 补未授权身份不可见、非法目标拒绝、旧 revision 拒绝和重复命令测试；
5. 能在无 Swing、无 JFrame 的测试中走完完整对局。

### 阶段 3：持久化端口与会话容器

1. 将战绩更新和 Replay 保存从核心流程改为应用层端口；
2. 本地继续使用 JSON 适配器；
3. 建立每局独立 `GameSession` 与生命周期管理；
4. 暂不引入数据库和账号系统。

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
2. 建立资源转码与内容哈希清单；
3. 线上先支持游客，账号/数据库按产品需求后置；
4. 增加本地 Web 启动器与进程生命周期管理；
5. Web 达成功能、视觉、键鼠操作和异常恢复对等后，再决定 Swing 的长期状态。

任何阶段只要无法满足既有 `clean check`、七配役轨迹、Replay 兼容或规则模式零模型调用，就停止扩展并回到上一个稳定点。

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
