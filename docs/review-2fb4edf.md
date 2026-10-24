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

本轮本地补修完成后尚未推送；没有新云端结果。真实PR创建、预期历史空白拒绝取证及main合流仍待明确授权，WF-005不标DONE。
