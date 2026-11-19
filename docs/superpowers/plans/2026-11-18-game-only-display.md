# Game-only Display Implementation Plan

> **For agentic workers:** 按用户“现在就开始做”直接在本会话实施已确认的小批次；不再请求重复设计批准。

**Goal:** 外围按钮进入仅游戏窗口模式，Esc恢复，所有桌面场景可用。

**Architecture:** App保持现有组件树，用模式类隐藏外围并缩放原1280×720窗口；捕获Esc避免场景返回串触发。必要恢复/声音操作用窗口内临时状态层承接。

**Tech Stack:** 现有Vue 3、CSS、Playwright；不引入依赖。

**Spec:** docs/superpowers/specs/2026-11-18-game-only-display-design.md

## Global Constraints

前端批次；单worker、2GiB内存门槛；不运行桌面全量/完整打包，不推送。不卸载音频/会话，不写存储或发游戏命令来切换显示。提交后双端同步，保护Windows数据与无关修改。

## Review Focus

- Info/Record及模态Esc：首次仅恢复外围，场景/确认保持。
- 多种窗口比例：舞台保持16:9，完整可见无页面滚动。
- 有活局及音频：切换零命令，游标/声音状态不变。
- 连接不确定/存储错误：窗口内仍可读、可再同步，不自动重放写操作。
- Replay/文件交换：隐藏外围说明不隐藏画布或确认流程。

## Task 1：展示切换及验证

Files: App.vue、main.js、新game-only.css、新tests/game-only.spec.js；状态文档及任务清单。

- [x] 增加只读/交互浏览器断言：菜单→Info/Record/Replay、各尺寸几何、Esc单次恢复、音频挂载与活局游标不变、错误恢复。
- [x] App添加模式入口/退出、尺寸监听及窗口内必要状态；CSS隐藏外围，保留游戏画面与原生确认。
- [x] 运行前端单测、生产构建、相关场景；最终默认浏览器回归一次。不因本批重复桌面全量。
- [x] 保存完整网页/纯游戏两种截图；更新TODO与验证记录，diff检查、提交及Windows备份同步核对。
