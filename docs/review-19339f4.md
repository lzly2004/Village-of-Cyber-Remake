# 19339f4 复评与 WF-003 处置

范围 ee9905c + 19339f4。外部评审接受 WF-002，无阻断；独立189前端、136默认浏览器、15 gate和文件重复验证通过，另复现缺git诊断、独立重算指纹/OID集合。Java617、FatJAR、FFmpeg门禁和远端对象返回字节未由reviewer独立复跑，仍是coder侧WF-002证据；本轮不将这些历史数字写作重跑。

## 逐项处置

- P2-1：采纳。补[远端状态账本](workflow-remote-state.json)中的31对象排序OID/大小、聚合SHA、fsck摘录和storage相对路径，供携带复核；历史HTTP状态请求数未采集，明确null，不把20对象冒充20次200请求。WF-005再接入通用采集/artifact打包。
- P2-2：采纳。账本记录20个补传对象、584372132字节、2026-10-24日期精度、用户授权原句、refs未推送及已知payload成本估算。不声称查询了账户计费或余额；WF-004推refs前核对缺失集合和具体授权范围，之后fresh clone/fetch/fsck。
- P3-3：采纳eol_policy字段与发布产物只取干净LF树。JAR415字节/大厅1字节差异为环境间观察，未隔离所有变量，不将EOL判为唯一原因。
- P3-4：补进校准文档，34/34是git-lfs进度而非当前提交树对象数，其计数来源尚未独立追踪；树内32路径/31唯一OID由指针及hash定义。
- P3-5：新入口日志采用run相对路径，保留hash/tail；旧WF-002绝对路径保留原始上下文，不假装reviewer能访问。artifact名/CI运行关联归WF-005。
- P3-6：新入口直接解析当次XML的tests/failures/errors/skipped，要求非零且无失败/错误/跳过，过期结果拒绝。此为工具契约测试通过，不代表本轮重跑617项。

WF-003具体入口约定见[统一验证入口](workflow-verification.md)。本轮未执行任何远端写入、没有Actions或Auto-Fix，WEB-036继续暂停。

## 本轮实施与验证

新增verify.sh/verify.py/verify-tiers.json与隔离工具测试，独立桌面交换配置/用例；未修改生产Java、规则、协议、存储、依赖或原安全脚本。实际执行与证据如下。

- `python3 -B tools/tests/test_verify.py -v`：最终26/26（原工作区1.613s）；全部临时Git/受控runner，无FFmpeg/JVM。脏树/指纹不等/CRLF、缺LFS、oracle缺失/旧/错名、未知路径/重命名/手工路径不能降级、缺授权/显示/服务器覆盖、首错停止、XML缺失/旧/失败/跳过、交换文件旧/缺/软链接、运行后输入变化与输出目录越界均覆盖。
- `python3 -B tools/tests/test_ci_inputs.py -v`：14/14（0.881s）。原审计工具无代码变更。
- shell `bash -n`、Python AST、策略/证据JSON解析及相关diff检查通过；不是Java/浏览器全门。
- 从19339f4在ext4独立检出，只复制6个最终工具文件并形成隔离候选提交 `1cd1318766ad0f095249444009476769e0383065`；`bash tools/verify.sh --base 19339f4 --json`实际按tooling执行diff-check、14审计测试（0.861s）和26入口测试（1.386s），均通过，前后输入/冻结/Java/政策指纹一致。原工作区6文件与候选逐字节相同，最终主分支提交仅另含文档/账本/状态。
- 同候选 `--full --dry-run --json` 返回planned，列出全部顺序与需本轮授权，不启动Java/浏览器。最初未水合时该完整计划明确拒绝LFS；随后只复用WF-002逐个hash/长度核验的31个本地对象，非本轮远端拉取。tooling本身不要求音频，因此此前工具级执行可成功。
- 新配置缺 `VOCR_VERIFY_DESKTOP_EXPORT` 时collection明确失败；显式使用WF-002 `desktop-actual.json`后，单项定向浏览器交换1/1（6.4s）通过。该项测试的配置版本在隔离提交824aac0，后续仅收紧Python入口仓库绑定并增测，配置/用例逐字节未变；不机械重跑浏览器。固定输入来自WF-002，**不是本轮新Java生成的完整跨端链**；当完整入口运行时另强制两份文件均为当次生成。
- 冒烟复用原JVM安全入口、单worker和2GiB门槛；未执行Java完整test/check/FatJAR、默认136、15gate、资源全门。WF-002的617/189/136/15等仅作为旧校准引用，未记本轮重跑。没有申请或复用旧全量授权。

失败/修正保留：首轮入口22/23，符号链接目录测试被更早的脏树门拒绝；仅在隔离fixture添加局部exclude后才触达目录门，未削弱生产拒绝。此后补工具诊断/策略形状/自身仓库约束至26项。首次真实交换启动因配置位于tools导致Vite默认cwd变为tools而ENOENT；修复为继承显式cwd或默认指回web-client，保留原命令与安全入口，先确认旧JVM退出后再验证，最终1/1。两者均不算“环境偶发”或掩盖为旧绿灯。

工具结果目录（相对候选根）：`build/verify/20261024T071127Z-ead1a3c3/result.json`，含直接复用的审计JSON、run相对日志与hash/tail；本轮准备/负例/冒烟日志在`build/wf003`。以下为可携带的最终套件日志摘要（名称相对run目录，不要求reviewer可访问本地/home）：

| 套件 | SHA256 |
|---|---|
| audit-tests.log | `8b62dbe219295a86da4c90a7545f30ade5265d934677d058a058cf4621b43141` |
| verify-tests.log | `b267f61f1083ba88cc482ae22b77f32043aea498f96d341be596ae2b8c24783d` |

Java ref/worktree均为`9ffa8d2d8714a555f856ee19f4b86ace466df6c1aade366df0b5701211b967b7`，冻结`6ce8aa549a2f49923b763b04d819d3ed29870c9bbd0c30745e97c08c3beca8b8`、输入`3d4c43fae21fbb7db0a68cdaa44db66aeaa8478760853479f6a539abfcabbe4a`；本轮政策34文件`6591431519b0993fdf7076280353c208d613c3a4cf258bea6b31ec49b057b51e`，与开始/结束一致。单次通过不构成无flake证明。A06/A07/A08、046/049人工与047容量保持not_covered。


收尾：原工作区实际调用dry-run返回DIRTY_OR_DIFFERENT_WORKTREE，未启动套件；原137条porcelain状态与原件hash按开工快照保留。定向冒烟退出后只读检查JVM/Chromium/Vite/Xvfb均无残留。本批只提交13个工作流相关文件，不混入用户修改；无远端写入。下一项WF-004，尚未实施。
