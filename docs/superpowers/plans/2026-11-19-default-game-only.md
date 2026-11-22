# 默认纯游戏入口 Implementation Plan

> **For agentic workers:** 按任务逐项实施；本轮由主Agent在现有会话执行，用户已批准规则及开发，不另开执行方式确认。

**Goal:** 默认呈现既有纯游戏窗口，同时保留主动Esc恢复外围和原生弹窗优先取消。

**Architecture:** 复用App显示状态和既有game-only CSS；仅修改初值、首次缩放及Esc路由。旧业务测试使用明确的辅助外壳前提，真实默认路径由game-only专用测试覆盖。

**Tech Stack:** Vue 3、现有CSS、Playwright、Node测试。

**Spec:** ../specs/2026-11-19-default-game-only.md

## Global Constraints

不改Java/规则/API/存储格式/依赖/预算；单worker、JVM可用内存2GiB门槛；不跑本机桌面全量；不自动推送、合流或部署。

## Review Focus

- 首帧及刷新：立即按视口缩放，不闪现外壳。
- Esc长按：取消弹窗后重复keydown不恢复外围。
- 已打开Info/Record/Replay：无原生确认时Esc仅切显示，不双重导航。
- 网络故障及声音启用：仅画布内恢复，不自动显示外壳。
- 旧多标签回归：每个使用外壳的标签明确恢复外壳，不放宽原断言。

### Task 1: 默认状态、Esc和回归（一个产品语义提交）

**Files:** App.vue；game-only.spec.js；test-fixtures/shell-test.js及需外围前提的测试导入；当前显示/迁移/任务文档。

- [x] 修改专用用例，断言首次/刷新默认纯窗口、模式跨场景保持、原生弹窗取消及Esc重复键。
- [x] 运行定向场景，保留实施前失败证据。
- [x] App默认gameOnly=true，首次渲染前计算比例；原生dialog优先取消，单次Esc不同时恢复外围。
- [x] 旧辅助面板场景显式使用shell-test前提，保留原业务/几何/网络/游标断言。多标签用例同样显式恢复。
- [x] 运行前端单测、生产构建、定向浏览器及默认浏览器回归，并按App入口路径策略生成/核对同源oracle、运行15项gate，检查diff/文档链接；内存或端口不足按项目约定处理，不结束其他应用。
- [x] 更新文档与用户已通过的fe64局部验收；通过后仅提交本批范围并汇报。Windows只读时保留bundle并报告，不覆盖用户数据。

分级结果与已保留失败见build/web050/default-game-only/verification.json；本地完成后提交。本轮云端及Windows同步仍未完成，不以本机结果替代。
