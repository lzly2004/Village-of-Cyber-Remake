---
name: 电脑村游戏项目规则
description: 电脑村狼人杀游戏重制项目的核心业务规则、代码约定和开发规范

---

# 电脑村游戏项目规则

## 描述
本技能定义了电脑村狼人杀游戏重制项目的开发规范和业务规则，确保 AI 在修改代码时遵循项目约定，避免引入新的 bug。

## 使用场景
当涉及电脑村项目的代码修改、bug 修复、功能开发、重构优化时触发。

---

## 一、项目基本信息

### 技术栈
- **语言**: Java 21
- **构建工具**: Gradle
- **UI**: Swing (Java AWT)
- **数据库**: 本地文件存储（存档/复盘）

### 核心模块
- **GameContext**: 游戏状态管理
- **GameRecorder**: 游戏记录与快照
- **DaySnapshot**: 每日快照数据结构
- **ReplayPlayerHandler**: 复盘界面处理
- **VoteInfoRenderer**: 局内投票信息渲染
- **PlayerStatusRenderer**: 玩家状态渲染
- **UIHelpers**: UI 工具方法集合

---

## 二、关键业务规则

### 2.1 角色编号映射（Role.java）
角色编号必须严格对应 `Role.java` 定义：

| 编号 | 枚举值 | 中文名称 | 说明 |
|------|--------|----------|------|
| 0 | NONE | 无 | 默认值 |
| 1 | zhan | 占卜师 | 人类 |
| 2 | ling | 灵媒师 | 人类 |
| 3 | lie | 猎人 | 人类 |
| 4 | gong | 共有者 | 人类 |
| 5 | mao | 猫又 | 人类 |
| 6 | wu | 无co | 村民 |
| 7 | renlang | 人狼 | 非人 |
| 8 | kuangren | 狂人 | 非人 |
| 9 | kuangxinzhe | 狂信者 | 非人 |
| 10 | yaohu | 妖狐 | 非人 |
| 11 | beidezhe | 背德者 | 非人 |

### 2.2 死亡原因枚举（whyDie.java）

| 编号 | 枚举值 | 中文名称 | 类型 |
|------|--------|----------|------|
| 0 | NONE | 未死亡 | - |
| 1 | chuxing | 处刑 | 白天 |
| 2 | daymaozhou | 白天猫咒 | 白天 |
| 3 | dayhouzhui | 白天后追 | 白天 |
| 4 | beiyao | 被咬伤 | 夜间 |
| 5 | nightmaozhou | 夜间猫咒 | 夜间 |
| 6 | nighthouzhui | 夜间后追 | 夜间 |
| 7 | zhousha | 咒杀 | 夜间 |

判断方法：
- `isDayDeath()`: 处刑、白天猫咒、白天后追
- `isNightDeath()`: 其他死亡原因（排除NONE）

### 2.3 猫又明确出局规则
当满足以下条件之一时，猫又被视为明确出局：
1. 猫又白天被处刑并发动猫咒
2. 累计两晚以上出现多人死亡
3. 任意一晚出现4人以上死亡

**约束**: 此时非人类角色不能声称是猫又

---

## 三、代码修改原则

### 3.1 最小修改原则
- 修复 bug 时，只修改出错的具体逻辑，避免影响正常工作的代码
- 禁止一次性修改多个不相关的逻辑
- 修改后立即编译测试，确认没有引入新问题

### 3.2 参考局内代码原则
- 当修复复盘界面显示问题时，**必须首先查看局内对应逻辑**
- 复盘界面的显示逻辑应与局内保持一致（如 VoteInfoRenderer vs ReplayPlayerHandler）
- 从局内代码逐行复制到复盘模块，仅调整数据源差异（ui.ctx → snapshot）

### 3.3 快照读取规则
- 每日快照（DaySnapshot）是在当天事件完成后录制的
- 第 d 天的技能结果（skillTarget）需要从第 d+1 天的快照中读取（dayOffset=1）
- `nonHumanMarked` 标记应使用当天快照检查（dayOffset=0），避免提前显示
- 复盘时使用 `getSnapshot(ui, d)` 获取第 d 天的快照

### 3.4 处刑显示规则（VoteInfoRenderer.java 第47-83行）

**核心逻辑**：遍历每天所有玩家，按死亡原因分类处理

```java
// 处刑显示核心代码结构
for (int k = 1; k < ui.ctx.getGameDay(); ++k) {
    for (int i = 1; i <= ui.ctx.getPlayerSum(); i++) {
        switch (ui.ctx.getDeathReason(i)) {
            case whyDie.chuxing:      // 处刑
            case whyDie.daymaozhou:   // 白天猫咒
            case whyDie.dayhouzhui:   // 白天后追
            default:                  // 其他(夜间死亡)
        }
    }
}
```

**详细规则**：

| deathReason | 角色类型 | 处理逻辑 | 显示示例 |
|-------------|----------|----------|----------|
| 1（处刑） | 妖狐(role=10) | 仅当背德者已死且死亡时间早于妖狐时才显示名字 | 妖狐→ |
| 1（处刑） | 猫又(role=5) | 完全跳过，名字会在猫咒中作为前缀显示 | - |
| 1（处刑） | 其他角色 | 直接显示名字 | 侦探→ |
| 2（猫咒） | 任意 | 显示"猫又+名字(猫呪)"，带猫又前缀 | 猫又+侦探(猫呪)→ |
| 3（后追） | 任意 | 显示"妖狐+名字(後追)"，带妖狐前缀 | 妖狐+少年(後追)→ |
| 4+（夜间） | 任意 | 计入死体计数，不显示到处刑区域 | 死体区域显示 |

**死体显示规则**：
- 无人死亡：显示"平和→"
- 1人死亡：显示"名字→"
- 多人死亡：显示"名字1+名字2→"

### 3.5 技能结果显示规则
- 占卜师(claimedRole=1): 从第1天开始显示技能结果
- 灵能者(claimedRole=2): 从第1天开始显示技能结果
- 猎人(claimedRole=3): 从第2天开始显示技能结果
- 使用 `GameLogicUtils.appendSkillResultLog()` 方法统一处理

---

## 四、DaySnapshot 数据结构

### PlayerStatus 关键字段

| 字段 | 类型 | 说明 |
|------|------|------|
| playerIndex | int | 玩家索引（1-based） |
| characterNumber | int | 角色编号 |
| actualRole | int | 真实职业（对应Role枚举） |
| claimedRole | int | 声称职业（对应Role枚举） |
| claimedRoleOrder | int | 声称顺序 |
| comingOutDay | int | CO日期，-1表示未CO |
| skillTarget | int | 技能目标（占卜/灵能/猎人），0表示无 |
| deathDay | int | 死亡日期，0表示存活 |
| deathReason | int | 死亡原因（对应whyDie枚举） |
| nonHumanMarked | boolean | 是否被标记为非人（破绽） |

### 快照解析规则
- 快照数据格式：`{1:n=42,r=6,cr=1,cro=1,dd=3,wd=1,nm=0,cod=1,st=15}`
- `n`: characterNumber
- `r`: actualRole
- `cr`: claimedRole
- `cro`: claimedRoleOrder
- `dd`: deathDay
- `wd`: deathReason
- `nm`: nonHumanMarked (0/1)
- `cod`: comingOutDay
- `st`: skillTarget

---

## 五、UI 渲染规则

### 5.1 渲染顺序
- 文本标签必须在头像之前添加到容器中，确保文本可见
- 死亡标记、角色图标等覆盖层必须在基础组件之后添加

### 5.2 位置计算
- 角色名称应位于头像底部居中位置
- 使用 `baseY - textHeight/2` 计算文本垂直居中位置
- 参考 UIHelpers.java 中的工具方法进行位置计算

### 5.3 组件配置
- JScrollPane 配置应统一使用 `UIHelpers.configureScrollPane()` 方法
- 保持一致的边框、滚动条策略和透明度设置

---

## 六、数据处理规则

### 6.1 浮点计算
- 胜率等浮点计算必须使用 `BigDecimal`，避免精度误差

### 6.2 枚举值使用
- EventName 枚举值必须使用原始拼音缩写，保持与资源文件兼容
- 禁止使用英文名称替代拼音缩写

### 6.3 数组索引
- 玩家索引使用 1-based 编号
- 数组访问前必须进行边界检查（isValidPlayerIndex）

---

## 七、重构流程与原则

### 7.1 重构任务标准流程

**完整流程**：分析 → 规划 → 执行 → 验证 → 提交

```
1. 分析阶段
   └─ 深入分析代码，找出实际代码问题（方法复杂度、重复代码等）
   └─ 提出重构方案，说明预期收益（代码行数减少、复杂度降低）

2. 规划阶段
   └─ 创建 Todo 列表，明确每个子任务的内容和优先级
   └─ 每个 Todo 项必须具体、可执行

3. 执行阶段
   └─ 按照 Todo 列表顺序，逐个完成任务
   └─ 每次只修改一个文件或一个逻辑块
   └─ 修改后立即编译测试

4. 验证阶段
   └─ 执行 gradlew compileJava 确认编译通过
   └─ 验证功能正常，无回归问题
   └─ 统计代码净变化量（新增 - 删除）

5. 提交阶段
   └─ 将完成的任务从 Todo 列表移到 Done 列表
   └─ 使用 git commit 提交，提交信息清晰描述变更
   └─ 记录代码净变化量
```

### 7.2 Todo/Done 列表管理

**Todo 列表**：
- 使用 `TodoWrite` 工具创建和管理
- 每个任务必须包含：ID、内容、状态、优先级
- ID 格式：`P5-XX`（项目阶段-序号）
- 状态：pending → in_progress → completed
- 每次只能有一个任务处于 `in_progress` 状态

**Done 列表**：
- 完成的任务必须移到 Done 列表
- 记录任务完成时间和提交的 git hash
- 用于追踪项目进度和成果

### 7.3 Git 提交规范

**提交信息格式**：
```
P5-XX: 简要描述重构内容

详细说明：
- 变更的文件
- 代码净变化量（+X/-Y 行）
- 解决的问题或优化的点
```

**提交频率**：
- 每个重构任务完成后单独提交
- Bug 修复和重构分开提交
- 避免一次性提交大量不相关的变更

### 7.4 有效重构标准
- 重构必须减少代码行数或降低复杂度
- 禁止"为重构而重构"，禁止添加语义方法而不合并重复逻辑
- 表面层级的代码合并（如 UIFactory）若无实际收益，应避免
- 重构后代码净变化量必须为负数或零（不应增加代码量）

### 7.5 设计模式使用
- 策略模式适用于小范围、相关联的逻辑块（使用枚举实现）
- 避免过度抽象，单个使用场景的接口/实现会导致代码膨胀

### 7.6 依赖管理
- 使用 GameModule 容器集中管理组件依赖
- 避免直接构造器注入，简化构造参数

---

## 八、调试与测试

### 8.1 编译验证
- 修改代码后必须执行 `gradlew compileJava` 验证编译通过
- 提交前确保无编译错误

### 8.2 日志使用
- 使用 DebugLogger 记录关键调试信息
- 避免在生产代码中添加过多日志

### 8.3 问题排查流程
1. 复现问题并收集证据
2. 分析相关代码逻辑
3. 提出修复方案
4. 实施最小修改
5. 编译测试验证
6. 确认无回归问题

---

## 九、禁忌事项

- ❌ 禁止随意修改 dayOffset 参数，快照读取逻辑有明确设计意图
- ❌ 禁止删除或简化现有测试用例以避免失败
- ❌ 禁止引入未经验证的重构方案
- ❌ 禁止在循环中重复调用修改状态的方法（可能导致覆盖）
- ❌ 禁止使用 PowerShell 批量替换操作，可能破坏文件编码
- ❌ 禁止在未理解业务规则的情况下修改处刑/死亡显示逻辑
- ❌ 禁止修改 whyDie 枚举的顺序或值，影响存档兼容性

---

## 十、参考文件

### 核心参考
- [VoteInfoRenderer.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/VoteInfoRenderer.java) - 局内处刑显示逻辑
- [DaySnapshot.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/DaySnapshot.java) - 快照数据结构
- [Role.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/Role.java) - 角色定义
- [whyDie.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/whyDie.java) - 死亡原因枚举

### UI 工具
- [UIHelpers.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/UIHelpers.java) - UI 工具方法
- [GameStrings.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/GameStrings.java) - 字符串常量

### 状态管理
- [GameContext.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/GameContext.java) - 游戏状态
- [GameRecorder.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/GameRecorder.java) - 游戏记录

### 复盘模块
- [ReplayPlayerHandler.java](file:///c:/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village%20of%20Cyber%20Remake/vocr/src/ReplayPlayerHandler.java) - 复盘界面处理
