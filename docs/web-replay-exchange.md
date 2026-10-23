# WEB-030：Replay 跨端交换契约

范围：定义既有格式与后续入口义务、建立跨语言兼容样本。本轮不改生产Java/HTTP/Replay文件格式，不接入浏览器保存、播放或导入导出。基线b3e09cc（029评审补修）；对应实现归031—035。

## 1. 已核对的实际入口

| 端与调用点 | 当前实际行为 | 后续缺口 |
| --- | --- | --- |
| StartSceneHandler → ReplayBrowserHandler | 标题Replay按钮进入9槽位浏览器，3页各3格；左键已有槽位播放，右键把当前recorder写入选中槽位，可确认覆盖 | 没有文件选择器、独立导入或导出按钮；右键保存不是导出到用户选文件 |
| ReplayBrowserHandler.saveReplayToSlot | `ReplaySave.fromRecords(slot, currentRecorder.getRecords())` → `ReplayManager.saveToSlot`，之后结束/停止recorder | 现有可用性检查只看记录非空，不证明对局已终局；Web新保存边界须单独门控 |
| ReplayManager / PathConfig | 内部`data/replay_saves.json`是`ReplaySave[]`，MAX_SLOTS=9；启动自动读取，槽位slotIndex为0—8 | 当前坏文件处理可能重命名备份/重建、过小文件删除，保存失败仅日志；禁止把此破坏性加载路径直接当文件导入验证器 |
| GameSession.recorder / MainLogic / GameRecorder | 桌面迁移期暴露recorder；记录完整角色、技能与快照，GameSnapshotRecordWriter写STATE/VOTE；不是现有公开投影 | 031隔离保存端口，不把GameContext/recorder直接暴露给网页；不改变录制与随机调用 |
| GuestGameServer / Web App | 现有API仅实时公开对局；无Replay GET/POST、无本地Replay库/文件入口 | 031保存端口、032本地列表/删除、033/034播放、035双方文件入口均未实现 |

结论：目前不能声称“已经双向交换”。035必须交付桌面可操作的文件入口（先读入隔离对象、校验、用户选槽和确认后写入），不能让用户手工覆盖正在使用的整份data文件来冒充验收。030只用临时文件复现既有序列化/加载行为，不动用户data。

## 2. 文件封装（冻结现有JSON，不另造生产格式）

- 单个交换文件使用UTF-8 JSON，推荐名`vocr-replay-YYYYMMDD-HHMMSS.json`，MIME application/json；扩展名/MIME不代替解析校验。JSON文本允许LF/CRLF，无BOM为导出标准；导入允许一个UTF-8 BOM。禁止压缩、JS模块、HTML、Java序列化和自动联网取资源。
- 顶层保持桌面9元素槽位数组，每格为ReplaySave对象或null；至少一格有效。单局导出放第0格且slotIndex=0，其余null；多局按用户选择装入最多9格。该slotIndex仅是**文件位置**，导入必须由用户另选本地槽位，不按它静默覆盖现有数据。
- 不增加外层`{replays:...}`、云账号、sessionId/凭据、浏览器数据库ID或媒体；浏览器内部ID、导入来源/时间等另存本地元数据，不混入交换文件。
- ReplaySave.version当前为3，不增加格式版本。接受显式历史0/1/2并迁移到3；不支持的未来版本、缺少version或无法确认结构应明确拒绝，不猜测新语义。**这是后续导入入口要求**：当前ReplayManager会宽松接受未来版本，缺version还会因Java字段初始化默认为3，不能据此声称已有严格校验。
- 拒绝路径/URL型引用和原型污染键；未知普通字段不渲染/执行，保留原始有效对象供无损再导出。当前Java mapper忽略未知字段，故桌面重写后未知字段不保证保留；双向验收以已知语义字段与rawRecords为准，不能宣传未知字段字节无损。
- JSON成员顺序、缩进、换行以及派生displayName可变化；原始文件下载备份可字节保留，但迁移后以语义一致而非文件hash一致判定。不得排序、去重或重排rawRecords/快照/票型/技能数组。

## 3. 已知字段及索引

| 层 | 字段 / 语义 |
| --- | --- |
| ReplaySave | version=3；slotIndex 0—8；saveTime为无时区`yyyy/MM/dd HH:mm:ss`显示值，不当唯一ID或可靠时间线 |
| 配役/计数 | peiyiName为jianyi/tongchang/yaoohu/kuangxin/beide/maoyou/daxing，peiyiOrdinal为1—7，NONE=0不算正常配役；拼写yaoohu保留。peiyiVillageCount/totalVillageCount在v1迁移/fromRecords时未知归-1；直接v2/v3缺失或零会保留Java默认0，界面均按未知处理，不合并入浏览器累计战绩 |
| 汇总 | totalDays、endResult（0未完/1村/2狼/3狐）、totalEvents。totalEvents=rawRecords.size，是**字符串条目数**而非HTTP事件数/可显示台词数。当前录制不是HTTP presentedEvents/确认流的副本，不能据此保证逐条台词/本地动画重放 |
| 内容 | daySnapshots有序数组、rawRecords有序字符串数组；rawRecords一项可含多行JSON、Unicode、特殊符号，不能用`split('\n')`拆成新条目 |
| 派生字段 | Java Jackson还可输出getPeiyiDisplayName/getResultDisplayName产生的peiyiDisplayName/resultDisplayName；仅显示缓存，不作为枚举/结果来源 |
| DaySnapshot | dayNumber、aliveCount、players、voteRounds、dailyVotingRule、greyTargetCharNums、designatedTargetCharNums；日期从1开始，不把数组下标等同日期 |
| PlayerStatus | playerIndex从1开始；characterNumber为44人物外观序号，与playerIndex不同；actualRole、claimedRole为Role ordinal；claimedRoleOrder、comingOutDay（未CO=-1）、deathDay（未死=0）、deathReason、nonHumanMarked、skillTarget、skillTargets |
| 票型 | dailyVotingRule -1未记录/0自由/1灰/2指定；VoteRound.round从1开始；votes为有序[voterPlayerIndex,targetPlayerIndex]，原录制可含target=0（无有效票）；不能强行当人物外观序号 |
| 候补名虽误导 | greyTargetCharNums实际保存玩家索引（writer votingCandidates、formatter characterNumber(snapshot,index)）；designatedTargetCharNums实际保存人物外观序号。保留字段名与两种索引，禁止统一改写 |

STATE文本每人`{i:n=...,r=...,cr=...,cro=...,dd=...,wd=...,nm=0/1,cod=...,st=...,sh=0/...}`。st是快照日-1的技能值；sh保留下标0，按技能日存完整历史。其他rawRecords如注释、JSON|GAME_METADATA（可能多行）、INIT|roles、DAILY_SNAPSHOT、VOTE_METHOD、VOTE_DETAIL、END均原顺序保留，不假设它们是EventName台词事件。

## 4. 旧版迁移与显示兼容

现有ReplayManager：显式0→1；1→2补村数未知值-1；2→3只提升版本，不补造历史。ReplaySave.fromRecords优先JSON元信息，读旧#Peiyi/#VillageCount兜底；解析DAILY_SNAPSHOT，完全没有时才尝试STATE重建；totalDays取已知结束日和最大快照日的较大者。当前桌面会宽容解析错误，后续导入不可把“没抛错”视为文件完整。

技能兼容必须保留两条路径：v3 `getVisibleSkillTarget`选择不晚于visibleDay+1的最新、包含该技能日的完整history；**零值也是该历史的值，不再回退旧st**。无完整history则取skillDay+1快照的st。球值>本局人数表示黑，目标=值-人数；其余有效正数为白/护卫目标；0无结果。玩家索引不能拿characterNumber代替。旧版没有history时保持空数组/原st，不自行填补过去未知值。

显示依据：先核对局内VoteInfoRenderer/UIHelpers，再记录Replay现有差异：局内占卜从1、灵能从2、护卫从2；ReplayTextFormatter占卜/灵能从1、护卫从2。ReplayPlayerHandler图标路径又按d=2..day的快照st并可回退actualRole；getSnapshot优先dayNumber，缺失再按day-1数组位置回退。030不统一这些行为；033/034要以冻结样本逐项对照，失败/缺日期明确诊断，不按常识补造。

死亡原因保持whyDie ordinal 0—7（NONE/处刑/白天猫咒/白天后追/被咬/夜猫咒/夜后追/咒杀）；角色ordinal保持Role.java 0—11；已知结构字段的JSON number使用安全整数（rawRecords中的seed可超出JS安全整数，应保留字符串原文，不把嵌套JSON解析后舍入），不能用网页英文deathReason字符串覆盖文件整数。

## 5. 新入口的验证与信息边界（031—035义务）

先完整读取、在隔离内存解析和校验、预览所有候选局/诊断，再由用户确认目标槽/库；失败不改现有好数据，不调用ReplayManager.load(用户路径)，不发起命令或下载外部资源。容量/配额失败明确并可导出，重复导入提示重复而非重复累计战绩。

新入口预算：单文件8MiB、最多9局、每局最多50个日期快照/44玩家、20000条rawRecords、每条64KiB；用于有界内存解析，035实测并报告边界样本，不擅删/截断。数量不合法、重复玩家/日期、未知枚举、非安全整数、损坏结构/截断/不支持版本、汇总与内容矛盾都应明确拒绝或进入**不写库的兼容诊断**；不能静默重建新局。历史缺失值只有上述已知默认值可以迁移；界面显示未知，不制造角色、技能或胜负。

Replay包含实际角色、全部技能、seed及未来日信息，是明文剧透数据，不是身份保密层。历史endResult=0仅作为“未完”资料只读处理，不允许恢复为进行中对局；浏览器新保存只面向终局。当前在途对局不提供完整Replay；后续保存入口须在权威FINISHED且前端全部事件已确认后启用，服务端/保存端口也校验终局，不能仅靠隐藏按钮。刷新、CO中断、确认游标、真实身份揭晓门槛均不因保存改变。共享文件不得夹带会话凭据；seed可作为录像原文资料，不能据此宣称可重新运行得到同局（未冻结随机源、配置及版本）。

## 6. 样本与验证边界

- 既有`src/test/resources/replay/legacy-record-v0.txt`、`legacy-v1.json`原件不改；新增`exchange-v3.json`九槽最小合成文件及`README-exchange.md`。样本含17条rawRecords/4快照/2轮投票、三人物索引与外观序号分离、late history和显式0、旧st、Unicode/多行/大seed。总人数3是边界合成样本，不冒充真实简易村12人的完整对局。
- Java实跑：`bash tools/run-gradle-safe.sh test --tests 'com.vocr.replay.ReplayExchangeContractTest' --tests 'com.vocr.replay.ReplayCompatibilityTest' --tests 'com.vocr.ui.ReplayTextFormatterTest' --offline`，10/10（新4+旧2+文本4，4s，详见忽略目录build/web030/java.log）。实际ReplaySave.fromRecords重建v3与固定JSON逐字段相等；隔离ReplayManager加载/保存语义相等；显式v0/v1/v2迁移不补造history，v3的cutoff/零值/旧st分支锁定。
- 独立JS实跑：`node --test tools/tests/replay-exchange.test.mjs`3/3（35ms），检查JSON往返、Java枚举对应、两类索引、opaque大seed/多行/Unicode、BOM/CRLF运输及截断JSON拒绝。首次2/3因测试把44号误写成Beatrice，核对枚举更正为Helmut后3/3，未改生产映射或样本数字。沙箱内Node子进程输出受限，宿主机执行取得上述结果。
- 没有新增生产导入解析器，故上文严格验证、配额、坏文件不写库等是031—035必须实现的义务，**本轮没有自动化证明其已经实现**。不运行浏览器/前端构建/Java全量test/check/FatJAR：本批仅文档、新测试和样本，生产路径及构建配置不变。029补修的102/99及构建是同轮前一提交的证据，不当作030重跑。
- 新Java测试改变了web017 oracle的源码指纹。下次需要gate时须按既有流程重新生成同源oracle，不改黄金轨迹或放宽指纹断言；本批不因只增测试而运行整套七配役gate。

030兼容测试不等于035真实双向UI交换验收，不关闭A09/A10。日期缺失与技能展示差异继续归033/034，入口实际选择/写入/重新读取及未修改好数据仍归035。

## 032实施更新

以上030“当前入口”表记录其基线状态，非032现状。031已隔离不可变录制/桌面适配；032新增终局只读HTTP和浏览器IndexedDB保存/列表/查询/删除，详见[本地库](web-replay-library.md)。文件封装仍为原v3九槽，内部内容ID与保存时间另存，不夹带凭据；播放/技能/历史迁移/文件交换及失败导出救援仍待033—035，A09/A10不放行。
