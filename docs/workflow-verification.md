# 统一验证入口（WF-003）

Linux 本地与未来 CI 使用同一入口 `bash tools/verify.sh`。只验证脚本所在仓库，不能用外部脚本替另一棵树宣称 tested SHA。运行前自行准备工具/缓存/LFS和必要显示；入口不安装、fetch、上传、改配置或推送，不刷新黄金。

## 调用

```bash
# 对实际提交差异规划；不会启动套件或写入证据目录。
bash tools/verify.sh --base <上次评审SHA> --dry-run --json

# 执行自动分级；默认base为HEAD^，首次提交或跨多提交必须指定base。
bash tools/verify.sh --base <上次评审SHA> --json

# 手工路径只能增加触发；真实Git差异仍全部参与，不能借此降低分级。
bash tools/verify.sh --base <上次评审SHA> --paths web-client/src/App.vue --dry-run --json

# 只升级完整计划，不授予Java/打包权限。
bash tools/verify.sh --base <上次评审SHA> --full --dry-run --json
```

`--base` 是提交差异起点，不是工作树替代版本。删除/重命名按禁用rename折叠的Git diff列出旧/新路径；未匹配路径强制full，非法路径/引用明确拒绝。没有`--suite`选择、跳过或脏树执行例外。`--paths`不会从源码文本推断风险。

首道门调用现有 audit-ci-inputs.py API，要求Git干净、每个跟踪文件与HEAD原始字节相同、Java ref/worktree指纹相等；不把CRLF只分类当作相等。局部Git转换策略须为false/lf或Linux未设置的等效默认，Java原始字节也须无CRLF。设置不符合时拒绝，不自动修复。LFS按所选套件需要校验实际payload，缺失时连dry-run也拒绝；干净指针不等于可运行音频。审计JSON直接进入结果，不另造来源指纹。

## 分级及顺序

机器可读规则在 `tools/verify-tiers.json`，argv定义在 `tools/verify.py`，没有由路径拼接shell命令。

| 分级 | 典型触发 | 范围 |
|---|---|---|
| docs | 文档/任务索引 | 提交差异空白检查及输入前后审计；内容事实与链接仍须人工核对 |
| tooling | 已明确列出的验证工具/测试配置 | 审计工具与入口测试；资源工具/监护/Node测试按相关路径追加 |
| frontend | 前端功能/浏览器测试 | 单测、build资源门、assets/audio、文件重复3次、默认回归；明确列出的共享会话/人物/舞台路径另加定向oracle生成与gate |
| full | Java/资源/概率/共享构建或未知路径 | 工具/资源/前端/兼容/生产runWeb无种子冒烟/完整Java与FatJAR/oracle/gate及桌面文件反向网页交换 |

只认最保守的匹配分级，附加套件取并集。所选/未选套件和`not_triggered_by_changed_paths`理由均进JSON；冻结与输入变动另外列为`sensitive_paths_requiring_review`，只报告，不自动授权更改。`AGENTS.md`/未知工具默认full；帮助/音频/Replay/独立样式由默认回归覆盖，不固定跑gate；App.vue等关键文件内即便只改一行文案，路径级判定仍保守追加gate。交换spec/config自身改动分级full，确保执行它并生成当次前置产物。未来细化规则须有正负例，不靠排除未知文件降级。

Java始终调用原 `bash tools/run-gradle-safe.sh`，单worker与2GiB门槛保持。未修改进程监护/清理，不增加独立的kill/超时重试逻辑。每个入口调用串行执行套件；不要并行运行多个入口或与其他JVM/浏览器验收叠加。

完整执行先检查本轮用户授权对应的 `VOCR_APPROVED_FULL_CHECK=1`，再检查DISPLAY；工具不会自行设置批准变量，不复用旧轮授权。需要时应先按AGENTS向用户申请具体完整命令；CI预授权范围及启用状态见[WF-004约定](workflow-ci.md)，未获授权不推送启用。带JVM浏览器套件拒绝`VOCR_WEB_SERVER_COMMAND`覆盖，gate直接调用原`test:gate`，保留release-baseline，不接入缩减服务器。

完整路径固定为：

1. 工具/前端与资源检查，文件用例三次重复、默认浏览器实际下载browser-export.json。
2. 要求该文件在本次入口开始后生成、在仓库内且不是符号链接；缺失/旧文件直接失败。
3. 显式设置该文件绝对路径给`VOCR_WEB035_BROWSER_EXPORT`，执行`fatJar --offline --rerun-tasks`。要求提供既有Xvfb/真实显示+CJK字体。离线依赖须提前准备；不会安装或降级依赖。完整资源工具测试还需要requirements中的固定生成器依赖，Pillow7只适用日常check/distribution，不能冒充转码工具链。
4. 从当次XML解析非零tests且failures/errors/skipped均为0；要求当次生成desktop-actual.json。`--rerun-tasks`避免旧XML或未注册Gradle输出被up-to-date当作本次执行。
5. 审计全部15份oracle精确文件名和最终Java指纹，再执行gate。纯前端共享改动则先定向`Web017JourneyTest --offline --rerun-tasks`，无需将它升级为桌面全量。
6. 使用独立exchange配置复用默认e2e服务器/工作目录/单worker/retries=0，实际读本次desktop-actual.json，导入/播放/下载逐字段比较，并断言活局不变。缺环境变量或文件时拒绝，无fixture回退。
7. 最终再次审计，拒绝HEAD漂移、任何跟踪文件变化、输入/冻结/政策指纹变化；不自动回写或恢复它们。

## 结果与失败

正常执行仅在Git忽略且不越界的`build/verify/<UTC时间>-<随机ID>/`写日志和result.json。`--json`的stdout为最终JSON，子命令输出进入各suite日志；log是run相对路径，含hash/tail、exit_code和耗时。XML有明确来源；其他套件此阶段只记录进程退出状态，不虚构测试数量。WF-005以独立evidence.json解析数量、关联CI artifact/run，不改变本result格式；契约与状态见[证据交接](workflow-evidence.md)。

返回0：执行的所选套件与最终审计通过，或dry-run的准入/计划成功（`status=planned`，不代表测试通过）。返回1：套件或最终输入审计失败；首个失败进`first_failure`并立即停止，后续不执行、不自动重试。返回2：准入/参数/授权/工具问题，stderr为JSON，不创建运行目录。缺git保留TOOL_MISSING诊断。正常中断仍须按AGENTS检查本次工具链退出状态，不能把超时/中断计作通过。

manual_gates保持not_covered：A06/A07/A08、046正式浏览器/DPI/辅助技术/听审与物理设备/循环接缝、049人工发布复核、047容量与长局。本地自动化通过不是发布放行、远端LFS证明或完整CI工作流已启用。

本轮具体命令、发现与最终实测集中见[19339f4复评处置](review-19339f4.md)。

WF-004前置补修：无效base统一报INVALID_BASE_REF；真实Git差异为空时即使附加--paths也报NO_CHANGES_TO_VERIFY。仅显式--full允许同SHA重新验收（仍需完整授权），用于以后稳定性观察。新增跳过测试须同时修改并评审XML零skip契约，不能静默接受。远端账本聚合算法的TAB/LF定义已写入JSON，不能按空格或JSON序列化猜测。

CI配置及CI辅助工具变动默认为full，包含ci-tests（事件SHA/缓存/权限/命令/失败等纯工具契约）。只有ci-tests用例本身的独立改动可在tooling层追加该套件，所需PyYAML为开发依赖，不启动JVM。

WEB-038增加独立production-smoke到full计划（21项，含内部检查），在默认浏览器退出后、Java全量开始前串行运行；配置拒绝种子服务器override，使用本轮生产构建。截图位于build/verify/production-browser，被现有CI证据收集覆盖。该套件不自动加入docs/tooling/frontend：控制每次前端改动的验收成本，代价是仅生产preview入口出现的回归可能到下一次full才暴露；这是有意的覆盖取舍，不表示frontend已经覆盖生产入口。该套件不替代七配役确定性门或人工门槛，详见[038记录](web-038-verification.md)。

WEB-042将full从21项增为22项，追加release-tests；java-full在同一安全入口执行`fatJar prepareWebRuntime --offline --rerun-tasks`。成功web-build绑定全部dist文件大小/SHA，成功java-full绑定运行时stamp。统一发布编排只消费同次full结果，随后校验/解包并实际运行发布包；CI full自动追加，其他层明确not_built。包内冒烟不是22项中重复的一项，失败仍使CI失败，详见[发布包契约](web-build-release.md)。

WEB-043 将 full 从 22 增至 24 项，新增 hosting-tests 与 hosting-smoke，后者在原生产/兼容/桌面交换退出后串行启动实际 Caddy + runWeb。CI 按 tools/hosting-toolchain.json 固定归档及二进制 SHA，记录工具版本；证书仅在 build/hosting-private，artifact 仍只采集 build/verify 等既有公开证据目录。旧 042 的22项发布声明保持只读兼容，新组包必须包含同次24项结果；docs/tooling/frontend不自动增加实际托管测试。见 [043操作与限制](web-localhost-hosting.md)。
