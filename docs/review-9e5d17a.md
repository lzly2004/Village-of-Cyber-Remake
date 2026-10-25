# 9e5d17a复评处置与WF-005

WF-004通过、无阻断。Reviewer独立核对云端API、artifact digest、617/189/136/15等计数及输入指纹；本轮不把其读证据称为本机重跑。

## 前置处置

- P2-1采纳：全量授权变量从job移到Execute once step，准备/上传不继承；该step的测试子进程仍继承，6346c7d的负例显式清理保留。没有改授权门槛或监护成功命令。
- P3-2采纳：账本分支值增加as_of，明确为6346c7d推送后、9e5d17a归档前快照，不伪造会记录自身SHA的文档。实时HEAD由GitHub核对。
- P3-3采纳但保留证据边界：WF-002本地与6346c7d云端JAR同为724197819字节，hash分别2e9a5959…与d7232b8c…。可能来自JDK21.0.12.1/21.0.11、编译class或ZIP压缩等工具链差异；未逐条比较两份JAR，**不是已确认成因**。同尺寸不能单独排除换行或证明等价；源LF与Java/frozen/input指纹相同是独立证据，不宣称产物逐字节可复现，也不据hash差异推断替换。已核对build.gradle统一设置preserveFileTimestamps=false与reproducibleFileOrder=true，因此不能未经比较就把原因归为条目mtime；本轮不扩展为产物可复现性重构。
- P3-4澄清：20指统一verify入口suite/internal校验，非GitHub job step数量。API的执行/post数量是另一口径，原记录加注明确。
- P3-5采纳：WF-005实现一方API认证下载/完整性复核，凭据来自用户既有环境，缺认证明确拒绝；不自动用nightly.link、不提取浏览器凭据、不自行创建token。

真实PR、同SHA三次full稳定性观察和frontend成本样本仍是建议/待验证项。WF-007前须具备三次稳定性证据，本轮不提前开展Auto-Fix、不把一次绿灯称无flake证明。向旧main的PR历史空白仍未修正；真实PR创建、额外分支发布和评论须另获具体授权。

前置实测：CI契约24/24（0.014s），actionlint通过，相关diff-check通过。未启动JVM/浏览器；只是缩窄变量设置作用域，执行命令不变。

## WF-005本地交付与实测

实现独立evidence.json/summary.md、每suite进度、上传后的独立receipt，以及只读一方API/本地ZIP/离线复核CLI。原result/游戏/Replay格式与进程监护不变，没有增加生产依赖或CI写权限；具体字段/失败/权限契约见[证据交接](workflow-evidence.md)。

最终适用验证（不是Java全量）：

- `python3 -B tools/tests/test_ci_workflow.py -v`：47/47（0.065s）。含计数来源/未知值、Java XML正文与计数一致、重复JSON键、缺环境证据、错SHA/plan/result/stdout/log、缺suite/顺序、准备失败、缓存不读取、诊断转义、回执自引用隔离、ZIP digest/大小/过期/attempt/路径/重复/符号链接/意外目录、重定向不带认证、403不回退、离线标记、实时元数据+本地ZIP、CI Python子进程真实stderr tee/退出码冒烟。旧24项保持，新增workflow与证据契约纳入同一ci-tests入口；没有并行JVM/浏览器。
- `python3 -B tools/tests/test_verify.py -v`：32/32（1.803s），新增进度仅stderr、running先落盘、失败不启动后续suite两项。`test_ci_inputs.py -v`：14/14（0.755s）。
- actionlint1.7.12、Python AST、JSON、相关文档链接与定向diff-check通过。没有对整个脏树跑格式化或把其CRLF差异当本轮改动。
- 原成功run40076634386与原失败run40076072948的保存ZIP，按原GitHub API digest重新核验。成功包重算617/189/136/15等，失败包明确guarded-tests用例与AssertionError:64!=0、后续未执行。这是**本轮执行解析器复算旧证据**，不记为本轮重跑617或浏览器全量；最终解析摘要build/wf005/final-historical-recompute.json。
- 一方自动下载实际冒烟：当前无GH_TOKEN/GITHUB_TOKEN，GitHub归档下载明确AUTHENTICATION_OR_ACTIONS_READ_REQUIRED（退出2），无第三方fallback。另以本地已保存的9e5d17a ZIP，实时从GitHub读run/artifact digest/当前源分支HEAD：首次GITHUB_NETWORK_FAILED，保留证据；只读重试一次通过（退出0）。此路径证明本轮一方元数据复核+本地ZIP，**不是本轮从GitHub认证下载了ZIP**。历史ZIP原获取来源仍按WF-004归档，没有改写。

47项内部阶段曾出现49项统计（DownloadTest继承测试类导致重复发现同一批用例）；发现后改为共享无测试方法的Fixture，最终按实际独立方法发现47项。不把中间重复计数作为新增覆盖。

本地验收阶段未启动本机JVM/浏览器、未跑全量Java/打包/默认e2e/gate；截至该阶段尚未推送新配置、创建PR或发评论。前轮同意的“本轮必要补修推送”属于WF-004，不擅自扩大为WF-005发布/PR创建；本轮代码完成后列具体SHA范围申请。WF-005仍DOING，等待真实云端与PR证据；WF-006/007未实施。原137 porcelain差异与136份文件内容保留。

剩余实测门槛：新配置的真实云端成功/失败证据、上传后回执与一方读取；经用户批准后的真实PR事件（需核对pull_request.base.sha和真实head，旧main历史空白仍应严格拒绝）。同SHA三次full属WF-007前置；frontend层成本待真实相关改动触发，不伪造生产改动凑样本。以上均未记为通过。

## 逐项核验与归属表

| 报告项 | 核验方式/证据 | 结论与归属 |
|---|---|---|
| P2-1授权step化 | YAML字段+CI契约只允许Execute once step携带变量；干净子环境负例仍在 | 采纳，前置提交54a39ae；不采纳“子夹具因此不再继承”的推断 |
| P3-2账本SHA快照 | JSON的as_of边界、当前远端元数据只读复核 | 采纳，前置提交54a39ae；快照不宣称实时HEAD |
| P3-3同尺寸不同hash | 核对两份原记录、JDK版本及build.gradle的去时间戳/排序配置 | 采纳成因边界说明，未比较JAR内部，原因仍待证实；不开展计划外可复现构建重构 |
| P3-4计数口径 | 原result的suite/internal列表与GitHub步骤为不同计数对象 | 采纳澄清；不把20个入口校验改写成18个入口校验 |
| P3-5一方读取 | downloader正负例、一方API实际403、实时元数据+本地ZIP重试成功 | 采纳，WF-005；认证自动下载本轮未成功，不能以离线模式冒充 |
| O1浏览器/字体安装成本 | 原记录约1–2分钟与非固定runner image；本轮没有新增时间样本 | 暂不改缓存或APT版本，WF-005后续真实成本样本再评估；不是本轮性能优化授权 |
| O2真实PR/fork边界 | 现有context夹具验证真实base/head；无真实PR事件 | 待实证，WF-005；需具体PR创建授权，保留旧main空白拒绝 |
| G1证据承载字段 | evidence/receipt契约与47项测试，原ZIP重算 | 本地采纳，WF-005；新云端回执仍待验证 |
| G2 PR摘要流程 | 本地handoff与PR正文草案，未发评论 | 本地采纳，WF-005；发布/创建待授权，不称真PR通过 |
| G3首条失败摘要 | 原失败包重算准确用例与64!=0；失败/未知/未执行负例 | 本地采纳，WF-005；新CI job summary实际显示待验证 |
| G4同SHA三次full | 本轮0次，前轮1次不得冒充三次 | 按原计划保留为WF-007硬前置；实施/费用/可用dispatch另确认，不在此提前Auto-Fix |
| G5 frontend成本样本 | 本轮未触发真实frontend层，旧full的子套件耗时不是该层实测 | 待采样，WF-005后续首个真实frontend变更；不伪造生产改动 |

## WF-005云端发布授权

2026-10-24用户明确回复“授权推送及本轮必要CI补修”。已从9e5d17a快进推送54a39ae与00dd98f至原准备分支，未改main/设置/secret或强推；新run40080861974已完整成功，证据复核如下。授权不含本机全量；独立PR创建问题尚未回答，未创建PR。前述“未推送”是本地验收阶段记录，不作为当前远端状态。

## WF-005真实push CI与证据闭环（00dd98f）

[run40080861974](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40080861974)，attempt1，2026-10-24 17:09:51—17:25:18（北京时间）。真实push.before=9e5d17aba848fd6320af3ab832b94f4db3b54129、head/tested=00dd98fe7cf6d2cad6e42cae443eabf27c4ac605。统一入口full的20项suite/internal校验全部通过；这些是当次云端结果，不复用WF-004旧数。

| 当次执行 | 结果 |
|---|---|
| 隔离监护 / 输入审计 / 入口 / CI证据契约 | 15/15、14/14、32/32、47/47 |
| 图片工具 / 音频工具 / 交换契约 | 17/17、18/18、3/3 |
| 前端单测 / 静态资产 / 音频浏览器 | 189/189、4/4、27/27 |
| Replay文件重复 / 默认浏览器 | 30/30、136/136 |
| 完整Java test/check/概率/运行时资源/FatJAR | 617/617，61份XML独立重算，失败/错误/跳过均0 |
| 当次oracle审计 / 七配役gate / 反向浏览器交换 | 通过、15/15、1/1 |
| 图片+音频构建门禁 / Vite | 通过；代码580082字节/gzip157269，全站77753114字节 |

新工具实际读取一方run/artifact/当前源分支HEAD元数据，用本地ZIP重算存于CI的evidence.json：**逐字段一致**，status=passed。全部suite日志、result/stdout/plan/context绑定与XML计数核对通过；case/错误/skip来源明确，非测试步骤counts为null，六项人工门槛仍not_covered。verify.stderr实收41条进度JSON（20 start、20 finish、1 final passed），证明真实CI中开始/结束输出已落盘，不只是mock。

主要artifact为verify-40080861974-1，ID12554724256，ZIP1828278字节，SHA256 `5b2df9ca3538d8d9d384d19183cb72fcc0f56ecbe8fbd8751a6cd7b95775d17e`。单独回执receipt-40080861974-1，ID12555885524，ZIP464字节，SHA256 `71817fd20152dcf4f92b6bca4dce4ab1bad679ce26c8d954c2320b173dd73352`。两者均与GitHub API digest一致；receipt内主artifact ID/URL/digest、run/attempt、tested head逐项匹配，未嵌回主ZIP。result SHA256 `75ecb76c246641a04b87f22547892b42304e2ff9289400dce161cbd67ba3f486`。

原包取得仍为公开nightly.link入站数据；新的download.py没有自动fallback。随后由新CLI实时读GitHub API元数据/远端HEAD，以--zip原包重算，退出0。**本次未获得API认证下载权限，不把元数据+本地ZIP模式写成一方认证ZIP下载成功**。原包、API元数据、receipt及重算evidence/handoff保存在build/wf005，14天保留期仍以GitHub元数据为准；未执行ZIP代码、未提取凭据。

同job两份交换文件另手工读取并比对result中的长度/SHA：browser-export.json为323ef3dd104bdc78c76c442a4c89905a3955927a79875e26f55d2e67bfa15c47；desktop-actual.json为8a9ef0551a0a59186da254b8e9452134dcbdad0c2ca6173d78d1276586a07695。FatJAR报告724197819字节、SHA256 d7232b8cc37d0c761924936b496c3b055866021aef372ae18681d31bd94d18a0，与6346c7d云端记录相同；JAR未包含在证据包、未下载重算，此值来源为本次CI wrapper。两次CI记录一致不证明本地2e9a…差异的成因，不扩张为跨工具链字节可复现声明。

环境报告runner image20261012.322.1，Python3.12.12、Node22.23.0、Microsoft21.0.11、固定Gradle/LFS/FFmpeg。225 Java、9冻结/1979输入指纹保持；build_policy为44文件，SHA256 688b74cc1028a78cd100cf818cc2e9c26f88817e898a004a89e12fa3954ccf24。

本轮源码无需CI补修，第一次真实push运行成功。后续纯归档提交只按docs层验证，完整结果仍绑定00dd98f。原137项工作区差异/136文件hash保留，本机仍未执行JVM/浏览器全量，main/设置/secret未改。**WF-005保持DOING：真实PR创建问题尚未获答复，未创建PR，也未声称pull_request/fork事件通过。** WF-006/007未进入；人工、容量、发布门槛仍开放。
