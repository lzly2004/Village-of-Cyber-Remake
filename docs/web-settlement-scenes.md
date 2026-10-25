# WEB-025：死亡、处刑与结算视觉

基线2f4ffa9，来源为仓库桌面调用与现存素材；不宣称外部原版发行程序实机或逐像素复刻。保留024日间/下午对话及021阅读区/独立确认区；不接音频、不改规则、API、随机序列或sessionStorage格式。A06/A07/A08未放行。

## 场景来源与边界

| 当前公开呈现 | 背景/选图 | 桌面来源 |
|---|---|---|
| NIGHT_RESULT / wsw | DIALOGUE_DAY_BACKGROUND，无人物；继续019系统台词 | DialogueDayDeathHandler |
| NIGHT_RESULT / yjsw | 同上；原JSON两图顺序，同一人物居中/底边30 | 同上，TRANSITION_SHORT_MS=1000 |
| EXECUTION / cxs、hzsw、mzsw | DIALOGUE_AFTERNOON_BACKGROUND；原JSON两图顺序；GameStrings对应处刑/后追/猫咒模板与人物显示名 | DialogueChuxingHandler，TRANSITION_MEDIUM_MS=2000 |
| SETTLEMENT / crsl、rlsl、krsl、yhsl | 按权威公开result选END_VILLAGE/END_WOLF/END_FOX背景，当前事件单图，不从实际职业猜图 | GameSceneHandler/EndGameHandler |
| FINISHED且所有事件已确认 | REVIEW_BOARD，胜负图标和WIN_*原文字；舞台内角色总览及下方详细玩家列表按有效揭晓映射选择职业头像，死者仍gs灰图 | EndAnimeHandler、ImageNames.characterResultPortrait |

sceneArtwork只读取当前公开事件和公开胜负，复用Resources语义的eventImageNames：special按ch2键存在优先，空/null不回default；ch3不选图。死亡数组不是两个人物、不是精确夜死因推断；不增加真职业或技能字段。布局仍1280×720/meet、原尺寸居中、底边y690。阶段/事件不匹配、图数异常、未知胜负/缺图均明确诊断和frame，不静默空白。

## 网页阅读节奏

网页沿用019即时纯文本及“一条公开事件一次确认”。桌面夜死会暂时隐藏台词，处刑会先确认发言、再隐藏面板等待、再确认结果；网页不复制这套额外确认/隐藏/打字机步骤。两图完整解码后整组就绪，本地按来源1000/2000ms顺序切图；文字及处刑结果模板始终可读，外部确认按钮始终按既有busy/未读条件工作。定时器不调用nextEvent、不写存储、不发命令；提前确认会卸载并取消定时器，刷新重放当前事件的本地图像节奏而不改变已确认游标。

原生“画像を切替”按钮可用键盘重看任一图，并停止该事件自动切换；prefers-reduced-motion直接从后图开始，同样可手动重看。前后图都仅属于当前已公开事件，不预取下一事件。使用024共享manifest/最多16个近期解码对象，前后图及背景全部解码成功后才发布；失败保持明确诊断，台词和确认仍可使用。025不以HTTP缓存代替解码或首帧实测，也不承诺任意设备永不闪烁。

## 终局身份与异常

原门槛保持`update.phase === FINISHED && !currentEvent`。门槛前不建立揭晓角色映射、不计算/请求职业头像，玩家卡片继续023公开外观/CO/通用夜死标记；结算发言立绘仅依据已公开事件JSON。门槛后校验revealedRoles必须为数组、数量匹配、编号与玩家一一对应、无重复且职业为11种正常值；NONE/未知/原型键/缺项不当作村民。异常整组停止职业图推断，各行明确“身分不明（表示診断）”，并保留返回/新局入口。

有效揭晓按playerNumber关联，不依数组顺序，也不把playerNumber当外观编号。活人依据ImageNames角色后缀：猫cs、狐fs、背德hs、狼ws、狂人/狂信ks、其他s；死者gs。未知外观不伪造人物，图片加载失败继续AssetImage明确兜底；真实职业文字也有未知诊断，不输出undefined或原始异常数据。

结算背景内用可聚焦的原生DOM网格展示全部人物及身份，保留原图/称号/死亡标记；长局在总览内滚动，不截断人数。它与独立下方详情均只在同一揭晓门槛后显示职业信息；沿用AssetImage可视加载，共享内容地址缓存。网格响应窗口宽度，不复制Swing固定两排的坐标；阅读区与网格分别限高并保留键盘访问。

实际验证、截图、失败与处置集中在[review-2f4ffa9.md](review-2f4ffa9.md)。025按源码/资源核对局部视觉；完整关键画面对照与正式浏览器/DPI仍须046及首发统一验收。026音频尚未开始，内存工具建议待独立安排。
