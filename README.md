# Village of Cyber Remake（电脑村重制）

这是一个使用 Java 21 与 Swing 编写的单机人狼推理游戏。项目包含七种配役、昼夜流程、CO、占卜/灵能/护卫/咬人、怀疑度与投票、胜负判断、战绩和复盘记录等系统。

## 开发环境

- JDK 21（必须；JRE 或 Java 8 不足以编译）
- Git LFS（首次克隆后执行 `git lfs pull`，否则音频可能只是指针文件）
- 无需单独安装 Gradle，仓库已包含 Gradle Wrapper

项目路径可以包含中文和空格。`gradle.properties` 已处理 Windows 中文路径下测试进程的类路径编码问题。

## 常用命令

在项目根目录执行：

```bat
build.bat test
build.bat build
build.bat run
build.bat package
```

也可以直接使用 Gradle：

```bat
gradlew.bat clean check
gradlew.bat fatJar
```

- 测试报告：`build/reports/tests/test/index.html`
- 可执行胖 JAR：`build/libs/vocr-1.0.3.4-all.jar`
- `jpackage` 应用目录：`portable_app/VOC_Remake/`

`fatJar` 在打包前会自动运行全部测试、校验 560 条概率规则，并验证代码引用的必需音频均存在、已从 Git LFS 拉取且具有 RIFF/WAV 文件头。

## 游客 HTTP 原型

在项目根目录执行 `gradlew.bat runWeb`（Linux：`./gradlew runWeb`），默认监听
`127.0.0.1:7070`。指定端口可用 `runWeb --args="7071"`。
此原型支持创建游客局、读取安全更新、指定投票与释放会话，尚无浏览器页面。
会话和战绩只保存在当前进程内；重启后清空。接口与示例见
[游客 HTTP API](docs/guest-http-api.md)。

## 目录说明

- `src/main/java/com/vocr/app/`：程序入口与应用装配
- `src/main/java/com/vocr/application/`：单局会话、玩家命令、安全视图与合法动作
- `src/main/java/com/vocr/config/`：全局常量、路径和概率配置
- `src/main/java/com/vocr/core/`：对局状态、流程协调、规则执行和录像采集
- `src/main/java/com/vocr/domain/`：事件、角色、配役、死亡原因等领域类型
- `src/main/java/com/vocr/history/`：战绩数据及持久化
- `src/main/java/com/vocr/logging/`：与游戏状态无关的普通诊断日志
- `src/main/java/com/vocr/replay/`：复盘存档模型及管理
- `src/main/java/com/vocr/resources/`：图片、台词和音频等资源访问
- `src/main/java/com/vocr/rules/`：通用权重规则引擎
- `src/main/java/com/vocr/ui/`：Swing 场景、渲染器和组件工厂
- `src/test/java/com/vocr/`：JUnit 5 测试，按生产包镜像组织
- `src/web/java/com/vocr/web/`：独立 HTTP 源集与游客会话组合根，不进入桌面 Fat JAR
- `src/main/resources/`：图片、台词、事件映射、音效和 BGM
- `docs/legacy/`：从旧源码目录移出的历史设计文档和进度记录
- `config/probability.json`：唯一的外部概率配置
- `data/`：本地战绩和运行时数据（不提交）
- `gradle/`、`gradlew*`：固定版本的构建入口

## 修改核心逻辑前

先运行 `gradlew.bat clean check`。七种配役已有固定种子的初始化黄金摘要；如果测试提示摘要变化，应先确认这是预期的规则变化，再更新基线，不能只为让测试变绿而替换期望值。

项目结构评价、当前技术债与分阶段重构路线见根目录的《项目全面评估与第一阶段重构记录.md》；新技术是否适合引入、适用边界与验收门槛见《技术栈与复杂度治理评估.md》；桌面与网页双端、每局隔离和 AI 决策路由的共同演进顺序见《双端与AI兼容架构规划.md》。网页路线细节见《网页端迁移可行性评估.md》，模型语义、部署和评测细节见《大模型接入开发规划.md》；本轮 AI 待决问题、规则缺陷与工程治理结论见《AI与规则审计决策记录.md》。
