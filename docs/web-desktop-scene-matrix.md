# 桌面场景与动作对照（WEB-050）

当前版本与交付状态见[迁移收尾状态](web-migration-status-2026-11-11.md)；下列历史测试计数仅适用于各自批次。Replay头像免滚动、中文INFO、淡色护卫及纯窗口模式已随f754a07统一交付并得到用户整局总体反馈；终局夜实际遇到且正常。回放提示补修已随fe64e1c由用户局部验收成功。本轮默认纯窗口入口按用户确认规则实施，其人工验收仍待，不将旧反馈记到新head。

初始场景闭环基线：`3c3ffda`；误选和Info/Record批次历史基线为`4497368`。目标仍是启动—终局—标题—再次开局闭环。状态按实现/验证分别更新，计划不等于通过。每行同时核对显示、动作、声音和返回。桌面源码位于 `src/main/java/com/vocr/ui/`，素材名位于 `com/vocr/resources/UiImage.java`。

| ID / 测试 | 桌面入口 / 源码符号 | 显示、动作、声音与返回 | Web 对应 / 当前状态 |
|---|---|---|---|
| game-only / default canvas | 浏览器包装边界，复用已验收原画布 | 首次/刷新默认仅游戏；Esc主动恢复外围，原生确认先取消；场景往返/异常不切模式，声音及会话保持挂载 | App.gameOnly；game-only.spec专用base fixture验证默认几何、刷新、原生弹窗/重复键、声音恢复、终局→菜单→第二局；本轮实施，人工待确认 |
| scene-title | StartSceneHandler.render | TITLE_BACKGROUND、TITLE_LOGO；START_MENU BGM；六按钮按插入顺序，见 UIConstants.START_BTN_* | DesktopStartScene，已实现；scene-title / 最终16项专项 |
| scene-select | StartSceneHandler.render.btnStart → GameSceneHandler.renderSelect | 无CLICK（setButtonSceneTransitionNoSound）；七配役图片按钮；返回图片1050,560，116×76；选择时CLICK并且创建一局 | DesktopStartScene，已实现；scene-title / 最终16项专项 |
| scene-noop | StartSceneHandler.render.btnContinue/btnCollections | 可见可点，CLICK后空操作；不伪造读档/图鉴 | 已实现；scene-title / scene-entry-return |
| scene-replay-return | StartSceneHandler.render.btnSave / ReplayBrowserHandler.doRender | CLICK进入录像；返回标题。三槽/页、九个固定槽；左键已占槽播放，空槽不响；右键确认保存，取消零写入；原版翻页/标题图片 | ReplayLibrary核心场景已实现；desktop-replay / replay-slots；f754a07 full24通过并已交付；文件/回放专项历史证据保留 |
| replay-player-native | ReplayPlayerHandler / ReplayTextFormatter | 原版背景、两排头像/结果球、日期牌、信息/票型/终局职业、按日/按轮及返回 | 主菜单进入DesktopReplayPlayer；desktop-replay覆盖日期锁、44人末项和返回焦点；f754a07已实际回放；残留提示已随fe64e1c局部验收通过，046正式组合验收仍开放 |
| native-selection | GameStage / DesktopHelpScene / RecordPage | 01游戏画布禁止原生文本误选与图片拖动；明确保留指定工具拖放、表单编辑和画布外诊断复制 | desktop-info-record / desktop-voting-parity；不全页拦截复制/右键 |
| dialogue-frame | DialogueController / DayStartAnime | 框体首次补全文字、再次推进；日数继续只属本地；无附加续读按钮或图片比例标签 | GameStage.message-surface / night-day / desktop-dialogue；不确认或伪造服务器事件 |
| replay-file-native | ReplayFileDialogs.addButtons/importFile/exportFile | 源x90/300,y20,190×40；导入选录像→目标槽→覆盖确认；导出一局到九槽文件0位；无CLICK；取消零写入 | DesktopReplayExchange；desktop-replay原位按钮、取消、配额回滚与显式重试、导出原文和零API断言；浏览器批量管理留核心外 |
| scene-record-return | StartSceneHandler.render.btnRecord / RecordSceneHandler | CLICK进入战绩；返回标题，不能删除本地记录 | RecordPage.desktop：01内原版标题/数据图片与坐标，Next循环八页、Return回总览、Title回标题；浏览器刷新说明在画布外。desktop-info-record / statistics / scene-entry-return，本机分级及f754a07 full24已验；用户总体反馈不等于逐页人工验收 |
| scene-help-return | StartSceneHandler.render.btnInfo / InfoSceneHandler | CLICK进入说明、INFO BGM；返回START_MENU | DesktopHelpScene：01内PVBG/原面板、五分类→主题→正文、原版返回/标题坐标与INFO BGM；help / desktop-info-record。2026-11-10起直接复用桌面中文分类、主题和正文，移除独立日文译文；配役及Web差异单列；完整语言人工复检仍开放 |
| scene-live-title | VoteButtonPanel.create.menuBtn | 直接回START_SCENE，无确认；不确认事件、不投票 | 已改为直接标题；scene-live-title / scene-finished-title-second-game |
| scene-finished-title | EndAnimeHandler.render.nextBtn | 直接回START_SCENE；不弹活局退出确认 | 已改为直接标题；scene-live-title / scene-finished-title-second-game |
| claim-empty-role | GameSceneVoteHandler.handleRoleCo | 请求成功events为空→GameStrings对应职业提示；createTishi(300,300,500,200)、1000ms非阻塞；CLICK | 已修复；scene-claim-role-empty-* / scene-claim-events / scene-claim-error-* |
| claim-empty-players | GameSceneVoteHandler.handleReturnButton | 选人后返回提交一次；空events→MSG_NO_CO「無人CO」；非空进入对话 | 已修复；scene-claim-players-empty |
| claim-failure | Web独有网络错误 | 请求失败不是无人CO，保留错误/不确定/同步语义；不自动重试，不确认游标 | 保留useGuestGame.handleError；scene-claim-error |
| scene-second-game | GameSceneHandler.renderSelect.startNewGame | 再次选择配役创建新局，进入夜/晨/对话；CLICK | 已验证：scene-finished-title-second-game / scene-replacement-failure；真实seed4201 gate追加同一闭环 |
| game-only | Web过渡期入口 / App.enterGameOnly、gameOnlyEscape | 原1280×720画布居中等比适配；外围与入口隐藏；Esc恢复且不串关闭子场景；必要音频/恢复在画布内 | 已实现；game-only.spec四项、f754a07 full24及用户总体试玩反馈；真实跨浏览器/DPI仍归046 |
| replay-save-notice | Web本地保存反馈 / ReplayLibrary.playEntry | 保存成功消息属于槽位操作；进入播放时消费，切日及返回不复活；错误反馈保留 | f6e09f5补修已随fe64e1c交付；desktop-replay.spec正常/纯窗口新增两例；用户已局部验收成功，销项 |
| release-default-game-only | 正式Web入口（用户最终呈现要求） | 默认只呈现游戏画面，过渡外围仅属开发期工具 | 98b6713已本地实现并完成分级验证；尚未推送或生成新试玩候选，人工确认待；验证边界见[显示记录](web-game-only-display.md)末节 |

## 必须保留的浏览器差异（非桌面豁免）

| 差异 | 原因 / 行为 | A/Q影响及状态 |
|---|---|---|
| 游客生命周期 | 本地主菜单不释放也不推进活局；再次选配役先DELETE旧游客局，成功才POST。失败保留凭据、显示诊断；不自动重发。终局记录组件保持挂载，不隐式保存Replay | Q04/A12仍开放；已自动验证；仍待用户体验确认 |
| 恢复入口 | 桌面Continue原本为空操作，不能借此伪造续局；浏览器恢复仍由sessionStorage和外部明确恢复按钮完成 | A12；已验证（scene-live-title / scene-entry-return） |
| Replay存储/浏览器管理 | IndexedDB v2新增独立九槽映射，旧录像原样迁移；同一录像可由多个槽引用。管理区按内容去重，删除该录像会清空引用它的槽；导出保持既有九元素交换格式，整库按管理顺序导出，不承诺保存本地空洞布局。管理区导入/搜索/诊断及原有详细检查播放器位于核心场景外；主菜单槽位播放使用桌面场景 | A09/A10继续按原格式验证；A06/A07尚未通过，战绩/说明主布局本批对齐，完整语言/字体门仍开放 |
| 声音手势 | 初始无声，用户启用；浏览器后台停止；主菜单/帮助/对局BGM互斥 | A08仍未通过 |
| 既有适配 | 1280×720逻辑坐标、窄屏按同一16:9等比缩放、长内容内部/页面滚动；字体系统回退；指示/记录同板；头像防裁切适配 | A07/046继续开放；不提高预算、不删反裁切断言 |
| 战绩/说明的浏览器补充 | IDB数据范围、刷新/错误说明、配役清单与中文来源保存在画布外默认折叠区；Escape回进入前场景，原版Title按钮回标题；活局不被销毁或推进 | A06/A07不因此放行；源中文帮助保留；历史语言/字体核对见[记录](web-desktop-language-font-audit.md)，其中日文帮助译文方案已由直接复用桌面中文替代；字体/DPI仍开放 |

## 验证与交付

实际命令/结果/失败记录集中于web-desktop-core-window.md及build/web050/scene-loop/local-validation.json；当时scene-loop阶段本机220单测、61受影响回归（历史输入；后续224及最终本批结果见core-window追加记录），当时其余16末次专项、gate15、图片5、音频30、生产整局1均通过。修复前默认232/1保留，不冒充最终完整默认全绿。截图索引在build/verify/desktop-scene-loop，真实seed4201的标题/第二局在build/verify/desktop-core。云端最终SHA验证待发布后另列；无人CO与终局返回已有自动修复证据，仍待用户新候选整局体验销项。CI绿灯、CR无阻断不替代用户完整试玩；PR #13保持草稿，044不启动。

2026-11-02误选与Info/Record追加批次的实际分级结果、失败处置和未验证范围，统一见web-desktop-core-window.md末节；不把4497368旧候选当新交付。

2026-11-03：完整刷新投票回顾无投票前快照时，省略未知人物/人数并在画布外说明，不用最新处刑后快照冒充；响应丢失但未刷新时沿用内存中原先已展示的公开状态。继续不写游标、不重发命令。A07/恢复差异需用户新候选确认。

## 本轮收尾基线（2026-11-19）

上文标明日期/计数的段落是历史批次证据。当前已交付且用户已正常整局试玩的是f754a07，云端run44329192581/full24通过；用户表示整体无大问题，并确认实际夜间终局直接胜利正常，新增唯一反馈为回放保存提示残留。fe64e1c已局部验收通过；本轮默认纯窗口入口及验证边界见[窗口记录](web-game-only-display.md)末节及当前[收尾清单](web-migration-status-2026-11-11.md)。不再笼统记“最新候选未试玩”，也不将总体反馈扩写成所有场景人工逐项销项。默认纯窗口入口本轮实施后仍待新候选人工确认；046语言/字体/浏览器/DPI/键盘/音频和其他A/Q门仍开放；PR未Ready/合流；2026-11-20用户另授权044先行，050交付暂缓，最新状态见收尾清单。
