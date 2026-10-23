# WEB-031：Replay 保存端口

基线27b6498（已按用户裁定处理030评审）。本批生产Java仅迁移保存边界；不增加HTTP接口、浏览器库、导入/导出、Replay字段或版本，不改变随机源、规则、事件顺序与实时公开DTO。

## 边界

- `ReplayRecording`只持有`List.copyOf`后的原记录字符串；不持有GameContext、可变GameRecorder、存储路径或会话凭据。JSON|内多行/Unicode/数字原文不拆、不重新编码为新录制格式。
- `ReplaySaveSink`是应用层写入端口，目标由适配器绑定；应用层不依赖ReplayManager/ReplaySave/Jackson。sink失败须抛出，不能当成功继续停止录制。
- `GameSession.completedReplay(confirmedEventCursor)`只允许FINISHED且游标**恰为全部已发布事件末尾**；NEW/RUNNING/CLOSED及负数/越界/未读游标均拒绝。首次观察终局时冻结录制副本，不受随后桌面兼容停止动作污染。读取不消费队列、不改revision/预约/统计，不自动写入文件；后续Web适配器必须使用此入口，实际HTTP仍归032。
- 桌面兼容路径`saveDesktopReplay(sink)`保持旧顺序：取得非空记录→sink成功持久化→`endGame(0,0)`→停止录制。原桌面允许非空的未完记录保存，本批不擅自改变此行为；它与Web终局读取严格区分。失败时不追加END、不停止录制，记录与事件可重试。
- UI不再获得GameSession.recorder/currentRecorder，也不调用endGame/setActive或自行构造ReplaySave。UI把槽位绑定给`ReplaySlotSink`；架构测试禁止UI/Web依赖GameRecorder，并禁止HTTP调用桌面兼容保存入口绕过终局限制。

## 桌面适配与失败

`ReplaySlotSink`仍调用原`ReplaySave.fromRecords`构造v3，`ReplayManager.saveToSlotChecked`保留九槽JSON和派生显示字段。其他槽位不重建，槽位在0—8校验后才使用。

为使端口确实能报告写入失败，新增checked路径：同目录临时文件完整序列化→ATOMIC_MOVE/REPLACE_EXISTING→发布内存candidate槽位；失败只清理本次临时文件，不替换已发布槽位、不停止录制。文件系统不支持原子替换时明确失败，不静默降级覆盖。原load/save/saveToSlot旧入口与历史迁移不借此重构；旧破坏性加载、导入校验仍归035，不宣称本批已解决所有存储异常。

CLOSED会话不再可保存；桌面正常路径不会持有CLOSED会话，此项不是对035关闭会话保存能力的兼容承诺。

ReplayBrowserHandler继续原右键确认/覆盖流程；只有应用保存成功才播点击音并刷新槽位。写入失败弹明确提示并保留录制，不把后续界面刷新异常误报为文件保存失败。

## 工作区保护

ReplayBrowserHandler原有整文件CRLF差异、ReplayManager混合换行以及Game.java空白等均保留。对重叠文件按原始字节做局部修改；提交时仅从HEAD构造本批语义blob，不能用全文件git add混入既有CRLF变化。验证使用工作区的同等语义源码，最终检查原有差异仍存在；不全仓规范化，不修改Game.java。

## 实跑验收

- 定向42/42（7s）：新录制/应用端口5、桌面适配器3、既有会话4、Replay兼容2/交换4/文本4、架构20。
- 补HTTP禁用桌面入口的架构约束后，架构20及实际Swing冒烟1共21/21（6s）。在DISPLAY=:1真实创建窗口、显示菜单、实际右键空槽并确认、通过应用端口保存、刷新出0号槽、重新加载隔离JSON相等；使用真实PNG与静音音频替身，未触及用户data或其他窗口。截图`build/web031/desktop-replay.png`已查看，不是035真实终局文件交换样本，也不作正式DPI/读屏验收。
- 既有HTTP/序列化定向107/107（11s），公开DTO/恢复边界断言未改。上述日志在忽略目录build/web031。
- 一次沙箱写源码返回只读，检查宿主挂载仍rw；刷新任务目录授权后完成写入。该次随后Gradle仅跑到已有架构项，**没有GUI源码，不能记作GUI通过**；实际GUI证据取desktop-final.log。没有重挂载或修复系统。
- 用户在本轮明确回复“授权本轮全量验收与打包”后，运行`VOCR_APPROVED_FULL_CHECK=1 bash tools/run-gradle-safe.sh fatJar --offline`，57类562/562、0跳过（36s）；七配役初始化黄金/特征11项及Web017JourneyTest8项在其中，概率配置/check/运行时资源门禁/FatJAR均成功。未刷新黄金摘要。JAR724,181,504字节（桌面包，不适用Web100MiB预算），含新端口且未混入com.vocr.web/Jetty/Javalin；摘要另存build/web031/full-summary.json，避免后续定向报告覆盖后误读全量计数。
- 全量测试生成15份当前oracle；指纹`02d47c11cad5b0d7c67e725a5b464021e5faf9f763b72c04a799654ed3af7e7e`与main/test/web工作区源码逐字节一致。`npm run test:gate`15/15（3.8min、retries=0），原先stale状态已恢复为本批同源验证通过；将来源码再变仍须重生成/标stale，不放宽指纹断言。
- 新原子写入另在项目实际NTFS/FUSE上复验适配器3/3（3s）：临时init脚本仅把Test JVM的java.io.tmpdir设为忽略目录build/web031/project-tmp，启动参数与测试日志中的实际junit路径均核对。首次读mutable systemProperties为null，不能据此计通过；随后核验脚本也因此显式失败。改为检查allJvmArgs后严格断言通过并取得真实路径证据，未改项目持久配置/源码或系统。原子写入及失败不覆盖标记文件均通过。
- 最终默认浏览器回归99/99（2.2min），public-information长流程5.3s通过；不据此关闭既有F-2偶发超时调查，未做五次全量重复或猜测性修改等待条件。
- 最终只读检查未见本轮JVM/浏览器/Vite残留，可用内存约5.1GiB；运行期间抽查三个受监护JVM的oom_score_adj均为500。全部串行，未降低2GiB门槛。

A09/A10仍未放行：032浏览器保存/列表与035双方文件交换尚未实现；031不把核心录制器直接序列化到浏览器。人工日文、音频和046/049门槛保持。F-2若真实复现仍须先留附件，不跑五次统计。
