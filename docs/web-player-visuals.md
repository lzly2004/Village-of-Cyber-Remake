# WEB-023：公开玩家头像、图标与人物展示

基线280613b；来源为仓库现有桌面调用和022图片清单，不是外部原版发行程序实机截图，也不声称逐像素复刻整个Swing布局。正式立绘/场景归024/025，终局彩色角色职业头像归025，全菜单/帮助日文化归029。A06/A07/A08仍未放行。

024已接续[对话立绘与背景](web-event-artwork.md)，复用既有清单并验证逐事件舞台重建的冷/暖解码；不改变本文玩家卡片的公开字段与按需加载边界。其他阶段场景仍归025。

025接续：全部事件确认后的职业头像及异常揭晓校验见[终局契约](web-settlement-scenes.md)。门槛前仍使用本文公开外观；PlayerPortrait仅在门槛后接收显式揭晓职业，不从VisiblePlayer读取实际身份。按需加载及失败诊断继续适用。

## 映射与边界

- [PlayerStatusRenderer](../src/main/java/com/vocr/ui/PlayerStatusRenderer.java)与[UIHelpers](../src/main/java/com/vocr/ui/UIHelpers.java)的局内调用传`showActualRole=false`，经[ImageNames](../src/main/java/com/vocr/resources/ImageNames.java)只选`NNs/NNgs.png`。网页仅取VisiblePlayer的公开character/alive；即使FINISHED或揭晓后，023仍使用同一公开状态头像，不读取实际职业或请求cs/fs/hs/ws/ks等结果图。
- NN来自44人物枚举ordinal，**不是playerNumber**。沿用019片假名表，补充[CharacterKanjiName](../src/main/java/com/vocr/domain/CharacterKanjiName.java)完整/短称号，44项单测与Java逐项比对。UI按UIComponentFactory.getCharacterDisplayName显示“完整称号 片假名”；SVG占位标签用片假名以免拥挤。协议键、事件JSON和原台词文本不变，双子27/28、ä/ü、刺繍/刺繡不擅改。
- 名单、指定/CO候补、事件关联人物、技能/票型/公开历史通过同一个显示函数使用日文姓名；存储及命令仍只用原编号/协议值，不以显示名寻找业务对象。正常职业文本取ZY_NAMES；未CO是界面状态“未 CO”，不冒充真实村民。
- 名单头像保持原64×98 CSS px；短称号使用`Njob.png`原图约半尺寸置于下方，与DOM完整姓名并存。公开CO图标32×24覆盖右上；只取公开claimedRole/claimedRoleOrder：占い师1—5、霊能者/狩人1—4；共有者/猫又为yaku4/5，不受顺位影响。其他职业没有相应公开CO图标，不自造图或推断真实职业。
- 有效正整数超出CO图范围：保留完整“CO順N”与职业文字，显示“表示診断：CO図なし”，不伪装成1号，不请求不存在的顺位图。无效顺位为“CO順不明（表示診断）”；未知职业/人物、非法存活状态有明确文字/图像诊断，NONE不是正常人物；原输入不回显为HTML。
- 死亡覆盖采用公开分类：EXECUTION→turi、DAY_CAT_CURSE→noroi、DAY_PURSUED→atooi、所有NIGHT_DEATH→kami。FINISHED投影的小写核心死因只按同四类映射，所有精确夜死因仍共用kami，不通过图标新增真实职业线索。死亡标记位置参照桌面x+5/y+10，53×71；真实身份文字仍只在原finished条件（FINISHED且全部事件确认）后显示。
- 日期继续用权威gameDay的DOM文字，不引入有限日期图；14/50日明确可读，不造14day/50day或回退成第1日。缺图边界/条件可达性的原口径不变。

## 加载、异常与交互

只消费[现有manifest](../web-client/public/assets/images/manifest.json)，不复制映射到新运行时清单，也不把464kB原清单再打进JS。每页首次可视图片通过共享Promise获取一次清单；只接受清单内完整64位内容哈希文件和正整数尺寸，拒绝原型键、外部URL/路径。图片预留固定空间，用IntersectionObserver在可视附近64px触发；名单内被裁掉的远端卡片不预取，所有44条仍在DOM，键盘/滚轮可进入。若浏览器无observer，退为直接加载；046正式支持清单需复检该兼容路径的加载范围。

AssetImage用版本号抑制过期异步结果，卸载时断开observer；切换资源重置图片状态，旧节点error不得标记新图片失败。清单请求/解析、未命中、格式不合法或图片加载失败均显示“画像未表示”与可访问“表示診断”，不静默返回frame，不显示一个假的头像。失败Promise在当前页面保持失败，不自动请求循环；整页刷新可重新获取。图片失败不禁用业务控件、不推进游标、不移动确认按钮、不写新存储。

图片为装饰，alt为空且aria-hidden，DOM姓名/角色/离场状态提供可访问信息，避免把图片文件名当姓名重复朗读。名单保留原命名region、tabindex、滚动说明、候补禁用条件及原点击处理。行按当前公开顺序，不按职业/存活或图片重新排序。图片增加后适当拓宽卡片，最大滚动高度仍19rem；窄屏单列且不截断条目。预约列表/确认区域/身份门槛不重构。

## 验证与接续

单测锁定44人物与12职业来源、全部CO图范围/越界、公开死因映射、私有字段读取负例、清单解析/内容路径及共享失败。浏览器区分真实API路径与合成44人/未知/越界/损坏边界；后者只验证显示，不作为规则可达性证明。静态生产页另外验证冷大厅≤1.5MiB、首次开村新增资源≤3MiB和可视加载，不以开发Vite传输代表生产体积。首页不整包预载约束保留。

共享结构按AGENTS追加默认浏览器回归；旧断言仅将英文显示预期替为来源一致日文、将舞台无素材请求限定为舞台本身（玩家名单现在有图）。完整gate只同步显示预期，未运行时不得声称其15项已复跑。实际命令、结果、截图和失败处置集中见[review-280613b.md](review-280613b.md)。024/025继续复用现清单，需实测事件切换时图片节点重建的解码/首帧闪烁；本轮玩家图不随GameStage实例重建。
