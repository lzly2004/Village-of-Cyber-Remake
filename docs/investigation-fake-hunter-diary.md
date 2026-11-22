# 假猎人护卫日记：单死体回避的历史查证（暂缓修改）

状态：IMPLEMENTED / VERIFIED。用户2026-11-02曾要求暂缓；2026-11-11续查后已明确批准恢复旧策略并保留晚CO历史与随机隔离。本轮完整验收已通过，实施/交付状态见文末，历史查证正文继续保留。调查基线44973680c32bfd70ea1a7e39d58219a3739ba817；本记录不是修复计划或规则变更授权。

## 本局证据和结论

用户4497368随机通常村截图：第5日公开报告“小说：薬師→職人→修道”，死体“教師→村娘→職人→学生”；护卫报告从技能日2开始，死体从日1开始，因此第3夜同时声称护衛職人且職人死亡。用户终局截图：小说真职业人狼、第5日処刑；職人真职业共有者、第3日狼噛。当前原生桌面与Web共用核心允许这种假日记矛盾，不是前端推算目标。

截图：/tmp/codex-clipboard-8d3eb521-f09a-4e1a-a706-975bb494aac9.png（第5日）、/tmp/codex-clipboard-4f32e2a5-2242-4a68-b7f0-c88107a9f56b.png（第8日）、/tmp/codex-clipboard-8139834f-fd54-4df7-818b-1539053e9ba0.png（终局）。临时路径可能被清理；本机持久副本另存于`build/web050/info-record/hunter-investigation/{day5,day8,settlement}.png`，`source-images.json`记录原路径、字节数与SHA256。文字观察保存在本文件；尚未获取该局导出Replay，不宣称复算其具体随机调用。

## 已定位的历史变化

1. **3b1502668d22d1a156d362ee1454327e5aa04294（v1.0.3，2026-01-01）** `vocr/src/MainLogic.java`，`frlyingaux` case3：夜死结算后生成两份猎文，初始权重50/50；狼(7)/狂信(9)在内部while中以10%概率继续重抽（倾向狼队友）；退出内部while后仍执行单死体检查。目标恰好是当夜唯一死体时，该候选权重降到1，最后按权重写skillTarget。是软回避而非禁止：一份死体一份其他→1/51，两份同死体仍可能/必然选死体。
2. **a0cf9af5223d574a73c8fc56d60bbde112f33271（trae重构v2.0，2026-06-22）** 提取`vocr/src/Fabricator.java`时，原内部while变成if，原break/continue原样保留。于是狼/狂信分支的break/continue作用于外层for候选循环，跳过下面死体降权，改变候选生成数量与重抽行为。此处是可由父/子提交确定的控制流语义变化，不能当作纯提取。
3. **0848f9d22207c7d7dd81342ed8addc4abad615d7（2026-06-28）** 仅把50/50和10%提取为常量；不能归咎于此处改预算/概率值。
4. **7b97fb881df67c8a9fcd1e03cb29ec8b993b3aab（修复晚CO猎人日志复盘缺失；作者时间2026-09-12 23:31+08，提交时间09-13 00:03+08）** 新增`prepareFakeHunterDiaries`，在MainLogic.nightaction死亡结算前确定/写入当夜假日记；取消COManager在晚CO时用当前存活情况补过去空日记。实际写入调用`selectFakeHunterTarget(num,List.of(),independentRandom)`，空死体使所有角色都不触发单死体降权。夜死后`preserveLegacyHunterRandomProgression`仍传真实死体运行生成器，但丢弃返回值，只为保留主随机流消耗，不能修正日记。
5. **b8ad851d1ac334221c20dd440b7eecb94473e4f0（隔离每局随机状态，2026-09-13）** 将遗留主随机调用改为ctx.getRandom，不恢复回避。其间`3f9681217b98bc9622b03771b3541817786e5e17`为类可见性边界调整。当前调用链仍如4。

早先Coder答复“当前桌面允许，不是Web迁移”只描述现状；继续历史查证后确认用户确实记得原机制，必须补充上述两次行为变化，不能说“原设计本来如此”而结案。

## 当前实现细节与源码入口

- `core/Fabricator.shouldMaintainFakeHunterDiary`：存活、未标非人、实际职业>=7；已claim猎可维护，未CO且不计划假占/假灵者可维护。已有当日记录不覆盖。
- `fabricateHunterEntry`：以配役/行动者/日数/人物与实际职业组成独立种子；仍复用HunterGuarder权重选择。独立日记不消耗主随机流；狼/狂信仍保留上述if分支，无当夜咬杀对象约束。
- `MainLogic.nightaction`：选择狼咬→真猎目标→假猎日记→DielogicCarrier死亡结算→判断终局→非人后处理。狼咬目标已经选出，但当前未作为日记约束传入。
- `HunterGuarder.guard`：剔除已死亡/自己，考虑公开/潜伏预约；占/灵/共CO权重、黑白球、非人标记与怀疑排序。排除“先前已死”不等于回避“当夜将死”。
- `DielogicCarrier.handleWolfBite`：wolfbite!=lietarget且不是狐才咬死。只有真实猎人目标参与保护，假报告不产生实际保护。
- `PublicInformationProjector.reports`与`UIHelpers.appendSkillResultLog`：护卫从技能日2，严格小于当前日/行动者死亡日；Web desktopVoteText只格式化公开目标，不计算实际技能。
- `GameSnapshotRecordWriterTest.nightlyFakeHunterDiary_shouldRemainChronologicalThroughLateCoAndReplay`：显式先生成假日记再把目标标为当夜被咬死，断言CO/Replay保留原历史。它覆盖不重编历史，不覆盖原单死体回避，也不证明策略自洽。

## 接续调查/决策入口

```bash
git show 3b15026:vocr/src/MainLogic.java
git show a0cf9af^:vocr/src/MainLogic.java
git show a0cf9af:vocr/src/Fabricator.java
git show 7b97fb8 -- src/main/java/com/vocr/core/Fabricator.java src/main/java/com/vocr/core/MainLogic.java src/main/java/com/vocr/core/COManager.java
git log --follow -- src/main/java/com/vocr/core/Fabricator.java
```

后续用户需要决定：恢复原概率软回避，还是另立绝不报告单死体的策略；晚CO历史生成/记录时点、主随机序列兼容与狼队已知咬杀信息如何处理。不要未授权就改为“硬排除”、刷新黄金或重新编造过去日记。若实施，作为独立生产Java/AI行为变更，新增分支覆盖并按AGENTS申请该轮全量，不混入纯UI批次。本轮没有为本局启动JVM/推进对局或修改核心。

## 2026-11-11：获批后的策略恢复

用户同意以旧机制为基准，恢复死后生成当夜假报告与单死体软回避，同时保留连续日记、晚CO不重编、Replay和独立随机；不是硬禁止所有夜间死者，不整提交revert。

- 结算前prepareFakeHunterDiaries只收集本夜演员；dielogic后、judgeend/终局或次日快照之前completeFakeHunterDiaries保存当夜目标。已有当夜非零记录不覆盖，未来CO不补造旧夜。
- 真正日记选择器恢复狼/狂信内层while；两份候选50/50、重抽10%、单死体降至1，常量未改。目标复用结算后的护卫权重，排除已死者和自己；无合法活目标不写假对象。
- 当夜死亡演员通过专用guardForDiary保留应有日记，真实guard入口继续死亡即返回0；该内部入口不提供实际保护，也不修改存档或API。
- 原selectFakeHunterTarget的控制流原样保留到selectLegacyFakeHunterTarget，仅消耗兼容主随机；新日记使用独立种子。两份方法体已逐字比较，legacy只改名，没有修其旧分支来扰动主随机。
- 12项策略定向用例通过；相关核心/HTTP/Replay首次150项中149通过，唯一失败是新终局夹具未命中分支；随后修正该用例的存活玩家/唯一处刑前提，最终12项通过。原有相关138项未受测试修改影响；不冒称单次150/150。
- 修改前隔离基线8项特征断言通过；修改后沿用旧黄金8项失败。9份逐行轨迹证明：变化只在非人且非假占/假灵的STATE录制字段st/sh；胜负/天数/演员状态/事件/投票/主随机序列均不变，交错局与单局逐字相同。旧黄金尚未更新，等待具体批准后仅更新7个摘要。
- 最终完整验收/FatJAR尚未获本轮批准，尚未执行或提交。Windows本批已有文件已核对预像，未覆盖其源文件；最终将对齐Git提交历史，而不只复制源码。

设计与计划：docs/superpowers/specs/2026-11-11-fake-hunter-diary-restoration-design.md、docs/superpowers/plans/2026-11-11-fake-hunter-diary-restoration.md。证据集中于build/fake-hunter-diary-restoration-20261111，包括原/新轨迹、逐字段白名单检查、拟议黄金值、首次失败和最终用例日志。Linux音频及其他未授权规则不在本批。

## 本轮最终批准与验收

用户明确批准仅更新FullGameCharacterizationTest的7个已核实摘要，并执行本轮全量验收/打包。未改变任何断言或冻结Replay夹具。标准完整test/check（含七配役特征）、概率560组合、运行时资源与FatJAR均通过：68个JUnit套件、683项、0失败/错误/跳过。自有Xvfb实际Swing窗口检查2/2通过，不读取用户存档、不播放音频；不是Windows运行证明或Linux听审。

命令：`VOCR_APPROVED_FULL_CHECK=1 XDG_RUNTIME_DIR=/tmp/vocr-wolf-hunter-runtime-1000 bash tools/run-gradle-safe.sh cleanTest check verifyRuntimeAssets fatJar --offline`。

FatJAR：724206454字节，SHA256=1ae341aa283b2d8b6e1316543d8eea72bf27aaca5f392616bf47d8f12e3a9b15。完整XML和后续Swing XML分目录保全，避免用定向测试计数覆盖全量。

Windows交付采用同一Git提交：先确认前序8adddbe/cbb23f5文件已经同步，确认无既有暂存修改；备份将覆盖的本轮文件及Git索引/HEAD/分支回执，再导入Linux本地提交对象，快进Windows同步分支和索引。此过程不checkout覆盖工作树，不把IDE或用户数据改动纳入提交；提交前后保护文件SHA必须一致。最终同SHA与备份路径以build/fake-hunter-diary-restoration-20261111/windows-sync-receipt.json为准。

用户可在Windows原工程执行Rebuild Project，再按原Game.java流程运行。尚未在Windows实际运行本轮代码，不推断其声音/流畅度已由Linux验收覆盖。旧Replay不重写；本批主随机兼容器保留旧实现只是兼容消耗，实际日记选择已恢复while策略，不能把该兼容方法误当成策略仍未修复。
