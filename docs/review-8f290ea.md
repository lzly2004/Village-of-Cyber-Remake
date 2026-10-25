# e3d0a5e / f979eca / 8f290ea Review 核验与 WEB-004

输入范围：外部 reviewer 以 2038d98 为基线审查上述三笔文档提交，没有阻断发现。本轮逐项核验，并执行下一项 WEB-004；不执行下一项 CLI/创建锁任务，不改核心投票规则。

## 逐项核验

| 编号/事项 | 证据与验证方式 | 结论 | 处置/归属 |
|---|---|---|---|
| C1：未知字段被忽略 | 生产 GuestGameServer 未改；新增真实 HTTP 用例提交带 targe 的创建 JSON，实际返回 400 INVALID_REQUEST；容量上限为 1 时随后合法创建成功。带 actualRole 额外字段的 PUT 亦返回 400，GET 整份更新与之前相同。此用例在任何生产改动前即通过；桌面产物确认 Jackson 2.17.1 | 排除当前缺陷：未显式 enable 不等于默认关闭。原契约关于拒绝未知字段的断言正确，不能按 reviewer 推断改成忽略 | 无需改配置或协议；WEB-004 将行为锁入持续 HTTP 测试，契约补明默认值及实测依据 |
| C3：null 与 NONE | Event 简写构造器将未提供角色参数置 null；CharacterEnglishName 有 NONE；实际序列化同一事件的 ch1=null、ch2="NONE"、ch3=null，测试通过；App.vue 已同时排除两者 | 采纳格式说明补充，不是新游戏语义 | 更新 v1 草案，明确 wire 区别与展示处理；不改现有客户端 |
| C4：末轮重试无硬上限 | 源码确认 conductVoting 在达到最大轮号后仍循环，VoteSelector 每次 select 并应用效果；VoteState 对未完成局的相同轮槽位可覆盖 | 接受为既有终止性风险观察；本轮没有复现永久不收敛，也未证明与原版规则不符 | 登记 PROPOSED 候选 CORE-TIE-001，不插入 49 步顺序、不改规则或随机消费 |
| 已验证声明 1—6 | 对照固定五种 CO 职业、快照 record、完整投票轮次校验、错误与 revision/游标实现及前轮记录，没有收到新的矛盾证据 | 接受此前证据范围，不将源码核验冒充本轮新增独立全量复审 | 维持原契约与任务归属；N1 已按用户决定关闭 |
| skill 移除 | 本地旧 SKILL.md 不存在，规则参考 skill 保留；现行规范已记录用户裁决 | 接受 | 不删除游戏规则参考，不恢复已停用重构标准 |

报告提及 C2 的疑问已自行排除，未作为新发现；不因编号跳号虚构待处理项。

## WEB-004 实现边界

- `GuestWireContract` 对 GameUpdate 的全部嵌套对象、列表与创建/投票响应外壳执行精确字段白名单；字段即便不叫“secret”也不能默默增加。普通标量位置不能藏对象/数组，非事件角色参数不接受 null。
- DAY_DECISION 禁止任何 revealedRoles；FINISHED 必须与 players 数量、编号顺序一致，实际职业非 NONE。同步验证事件原始/展示投影及游标窗口一致。
- 类型图递归检查 18 个明确允许的 record 及枚举/标量、参数化 List；不接受 GameContextView、GameRecorder、Random、Object、原始 List 或未审查的新类型。
- 在既有七配役完整 HTTP/直连对等用例中，创建、选择响应、投票响应、每次 GET 及终局均执行同一门禁；额外验证应用层 CO 和非空技能预约的序列化。新增字段或类型须显式审查后调整白名单。
- 负例在测试自己的 JsonNode 副本中注入根/玩家嵌套字段、无害名称的新字段、响应外壳字段、提前身份揭晓、假终局、标量藏对象，并断言门禁失败；类型检查也验证拒绝核心/无界类型。不改共享生产源码、桌面数据或黄金摘要来做探针。
- 这是构建时持续回归门禁，不是运行时通用脱敏器，也不证明白名单内每个值的语义永远安全；后续 WEB-013/014 仍须逐字段信息审计，新路由应复用门禁。日志凭据风险仍归 WEB-041/044，不声称本轮已修复。

## 本轮验收

环境：Java 21、Gradle 8.8；依赖沿用锁定版本。无生产、前端、依赖或 Replay/战绩格式变更。

1. 定向：`gradle cleanTest test --tests com.vocr.web.GuestGameServerTest --tests com.vocr.web.GuestSerializationBoundaryTest --console=plain` 通过。首次新增类型门禁漏列 List<Integer> 的 Integer，导致 1 项失败；补齐标量白名单并重跑通过，没有修改生产类型或放宽核心边界。随后补响应外壳负例并执行完整 clean 验证。
2. 完整：`gradle clean check verifyRuntimeAssets fatJar --console=plain` 通过；433 项测试零失败，包括 HTTP 11 项、序列化专项 13 项与 FullGameCharacterizationTest 的七配役轨迹/交错验证；560 条概率与音频资源校验通过，未更新黄金摘要。
3. Fat JAR SHA-256：`9d1c93f0d1b0bb0114333874728cd2b5364bd6c7e0f0e50314c001144fd39f2d`，与此前桌面产物一致。
4. `npm run test:e2e`：7 项真实 Chromium 浏览器测试全部通过（9.2s），沿用 web-client/README.md 的 Ubuntu 平台覆盖及临时浏览器配置；无 UI 改动，不额外修改前端。差异/文档链接检查通过。
5. 完整检查使用现有工作区（包含用户原有 Game.java 空白差异），不将该差异纳入提交；用户已有换行变化与其他未跟踪文件全部保留。未知字段 PUT 负例特意使用最新合法投票目标，避免由死者/非法目标造成 400 而误认拒绝原因。
