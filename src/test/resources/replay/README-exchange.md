# WEB-030兼容样本

- `legacy-record-v0.txt`、`legacy-v1.json`：既有最小回归原件，保留内容。前者为旧录制文本，不是本轮承诺可直接导入的交换文件；后者为9槽JSON且含未知字段，测试桌面已知迁移/忽略行为。
- `exchange-v3.json`：本轮新增最小**合成**9槽文件（一个非空槽），只用来固定序列化语义，不是完整真实对局或规则正确性轨迹。包含3人物外观27/28/44、1基玩家索引、两轮票型与0票目标、两类候补索引、延后history/显式0覆盖、次日快照、Unicode和一个包含多行的JSON|条目。
- JSON|元信息seed为9007199254740993，超出JS安全整数，但它处于rawRecords字符串内：必须原文保存，不先parse再舍入。所有已知结构字段仍为安全整数。
- `ReplayExchangeContractTest`从rawRecords调用实际ReplaySave.fromRecords重建，并用隔离ReplayManager读写与原结构比对；0/1/2由旧v1样本构造测试变体，不伪造历史文件出处。
- `node --test tools/tests/replay-exchange.test.mjs`是独立JS读写检查，不是生产浏览器导入器。真实文件选择/写库/双向UI交换仍归035，A09/A10不放行。

格式/边界及入口缺口见`docs/web-replay-exchange.md`。所有Java读写测试只用TempDir；禁止试用真实data目录。
