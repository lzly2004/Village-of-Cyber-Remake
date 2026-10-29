# WEB-050-A：首批桌面参考与对齐差异

2026-10-29用户批准050提前后，先完成桌面端实际渲染采集。本文件为基线准备结果，尚不是用户批准的视觉黄金图，也不表示Web外观已改造。

## 来源与可复算入口

- 产品源码：117eda9b17bc21d61450c9b88bfddfe5230af961，与当前WF-008候选相同；采集工具提交4e9d149；新增采集代码单独记录哈希，不冒充其已在该提交中。
- 显式启用的采集器：`tools/visual-baseline/DesktopBaselineTest.java`与`desktop-baseline.init.gradle`。普通构建/默认测试不启用这一路径；没有修改生产Java、build.gradle、默认测试、前端、API或依赖。
- 真实Swing UI/Handler/图片/台词；固定测试局`web050-reference`/50050，临时Replay、内存战绩；音频代理关闭播放，不提供物理声音结论。只操作本测试自己创建的窗口，不操作用户桌面/浏览器。
- Java21.0.12.1+1-LTS，Linux；已有Xvfb独立显示`:97`，1440×1000、96DPI、scale1。无窗口管理器装饰，因此窗口与客户区均1280×720、insets均0。普通桌面装饰/浏览器工具栏尺寸仍需另测；这里的零边框不推广到真实桌面。
- 本机产物：`build/web050/desktop-baseline/desktop.json`（组件树）、`sha256.json`（10 PNG+JSON索引）、`source-proof.json`（工具哈希）；日志`build/web050/baseline-test.log`、`build/web050/xvfb.log`。未上传GitHub，不声称CR已独立读图。
- 复跑需已有独立测试DISPLAY，经既有安全入口：`bash tools/run-gradle-safe.sh test --tests com.vocr.ui.DesktopBaselineTest --offline -I tools/visual-baseline/desktop-baseline.init.gradle`。保持≥2GiB与单worker；不在用户显示上执行采集。

## 首批五场景（已实际查看）

| 场景 | 桌面画面与Web差异 | 后续范围 |
|---|---|---|
| title | 整窗标题背景与六个图片按钮；Web现有侧栏与标题顶栏不在此参考中 | 050-C主菜单，Web专属解锁/恢复入口单列 |
| configuration | 原图配役按钮左右两列（4+3），标题背景与右下返回 | 050-C配置/导航 |
| dialogue | 人物立绘、底部原版消息框；Web当前半透明文字层及事件编号/额外说明不同 | 050-B核心窗口与统一逐字显示 |
| vote | 顶部两行头像/公开CO与死亡标记，左侧信息区，右侧图片按钮，日数状态面板 | 050-B名单、信息、操作与切换层级 |
| settlement | 两排人物，下方逐人公称/真实职业、死亡日/死因或最终结局；Web当前卡片缺信息 | 050-C完整结算与同状态对照 |

对话原版消息框为父坐标(260,450)、760×230；正文相对框(20,50)、710×200，字号26；姓名在框顶部；确认按钮覆盖消息框，第一次点击补全文字，之后才进入下一条。投票右侧主按钮x=1060、宽194、高约126，y=10/146/282/418/554；日数面板(890,25)、100×130，字号20；信息正文字号24。结算说明字号16，分列说明100×150；横向步距由头像宽+40计算，不能写死成Web卡片宽。

组件树bounds是相对各自父组件，不是全局坐标；visible是组件本地属性，不证明没有父隐藏/遮挡。桌面会保留被前景覆盖的旧组件，不能照组件数量复制到DOM。Web同态成对截图、完整坐标差异/可接受偏差仍待核心窗口实现前确认；这五图未覆盖44人/50日/125%/150%/长台词或所有特殊事件，不能据此放行A07。

## 字体：已经证实与尚未证实

- 桌面声明`Takao Mincho`，本机Java可用字体列表中未安装；实际组件`getFont().getFamily(Locale.ROOT)`为逻辑`Dialog`。对话26、投票24、结算16等字号已从真实组件记录。
- `fc-match 'Takao Mincho'`返回DejaVu Sans；日文字体候选查询`fc-match 'sans-serif:lang=ja'`为本机Noto Sans CJK JP，`serif:lang=ja`为Noto Serif CJK JP。fontconfig查询不是Java每个CJK字形物理来源的证明；逻辑复合字体亦不能简单称为DejaVu实际绘制全部日文。
- 这批参考代表当前Linux环境实际运行桌面程序的效果。尚未证明用户心中的原桌面运行环境也是同样回退，不把不存在的Takao字体名写进CSS便声称一致。
- 下一步需展示桌面实际参考与可分发字体候选样张、明确许可/文件/覆盖/子集/加载/成本及font-display；本批未新增Web字体或依赖，未调整预算，也未确认替代字体。若用户期望另一个桌面平台的字体，先补该平台实际参考。

## 结算字段对照（更正上一轮初步风险判断）

| 桌面来源/文案 | 现有Web数据 | 展示要求 |
|---|---|---|
| getClaimedRole、公称職業 | VisiblePlayer.claimedRole | 结算继续保留，与真实身份分别显示 |
| getActualRole、真の職業 | revealedRoles中的actualRole | FINISHED且无当前未确认事件后读取；整组校验继续保留 |
| getDeathDay | VisiblePlayer.deathDay | 依桌面格式显示死亡日期 |
| getDeathReason | VisiblePlayer.deathReason | PublicDeathReason.project在FINISHED已有精确枚举；进行中仍合并夜死，不推断 |
| END_SURVIVE/END_DEAD/END_WIN | 公开胜负、存活、终局实际身份 | 按EndAnimeHandler具体分支显示最终结局，不把引擎alive误解释为所有阵营均最终生还 |

上一轮仅看到运行中`NIGHT_DEATH`而提出可能需要终局专用投影；进一步核对PublicDeathReason和VisiblePlayer后确认精确终局值已存在。因此不预设新增API/字段，更不从夜死事件推断。050-C优先复用该既有契约，测试FINISHED但仍有currentEvent时不提前显示揭晓。

## 本批验证、失败与边界

进程监护隔离回归15/15通过；最终专用Swing采集1/1通过，含真实窗口/图片、五场景和首次补全文字不推进待呈现队列的断言。执行前可用内存3GiB以上，单worker，既有safe入口；未运行本机全量check/test/FatJAR或浏览器全回归，不引用旧650/205/192为本批结果。

失败保留：首次init脚本目录解析错误导致未发现测试；修正为projectDir绝对源目录。之后真实对话需要getEventText/getEventImage，补采集代理转调真实Resources；新增Event与java.awt.Event名称歧义改为明确限定类名。都属于采集器前提/编译问题，未改产品逻辑、删断言或放宽超时。日志见build/web050/baseline-first-no-tests.log、baseline-second-resource-interface.log、baseline-second-failed.xml、baseline-third-ambiguous-import.log。最终增加明确队列断言后再采集通过。

当前050只进行基线准备，WF-008仍等待CR增量及用户最终完整试玩结论、明确合流授权和main核验。已交付117eda9、专用候选资料及原NTFS脏树不变。050产品B/C不混入PR #12，基于其合流后的main开干净产品分支；本批准备可随后移植。A06/A07/A08/046/047/048/049、公网和最终发布均未放行。
