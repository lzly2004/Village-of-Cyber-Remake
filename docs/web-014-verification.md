# WEB-014实施与完整验收

> 安全更正：旧启动器存在全机PID误选缺陷，已按用户单独批准修复并通过15项无JVM隔离验证，见[事故记录](incident-2026-10-22-process-cleanup.md)。随后用户再次批准恢复真实验收；下列“完整验收”均在修复后的监护程序下执行，不拿旧版成功输出替代安全证据。

状态：WEB-014完整验收通过，014为DONE，015解除依赖门为READY；本轮没有实施015。

## 交付范围

- PublicDeathReason在投影层运行中五类公开分类，四类夜死归NIGHT_DEATH；仅FINISHED给原始精确值。核心whyDie不动，不保留泄漏兼容字段。
- PublicInformation不可变DTO：当前公开CO技能历史（含死亡者、CO前桌面可见历史）、报告行破绽、日3起存活观察者前三目标/比较类别、按桌面拼接的公开死亡历史。不返回原始分数/差值/矩阵、未CO结果或额外身份。
- GameUpdate做阶段/值级约束；GuestWireContract精确嵌套白名单扩展，扫描运行中完整响应的精确夜死字面量，类型图拒收核心whyDie、上下文、recorder等。
- 无新路由、依赖、核心规则、随机源或存储格式变更。终局原始精确死因是已批准选择，不假称与桌面终局合并文字粒度相同。
- 015页面展示未做；Vue当前不读取deathReason，已有动作不依赖该字段。新增浏览器测试只验安全响应/刷新一致性，不把其称为015报告浏览。

## 已执行

重启恢复后，以持久JDK21/Gradle8.8离线、无常驻daemon、单worker、受内存监控运行：

```sh
bash tools/run-gradle-safe.sh test \
  --tests 'com.vocr.application.*' \
  --tests 'com.vocr.ui.PublicInformationDesktopParityTest' \
  --tests 'com.vocr.web.GuestSerializationBoundaryTest' \
  --tests 'com.vocr.web.GuestGameServerTest' \
  --tests 'com.vocr.core.GameInitializationBaselineTest' \
  --tests 'com.vocr.config.ProbabilityCalculatorTest' \
  --tests 'com.vocr.architecture.ArchitectureBoundaryTest' \
  verifyProbabilityConfig verifyRuntimeAssets --offline
```

历史定向验收185项，0失败/错误/跳过，构建成功11秒：应用44、HTTP90、序列化17、真实Swing文本对等1、初始化黄金基线11、概率3、架构19。该轮旧清理脚本存在严重缺陷，测试断言通过不等于当时的清理行为安全。修复后完整验收见下文。

其中公开投影单测18项：全8死因运行/终局、技能日期/颜色/死亡者与潜伏排除、7个比较边界、死亡历史狐猫组合、阶段值级拒绝。HTTP新增10项：全8死因、游标及服务重启失效、诊断路径不可读；每次使用wire门禁。序列化新增4个精确夜死嵌套注入负例。共新增33项。

HTTP全8枚举使用合成只读上下文经真实服务器/JSON，不假称每种死因都由完整自然局触发；既有七配役完整对局也继续经过同一wire门禁。重启只验证旧凭据404及同固定种子新局一致，不承诺游客会话持久化。

历史浏览器单项断言通过，但旧清理逻辑会误杀桌面，原“退出安全”判断撤回。修复后的本轮已重新运行前端构建及全部41项浏览器回归，通过后真实后端无残留，桌面会话保持。详见[内存记录](desktop-memory-safety.md)。

Swing对等通过真实VoteInfoRenderer/createDoubt生成的文本比对，图片为占位、上下文合成，未启动交互窗口。独立审计探针已重新编译运行，输出FIXED_RUNNING_PROJECTOR=NIGHT_DEATH及PASS，并验证其本次临时数据目录已清空；桌面生产UI未改。

## 修复后的完整验收（2026-10-22）

持久JDK21/Gradle8.8，堆上限512MiB、测试256MiB，单worker，使用修复后的Linux子进程监护入口；不改锁屏/驱动/系统内存配置：

1. 15项启动器隔离回归全部通过（含外部哨兵、正常/异常退出、PID归属、低内存模拟、另建会话后代、锁释放）。
2. 最小真实`test --tests com.vocr.application.PublicInformationProjectorTest --offline`，18项通过、3秒，退出正常。
3. 用户明确批准后，`VOCR_APPROVED_FULL_CHECK=1 bash tools/run-gradle-safe.sh clean check verifyRuntimeAssets fatJar --offline`，39秒成功。clean后XML汇总52个suite、540项，0失败/错误/跳过；含HTTP90、FullGameCharacterizationTest七配役黄金轨迹与随机隔离8项、ReplayCompatibilityTest2项、概率560组合及运行时资源校验。未刷新任何黄金摘要。
4. `npm run build`成功；真实固定种子后端、Playwright workers=1、retries=0，41项全部通过（1分钟）。最后新增安全投影/刷新测试亦通过。初次命令因自动审批服务额度耗尽被拒、未执行，额度恢复后按原审批途径执行，无绕过。
5. 独立探针串行javac/java，经同一子进程监护执行；数据目录指定在`/tmp/vocr-web014-audit-jgUrCy/data`，输出PASS及PROBE_DATA_CLEANUP_PASS；仅编译输出目录保留作证据，不清理旧运行目录。
6. 最小测试、全量和浏览器退出后均无Java进程；GNOME shell PID从35882保持不变；截至14:24核对，本轮内核无新增OOM，MemAvailable约5.0GiB（只作快照，不冒充峰值或长期保证）。

Fat JAR SHA-256：`e6560fb3fa58813fdd497e9b6375ae6df06e33ec820e03c4ab52d263818e60e2`。应用层新增类型使桌面产物变化，不宣称旧hash不变；桌面游戏规则/随机源/存储格式不改。上述540项在包含安全启动器与WEB-014的同一工作区联合验收，不声称仅检出资源保护提交时也有540项。

## 已知风险与后续

- 全量/打包已按用户明确授权完成；不把本次授权扩展为今后每轮自动全量授权或生产部署许可。
- 既有DebugLogger默认true与Swing诊断按钮未改；HTTP无诊断端点回归不等于日志安全已关闭。实际治理仍归041/044，017任何公网试玩前必须关闭并验证。
- 安全启动器是尽力而为的监测/清理，不是硬cgroup限额；不做制造全机OOM的破坏性验证。
- 原有批量换行、Game.java空白、.workbuddy与NUL保留；仅精确暂存本轮自有文件。下一项READY为WEB-015公开信息/技能报告浏览，本轮未实施。
