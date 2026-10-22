# WEB-018：日文展示来源目录

基线abb3caa。本目录供019台词、023人物展示、024场景、029菜单/帮助及030/034 Replay核对使用；本轮只整理来源、映射与差异，不替换页面/生产资源，不修改规则、协议或枚举。A06仍未通过，不能把目录完成写成“网页已全量日文化”。

## 来源与证据等级

- **源码/资源已核对**：以下以仓库当前桌面实际实现及现存资源为依据，不是凭常识翻译，也没有独立运行外部原版发行程序。不能据此证明重制文本与外部原版逐字一致。
- **图片已查看**：仅明确列出的四张按钮图实际打开核对；其他图片只核对用途与路径，字样待对应素材任务查看，不从文件名猜字。
- **待迁移/待核对**：网页新交互提示、帮助中文、原版未取证的文字均保留缺口和归属，不伪称有原版日文依据。
- 查找顺序：实际调用路径→源常量/枚举/JSON→必要的图片核对；注释、EventName中文说明、网页临时翻译都不能覆盖实际取值。

| 类别 | 权威来源 | 当前Web / 后续消费方 |
|---|---|---|
| 人物身份键/完整称号/短称号/片假名 | [English枚举](../src/main/java/com/vocr/domain/CharacterEnglishName.java)、[Kanji枚举](../src/main/java/com/vocr/domain/CharacterKanjiName.java)、[Katakana枚举](../src/main/java/com/vocr/domain/CharacterKatakanaName.java)、[UIComponentFactory](../src/main/java/com/vocr/ui/UIComponentFactory.java) | labels/App目前直接用英文键；019参数替换、023角色展示 |
| 职业名称 | UIComponentFactory.ZY_NAMES/getZY；[Role](../src/main/java/com/vocr/domain/Role.java)只管枚举/编号 | labels.roles多为中文，claimRoleNames五项已有日文；023/025/029 |
| 系统文字、胜负与票型 | [GameStrings](../src/main/java/com/vocr/ui/GameStrings.java)及实际调用者 | 公共信息/票型已有日文但样式不完全一致；019/023/025/029 |
| 逐角色台词/特殊对象 | [event_lines.json](../src/main/resources/lines/event_lines.json)、[Resources.getEventText](../src/main/java/com/vocr/resources/Resources.java) | 当前eventLabel仅摘要；019负责真正台词与兜底，024接场景 |
| 按钮/配役内嵌文字 | [UiImage](../src/main/java/com/vocr/resources/UiImage.java)、[ImageNames](../src/main/java/com/vocr/resources/ImageNames.java)、[StartSceneHandler](../src/main/java/com/vocr/ui/StartSceneHandler.java)、[VoteButtonPanel](../src/main/java/com/vocr/ui/VoteButtonPanel.java) | 020/022核对图片，023/024/029接入，不把中文文件名当日文原文 |
| 帮助 | [InfoSceneHandler](../src/main/java/com/vocr/ui/InfoSceneHandler.java)、[help目录](../src/main/resources/help) | 029待接；现存Info1-1-1.txt正文实际为中文，不能冒充现成日文帮助 |

## 人物目录（完整44人）

表中编号是外观枚举ordinal，不是对局playerNumber，也不是狼人职业。协议继续保留CharacterEnglishName原键（含ä/ü），不能用显示译名替换身份键或按当前出场顺序配表。表值以枚举实际定义为准，不按注释纠正数据。

| 外观编号 | 协议键 | 完整称号 | 短称号 | 片假名 |
|---|---|---|---|---|
| 1 | `Abel` | 青年 | 青年 | アーベル |
| 2 | `Erich` | 研究生 | 研究 | エーリッヒ |
| 3 | `Matthäus` | 傭兵 | 傭兵 | マテウス |
| 4 | `Otfried` | 教師 | 教師 | オトフリート |
| 5 | `Karl` | 情報通 | 情報 | カルル |
| 6 | `Till` | 少年 | 少年 | ティル |
| 7 | `Samuel` | 宝石商 | 宝石 | ザムエル |
| 8 | `Hans` | 旅人 | 旅人 | ハンス |
| 9 | `Beatrice` | 少女 | 少女 | ベアトリーチェ |
| 10 | `Amanda` | 陶芸家 | 陶芸 | アマンダ |
| 11 | `Irene` | ランプ屋 | 洋灯 | イレーネ |
| 12 | `Elsa` | 歌姫 | 歌姫 | エルザ |
| 13 | `Nora` | 未亡人 | 未亡 | ノーラ |
| 14 | `Johanna` | 物識り | 物識 | ヨハナ |
| 15 | `Milli` | 読書家 | 読書 | ミリィ |
| 16 | `Judith` | 召使い | 召使 | ユーディット |
| 17 | `Michael` | 貴族 | 貴族 | ミハエル |
| 18 | `Günther` | 自衛団長 | 団長 | ギュンター |
| 19 | `David` | 騎士 | 騎士 | ダーヴィッド |
| 20 | `Julian` | 職人見習い | 職人 | ユリアン |
| 21 | `Klemens` | 神父 | 神父 | クレメンス |
| 22 | `Heinrich` | 探偵 | 探偵 | ハインリヒ |
| 23 | `Liddi` | 学生 | 学生 | リディ |
| 24 | `Brigitte` | 小説家 | 小説 | ブリジット |
| 25 | `Helga` | 酒場のママ | 女将 | ヘルガ |
| 26 | `Natalie` | シスター | 尼僧 | ナターリエ |
| 27 | `Volker` | 双生児弟 | 双弟 | フォルカー |
| 28 | `Eva` | 双生児姉 | 双姉 | エーファ |
| 29 | `Willy` | 流れ者 | 流者 | ヴィリー |
| 30 | `Reichard` | 修道士 | 修道 | ライヒアルト |
| 31 | `Hugo` | 宿屋主人 | 宿主 | フーゴー |
| 32 | `Rosa` | 踊り子 | 踊子 | ローザ |
| 33 | `Wendel` | 神学生 | 神学 | ウェンデル |
| 34 | `Sergius` | 薬師 | 薬師 | ゼルギウス |
| 35 | `Kaja` | 煙突掃除人 | 煙突 | カヤ |
| 36 | `Betti` | 店員 | 店員 | ベッティ |
| 37 | `Chloe` | 洗濯女 | 洗濯 | クロエ |
| 38 | `Carmen` | 娼妓 | 娼妓 | カルメン |
| 39 | `Renate` | 剣士 | 剣士 | レナーテ |
| 40 | `Romi` | 村娘 | 村娘 | ロミ |
| 41 | `Gerda` | 刺繍工 | 刺繡 | ゲルダ |
| 42 | `Iwan` | 大工 | 大工 | イヴァン |
| 43 | `Oktavia` | 貴婦人 | 婦人 | オクタヴィア |
| 44 | `Helmut` | 音楽家 | 音楽 | ヘルムート |

`NONE`为第0项哨兵，不是第45位人物。桌面getCharacterDisplayName拼“完整称号 片假名”，getCharacterFullName额外拼“/英文”；getJobText仅用短称号。Beatrice注释中的“ベアト”不是实际完整片假名；双子注释与实际值存在交叉，必须按27/28的实际枚举值匹配。刺繍/刺繡也不擅自统一字形。本轮不改枚举、注释或协议。

## 职业、配役与系统文字

以下职业名逐项来自ZY_NAMES；NONE走getZY的“無し”分支，不能据此推断真实村民身份。角色外观称号与职业名称是两个不同目录。

| 协议职业 | 编号 | 桌面名称 |
|---|---|---|
| `NONE` | 0 | 無し |
| `zhan` | 1 | 占い師 |
| `ling` | 2 | 霊能者 |
| `lie` | 3 | 狩人 |
| `gong` | 4 | 共有者 |
| `mao` | 5 | 猫又 |
| `wu` | 6 | 村人 |
| `renlang` | 7 | 人狼 |
| `kuangren` | 8 | 狂人 |
| `kuangxinzhe` | 9 | 狂信者 |
| `yaohu` | 10 | 妖狐 |
| `beidezhe` | 11 | 背徳者 |

配役键保持`jianyi`、`tongchang`、`yaoohu`、`kuangxin`、`beide`、`maoyou`、`daxing`，对应1—7的gameN.png选择图、gameN-1.png标题图；不修正yaoohu拼写，也不把职业yaohu与配役yaoohu混用。选择来源是GameSceneHandler.renderSelect和ImageNames.gameSetupButton/gameSetupTitle。七组图的具体字样本轮未逐张核图；网页“简易/通常/妖狐/狂信/背德/猫又/大型”和介绍语是当前原型，不作为已确认的原版日文名称，029以核图及帮助核对为准。

| 用途 | 实际源文字/模板 | 迁移边界 |
|---|---|---|
| 入日/和平 | DAY_START_FORMAT=`%d日目になりました。`；NO_SACRIFICE=`犠牲者はいませんでした。\n` | 019/024保留换行，wsw走系统模板，不从空JSON猜台词 |
| 胜负 | WIN_VILLAGER=`村人勝利`；WIN_WOLF=`人狼勝利`；WIN_FOX=`妖狐勝利` | 025/029；真身份展示继续受终局/事件读完门槛约束 |
| 投票方式 | VOTE_FREE=`自由投票\n`；VOTE_GREY=`グレラン：\n`；VOTE_DESIGNATED=`指定投票：\n` | Web目前“グレー投票”不是该源文字；019/023/029按场景核对 |
| 平票/处刑 | VOTE_TIE=`投票が同点となりました。再投票を行います。`；VOTE_RESULT_FORMAT=`%d票で%sさんが処刑されました。` | 不改票型规则，只格式化权威结果 |
| 公开分区 | `[占い師]`、`[霊能者]`、`[護衛先]`、`[処刑]`、`[死体]` | 023；保持公开条件，不能为拼字导出私有状态 |
| 疑い先 | DOUBT_TITLE=`- 疑い先 - 全体表示\n\n`；MSG_NO_DOUBT_YET含“まだ特に疑い先もなく” | 比较符号按DTO，不按日文字面重算数值 |
| 已有混合语言 | MARKER_EXPOSED=`破绽`；VOTE_HISTORY_TITLE含`轮`；VOTE_TITLE_REDO含`重新投票`；END_SURVIVE/END_DEAD/END_WIN沿用现有措辞（如`最終存活`、`最終胜利`） | 这些是当前重制常量，不宣称原版纯日文；019/023/025/029核对后迁移，不能无依据声称“原样日文化” |

### 图片中的字：用途不等于字面

| 图片与调用 | 本轮核对结果 | 归属 |
|---|---|---|
| startButton.png / START_BUTTON | 实际打开，文字为英文`Start`，不是已核实的“はじめる” | 020/029，是否保留原设计按素材核对，不自行编造 |
| tohyoGrey.png / VOTE_GREY | 实际打开，日文`グレラン` | 023/029，和系统常量相符 |
| check.png / VOTE_INFO | 实际打开，文字`記録確認`；GameSceneVoteHandler注释写“履歴チェック”，二者是不同证据 | 023/029，不把注释误认成按钮图字 |
| 开启回避.png / AVOID_ENABLE | 实际打开，字面`開啟回避`，并非日文“回避あり” | 020/029，当前图片混合语言登记，不改开关含义 |
| continue/replay/record/info/collectionsButton.png | StartSceneHandler与UiImage已核对用途/资源路径，未逐张确认字面 | 020/022/029；部分桌面按钮本身空回调，不据图片承诺Web已有功能 |
| goTohyo、doCO、reiCO、kariCO、uranaiCO、kyouyuCO、catCO、shiteiUranai、shiteiGoei等png | VoteButtonPanel用途/路径已核对，字面待核图 | 020/022/023/029，不从拼音/文件名推译 |

## 事件键目录（44项含哨兵/系统特例）

[EventName](../src/main/java/com/vocr/domain/EventName.java)的中文label/toString是开发说明，不能当作原版日文标题；JSON键和HTTP eventname都保持枚举name。Web [labels.js](../web-client/src/labels.js)当前44项中文摘要也不是原台词。下面按键分组列出全部44项，不另发明“原版日文事件标题”。019真正展示的是对应角色台词，摘要若保留为辅助UI须另有明确日文文案依据/审核。

| 分组 | 全部事件键 | 来源/显示方向 |
|---|---|---|
| 共有与潜伏 | `gyfo1r`、`gyfo1`、`gyho2`、`qfgsw3`、`gkgsw4`、`qfjcqr5r`、`qfjc5`、`gycx6` | JSON逐角色台词，含对象special，不用中文开发label替代 |
| 占い、认证与相关发言 | `zco7`、`jhdh8b`、`zjgh8b`、`jbdh8r`、`zjgb8`、`zrzbjg9`、`zbdxsw10`、`gprz11p`、`gprz11r`、`zcrh12r`、`zcrh12`、`gyzs13`、`zs14`、`zspz15`、`szsm16`、`wz17` | 依ch1角色及ch2对象查表；发言是公开声称，不是真职业证明 |
| 霊能 | `lnco18`、`ljgh19b`、`ljgb19` | 同上，不据职业常识更改历史起始日 |
| 处刑发言 | `cxs` | 角色台词，和系统处刑模板不是同一来源 |
| 终局发言 | `crsl`、`rlsl`、`yhsl`、`krsl` | 角色台词，事件确认顺序和身份揭晓门槛不变 |
| 回避 | `hblr`、`hbg`、`hbln`、`hbz`、`hbm` | 真实CO中断的公开台词，不能把中断显示成已处刑 |
| 其他CO | `lrco`、`mco` | 公开声称，不泄露潜伏行动者 |
| 死亡叙述 | `yjsw`、`hzsw`、`mzsw` | JSON第三人称叙述；ch1是被叙述角色，不统一当成活人说话；夜死公开分类不放宽 |
| 无死系统特例 | `wsw` | JSON没有逐角色条目；DialogueDayDeathHandler直接用NO_SACRIFICE |
| 哨兵 | `NONE` | JSON没有条目，不伪造角色台词；缺失/未知状态明确处理，不能当正常空发言 |

资源键静态核对：44个真实人物、每人42个事件条目，共1848；上述42个正常键覆盖全部44人。`NONE`/`wsw`均为0条是已知哨兵/专用系统路径，不是删除用例后的“覆盖通过”。这只证明现存JSON键/文本结构，不证明所有完整局都能自然触发，也不证明与外部原版逐字一致。

### 019必须保留的台词查找与参数语义

1. Resources.resolveResourceLookup先用`event.ch1.name()`找人物，再用`event.eventname.name()`找事件；special按`ch2.name()`精确匹配，键存在即取special，否则default。special空内容当前会进入“无可用台词”诊断，并非再回退default；不能凭习惯改优先级。
2. 现有三类占位符`<param=talkerName>`、`<param=targetName>`、`<param=partnerName>`**全部替换为ch2的片假名**，不是把talkerName换成ch1。ch2为空或NONE时名字为空串；当前文本替换不使用ch3。该结论来自Resources源码，不是根据参数名猜规则。
3. 例：Abel/gyfo1/ch2=Erich取default，得到`共有COするよ。\n相方はエーリッヒだ。\nよろしくね。`；ch2=Betti命中special，源文字为`共有COするよ。\n相方はベッティちゃんだ。\n一緒に頑張ろう！`。Abel/gyfo1r/default中的talkerName同样指ch2。
4. 原实现用Matcher.quoteReplacement处理替换；未知占位符替换为空串；未知人物/事件、空台词当前会返回中文诊断。019须明确兼容与用户可见兜底，不能把诊断字符串当日文原文、不能静默吞掉异常或dump私有状态。
5. JSON原换行、全/半角CO、标点和称呼均保留来源证据，按纯文本展示；不使用v-html解释文本。wsw走系统模板而非造一个人物；图像映射/多人物布局另归024，不混入文本猜测。

## 已确认差异与接续验收

| 差异 | 当前事实 / 不变边界 | 承接 |
|---|---|---|
| 局内/Replay灵能起始日 | [VoteInfoRenderer](../src/main/java/com/vocr/ui/VoteInfoRenderer.java)调用UIHelpers：占卜1、灵能2、护卫2且无球；[ReplayTextFormatter](../src/main/java/com/vocr/ui/ReplayTextFormatter.java)占卜/灵能1、护卫2。规则参考§3.5本轮修正文档，不改两条运行路径 | 018关闭文档归属；030/034保留兼容核对 |
| 报告行与括号/字形 | 桌面UIHelpers聚合“短称号 : 目标○→目标●”；Web逐条“第N日”及全角括号，破綻/破绽也非字形一致；空疑い行仍留空 | 019/023/025对照实际源再统一，不在目录阶段改UI |
| 未读聚合信息 | Web显示最新公开状态，桌面历史入口在决策界面；017仅接受为当前原型差异，不豁免首发对等 | 019/024评估正式叙事；改变交互须批准，不做本地快照回滚或提前揭晓 |
| 菜单/帮助/缺失来源 | 部分图片英文/中文、Info帮助正文中文、新游客失效/网络提示没有对应桌面原文 | 020/029区分保留设计、翻译提案和用户批准；不自行宣称原版出处 |

019从本目录的键和真实Resources语义建立文本选择/替换及正负回归，再接浏览器显示；023消费人物名称映射，不修改wire键；029逐项迁移菜单/帮助并明确来源。不借018完成提前标记019或A06通过，49步顺序不变。
