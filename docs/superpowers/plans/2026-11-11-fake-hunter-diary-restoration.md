# Fake Hunter Diary Restoration Implementation Plan

> **For agentic workers:** Implement natively in this session, following the user's explicit instruction to begin; no delegation.

**Goal:** 恢复死后生成和软回避，保留按夜历史与随机隔离，双端提交。
**Architecture:** MainLogic在死亡前收集演员，死亡后/终局快照前完成日记；Fabricator采用独立随机源和恢复的内层循环，旧随机推进器单列保持不变；HunterGuarder增加内部日记入口以覆盖当夜死亡演员。
**Tech Stack:** Java21、JUnit5、Gradle、既有HTTP/Replay与Swing验证。
**Spec:** docs/superpowers/specs/2026-11-11-fake-hunter-diary-restoration-design.md

## Global Constraints
单JVM/worker、至少2GiB；不改概率值/存档/API/UI/黄金；新全量与打包单独授权；Windows备份与哈希保护，不能只同步不提交却称已双端提交。

## Review Focus
- 终局夜不因提前return丢记录：MainLogic调用顺序与真实链路覆盖。
- 当夜死亡演员仍记录，真猎死后仍返回0：分别断言。
- 狼/狂信while不跳过权重，主随机兼容器不跟着变化：控制候选/随机区间断言。
- 重复生成与晚CO不重写旧日记，缺合法目标不写自己/尸体：独立用例。
- Windows旧工作区带有已授权前序源文件差异：逐文件预像检查，独立语义提交，不git add全部。

### Task 1: 测试和实现
- [x] 新建src/test/java/com/vocr/core/FakeHunterDiaryPolicyTest.java，先验证旧实现的单尸路径失败。
- [x] 修改Fabricator.java：prepareFakeHunterDiaries()、completeFakeHunterDiaries(List<Integer>, List<Integer>)；独立选择器恢复while，legacy选择器原样单列。
- [x] 修改HunterGuarder.java：guardForDiary(int, IntUnaryOperator)及共用目标权重入口；真实guard保留死亡拒绝，日记无合法活目标返回0。
- [x] 修改MainLogic.java：capture → dielogic → complete → judgeend → snapshot；更新GameSnapshotRecordWriterTest.java为死后日记时序并保留晚CO/序列化断言。
- [x] 运行定向核心、Replay/兼容、HTTP与七配役特征；若黄金变化，先记录差异，不刷新。

### Task 2: 最终交付
- [x] 已取得本轮授权，仅更新7个摘要；完整683项、概率/资源/FatJAR和2项Swing均通过。
- [ ] 验收通过后检查Windows前序差异，备份保护数据，提交Linux本批，再使Windows拥有前序基线及本批提交；不推送/合流/部署。
- [ ] 完成提交后源文件/父链/保护哈希核验，报告Windows未实际执行这一限制。

## 当前执行记录

12项新增策略用例通过；其终局夹具的先前失败已修正并保留。旧基线8项特征通过，新策略8项旧黄金断言失败；9份原/新轨迹逐字段核验，仅非人非占灵的录制st/sh变化，其他行为和主随机逐字一致。尚未刷新7个黄金摘要，最终全量与打包待本轮批准。本轮Windows提交将连同已同步但未对齐的8adddbe/cbb23f5历史完成快进；保留其IDE/数据未提交修改，禁止git add全部。

最终验收完成：683/683，Swing2/2；原/新轨迹证据与Windows同SHA交付回执集中于build/fake-hunter-diary-restoration-20261111。仅交付手续待完成，执行结果以该回执为准。
