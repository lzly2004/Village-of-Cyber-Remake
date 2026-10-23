# 电脑村 · Vue 文本试玩

本机 HTTP 原型的最小客户端。所有玩家状态、合法候补、票型与胜负来自 Java 应用快照。
目前支持七配役开局、逐角色日文台词、按职业/玩家询问 CO、投票回避开关、指定/自由/灰区投票、全体/单个公开CO行动者占卜与护卫预约、公开报告/疑い先/死亡历史、全日全轮票型和终局揭晓。
事件直接复用桌面 event_lines.json，按 ch1/eventname 与 ch2 special 选择，三类名字参数均取 ch2 片假名；保留换行并纯文本渲染。wsw 使用系统 NO_SACRIFICE；NONE、缺失/空台词等异常显示明确标注的界面诊断，仍需逐条确认。021已加入1280×720舞台并等比缩放，长文字、玩家列表与个体预约列表可用键盘滚动，个体预约可按行动者折叠。022的WebP选集已用于023玩家头像、称号及公开CO/死亡图标，列表支持日文人物显示名与按需加载；024已接入原事件映射的日间/下午对话立绘、背景及连贯双人布局，刷新时仅补读公开呈现前缀，详见[事件舞台契约](../docs/web-event-artwork.md)。025已接[死亡/处刑/结算场景](../docs/web-settlement-scenes.md)，画面切换不推进事件，全部确认后才显示职业头像，异常揭晓明确诊断。027已接入BGM手势启用、场景选曲与释放；028已接音效、独立音量、静音与本地设置；029已接入日文菜单、五类36项帮助要约/中文原文及七配役说明，来源与Web差异见[帮助契约](../docs/web-japanese-help.md)；A06仍待人工校对与剩余文字复检。

## 开发与构建

需要 Java 21 与 Node.js 22.12+。依赖固定为 Vue 3.5.44、Vite 8.3.5、
Vue 插件 6.0.10 和 Playwright 1.64.1，完整依赖树由 package-lock.json 锁定。

终端一在仓库根目录启动服务：

```sh
./gradlew runWeb
# Windows: gradlew.bat runWeb
```

终端二在本目录运行：

```sh
npm ci
npm run dev
```

访问终端输出的 `http://127.0.0.1:5173`。Vite 将同源 `/api` 请求代理至
`http://127.0.0.1:7070`；后端改端口时可用 `VOCR_API_URL` 指定代理地址。
页面读取真实 Java 服务，不包含本地规则引擎。

图片日常校验只需Python3与能解码WebP的Pillow（reviewer已实测系统Python3.8/Pillow7）。
重新生成图片才需[固定编码依赖](../tools/requirements-web-images.txt)及同版libwebp，详见资源契约。
`npm run assets:check`验证当前1171条图片清单、源/输出哈希、解码与预算；
`npm run build`前后自动核对public/dist资源，并统计含台词的全部代码预算，任何缺失或漂移都会失败。
生成、边界和画质约定见[图片资源契约](../docs/web-image-assets.md)。
`npm run test:assets`只启动静态preview，验证全部唯一图片解码、大厅无玩家素材请求以及开村后仅按可视名单加载和传输预算，不需要Java。

026已交付23首BGM/9音效的独立MP3、13份说明、内容哈希和按需资源接口；027在App接入BGM。初始无音，点击侧栏「音楽を有効にする」后才读取当前曲；白天可决策时「曲を切替」按原列表轮换。停止、后台与刷新不自动恢复，详见[BGM契约](../docs/web-bgm-playback.md)。
`npm run audio:check`及`npm run build`音频前后门禁还需要**完整FFmpeg**（含MP3解码、astats/asdr；重新生成需libmp3lame），以`FFMPEG`指定路径；Playwright自带视频FFmpeg不可替代。
可用Python≥3.9隔离安装[固定开发工具](../tools/requirements-web-audio.txt)，不需要系统安装或生产依赖变更；安装/复现及预算见[音频资源契约](../docs/web-audio-assets.md)。
`npm run test:audio`仅静态preview，逐文件完整解码并验证按需请求、哈希异常/取消及真实BGM媒体播放、循环、切换和释放，不启动JVM；同时检查音效ended/第二帧、混音节点、静音与存储恢复；headless播放不等于人工听审。
资源模块导入不请求网络；BGM/单路音效满档各0.25增益，为026峰值留混音余量。028已接[音效/设置](../docs/web-audio-settings.md)：侧栏两条音量与消音，vocr.audio.v1仅保存数值，不保存启用状态；刷新仍须手势。人工听感、物理设备和无缝拼接未放行，A08未通过。

`npm run build` 输出 `dist/`。这是客户端静态产物；本批只配置开发期代理，
生产部署仍需同源 API 反向代理，不能单独双击 HTML 游玩。
`npm run preview` 仅预览静态构建，未配置生产 API 托管。

## 交互与恢复

- 待确认事件按服务器提供的展示阶段逐条显示。确认一条后才递增已确认游标，读完才能投票。
- 选择与投票使用服务器最新 revision；成功响应更新权威状态，按钮在请求期间禁用。
- 409 只触发读取同步，不自动重试写命令；玩家确认新发言与候补后再操作。
- 请求失败后可能无法确定写入是否完成，因此暂停写入，要求先重新同步。
- sessionStorage 只保存会话 UUID 和已确认游标；刷新后通过 GET 重建玩家与候补状态。
  未确认的当前事件会重新显示。不同标签页默认独立保存，关闭标签页后不保证恢复。
- 失效会话会清除本地记录并回到开局页面；用户也可显式释放当前会话。
- 结束身份只在服务器已结束且待确认事件读完后展示。
- 职业 CO 按钮来自服务端 claimInquiry.roles，读完事件且状态确定后才可操作；成功后展示应用层新发言，
  刷新恢复公开 CO 与未读事件，409 只同步不重放。新按钮沿用原版日文职业名；其余原型文案的整体日文化仍归 WEB-018/029。

会话仍位于 Java 服务内存，服务重启后失效。本批没有账号、跨设备存档或 Replay 保存。
按玩家询问支持单选/多选，候补来自服务端；未提交多选仅是页面草稿，取消不发命令，
新权威快照/同步/刷新即清空，不混用指定投票目标。已执行询问的公开状态和未读事件由服务端恢复。
回避开关完全显示服务端 selections.avoidanceEnabled，等待响应才更新，不做乐观切换或本地持久化；
刷新恢复权威值，新局默认开启。关闭只影响既有规则中的普通指定投票回避，共有者回避不受此开关禁用。
自由/灰区投票直接使用 legalVoteActions 的完整有序候补，不与指定目标混用，不自行计算灰区；
无 GREY 动作时隐藏对应按钮并说明无候补。未读、在途或不确定时禁止提交，终局隐藏入口。
刷新恢复最新候补与票型，409 只同步、失响应先同步，不自动重复投票。
全体占卜/护卫预约与取消使用服务端合法目标，复选框显示匿名潜伏与公开CO预约的并集（不是所有行动者全选的保证），
等待权威响应才改变，不保存本地镜像；预约是当日候补而非实际技能结果，跨日重新读取，终局隐藏。
占卜/护卫/投票选择互不混用。个体预约只显示服务端合法公开CO行动者，分行列明CO顺位；
目标及勾选均来自权威快照，不显示潜伏身份，也不将CO视为真实职业。
全体框是并集：个体预约可令其勾选；点击已勾选全体框会统一取消该目标，覆盖相关个体差异，
再次勾选会统一设置该目标，不影响其他目标。全体面板已明确提示覆盖语义。
个体预约也等待权威响应、409不重放、失响应先同步，不保存本地镜像；新日重读、终局移除。
投票履历按天折叠，默认展开最新一天；可用键盘展开早先日期，查看每轮全部投票者、目标、得票数、
全部同票首位与最终处刑者。候补、票数和首位顺序均直接使用服务端投影，不本地计票或推断胜者。
回避CO中断不生成一条完成票型；未知历史投票方式明确显示未支持，不伪装成已知方式或开放新投票按钮。
折叠状态只属临时浏览状态，刷新回到默认展开；浏览不写入、不推进事件游标、不新增存储键。

## 浏览器验收

在本目录执行：

```sh
npm ci
npx playwright install chromium
npm run test:e2e
```

测试自动启动固定种子的真实 Java API（17071）与 Vite（5175），结束后停止它们，端口须空闲。
Linux 默认命令是 `bash tools/run-gradle-safe.sh runWebTestServer --args=17071`，
安全入口内部已固定 `--console=plain`，调用时不得重复传入。Windows 默认使用
`gradlew.bat --no-daemon --max-workers=1 --console=plain runWebTestServer --args=17071`。
测试源码中的启动器为每局注入 seed=4202，
不改变生产 `runWeb` 随机行为，不新增种子 HTTP 参数，也不进入 Web 生产类路径或桌面 Fat JAR。
如需其他 Gradle 入口，可通过
`VOCR_WEB_SERVER_COMMAND` 指定完整启动命令，例如
`gradle runWebTestServer --args=17071 --console=plain`，Java 环境由当前终端继承；
覆盖命令仍须启动该测试入口，不能用随机生产服务声称通过固定种子中断验收。

默认套件现含六十五项测试，使用真实固定种子后端，并以明确标注的合成响应/文本提供者覆盖展示边界；覆盖：

1. 配役选择、事件确认与刷新恢复游标；
2. 浏览器指定投票至终局与刷新恢复身份揭晓；
3. 外部请求改变版本后，409 同步且不重试写入；
4. 已释放会话的恢复与清理；
5. 后端已完成写入但响应丢失，读取恢复正确结果；
6. 显式释放会话；
7. 390px 手机视口的可访问操作和横向溢出检查。
8. 通常村 seed=4202 实际触发 CO 中断：提示、下午事件、命令/GET 一致、刷新保留未读事件，读完前不得再投票。
9. 按职业 CO 的候补、未读禁用、新发言、公开身份与刷新恢复（含页面截图）。
10. 职业 CO 的旧版本冲突只同步，不自动重放。
11. 职业 CO 已执行但响应丢失后先同步确认，不重复询问。
12. 按玩家批量 CO 按选择顺序提交，恢复公开状态和未读发言（含截图）。
13. 玩家 CO 未发送选择可取消，刷新不提交也不恢复草稿。
14. 玩家 CO 冲突只同步并清理草稿，不自动重放。
15. 玩家 CO 已执行但响应丢失，先同步且不重复提交。
16. 回避设置等待权威响应，支持开关、刷新恢复、新局默认重置（含截图）。
17. 回避设置冲突只同步权威值，不重放切换。
18. 回避设置已执行但响应丢失时暂停操作，手动同步后恢复。
19—24. 自由/灰区各自完整对局（GREY 耗尽时确实无按钮，使用 FREE 继续）、有序候补请求体、票型方法、未读禁用与刷新恢复；各自的409不重放及已执行失响应先同步。
25—32. 全体占卜/护卫各4项：多目标预约/取消等待权威值、未读/在途禁用与刷新（含截图）；真实预约后完整投票跨日/终局；409只同步、已执行失响应先同步且不重放。预先设置另一技能预约验证互不串扰。
33—40. 个体占卜/护卫各4项：公开行动者与CO顺位、等待权威值/多目标/取消/刷新、不修改其他或匿名预约；个体与全体设置/取消组合及其他目标保留、跨日与终局；409只同步及已执行失响应先同步，不重复提交。
41. 运行中公开死因的值级安全与刷新恢复。
42—48. 公开信息真实跨日至终局、刷新只读、技能报告/怀疑符号/死亡历史合成边界、空与缺失投影、失效新局、409与失响应恢复。
49—55. 全日全轮票型对照真实HTTP、键盘展开/只读/刷新/终局、合成同票与全部首位/三种方式/零票/顺序、未知枚举兜底及不开放新动作、409/失响应不重复投票、CO中断不造历史、失效与新局清理。

56—60. WEB-019：真实HTTP事件逐条对照资源与刷新；default/special、Unicode、死亡叙述、wsw及明确诊断的合成边界；终局台词/诊断读完前不揭晓；纯文本而非HTML；空事件不崩溃且不阻断确认。资源单测另逐分支核对1848条default和744条special；不宣称所有分支均由真实对局触发。

61—65. WEB-021：真实事件在1920/1440/1280/1024窗口保持16:9、正文与游标；连续长台词的键盘滚动/换事件复位及终局确认门槛；44人列表键盘/滚轮；多行动者预约列表有界/折叠/刷新且浏览不写入；125%/200% CSS zoom压力（不是浏览器工具栏缩放验收）。

布局与后续素材接入约定见[web-stage-layout.md](../docs/web-stage-layout.md)。

固定种子测试不代替生产组合根验收；WEB-038 内部试玩须另用生产 `runWeb` 执行无种子浏览器冒烟。

截图与失败轨迹位于忽略的 `test-results/`。Java/HTTP 验收仍按仓库根目录的
`clean check verifyRuntimeAssets fatJar` 执行。

当前 Ubuntu 20.04 不在 Playwright 1.64 的官方支持列表中。本次验收在临时目录下载
Ubuntu 22 x64 构建，确认 Chromium 156 可运行后，以
`PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu22.04-x64` 运行；WEB-005 的八项测试亦以该环境验证。
新环境推荐使用官方支持的系统；该覆盖参数不代表对所有 Ubuntu 20 环境的兼容保证。

### 本机 Linux 缓存工具链复现

在本目录执行以下命令（缓存已安装，无需重复下载）。不设置后端覆盖变量，验证默认安全入口：

```sh
export JAVA_HOME=/home/noeticuser/Desktop/.vocr-toolchain/java/jdk-21.0.12.1+1
export PATH="/home/noeticuser/Desktop/.vocr-toolchain/node/bin:$JAVA_HOME/bin:$PATH"
export VOCR_GRADLE=/home/noeticuser/Desktop/.vocr-toolchain/gradle-8.8/bin/gradle
export GRADLE_USER_HOME=/home/noeticuser/Desktop/.vocr-toolchain/gradle-home
export PLAYWRIGHT_BROWSERS_PATH=/home/noeticuser/Desktop/.vocr-toolchain/browsers
export PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu22.04-x64
unset VOCR_WEB_SERVER_COMMAND
npm run test:e2e
```

`VOCR_GRADLE` 避开本机已有 CRLF 的 Wrapper，不修改用户文件；浏览器路径必须与安装缓存一致。
仅默认`npm run test:e2e`如需离线启动，可显式设置
`VOCR_WEB_SERVER_COMMAND='bash tools/run-gradle-safe.sh runWebTestServer --args=17071 --offline'`，
但须在验收记录中注明用了覆盖命令，不能把它当作默认配置路径的证据。
安全入口检查内存并串行启动 JVM；不要绕过监护、并行启动构建或附加未批准的全量任务。

## 显式诊断探针（故意失败，默认回归不包含）

`npm run test:diagnostic-probe`单独启动受监护测试服务并**故意退出1**，用于核验public-information诊断JSON/截图。产物在忽略目录`../build/web030-review/probe-results/`，不得将这个失败记为产品回归。正常`test:e2e`不发现`diagnostic-probes/`，也不再读取旧`VOCR_PUBLIC_INFO_DIAGNOSTIC_PROBE`环境变量；遗留变量不会触发默认回归故意失败。真实回归失败时的自动取证保持不变。

## WEB-017 七配役内部功能门

这是本机回环自动试玩，不是公网发布或人工原版视听验收。先按上段设置同一工具链环境，
必须先`unset VOCR_WEB_SERVER_COMMAND`：gate显式拒绝任何已设置的命令覆盖（包括空串），
在启动服务前提示错误，不自动猜测或改写自定义命令；上段的offline命令覆盖仅供默认e2e使用。
在本目录**依次**执行（前一步成功才继续，不能并行 JVM）：

```sh
bash ../tools/run-gradle-safe.sh test --tests 'com.vocr.web.Web017JourneyTest' --offline && npm run test:gate
```

第一步用七配役4201—4207建立应用直连与真实HTTP逐命令对等，生成忽略目录
`../build/web017/`中的15份临时公开DTO脚本：每配役全程指定/混合操作各一局，加通常村真实CO中断。
第二步通过测试专用`--release-baseline`模式运行相同种子与命令；全部写操作均由页面发起，
逐次核对请求体、响应、HTTP全量重读与终局。七配役混合策略必须实际执行全部三种投票方式，
而不是靠FREE回退声称覆盖。妖狐4203无公开护卫者是显式断言的合法缺席，不删除该局或换种子。

脚本带main/test/web全部Java源码SHA-256指纹（包括生成器Web017JourneyTest）。任何这些源码变化的批次，必须同批重新生成oracle，或明确登记 **gate stale / 尚未验证**；不把旧对照的指纹失败计为产品行为失败，也不能说gate通过。真正运行gate前必须先按最终源码生成oracle，禁止放宽/排除测试源码指纹。

当前从bc6f545新增Java测试起，旧build/web017对照已stale；下一次需要gate时生成新对照，本次不为恢复绿色而提前跑全门。031还会改变源码，故不在开工前重复生成。
该指纹不包含`config/probability.json`、`build.gradle`或前端源码；概率/构建配置变化也必须重新生成，
不能把指纹相同当作全部输入未变。前端变化须按影响验证浏览器；若同轮准备运行gate，先重新生成对照，
不要把可能的响应差异只归咎于浏览器。不在本批扩展指纹或改游戏输入。
这些文件不提交、不替换黄金摘要、不进入桌面或生产Web产物，不含会话凭据。
`npm run test:gate`缺产物或指纹不匹配会失败；不要手工刷新文件来掩盖对等断言失败。
Gradle若显示UP-TO-DATE可复用同源成功结果；需强制重跑时可在同一定向命令末尾追加`--rerun-tasks`，
不得删除用户存档或放宽黄金断言。

基线模式仅测试进程关闭DebugLogger，不修改生产默认诊断开关或桌面按钮；生产日志治理与公网门禁仍待041/044。
默认`npm run test:e2e`保持原4202模式及既有55项回归，并加入019的5项台词与021的5项布局用例，不因内部功能门变更。截图在忽略的test-results，
journey-summary作为Playwright附件由reporter管理，默认list不保证将内联附件单独落盘；需机器报告时可用
`PLAYWRIGHT_JSON_OUTPUT_NAME=test-results/gate-report.json npm run test:gate -- --reporter=list,json`。
后一次Playwright运行可能清理前一次产物，应在后续运行前先查看/保留需要的证据。
逐项A/Q状态、差异及决策见`../docs/web-017-verification.md`；没有账号/Replay/音频等后续能力不能标首发已完成。
