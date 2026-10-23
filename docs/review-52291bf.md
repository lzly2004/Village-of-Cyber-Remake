# 27b6498 / 52291bf 评审核验与补修

外部review比较基线bc6f545，结论无阻断；Java562/562、FatJAR724,181,504字节、gate15/15及指纹同源由reviewer独立复验。其默认浏览器98/99在公开信息长流程超时，与coder先前99/99并列保留，不改写旧证据。reviewer脏树142项属于其环境；本工作区开工为137项。

## 逐项处置

- H-2采纳，用户明确同意本轮先补修：附件所述浏览器cursor10/未确认1、服务端next11/无在途请求，支持drain一次采样空窗退出的根因。四个spec改用共享drainEvents，单次DOM快照判可读事件/合法可决策按钮/已结算；等待重试，不加sleep、重复count、超时或非零容忍。控制空窗的两项测试锁定：确认后暂时卸载按钮、保持决策禁用时不得返回；再放行最后事件并验证确实确认，分别覆盖决策/终局。
- H-1采纳，用户授权：全仓仅定义、无调用者的public saveToSlot删除；有效桌面保存仍仅走checked原子路径。旧load/save的历史容错不在本次扩大修改，035仍需独立严格导入。
- H-3采纳：探针输出解耦为build/diagnostic-probe/results，不再使用批次名。
- H-4采纳：保存端口文档说明CLOSED不可保存，正常桌面无该路径，不冻结为035兼容承诺。
- H-5采纳：032任务写明Web禁用持锁磁盘I/O的saveDesktopReplay，使用completedReplay只读终局边界；架构断言继续保留。
- 不要求reviewer额外整理附件：现有报告的最小状态证据已归档；未收到的原始PNG/JSON不冒充本地实证。reviewer的AWT库/shim限制仅记外部环境，本机继续既有安全入口，不复用其oom_score_adj结果、不修改系统。

## 本轮实跑

- 定向Java：ReplaySlotSinkTest3、ReplaySavingPortTest5、ReplayCompatibilityTest2、ArchitectureBoundaryTest20，共30/30，6s（build/web031-review/targeted.log）。
- 配置/诊断单测8/8；补输出目录断言后最终8/8，1497ms（config-final.log）。
- 首次浏览器验证：受控空窗2项通过，但真实场景发现首版收敛仅看指定投票按钮、漏掉未选指定候补时的自由投票，随后主动中断；该结果不计最终通过。修正为所有合法投票按钮，重跑受影响范围。
- 最终受影响浏览器58/58（1.6min）；默认全量101/101（2.3min，retries=0）。原超时长流程另做5次重复，5/5（37.2s）；未做五次全量重复。
- 用户另明确授权本轮补修及032全量验收/打包后，通过既有入口fatJar --offline：Java57类562/562、0跳过（38s），七配役轨迹/概率/check/运行时资源/FatJAR通过，摘要在build/web031-review/full-summary.json。该次全量已生成本批指纹oracle，但补修不另跑七配役浏览器gate，032改完将重新生成并验其最终状态；不把旧15项当本轮实跑。

F-2由未定位风险收敛为H-2采样竞态修复，受控空窗、实际回归通过后关闭此已定位采样缺陷；其他新超时仍需独立取证。A06/A07/A08/A09/A10及046/049人工门槛不因补修放行。
