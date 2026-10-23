# WEB-027：BGM、浏览器解锁与生命周期

基线aad8ed3，直接复用[026音频](web-audio-assets.md)，不重新转码或改变manifest。本轮只接BGM；音效、音量/静音及设置恢复归028，A08尚未放行。实际命令/失败与结果集中见[review-aad8ed3.md](review-aad8ed3.md)。

## 展示来源与切换

只取当前公开展示阶段、公开存活数/日期、已展示结算的公开胜负，不读真实职业、技能或待播事件列表。服务器可能已计算到FINISHED，仍以currentEvent.stage控制曲目，不按phase提前奏结算曲。

| 当前展示 | BGM |
|---|---|
| 大厅 | START_MENU |
| 恢复中、同步不确定、未知阶段/胜负 | 无音 |
| NIGHT_RESULT、EXECUTION | 无音（桌面GameSceneHandler.renderNight及DialogueChuxingHandler.stopBgm） |
| DAY_DIALOGUE、AFTERNOON_DIALOGUE、白天决策 | 新一天默认EMOTIONALLY_UNSTABLE；实际整数条件命中3/4存活者用LIGHTLESS_GAZE；同一天的对话/CO/决策不重启或覆盖用户选曲 |
| 当前SETTLEMENT或读完后的终局 | VILLAGE_WIN→VICTORY_SCREEN；WOLF_WIN/FOX_WIN→DEFEAT_SCREEN；未知结果无音并保留原场景诊断 |

白天可决策时“曲を切替”复用GameSceneVoteHandler顺序：从manifest正常13项或紧张4项列表的`count % length`选择再递增；同页跨天/对局保留count，刷新归零，无新增存储。CO期间保留曲目但不能操作选曲；新一天恢复默认。回大厅切START_MENU。帮助/Replay尚无页面，不提前接INFO/REPLAY用途。

源条件是`(aliveCounter - 1) / 2 == 1`，Java整数除法命中3、4人；注释“≤3”不作为改动依据。单测从当前UIHelpers、GameSceneVoteHandler、StartSceneHandler、EndGameHandler及停止调用点读取断言，并验证私有/未来字段的抛错getter不被访问。

## 手势、失败与释放

初始、刷新后均关闭，不请求音频目录、不创建Audio/AudioContext。只有专门的日文启用按钮（可键盘操作）才同步创建/恢复AudioContext，再异步下载所选曲；不借开始对局/确认事件偷启音频。按钮在舞台之外，逐事件GameStage重建不重建播放器。

基于一个HTMLAudioElement + MediaElementAudioSource + GainNode；资源层完整SHA/长度校验后创建当前曲目的Blob URL，不预取、不用decodeAudioData积累全库PCM；native media.loop循环。固定Gain=0.25（-12.04dB），相对026测得最高+3.859dBFS提供单路BGM约8.18dB数值余量。不交叉淡入混音、不改变源电平；这不是声卡/扬声器无失真保证，028音效混合后仍须重新核对。

切曲前暂停旧媒体、移除src并load、撤销旧Blob URL、中止下载；版本号拒绝晚到数据/play结果。相同曲目不重启；无音场景也立即释放当前数据，不回退其他曲。仅保留一个上下文/媒体节点，停止时suspend设备且清空资源，重新启用复用该上下文。组件卸载断开节点、close上下文；pagehide非BFCache离页close，BFCache则停止/suspend，恢复后需重新点击。后台visibilitychange停止，回前台不自动恢复。浏览器自行suspend/interrupted时失败关闭，提示重新启用，不抢占用户操作。

网络/完整性错误、媒体错误、拒播、解锁未成功及超时均显示独立日文诊断/有限错误码，不显示原错误或用户输入。控制器解锁/加载播放15秒超时；保持026目录共享15秒超时且拒绝后不重试的语义。显式重启可重试曲目，但坏目录需刷新后重建资源实例，不循环重试。音频失败不影响游标/投票/同步/退出。

参考原生API：[AudioContext.createMediaElementSource](https://developer.mozilla.org/en-US/docs/Web/API/AudioContext/createMediaElementSource)、[HTMLMediaElement.play](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/play)、[load](https://developer.mozilla.org/en-US/docs/Web/API/HTMLMediaElement/load)。播放状态只在play Promise成功且context为running后标为“再生中”。

## 验收边界

`npm run test:unit`含场景/生命周期/乱序/异常及manifest.policy漂移断言；`npm run test:audio`以静态preview运行资源及真实媒体BGM测试（无JVM），`npm run test:assets`保留首页关闭音频的首屏预算。`npm run test:e2e`安全入口启动后验证真实会话中音频操作无API写入、无游标/存储变化，并回归既有功能。单worker，不并行JVM/浏览器套件，不降低内存门槛。

自动化已测/待测数字以当轮记录为准。native loop跨越末尾只证明持续循环，不证明跨MP3拼接零间隙；没有逐曲人工听审或物理声卡输出验收。A08仍待028的音效、设置、混合/听审和最终统一验收，046复核其他正式桌面浏览器/设备策略。

FFmpeg继续通过现存临时二进制和FFMPEG环境变量用于构建门禁，不进入播放器。持久化工具链及内存工具候选不含在本任务授权内，单独安排；/tmp清理后须按026说明重建开发环境，不能宣称已解决工具持久性。
