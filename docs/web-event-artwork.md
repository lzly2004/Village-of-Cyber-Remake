# WEB-024：事件立绘与多人物对话

基线d743b22。对照仓库桌面真实调用与原素材，不宣称外部原版程序或全量日文化已验收。仅接DAY_DIALOGUE/AFTERNOON_DIALOGUE的背景和立绘；其余阶段/决策/终局暂保留021占位，死亡、处刑、结算与数组切换由025接续。A06/A07/A08未放行。

## 选图与布局

- [eventArtwork.js](../web-client/src/eventArtwork.js)直接import原`event_image_mapping.json`，不复制或重译。沿Resources：ch1找角色、eventname找事件，special按ch2精确键存在优先；空/null special不再回default。ch3不参与查找/布局。字符串为单图，数组顺序完整保留给025；错标为对话的数组显示诊断和frame，不静默丢第二张。
- wsw是系统无人物特例，NONE不是正常人物；缺失/非法角色、事件、target、配置或路径均明确诊断。仓库当前全局兜底为UiImage.FALLBACK即frame，代替图有独立显示诊断，不伪称角色原图。文件必须命中原清单内容地址；加载/解码失败独立诊断，台词和确认继续可用。
- 普通对话首图按原尺寸居中，底边在设计y690（720减30）。六个左侧起始事件来自UIHelpers：gyfo1/qfjc5/zjgh8b/zjgb8/gprz11p/zcrh12，x300。
- 对话配对来自**前一已展示事件的缓存图**，不是把ch2/ch3各找一张头像。DAY的连接白名单与DialogueDayHandler一致；普通配对为缓存左x300、当前右x650，左图在右图上层，文字仍在最上层。gprz11r后紧接gprz11p时按桌面specialEvent三连分支缓存当前，下一条换成当前左、缓存右。只用下一事件的键决定这条连贯关系，不提前取下一事件图片。
- DAY→其他阶段及非连接DAY重置旧配对；AFTERNOON沿原handler消费已有缓存。背景分别UiImage.DIALOGUE_DAY_BACKGROUND→haikei3、DIALOGUE_AFTERNOON_BACKGROUND→haikei，按桌面1280×720画布铺满。原图480×270会放大，不能称作高清重绘。艺术SVG仍viewBox 1280×720/meet；台词保留独立原生DOM/换行/Unicode和局部禁锚定，事件key仍创建全新阅读容器。

## 刷新与解码

useGuestGame仅新增**内存中的公开presentation前缀**，按绝对eventCursor合并窗口，不新增sessionStorage字段。开始/正常命令响应的已有窗口足够时不额外请求。刷新后若缺前缀，后台只读GET同会话`?eventCursor=0`补齐；保留原GET当前游标及400恢复路径。补读不延迟原权威更新、不写入、不重试命令；版本号/sessionId/revision不匹配或请求失败时不替换对局状态，立绘明确标示上下文不足，并单独展示当前已知图，不能猜之前人物。整页刷新恢复同一布局，异步晚到不回退确认游标。

[stageImageCache.js](../web-client/src/stageImageCache.js)共享023单次manifest读取；只请求当前背景/当前人物/必要已确认前图，最多16个近期解码对象（含背景），不全包预取或预取后续事件。Cold时背景与全部人物decode/尺寸核对完成后才整组发布，期间明确“画像読込中”，不出现只有半组人物的过渡。Warm重建可同步命中仍保留的解码对象，避免每次watch的空白帧。事件组件卸载或计划变化用版本号忽略旧解码结果；SVG最终加载异常也有诊断。

冷加载、暖缓存8帧和逐事件组件重建须用受控请求/RAF及截图实测；HTTP缓存本身不作为“无解码/无闪烁”的证据。本轮只对实际测试环境及所测序列负责，不能由8帧观察推及所有硬件/浏览器/显存回收。046继续正式支持列表/DPI/辅助技术，025复检新场景和资源组合。

## 验证入口

`npm run test:unit`包含全部default/special及176有序数组、连接/三连/阶段/历史窗口/私有字段负例、容量有界解码缓存；测试串行。`npm run build`继续022前后门禁并统计含台词/图映射的全部代码；`npm run test:assets`在生产preview验证完整资源解码、无大厅素材预取及含首舞台的新增≤3MiB。相关浏览器入口为event-artwork.spec.js/stage-layout.spec.js，默认共享回归另跑；功能门新增每条对话实际图、解码完成、无图像诊断的断言。

实际命令、失败处置、数字、截图及剩余门槛集中在[review-d743b22.md](review-d743b22.md)。保留原图、WebP与清单，不重复入Git或重写历史；025仍处理死亡/处刑/终局背景、阶段转换与异常揭晓输入，不用024的完成代替它。
