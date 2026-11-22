# WEB-050 框体交互与说明/战绩场景 Implementation Plan

> **For agentic workers:** 按任务逐项实施，由当前Coder直接执行；用户已明确要求现在处理，无新增范围确认环节。

**Goal:** 消除游戏区域原生误选造成的吞点击，将Info/Record主入口接入01的桌面场景，归档假猎人调查供以后继续。

**Architecture:** 不改公共API、规则、IDB或录像；RecordPage增加核心场景展示分支以复用既有读取/聚合，说明核心场景复用helpGroups文本。01内限制原生选择/图片拖动，明确保留指示工具的游戏拖拽与外围可复制诊断。

**Tech Stack:** Vue3、现有图片manifest、IndexedDB只读聚合、Playwright单worker、桌面Java源码与已有基线。

**Spec:** 用户2026-11-02四项反馈及“第四项不构成问题”、本轮“现在处理所有问题，猎人逻辑暂放但保存查证”。源：InfoSceneHandler、RecordSceneHandler、ReplayFileDialogs、GameStage。

## Global Constraints
- 基线4497368，PR #13仍草稿；不合流、不进入044，不改猎人/任何生产Java或规则。
- 不改变02等辅助区阶段显隐条件；Replay文件按钮已实现，补使用指引，不删功能或伪造试玩已通过。
- 不提高资源预算、放宽超时或刷新黄金；不改存储/交换格式，不把说明摘要冒充桌面日文原文。
- 单worker/JVM，MemAvailable至少2GiB；优先无JVM mock验证。真实HTTP默认/gate按最终风险补验，不启动并行JVM、不关闭用户试玩或其他程序。
- 本机完整全量/打包与扩大范围发布须按流程另获当轮明确授权；本轮先完成分级允许的实现/验证。

## Review Focus
- 连点、拖动选择、外侧残留选区不能吞框体操作；补全不请求，推进仅一次；保留键盘及滚动。
- 指示工具仍可拖拽，表单仍可编辑、外围诊断可复制；不全页preventDefault、不屏蔽所有右键。
- 战绩只读，错误不显示零或旧值；总合无当前连胜，配役页显示最大/当前，精度不变。
- 菜单内Info/Record不创建/释放/推进活局，返回路径/焦点/BGM与原版一致。
- 既有帮助每个主题、原文与Web补充仍可达，窄窗口内容/最后按钮可达，A06/A07不提前通过。

### Task 1: 归档猎人查证
**Files:** 新增docs/investigation-fake-hunter-diary.md；开发任务清单.md仅登记延期入口。
- [x] 写明3b15026原50/50→单死体方案1权重，非硬排除；a0cf9af while→if改变break/continue作用域；7b97fb8前置写日记、空死体输入、旧路径丢弃返回值。
- [x] 固定原始/变化提交完整SHA、源码符号、调查命令、截图事实和未复算随机过程；明确未修复、待用户另行决定。
- [x] 核对git对象与当前源码，不启动JVM或修改游戏。

### Task 2: 消除游戏区域误选
**Files:** web-client/src/GameStage.vue、AssetImage.vue，新增desktop-interaction.css；tests/desktop-info-record.spec.js。
- [x] 先加浏览器原生鼠标连点/拖动选择、外侧已选文字、图片不可拖、显式工具可拖及输入可编辑断言。
- [x] 01核心画布user-select:none；素材img draggable=false；保留draggable=true工具；readClick不因页面其他选区吞动作，不拦截真实表单/链接。
- [x] 无JVM真实浏览器先验失败再实现复验；对话/音频/指示业务相关回归最终补验。

### Task 3: 战绩核心场景
**Files:** RecordPage.vue、App.vue，新增desktop-information.css；tests/desktop-info-record.spec.js、statistics.spec.js。
**Interfaces:** RecordPage({desktop:Boolean})仍从createRecordStore().read()读取，emit close；recordPage选择/刷新/错误不变；核心总览/配役翻页与next/return/title在01内完成。
- [x] 固定脚本手算统计沿用，新增源坐标/图片与只读/返回/焦点、空库/坏库断言。
- [x] title(30,20,200,60)，labels(35,120+115r,185,38)，values(43,175+115r)，右侧next(1050,250,194,126)、return(1050,400)、title(1050,550)；配役max/Now同源格式。浏览器刷新/范围说明放画布外。
- [x] 保留旧错误分类、逐次读取版本守卫、IDB不写入；所有既有统计期望保留。

### Task 4: Info核心场景与导航
**Files:** 新增DesktopHelpScene.vue；App.vue、desktop-information.css；tests/desktop-info-record.spec.js、desktop-scene-loop.spec.js、help.spec.js。
**Interfaces:** DesktopHelpScene emit close；通过helpGroups提供既有日文摘要，层级root→group→topic，返回上层/标题；Web补充和原文在核心外可达。
- [x] root五分类按钮(80,10+70i,220,50)，group主题同坐标宽222；背景PVBG、面板(10,10,980,660)、topic(50,50,880,630)、return(1050,400,194,127)、title(1050,560,194,127)；保留源码按钮声音分支。
- [x] App主菜单与侧栏入口都切到01对应核心场景；记录进入前scene，局内返回不会销毁会话；标题返回按桌面进入title。无额外API请求/隐式写入。
- [x] 既有原文/配役清单仍可作为画布外补充，不在主菜单核心显示旧白色弹窗；菜单BGM INFO/START_MENU切换与Record源分支核对。

### Task 5: 最终验收和交接
- [x] 前端单测、build、native相关浏览器/默认、assets/audio和受影响完整gate按最终输入串行验证；需要JVM先检查内存与已有试玩，不能擅停用户程序。
- [x] 同数据实际Swing/Web截图、坐标与动作证据；不以参考图替代真实渲染，不假称跨字体/DPI一致。
- [x] 更新场景矩阵/核心记录和候选范围，记录失败与修复；代码全验后按主题提交。
- [ ] 最终新head云端CI/产物发布、用户完整一局及CR按流程，不拿4497368旧候选当新交付。

## Self-review
范围覆盖误选、Info/Record和可续接猎人归档；辅助区显隐保持，文件交换不重写。所有涉及数据的修改局限只读展示，不改AI策略。已准备无JVM测试入口以避免并行启动服务。

## 实施结果
本机分级验收及源码实画面对照完成，结果与失败处置集中在web-desktop-core-window.md末节。默认249先通过；Replay交叉反例补修后54相关通过；最后原版20号分类/可见标题/系统回退校准后16相关及build/assets5再通过。单测224、音频30、gate15均通过。新候选云端及用户完整一局尚待，不把所有不同输入日志拼成full结果。
