# WF-008 本机实施验收与交接

基线：main2698ca5；设计aa37aa5。仅本轮内部完整试玩工具，不实施044或050，不改变生产Java/规则/协议/Replay/IDB格式或预算。

## 来源与范围

完整共享构建受测LF快照cfc74dc04d6c5efc173f7c6dea42ae2977ad8a1d，独立临时Git提交仅用于提交前验收，并非交付提交。full以main2698ca5为base；同一最终快照的frontend计划以工具基础快照2de05ed11c9dafd0f70dd40a7afdce7ebdb42de8为base，仅增真实只读就绪探测回归，未伪造产品改动触发分层。两份结果分别保存，frontend不冒充full。

full维持24项。frontend独立preview声明javaFull=false；重新编译并绑定同head runtime stamp，使用实际解包的组合产物执行原生产无种子整局冒烟。工具包不包含运行时二进制、用户数据或私钥。

## 已保留的失败与修正

- 初次完整入口误用了系统Pillow7，图片工具17项失败，停止，未进入JVM。
- 一次环境重试改用已有Python3.12/Pillow12.3，但漏接PyYAML，ci-tests导入失败，仍未进入JVM。不是产品断言失败，也不记通过。
- 用户单独批准额外一次环境重试后，使用已有Python3.12.14/Pillow12.3/WebP及本地PyYAML5.3.1纯Python副本；不安装或更改系统。组合环境预检CI89/证据39/图片17/音频18/发布17/托管14通过。
- 成功入口预检发现Playwright配置使用相对guardian路径，而worker校验绝对归属路径；统一为本仓绝对路径后制作最终快照，不把此前快照当最终通过。
- 先前可见浏览器实测不能证明停止后存活；保留原限制，随后用独立会话与合作fixture验证服务中断后浏览器仍存活，明确该fixture不是图像/听审。

## 最终本机结果

- full24/24、frontend9/9均在同一最终LF快照完成，未降低原full门禁。Java XML65份650/650，概率/运行时资源/check/FatJAR通过；前端200、默认181、gate15、assets4、audio27、files-repeat30、production1、desktop-exchange1、hosting-smoke7；工具15/14/35/90/17/14/17/18及交换契约3。
- full正式ZIP实际解包生产整局1通过；试玩HTTP worker对该ZIP再完成整局1通过；独立frontend preview实际解包生产整局1通过，javaFull=false、full/preview双向拒绝。仅prepareWebRuntime依赖的生产编译/资源重跑，不把它记为第二次Java全量。
- 独立工具包可解压并以标准库Python显示CLI，无源码/Node依赖；外部Java/Caddy/浏览器仍须显式配置。完整CLI运行与旧043官方包兼容导入通过，不冒充本轮新候选已获云端来源确认。
- 真实可见Chrome在独立测试显示正常完成一局并重启同一候选两次，战绩1、Replay1和音量37/22/静音设置保留；服务停止后浏览器存活，正常关闭释放profile锁。协调器并发锁、活跃profile锁拒绝；合作fixture另验Ctrl+C/独立会话。测试CDP只在本地测试wrapper开启，正式启动器无调试端口。该项是自动化持久性证据，不是用户画面/听感验收，也不证明跨版迁移。
- 原NTFS136个文件hash逐一不变，默认porcelain137条状态字节完全一致（展开142项）；无检出覆盖。最终服务端口18500/17075已关闭。

详细原始日志、结果JSON、快照输入、逐文件等价证明、SHA清单、截图和进程证明集中在`build/wf008/`，不是云端CI。实际交付提交SHA见首审预备包；验证快照不是交付提交，后补文档不冒充新一轮全量。


## 同版重启自纠

扩展实测发现：服务已停止、connect=ECONNREFUSED，但两个端口仍处TCP TIME_WAIT（内核状态06/inode0），旧bind预检返回EADDRINUSE，造成误报PORT_IN_USE。补修前新增隔离回归确实失败；预检按真实服务的SO_REUSEADDR语义绑定后通过，并保留正在listen的占用者拒绝。实际服务仍须各自成功绑定，预检不承担原子预留，也不启用SO_REUSEPORT、换端口或结束占用者。工具契约90/90、真实同版立即重启通过；随后在新快照重做完整24/9及两类包，不用补修前的通过覆盖最终源码。

额外CDP验证脚本初版遗漏baseURL，曾在导航时失败；修正的是忽略目录内的测试脚本，生产UI和既有生产冒烟未改。所有失败记录保留。源码版本与包指纹以最终快照为准。

## Coder目视范围

已查看真实Chrome测试产生的战绩、Replay和完整页面截图：能实际显示并保留既有内部/横向滚动入口。当前Web外壳、整体页面滚动和终局内部总览仍是既有实现，不宣称等同桌面；050尚未实现。此目视不替代用户完整一局、独立CR（尤其不支持图像的模型）或A06/A07/A08/046/049。

## 开放门槛

云端CI、独立代码首审和用户对最终候选的正常完整一局尚未完成。用户的画面/声音/操作反馈与CR报告共同交给Coder，补修新head后重验、增量复评和整局复玩。发布/Ready/merge均未授权。A06/A07/A08/046/047/048/049、045跨版迁移、050外观与日文化、HTTPS/SAN/公网、fork及认证下载路径仍保持各自范围；HTTP工具不提前放行。

## 本轮完整快照产物摘要（非正式交付候选）

- full-package.stdout: vocr-web.zip / 86044681 B / SHA-256 `9368828bbd35ae317a6f384b23a1dc18adea50d97a4ee639d1e32947fe2cc18a`；实际解包冒烟1通过。
- preview-package.stdout: vocr-preview.zip / 86044062 B / SHA-256 `c5ecaf9dea69c895a295496a5ef12fd3fbe50f18884d9281918520ac9fad8477`；实际解包冒烟1通过。
- 本机FatJAR：724197819 B / SHA-256 `2e9a59594169287ba97efb14a055fa62d16eaf0ac4dc6d82fe9925347a0af57c`。它来自本机JDK，不冒充后续云端产物。
- Java/frozen/inputs指纹保持既有值；build_policy65文件SHA-256 `75a017260f5ddc5c51e08ab81eee923d7615a232903c9f25e99d5159dda140e4`，两份计划一致。

## 完整快照后的窄工具补修

跨候选锁检查在共享构建快照完成后补齐：同一资料根的旧窗口若未关闭，拒绝启动另一候选，防固定origin使旧页面误连新服务。改动仅playtest_store.py、playtest_runtime.py提示及test_playtest.py；新增用例在修复前确实失败，修复后工具91/91与真实浏览器锁/重启验证通过。Java、前端、共享构建、CI驱动/证据及preview校验器字节均与上述完整快照相同；按工具风险补验，没有把旧full的ci-tests90写成91。完整差异列在input_equivalence；最终交付head尚待云端full，不声明本机已对交付SHA重跑全部24/9。
