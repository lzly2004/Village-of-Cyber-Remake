# WEB-044：运行诊断、健康检查和本地告警

## 当前范围

本轮先实现代码和定向验证，未部署公网、未安装监控服务或定时任务，也没有发送任何外部通知。既有localhost托管和回环后端保持原监听边界；不启用原始访问日志。044不修改玩法、Replay/战绩、前端或声音。

## 健康检查

`GET /api/v1/health`：正常返回HTTP200及`{"status":"ok"}`；服务停止过程中或最近一次定时会话回收报错时返回HTTP503及`{"status":"unavailable"}`。下一次成功回收后恢复健康。停止后端口不再可连接同样视为故障。响应设置no-store，不包含版本、会话数、ID或游戏数据。

检查不创建、借用、清理或续期任何对局，仍受已有全局请求速率和在途限制；429表示本次检查不可确认就绪，不能当正常。它只覆盖HTTP响应及回收任务状态，不保证每局命令、所有静态素材、BGM或物理设备正常，也不代替047容量测试。

在仓库根目录按实际后端端口运行：

```sh
python3 -B tools/check-web-health.py --url http://127.0.0.1:7070/api/v1/health
```

已经按043启动隔离HTTPS托管时，可检查代理到后端整条连接；指定本次隔离CA，不禁用证书/主机名验证：

```sh
python3 -B tools/check-web-health.py --url https://localhost:18443/api/v1/health --ca-file /absolute/private-host/host/ca.pem
```

不要对试玩器端口18500作假设：现有内部试玩器是否代理此新路径需其下一候选单独验证，当前已发布包没有健康接口。检查命令不启动或重启服务。

## 告警接法与固定输出

| 退出码 | 输出 | 运维含义 |
|---|---|---|
| 0 | HEALTH_OK | 本次健康检查成功 |
| 1 | HEALTH_FAILED | 网络/TLS/超时、重定向、HTTP非200、响应不符；需要检查服务 |
| 2 | HEALTH_CHECK_REFUSED | 参数或本地CA配置不合法；先修正监控配置 |

建议未来由明确授权的服务监控每分钟调用一次，连续失败后提醒运维，恢复时解除告警。本轮只交付单次检查接口，不自动安装/启用定时器、不向第三方发消息、不自动重启/杀进程。公网域名和通知渠道须部署时另定，不能把本轮工具当作已运行的公网告警系统。

工具仅接受固定健康路径和localhost/127.0.0.1，无用户名、密码、查询或片段；不使用环境代理，不跟随重定向；TLS保持验证；正文最多256字节。`--timeout`允许1—30秒（默认5），是连接、TLS握手、请求发送、响应头及正文读取共享的总网络时限；本地CA配置加载不计入网络时限。localhost固定连接IPv4回环，TLS仍校验localhost名称，不经过DNS/环境代理。任何失败只给固定结果，不回显响应、URL或异常详情。

## 日志与终局降噪

WebDiagnostics继续只接受固定Event枚举和经校验的数字端口，不接受自由文本、请求、Throwable。错误输出示例：

```text
[HTTP] REQUEST_FAILED time=2026-11-20T00:00:00Z level=ERROR
```

错误包括REQUEST_FAILED、SERIALIZATION_FAILED、REAP_FAILED、START_CLEANUP_FAILED、START_FAILED；INVALID_PORT为WARN。时间为UTC。保留原固定事件前缀，方便现有运维匹配。不得记录完整URL、Location、正文、Cookie/Authorization、异常链或私有对局状态。

正常请求和终局不新增逐局日志。Web独立进程继续禁用DebugLogger；录制器仍生成Replay，桌面日志默认值不改。框架自身可能在启动时提示缺少SLF4J provider，该固定启动提示不属于私有对局日志；本轮没有为消除此提示引入日志依赖。Caddy原始运行日志仍discard，代理断线通过健康探针检测；本轮不声称已经治理任何第三方agent/provider或致命JVM错误的输出。

输出仍是前台stdout/stderr。本轮不新建持久日志目录或改系统日志配置；正式部署时应由服务管理器限制日志保留量，使用固定事件告警，禁止重新开启未经脱敏的访问日志。

## 验证与剩余门槛

具体命令、失败和结果集中在`build/web044/`。新增Java定向测试覆盖健康无会话副作用、限额、回收故障/恢复和真实种子终局保持安静且录像存在；既有HTTP/安全/生命周期测试继续执行。Python检查工具的测试纳入原hosting-tests套件，不新增full套件编号；已有托管配置仍验证不记录原始URL。

用户随后明确授权本轮本机最终验收：完整test/check693项、七配役特征轨迹、概率/运行时资源、FatJAR及Web运行时构建均通过，0失败/错误/跳过，黄金摘要未改。首次完整执行690通过/3项Swing用例因无显示失败，使用本轮一次环境重试建立自有Xvfb后完整693项通过；旧日志/XML保留，不将失败轮称为通过。新FatJAR独立输出在/tmp，旧发布FatJAR未覆盖，身份见build/web044/full-artifacts.json。前端/真实Caddy健康链路、Windows同步、云端产物及公网告警均不得冒充已验收。Data盘目前只读，未改Windows原工程或其存档/IDE。
