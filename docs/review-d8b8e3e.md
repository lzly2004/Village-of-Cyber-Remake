# WEB-035复评处置（d8b8e3e）

范围4101819..d8b8e3e，实际29文件（报告写27）；本机开工137项原差异，不混用reviewer的142项。外部无阻断，617项Java/FatJAR、189单测、15项同源gate、静态4与相关音频2为reviewer对原提交的独立证据；其默认132/134、单文件5/8的失败保留，不用coder原134/134否定。reviewer未复现图片/音频构建门禁，choom shim评分未生效等环境边界不复用为本机结论。

## 逐项处置

- P-1采纳：原confirm只等待click，随后raw直接读库，确实未等待哈希/事务完成。拆分submit和成功态等待imported(page, expected)：先等候选预览消失，再严格等待期望总数及导入完成消息；重复导入总数不变也必须等完成，不能只查原有一行。配额失败路径只submit，等待明确错误后严格核对原库；无sleep、重试绿灯或放宽断言。
- 增加受控前提：暂停第一次真实crypto.subtle.digest，确认点击后按钮禁用、预览仍在、库仍为空；显式释放后等成功态，再逐字段核对库内容。证明click不是提交完成，不依赖机器速度。原三处读库均改为成功态之后，其他成功调用同步使用同一约定。
- P-2采纳：仅前端文案改为“未完（概要のみ・取り込み不可）”及明确不可播放，不新增未完播放器或入库能力；测试仍严格断言复选框/确认禁用、库为空。
- P-3部分采纳：README补现有人工救援路径和限制（副本提取可验证replay→原九槽/slotIndex→完整校验确认）；不承诺坏条目可恢复，不直接写IDB。辅助恢复工具登记REPLAY-RECOVERY-001 PROPOSED，本轮未实现。
- P-4接受现有最小范围：桌面单局导出、网页整库导出有意不对称；“单局”不等于“单向交换”。在038登记是否满足用户预期，不提前做多局桌面导出。

## 本批验证

本批无Java/规则/协议/文件格式/存储格式变化；按纯前端局部文案与测试分级：单测、生产构建、文件用例重复和最终默认浏览器回归，不复用上轮全量授权、不启动全量Java/FatJAR或gate。单worker，JVM仅默认浏览器服务经原安全入口串行启动，2GiB门槛保持。日志build/web035-review为忽略产物。提交前宿主机只读复查本轮JVM/Chromium/Vite/Playwright无残留；原137项差异的文件hash保持，仅暂存本批5文件，cached diff --check通过。

首次沙箱内单测报告gate-config.test.js失败（15文件通过/1失败，内部子进程断言未完整输出），后续build未执行；不改断言。相同源码转到已授权的宿主入口后189/189通过，保留unit-sandbox-failed.log，不将首次计为通过或臆断具体内部错误。

- `source build/web035/env.sh && npm --prefix web-client run test:unit`：189/189（2392ms），unit.log。
- `npm --prefix web-client run build`：图片/音频prebuild、postbuild门禁通过；代码580502字节/gzip157317，全站静态77753534字节，build.log。未重新编码或生成素材。
- `npm --prefix web-client run test:e2e -- tests/replay-files.spec.js --repeat-each=5`：9用例×5=45/45（37.2s），retries=0，files-repeat.log。
- `npm --prefix web-client run test:e2e`：最终默认135/135（2.7min），retries=0，default.log；已查看最终库截图，概要限制文案换行完整、操作无重叠。Java617/gate15/音频2/静态4是原提交及reviewer记录，不冒充本批重跑。

## 工作流建议的核验边界

2026-10-24只读GitHub API显示远端main为6163620（本地d8b8e3e的祖先，差152个提交），远端Actions工作流列表为空；当前源码提交尚需同步，本轮未push、未配置远端、未运行附件root恢复脚本。建议先核对提交历史/LFS与干净检出可复现性，再单独授权CI准备批次，不重排036等49步。

采纳“确定性CI、独立语义CR、结构化证据”，但不采纳“本地绿必然CI绿”的绝对承诺；目标应为同SHA/同输入/同套件/工具链可追溯，环境失败单列。oracle是临时对照，CI应按该次检出源码生成再核对；黄金摘要是既有期望，不能为消红自动更新。概率/配置/资源输入另留指纹与校验，不靠手写oracle状态声明代替。

先不开Auto-Fix。以后如试点，应单次生成草案、机械检查允许修改的路径/规模、独立只写PR发布步骤、固定失败SHA并确认非过期、独立复评，禁止自动合并和循环自修。OpenAI当前[非交互指南](https://learn.chatgpt.com/docs/non-interactive-mode)已将补丁生成与PR发布分开；[旧示例](https://developers.openai.com/cookbook/examples/codex/autofix-github-actions)有归档/凭据布局警告，不照搬。

Chat规划为复杂任务可选：基于SHA+路径/符号/摘录的事实包输出方案、验收及未决项；执行者仍须核对事实并能上报计划错误。只读reviewer不必额外负责每轮总结；机械事实由脚本提取，CR仍独立读源码/契约。是否省额度需试点量测总调用、返工和人工搬运成本，不保证Chat工具权限或免费额度。未在本轮启用任何AI/CI自动化。
