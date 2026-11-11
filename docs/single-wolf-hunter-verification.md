# 整局唯一狼猎修复：定向验收记录（2026-11-11）

## 行为与实现

用户确认整局累计最多一名实际人狼成功CO猎人，死亡后也不恢复。共享核心对桌面与Web同时生效；真猎、其他非人不受狼方唯一性约束。ClaimState增加单局实际所有者（与已有计划占位布尔标记区分），GameContext原子登记保护，COManager在事件、身份、顺位与怀疑度变化前拒绝重复。ClaimResponsePlanner统一收集回应并在同批占位，预定计划不能绕过。处刑回避显式计算可用职业：猎被占且猫可用时选猫，都不可用时不CO。

这次没有修改Replay格式、概率配置、旧录像、前端或黄金摘要。Windows只同步本轮相关源码、测试和文档，不动IDE配置和存档。前序Replay灰字相关未提交改动继续保留，不混作本轮新实现。

## 定向验证

- 旧实现新增专例12项中9项失败，证明用户路径及相关绕过确实被捕获。
- 修复后补足两种规划顺序/猫不可用分支，共15项新专例通过。
- 核心、HTTP、Replay相关13套件163项：首次162项通过，1项Swing组件测试因工具链残留DISPLAY=:2不可连接而失败；清除该变量后该单例通过。该headless组件验证不等于实际桌面窗口试玩。
- 原顺位测试改为选非狼玩家，避免随机选两狼与新规则冲突，11项复验通过；新专例单独验证狼猎限制，不弱化原顺位断言。
- 七配役固定种子特征轨迹及随机隔离8项通过，未刷新任何黄金摘要。这不表示所有随机对局都不变，修复命中路径的行为会变化。
- git diff --check通过。

日志、XML、源码SHA与结果集中在build/wolf-hunter-fix-20261111/verification.json。首次基础设施失败（只读运行锁目录、沙箱Gradle本机套接字限制）发生于测试前，修正执行环境后才正式运行；不记为通过。

## 获批后的最终验收

用户明确授权本轮完整验收、FatJAR和Windows同步。经既有安全入口、单worker及至少2GiB内存门槛，执行：

`VOCR_APPROVED_FULL_CHECK=1 XDG_RUNTIME_DIR=/tmp/vocr-wolf-hunter-runtime-1000 bash tools/run-gradle-safe.sh cleanTest check verifyRuntimeAssets fatJar --offline`

- 完整自动测试：67个JUnit套件，671项，0失败/错误/跳过，含15项新狼猎回归与七配役8项特征/隔离测试。
- 概率配置560组合、运行时资源校验、FatJAR成功。JAR大小724205559字节，SHA256为6c9fa6eee2c42a335168c1a508daf5e85066c5945e6f56279a240a88a201d993。
- 自有Xvfb中运行opt-in DesktopBaselineTest、DesktopGuardHistoryBaselineTest，两项通过；实际打开Swing主菜单、对局与Replay，不使用用户存档、不播放音频。已目检主菜单截图。此项不是Linux音频问题复测，也不是Windows执行证明。
- 原始完整XML保存在build/wolf-hunter-fix-20261111/full-xml，桌面XML单列desktop-xml，不用后一次定向结果覆盖全量计数。

## Windows交付及范围

目标原工程：`D:\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake`。

同步前核对原工程4个核心源文件及原测试均与本轮修改前HEAD一致；对将覆盖的文件先逐一备份，再同步本轮4个核心文件、2个测试和3份文档，逐文件核对哈希。战绩、Replay、IDE配置及模块文件的同步前后哈希必须一致；最终备份路径与结果以build/wolf-hunter-fix-20261111/windows-sync-receipt.json为准。未同步bin/out等编译缓存，不让旧class冒充新源码。

用户在Windows原工程中先Rebuild Project，再按原来的Game.java运行。新规则仅影响新对局；旧录像里已经存在的多狼猎仍如实显示，不重写。尚未实际在Windows执行本轮修改。

## 历史与未改事项

查证发现：最早可见3b150266（2026-01-01）已有预定计划/批量询问绕过；5fdcc7784e7b8caf3a260ac0d1c9d0a0437bbbdc（2026-06-28）另将处刑回避中“已有狼猎、猫仍可用”的分支从猫改成猎。本局存档确认流者/読書均为狼且D2猎CO，用户确认依次指定处刑并开启回避；录像不保存预定计划，不能唯一判定第二狼走哪条分支。本批同时封住上述重复狼猎路径。

假猎人护卫目标选择和Linux音频卡顿仍是独立未处理事项。不推送、不合流、不部署，不刷新黄金摘要；前序Replay灰字改动继续独立保留，不纳入本批规则提交。
