# Single Wolf Hunter Implementation Plan

> **For agentic workers:** Execute natively in this session following the user's explicit instruction to start; no delegation is required.

**Goal:** 整局累计最多一名人狼成功CO猎，死亡不释放。
**Architecture:** ClaimState保存实际首次狼猎所有者，GameContext统一资格/原子登记，COManager副作用前保护；规划器在本批回应内预占猎名额，修正回避可用性选择。
**Tech Stack:** Java21 / Gradle / JUnit5，复用共享核心。
**Spec:** docs/superpowers/specs/2026-11-11-single-wolf-hunter-design.md

## Global Constraints
不改概率、交换格式、旧录像或黄金摘要；单worker、至少2GiB，最终全量与打包另获批准，前序未提交Replay改动保留。

## Review Focus
- 已有预定占位不应拦住首个实际狼猎；所有者与规划标记区分。
- 死亡、候补移除后不释放；新局重置。
- 顺序/同批回避与预定计划均不能重复，拒绝不发事件不写身份/怀疑。
- 真猎、非狼假猎不被误拦。
- 猫不可用时不硬选猫；两者不可用时无CO。

### Task 1: 回归用例
- [x] 新建src/test/java/com/vocr/core/WolfHunterClaimLimitTest.java，固定种子与100%测试概率，覆盖上述条件以及角色/名单批量回应。
- [x] 经tools/run-gradle-safe.sh test --tests '*WolfHunterClaimLimitTest' --offline确认旧实现失败，保留日志。

### Task 2: 共享核心修复
- [x] ClaimState.java记录首次实际狼猎所有者；GameContext.java提供canClaimRole并在registerClaim拒绝重复。
- [x] COManager.java执行前保护；processMaoLieCo显式计算猎/猫可用性。
- [x] ClaimResponsePlanner.java所有猎回应经过同批检查，不让预定计划绕过，名单首次选定后本批即占位。
- [x] 同一命令重跑新用例至通过；运行既有ClaimState/GameContextClaim/ClaimResponsePlanner及相关兼容/HTTP定向测试，git diff --check。

### Task 3: 最终交接
- [x] 更新本计划及查证记录，区分定向结果与未运行全量；如发生黄金差异，记录真实差异不刷新。
- [x] 生产Java全量/特征轨迹/资源/概率/FatJAR在明确授权后执行，之前不提交，不宣称已可发布。

## 执行记录

2026-11-11：新增专例15项均通过。相关13套件163项首次162通过、1个Swing用例因环境变量DISPLAY=:2指向不存在服务而失败；清除DISPLAY后单例重试通过。原顺位测试改为选择非狼，复验11项通过。七配役特征轨迹与隔离8项通过，黄金摘要未改。具体命令/首次失败/日志与源码SHA集中于build/wolf-hunter-fix-20261111/verification.json。完整自动测试/check/资源/FatJAR尚未运行，按AGENTS等待本轮独立授权；本批未提交，前序Replay未提交改动原样保留。

最终授权后：完整671/671通过，概率/资源/FatJAR通过；真实Swing自有显示2/2通过，未使用用户存档或音频。最终Windows备份同步回执与提交信息见当轮verification记录。前序Replay工作不纳入本批规则提交。
