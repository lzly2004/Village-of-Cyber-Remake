# d743b22 Review 核验与 WEB-024 接续

## 评审结论与处置

比较`280613b..d743b22`。采纳无阻断、无路径修改；reviewer独立完成49项单测、构建/资源门禁、静态3项、默认71项及完整七配役功能门15项（3.3分钟）。后者补上023 coder仅--list的范围，本轮不冒充旧15项为024重跑。

- 44人物/职业来源、公开字段白名单、CO范围/死亡通用标记、内容路径/解码/加载安全及预算已独立验证；024复用，不重新编码或复制静态资源。
- playerVisual重复计算目前无性能问题，保留可选观察，不借024重构玩家行。
- 未CO状态/共有短全称统一归029；046浏览器/DPI/辅助技术、A06/A07/A08与公网日志/容量门槛维持。
- 接受共享展示应追加定向gate的建议：024将更新gate对话立绘检查并实际执行相关功能门；不只做用例发现，也不把前端方向门扩大为未经授权的Java完整test/Fat JAR。

## WEB-024 范围与证据

复用原事件图片JSON与022 WebP，按桌面DialogueDayHandler/DialogueAfternoonHandler/UIHelpers对话分支实现单/双人物与三连换位，保留原生台词及确认门槛；NIGHT_RESULT/EXECUTION/SETTLEMENT视觉和资源数组的死亡切换仍归025。来源及异常边界见[事件舞台契约](web-event-artwork.md)。

开工按纯前端共享结构分级：定向映射/历史/缓存单测、构建/静态预算、相关浏览器/首帧与最终默认回归、相关功能门。单worker/JVM；不改规则、Java、API、随机源或存储格式，不改系统/审批/swap/磁盘。

首次从根目录误调npm build为ENOENT，未构建；随后在web-client正确构建及静态3项通过。开发中加强误标对话阶段数组诊断及ch3/private getter负例后，映射10/10再次通过。实际API浏览器首次启动被安全入口以MemAvailable<2GiB、exit75拒绝（未运行测试、未启动游戏JVM），只继续轻量核验；用户表示会释放内存，未降低门槛或结束其他应用。

## WEB-024 提交前实际验证

用户释放内存后MemAvailable约3.8GiB，使用既有安全入口重试定向套件通过；随后串行完成以下验证。所有浏览器配置为单worker、retries=0；未运行Java完整test/check、重新生成oracle或Fat JAR。

| 命令 | 实际结果 |
|---|---|
| `npm run test:unit` | 最终59/59，0失败，1.596秒；含误标对话阶段数组明确诊断、读取ch3/private getter抛错负例 |
| `npm run test:e2e -- event-artwork.spec.js stage-layout.spec.js` | 14/14，15.3秒；含实际API/刷新、四种窗口、精确special、双人/三连、补读失败/晚到/revision不同、损图与原D1阅读区稳定检查 |
| `npm run build` | prebuild/check、Vite、postbuild/distribution通过；JS452.47kB/gzip114.03kB，CSS15.75kB/gzip4.28kB；全部代码468,820字节/gzip117,216，静态总量22,224,930字节 |
| `npm run test:assets` | 3/3，6.3秒；冷大厅119,474字节，首局新增397,068字节；初始17张（含当前背景/立绘2张），远端玩家仍按需加载；1164张唯一图完整解码 |
| `npm run test:e2e` | 最终默认80/80，1.8分钟；共享恢复/CO中断/全部操作/终局门槛未减弱 |
| `npm run test:gate` 首次 | 前三项jianyi/designated、jianyi/mixed、tongchang/designated通过；运行中监护输出`ABORT: MemAvailable fell below 2 GiB`后停止自身JVM，后12项因API服务断开失败，整体exit1（50.4秒）。保留这次环境中止证据，不归为已通过或业务断言缺陷 |
| `npm run test:gate` 获准额外重试 | 用户回复“好”批准额外一次重试；启动前MemAvailable约6.7GiB，沿用安全入口，最终15/15通过（3.8分钟），exit0；七配役双策略14完整局及真实CO中断均含逐条对话实际图片/解码检查。这是024实际重跑，不引用023旧15/15补足 |

原图片/清单未变化：1171路径、1164唯一WebP、21,291,852字节，manifest464,258字节。没有重新生成或提交素材。gate核对同源Java指纹；概率配置、build.gradle及Gradle参数无语义差异（保留已有CRLF差异）。只读抽查15份既有oracle的111个非空命令窗口，未见跨窗口“左起始→连接事件”的组合；这不构成任意合成事件或未来生产者的布局证明。

首帧实测：受控阻塞第二人物时，至少连续8个RAF帧没有半组人物，显示加载状态；释放后两人整组出现；返回已解码图片的下一事件，前8帧均有完整单人组且无加载状态，同一图片仅请求一次。定向与默认各执行一次该用例；未执行额外5次重复，不扩称所有浏览器/设备无闪烁。已人工查看composition-0/2/5截图，核对单人/双人/三连左右换位、背景与文字覆盖。截图为忽略目录test-results的可再生验收产物。

内存中止后只读核查：MemAvailable回升约3.3GiB，未见本轮JVM、Chromium、Playwright或Vite残留；没有全机批量kill或修改系统设置。当时按AGENTS重试上限停止，保持DOING且未提交，等待用户明确批准。用户释放更多内存并批准额外重试后，仅补跑上述功能门，未再改生产逻辑或机械重复已成功的单测/构建/默认回归。重试退出后MemAvailable约6.7GiB，`ps -C java,node,chrome,chromium`无匹配进程；未遗留本轮JVM/浏览器/Vite。

相关跟踪差异`git diff --check`及新增文件行尾检查通过；仅暂存024实现、测试和相关文档，保留既有Game.java空白、CRLF、.workbuddy、NUL等无关修改。WEB-024按分级验收完成，下一项仍为WEB-025；A06/A07/A08未放行，正式支持浏览器/DPI及所有设备首帧表现、公网日志与容量门槛仍待原队列。
