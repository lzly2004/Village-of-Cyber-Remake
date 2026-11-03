# WEB-050 字体、语言与画面核对边界

2026-11-03本轮核对；实现仍须新候选用户完整试玩。逻辑客户区1280×720，JFrame边框与设备缩放不计入Web坐标。A06/A07仍未正式通过；“已核对”不代表跨平台像素相同。

| 场景 | 源码依据 / Web对应 | 本批核对与保留 |
|---|---|---|
| 标题、配役 | StartSceneHandler / DesktopStartScene | 原版图片、坐标/返回/CLICK契约保留；无开局API直到选择配役；同源系统字体栈 |
| 对话、日数 | UIConstants、DialogueBox、DialogueDayHandler / GameStage/useEventReading、夜晨组件 | 26逻辑字号、50ms逐字、点击补全再继续；人物与框体位置沿已确认基线，禁选/原生拖图 |
| 指示、票型 | GameSceneHandler / DesktopVoteWindow、DesktopVoteReview | 原版信息板/人物印/日历/按钮；回顾本地继续不确认处刑；刷新缺投票前快照的降级明确在外侧说明 |
| 结算 | EndAnimeHandler / SettlementBoard | 公称/真职业、死亡日/原因、存活/胜利文案保留源行为，不伪造新死因 |
| Info | UI INFO_SCENE、资源help77文件 / DesktopHelpScene | 原三级导航与背景、20逻辑按钮/24正文；36项完整日文译文替换原摘要，源中文原件与Web差异在画布外可查 |
| Record | RecordSceneHandler / RecordPage | 原标签/数字布局、34/24字号与八页循环；总览无current，五位胜率沿只读聚合，加载/错误不当0 |
| Replay列表/播放 | ReplayBrowserHandler、ReplayPlayerHandler / ReplayLibrary、DesktopReplayPlayer | 九槽/图片坐标、文件导入导出/取消不写、颜色和标记；日文角色、普通日CO和终局真职业ARIA分开 |

## 字体实际来源

本机fc-match：Takao Mincho实际落到DejaVu Sans，文件/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf（fontversion155320）；CJK回退为/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc（131137）。既有Swing采集器实际解析Dialog，不能由声明Takao推断已装明朝体。Web统一优先Takao Mincho→DejaVu Sans→Noto Sans CJK JP→系统sans-serif，与本机实际字体选择尽量接近。

出处/许可记录分别见本机/usr/share/doc/fonts-dejavu-core/copyright及/usr/share/doc/fonts-noto-cjk/copyright；本批不复制或分发任何字体文件，不凭名字认定可嵌入许可。没有@font-face、字体网络请求或font-display策略；未来若分发webfont，仍须既定许可/子集/加载前后几何/预算用户确认门。系统缺字由系统CJK回退，不承诺用户自定义Replay姓名在所有设备相同。

新验收在document.fonts.ready及素材就绪后截图，检查1440/1024/800/768、125%/150% CSS缩放及末条帮助可达、按钮返回、零开局请求/零字体传输。CSS zoom自动化不是046正式浏览器缩放、DPI、辅助技术或人工观感验收；44人列表及对话等既有几何断言继续保留。

## 语言清单与诚实边界

36项译文逐一保留source文件名，77原件及helpSourceHashes不变；游戏说明、策略、版本记录、作者/来源与引用覆盖，Web新增说明不冒充原日文。原文损坏的更新日期/人数不猜补。保存/Replay/指示旧说明与当前实际差异明确分开，不再说Replay未实现或指示只能勾选。

- UI可见角色与Replay辅助名称使用既有日文roles表；局内GameStrings直接源文（如「破绽」「第…轮」「空槽位」及「最終存活」）属于桌面字面保留项，不能以汉字检测判成中文并擅改源素材/规则文案。
- 中文原件、源台词/帮助原件、协议键/日志格式不翻写；中文原件明确lang=zh-CN。画布外的开发诊断/存储/错误仍有历史中文，完整A06清单/046复检不能被本批译文通过替代。
- title/tooltip/placeholder/aria与可见字分别核对；浏览器文件选择、下载路径及原生校验属于浏览器语言，不可保证日文，列为Web差异；不拦截浏览器复制或修改全页交互。
- 无人CO、失败/不确定、只读面板、迁移期间CO禁用等既有契约保持。猎人AI日记疑点只登记，不改变说明或投影去掩盖它。

逐PNG哈希侧车在build/verify/desktop-closeout，旧Swing源采集在build/web050/desktop-parity-baseline；同一截图既不能冒充源Swing，也不能冒充用户目视。必要浏览器差异继续由场景矩阵记录并交用户确认。
