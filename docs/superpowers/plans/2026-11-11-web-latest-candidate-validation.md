# 最新Web候选统一验收与交付执行计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> 本批为既定交付流程的执行准备，不新增功能；当前用户已授权独立完成准备，完整验收/打包及外部发布仍遵循项目的逐轮授权。

**Goal:** 交付包含最近五个产品提交的同SHA完整Web试玩候选，取代c9f5db2旧候选。

**Architecture:** 使用当前Git提交的独立LF克隆，按提交指针校验32份LFS实际payload；复用tools/verify.sh完整计划和build-web-release.py的实际解包冒烟，不复用旧构建输出。窗口、音频、规则、API、格式、依赖与预算均不再修改。

**Tech Stack:** 既有Java21/Gradle8.8、Node/Playwright、Python、固定Caddy2.11.7；本机工具版本和CI固定版本分别记录。

**Spec:** [已确认收尾范围](../../web-migration-status-2026-11-11.md)、[统一入口](../../workflow-verification.md)、[交付流程](../../workflow-daily-guide.md)。

## Global Constraints

- MemAvailable至少2GiB；单worker、禁止并行JVM；只使用自建测试显示，不使用真实用户存档或关闭其他应用。
- 不刷新黄金、不改变断言/规则/API/交换格式，不更改系统信任、不监听公网、不购买域名。
- Linux桌面卡顿不是本批阻塞；Windows最新规则人工运行仍未验证，不冒充自动化证据。
- 本机全量/打包须新批准；新轮推送、云端产物上传及并发下载另核授权。不Ready、不合流、不部署。

## Review Focus

- 旧试玩版本被误认为最新版：固定head/base、run/attempt与产物SHA，并拒绝跨候选复用。
- 大村Replay裁切/滚动：默认回归覆盖七配役/44人头像与键盘路径。
- 淡色护卫历史丢失/错公开：覆盖真猎未CO、曾猎CO非人、CO前后/倒退及首次行动前死亡；活局不得暴露终局秘密。
- 规则修复破坏随机/旧录像：保留七配役轨迹、兼容与真实双向文件交换，不刷新既定摘要。
- 启动环境中止：内存/端口/profile/显示检查；不清锁或终止他人，保留首失败，环境重试最多一次。

## Task 1: 固定来源与执行前提（本轮准备范围）

**Files:** 本计划；build/web050/latest-candidate-preparation/中的plan.json、preflight.json、snapshot.json、changed-paths.json；独立克隆web050-latest-validation-20261111。

**Interfaces:** 输入c9f5db2至当前干净HEAD；输出精确SHA、逐文件哈希、LFS核对和只读dry-run计划。无旧build/test XML/oracle复制。

- [ ] 提交本计划，Windows只同步同一文档及Git提交，核对42项无关状态保留。
- [ ] 创建独立克隆，校验跟踪字节/CRLF/LFS；node_modules仅复制锁文件对应现有安装，构建输出从零生成。
- [ ] 运行`bash tools/verify.sh --base c9f5db2 --full --dry-run --json`；要求status=planned、tier=full、所选24项，不计测试通过。
- [ ] 核对本地Python的Pillow/WebP/PyYAML、Node/Java/Gradle/浏览器/字体、固定Caddy哈希及磁盘/内存。不启动JVM、浏览器、Xvfb或服务。

## Task 2: 获本轮批准后统一验收与打包

**Files:** 快照内build/verify、build/releases/latest-candidate、build/playtest-tools.zip；不修改跟踪文件。

**Interfaces:** 输入Task1固定快照及新授权；输出同head的result.json、发布包/manifest/report、工具包及实际解包冒烟。

- [ ] 即时复核内存、旧工具链daemon及自有进程；按既有安全显示方式创建独立Xvfb。不得使用env.sh中可能过时的DISPLAY=:2。
- [ ] 设置本轮获准的VOCR_APPROVED_FULL_CHECK=1，工具链环境、兼容Python、VOCR_CADDY和自建DISPLAY；串行执行`bash tools/verify.sh --base c9f5db2 --full --json`，保存stdout/stderr。
- [ ] 要求24项完成、全部必要XML零失败/错误/跳过、最终输入审计一致；否则停止并记录实际失败，不打包失败结果。
- [ ] 运行`python3 -B tools/build-web-release.py build --verified-result <本次result.json绝对路径> --output <快照>/build/releases/latest-candidate`。要求check_release及packaged_smoke=passed；不重复触发另一轮full。
- [ ] 调用playtest_bundle.bundle打包当前源码工具，回读核验SHA256SUMS；核对发布source_sha、runtime stamp、前端inventory与result三方绑定。停止自己启动的显示和服务。

## Task 3: 具体发布授权后的云端交付

**Files:** build/web050/latest-candidate-preparation/publication/中的PR正文、证据、CR增量包及最终start.sh。

**Interfaces:** 输入Task2通过证据和精确head；输出同SHA真实云端CI候选，不把本机结果当云端结果。

- [ ] 重新只读核对实际远端PR #12/#13的head/base/draft及main，不使用当前指向Windows目录的本地origin误推。只使用核实的GitHub仓库/分支路径。
- [ ] 备好旧候选c9f5db2→新head的完整差异、五项产品变化、历史评审范围与未验收项，再请求所缺推送/产物上传授权。
- [ ] 获准后更新现有PR #13草稿；核验云端full24和verify/receipt/release/工具包，下载后逐项复算，不将pending或旧green当通过。
- [ ] 最终交付前复查18500/17075、管理profile与内存，输出同head自助命令；不自动清锁、不关闭浏览器。完整一局重点为菜单→开局→对话/Info/Record→结算→Replay/历史→返回/再开局。
- [ ] 用户整局反馈与CR分别记录、处置；本计划不授权Ready/合流，也不关闭人工、容量或发布门。

## 准备完成后的交接

以build/web050/latest-candidate-preparation下真实报告勾选准备步骤；计划中的未勾选步骤不等于已执行。纯文档计划提交不重跑游戏；最终候选必须绑定最终head，后续代码变化使当前准备失效并重做受影响范围。
