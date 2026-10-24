# f2e748e 复评与 WF-004 处置

WF-003获接受，无阻断。reviewer独立复现计划器18组路径、缺git诊断和交换冒烟1/1，并重算指纹及远端账本集合；未执行入口26项/审计14项或完整执行链，不把方法计数当测试通过。Java/冻结/输入及LFS仍未变。

## 前置补修

- P2-1：收窄gate触发到明确的共享会话/人物/舞台路径，另保留gate用例/配置触发；帮助、音频、Replay、独立style/labels继续默认前端回归，不固定追加gate。未仅因gate直接import characterDisplay就忽略它通过浏览器验证的其他共享流程。App.vue内窄文案修改仍保守触发，路径规则无法证明语义范围，已明示成本取舍。
- P2-2：账本补精确定义：OID按ASCII升序，`oid + TAB + 十进制bytes + LF`拼接，最后也有LF，以ASCII编码取SHA-256。原列表/数值不变，重新按定义核对。
- P3-3：交换spec/config改动直接分级full，包含当次浏览器文件、java-full、XML/桌面产物与desktop-exchange；没有只加一个缺前置产物的孤立suite。
- P3-4：不存在或非法base均明确INVALID_BASE_REF。
- P3-5：真实Git差异为空即NO_CHANGES_TO_VERIFY，--paths不能伪装实际差异。显式--full可同SHA重新完整验收，仍需独立授权，为以后稳定性观察保留入口。
- 零skip契约：新增跳过须同步改契约并评审，不静默放行。

实测 `python3 -B tools/tests/test_verify.py -v`：30/30（1.777s），新增四项覆盖取舍/交换前置/无效base/空差异；原26项保持。未运行Java、浏览器或打包，原入口执行命令/监护未变，不因只增拒绝与分级变化强加全量。具体日志原仓库build/wf004/review-fixes-tests.log。账本31/20对象聚合hash和字节重新校对通过；本轮没有远端对象写入。

## WF-004约束

单job保留browser-export/desktop-actual当次mtime契约，不跨job传递；原产物格式不加CI字段。PR使用真实base/head SHA，push使用before（新分支明确以default branch作基线）；先dry-run再执行。Action pin完整SHA、contents:read、取消同分支旧运行、45分钟预算、LFS缓存后仍校验实际内容、always上传现有result/log。候选/最终文件一致性脚本化，CI与授权/启用状态另见本轮工作流记录，不能把本地契约测试称为云端成功。

## WF-004本地配置实测（远端待授权）

- CI辅助契约 `python3 -B tools/tests/test_ci_workflow.py -v`：24/24（0.023s）。覆盖PR真实base/head、push.before、新分支default基线、非法SHA/错误检出/删除事件、缺base不回退、显式full、权限/单job/时序/缓存/Action pin、下载缓存哈希/坏下载清理/ZIP越界/LFS二进制、plan/result/HEAD不符，以及CI wrapper实际调用一次并传播失败、不把无效输出标绿。全部临时数据/受控runner，无网络/JVM。
- 最终入口30/30（1.816s）、原审计14/14（0.773s）；新增ci-tests被full策略强制包含，其独立用例改动在tooling层追加执行。未改原监护或启动器。
- actionlint1.7.12（官方归档SHA256 8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8）静态校验通过，命令 `actionlint -shellcheck= .github/workflows/verify.yml`；未把这项称为云端运行。Python AST/JSON与相关差异检查通过。
- 首次YAML解析发现plain scalar中的`--only-binary=:all:`与YAML冒号语法冲突，改用block scalar后24项通过；该失败在collect前，不记作已执行测试数，也不归因环境偶发。
- 复用本聊天干净LF隔离树，未改用户目录；基线b38da1e，仅加入本轮9个代码/配置文件形成候选 3cc658c4753f3308ba126774727660c3c0a80561。真实入口`--base b38da1e --dry-run --json`返回planned/full；未批准的实际调用返回FULL_JAVA_PACKAGE_APPROVAL_REQUIRED，未创建新运行目录/启动套件。同候选CI契约再24/24。当前9文件与候选逐字节相同，聚合SHA256 `cce745816294a3bf9cfd60678be21342342dddca5f6629ed97fe484001a737c7`（算法按UTF8路径升序，逐项UTF8(path)+NUL+ASCII(content SHA256)+LF）。本轮完整Java/FatJAR/default/gate未运行，不能用旧WF-002结果冒充。
- CI工具选择精确版本并固定哈希；官方Microsoft安装manifest没有宿主21.0.12.1，明确选择21.0.11，新的完整结果仍待临时runner验证；未安装本机系统包/改配置。GitHub临时runner上的依赖安装和CI全量范围包含在待批准方案中。
- 同步区间检查：旧远端main6163620到本地包含历史尾随空白，严格检查会拒绝，未改旧Java或放宽diff-check。提出先推已审f2e748e准备基线、再推CI提交的两步方案；未来PR仍使用真实main base并报告旧问题。完整范围/触发/授权见workflow-ci.md。
- 远端只读：准备分支不存在，main仍6163620；历史额外LFS对象5864014字节从空目录实际下载并核对，32个历史唯一身份与当前31分开记录。新增对象上传0，Git refs推送0。

源码日志：build/wf004；候选dry-run与缺授权负例：隔离树build/wf004。Java/frozen/input指纹与已审基线一致，政策因新增CI工具改变；不声称候选整提交等于最终含文档提交。完成状态保持WF-004 DOING，只有具体推送/CI授权与实际云端结果具备后才转DONE。

## 获准推送与首次云端失败（2026-10-24）

用户明确回复“授权两步推送并启用该CI”；f2e748e建准备分支、f6a1679快进均exit 0，main未变。授权包括该配置所列事件下GitHub临时runner的完整验收/打包与依赖安装，不是本机全量授权。

首次[run 40076072948](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40076072948)，真实base=f2e748e、head=f6a1679、full。工具链、LFS fetch/checkout/fsck、npm ci、正式dry-run、Chromium/Xvfb/字体安装及testClasses预热均成功；实际执行diff-check通过，guarded-tests为14/15，随后按fail-closed停止，其他套件与完整Java/FatJAR/gate尚未执行。不能把job环境准备成功称为全量通过。

原因为拒绝路径夹具继承了job的VOCR_APPROVED_FULL_CHECK=1，却断言无授权时exit 64，实际0。本机使用/bin/true夹具、同授权环境反向复现1/1失败；修正测试env显式移除继承授权，同时新增同夹具显式批准后exit 0及外部哨兵存活断言。无生产安全入口/监护逻辑、内存门槛或游戏改动。修后带继承授权的完整隔离监护回归15/15（6.569s）、CI契约24/24（0.025s）、actionlint通过。本机未启动JVM或全量验收。

公开REST日志下载403（需更高认证），未读取/提取用户浏览器凭据。通过nightly.link下载公开artifact，只作为入站数据；ZIP SHA256与GitHub artifact API公布的2b1c27b341e98e350dee5758f468743c8d618167cc310615e4072c16d3aca916一致。artifact verify-40076072948-1（ID12552864540，14天）；原包/元数据保存build/wf004。已读取context/tools/result/失败日志，未运行其中代码。新runner输入审计确认干净LF树、225 Java/9冻结/1979输入指纹与候选相同、32指针/31唯一LFS payload全部长度与SHA有效；不把审计里的remote_verified=false改写为该工具完成了远端逐对象证明。GitHub步骤结果仅证明该次fetch/fsck命令成功。

补修6346c7d在用户明确回复“授权本补修及本轮必要CI补修推送”后快进推送成功；授权仅准备分支、本轮必要CI配置/测试夹具补修，不包括游戏/main/设置/secret/PR或本机全量。[第二次CI 40076634386](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40076634386)使用真实push.before=f6a1679、head=6346c7d，结果success，具体证据如下；此前首次失败记录保持。

## WF-004首次完整云端通过（6346c7d）

[run 40076634386](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40076634386) / attempt 1，2026-10-24 16:04:50—16:19:35（北京时间），真实base=f6a1679592be40d125b17ad33ca92e2c3f2e3f76、tested/head=6346c7d5f55b1a36fde12bd1d662314209f25f22。统一入口full，统一验证入口的20项suite/internal校验全部passed（不是GitHub job执行步骤数）；oracle-generate由本次java-full生成覆盖，并非跳过Java测试。没有靠重试、放宽断言或替换黄金通过。

| 当次云端实际执行 | 结果 |
|---|---|
| 隔离监护 / 输入审计 / 统一入口 / CI契约 | 15/15、14/14、30/30、24/24 |
| 图片工具 / 音频工具 / 交换契约 | 17/17、18/18、3/3 |
| 前端单测 | 189/189 |
| 图片+音频prebuild/postbuild、Vite生产构建 | 通过；代码580082字节/gzip157269，全站77753114字节 |
| 静态资产 / 音频浏览器 | 4/4、27/27 |
| Replay文件重复 / 默认浏览器 | 30/30（repeat-each=3）、136/136 |
| 完整Java test/check/概率/运行时资源/FatJAR | 通过；61份XML独立重算617 tests，failure/error/skip均0 |
| 当次oracle审计 / 七配役gate | 通过 / 15/15 |
| 浏览器→桌面→浏览器真实文件交换 | browser-file、Java读取、desktop-exchange 1/1通过；两文件在同job产生 |

桌面FatJAR为724197819字节，SHA256 d7232b8cc37d0c761924936b496c3b055866021aef372ae18681d31bd94d18a0；CI检查Game.class存在且没有Web/Javalin/Jetty类。大JAR未上传，本地没有下载或重新构建该JAR，因此字节/hash/类边界来自当次CI wrapper报告。构建中的字体mtime恢复警告未导致失败，不据此声明正式字体/DPI验收。

证据artifact verify-40076634386-1（ID12553728300，14天），ZIP SHA256 2acb5f0778bc31e6220aa8c1d0c63cb018d192bda501adb213dda91d1c976091，与GitHub artifact API digest一致；通过公开nightly.link入站读取，原包/元数据/解析摘要在build/wf004。逐份核对suite日志hash、result hash（6fa8f2ceb9c556a56770f89c79768f112ed9868dd61b94938b1848eb9aece93d）、context/base/head，61份XML重新统计。没有把nightly.link当新的正确性来源，也没有运行下载包中的代码。

工具报告Python3.12.12、Node22.23.0、Microsoft21.0.11+10-LTS、固定Gradle/LFS/FFmpeg归档与二进制SHA；runner image 20261012.322.1（与首次失败运行的20261023.322.3不同，不能把ubuntu标签视为不可变镜像）。干净LF权威检出、225 Java与9冻结/1979输入指纹仍与已审基线相同；LFS fetch/checkout/fsck步骤成功，输入审计32路径/31唯一payload全部长度和SHA匹配。没有再做本机重复冷拉31对象，也不把缓存/网络下载量猜作账户计费。

WF-004完成：已真实推送触发full、读取成功和失败证据、验证单job双向交换与完整桌面/Web/资源链。后续仅含归档文档的提交按docs层验证，完整证据仍绑定6346c7d，不冒称后续文档SHA重新跑完整链。本机本轮未启动JVM/浏览器或执行完整验收。原137项工作区差异及136份文件内容保留，main/设置/secret/PR未修改。WF-005转READY，尚未实施；WEB-036仍暂停。

仍开放：真实PR事件未创建/运行（只做事件夹具与工作流契约验证）；未来面向旧main的PR仍会严格报告历史空白，需独立决定主线整合。A06/A07/A08、046/049人工、047容量不因CI全绿关闭。自动化AI评审/修复/发帖/合并没有启用。
