# 夜晚与晨间转场 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在当前PR恢复桌面夜晚、晨间背景、日数继续和音画时序，不消费额外权威事件。

**Architecture:** 用纯公开历史规划入口，由App级本地状态控制器持有转场完成键和阶段；GameStage只呈现当前阶段，StageArtwork继续负责结果自身两帧。BgmControl接收明确展示阶段并在图像就绪后发音，不让首夜音效靠队列自然结束猜时序。

**Tech Stack:** 现有Vue3、JavaScript、Web Audio、Playwright/Node测试；无新增生产依赖。

**Spec:** [夜晚与晨间转场补修](../specs/2026-10-29-night-day-transition.md)

## Global Constraints

- 基线300a806；只在原干净LF feature/wf008-playtest分支；用户NTFS脏树只读保全。
- NIGHT_SCREEN_DURATION_MS=7000、HOWL_TRANSITION_MS=2500、TYPEWRITER_DELAY_MS=50，来源锁定Java；预算1.5MiB/3MiB/100MiB不变。
- 本地继续不调用nextEvent、不写存储、不推进确认游标；不改Java/API/规则/格式/源台词。
- 一次只跑一套浏览器/JVM；无本机全量授权时只执行前端分级。用户已授权同分支必要补修发布及云端复验，不含Ready/合流。
- 执行沿既定用户分工：本Coder在此会话逐项实施，独立CR由用户转交；不另派代理代替CR。

## Review Focus

- 多名夜死或后追：同夜只插一次入口，不能每条死亡都重放夜晚。
- 当前公开结算紧随执行组：仅夜背景，不用未来胜负提前显示结算或狼嚎。
- 刷新/补读晚到/换局：计时器与图片发布受版本守卫，游标与权威快照不被本地继续改写。
- 损图/前缀缺失：有限诊断和本地退出路径，不无限等待、不猜图、不禁用全部业务。
- 未启用/切后台/重看/减少动态效果：不能补播旧声音；静态夜/晨画面保留，无新增自动授权或跳事件。

---

### Task 1: 公开规划与本地状态

**Files:** Create `web-client/src/nightDayPresentation.js`、`web-client/src/useNightDayPresentation.js`；Test `web-client/unit/scene-artwork.test.js`（沿既有单测入口登记）。

**Interfaces:** `nightDayPlan(history,cursor,current,day)`→null或`{key,nightOnly,dayText}`，缺前缀返回有界诊断；`useNightDayPresentation(input)`→`{phase,artwork,dayText,continueLocal,sceneReady}`。输入只含sessionId/cursor/current/history/day/uncertain，无私有字段。

- [x] 写失败测试：Java图片/时长/格式一致；首夜及每组首事件触发、组内不触发、执行→结算仅夜、未知前缀诊断；抛错getter证明不读私有/未来胜负。
- [x] `node --test web-client/unit/night-day.test.js`确认新增断言失败。
- [x] 实现纯规划与本地状态；完成键由session+组首绝对cursor组成，同局更新不重播，换局清空。计时从图像发布起算，版本守卫取消旧回调。
- [x] 定向单测通过；本语义随Task2组件连通及分级验证后提交，避免半接入产品。

### Task 2: 显示与声音同步、失败出口

**Files:** Modify `App.vue`、`GameStage.vue`、`StageArtwork.vue`、`BgmControl.vue`、`bgmScene.js`、`soundScene.js`；必要小样式在`style.css`；Test `tests/night-day.spec.js`及`audio-tests/sound.spec.js`。

**Interfaces:** GameStage增加当前背景就绪事件；本地phase为night/dawn/day-text/event，事件文字只在event挂载。本地继续按钮独立data-testid=day-start-continue；原next-event只消费权威事件。BgmControl增加展示阶段输入，night/dawn就绪分别发NIGHTFALL/WOLF_HOWL，结果音不重复。

- [x] 失败浏览器用例：就绪后6999ms仍夜、7000ms晨；晨2499ms无字、2500ms日数；按本地继续只显示当前事件，authority/storage/writes不变。多夜死、刷新、终局仅夜、损图/晚到/挂起15秒诊断、键盘与reduced-motion均有断言。
- [x] 实现遮挡/隐藏当前结果的纯背景阶段、逐字日数和本地继续；资源失败提供显式本地退出而不确认事件。
- [x] 音频用例断言声音在背景就绪后且按阶段发生，无重复/补播；在减少动态效果下保留静态场景与原死亡帧策略。
- [x] 更新旧场景/真实旅程测试的操作前提：显式完成本地转场，再执行原业务断言；测试可用虚拟时钟推进展示，不改生产时长，不删业务/边界断言、不扩大超时凑通过。
- [x] 运行前端单测、生产构建、定向浏览器、默认浏览器、audio/assets、相关gate；核对来源oracle。实际查看夜/晨/死亡三图并附hash与环境，至少一条真实计时/生产路径冒烟。
- [ ] 验证全过后提交独立前端补修，不纳入无关原工作区修改。

### Task 3: CR处置、增量包与复玩

**Files:** Create `docs/review-300a806.md`；Modify `docs/web-playtest-verification.md`、`docs/web-playtest.md`；验收/候选入口在忽略的build/wf008目录。

- [x] 归档CR无阻断及四项P3：preview真实云端路径未来自然验证；工具包外层digest绑定写清、不改报告schema；CLI非交互confirm-page责任写清；人工来源确认继续登记。
- [x] 记录用户可闻复测与初次无声未定因；明确当前缺失转场补修和050整体外观边界；不勾A06/A07/A08等。
- [ ] 合并必要文档与前端补修后，按原授权同分支快进推送、验证新head云端full24与候选/工具包/回执复算。若新需求超出现有授权先申请。
- [ ] 交付300a806→新head增量首审包和新候选完整一局入口；用户自行确认来源、跨版隔离；不重启/覆盖旧试玩资料，不自动Ready或合流。

实施落点更正：5条夜/晨纯规划契约最终集中在现有scene-artwork.test.js，避免单独文件既被import又被通配发现导致重复计数；package.json命令和预算不变。最初失败日志仍保留原文件路径。

清单状态截至本机最终验收；云端新head、公开产物和增量交接的后续状态写入PR与交接包，不为补一个勾选制造额外未审head。
