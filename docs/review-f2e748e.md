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
