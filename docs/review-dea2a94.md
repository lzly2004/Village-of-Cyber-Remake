# 8ef3240合流及394779a/dea2a94补充评审处置

2026-10-25用户要求coder决定评审待定项并继续维护。评审结论通过，无阻断；617/189/136/15及main首次CI是前轮真实云端证据，不能写作本轮本机重跑。审查环境已由用户恢复，本轮不运行root/bootstrap、修改挂载或系统配置。

## 决策与事实核实

| 项 | 处置 |
|---|---|
| F-1 main文档滞后 | 采纳。维护分支从最新origin/main@8ef3240建立；同步三个源提交ddd86b1→394779a→dea2a94，不能只取后两项遗漏前置归档。合并树等于3b8641c不代表包含其后ddd86b1文档 |
| F-2 / P3-C required checks | 推荐后续配置main的verify required并要求分支与最新base同步，保留手动merge commit，不启用自动合并；不新增单人项目审批人数要求。远端设置本轮不改，具体生效/管理员约束/权限和回退需独立可审配置方案与明确设置授权。CI存在不等于强制门禁 |
| F-3 full预算 | 604秒为main首轮verify任务观测，run总时长608秒；仅作缓存已热的近期参考，不当作固定时限或后续速度承诺 |
| F-4 / oracle | 干净LF树Java指纹ff7cb300与main相同；原CRLF用户树42d…不作权威执行树。本轮不跑本机gate，后续输入改变须按最终源码重新生成；相同源码已有oracle只读核对同源后才可复用，不因提交hash改变机械重建，也不跨不同原字节树复用 |
| P3-A 从main建分支 | 采纳。实际git cat-file能读取8ef3240对象；原分支与main共同祖先3b8641c，三点diff为11文件，不会仅因分叉自动回到3700多文件。此核验不否定reviewer沙箱曾无法读取对象；仍选从main开干净分支降低基线歧义 |
| P3-B tools/ci分级 | 保留未知路径full，不将tools/ci/*整体降到tooling。该目录包括context/启动、工具下载/解包和完整性验证；本轮bootstrap也影响云端准备。55/32/14契约虽必要，不能一概替代成功路径及完整集成验证。未来按具体文件细分须单独提出覆盖矩阵与策略测试，不能为了省本次全量直接改门槛 |
| JAR与类字节声明 | 原始字节补丁和相同云端JAR记录相互支持无行为变化；未下载724MB JAR，继续声明hash来源为wrapper，不把记录比对冒充独立JAR字节计算 |
| 未验证项 | fork/真实外部PR、一方认证ZIP下载、runner内部/字体包固定、人工/容量/发布仍开放；不以自动化全绿关闭 |

## 本轮实施

在既有wf003-clean隔离树从源仓库已同步的origin/main引用拉取对象，建立maintenance/wf005-tools。原脏仓库不switch/reset、不移动本地main@1332d3b、不推旧准备分支。

三个源提交及迁移后提交：ddd86b1→cbffc12（空白补修与PR绿灯归档）；394779a→477ff3c（Git环境记录/assembly_revision 4/兼容测试）；dea2a94→6fde232（主线合流归档）。迁移无冲突，前三项迁移后的完整文件树与源dea2a94相同，仅父历史与commit hash改变。随后本记录及当前状态文档独立更新。不改Java/游戏规则/随机源/存储/协议/生产依赖、分级策略或workflow触发配置。

本轮本机实际运行：python3 -B tools/tests/test_ci_workflow.py -v 55/55（0.062s，证据29项为其中子集）；tools/tests/test_verify.py 32/32（1.862s）；tools/tests/test_ci_inputs.py 14/14（0.737s）。实际调用bootstrap.git_environment只读返回git version 2.25.1、git_diff_rename_limit=null。AST/文档链接/增量diff-check及分级dry-run另行核对。没有启动JVM或本机全量，未将前轮617/FatJAR当本轮结果。

## 发布与交接边界

准备一个基于main的维护草稿PR，合并前三项历史归档及工具补修，整项按现有规则触发full层20项；不拆成文档先推main、工具再混入的直接写主线流程。原准备分支保留。新分支推送和新PR创建需列最终SHA/范围后获得明确授权，已有PR #1合流授权不扩展成任意新PR授权。云端full仍用已授权配置；不借维护任务运行本机完整Java/打包。

新维护PR验收/独立CR前不合并该补修，不改仓库设置。WEB-036原READY顺序保留，本轮维护期间不混入实现；WF-006/007保持PLANNED，未开展三次同SHA/失败注入/Auto-Fix。
