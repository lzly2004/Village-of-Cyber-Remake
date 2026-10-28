# WEB-043：localhost HTTPS 与同源托管

## 范围与使用边界

用户批准先用 localhost 验证，采用 Caddy；付费公网域名以后再决定。本轮仅 Linux amd64 回环服务，不购买域名、不修改系统信任/hosts/防火墙、不监听公网、不部署。Java 21、Python 3、OpenSSL 和固定 Caddy 为运行/操作前提；浏览器界面及游戏规则不改。Q04/Q05、人工、容量和正式发布门不据此放行。

固定入口：`https://localhost:18443`；同一静态文件也监听 `https://localhost:18444`，用于端口隔离对照。HTTP `127.0.0.1:18080` 仅把 GET/HEAD 转到固定 HTTPS 根路径，其他方法拒绝，不把能力路径/查询放进 Location。服务全部绑定 `127.0.0.1`，用 IPv4 回环访问；后端独立监听 `127.0.0.1:17075`。没有 Caddy admin、配置自动持久化、自动证书管理、ACME 或自动安装根证书。TLS 只启用 HTTP/1.1、HTTP/2，不需要 HTTP/3 UDP 缓冲区配置。

## 从已核验 ZIP 准备

先核对交接包提供的 ZIP SHA-256、来源 SHA、验证产物及回执，再运行以下命令。工具的 ZIP 校验可证明包内一致性，**不能代替来自可信渠道的外部 digest/来源核验**。

```sh
python3 -B tools/host-web.py install-tool --directory build/toolchain/caddy-2.11.7
python3 -B tools/host-web.py prepare --archive /absolute/path/vocr-web.zip --output /absolute/path/new-private-hosting
```

`install-tool` 只在指定用户目录下载固定 Caddy 2.11.7，校验官方归档 SHA 与解出二进制 SHA；每次启动再校验二进制。平台锁见 `tools/hosting-toolchain.json`，不宣称签名验证或跨平台支持。来源为 [Caddy 官方发行](https://github.com/caddyserver/caddy/releases/tag/v2.11.7)。

`prepare` 拒绝已有输出、软链、在 `build/verify` 下放私钥；先完整检查发布包再解包到 `new-private-hosting/release`，配置和证书放 `new-private-hosting/host`。目录权限 0700、私钥 0600。生成仅两天有效的隔离 CA 与 localhost/IP 证书，显式 OpenSSL 配置避免系统默认扩展干扰，生成后验证证书链与 localhost 名称。不得将整个私有目录放入 Git、公开目录或 artifact。失败可能保留本次新建的私有目录，需检查后选新输出路径重做，不覆盖旧目录。

另一终端通过既有安全入口/已审核的发布包启动后端，端口指定 17075；发布包 `backend/run.sh` 启动方式见 [042 契约](web-build-release.md)，本机仍执行内存安全约定。随后启动 Caddy：

```sh
python3 -B tools/host-web.py serve --caddy build/toolchain/caddy-2.11.7/caddy --directory /absolute/path/new-private-hosting/host
```

`serve` 核对完整前端文件集合及 SHA、证书 SHA、生成配置精确内容，运行 Caddy validate 后前台启动。输入漂移明确拒绝，不启动第二个 Java、不设置全局环境。停止时终止自己启动的进程；后端重启会使原游客凭据失效。私有配置中的绝对路径不应搬移，需在新目录重新准备。

开发定向验证也可用 `prepare --frontend /absolute/path/web-client/dist --output /absolute/path/new-private-hosting`；这只绑定该目录快照，不代表完整发布 ZIP 的来源验证。

## TLS 证据与浏览器限制

普通浏览器尚未信任这个隔离 CA，可能显示证书警告；本轮不会替用户修改系统/浏览器证书信任，也不指导忽略任意证书。命令行正例可用 `curl --cacert <host>/ca.pem https://localhost:18443/`。需要用户日常浏览器正式信任或公网可信 HTTPS 时，另行确定证书与信任方案。

自动化 Node 客户端用该 CA 验链及名称，错误 CA 和错误名称必须失败；Chromium 仅在独立测试 profile 对本次临时证书使用 SPKI allowlist，`ignoreHTTPSErrors=false`。这证明限定测试条件下可用，**不是**系统已信任、普通浏览器零警告或公网证书验收。证书过期轮换仍须部署验收。

## 路由、安全与故障语义

- API `/api`、`/api/*` 先于静态路由，完整保留方法/正文/路径/查询；仅转至固定回环后端，不重写会话能力，不透明重试。后端 JSON 4xx/429、`Retry-After`、`Location` 保留。
- 删除客户端伪造的 Forwarded/X-Real-IP/X-Forwarded-*，后端 Host 固定为回环；它不是公网可信代理链或多用户 IP 限流方案。
- 静态仅 `/`、`/index.html`、`/assets/*` 的 GET/HEAD，无目录浏览与 SPA 兜底；未知 API 不会返回页面。前端目录拒绝额外文件/链接；后端 JAR、概率配置、metadata 和工具均不公开。
- 非 localhost/127.0.0.1 Host 返回 421；静态写方法 405。API 正文最多 16KiB。上游断开 502、过大正文 413 等代理拒绝用固定文本，客户端按既有未知失败/不确定语义处理，不能把代理拒绝误称应用 JSON 错误或安全重发成功。
- 静态/API 响应增加 no-store、no-referrer、nosniff、DENY 与同源 CSP；现有 Vue 内联样式需要 `style-src 'unsafe-inline'`，不声称完全消除 XSS。媒体/图片 Blob 保留，未增加跨域权限。
- 不启用访问日志，Caddy 默认运行日志 writer 为 discard，防止新增完整能力 URL/查询泄露；这是有意的可观测性缺口，044 再设计脱敏日志/健康诊断，不能宣称生产日志治理完成。现有 Java 固定诊断边界不变。

## origin 与后续公网域名

浏览器按**协议、主机名、端口**区分存储。分别验证 localhost→127.0.0.1、18443→18444、同 localhost:18500 的 HTTP→HTTPS；每次新 origin 无旧会话/战绩/Replay/音量偏好，返回旧 origin 数据仍在，未靠清库制造隔离。新 origin 不自动读取、迁移或删除旧数据。

更换付费域名之前：用户批准购买/账户操作，确定 DNS、公网绑定、可信证书与续期、生产代理/限流/日志和 047 容量；固定正式 origin 后再完成 046/049。Replay 可显式导出/导入，战绩当前无跨 origin 迁移入口，未导出数据不能靠服务回滚搬回。不得因 localhost 验收通过就自动切公网。

## 本轮验证记录

定向命令：设置本轮固定 `VOCR_CADDY` 后运行 `python3 -B tools/hosting-smoke.py`；内部经 `run-gradle-safe.sh runWeb`，单 worker/retries=0。不会设置全量批准变量。

- 内存中止记录：`build/web043/hosting-targeted-second.log`，安全入口因低于 2GiB 中止；用户释放内存并批准额外一次环境重试。安全门槛未降低。
- 获准重试 `hosting-targeted-authorized-retry.log`：7 项失败；空 `<ul>` 不可见被错误当作未加载，且系统 OpenSSL 配置与参数重复写 CA basicConstraints，Node 拒绝证书链。不是通过。
- 修正为可操作的列表加载按钮/列表存在断言，并使用独立证书配置+启动前验链；`hosting-targeted-certificate-fix.log` 为 6/7，剩余非法 HTTP Host 测试被 Node 自动用于 TLS SNI，握手提前失败。
- 显式保留 URL 的 TLS 名称，把非法 Host 与 SNI 负例分开；最终 `hosting-targeted-host-fix.log` **7/7（13.0s）**：三种 origin、真实 TLS/静态边界、受控上游故障、同入口真实无种子整局。生产逻辑无额外等待/重试，严格断言保留。
- 纯工具：hosting **14/14**、verify **35/35**、release **17/17**、CI **66/66**。实际 042 旧包仍通过校验/解包/配置 validate（`release-legacy-check.json`）；这不冒充 043 的新 full/发布包冒烟。

记录集中在 `build/web043`。用户另行批准本轮完整验收与打包后，隔离 LF 快照 `70bc72f6e27bcaf0c3fdfc979fc36dc25decc99d` 完成 **full24/24**：Java650/650（65份XML重算、0失败/错误/跳过）、前端200、默认180、gate15、assets4、audio27、files-repeat30、production1、desktop-exchange1、hosting7；工具guarded15/audit14/verify35/CI66/release17/hosting14/image17/audio-tool18/exchange3全部通过。完整test/check、特征轨迹、概率/运行时资源校验、FatJAR及运行时包成功；同源oracle全部匹配。无全量重试。

本机新运行 ZIP **86,044,676B**，SHA-256 `ca601eb8c49dddc1778e424b979acce6f590b61f783118165b381f952816852c`；绑定上述验证快照，不冒充随后交付提交或云端产物。解包真实整局冒烟 **1/1**、包内文件不变及独立check通过；新ZIP再经host-web prepare解包/配置validate通过。HTTPS整局是dist+runWeb，包内整局仍是原042的解包后端+preview，两项来源分开，不声称公网部署或普通浏览器信任已通过。

本机FatJAR724,197,819B / `2e9a59594169287ba97efb14a055fa62d16eaf0ac4dc6d82fe9925347a0af57c`，本轮实构建/计算，不与云端JDK产物hash混淆；Java输入指纹9809ec71、frozen6ce8aa54、inputs3d4c43fa保持042基线。新工具/配置进入build_policy指纹。旧revision4/5真实证据分别精确复算通过，不把兼容复算记为重跑旧套件。

安全入口实测本轮三个受监护JVM oom_score_adj均500；原工作区137porcelain/142展开/136文件SHA仍保持。完整结果与日志见 `build/web043/full-summary.json`、`local-evidence`、`local-evidence-sha256.json`；私钥位于独立忽略目录，证据不含私钥。尚未发布PR、未部署。人工语言/视觉/听审、正式浏览器/DPI、容量、付费域名与公开服务均未验。独立 CR 仍由用户转交首审包，不由 Coder 自评代替。
