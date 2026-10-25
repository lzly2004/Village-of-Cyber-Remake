# e3e67c2 Review 交接与 WEB-019 验收

## 018 外部 reviewer 交接

用户本轮交接确认：WEB-018（当前基线 e3e67c2）已获独立 reviewer 接受，无阻断或非阻断发现；人物/职业/事件目录、参数语义和规则参考§3.5修订均准确。本轮采纳该结论，不复制整份报告或重新盘点历史。

019仅定向复核 Resources.resolveResourceLookup / resolveSpecialOrDefault / getEventText / replaceParams、CharacterEnglishName/KatakanaName、DialogueDayDeathHandler的wsw调用与GameStrings.NO_SACRIFICE；结果与018目录一致。A06仍未通过，目录完成和本次台词接入均不代表全量日文化。无待修review发现，无需重排49步队列。

## 019 交付与边界

风险分级：纯前端功能。直接导入既有 `src/main/resources/lines/event_lines.json`，不另存台词副本，不改生产Java、原JSON、协议或依赖。名字目录只为台词替换服务，由实际English/Katakana枚举顺序配对，测试锁定44人（包括ä/ü和双子映射）；不新增真实身份或私有技能数据。

- 用ch1和eventname查角色事件；special仅按ch2精确键匹配，键存在即采用，即使空/null也不回退default。
- talkerName、targetName、partnerName均取ch2片假名；ch2为null/缺失/NONE时为空，不用ch3；未知参数按原行为替换为空，不trim或重写原文。
- wsw绕过逐角色目录，使用与NO_SACRIFICE一致的系统文本及末尾换行。NONE作为明确异常，不冒充人物台词。
- 台词和死亡叙述都按纯文本显示，保留换行/Unicode/特殊称呼；移除旧中文事件摘要映射。原关联人物区保留，ch3仍可按原UI列示，但不参与文本替换；没有将死亡叙述标为活人发言。
- 只更换事件正文，不改变呈现顺序、确认按钮/游标、写入或刷新流程、CO中断、终局待事件读完才揭晓的条件；原本最新公开聚合信息的时序差异保留，正式场景叙事待024/025。

异常返回有界诊断代码，UI独立标记“台词显示异常”“此为界面诊断，非日文原文”，不输出原事件对象/私有状态，不静默catch；用户仍须主动确认该事件。错误类别：INVALID_EVENT、SENTINEL_EVENT、UNKNOWN_CHARACTER、UNKNOWN_TARGET、MISSING_CHARACTER、MISSING_EVENT、INVALID_ENTRY、EMPTY_LINE。空事件外层关联人物区也有保护，避免诊断被模板异常遮蔽。非法JSON本身由构建显式失败，不以伪造台词掩盖。

## 本轮实际验证（2026-10-22）

工具链沿用web-client/README.md的本机Linux缓存：Node 22.23.0、Java 21.0.12.1、Gradle 8.8、Chromium缓存及ubuntu22.04-x64覆盖参数。Playwright始终1 worker，后端均由默认 `bash tools/run-gradle-safe.sh runWebTestServer --args=17071` 启动，未设置VOCR_WEB_SERVER_COMMAND。安全入口串行停止旧daemon、检查内存并监护自身子进程；启动前可用内存超过2GiB。两次浏览器启动串行，未绕过安全入口，未修改系统/自动挂载/审批/swap等配置。

1. 覆盖前提与定向单测：`node --test web-client/unit/event-dialogue.test.js` 通过（26项）。先验证资源恰为44人物×42正常事件，再逐分支核对1848条default与744条special；含明确原文字面样例、空special不回退、三类参数/ch2空值/ch3无关、未知参数、系统wsw及缺失/未知/错误类型负例。静态穷举不宣称全部分支在真实局里自然触发。
2. `cd web-client && npm run test:unit`：42项全部通过（26项新台词测试和16项既有单测/配置契约）。
3. `npm run build`：通过。最终静态JS 364.33kB（gzip 97.49kB）；复用JSON已进入产物。此为前端生产构建，不是Fat JAR/发布打包。
4. `npm run test:e2e`：本轮默认59项全部通过，1.6分钟（既有55项+最初4项新台词测试）。覆盖真实固定种子事件逐条对照资源、刷新、特殊对象/Unicode/死亡叙述/系统/诊断的合成展示边界、合成终局不提前揭晓、合成文本提供者的HTML纯文本验证；既有默认恢复、CO中断、技能预约、公开信息与票型回归均实际执行。
5. 此后补空事件外层保护与第5项台词浏览器测试；按影响重跑 `npm run build` 及 `npm run test:e2e -- --grep 'WEB-019|选择配役、逐条阅读|真实对局从浏览器指定投票直至终局|固定种子 CO 中断'`：最终8项全通过，15.9秒。包含5项台词用例及3项恢复/真实完整局揭晓/真实CO中断。没有将先前59项写成“最终默认60项重跑”；resolver和单测输入未再修改，复用同轮42项成功结果。
6. 已查看特殊称呼台词截图：三行原文/换行及“ベッティちゃん”显示正确。截图在忽略的test-results目录，不纳入提交。
7. 本批文件差异、暂存差异及 `git diff --cached --check` 在提交前核对；只暂存019相关路径。原有大量CRLF差异、Game.java空白、.workbuddy和NUL均保留。

开发期失败与处置：首次定向测试25/26，通过失败断言发现测试工厂将显式undefined误用为默认Erich，已修正测试输入并重跑；首次构建因移除摘要映射时误删configurationName导出失败，已恢复原导出。沙箱内Node的spawnSync报告EPERM，导致既有gate配置测试读不到子进程stdout；获准在沙箱外运行同一命令后42项通过，未修改或放宽既有断言。上述失败均不计通过。

未运行全量Java/check/test、七配役完整gate、黄金轨迹、概率/资源全量校验、Fat JAR或桌面窗口验收；未将018或旧基线结果记作本轮重跑。测试后端启动不等于Java全量验收。未刷新黄金摘要。

## 剩余缺口与下一项

A06仍未通过：现存重制资源不等于与外部原版发行逐字比对；人物列表、场景、菜单/帮助及源资源混合语言仍待023/025/029，图片/音频待020起对应任务。最新聚合公开信息与逐事件时序仍是已登记的叙事差异，不在019引入本地快照回滚。正式首发仍需后续统一放行。

WEB-019完成；下一项READY为WEB-020（素材来源、可发布范围、缺失清单与体积预算）。未提前执行020、素材、音频或完整菜单日文化。
