# WEB-030兼容样本

- `legacy-record-v0.txt`、`legacy-v1.json`：既有最小回归原件，保留内容。前者为旧录制文本，不是本轮承诺可直接导入的交换文件；后者为9槽JSON且含未知字段，测试桌面已知迁移/忽略行为。
- `exchange-v3.json`：本轮新增最小**合成**9槽文件（一个非空槽），只用来固定序列化语义，不是完整真实对局或规则正确性轨迹。包含3人物外观27/28/44、1基玩家索引、两轮票型与0票目标、两类候补索引、延后history/显式0覆盖、次日快照、Unicode和一个包含多行的JSON|条目。
- JSON|元信息seed为9007199254740993，超出JS安全整数，但它处于rawRecords字符串内：必须原文保存，不先parse再舍入。所有已知结构字段仍为安全整数。
- `ReplayExchangeContractTest`从rawRecords调用实际ReplaySave.fromRecords重建，并用隔离ReplayManager读写与原结构比对；0/1/2由旧v1样本构造测试变体，不伪造历史文件出处。
- `node --test tools/tests/replay-exchange.test.mjs`是独立JS读写检查，不是生产浏览器导入器。真实文件选择/写库/双向UI交换仍归035，A09/A10不放行。

格式/边界及入口缺口见`docs/web-replay-exchange.md`。所有Java读写测试只用TempDir；禁止试用真实data目录。

## WEB-034展示样本

`web034-display-cases.json`为12组独立固定期望的合成快照，不是交换入口/真实对局样本，不用于从rawRecords重建：多数组沿用030样本再明确变更已知展示字段。Java `ReplayWebDisplayContractTest`用实际ReplayTextFormatter逐项核对normalText、职业分组与投票左右列；网页单测读同一份期望核对文本与结构化表格行。包含晚到history/显式0、空history旧st、Replay灵能日1、死亡与破绽、位置回退、三种投票/弃权/多轮、晚CO护卫日记、猫咒/后追与狐/背德死亡条件。只进测试资源，不打入页面或生产录像；原030文件不改。

## WEB-035真实桌面样本与文件边界

`desktop-saved-v3.json`来自隔离Swing窗口实际运行SeededGuestSession(35035)、原右键保存和新文件选择器导出；12人简易村、7日、33条rawRecords、村人胜，saveTime为2026/10/24 00:01:16。不是手写快照或用户私人对局；SHA-256为d36135d531e3046afaf366c00e7c92d3de3e7b11e4adb101ad5d152577ce6487。实际浏览器导入/下载后，再由隔离桌面导入槽8并再导出，对比已知字段、快照和rawRecords。运行期新产物保留在build/web035，不覆盖冻结原件。

`web035-file-cases.json`含30个共享正负例变换；两端生产解析器独立验证。含已知0/1/2迁移、普通未知字段与只读UNFINISHED；原legacy-v1.json的灰名单[30]越界，文件入口明确兼容诊断，不猜测修复。详见docs/web-replay-files.md；人工及容量首发门槛仍开放。
