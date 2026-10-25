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


## 用户授权后的两步发布与独立空白提交

2026-10-25用户明确授权本轮完整验收及两步推送，包含必要工具/证据归档的原准备分支快进，不含main/设置/secret/强推/合并。第一步2063274→0b28449（含已审本地归档f089bda），第二步0b28449→3b8641c；未提前推送未验收Java。

### 真实新红灯：P2-1生效

PR run40153657465，head/tested=0b28449，event=pull_request，status=failure；仅diff-check失败，后续19项unexecuted。revision 3包复算精确一致，case=src/main/java/com/vocr/core/ExecutionManager.java:48；first_error=git diff --check: 24 issues / 3 files (trailing whitespace: 24); first同一位置。证明新摘要在真实PR生效，不只是本地预览。

主artifact ID12583367712，466566字节，SHA256 dcccabeb034a4dbe97cdc204d2f48fadd388d6ded547cec9b55ad764e6c325e0；receipt ID12583288077，464字节，SHA256 e8cefaa56ae8fd2dfd6e5ae5681e49aef32ff5c3d3de75bade830c961d2d5653。API digest、报告逐字段重算及回执身份关联均通过。

同head重复push run40153653112被平台现有并发策略取消，不计为通过。3b8641c的重复push run40153986129也被平台自动取消；曾准备核对并取消冗余push，但打开页面时已显示Cancelled，未执行手动取消。未修改workflow并发配置。

### 独立3文件24行提交与本机完整验收

3b8641c仅删行尾空白240字节：ExecutionManager.java的9行、GameContext.java的3行、ReplayPlayerHandler.java的12行。逐行核对其余字节与行数均不变，增量diff-check及--ignore-space-at-eol比较通过；提交只含3文件，未改逻辑、规则、格式、依赖或diff-check。

在既有wf003-clean隔离树实施，原仓库Java文件字节未修改。验收后仅同步三个目标索引条目与准备分支快进引用，不检出覆盖原脏工作区；源树137项porcelain与136文件hash保持。没有全仓规范化或整体暂存。

本机实际先执行当次浏览器导出前提1/1（6.9s），导出21980字节，SHA256 323ef3dd104bdc78c76c442a4c89905a3955927a79875e26f55d2e67bfa15c47；随后VOCR_WEB035_BROWSER_EXPORT指向此新文件，通过既有安全入口运行fatJar --offline --rerun-tasks，33s成功、9任务实际执行。61份当次XML按正文独立核为617/617，失败/错误/跳过均0；完整test/check、七配役特征轨迹、概率与资源检查、FatJAR均通过。JAR724197819字节，SHA256 2e9a59594169287ba97efb14a055fa62d16eaf0ac4dc6d82fe9925347a0af57c；该值为本机实算，不冒充云端JAR字节证明。

首次浏览器前提命令因将grep锚定在完整标题开头而No tests found，未启动后续完整Java；保留失败日志。Playwright完整标题有文件前缀，移除错误的起始锚定后1/1通过，没有改断言、等待或重试配置。使用已验证可用的DISPLAY=:1（旧env.sh的:2已无服务），未修改系统或启动额外显示服务器。本机启动前MemAvailable约3.6GiB，原2GiB门槛/监护保持，未并行JVM；结束后测试服务/JVM/Vite/headless-shell未见残留，未结束用户应用。

3b8641c的require-clean/LFS/oracle输入审计exit0、failures=[]，15项oracle同源：Java225文件hash ff7cb30010c0ad6ffc53c890346d134d2ab0f45c7da22c34960f7b81df6f1741（仅空白导致原始blob指纹变化）；冻结9文件6ce8aa54…、输入1979文件3d4c43fa…不变；build_policy44文件f355d13a9958e7dbe878f9920d950497da5d3c6e774e61db5e9e463662d264b2。

本机Git2.25.1跨main长历史时因不严格重命名搜索上限会额外报告旧移动文件的空白：未修补前53处，24行补丁后仍29处。仅用一次性-c diff.renameLimit=10000做原因诊断，分别得到24与0；未改任何持久Git配置/CI命令，不用该参数作为正式门禁放行。独立补丁自身的标准增量检查通过；最新真实PR用原标准命令检验main→3b8641c，是全区间验收依据。此环境差异已披露，不扩大补丁到另外29处。

本轮原始日志/XML/manifest/输入审计/字节证明保存在build/wf005-followup；完整本机JAR保留隔离树build/libs，未复制进Git。


### 最新head真实PR完整绿灯与取消检查补验

[PR run40153989566](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40153989566)，attempt 1，2026-10-25北京时间10:19:07–10:37:20，event=pull_request、head/tested=3b8641c3bf6e4bcc98cda8fa738862d702c738c0、base=main@616362073999cb280ef8891a8f69272541904357、base_source=pull_request.base.sha，completed/success。原标准main→head diff-check通过，无白名单或门槛改动。

20项套件/内部步骤全部passed：diff-check；guarded15、audit14、verify32、ci-tests53、image-tool17、audio-tool18、exchange3、frontend-unit189；web-build；assets4、audio27、files-repeat30、default-e2e136；browser-file；java-full617、java-results、oracle-audit、gate15、desktop-exchange1。测试失败/错误/跳过/取消/flaky/interrupted均0；first_failure=null、unexecuted=[]，manual_gates六项仍not_covered。53中的证据28项是子集，不重复相加。Java/XML重算与本机同源指纹一致；本机并未重跑189/136/15，这些是本轮云端实跑。

主artifact ID12583743755，2273678字节，SHA256 8967eec76ce8c5866aafd485b421bd90c61cfbc73f41fed944aaa9f6711a4e17；receipt ID12583608929，467字节，SHA256 d8f8b4ad17ced1d5585259da8ab0b78c97ba2a85ef0f153537297d835c72c982。报告逐字段重算、全部数量/指纹及回执run/attempt/head/id/digest对应均通过。

PR成功后，查询最新check-runs发现重复push的attempt 1取消仍使整体为unstable，未直接宣布“所有检查全绿”。在既有本轮云端验收授权内仅重跑该push一次，未改代码/配置、未开启debug日志：[run40153986129 attempt 2](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40153986129/attempts/2)于北京时间10:41:55–11:00:21成功，event=push、head/tested仍3b8641c、base=0b28449、base_source=push.before。不得用run原始created_at冒充attempt 2开始时间。20项同数量全部通过，原取消attempt 1证据保留。

attempt 2主artifact ID12585325239，1832669字节，SHA256 82717e0d2268b1a399bd2a6a82e30f51c0393da04d4ae8b81043f32f8a451914；receipt ID12584389798，466字节，SHA256 1594bccbfa72cf91c69c55aa954507ac64edaea3a4828fb7e885c4813295712e。严格选择名称尾缀-2的产物，API digest、包内文件hash/报告逐字段重算、全部测试计数/指纹、回执关联均通过。云端两次报告的JAR均724197819字节、SHA256 d7232b8cc37d0c761924936b496c3b055866021aef372ae18681d31bd94d18a0；该值来自云端记录，未下载724MB JAR，不宣称与本机字节相同。

最终API两个现代verify check均success（PR 121051555997、push补验121055754091），PR #1 draft=true/open/merged=false、mergeable_state=clean、169提交；准备分支远端3b8641c，main仍6163620。旧legacy status total_count=0不替代现代checks结论。PR原正文已同步结果，未发评论、邀请reviewer、转ready或合并。

所有ZIP通过公共nightly.link取得，逐个与一方GitHub REST artifact元数据的字节数/SHA256核对，再做有界ZIP/包内hash/完整报告重算与receipt校验。未设置或提取token；不把这条路径称为一方认证ZIP下载成功。原始API/ZIP/复算结果/回执、取消attempt及最终check状态保存在忽略目录build/wf005-followup，独立审查可按run/attempt重新下载复算。

### 收尾边界与下一步

本轮有两次相同3b8641c的完整云端成功（真实PR与push attempt 2）；取消不计通过，这不是WF-007“三次同SHA”观察或Auto-Fix授权。双事件重复完整执行的成本已观察到，但未擅自调整并发/触发策略。

本地四份文档仅补本轮事实、链接与状态，按文档级核验，不为归档重复启动Java/浏览器套件。归档提交保留本地，不继续推送触发同一长历史PR完整验收；已测/已推代码head明确为3b8641c。源工作区137项porcelain/136文件hash保持，未整体暂存；测试工具进程已退出，用户浏览器保留。

WF-005流程与本次补修完成，主线仍需独立审查及用户明确合流决策。fork事件、一方认证在线下载、runner镜像内部、正式人工音频/浏览器/设备、容量及发布批准仍未放行；WF-006/007仍PLANNED，WEB-036仍暂停，不提前开发。
