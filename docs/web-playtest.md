# WF-008：每轮完整自助试玩

状态：本机实现与分级验收已完成，云端CI、独立代码首审、用户最终候选整局试玩及合流仍待；本机验证范围与后置窄工具补修见[验收记录](web-playtest-verification.md)。见[批准设计](superpowers/specs/2026-10-28-wf008-playtest-design.md)和[实施计划](superpowers/plans/2026-10-28-wf008-playtest.md)。仅为内部验收工具，不是面向最终用户的本地Web发行产品。

## 每轮用户流程

1. 从Coder的同一交接入口下载固定SHA的候选、verify、receipt和source-metadata.json；首审包交独立CR，可以和自己试玩并行。
2. 首次配置已有Java21+、固定Caddy、可见Chromium/Google Chrome。解开工具包，在桌面终端执行`bash playtest.sh`；源码中的入口为`bash tools/playtest.sh`。不sudo、不改系统/信任、不保存PAT，不自动装工具。
3. 菜单导入候选，按官方GitHub运行页核对head/run/attempt及三个artifact的id/digest后输入CONFIRM。同目录SHA或任意metadata文件不能自证来源。校验包、证据逐字段复算与回执关联后原子导入；缺来源、限流或下载不完整均不当作通过。
4. 自选七配役，正常完整玩至少一局，可继续多局、查看历史；所有已实现功能均可用，不做片段演示/固定种子/假响应/代投/加速/跳日。声音需真实手势启用，使用实际设备听。终端独立，不向游戏窗口注入控制覆盖层或强制置顶，不遮挡050窗口。
5. 正常关闭试玩浏览器，并停止服务。Ctrl+C或另一个终端的stop只停止自有服务；关闭浏览器本身不等于停止服务。停止后活局失效，不能续局；已保存的本地历史是另一生命周期。
6. 创建反馈，填完整/中断、配役、画面/声音/操作结论及步骤，仅附主动选择的截图/录像。材料可能含隐藏身份或私有文件，分享前自行检查。将本人反馈和CR完整报告一起给Coder。

缓存候选可离线重复启动，不需要Coder、GitHub登录、Gradle、Node或源码构建。首次下载和来源确认不承诺零点击，认证下载由用户浏览器处理，不读取cookies。远端artifact现行保留14天，本机缓存不因远端过期自动删除。每轮整局不代表七配役/罕见分支全部人工覆盖，CI/CR也不能否定具体体验问题。

## 首次准备及命令

Linux首版；使用当前真实桌面DISPLAY/Wayland，不沿用已失效的构建Xvfb显示号。工具路径为真实可执行文件，非软链，必要时先用readlink -f查看。Caddy只接受[固定锁](../tools/hosting-toolchain.json)，复用web_hosting.checked_caddy；缺失时用户显式准备，启动不另开下载面：

```sh
python3 -B tools/host-web.py install-tool --directory /absolute/user/toolchain/caddy-2.11.7
bash tools/playtest.sh configure --java /absolute/jdk21/bin/java \
  --caddy /absolute/user/toolchain/caddy-2.11.7/caddy --browser /absolute/chrome/chrome
bash tools/playtest.sh
# 非交互入口：
bash tools/playtest.sh import --artifact release.zip --verify verify.zip --receipt receipt.zip \
  --metadata source-metadata.json --confirm-page
bash tools/playtest.sh list
bash tools/playtest.sh start 'full-<40位SHA>-<64位内层ZIP哈希>'
bash tools/playtest.sh status
bash tools/playtest.sh stop
bash tools/playtest.sh feedback 'full-<40位SHA>-<64位内层ZIP哈希>' --attach /absolute/chosen.png
```

独立工具解压目录的playtest.sh用法相同。默认数据根`~/.local/share/vocr-playtest`，可在子命令前传`--home /absolute/private-directory`用于开发隔离实验。source-metadata.json由Coder交付：schema1、repository、confirmation=user-confirmed-page、kind=full/preview，run和verify/receipt/package三份官方来源元数据，含head/status/conclusion/attempt与id/name/size/digest/expired/workflow_run。`--confirm-page`仅记录用户已核对来源，不冒充工具在线认证、签名或实时head新鲜度。

## 完整功能与桌面对应

下表是正常入口/边界，不代表本轮逐项人工通过。缺口必须报告，不能在试玩器中禁用绕过。

| 桌面实际功能 | Web正常入口与差异 |
|---|---|
| 七配役、逐事件到终局、连续开局 | 大厅/事件确认/新对局；无种子生产随机，不承诺同分配同结局 |
| 职业/玩家询问CO、回避、指定/自由/灰区投票 | 决策区全部控件；权威候补/生效时机不改 |
| 全体/个体占卜护卫预约/取消 | 技能预约区；Web控件适配不改变执行语义 |
| 公开报告、技能信息、票型历史 | 对局信息区；真实身份/未公开技能不得为调试扩入正式API |
| 立绘、死亡/处刑/结算、台词 | 游戏舞台；整体外观/字体/位置/日文化仍归050，A06/A07不提前通过 |
| BGM、音效、选曲和音量 | 音频面板；手势和后台停止保留，混音/物理输出/接缝归046/049 |
| Replay保存、切日、信息与技能历史 | 终局保存/Replay库及播放器；不自动读取桌面槽位 |
| 文件双向交换 | 导入、单局或整库导出；桌面当前单局导出范围沿已批准裁定 |
| 战绩、胜率、连胜 | 浏览器战绩页，不自动共享桌面data，不虚构战绩文件迁移入口 |
| 帮助、配役说明 | 菜单；原文/译文边界保持，剩余日文化050 |
| 继续/收集按钮 | 当前桌面StartSceneHandler监听为空，不凭按钮名杜撰已实现功能 |
| 刷新/重启 | 同标签刷新恢复；空闲30分钟/最长24小时/后端重启失效保持，非活局存档 |

当前Replay不保证恢复未完游戏或重执行全部随机决策。050仍须桌面样例、用户确认差异与正式复检，工具不提前声明全功能/视觉对等。

## 数据、端口和安全停止

- 候选键`full/preview-<sourceSHA>-<archiveSHA>`，不覆盖不同包；candidates只读核验。profiles/<key>/chromium及downloads同版持久保存，多个正常局累计，不用无痕假装持久。
- 跨版默认独立profile，不自动迁移/复制/删除，不能拿隔离当升级回滚保护证明；045另验。同一资料根内任一候选的profile锁（含遗留锁）存在时均拒绝启动/切版，防旧窗口误连新服务；不强杀或删锁；异常恢复前先确认浏览器状态、保留整个profile，不直接删数据库。首版无一键清库/坏账本恢复。浏览器驱逐风险仍在，重要Replay请导出。
- 同UID一个协调器，锁/socket位于$XDG_RUNTIME_DIR/vocr-playtest或/tmp/vocr-playtest-UID（0700）；磁盘PID不授予信号权限。异常强杀导致残留时不按PID文件清理。
- 18500前端、17075后端固定IPv4回环。043标准18443/18444/18080虽不占18500但共享17075仍互斥，测试plain-origin占18500也冲突；18501/18510/18511不是本工具监听。PORT_IN_USE不自动换端口或杀占用者。
- 就绪只读GET /api/v1/__playtest_ready__，严格404 JSON/HTTP_ERROR，结合自身Java监听、同源代理和index字节；不创建/推进用户对局，不新增044健康API。
- 服务子树受既有guarded-process保护，Java choom评分500、小堆和2GiB门槛，仅清自身未回收子进程。浏览器在该树之外且独立会话，Ctrl+C不得传播给浏览器。服务READY/浏览器进程启动不等于人工看到画面或听审通过。

## 候选和验收边界

full复用正式候选ZIP及check_release，full24不降级。frontend另产kind=vocr-playtest-preview/schema1、preview-manifest.json：runtime/dist/result同head，实际解包组合真实无种子整局冒烟，javaFull=false，不能被正式发布检查器接受。docs/tooling无运行变化时明确引用旧候选；影响启动/产物则重建验证。

revision6证据新增preview report/实际冒烟log/上传身份绑定，rev1—5按原规则复算。CI构建/冒烟/工具包/上传失败不交正常候选。工具包只含标准库脚本/检查器/锁，不含Java/浏览器/Caddy二进制、私钥、用户数据或缓存。

HTTP试玩不覆盖043 HTTPS、证书/SAN/信任、正式代理或公网。下载有外部流量，游玩占用本机资源并写专用浏览器数据，不能承诺零影响或OS级隔离；无公网监听、系统信任/防火墙修改，默认不录屏录音/上传/后台更新。A06/A07/A08/046/047/048/049及Q04/Q05保持各自门槛，日常一局不自动放行。
