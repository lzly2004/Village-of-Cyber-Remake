# WEB-050 试玩可靠性、对话与 Replay 对齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 本轮按用户要求由 Coder 在当前会话直接实施，统一交付一个候选。

**Goal:** 修复 f961bee 试玩暴露的运行中止反馈与清理缺口，统一框体点击，并对齐 Replay 列表和复盘场景。

**Architecture:** 保留游客/API/Replay 格式与安全门槛；运行可靠性只改试玩工具和既有进程监护。对话框共用本地点击分流；Replay 使用既有校验/投影/存储，替换桌面场景显示，浏览器交换管理单独展开。

**Tech Stack:** Python/Linux waitid/subreaper，Vue/IndexedDB/Playwright，现有原版图片与桌面 Java 源码作为基线。

**Spec:** 用户本轮四项试玩反馈及追加「删除多人对话1/2、2/2标识」；桌面源码 ReplayBrowserHandler、ReplayPlayerHandler、UIConstants、GameSceneHandler；docs/web-desktop-scene-matrix.md。

## Global Constraints

- 基线 f961bee，现有 PR #13 保持草稿；不合流、不进入044，不变更系统信任/设置/secret。
- 单 worker/JVM；MemAvailable < 2 GiB 不启动，运行中门槛不降低；用户2026-11-02已明确授权本轮完整本机全量/打包；不复用于后续轮次。
- 监护信号仅经 waitid 核实自身未回收直接子进程，不扫全机/不发进程组信号/不根据历史PID自动杀进程，不杀浏览器或自动清锁。
- 不改 Java 生产源码、规则、对外 API、Replay交换格式、资源预算或黄金摘要。用户已批准新增本地九槽映射；旧录像内容不变，现有库迁移不删除录像，覆盖须明确确认。
- 所有可见对话框点击：未显示完先补全，已完成再推进；日数继续不确认权威事件；不显示额外续读/图片切换/分段比例标识。
- Replay 管理页的Web额外功能可独立显示，但核心列表/播放器按源端素材、坐标、动作、声音和返回核对；不借布局改造删除信息边界断言。

## Review Focus

- 协调器意外退出：监护能接到父进程消失并清理已拥有的服务，外部哨兵/浏览器不受影响。
- 低内存：明确停止原因与重启失效提示，不把守护中止当规则死循环；刷新仍能获得试玩停止说明，不能暗示可恢复活局。
- 日数/多阶段事件：首次补全不发请求，后续点击严格按本地片段/权威事件顺序推进，双击/故障/键盘无重复提交。
- Replay：坏库/满库/重复/确认取消/保存中换局不覆写；浏览复盘不推进活局或重复入账。
- 长内容/44人/缺失旧录像字段：两段头像、日期/投票翻页/角色揭示可达，无未经授权信息；旧诊断保留在核心画面外可达位置。

### Task 1: 试玩进程与停止反馈

**Files:** tools/guarded-process.py、tools/playtest_runtime.py；新增tools/playtest_stopped.py；tools/tests/test_guarded_process.py、tools/tests/test_playtest.py、tools/playtest_bundle.py。

**Interfaces:** 监护新增可验证父进程终止绑定，仍通过run(command)；停止说明服务仅绑定127.0.0.1:18500，只读GET/HEAD说明，其他方法明确503拒绝，原因白名单，不转发API、不记录凭据。status/stop保留现有CLI。

- [x] 先运行既有隔离监护回归；新增协调器退出/外部哨兵存活/父进程竞态与假PID不发信号测试。
- [x] 实施父进程终止通知与退出状态诚实性；协调器存活时低内存退出显示明确停止说明，可由status/stop关闭，不自动启动JVM重试。
- [x] 测试低内存/中断/端口冲突/残留记录/来源边界；不以真实桌面或故意OOM实验。
- [x] 运行python3 -B tools/tests/test_guarded_process.py -v和test_playtest.py；成功路径已随最终full24完成真实托管冒烟；全量已获当轮明确授权。

### Task 2: 框体点击与无附加UI

**Files:** web-client/src/{App.vue,GameStage.vue,StageArtwork.vue,useNightDayPresentation.js,desktop-dialogue.css}；相关测试与test-fixtures/presentation-test.js。

**Interfaces:** GameStage的read-continue统一分发到日数/错误/事件处理；continueLocal在文字未完成时补全；保留无可见文字的语义键盘入口与既有测试语义，框体空白/标题区均可点。

- [x] 新增日数首次补全/二次继续零请求、普通事件首次补全/二次权威确认、处刑多段、框体空白点击、Enter/Space、无比例标签断言。
- [x] 移除可见続きを読む、画像を切替按钮；保留画面data-art-frame便于无视觉污染的回归核验。
- [x] 更新现有夹具为实际框体动作，不通过隐藏按钮自动推进，重跑相关对话/夜晨/声音与默认回归。

### Task 3: Replay 桌面列表与复盘

**Files:** web-client/src/{ReplayLibrary.vue,ReplayPlayer.vue,ReplayInformation.vue,ReplayEntry.vue,replayPlayback.js}，新增desktop-replay.css、DesktopReplayPlayer.vue、DesktopReplayExchange.vue；replay相关浏览器/单元测试；docs/web-desktop-scene-matrix.md。

**Interfaces:** 保留createReplayStore/readCompletedReplay/readReplayFile/validateReplay，核心展示消费既有entry/frame，录像内容和交换格式不变；IndexedDB升至v2增加独立slot-layout元数据，旧条目原样迁移。桌面列表每页3槽、翻页/返回素材，播放器复用原始日期和记录布局。浏览器导入/导出/诊断在独立管理区域。

- [x] 逐条读取ReplayBrowserHandler/ReplayPlayerHandler/ReplayTextFormatter，记录源码坐标与空槽/已占槽点击和声音；补成对基线。
- [x] 用真实IDB/固定合法Replay建立保存、分页、播放、按日/按轮、终局角色、返回、取消与重复/坏库负例。
- [x] 对齐列表和复盘核心场景；保存覆盖语义不得静默改变既有库，浏览器与源端不一致的必要差异明示，不伪造槽位能力。
- [x] 运行完整Replay相关测试和桌面交换，检查1440/1024/800及44人，不删除末项可达/无副作用/格式兼容断言。

### Task 4: 统一交付

**Files:** 更新docs/web-desktop-core-window.md、场景矩阵和任务清单；当轮build证据目录。

- [x] 单测/构建/默认浏览器/资源/音频/相关完整gate按最终输入串行验证，失败分开保留。
- [x] 共享监护/工具改动完成后准备隔离LF快照与具体命令，再申请本轮最终全量/打包授权。
- [x] 所有本机验收通过，按监护/对话/Replay/文档四个主题保存本地提交；本批扩大范围的推送/产物发布单独提出具体申请。
- [ ] 最终同SHA云端证据、回执、发布ZIP与试玩工具核验；交付一个自助完整试玩入口与增量CR包，不以自动通过替代用户试玩。

## Self-review

以上任务覆盖用户四点和额外比例标识；运行风险先验，界面对齐不削弱信息/存储契约。父进程终止绑定无法处理监护自身SIGKILL，仍明确保留该限制；不会声称不存在任何残留可能。

## 最终本机结果

2026-11-02隔离LF快照6516ae1，full24全部通过；Java650、单测224、默认245、gate15，解包整局1通过，详情见docs/web-desktop-core-window.md。两轮失败记录独立保留；最终交付head的云端验证、用户完整试玩与CR尚待。无推送/Ready/合流/部署。
