# WEB-043首审与PR #11主线轻量核验处置

范围：main5ffc627→已审head496d7ea（归档80051b3＋实现496d7ea），PR #11以merge commit2698ca5合入main。本记录归档用户转交的独立CR结论，不是Coder重跑043全量。

## 已核验声明及来源

CR确认两个提交保留、父链[5ffc627,496d7ea]、合并树7ae67ca9等于已审head、main protected。main run40659366089 attempt1，push.before=5ffc627、tested2698ca5，约1125秒，无重试，full24 passed；65XML重算650/0/0/0，前端200/默认180/gate15、托管14/7等与首审一致。Java9809ec71（231）、frozen6ce8aa54（9）、inputs3d4c43fa（1979）、policy cf6ce45a（55）与PR相同，FatJAR d7232b8c/724197819B。

CR用认证REST下载并复核verify12803464277（6550437B/4a5c9e30…）、receipt12803524025（467B/e2e0376a…）、release12804395945（85991192B/39b40ff5…）；99条证据files、回执、release-upload、报告和runtime stamp关联一致。内层vocr-web.zip86044663B/55043383…，源2698ca5；该源码check_release通过、SHA256SUMS1262/1262、前端1215/1215与web-build inventory一致、包内冒烟1 passed。与首审包同构，sourceSHA带来manifest/stamp/ZIP哈希变化属预期。

CR明确**未运行**仓库download.py完整离线重算，也未复跑本机24项或核NTFS/136文件哈希；不得写成其已执行。Coder上轮自己的download.py复算/来源限制/保全证据在build/web043/main-merge-ci，二者分开。WF-008复用保存的043真实包检查新导入器，属于兼容验证，不是重跑043CI。本工具不读取、复制或索取CR使用的PAT。

## 处置和归属

- 无阻断，043闭环，任务43同步已合流2698ca5。
- Caddy计划锁路径纠正为tools/hosting-toolchain.json，不改锁或下载源。
- hosting-proof诊断与正式evidence.files绑定差异留044；run.cmd端口/原生Windows执行留045。
- origin变化不迁移本地历史，两天证书、IPv4、SHA非签名、普通浏览器信任/公网未验继续明示。
- 用户此前简单试玩一局未见明显功能问题，但拒绝当前外观；不记全面功能/视觉通过。01区域桌面完整游戏窗口和外围临时调试壳归050，真实身份不扩入正式公开API。
- 2026-10-28用户批准先插WF-008完整自助试玩再恢复044。正常完整产品、每轮用户整局反馈，不能用片段/固定种子替代；不提前关闭050/人工/容量/发布门。
