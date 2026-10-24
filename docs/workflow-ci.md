# WF-004：确定性CI最小闭环

当前状态：用户于2026-10-24明确回复“授权两步推送并启用该CI”。两次准备分支推送已成功，首次完整CI在安全测试夹具的授权环境前提处失败，补修已完成本地验证，待确认推送后云端复验；未记为云端通过。没有改main、远端设置、secret或启用AI评论/修复。WEB-036仍暂停。

## 触发、身份和执行

`.github/workflows/verify.yml`在main或workflow/**准备分支push、面向main的PR，以及手动dispatch时触发。PR检出`pull_request.head.sha`并以`pull_request.base.sha`为差异起点；push检出after并以before为起点，新分支before全零则显式使用origin/default_branch。手动必须提供40位base SHA，可显式full用于同SHA复验。缺base对象、检出merge SHA而非事件head、删除分支或非法参数均拒绝，不回退HEAD^。

单job（ubuntu-22.04，45分钟）串行执行，取消同来源仓库/分支旧运行；push和PR使用同一分支组，避免同分支两个事件长期重复消耗。所有Action固定到完整commit SHA，见`tools/ci/toolchain.json`；只有`contents: read`，checkout不保留凭据，不使用pull_request_target、workflow_run、可写token或自动合并。

先作只读scope preview来选择依赖准备，再真实执行统一入口`--dry-run --json`，最后执行同一入口。preview不是准入通过；正式dry-run仍核对干净LF树/LFS与政策，实际执行后再比对base/head/plan/政策hash。执行日志与现有`build/verify/<run>/result.json`原样上传，CI关联信息旁写outcome.json，不改游戏或Replay格式。

browser-export.json、完整Java、desktop-actual.json与反向浏览器交换保持同一job、同次入口；**不跨job/artifact传递这些前置产物**。缓存只包含原始LFS对象、校验过的工具归档和依赖，不缓存oracle/XML/交换结果，不用缓存产物冒充当次生成。mtime契约维持原样，WF-005若拆job须另加来源证明。

## 工具与缓存

- Python3.12.12、Node22.23.0、Microsoft JDK21.0.11、Gradle8.8、Playwright1.64.1；依赖锁定在源码与CI lock。Microsoft官方安装目录未列出宿主21.0.12.1，因此CI明确采用目录中可核实的21.0.11；这是新环境，必须由云端全门重新验证，不引用旧617作为通过。
- Pillow12.3.0、imageio-ffmpeg0.6.0、PyYAML6.0.3只作为CI工具，通过CPython3.12 Linux x64轮子哈希安装。Gradle ZIP、Git LFS3.8.1归档及二进制、FFmpeg二进制均核对固定SHA；缓存命中仍重新验证，不自动吞掉坏缓存或降级版本。
- LFS先无smudge checkout，再恢复对象缓存、安装局部filter，执行真实fetch/checkout/fsck。不是lfs:false后跳过水合；该顺序避免先下载再恢复缓存。正式入口再次校验全部工作payload长度/SHA。
- Java依赖先通过原安全入口testClasses联网预热（不执行测试），正式完整验收保持offline与rerun-tasks。没有修改本机启动器、2GiB门槛或单worker约束。
- 浏览器/字体/Xvfb/xauth的依赖安装仅在**GitHub临时runner**发生；本机不执行sudo/apt或系统改动。runner label及APT不是OS镜像/字体逐字节锁，实际镜像版本另记，不把CI通过当正式DPI/听审。

## 证据与权限边界

始终上传context、plan、tools、outcome、现有result/log、XML、失败trace及测试交换JSON，保留14天。日志路径相对run，保留hash/tail。缓存/浏览器二进制/完整724MB级FatJAR不上传；FatJAR存在性、桌面类边界、字节数与hash写入outcome。artifact名为verify-<run_id>-<attempt>。任何步骤失败仍上传已存在证据；不存在的结果不制造成功。

发布此工作流会允许上述事件在临时runner自动执行确定性测试和必要Java全量/check/FatJAR；它不是本机逐轮全量授权，也不授权AI发帖、修复、合并、发布或改仓库设置。首次推送前必须由用户明确批准这个具体范围。未来变更权限/触发范围或生产发布另行批准。

## 首次同步的实际范围与历史边界

2026-10-24只读核对：远端main仍616362073999cb280ef8891a8f69272541904357，准备分支workflow/baseline-input-audit不存在。本地b38da1e时有159个未推送提交。直接新分支一次性推送会把CI base设为旧main；该长区间存在历史尾随空白，严格diff-check会首先拒绝，不能用CI搭建偷偷改生产Java或放宽门槛。

已获授权并执行的首次同步：

1. 已把最近已接受的f2e748eb0ad048803bc3f2f2d6623835632ffe04推为同名准备分支，建立已评审基线；该提交不含.github工作流，不会启动本配置。
2. 已快进推送本轮CI提交f6a1679592be40d125b17ad33ca92e2c3f2e3f76（包含独立补修b38da1e），由真实push.before=f2e748e触发完整CI。本轮范围包含.github，因此按full执行，不会因只比较准备阶段而缩减首次完整套件。
3. 不创建PR、不合并、不更新main或master。未来向仍旧的main提PR会按真实PR base检测出历史空白；届时需另行授权处理或决定主线同步，不能伪造PR base。这项历史问题保留为后续主线整合前置，不影响本次准备分支CI机制验证。

已读核对全部历史32个唯一LFS身份：当前31个使用WF-002的远端证明，额外历史Info (1).wav对象343e86f0…432b（5864014字节）本轮从空目录远端实际下载核对。没有补传新对象；当前31与历史额外1的口径分开，详见远端账本。推送时仍需核对目标分支未被他人创建/更新，并只做快进，不force；原137项工作区差异不进提交。

官方语义与接口核对来源：[Actions触发](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows)、[缓存作用域](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching)、[setup-java固定版本](https://github.com/actions/setup-java/tree/b843ebf9b913146b38da2eaa7aeddf76074ae1ad)、[setup-node固定版本](https://github.com/actions/setup-node/tree/949feb2413d6458794dcd2491c4babbbce0c15c1)。Action tag仅作说明，执行依据完整SHA。

本轮具体本地验证与外部执行状态集中见[复评处置](review-f2e748e.md)。A06/A07/A08、046/049人工和047容量门槛均未放行。

首次云端运行：[40076072948](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40076072948)，event=push，head=f6a1679；结论为failure：diff-check通过，安全测试14/15，其他套件尚未执行；补修与实际验证见复评记录。用户授权涵盖本配置所列事件下临时runner的自动完整验收/打包与所需依赖安装，不复用为本机全量授权。
