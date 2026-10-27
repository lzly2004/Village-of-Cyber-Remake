# WEB-042 统一构建与可追溯 Web 发布候选包

## 范围与边界

用户2026-10-27批准“可解压运行ZIP”，设计由Coder决定；本轮不引入容器、生产依赖、部署、GitHub Release或预算变更。不改Java游戏源、规则、随机源、API、录像/存储格式。原桌面FatJAR继续按原任务与内容边界构建。

本包供后续托管准备，不是面向最终用户的双击本地Web启动器。后端可由外置Java21+运行；前端是独立静态文件，生产同源/HTTPS/代理由043实施。当前本机验收仅使用既有Vite preview，不可把它当生产服务器。人工、容量与发布门槛不因产出ZIP关闭。

## 唯一构建与验收链

在干净LF检出、LFS原件和固定工具链已准备、MemAvailable至少2GiB、有显示/CJK字体的Linux环境执行。须先取得**本轮**全量及打包授权，获批后才设置批准变量。所有JVM步骤仍经`tools/run-gradle-safe.sh`，单worker、不并行、不修改监护/系统。

```bash
# 只规划，不授权，也不生成ZIP。
bash tools/verify.sh --base BASE_SHA --full --dry-run --json

# 仅在当轮获批后运行；自动调用既有verify full一次，然后组包并实际冒烟。
VOCR_APPROVED_FULL_CHECK=1 python3 -B tools/build-web-release.py build \
  --base BASE_SHA --output "$PWD/build/releases/UNIQUE_DIRECTORY"

# 同次完整验证已单独完成时复用精确result，不再次跑全门；包内冒烟仍需批准。
VOCR_APPROVED_FULL_CHECK=1 python3 -B tools/build-web-release.py build \
  --verified-result "$PWD/build/verify/RUN_DIRECTORY/result.json" \
  --output "$PWD/build/releases/ANOTHER_UNIQUE_DIRECTORY"

# 只读检查ZIP：不提取、不执行、不需要完整验收授权。
python3 -B tools/build-web-release.py check --archive build/releases/UNIQUE_DIRECTORY/vocr-web.zip
```

默认编排不会重复验证。`--verified-result`不是跳测：只接受当前HEAD、干净输入、输入前后审计相等的full **22项**全部passed；逐套件日志哈希、构建策略、源码/概率、运行时stamp和当前文件均须匹配，拒绝dry-run、失败、旧HEAD或缺项结果。没有跳过冒烟开关。额外保留21项时期的历史证据，但其结果不能用于新发布。

22项是在038之后21项基础上增加`release-tests`。`java-full`实际执行`fatJar prepareWebRuntime --offline --rerun-tasks`，完整test/check、概率/资源、特征轨迹及FatJAR门槛保持；最终同源oracle与15项gate、真实文件双向交换保持原顺序。包内冒烟是22项**之后**的独立步骤，不伪装成其中一个套件。

`web-build`成功时记录全部dist相对路径/字节/SHA；`java-full`成功时记录runtime-build.json哈希。组包拒绝忽略目录中迟后新增、删改的前端文件或替换的runtime stamp，不能只靠Git干净放行构建产物。

## ZIP与来源

输出目录必须尚不存在且位于本仓库`build/`内，不允许符号链接/越界/覆盖。候选包经完整检查、安全解包、真实整局冒烟及最终复核后才原子移至指定目录。失败候选留在`build/release-attempts/`，冒烟日志/截图留在`build/verify/release-smoke-*/`供诊断，不自动重试、不当成功发布。

| 路径 | 内容 |
|---|---|
| frontend/ | 本次生产dist全部文件；不含源WAV/PNG、依赖缓存、用户录像或本地账本 |
| backend/server.jar | main+web生产编译类，GuestGameServer入口；无种子测试入口、JUnit、原媒体或用户data；共享桌面代码类仍保留，不声称已从共享代码剥离Swing |
| backend/lib/ | 原解析依赖JAR，分别保留services/签名/许可等元数据，不压平或重复引入依赖 |
| backend/run.sh、run.cmd | 固定classpath/小堆/loopback入口，核对概率配置SHA；CMD原生执行仍待Windows实测 |
| config/probability.json | 源码树唯一概率文件的原始字节；不允许就地修改后仍声称是已验包 |
| metadata/runtime-build.json | 源码SHA、Gradle/JVM版本、生产Java与构建/概率输入哈希、运行时JAR清单 |
| LICENSE、RELEASE-README.txt | 项目许可与运行边界；第三方依赖自身元数据继续保留 |
| release-manifest.json | 源码/输入指纹、工具/前端依赖版本、验证result哈希、预算与逐文件大小/SHA |
| SHA256SUMS | 所有其他ZIP条目（含manifest）的SHA；不自引用 |

最终目录另含`release-manifest.json`和`release-report.json`侧车。报告绑定ZIP字节数/SHA、manifest SHA、同次result SHA和包内冒烟日志SHA。ZIP内部不放其自身哈希或后续冒烟报告，避免自引用；内部验证声明明确“打包前完整验证”，不冒充已包含打包后证据。

ZIP条目排序、时间/权限/压缩策略固定，相同输入及相同验证结果可逐字节重建；不同JDK/依赖/验证结果日志不承诺同哈希。拒绝重复或大小写冲突、路径穿越、链接/特殊文件、加密、超大条目/膨胀、意外文件、清单/哈希不符。ZIP192MiB、单条32MiB、2万条是解析安全边界；backend64MiB是独立工程边界，不挪用020浏览器预算。

## 预算与实际运行

静态预算沿020：全部100MiB；代码含台词1MiB/逐文件gzip512KiB；图片32MiB/单图512KiB；BGM64MiB/单曲5MiB；音效1MiB；JSON/TXT合计1MiB。按全部实际文件计量，备用编码也算；未知格式（含尚未由050确认的字体）明确拒绝，不能漏算。复用现有图片与音频distribution门禁，不重新转码、不提高上限。

1.5MiB冷大厅/3MiB首局新增仍由既有assets浏览器实测；ZIP压缩体积不等于浏览器传输预算，也不等于容量100验证。049仍须复检最终文件集，050引入字体仍按已批准前置门。

解包后先核对外部来源与ZIP SHA，运行只读check，再在解包根目录用`sha256sum -c SHA256SUMS`核对全部文件。Linux需要Java21+、bash、sha256sum；Windows脚本还需PowerShell。`backend/run.sh [port]`默认127.0.0.1:7070，禁止对公网直接暴露后端；启动前概率文件缺失/变更即失败，不允许悄悄退回默认配置。运行不需Gradle、源码仓库、原WAV/PNG或前端开发工具。

本机包内冒烟在build内解包，通过既有安全入口调用`runPackagedWeb`→包内run.sh；Vite preview仅服务解包frontend。复用无种子生产整局用例，检查刷新、终局、浏览器战绩、录像导出/回放，无routes/fixture/选结果；单worker/retries=0。结束后还核对ZIP未变、解包文件集/字节未变及原验证输入未漂移，失败不发布。该用例不是多用户/容量/人工听审或HTTPS验收。

## CI交付与独立CR

full层成功后wrapper复用本次result，运行同一组包入口与包内冒烟；docs/tooling/frontend不生成发布包。构建或冒烟失败，即使22项全绿也判失败。独立上传`release-RUN-ATTEMPT`（ZIP及两个侧车），14天保留；不会创建GitHub Release或部署。上传失败也不得声明交付成功。

轻量`verify-RUN-ATTEMPT`仅带报告、上传身份、原日志和小截图；不把大ZIP塞进它，也不放宽既有download.py的32MiB单条限制。修订5证据绑定release artifact id/digest/URL/head/run/attempt与报告SHA，并复核包内冒烟原日志的SHA及1项通过/无失败跳过声明；修订1—4按原规则精确复算。

CR先复算verify/receipt，再独立下载release artifact并核对GitHub外层digest，核对内部ZIP的SHA/字节数与报告一致，再用只读check校验内容；**仅绑定上传元数据不代表已经下载验证二进制**。交接包分别注明每一层实际做没做。下载外层ZIP仍须防越界/链接/膨胀，不能因域名可信而盲目解包执行。

Coder负责实现、验证、CI读取及交接包；用户负责转交独立CR与具体发布/合流授权；CR只读审查。首审后任何追加提交都先增量复审，Ready/merge commit及main CI另行获批；本轮不直接进入043。

## 本轮本机验收记录

轻量验证已实际执行：release工具16/16、verify入口35/35、CI契约65/65、证据35/35；配置`--list`发现真实生产冒烟1项（不等于执行通过）。保存的041 main真实修订4产物以新下载器离线逐字段复算通过，属兼容验证，不是重跑041全量。

保留首次失败：新增release测试工厂的`self.build`字典遮蔽方法导致12项中11 errors，修正工厂后通过；证据测试共享proof方法放置错误导致2 errors，移到无测试方法的Fixture后通过；新增前端产物测试因夹具未跟踪dist导致输入审计拒绝，修正夹具提交前提后35/35。未放宽生产断言。

用户本轮明确授权后，在隔离LF快照`c3ebef7014beb224c5e80034a9d9699a2ee54f28`实际执行完整22项，**全部passed，首次执行成功、无重试**。此SHA是验收输入快照，非最终交付提交；后续只同步验收文档，提交前另以逐文件字节/政策指纹证明实现与快照等价，不把不同SHA偷换成同一个。

| 实际执行 | 结果 |
|---|---|
| Java完整test/check、概率/资源、FatJAR | 648/648，65份XML，0失败/错误/跳过；Gradle11个任务全部实际执行 |
| 前端单测/默认浏览器 | 200/200、180/180 |
| 七配役gate/双向桌面交换 | 15/15、1/1；oracle全部匹配最终Java指纹 |
| assets/audio/files-repeat/生产preview | 4/4、27/27、30/30、1/1 |
| guarded/audit/verify/ci/release/image/audio工具/交换契约 | 15/14/35/65/16/17/18/3，全部通过 |
| 发布包实际整局冒烟 | 解包后run.sh + 解包frontend preview，1/1（7.7秒）；成功后ZIP及全部解包文件字节未变 |
| 独立包检查/解包SHA清单 | 只读check通过；sha256sum -c共1262条通过 |

静态预算本轮实测：总77,768,263字节；代码595,231/gzip162,265；图片21,291,852；BGM54,890,214；音效498,275；JSON/TXT492,691。均在原限额内，未调整预算。独立运行时40个JAR（server+39依赖），server.jar518,216字节，无测试入口/原媒体，整runtime约10.09MB；这些后端字节与浏览器100MiB预算分开列示。

发布ZIP **86,044,678字节**，SHA-256 `af38cff6c500f25e0723cf17b85667bb9b53b5fc721f350a64eaf711ea763ba8`；manifest SHA `f60f62b48247c1007f5ab166dec78a7c7b85a61409e579b132b74765633a1cff`。侧车绑定原验证result SHA `7c85ed0f4bf251a1f0db62bd2c0d2a99c2b3df77db6bd25591e9fd9ba6417b5f` 和包内冒烟log SHA `5111b09bc51d99981141941907ab7d7d387d9f562ca7b6e5f34606aa5fea5d3f`。ZIP绑定快照源码，不重写为尚未验证的交付/远端SHA；云端发布时须在真实head重新产出自己的包。

本机FatJAR本轮重新构建并计算：724,197,819字节，SHA `2e9a59594169287ba97efb14a055fa62d16eaf0ac4dc6d82fe9925347a0af57c`；与041本机值一致，但本条是本轮实际执行，不引用云端d7232b8c冒充。Java指纹`f4d94f7a…9ae2`（231）、frozen`6ce8aa54`（9）、inputs`3d4c43fa`（1979）未变；build_policy49文件为`281e793b594c9c53e0b9e6f81c082c65c036940d7247ed92e2bfdeee7b1cba97`。

工具：本机JDK21.0.12.1/Gradle8.8，完整资源工具使用已准备Python3.12.14/Pillow12.3.0/libwebp1.6.0与固定FFmpeg；不安装系统软件。启动前可用内存约7.7GiB；安全入口保留2GiB门槛。抽测Gradle launcher与daemon的oom_score_adj均为500，未冒充硬cgroup隔离。包内启动仍有既有Javalin/SLF4J无provider警告，本轮未加日志依赖；不把整局通过等同于044日志治理。

包内三张截图（战绩、Replay、终局）已由Coder目视并登记SHA：可见内容未发现本轮新增异常；Replay横向/弹窗纵向、终局名单内部滚动仍是既有交互，不能把单张截图没展示全部内容当作已验证末项可达。正式布局/字体/日文化对照仍归050/046，CR图像能力限制单列，A06/A07/A08不放行。

原始日志、full-result/summary、runtime-boundary、包检查、截图哈希和保护证明集中于忽略目录`build/web042`；快照完整结果位于`build/verify/20261027T121751Z-ffe79c98/`，包位于快照`build/releases/web042/`。尚未推送/云端CI、Windows原生执行、HTTPS代理、容量100或人工正式验收；不得以本机结果替代云端首审包。
