# WEB-050 review closeout Implementation Plan

> **For agentic workers:** 当前Coder按任务直接执行；用户已明确要求依次完成并交试玩，不重复方案审批，不调用子agent。

**Goal:** 处置4497368评审与本地04d410d差异，补齐Replay可访问名称、恢复/重开边界和场景语言核对，交付真实新head候选。
**Architecture:** 保持生产Java、规则/API、IDB和录像交换格式不变。先测试恢复与并发边界，再按已证实缺陷做最小前端修复；沿用共享日文角色标签和现有资源/系统字体，不引入字体依赖或抬预算。
**Tech Stack:** Vue3、Playwright单worker、Node tests、现有full24 CI与自助试玩工具。
**Spec:** 用户批准2026-11-03五步收尾方案并要求直接执行至试玩；CR4497368报告；猎人调查DEFERRED。

## Global Constraints
- PR #13草稿、不Ready/合流、不进入044；允许本轮修复、既有PR更新/云端产物和最终自助入口。
- 不改变猎人AI或任何生产Java/协议/存储格式，不增加盲复盘玩法；原版复盘彩色头像保留。
- 本机按纯前端分级：unit/build/相关浏览器、共享交互默认回归、相关oracle/gate；不运行未单独批准的本机full/check/FatJAR。云端实际累计路径full24。
- 2GiB门槛、单worker、不并行JVM或浏览器套件、不清用户profile锁或杀用户程序。
- 旧220/224与不同SHA证据按历史归属标注，不改历史数字冒充同轮；PNG用现有哈希索引，不修改evidence schema。

## Review Focus
- 终局前ARIA不能多读出精确真职业；终局后与可见真职业一致，原彩色头像不改。
- 刷新/响应丢失恢复投票结果，不自动确认处刑事件或投票，不显示未来日状态。
- 延迟DELETE/POST期间鼠标双击/键盘不重复释放或创建；失败不假成功，不自动重发。
- 日文标签、帮助内容、字体回退、125%/150%及800/768视口不隐藏末项；原文/译文/浏览器差异明确。
- 最新head的CI、包、回执、工具与启动命令必须一致，旧候选不替代新候选。

### Task 1: Replay information boundary
Files: DesktopReplayPlayer.vue、labels.js、tests/desktop-replay.spec.js。
- [x] 新增普通日ARIA无精确真职业、终局ARIA有日文真职业用例，先复现。
- [x] 复用roles/replayRoles，ARIA从当前frame人物与可见CO生成；终局用当前frame.actualRole，禁止按数组位置读第0日真身份。
- [x] 核对源ReplayPlayerHandler彩色头像契约，保持所有图片/交换断言。

### Task 2: Recovery and replacement
Files: App.vue、useGuestGame.js、tests/desktop-vote-review-public-state.spec.js、desktop-scene-loop.spec.js。
- [x] 增加刷新恢复/丢失投票响应后的回顾测试；保持游标零变化与零重复命令；按实际失败修复fallback投票前展示，不能猜造权威状态。
- [x] 增加延迟DELETE/POST双击与键盘测试；仅复现异常才加replacement guard，保留DELETE失败阻断创建语义。
- [x] 记录服务端VoteResultProjector每日至多一条与VoteState完成禁覆盖契约，不为假设改服务端。

### Task 3: Visual/language and evidence closeout
Files: helpContent.js/相关Web显示、desktop CSS、docs/web-desktop-scene-matrix.md、core-window、review-4497368.md。
- [x] 主菜单/配役/对话/决策/结算/Info/Record/Replay逐场景核对源码、字体、文案、动作和声音；保留原版图片与明确浏览器差异。
- [x] 核对完整帮助内容覆盖与源文本，日文为译文，中文原件保持；剩余中文按原版/来源原件/内部诊断分类，不能以检测汉字等同中文残留。
- [x] 使用现有系统字体，不分发未审字体，检查字体完成后实际几何、窄屏与缩放；准确记录跨平台人工门。
- [x] 历史/当前数字分开；归档CR九项处置；旧snapshot等价证明及逐PNG索引纳入可达交接。

### Task 4: Validate, publish, playtest
- [x] 在最终输入串行运行unit、build、相关/默认浏览器、assets/audio和实际需要的gate，保存失败；全部通过后主题提交。
- [ ] 推送同分支更新PR #13草稿，核验真实full24与verify/receipt/release；不修改设置或部署。
- [ ] 逐字节核对发布ZIP/清单/运行时stamp，准备同head工具/入口，给用户正常完整一局命令；不代确认首次来源、不假称用户已玩。

## Self-review
本轮是原050反馈补修；无新增规则/存储、预算或第三方依赖。各边界测试先于修复，最后一个候选绑定同一head。用户显式要求完成至试玩，已有执行方式，无进一步设计确认节点。

外部发布之后的精确SHA、run/attempt、产物身份及最终试玩命令记录于当轮publication/review-handoff.md，不为回填本文件而追加未审新head。
