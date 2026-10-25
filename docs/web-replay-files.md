# WEB-035：录像文件导入导出与双向交换

基线4101819，评审处置见[归档](review-4101819.md)。新增网页与桌面文件选择/隔离校验/预览/确认入口；不改规则、随机源、v3九槽交换格式、HTTP、现有IndexedDB条目格式或累计战绩，不进入036。不允许将外部rawRecords交给fromRecords/已完成会话端点，不调用ReplayManager.load(用户选择路径)。

## 文件边界与迁移

文件为UTF-8 JSON九槽数组，至少一非空、每格slotIndex等于文件位置；允许一个BOM和CRLF。单个网页导出重排到文件第0格，多选按选择次序放入前N格，其余null；内部始终归一化slotIndex=0。桌面仅在最后确认后把所选局复制到用户选择的0—8本地槽位，文件位置不决定覆盖目标。

输入上限8MiB（包含空白/BOM），最多50日/44人/20000条rawRecords、每条64KiB、技能history最长51、每日至多100轮票型；保留数组顺序与两类索引，不拆分原文/嵌套JSON，不解析大seed为JS数字。结构深度32、至多100万JSON值、数值token至多128字符/十进制scale绝对值400；不支持的或无法无损表达的数值拒绝，不静默舍入。文件名/MIME只是选择提示，不能代替内容校验。

两端在JSON成员被覆盖之前拒绝重复键（含Unicode转义后的重复），拒绝非法UTF-8、第二BOM、截断/多根、越界/重复玩家/日期/投票者、身份映射漂移、未知枚举、汇总/END矛盾。拒绝原型污染/凭据/资源引用键、URL/绝对路径/上级路径型引用；普通未知JSON字段不执行/不渲染。网页保留普通未知字段并随导出带回；桌面DTO忽略未知字段，桌面重写后的未知字段不承诺保留，已知语义/rawRecords是双向比对基准。

版本0/1/2显式迁移到3：v0/1缺失或<=0的两类局数归-1；v2/v3缺计数归Java已知默认0；缺history只补空数组，缺票型列表/候补补空数组、方式默认-1，不制造历史。缺version/未来版本拒绝。缺少无法确认的必要身份字段、不合法旧索引等进入不写库的LEGACY_COMPATIBILITY_DIAGNOSTIC；不按常识“修正”。原legacy-v1.json的灰名单[30]超出两人索引，故保持原件并诊断，不猜成外观编号。legacy-record-v0.txt是旧录制文本，不是承诺可直接导入的九槽JSON。

历史endResult=0允许无END或实际GameRecorder写出的END|result=UNFINISHED|day=0至totalDays；仅预览资料、保管原本，不能入库、播放恢复或累计战绩；正常终局仍须END与汇总一致。所有解析/迁移都发生在独立对象中，选择/取消/失败不写库。

## 网页与桌面交互

网页：仅显式打开Replay库才加载文件工具；选择后完整验证并预览所有候选，默认不勾选，确认所选终局后执行一次readwrite事务。批内及已有重复按内容hash去重，不覆盖既有数据；9局/64MiB、配额、校验或多标签竞争失败整批回滚。15s文件读取超时不自动发布晚到预览，不自动保存；重新选择须新显式操作。导入/播放/下载不访问游戏命令或终局API，不改变活局游标/预约/身份门槛，也不恢复录像为对局。

导出：网页可单局或整库生成九槽JSON，8MiB总上限仍适用，超限应分局导出，不截断。选中文件原本备份逐字节保留（即使验证失败），不标为可交换。坏库可用只读游标备份既有IDB entry数组至vocr-library-recovery.json，预算64MiB+1MiB/15s；**这是私有修复资料，不是交换Replay，不可导入**，不新增存储格式/版本、无自动修复、无清库。界面明确不要共享修复资料。浏览器原生文件控件语言与实际落盘/覆盖确认由浏览器环境决定；生成下载不等于用户已保管文件。

桌面：Replay浏览器顶部新增文件导入/导出按钮；真实JFileChooser、候选局预览、目标槽选择、最后覆盖确认全部完成才调用原saveToSlotChecked。候选数据私有保存，data()只给副本；不暴露可被调用方篡改的待写入对象。导入只写一次已确认槽位；源文件不动，原其他槽位不变。导出只读本地所选槽并复制为第0格，已有目标必须另确认，失败不破坏旧文件；拒绝源/目标符号链接，原子替换已确认目标，不退回不受保护的覆盖方式。新目标同目录临时完整文件移动发布，拒绝未确认目标竞争。

## 真实样本与实际双向路径

src/test/resources/replay/desktop-saved-v3.json由本轮ReplayFileDesktopTest实际运行SeededGuestSession(35035)核心对局，经原桌面右键保存与新文件选择器导出取得；不是手写快照，不是用户私人对局。简易村12人，7日，33条rawRecords，村人胜利，saveTime原值2026/10/24 00:01:16。只含虚构角色/公开测试seed/录像字段，无会话凭据、API地址或主目录；不把固定种子自动操作冒充人类试玩。

原样本SHA-256 d36135d531e3046afaf366c00e7c92d3de3e7b11e4adb101ad5d152577ce6487。原030合成样本、034展示变体不改。桌面测试运行后生成的新产物可有不同saveTime，不能覆写冻结样本当作期望通过。

1. 实际Swing窗口右键确认保存→新文件选择器导出，产物build/web035/desktop-actual.json，仅隔离TempDir/忽略目录，实际资源图片、静音替身；不启动用户Game单例或触碰真实data。
2. 浏览器file input导入冻结桌面原样本→预览/勾选确认→12人播放→下载实际浏览器文件build/web035/browser-export.json；结构/快照/rawRecords逐项一致，旁边真实活局快照、sessionStorage及写请求不变。
3. 设VOCR_WEB035_BROWSER_EXPORT指向该实际下载文件，再启动隔离Swing窗口：文件选择→候选预览→槽8→确认；验证原槽0和选中文件不变，再经文件选择导出并逐已知字段/快照/rawRecords与浏览器文件比对。默认无此环境变量时测试用冻结桌面样本，仅是桌面入口测试，不能冒充跨浏览器证据。

## 验证记录

日志/截图位于忽略目录build/web035与web-client/test-results。两端共享web035-file-cases.json正负例，分别执行真实Java隔离解析器与JS入口；不依赖各自的“没抛错”。实际Swing窗口/文件对话框的选择、预览、取消、拒坏、指定槽、再导出均已验证，已检查新按钮和槽8截图。

- 首版JS新测试括号语法错误，修正后CRLF工厂又把rawRecords字符串内逗号插入裸换行；改用JSON缩进换行转换，不改生产逻辑或放宽断言；文件/O-1共51/51随后通过。
- 首版桌面测试var复合声明编译失败，改为两个声明；实际核心保存导出与其后桌面导入均通过。
- 首轮文件浏览器6/7，多选工厂在同一JS任务内合成所有click导致Vue模型只剩末选项；改为逐个真实check，无sleep，保留9局严格断言并加批内去重。补文件超时晚到后8/8（11.2s）。
- JS最终单测188/188（2.43s）；Java定向56/56（新增文件32+桌面2+既有22，5s），已用实际browser-export.json。之后补实际UNFINISHED历史marker及数值无损边界，以上数值不当作最终重跑，最终结果另补。
- 中断重启丢失/tmp环境脚本，首次build因FFmpeg未在环境中而退出，后续测试未执行。固定二进制仍在，环境脚本迁至忽略目录build/web035/env.sh后按原入口重试一次，不改系统配置或安装依赖。

本轮含生产Java，按AGENTS须在定向通过后另获本轮全量test/check/FatJAR批准；普通继续不复用031/032授权。最终源码的oracle必须重生成后才可跑15项gate。未完成/未授权前不提交、不声称A09/A10正式放行。046/049听审/DPI等人工与047容量门槛不因小样本双向成功而关闭。

### 最终状态验收（本轮实跑）

用户在定向通过后明确授权本轮全量验收与打包。所有JVM经原安全入口串行、单worker、2GiB门槛；无系统/磁盘配置变化。续接时项目路径在沙箱可写范围之外，最初误判为实际分区只读；取得项目写权限后确认是沙箱映射。首次全量尝试又在/run/user安全锁只读处退出，未启动JVM，随后获准使用沙箱外原安全入口，37s成功。不改锁目录或安全脚本，也不把启动前退出算产品失败或通过。

- 前端全量单测：`source build/web035/env.sh && npm --prefix web-client run test:unit`，189/189（2.41s），日志unit-final.log。
- Java定向：`VOCR_WEB035_BROWSER_EXPORT="$PWD/build/web035/browser-export.json" bash tools/run-gradle-safe.sh test --tests com.vocr.ui.ReplayFileDesktopTest --tests com.vocr.replay.ReplayFileExchangeTest --tests com.vocr.replay.ReplayExchangeContractTest --tests com.vocr.replay.ReplayCompatibilityTest --tests com.vocr.ui.ReplayTextFormatterTest --tests com.vocr.ui.ReplayWebDisplayContractTest --offline`，57/57（6s），日志java-final-targeted.log；其中文件33、真实桌面2、既有22。
- `npm --prefix web-client run build`：图片/音频prebuild与postbuild通过，代码580436字节/gzip157300，全站静态77753468字节；JS主块484.07kB/gzip124.79，CSS21.49kB/gzip5.48。日志build-final.log。
- `npm --prefix web-client run test:audio -- -g WEB-033`：相关Replay音频2/2（2.4s），未冒充完整音频套件。`npm --prefix web-client run test:assets`：4/4（6.5s），冷大厅131274字节、首局新增397068、初始17图。日志audio-final.log/assets-final.log。
- `npm --prefix web-client run test:e2e`：最终默认134/134（2.6min），含8个文件用例，retries=0；日志default-final.log。查看最终导入库截图，文件说明/导出/修复/行操作均可见，浏览器原生文件控件语言不作应用文案校对结论。
- `VOCR_APPROVED_FULL_CHECK=1 VOCR_WEB035_BROWSER_EXPORT="$PWD/build/web035/browser-export.json" bash tools/run-gradle-safe.sh fatJar --offline`：617/617、0失败/错误/跳过（61个测试类），七配役特征轨迹、概率配置、运行时资源与FatJAR通过，总37s。日志full-final.log。实际桌面导入槽8/按钮截图已查看；原桌面深色标签沿用，不借此宣称桌面全局视觉重新验收。
- FatJAR 724198234字节，SHA-256 3ac39a376a5989b3b8da9f325dc0c8dc576b79806a94c0dc7c703d668d573ebb；含新增桌面类，无com/vocr/web、Javalin或Jetty服务类。全量生成的15个oracle与最终main/test/web Java指纹42d4233b4ac3b66954fc5f0003a15d0bd55b082778c5198700301642ad4944c1全部一致。`npm --prefix web-client run test:gate`最终15/15（3.6min，retries=0），日志gate-final.log；七配役双策略与真实CO中断均通过。

已核对开工137项原修改的内容hash未变；ReplayBrowserHandler原为CRLF，仅在原字节上添加按钮调用，暂存须使用HEAD的LF加同一调用，保留其既有CRLF工作区差异。真实data文件仍为原9月时间戳，隔离测试不使用用户路径。A09/A10本批功能证据不代替038/046/049统一与人工验收；047仍须大文件/满库、多标签与内存量测。不进入036。

提交前在宿主机按实际可执行文件/参数只读复查，本轮JVM、Chromium与Vite/Playwright均无残留；仅选择性暂存29个任务文件，cached diff --check通过，无全仓规范化。
