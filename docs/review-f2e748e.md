# f2e748e 复评与 WF-004 处置

WF-003获接受，无阻断。reviewer独立复现计划器18组路径、缺git诊断和交换冒烟1/1，并重算指纹及远端账本集合；未执行入口26项/审计14项或完整执行链，不把方法计数当测试通过。Java/冻结/输入及LFS仍未变。

## 前置补修

- P2-1：收窄gate触发到明确的共享会话/人物/舞台路径，另保留gate用例/配置触发；帮助、音频、Replay、独立style/labels继续默认前端回归，不固定追加gate。未仅因gate直接import characterDisplay就忽略它通过浏览器验证的其他共享流程。App.vue内窄文案修改仍保守触发，路径规则无法证明语义范围，已明示成本取舍。
- P2-2：账本补精确定义：OID按ASCII升序，`oid + TAB + 十进制bytes + LF`拼接，最后也有LF，以ASCII编码取SHA-256。原列表/数值不变，重新按定义核对。
- P3-3：交换spec/config改动直接分级full，包含当次浏览器文件、java-full、XML/桌面产物与desktop-exchange；没有只加一个缺前置产物的孤立suite。
- P3-4：不存在或非法base均明确INVALID_BASE_REF。
- P3-5：真实Git差异为空即NO_CHANGES_TO_VERIFY，--paths不能伪装实际差异。显式--full可同SHA重新完整验收，仍需独立授权，为以后稳定性观察保留入口。
- 零skip契约：新增跳过须同步改契约并评审，不静默放行。

实测 `python3 -B tools/tests/test_verify.py -v`：30/30（1.777s），新增四项覆盖取舍/交换前置/无效base/空差异；原26项保持。未运行Java、浏览器或打包，原入口执行命令/监护未变，不因只增拒绝与分级变化强加全量。具体日志原仓库build/wf004/review-fixes-tests.log。账本31/20对象聚合hash和字节重新校对通过；本轮没有远端对象写入。

## WF-004约束

单job保留browser-export/desktop-actual当次mtime契约，不跨job传递；原产物格式不加CI字段。PR使用真实base/head SHA，push使用before（新分支明确以default branch作基线）；先dry-run再执行。Action pin完整SHA、contents:read、取消同分支旧运行、45分钟预算、LFS缓存后仍校验实际内容、always上传现有result/log。候选/最终文件一致性脚本化，CI与授权/启用状态另见本轮工作流记录，不能把本地契约测试称为云端成功。

## WF-004本地配置实测（远端待授权）

- CI辅助契约 `python3 -B tools/tests/test_ci_workflow.py -v`：24/24（0.023s）。覆盖PR真实base/head、push.before、新分支default基线、非法SHA/错误检出/删除事件、缺base不回退、显式full、权限/单job/时序/缓存/Action pin、下载缓存哈希/坏下载清理/ZIP越界/LFS二进制、plan/result/HEAD不符，以及CI wrapper实际调用一次并传播失败、不把无效输出标绿。全部临时数据/受控runner，无网络/JVM。
- 最终入口30/30（1.816s）、原审计14/14（0.773s）；新增ci-tests被full策略强制包含，其独立用例改动在tooling层追加执行。未改原监护或启动器。
- actionlint1.7.12（官方归档SHA256 8aca8db96f1b94770f1b0d72b6dddcb1ebb8123cb3712530b08cc387b349a3d8）静态校验通过，命令 `actionlint -shellcheck= .github/workflows/verify.yml`；未把这项称为云端运行。Python AST/JSON与相关差异检查通过。
- 首次YAML解析发现plain scalar中的`--only-binary=:all:`与YAML冒号语法冲突，改用block scalar后24项通过；该失败在collect前，不记作已执行测试数，也不归因环境偶发。
- 复用本聊天干净LF隔离树，未改用户目录；基线b38da1e，仅加入本轮9个代码/配置文件形成候选 3cc658c4753f3308ba126774727660c3c0a80561。真实入口`--base b38da1e --dry-run --json`返回planned/full；未批准的实际调用返回FULL_JAVA_PACKAGE_APPROVAL_REQUIRED，未创建新运行目录/启动套件。同候选CI契约再24/24。当前9文件与候选逐字节相同，聚合SHA256 `cce745816294a3bf9cfd60678be21342342dddca5f6629ed97fe484001a737c7`（算法按UTF8路径升序，逐项UTF8(path)+NUL+ASCII(content SHA256)+LF）。本轮完整Java/FatJAR/default/gate未运行，不能用旧WF-002结果冒充。
- CI工具选择精确版本并固定哈希；官方Microsoft安装manifest没有宿主21.0.12.1，明确选择21.0.11，新的完整结果仍待临时runner验证；未安装本机系统包/改配置。GitHub临时runner上的依赖安装和CI全量范围包含在待批准方案中。
- 同步区间检查：旧远端main6163620到本地包含历史尾随空白，严格检查会拒绝，未改旧Java或放宽diff-check。提出先推已审f2e748e准备基线、再推CI提交的两步方案；未来PR仍使用真实main base并报告旧问题。完整范围/触发/授权见workflow-ci.md。
- 远端只读：准备分支不存在，main仍6163620；历史额外LFS对象5864014字节从空目录实际下载并核对，32个历史唯一身份与当前31分开记录。新增对象上传0，Git refs推送0。

源码日志：build/wf004；候选dry-run与缺授权负例：隔离树build/wf004。Java/frozen/input指纹与已审基线一致，政策因新增CI工具改变；不声称候选整提交等于最终含文档提交。完成状态保持WF-004 DOING，只有具体推送/CI授权与实际云端结果具备后才转DONE。
