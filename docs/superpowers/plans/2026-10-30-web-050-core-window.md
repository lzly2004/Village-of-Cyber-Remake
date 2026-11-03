# WEB-050 核心窗口完整试玩候选 Implementation Plan

> **For agentic workers:** 按用户明确要求由当前 Coder 直接实施；独立 CR 仍由用户转交，任何发布/合流须另获具体授权。

**Goal:** 把已完成但未交付的B1接入新候选，优先实现桌面完整结算，再实现投票窗口，避免用户重复试玩117eda9旧包。

**Architecture:** 沿用Vue公开投影和现有命令。结算组件仅在FINISHED且没有当前事件时挂载；纯展示函数逐字段对照EndAnimeHandler/GameStrings。投票布局重用现有选择及命令，不新增API、私有数据或存储格式。

**Tech Stack:** Vue3、原版图片、CSS、Node test、Playwright，单worker。

**Spec:** ../../web-desktop-visual-alignment.md；用户2026-10-30已批准核心窗口优先、接上B1后交付新候选。

## Global Constraints
- 对话框逻辑(260,490,760,230)，50ms逐字，保持已有夜晨时序。
- 结算仅在FINISHED且无未读事件后显示；不根据存活状态猜测死因。
- 字体分发/日文化后续门保持开放；本批不引依赖、不改规则/API/预算。
- 所有角色字段完整可读，44人末项和键盘可达；不以缩小字体或裁切信息换取一屏。
- 完成相关单测/构建/定向/默认/gate/资源及音频验证后才能提交产品；不冒充本机全量或云端CI。

## Review Focus
- 终局有未读事件：不能读revealedRoles或挂载结算组件（Task1/2）。
- 呪殺/后追/猫呪与公开NIGHT_DEATH：精确死因按终局字段，未知显式诊断（Task1）。
- 狼胜/狐胜仍alive者：桌面最終死亡/最終胜利与alive不同，不伪造死亡日（Task1）。
- 44人与长角色名：两行横向浏览、卡片子项完整、末项可达（Task2）。
- 投票在busy/uncertain/回避后：沿用最新合法候选及命令守卫（Task3）。

### Task1: 终局信息映射
Files: web-client/src/settlementVisual.js; web-client/unit/scene-artwork.test.js。
- [x] 写并运行失败单测：七精确死因、无CO/真实身份、三阵营存活结局、未知字段、未揭晓毒getter。
- [x] 实现settlementDetails(finished,player,actualRole,result)，返回claim/actual/fate/diagnostic；非finished不读输入；角色/死因白名单。
- [x] 运行对应Node测试，逐项对照EndAnimeHandler/GameStrings，不修改服务端。

### Task2: 桌面完整结算画面
Files: create web-client/src/SettlementBoard.vue; modify App.vue/desktop-dialogue.css; tests/scene-artwork.spec.js。
- [x] 用终局三阵营夹具写断言：全部字段、揭晓门、未知诊断、刷新零写入。
- [x] 结算占完整舞台，胜利图标/标题在上，人物两行，每卡含头像、公称/真职业和结局；取消旧底部绿色信息框，保留可访问诊断。
- [x] 1280/1024/800下测量每卡和子项无纵向裁切，44人键盘横向到末项并截图；对照真实桌面基线。

### Task3: 桌面投票窗口
Files: App.vue; DesktopVoteWindow.vue; GameOrders.vue; PublicInformationPanel.vue; desktop-dialogue.css; tests/guest-game.spec.js、stage-layout.spec.js、scene-artwork.spec.js。
- [x] 对照GameSceneVoteHandler/PlayerStatusRenderer/VoteButtonPanel的坐标和功能，记录实际迁移映射。
- [x] 将公开人物、信息、投票及CO/技能入口整合到游戏窗口，重用原命令/选择对象，避免重复可交互副本。
- [x] 定向验证所有投票方式、回避、CO、技能、历史及忙碌/不确定路径；多人长文无裁切。

### Task4: 同版交付
- [x] 跑最终分级套件并实际目检截图，保留失败日志；同步任务状态、边界与CR重点。
- [ ] 申请必要全量/打包、发布授权，生成绑定新head的可试玩候选，不替换旧包目录伪装身份。
- [ ] 用户完整试玩与独立CR均完成并处置后，才申请Ready/合流。

Self-review: 数据边界、三类终局、未知输入、44人、投票守卫均有对应任务；B1已存在，未声称本计划已经验证或发布。
