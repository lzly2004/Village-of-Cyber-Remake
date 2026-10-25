# af8fac0 Review 核验与 WEB-008

输入报告审查 dd34d18 → af8fac0（WEB-007），只读，无阻断/非阻断发现。本轮先核对环境，再执行首个 READY 的 WEB-008，队列不重排。

## 逐项核验

| 事项 | 证据/核验方式 | 结论与处置 |
|---|---|---|
| 声明1：路由最小改动 | af8fac0 的 by-players 路由直接复用命令/会话，无规则复制 | 接受；本轮 avoidance 路由沿用同一模式 |
| 声明2：校验链 | AskClaimByPlayersCommand 和 GameApplication 的前置校验对应既有18类畸形负载及状态不变断言 | 接受，原 HTTP 用例继续回归，不放宽 JSON/候补规则 |
| 声明3：顺序 | 既有反序批量与直连比较、浏览器请求体精确比较保留 | 接受，不在新接口改动这些语义 |
| 声明4：草稿 | 页面草稿不持久化、watch(update) 清理及三类异常用例保持 | 接受，新权威回避更新也会正常清理未提交草稿，不建第二份回避镜像 |
| 声明5：验收历史 | 上轮454项/27 HTTP/15浏览器与原始记录相符 | 接受为上轮证据；本轮重新验收并另报实际数量 |
| 声明6：先前观察落实 | WEB-038/Q08 的生产 runWeb 无种子冒烟、Jackson 实际版本边界、日文过渡范围均保留 | 接受，不提前实施生产组合根试玩，也不宣称全量日文化完成 |
| 固定种子公开编号脆弱性 | 现有4202样例显式检查合法性/预期效果，编号变化会使测试失败 | 接受为测试前提而非缺陷；未来不能为让测试通过盲目刷新编号，需先解释布局/规则变化 |
| 环境恢复及“会不定时掉挂载” | 当前 findmnt 显示分区读写挂载；日志记录昨晚正常卸载、今早启动后重新挂载；没有运行中随机掉盘的确证 | 接受恢复事实，排除将“随机掉挂载”当成已确认根因；详见下节，不改系统自动挂载配置 |

## 环境问题：已恢复可用，非已证实磁盘故障

- `journalctl -b -1 -u udisks2`：2026-10-21 23:39:01 停止磁盘服务，ntfs-3g 正常卸载 /dev/nvme0n1p3；当前会话记录 10-22 08:49:24 服务启动、08:52:05 正常读写挂载。uptime 的本次开机时间为08:49:17（均为本机日志的上海时间）。
- `/etc/fstab` 仅配置 Linux 根/EFI/home/swap，没有 Windows-SSD 自动挂载项；临时目录清理配置含 `D /tmp 1777 root root -`，systemd-tmpfiles-setup 的 ExecStart 为 `systemd-tmpfiles --create --remove --boot --exclude-prefix=/dev`。这些证据解释了本次开机后挂载需要恢复、/tmp 工具丢失的现象，不证明所有历史未挂载情况都有同一根因，也不能据此断言磁盘健康已获全面检测。
- 当前挂载正常、仓库 HEAD/无关修改完整；本轮未强制挂载、修复 NTFS 或修改 fstab。永久开机自动挂载如需实施，须另获用户批准，重启后仍可能需要正常手动挂载。
- 为避免反复下载，测试工具改缓存在授权桌面的 `.vocr-toolchain` 隐藏目录，JDK/Node/Gradle/浏览器及 Gradle 用户缓存不再依赖 /tmp；不进项目 Git，不安装系统包，不改变生产依赖。Node 对照官方 SHASUMS256 校验，JDK 使用厂商 HTTPS 归档。

## WEB-008 实现边界

- 新增 PUT `/{id}/avoidance` → SetAvoidanceCommand → GameSession.setAvoidance，直接复用应用权威开关。新局 true，合法设置同值亦增 revision，无额外事件；不引入新的接口字段、核心规则或存储格式。
- 新增日文 aria switch，从 update.selections.avoidanceEnabled 直接显示；仅 canDecide 时允许提交，等待响应而非乐观切换。刷新、409 与失响应沿用现有同步逻辑，不持久化本地开关值，不自动重放；终局隐藏入口。
- 严格遵循 ExecutionManager.handleAvoidanceCO：只有指定投票执行普通回避判断；共有者分支在开关判断前，因此关闭开关仍可能发生共有者回避。UI 与接口文档明确这一例外，不改成“禁止全部 CO”。
- HTTP 七配役 false/false/true 与直连逐次一致，空事件窗口重复 GET 一致，游客隔离；无效输入、旧版本、并发一次接受、终局拒绝都回归。额外真实投票样例验证4202/9号开启中断、关闭可完成，以及关闭后4202/3号共有者仍发 hbg 中断，不只检查布尔字段。
- 浏览器新增等待响应不乐观、刷新/新局、409不重放、已执行失响应先同步三项；原默认开启的 CO 中断用例继续保留。安全门禁和历史询问场景不被新测试取代。

## 最终验收

使用持久缓存的 Microsoft OpenJDK 21.0.12.1+1、Node22.23.0、Gradle8.8、锁定 Playwright 与 Chromium headless shell156（Ubuntu22平台覆盖）执行。

- 定向 `gradle cleanTest test --tests com.vocr.web.GuestGameServerTest --console=plain` 通过；随后 `gradle clean check verifyRuntimeAssets fatJar --console=plain` 全部通过：465项测试零失败，其中HTTP38项，七配役黄金轨迹、560条概率及资源校验保持通过，未刷新黄金摘要。
- `npm run test:e2e`：18项真实浏览器测试全部通过（21.0s），新增3项开关场景均执行。等待权威响应测试只暂缓实际请求发送，不伪造设置成功；失响应测试先由真实服务执行再丢弃响应。
- `npm run build`、JS语法及差异检查通过；Vite输出JS/CSS gzip合计约35.4 kB。已实际查看 avoidance.png，关闭状态与共有者例外提示正常、无覆盖；截图仅在忽略的test-results中。未宣称全部原版视觉/日文化完成。
- Fat JAR SHA-256 仍为 `9d1c93f0d1b0bb0114333874728cd2b5364bd6c7e0f0e50314c001144fd39f2d`；Swing未修改，未额外启动桌面UI。新增Web操作属于用户批准的WEB-008范围，不改变既有核心行为。
- 最终文档链接、已实现8条路由、49步顺序/状态检查通过；用户原有Game.java空白、批量换行与其他未跟踪文件均保留。完整验收使用当前工作区，不是隔离历史快照。
