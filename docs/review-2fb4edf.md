# 2fb4edf评审处置（WF-005补修）

范围：54a39ae、00dd98f、2fb4edf，reviewer通过、无阻断。其公共API/ZIP digest、回执、字段复算、指纹及50bf47b6… LFS聚合核验均属独立评审证据；不作为本轮重跑Java/浏览器的声明。

## 逐项处置

| 建议/观察 | 核实与本轮处置 | 验证或后续归属 |
|---|---|---|
| P3-1 not_covered重复release approval | 确认基础列表与result拼接产生重复；新报告按首次出现顺序去重 | 新断言覆盖跨列表/列表内重复及人工门槛仍未关闭 |
| P3-2 artifact先于run | 确认两次同位置插入会倒序；改为一次插入run→artifact→digest | 新断言要求三者顺序及位于suite表之前 |
| P3-3 脚本路径入口 | 采纳最小文档方案，文件头明确`python3 -B tools/tests/test_ci_workflow.py`，不承诺`-m unittest`兼容 | COMMANDS原调用不变；实际按脚本路径运行50项 |
| P2-1变量作用域、账本as_of、JDK差异边界 | reviewer确认已处理；本轮不改既有结论 | 不因同尺寸/同输入推断JAR差异原因 |
| 47/49测试发现口径 | 保留共享Fixture，未重新引入继承重复发现 | 原47加3个独立用例=50，其中证据25项（原22加3），不能相加称75项 |
| 真实PR/fork事件 | 仍无真实PR，不能将夹具当端到端证据 | WF-005保持DOING；准备具体草稿正文后另请用户批准创建。普通继续不代替此前明确排除的PR授权 |
| 617内容、724 MB JAR | 本轮不重新审每条Java用例，不下载JAR | 原记录仍标注计数来自XML重算、JAR hash来自CI wrapper |
| 一方在线ZIP下载 | 无新增凭据或认证下载实测 | 保留AUTHENTICATION_OR_ACTIONS_READ_REQUIRED边界，不以离线通过冒充在线下载 |
| runner镜像内部未锁版本 | 保留声明，不扩大本轮为镜像/apt锁定工程 | CI环境记录与可复现性边界继续有效 |
| main合流决策 | main仍旧基线；165提交差值是评审观察快照，不是未来实时值 | 草稿PR与main更新分开决策；不修历史Java空白、不改main或放宽diff-check |
| WF-006/007与同SHA三次full | 仍PLANNED；本轮不做Chat/Auto-Fix试点或额外dispatch | 同SHA三次完整CI及失败注入仍为WF-007前置，人工/容量/发布门槛不关闭 |

## 历史证据兼容

去重改变证据字段，若直接拿新规则对旧包比对，会错误拒绝已发布的WF-005证据。因此schema 1增加组装修订标识`assembly_revision: 2`；缺该字段的旧WF-005包按原revision 1精确重组（保留原重复项、无新增字段）。未知修订拒绝，仍要求全部字段完全一致；不先归一化包内报告，也不忽略状态/门槛等字段差异。没有包内报告的WF-004包仍从原始输入重新计算。

用例覆盖旧报告重复项完整保留、修订类型/数值拒绝、状态/列表/修订标识篡改拒绝，当前revision 2包仍走既有逐字段复算用例。未改result格式、游戏/Replay、Java、生产依赖、授权作用域或进程监护。

## 本轮实际验证

- `python3 -B tools/tests/test_ci_workflow.py -v`：50/50（0.071s）；其中证据25项也单独运行通过（0.045s），属于重叠范围。
- 四份保存的真实API元数据与ZIP再次校验digest并复算：40080861974（旧WF-005 full）及40082353246（旧WF-005 docs）逐字段一致、passed；40076634386（旧WF-004 full）passed；40076072948仍failed、未执行项保留。证据在忽略目录build/wf005-review；这是本轮解析器运行，不是重新执行617、189、136或15项。
- Python AST、文档链接与定向diff-check通过；生产成功启动命令未变，无本机JVM/浏览器/全量构建。
- 原137项porcelain差异及136个原文件hash保留，不整体暂存，不修改用户数据或系统配置。

本地补修提交前状态：尚未推送，没有新云端结果。真实PR创建、预期历史空白拒绝取证及main合流仍待明确授权，WF-005不标DONE。


## 授权发布与真实PR闭环（2063274）

用户随后明确授权“推送并创建草稿PR”，范围为准备分支快进至2063274及面向main创建草稿，不含main更新/设置/secret/强推/合并。本次只推送2063274（2fb4edf→2063274）；创建前实时核对main仍616362073999cb280ef8891a8f69272541904357、head为206327447b76317a0d2cc58492cf0f2fc5297757，共166提交。没有因浏览器连续超时重复提交；用户完成登录并重新打开页面后，改用辅助功能交互创建成功。

[草稿PR #1](https://github.com/lzly2004/Village-of-Cyber-Remake/pull/1)已创建并附于当前对话，保持draft/open，未邀请reviewer、发额外评论或合并。正文明确包含已有长历史与预期空白拒绝，不将其描述为仅8文件补修。API核对真实base/head与draft=true；UI截图另存/tmp/wf005-pr-1.png。

### 真实push完整CI

[run40086327059](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40086327059)，attempt1，2026-10-24北京时间18:32:33—18:48:02，head/tested=2063274，status=success。20项suite/internal校验通过：guarded15、audit14、verify32、ci-tests50、图片工具17、音频工具18、交换契约3、前端单测189、assets4、audio27、文件重复30、默认浏览器136、Java617、gate15、desktop-exchange1；其余为非测试步骤，只记录退出状态。这些来自本次云端日志/XML，不是本机重跑。

主artifact verify-40086327059-1，ID12557776640，ZIP1825643字节，SHA256 d92a983b62078476c4fba6a5f70a62e08b477c629cdc155091764784c55022c3；receipt artifact ID12558160040，ZIP464字节，SHA256 e2c753b945c58bc528a0d183c5623dfbd140037ca71839f4582ab0a19311b78b。均与GitHub API digest逐位核对；包内assembly_revision=2，逐字段复算通过、not_covered无重复，人工六门仍not_covered；回执ID/digest/tested_head/run/attempt/URL一致。首次本地回执探针误读不存在的嵌套artifact键而KeyError，按文档实际扁平字段修正后核对通过；这是探针错误，不是CI失败。

### 真实pull_request拒绝证据

[run40149558683](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40149558683)，attempt1，2026-10-25北京时间09:10:05—09:12:24，event=pull_request，status=failure。证据context明确base_source=pull_request.base.sha，base=6163620完整SHA、head/tested=2063274完整SHA，与PR API及执行命令相同；不是HEAD^或合成merge。执行命令git diff --check BASE HEAD --，exit2，0.788s。

原始diff-check.log核对为3个Java文件共24处trailing whitespace（ExecutionManager.java、GameContext.java、ReplayPlayerHandler.java），log SHA256 6c99390645dcbb5e754f4c291c4a0f3ff29e5dfa013fb78bfd0a6ad894a59831。这与此前历史空白预期一致；没有修改源文件或降低检查。仅diff-check执行且失败，后续19项全部unexecuted，不能把push完整成功写成PR通过。first_failure.case/first_error无法按现有解析器识别，保持null，具体原因来自原日志。

主artifact verify-40149558683-1，ID12581546968，ZIP466320字节，SHA256 ae0717d55a99ee2ae514aefed93708f2079eda7bbce5fedbace33a654ea32faf；receipt artifact ID12582436498，ZIP464字节，SHA256 d1d072bdc0721b0cd9a171a63b3b436f164cabc51ecc6325b5d7777057a54cf5。API digest、包内逐字段重算、独立回执关联均通过；合法失败仍返回failed，不吞错、不制造绿灯。

两次ZIP均为公开nightly.link入站数据，随后按一方GitHub API metadata/digest严格验证，不自动提取或执行归档；不得称一方认证ZIP下载成功。原包、元数据、重算JSON与失败日志保存在忽略目录build/wf005-review。本机没有新JVM/浏览器游戏套件/全量Java，未复用历史本机全量授权。

WF-005交付完成：真实push成功及真实PR拒绝的身份/失败传播/证据归档路径均经验证；**这不是PR已绿或main可合并**。下一步是用户决定主线整合路径：建议在隔离分支仅处理这24处历史尾随空白，经独立CR与云端完整门后再决定合流；本轮未执行此修复或合流。真实fork、一方认证ZIP下载、同SHA三次full、人工/容量/发布仍开放；WF-006/007仍PLANNED，WEB-036仍暂停。

本次仅文档归档：核对事实/链接/相关差异和原137项porcelain、136文件hash保留。归档提交未获额外推送授权，保留本地，不把文档HEAD称为已重跑2063274的完整门。
