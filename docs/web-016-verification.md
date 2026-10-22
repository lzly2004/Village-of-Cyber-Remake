# WEB-016：全日全轮票型与测试入口复现性

基线c386717。范围：Vue展示、前端测试、Playwright命令配置与相关文档。main/web/test Java、DTO/API、依赖、核心规则、随机源、Replay与存储格式均未改动。D1/D2逐项核验见review-c386717.md。49步顺序未变，下一项017。

## 实现与边界

- VoteHistoryPanel按服务端voteHistory顺序显示全部日期；默认展开最新日期，其他日期通过原生details/summary鼠标或键盘展开，每日列出全部rounds，而非只取末轮。
- 直接显示method、candidates、round、maximumVotes、全部leaders和lines中的playerNumber/targetPlayerNumber/receivedVotes；不按客户端票数重算首位、不排序、不删零票行。多首位标为同票再投，唯一首位标为处刑，沿用已完成投票DTO的构造约束。
- CO中断不加入完成票型，空history不生成假历史；不会将中断目标标为已处刑。终局保留投票历史，不新增真身份/死因字段，也不改变此前最近票型可在未读事件时浏览的时机。
- 不新增浏览请求或本地镜像，折叠是非持久UI状态，刷新回到默认展开；sessionStorage仍仅sessionId/eventCursor。失效/新局不保留旧历史；409只同步、响应丢失先同步，不重投。
- 未知method显示“未対応の投票方式”，但仍可浏览已提供的公开票型。没有对象dump/v-html，额外字段哨兵不显示。alternativeVotes仍只开放FREE/GREY，指定入口只查DESIGNATED；未知未来动作不产生按钮。
- GameApplication在进入核心前按method查找当前合法动作并逐项比较有序candidates，因此未来仅加枚举不等于开放动作。该结论来自源码复核，不声称本轮修改枚举并实跑Java实验。现有未知枚举HTTP拒绝证据属于014的90项基线，不作为本轮重跑。

## 实际命令与环境

在web-client目录使用：

```sh
export JAVA_HOME=/home/noeticuser/Desktop/.vocr-toolchain/java/jdk-21.0.12.1+1
export PATH="/home/noeticuser/Desktop/.vocr-toolchain/node/bin:$JAVA_HOME/bin:$PATH"
export VOCR_GRADLE=/home/noeticuser/Desktop/.vocr-toolchain/gradle-8.8/bin/gradle
export GRADLE_USER_HOME=/home/noeticuser/Desktop/.vocr-toolchain/gradle-home
export PLAYWRIGHT_BROWSERS_PATH=/home/noeticuser/Desktop/.vocr-toolchain/browsers
export PLAYWRIGHT_HOST_PLATFORM_OVERRIDE=ubuntu22.04-x64
unset VOCR_WEB_SERVER_COMMAND
npm run test:unit
npm run build
npm run test:e2e
```

定向命令为`npm run test:e2e -- tests/vote-history.spec.js`，与完整前端浏览器回归串行运行。没有设置后端覆盖变量：实际默认启动`bash tools/run-gradle-safe.sh runWebTestServer --args=17071`，wrapper内部唯一一次添加console=plain/no-daemon/单worker；Windows命令仍自己带console，未在Linux冒充Windows执行证据。

015实际曾使用带offline的VOCR_WEB_SERVER_COMMAND，遗漏的环境已补记到015验收文档；本轮没有用该覆盖路径替代默认入口验证。没有修改用户CRLF Wrapper、浏览器安装目录或系统配置。

## 已执行结果

1. Node单测11/11通过：原8项，加方式/未知值兜底、同票全首位/顺序与权威票数、Linux默认命令回归3项。无新增依赖。
2. Vite生产构建通过：20模块；JS94.97kB（gzip35.98kB），CSS9.98kB（gzip2.93kB），HTML0.59kB。
3. 浏览器全部55/55通过（1.4分钟，原48+新增7，workers=1/retries=0）。新增最终未知动作按钮负例补入后，再串行定向复跑最终文件7/7通过（16.7秒）。均使用unset覆盖变量的Linux默认安全入口。
4. 真实通常村seed4202逐次自由投票至终局，每个日期、轮次、候补、首位、每条投票和得票数均与当前HTTP响应逐项比对。至少两日时键盘收合/展开、刷新、存储不变与零写请求、服务端完整状态不变均通过。实际截图含第2日首轮Hans/Milli同票、第二轮Milli处刑及第3日票型；这是自然产生的真实多轮，不仅是合成覆盖。
5. 合成展示边界明确标注：三种方式、两轮平票/唯一首位、零票、非升序候补/首位、未知方式、未知动作与额外私有字段哨兵。它不证明该合成状态在固定种子中自然可达；未知动作无对应按钮、未知历史方式不被冒充为指定投票。
6. 真实409冲突和真实执行后丢弃响应均只同步恢复完成历史，writes=1；实际CO中断断言voteCompleted=false、history与投票前相等、刷新一致；失效清理与新局空历史通过。
7. 首次定向6/7通过、1项测试失败：测试把首个投票日写死为1，而通常村实际首个投票日为2。修正为核对投票前权威gameDay及完成history日，不改产品逻辑、不换种子、不降低完成历史非空断言；后续完整和最终定向全通过。
8. 实际查看最终vote-history.png：两日三轮票型/全部首位与表格可读，无遮挡裁切；原生键盘操作有断言。长列表仍是文本原型，布局/视觉统一归021/023，不声称正式舞台或全量日文化完成。截图位于忽略的test-results。
9. 串行任务退出后无Java进程，GNOME shell保持原PID35882（GDM的1552也未变），MemAvailable约5.2GiB。本轮未修改监护、未使用全机扫描清理或批量kill，未调整swap/earlyoom/锁屏/驱动。

本轮完整验证的是前端单测/构建/浏览器；Java540项、七配役黄金轨迹、概率/资源与Fat JAR属于014已有基线。本轮无Java/资源/协议改动，未借“继续”再次执行桌面全量、打包或Swing交互，不把旧哈希当作新构建证据。按风险验收前端后提交；保留Game.java空白、既有CRLF、.workbuddy/NUL等无关修改。

## 后续门槛

017必须复验七配役整体流程，并评估未读事件时最新聚合公开信息与桌面决策入口的叙事差异；任何公网试玩前关闭私有诊断输出的门槛不变。018登记报告聚合/逐日及括号差异，019/023对照原版统一。047的生产256MiB/100会话容量仍未验证，不能据本轮浏览器测试宣称容量达标。CORE-TIE-001与CORE-CLAIM-ORDER-001继续仅为PROPOSED。
