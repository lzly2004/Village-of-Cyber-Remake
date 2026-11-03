# WEB-043 Localhost Hosting Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> 用户已明确“按Caddy推荐直接实施，细节由你定”。保留本会话Coder串行实施→用户转独立CR的既有执行方式，不自动委派，不增加设计/计划审批；未安装的执行技能不作前置。

**Goal:** 用固定Caddy在localhost实际提供构建后的静态页面、同源API代理与HTTPS，验证origin隔离，为未来公网托管保留明确迁移边界。

**Architecture:** Caddy与Java分开运行，均绑定127.0.0.1；代理只暴露frontend与/api，不暴露backend/config/metadata。Python标准库生成受约束JSON配置/本地测试证书并核对固定工具；Playwright用真实后端/代理做定向验证，验证入口纳入full。042旧发布包检查保持兼容。

**Tech Stack:** Caddy2.11.7（官方发布归档及SHA固定）、现有Java21/Gradle/Python/OpenSSL/Vue/Playwright；无生产应用依赖改动。

**Spec:** 开发任务清单WEB-043；docs/web-release-acceptance.md的origin隔离；用户2026-10-28选择localhost+Caddy，不购买/部署公网。

## Global Constraints
- 不购域名/云主机，不做公网或Tunnel，不改系统信任/hosts/防火墙/服务，不用sudo，所有监听回环高端口。
- 现有Java安全入口、单worker、2GiB门槛不变；本机full/check/打包另获本轮授权，发布/草稿PR亦另行申请。
- 不改规则/随机/协议/Replay/IDB格式，不迁移、清空用户数据；原脏树137porcelain/142展开/136SHA保全。
- 020预算不放宽。HTTPS是本地测试证书，不等于公网CA、DNS或正式浏览器信任验收。
- 不自动重试代理写请求；应用JSON状态/Retry-After原样传递，代理错误保持非业务诊断，不伪装成已知执行前拒绝。
- 043收口同源路由/证书与代理边界；044日志/健康/告警、047容量、050视觉语言、046/049人工门仍开放。

## Review Focus
1. API未知路径/非2xx不得回退SPA；配置、源码、后端与路径越界不能被静态服务读取。
2. 不可信Host/Forwarded头不能变成代理目标或身份；真实应用429/Location/Cache-Control需保持，代理502不能冒充安全重试。
3. 主机名、协议、端口分别改变时旧凭据/IDB/偏好不自动迁移，回旧origin数据仍在；协议对照不能同时改变端口来掩盖遗漏。
4. 本地私钥不入Git、发布ZIP或CI证据；TLS客户端须验证证书名/信任失败，浏览器仅隔离测试profile固定SPKI，不设ignoreHTTPSErrors或全局忽略证书。
5. 固定工具缓存篡改、配置注入/软链、未知文件、错误body/失联上游均明确拒绝，日志不能新增完整能力路径；不将本地验收当公网放行。

## Tasks

### 1. 042归档与配置生成器
**Files:** docs/review-c0ef2b8.md、开发任务清单.md；tools/web_hosting.py、tools/host-web.py、tools/hosting-toolchain.json、tools/tests/test_web_hosting.py。
**Interfaces:** `configuration(frontend, cert, key, https_ports, http_port, upstream_port)`产生限定回环的Caddy JSON；`prepare(...)`在新目录生成配置/本地证书，不安装系统信任；`checked_caddy(path)`拒绝未知二进制。
- [x] 归档042首审/主线核验与P3处置；纠正CR receipt id笔误，区分CR独立实跑与未运行download.py，42标已合流5ffc627。
- [x] 先写配置负例/工具校验/路径与监听限制测试，验证红灯，再实现固定安装、配置及CLI；Caddy先validate再run，不允许admin/任意上游/公网绑定。
- [x] 静态仅GET/HEAD及允许文件；API先路由、保留方法/正文/查询/响应；拒未知Host，丢弃不可信转发头，无透明重试，配置日志默认不输出请求。

### 2. 真实代理/TLS/存储验证
**Files:** tools/hosting-smoke.py、tools/playwright.hosting.config.mjs、web-client/hosting-tests/*。
**Interfaces:** smoke准备build/hosting-private临时目录和只包含公开指纹的plan；Playwright经安全入口启动runWeb，Caddy实际服务dist；证据输出build/verify，私钥仅在外部私有目录。
- [x] 新增实际TLS链/主机名验证，非法Host/路径/静态方法/不存在API、配置泄露负例；应用4xx/429与代理502分类、Retry-After/Location/no-store及安全头正例。
- [x] 重用真实无种子整局验证新入口；单独测试换主机、换TLS端口、同端口HTTP→HTTPS的会话/战绩/Replay隔离，返回旧origin仍保留，不靠清库伪造。
- [x] 证书失败/上游关闭、请求包含合成秘密的日志检查；不记真实会话ID到测试日志/trace；证书过期/真实公网/多用户留待正式验收。

### 3. 统一验证、CI与兼容
**Files:** tools/verify.py、tools/verify-tiers.json、tools/web_release.py、tools/ci/bootstrap.py、tools/hosting-toolchain.json、相关契约测试/CI缓存。
- [x] full增加hosting-tests与hosting-smoke（22→24），现有22项和包内smoke不删不替；CI固定Caddy版本/归档/二进制SHA，记录工具信息。
- [x] 新发布仅接受24项完整同次结果；读取旧042包保留22项声明兼容、旧证据1—5可精确复算；写入与读取的约束分开验证。
- [x] 契约单测、真实定向与本轮另获批准的full24/打包已通过；最终源码快照同源oracle/15gate通过，未推送或合流。独立CR须在获准发布后由用户转交，尚未发生。

### 4. 操作契约与交付
**Files:** docs/web-localhost-hosting.md、docs/web-build-release.md、README.md、验收/工作流文档及任务清单。
- [x] 说明生产代理部署顺序、仅本地证书、浏览器信任限制、切换付费域名的DNS/证书/origin影响、数据导出及战绩不自动搬迁。
- [x] runtime版本/依赖字段声明性与文件SHA/stamp/同次result信任根区分；run.cmd端口对齐归045，Windows原生仍待。
- [x] 原日志/失败/兼容验证与保全统一放build/web043，首审预备包列实际验证与未验事项；发布及合流仍另获具体授权。
