# 开发—CI—独立评审工作流计划

2026-10-24用户授权列完整计划并执行第一步。本计划是独立工作流准备队列，不插入或重排WEB-001～049；WEB-036及以后保留READY但暂停功能开发。WF-001～005已完成；WF-006/007仍为PLANNED；不因表中计划而获得推送、付费、自动化发帖/修复、全量验收或系统配置授权。

## 完整计划表

| 编号/优先级 | 状态 | 交付 | 完成标准与依赖 |
|---|---|---|---|
| WF-001 / P0 | DONE | 基线准入：F-1前置补修、只读提交/工作区/LFS/oracle审计、隔离ext4检出、EOL及远端同步范围决策 | 工具正负例与真实审计通过；明确缺LFS/旧oracle会拒绝。只完成准入调查，不称全门可复现。证据见下文 |
| WF-002 / P0 | DONE | 干净检出工具链与全门校准；补Git LFS、固定JDK/Node/Gradle/Playwright/FFmpeg/Pillow、显示与字体 | 在隔离目录验证实际LFS，不依赖原工作区/tmp；用户另授全量Java/打包后，完成浏览器→桌面实际文件传递、完整桌面/资源/浏览器/gate验证，记录新SHA与指纹。失败先分类，不调低内存/放宽断言 |
| WF-003 / P0 | DONE | 单一验证入口tools/verify.sh与路径分级、dry-run、状态/输入检查 | CI和本地同一入口；保守分级，未知路径不静默降级；工具测试验证命令选择和失败传播。黄金/冻结样本变更报告但不自动改写；EOL/ignore如需落地须独立局部补丁，不全仓renormalize |
| WF-004 / P1 | DONE | 确定性GitHub Actions最小闭环、LFS/依赖缓存、分支与提交同步 | WF-002/003完成；先给具体分支/提交范围再推送。工作流固定工具链、单worker/串行套件、现有Playwright配置、oracle先生成再核对，缓存缺失不跳门；缺套件/证据显式失败；本轮不启用任何远端设置 |
| WF-005 / P1 | DONE | 结构化证据包、纯脚本失败摘要、dsh独立CR交接与PR流程 | 记录base/head/tested SHA、run id、环境/工具版本、suite命令/数量/失败/跳过/时长/日志及三类指纹、not_covered；补充按suite显示开始/完成进度，避免单个长步骤只能看到in_progress；处理公开日志API的403与artifact读取来源/哈希，不能依赖未授予的凭据。最新SHA与已测试SHA一致。明确本地定向与CI完整门分工、CI全量预授权范围，才能减少重复全量；dsh仍可要求定向/全套复跑和反向探针。PR通路红→绿已验证（3b8641c）；草稿未合并，合流归用户主线决策 |
| WF-006 / P2 | PLANNED | Chat可选规划试点 | 复杂跨端/协议/格式或多模块/高返工任务试用；事实包SHA+路径/符号/摘录+契约+未决项，输出方案、验收、待验证假设；Codex核对并上报计划错误。记录总调用、返工、搬运时间后决定固定触发条件 |
| WF-007 / P2 | PLANNED | Auto-Fix可选试点 | CI稳定观察（至少同SHA三次完整运行，非“无flake证明”）、失败注入和最新SHA核对完成后另决策。先人工触发/一次一个草案；脚本检查路径/规模，禁止测试/黄金/规则配置/凭据/发布门槛越权修改；补丁生成与PR发布隔离权限，修复PR必须再触发CI与独立CR，禁止自动合并和循环自修 |

推荐目标：复杂任务可选Chat方案→用户确认目标→Codex事实核对/实施/定向→任务分支/PR→确定性CI→dsh独立语义/证据审查→Codex补修→CI再验→用户合并。CI失败也允许dsh先做范围/语义快检，不强迫先烧AI修复。AI输出与第三方报告是待核实建议，不是新的授权。

## WF-001实际交付

工作分支`workflow/baseline-input-audit`。F-1独立前置提交`5e615dd`，从`1332d3b`开始，未更改main引用或远端。修复及外部报告逐项处置见[复评归档](review-1332d3b.md)。

`python3 -B tools/audit-ci-inputs.py --ref HEAD --oracle-dir build/web017`只读输出JSON，不规范化、不暂存、不fetch、不写报告文件；输出重定向由调用者负责。Git读取关闭optional locks，防止status刷新索引。默认仅盘点，不等于CI通过：

- `--require-clean`要求Git状态干净且每个跟踪文件的工作字节等于所选ref（已验证LFS payload按指针比较）；CRLF只分类，仍不接受为完全相同。
- `--require-lfs`要求工作目录每个LFS文件长度和SHA-256匹配；本地对象另外校验，不把它当作远端对象已验证。
- `--require-oracle`要求七配役×两策略+真实中断共15个精确文件名，每个指纹匹配所选ref的Java原始blob；否则明确要求按最终源码重新生成，禁止削弱指纹/刷新黄金。
- 返回0表示所选检查没有拒绝，1表示明确准入拒绝，2表示输入/工具错误。没有选择任何require时是库存报告，不是测试放行。

三类记录：生成型oracle单独校验；冻结型src/test/resources及两份图片/音频manifest只读聚合；输入型src/main/resources与概率配置聚合，LFS还记录解析后的payload SHA/长度。构建/验证脚本、Gradle、package/lock、Playwright配置另聚合。Java指纹与既有生成器完全同算法：按main/test/web根顺序、根内路径排序，逐项`UTF8(path)+NUL+原字节`。其他组采用排序后的`UTF8(path)+NUL+ASCII(内容SHA256)+LF`；LFS在组内是提交的pointer字节hash，payload身份另外列出，不能混称真实音频内容hash。完整工具/政策改动仍以Git提交SHA追溯。

本轮工具不执行“样本变更已获评审”的自动授权，不把同源证明当规则正确性；后续verify/CI还须执行冻结测试、显式列出基线差异和输入变动。schema 1仅为开发工具库存输出，不是游戏、Replay或存储格式。

### 隔离检出与EOL决定

原工作区的137条状态及内容全部保留，不按reviewer要求清掉CRLF/缩进/私人文件。新目录在ext4，`git clone --local --no-hardlinks --no-checkout --branch workflow/baseline-input-audit <源仓库> <新目录>`，从已提交对象复制（不含用户data或未跟踪笔记），不共享hardlink；随后detached checkout `5e615dd`。此为本地提交检出，不是GitHub远端完整克隆证明。

隔离仓库局部设置`core.autocrlf=false`和`core.eol=lf`，不改全局；checkout此次关闭LFS smudge/required/process，仅保留真实提交的pointer，明确标“未水合”。没有伪造clean filter、假装指针是音频，未复制未校验payload。

目录：`/home/noeticuser/Documents/Codex/2026-10-22/village-of-cyber-remake-web-019/wf001-clean`。本轮只准备与审计，未在此目录跑完整Java/浏览器。缺少git-lfs命令及32个payload，WF-002准备固定工具后再真实水合并校验。禁止把原工作区的42d… oracle复制过来冒充此检出对照。

不在原脏.gitattributes/.gitignore上追加或整体暂存；下一阶段如需LF属性/ignore，按HEAD构造最小补丁，保留用户内容；不移除已跟踪IDE文件，不笼统忽略.trae规则目录。Windows/NTFS、字体/DPI/物理输出等仍列未覆盖，不用ext4自动化替代。

### 本轮实测与准入结论

日志在源仓库忽略目录`build/wf001`；未新增系统/全局Git配置、依赖、JVM启动/监护变更或真实身份字段。

- `python3 -B tools/tests/test_ci_inputs.py -v`：12/12。隔离临时Git覆盖原Java算法、CRLF/实质字节差异/缺失/符号链接、未跟踪、LFS pointer/正确或同长度损坏payload/本地对象、独立冻结输入hash、旧/缺失/坏/错名oracle、index与ref分离、非法pointer、CLI退出和不写原件/索引。
- 原提交基线`1332d3b`及F-1后的`5e615dd`：225 Java。提交指纹`9ffa8d2d8714a555f856ee19f4b86ace466df6c1aade366df0b5701211b967b7`；原工作区`42d4233b4ac3b66954fc5f0003a15d0bd55b082778c5198700301642ad4944c1`。原有134项仅CRLF，Game.java为其他字节差异（缩进事实经对照，不据此声明语义证明）；工作区不是CI输入。
- 该基线冻结组9文件SHA-256为`6ce8aa549a2f49923b763b04d819d3ed29870c9bbd0c30745e97c08c3beca8b8`，输入组1979文件为`3d4c43fae21fbb7db0a68cdaa44db66aeaa8478760853479f6a539abfcabbe4a`；构建政策组另报，后续新增工具会改变该组，不将基线值硬编码为永久许可。
- 32 LFS（23 BGM/9音效），620138120字节；原工作区payload和本地对象均32/32匹配。远端可用性、账户余量未验证；未下载远端LFS、不启用付费。
- 干净ext4检出：`--require-clean`退出0，状态干净/跟踪字节等于ref，Java指纹9ffa…；LFS水合0/32、旧oracle15份均stale_for_ref。加`--require-lfs --require-oracle`后退出1，明确同时报LFS_PAYLOAD_NOT_READY与ORACLE_STALE_OR_MISSING，非产品测试失败。
- F-1反向失败和最终30/30见复评记录。617 Java、旧135浏览器、15 gate等是前轮证据，不计本轮重跑；新增用例后默认预期136，应由最终检出实际发现数核对。

WF-001结论：完成“能审计并拒绝不完整输入”的准入准备，不是“干净检出全门已通过”。完整校准是WF-002，必须先补工具/LFS，并按AGENTS另获本轮全量Java/FatJAR授权。

### 远端同步与资源预算

2026-10-24公开API只读核对远端main仍为616362073999cb280ef8891a8f69272541904357，Actions工作流列表为空。该提交是1332d3b祖先，至该基线153个提交；F-1/后续工作流提交另计。未fetch到原仓库、未push、未改远端规则或secret。

实际推送前应列分支与commit range、审阅新增文件/冻结或敏感路径、核验LFS远端对象，先推独立准备分支，不直接覆盖main；不要git add -A，不带用户工作区改动。当前git-lfs缺失的post-checkout/post-commit警告不靠删除hook解决。

当前[官方LFS计费](https://docs.github.com/en/billing/concepts/product-billing/git-lfs)Free含10GiB带宽/10GiB存储，不采用报告1GB旧额度；620138120字节约0.578GiB/完整冷拉，仅是32个当前payload估算，不含历史对象/其他下载，也不代表账户余额。缓存按对象OID、缺失只拉所需集合，首次必须验证实际内容；缓存不是正确性来源，不能为了省流量放过指针或跳掉资源门。

## WF-002的具体校准顺序（已执行）

1. 在隔离检出准备可追溯工具链和真实LFS；依赖预热可在线、验收离线，不能假设Gradle首次就能offline；不使用reviewer临时shim或覆盖Playwright配置。
2. 开始/结束记录require-clean与输入清单；前端单测、生产构建、默认浏览器（含F-1固定repeat-each=3）实际下载browser-export.json，静态资源及音频套件按最终范围执行。
3. 明确VOCR_WEB035_BROWSER_EXPORT指向该次下载并检查存在，用户批准后经原安全入口串行fatJar --offline（含全量test/check/特征/概率/资源）；Swing在真实显示或Xvfb+字体，不能headless跳过。缺跨端文件直接失败，不走默认fixture冒充。
4. 全量生成该次源码15份oracle，核对指纹后调用仓库test:gate（原配置自带release-baseline）。不刷新黄金，不手工另起漏参数服务器，不并行e2e/gate/JVM。
5. 确认冻结/输入文件没有被运行自动改写，保存XML、日志、trace和简明摘要；未跑/环境失败/跳过明确区别，原先成功数字不作本次证据。全量批准缺失时停在允许的定向范围，不擅跑或声称WF-002完成。

本轮 WF-002 评审处置见 [82615c0 复评](review-82615c0.md)。权威验证树必须为干净 LF 检出，Java ref/worktree 指纹相等；原脏工作区不作为 CI 等价证据。用户已分别授权本轮全量 Java/打包和仅补传20个缺失LFS对象，均不构成 Git 分支推送授权。

WF-002已完成：干净LF检出与真实远端LFS证明、工具链、完整桌面/Web/资源/gate校准通过，实际命令/新SHA/产物和未覆盖门槛集中见[校准记录](workflow-clean-calibration.md)。WF-003已接续完成（见下文）；单一入口保留权威树与ref/worktree相等约束、机器可读保守分级及失败传播。

## WF-003及后续评审约束

权威验证树为干净LF检出，ref/worktree原始字节及Java指纹必须相等；不为方便添加脏树执行例外。入口由实际Git提交差异确定最低分级，--paths仅可增加触发，未知路径全量。只读dry-run显示计划和跳过理由，不代表套件通过。

远端状态账本见[OID/字节与授权记录](workflow-remote-state.json)：WF-002仅补传20个对象；WF-004按2026-10-24明确授权已完成两次准备分支推送，main未改。云端新检出已执行LFS fetch/fsck并通过输入校验；具体证据及无法独立读取的日志边界见复评记录。账本估算不是账户计费或剩余额度。

WF-005证据须保留排序OID/大小列表或聚合hash、fetch HTTP状态计数（未采集则null，不能以对象数冒充请求数）、fsck摘要、storage相对路径；日志用run相对路径+artifact名、保留hash/tail。发布产物一律记录干净LF检出的eol_policy、tested SHA；Java数量直接来源当次XML，并记录失败/错误/跳过。未覆盖人工门槛不得因自动化全绿关闭。WF-003先复用审计JSON和最小结果记录，不宣称已交付完整CI证据平台。

WF-003已完成：入口用法见[统一验证入口](workflow-verification.md)，本轮具体验证/失败处置见[19339f4复评](review-19339f4.md)。WF-004已获具体两步推送及CI临时runner全量预授权并执行首次CI；本机全量仍逐轮申请。

WF-004本地草案、两步首次同步方案与权限范围见[CI约定](workflow-ci.md)，本轮评审补修及具体验证见[复评记录](review-f2e748e.md)。首次CI的授权夹具前提补修6346c7d已通过完整云端run40076634386，WF-004完成；失败和通过证据均已归档，WF-005转READY但未实施。WEB-036仍暂停。

WF-005接续：复评处置见[9e5d17a归档](review-9e5d17a.md)，证据/进度/一方下载与CR/PR契约见[证据交接](workflow-evidence.md)。本地工具与真实push CI（00dd98f/run40080861974）的证据重算、回执通过；WF-005仍DOING，仅真实PR事件待授权验证，不声称PR/fork通过；WF-006/007不提前实施。

WF-005复评补修：三条P3与历史证据兼容见[2fb4edf归档](review-2fb4edf.md)；本轮仅本地工具/文档补修，真实PR仍待授权，状态不变。

2026-10-25收尾：用户明确授权推送2063274并创建[草稿PR #1](https://github.com/lzly2004/Village-of-Cyber-Remake/pull/1)。push run40086327059完整成功；真实PR run40149558683正确采用pull_request.base.sha，并在3文件24处历史尾随空白处严格拒绝、后续19项未执行，证据/回执复算通过。详见[本轮归档](review-2fb4edf.md)。WF-005为DONE（流程验证完成，不等于PR绿灯）；下一步主线整合待用户决定，不改main/门槛，不提前开展WF-006/007或WEB-036。前文DOING/待授权是对应日期阶段记录。

2026-10-25后续评审：P2-1摘要补修、P3-2表格口径、P3-3退出码解释见[归档](review-f089bda.md)。仅准备3文件24行空白补丁，未改main；正式Java验收/提交及两步推送范围待单独授权。WF-005状态不重开为未完成，WF-006/007仍不提前实施。

2026-10-25授权后收尾：0b28449真实PR红灯确认新摘要；独立3文件24行空白提交3b8641c经本机完整验收及真实PR完整CI通过，取消push单次补验也成功，最新两个verify全绿、PR clean且仍draft。证据包/回执复算和边界见[本轮归档](review-f089bda.md)。main仍6163620；最终文档归档留本地，不将其冒充已测/已推代码SHA。前文待授权/红灯为对应阶段记录。下一步独立CR与用户主线决策，不自动推进WF-006/007或WEB-036。
