# WF-002 干净检出全门校准

状态：完整校准已通过。测试源码是 ee9905c92286516a358e1376cdabf895794cd757，分支 workflow/baseline-input-audit。本批仅审计工具补强与校准记录，不改生产 Java、游戏规则、协议、存储或依赖；未进入 WEB-036。

## 输入与环境

权威树为 ext4 的 `wf002-clean`，位于本聊天工作目录下。先从 GitHub HTTPS 做无检出浅克隆（远端 main 为 616362073999cb280ef8891a8f69272541904357），再仅获取原仓库已提交的 Git 对象至 ee9905c 并 detached checkout。此为“远端基线 + 本地尚未推送提交”，不是声称该测试 SHA 已在 GitHub。原工作区的 137 条 porcelain 差异与私人文件不作为校准输入。

局部 core.autocrlf=false、core.eol=lf，真实 git-lfs filter，不改全局配置。`--require-clean` 同时核验 Git 状态和跟踪文件原始字节；225 份 Java 的 ref/worktree 指纹均为 `9ffa8d2d8714a555f856ee19f4b86ace466df6c1aade366df0b5701211b967b7`。冻结组9文件 `6ce8aa549a2f49923b763b04d819d3ed29870c9bbd0c30745e97c08c3beca8b8`、输入组1979文件 `3d4c43fae21fbb7db0a68cdaa44db66aeaa8478760853479f6a539abfcabbe4a`、本轮构建政策组28文件 `f325f9eac22e1675884066a3c0668f5f33a6d8fc30e3d908267e081c4cde65fb`。运行前后重新审计，不改黄金或冻结样本。

工具位于持久的 `wf002-toolchain`，没有安装系统包、修改系统/挂载/swap/全局 Git/SSH 配置，也没有使用 reviewer 的 choom shim。JDK/Node/Gradle/浏览器复用既有版本化用户运行时，未重新下载其原始安装包；Gradle user home 和 npm cache 是本次新建的空目录，依赖先联网预热，再离线 Java 验收。

| 工具 | 本轮实际版本与用途 |
|---|---|
| JDK | Microsoft 21.0.12.1+1-LTS |
| Node / Gradle | 22.23.0 / 8.8 |
| Playwright / Chromium | 1.64.1 / 156.0.8078.4，revision 1248；仓库配置不覆盖 |
| Git LFS | 3.8.1；官方归档 SHA256 e72cf723a4a660438862282e8aecf3885fc8123e88bfcad534cf6b941a354b75，二进制 8bf08ae98ff83773430ce8f45b9156dcf7699ee1de65ae4a388e746e9dd7597c |
| FFmpeg | 7.0.2；持久副本 SHA256 e7e7fb30477f717e6f55f9180a70386c62677ef8a4d4d1a5d948f4098aa3eb99 |
| Python / Pillow | 3.8.10 / 7.0.0，系统 WebP decoder 0x000601（0.6.1）；仅日常 check/distribution，未重新转码，不声称满足生成器固定依赖 |
| 显示 / 字体 | 用户目录解包 Xvfb 2:1.20.13-1ubuntu1~20.04.20，独立 :2；fonts-noto-cjk 1:20190410+repack1-2。Xvfb 包 SHA256 b671759ad2b8280723b0b55361368dc69c12cb301ea9a6a5e443e4f8d2745a2d |

`build/wf002/toolchain.json` 保留下载来源、运行时/浏览器/字体及依赖 JAR 的实际哈希。`wf002-toolchain/env.sh` 显式设置 JAVA_HOME、VOCR_GRADLE、独立 GRADLE_USER_HOME、FFMPEG、PLAYWRIGHT_BROWSERS_PATH、DISPLAY，并 unset VOCR_WEB_SERVER_COMMAND。Xvfb 由单独父进程管理直接子进程，关闭时等待回收，不扫描或批量结束用户进程。

## 远端 LFS 修复

首次克隆时 LFS store 为空，真实远端 fetch 下载11个唯一对象，其余20个唯一对象明确404（21条路径，584372132字节）。当前树总计32个指针、31个唯一 payload；按路径声明620138120字节，按唯一对象605302710字节。随后为继续本地校准，仅将原仓库中已核对长度/SHA的20个对象复制到校准树，属于混合来源，不能将这次本地水合称为远端拉取成功。

用户另行明确授权仅补传这20个对象；使用 `git lfs push --object-id origin <20个精确OID>`，不推 Git refs。首次因 SSH host-key 检查失败且无可用身份，0对象发送；从 GitHub HTTPS meta 获取并对照官方指纹写入独立 known-hosts 文件，用户自行生成并添加认证公钥后再执行。未读取、复制或记录私钥/口令，不修改用户 SSH 配置。

补传已成功（20/20）。远端证明另用全新的 `.git/lfs-remote-proof`，只放入首次从远端下载的11个对象，禁止预置原缺失的20个，再执行 `git -c lfs.storage=<proof> lfs fetch origin HEAD`、`git -c lfs.storage=<proof> lfs fsck`，最后逐个核对全部31对象大小/SHA。最终 fetch、fsck 与31对象大小/SHA校验全部通过，remote-proof.json 记录首次远端11对象与补传后新下载20对象的来源。

## 本轮验证

日志在权威树 `build/wf002`；网络/原工作区保护证据另在原仓库 `build/wf002`。所有 JVM 与浏览器套件串行，单worker；Java 通过原 run-gradle-safe.sh（真实 choom +500 与监护），没有降低2GiB门槛。完整 fatJar 和15项gate已获得本轮单独授权。

| 命令（仓库根执行） | 本轮结果 | 证据 |
|---|---|---|
| `python3 -B tools/tests/test_ci_inputs.py -v` | 14/14 | tool-tests.log |
| `bash tools/run-gradle-safe.sh testClasses` | 冷缓存依赖预热成功，1m25s；未执行测试 | bootstrap-java.log |
| `npm --prefix web-client ci --cache <新缓存>` | 安装成功 | npm-ci.log |
| `npm --prefix web-client run test:unit` | 189/189，2185ms | unit.log |
| `npm --prefix web-client run build` | 图片+音频 prebuild/postbuild 全通过；代码580082字节/gzip157269，全站77753114字节 | build.log |
| `npm --prefix web-client run test:assets` | 4/4，6.3s；冷大厅131273字节、首局新增397068字节、初始17图 | assets.log |
| `npm --prefix web-client run test:audio` | 27/27，36.0s | audio.log |
| `npm --prefix web-client run test:e2e -- tests/replay-files.spec.js --repeat-each=3` | 30/30，32.7s，retries=0 | files-repeat.log |
| `npm --prefix web-client run test:e2e` | 136/136，2.7m | default.log |
| `VOCR_APPROVED_FULL_CHECK=1 VOCR_WEB035_BROWSER_EXPORT="$PWD/build/web035/browser-export.json" bash tools/run-gradle-safe.sh fatJar --offline` | 617/617，61类，0失败/错误/跳过；29s；check/概率/运行时资源/FatJAR通过 | full-final.log、build/test-results/test |
| 独立 LFS storage 远端 fetch + fsck + 全部大小/SHA | 32路径/31唯一对象全部通过；20个缺失对象确实重新下载 | remote-proof.log/json |

本轮网页实际下载 `build/web035/browser-export.json`，SHA256 `323ef3dd104bdc78c76c442a4c89905a3955927a79875e26f55d2e67bfa15c47`。全量测试显式传入其绝对路径，存在性先核对；ReplayFileDesktopTest 在显示中导入至槽8并再导出逐字段比较，没有走空环境变量的 fixture 回退。ReplayFileExchangeTest 33项、ReplayExchangeContractTest 4项、ReplayFileDesktopTest 2项均包含在617内，不另加总。原默认文件套件以提交中的 desktop-saved-v3.json 为输入，不能冒称读入了本轮桌面输出。全量 Java 实际新生成 `build/web035/desktop-actual.json` 后，另做独立单项浏览器探针：复用现有首项严格用例，只将输入换为此文件、输出写往 build/wf002，保留原 rawRecords/daySnapshots/整份导出相等、活局快照/存储/写请求不变断言；临时 spec 在 finally 删除。探针1/1通过（6.9s）；脚本和日志保留在忽略目录，不修改提交中的冻结样本或默认套件。截图 desktop-real-save.png / desktop-import-slot8.png 留存，并查看导入槽8画面，CJK正常。正式字体/DPI/物理设备验收仍未完成。

FatJAR `build/libs/vocr-1.0.3.4-all.jar`：724197819字节，SHA256 `2e9a59594169287ba97efb14a055fa62d16eaf0ac4dc6d82fe9925347a0af57c`；检查不含 com/vocr/web、Javalin、Jetty服务类。干净LF输入的JAR/前端字节与旧脏树记录不同，记录实际值，不沿用旧hash；不是逐字节重建可复现证明。

元数据采集最初调用 Pillow7 不存在的 features.version 接口失败，仅修正采集方式为 _webp.WebPDecoderVersion；产品测试未失败，不因此重跑无关门。首次缺 LFS 与认证失败属于输入/环境阻断，补齐后再给远端通过结论；不记成产品断言失败。默认浏览器和本轮全量无重试或放宽断言。

功能门 `npm --prefix web-client run test:gate`：15/15（3.9m，retries=0），使用本轮全量生成的15份oracle。启动前 `--require-oracle` 已核对全部精确文件名及9ffa…指纹；未设置服务器覆盖，保留 release-baseline。运行中只读核实自身 SeededGuestGameServer 的 oom_score_adj=500，未用shim或改监护逻辑。

## 证据和后续门槛

简明结构化记录见 [WF-002证据](workflow-wf002-evidence.json)，完整日志/XML/运行时文件哈希留在校准树 build/wf002。它是本地校准记录，不是 GitHub CI run；没有 run id 或 Actions 成功声明。WF-005 再建立通用证据生成/归档入口，本轮不假装已完成。

not_covered/manual_gates：A06/A07/A08正式放行、046浏览器/DPI/辅助技术/音频听审和物理设备/循环接缝、049发布前人工复核、047容量与超长局均未覆盖。单次全绿不是无flake证明；尚未证明从全新操作系统镜像自动安装全部运行时，也没有做逐字节可复现构建。此环境支持下一步WF-003统一验证入口与机器可读分级；WF-004才设计/启用Actions，推送分支/设置/AI自动修复仍须具体授权。WEB-036及以后继续暂停。

最终 require-clean/require-lfs/require-oracle 与本地 fsck 全通过，Java/冻结/输入/政策指纹前后相同；Xvfb父进程收到stop后已回收自身子进程。原工作区保护按初始137条porcelain及原件hash另行核对，未将其CRLF/私人差异纳入本批。

WF-003评审澄清：git-lfs checkout进度曾显示34/34；该进度计数的来源未独立追踪，不用于树内对象计数。当前提交树始终按32条pointer路径/31个唯一OID核验。外部报告观察到LF/CRLF环境JAR相差415字节、冷大厅相差1字节，但未隔离全部环境变量，不据此认定EOL是唯一原因；发布哈希仍只采用干净LF检出。远端对象账本与可携带OID集合见[远端状态](workflow-remote-state.json)。
