# 2063274 / f089bda复评处置

Reviewer通过，无阻断；真实push全门与真实PR红灯证据已独立复算。其617/189/136等是当次云端/外部读取证据，不作为本轮本机重跑。WF-005流程验证完成口径保留；main仍未更新，PR仍草稿。

## 逐项处置

| 项 | 本轮核实与处置 |
|---|---|
| P2-1 空白错误摘要全null | 采纳。仅diff-check套件启用Git空白诊断解析，case给首个文件:行号；first_error有界聚合问题数、文件数及原因。Git同一行合并trailing whitespace/space before tab也识别。保留原TAP/unittest/Playwright模式及未知诊断null，不对其他suite误用Git模式 |
| P3-2 DONE口径 | WF-005表格完成标准补“PR通路已验证（红），绿灯/合流归主线决策”；未将红灯写成PR通过 |
| P3-3 子命令exit2 | 文档明确子套件退出码不做全局语义映射；suite failed/exit2与统一入口拒绝exit2是不同上下文，下载器合法失败返回1的契约不变 |
| 历史空白四条标准 | 生成独立补丁：ExecutionManager 9行、GameContext 3行、ReplayPlayerHandler 12行；仅删240字节尾随空白，逐行其余字节/行数不变。不改diff-check、不改main，不把push旧绿灯当PR最新head通过；正式提交与PR转绿尚未完成 |
| fork / pull_request_target | 未创建fork；不增加pull_request_target或写权限。保持未验证，不以同仓PR推断fork通过 |
| 测试内容/JAR未独立重跑 | 接受证据边界；本轮工具补修不运行617/189/136，未下载JAR |
| 一方认证下载 | 不创建或提取token，继续注明保存ZIP与一方metadata模式，不将其称认证ZIP下载成功 |
| 空白历史归属未逐提交溯源 | 只声明相对main的24处，不未经blame核对指认某个旧提交；不影响字节级补丁范围 |
| LFS 32/31/620138120口径 | 路径总字节620138120；按OID去重的31个payload为605302710。复核原证据，两值同时成立，不把路径总量冒充唯一对象大小 |

## 兼容与验证

摘要字段改变会让旧revision 2 PR包（null摘要）与新组装结果不同，故新报告采用assembly_revision 3；下载器继续按revision 1/2原规则逐字段重算，不修改旧报告。未知修订拒绝，旧失败不转绿；游戏/Replay/result格式不变。

最终`python3 -B tools/tests/test_ci_workflow.py -v`：53/53（0.052s），其中证据28项是子集。新增覆盖三类诊断/CRLF/ANSI、合并原因、未知行号/错误不猜测、输出有界/转义、其他套件不受影响、revision 2失败包精确重算、revision 3摘要及unexecuted/exit2保留。真实Git探针验证三类原因：同一行合并两类原因，2处问题/1文件/3类原因；--no-index返回3，不以其为统一入口语义。

自纠：首次真实Git探针错误预期--no-index exit2，实测为3；修正探针前提后又发现Git会合并同一行两类原因，旧匹配只识别末条EOF诊断。先补用例，确定失败于首个case为null（1/1失败），再扩展白名单到实际合并格式；最终53项与真实Git探针通过，未放宽匹配为任意日志字符串。

六份保存的真实包最终复算：40080861974/40082353246按revision 1逐字段一致；40086327059/40149558683按revision 2逐字段一致；WF-004无包内报告的40076634386/40076072948以当前规则重算。原成功仍成功，首失败与PR失败仍failed。这是解析器本轮运行，不是重跑其Java/浏览器测试。

用原PR原始输入预览新revision 3摘要，得到case=src/main/java/com/vocr/core/ExecutionManager.java:48，first_error=git diff --check: 24 issues / 3 files (trailing whitespace: 24); first同一位置。该JSON明确是本地新规则预览，不冒充新增云端artifact；旧包原摘要仍保留null。

本轮AST、相关链接、定向diff-check及原137项porcelain/136文件hash核对通过。证据与独立空白补丁/行号/前后hash在忽略目录build/wf005-followup。

## 下一阶段与授权边界

本次先提交工具/文档补修，与Java空白分开。原Java工作区未动，24行补丁未正式提交；建议在既有隔离干净树应用，防止混入CRLF与无关修改。生产Java按AGENTS需另获本轮完整test/check/FatJAR授权后验收并提交；已有云端完整验收预授权不等于本机全量授权。

后续拟两步快进推送同一准备分支：先推摘要补修（历史空白仍在，真实PR红灯应直接给出24处/3文件），再推独立24行空白提交使最新PR重新完整验证。此范围尚未获新增推送授权，本地归档f089bda也尚未推送。不复用上一轮仅至2063274的推送范围，不强推、不改main/设置/secret、不自动合并。取得新授权后才执行；同SHA三次full仍归WF-007，不提前dispatch。
