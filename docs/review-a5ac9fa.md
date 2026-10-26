# WEB-038增量首审②处置（下次正常批次入仓）

CR范围a9410bd→a5ac9fa，1提交/6文档，无阻断。CR用保存REST元数据+ZIP独立重算run40298234106（head a5ac9fa）full21项通过，主ZIP 53ab0507…/3,792,484B，回执与head/run/attempt匹配。新鲜REST独立读取受403限制、模型不支持图像输入的边界保持。CR撤回将134,994B/397,068B当作预算上限的判断；实际测试限额1.5MiB/3MiB，font必要成本须未来实测，不能先推断超标。

四项P2-A/B、P3-C/D均确认采纳有效，A06/A07保持未通过。残留三条为可选，登记随下一正常批次同步，不为此追加PR #6新head：

1. 050语言清单补title/tooltip/placeholder（这些是Web自有字符串，应纳入翻译）；浏览器文件选择默认按钮/原生校验消息另列浏览器原生文案，记录浏览器语言与可控边界，不能以HTML lang保证其全为日文，不修改用户系统语言来过验收。具体可访问替代/差异由050基线确认，不能把整个页面当不可控例外。
2. 050具体字体方案确定时固定font-display及必要预载/加载顺序，覆盖加载前/后与失败回退几何、FOIT/FOUT/布局偏移；不是本轮直接选定swap/block，也不调整预算。
3. 050的Web差异表补“受影响A/Q行及理由”，例如游客恢复关联A12/Q04、浏览器本地库/交换关联A09/A10，便于046/049逐项复检。

## PR #6主线轻量核验（用户本轮回传）

PR #6以merge commit合入main 6f62d8f（父6cc32b5+a5ac9fa），4提交/18文件保留，树与已审head相同，零合流编辑。CR通过GitHub API与公开artifact独立重算run40305236487 attempt2：push/full21项全passed，Java617（61XML）/前端192/默认147/gate15/production1；主ZIP3,804,192B、b949a0c9…e6ac1，回执1282b59b…0ea09绑定正确。Java ff7cb300、policy45文件de3ed92f与PR head相同。FatJAR报告724,197,819B/d7232b8c…；六项人工门保持未覆盖。

首次字体下载等待由Coder主动取消，唯一一次同SHA环境重试通过；CR官方API确认attempt1 cancelled，其产物入口404。严格口径为“首次产物当前未取得/不能复算”，不能从404断言从未生成：当时页面曾显示artifact id/digest，但未成功下载。attempt2的ZIP/回执已独立取得并重算。详细取消/缺失及来源限制留在忽略证据目录build/web038/main-merge-ci/main-handoff.md。

CR本轮挂载缺失未做本地字节/工作区抽样；保全为Coder侧136文件哈希与137porcelain/142展开项证明，不冒充CR独立核验。未运行本机全量、fork、认证ZIP端点或724MB JAR独立hash。主线核验无阻断通过，无需CR补材料；下一项039确为生产Java分级，本轮定向后须另获完整验收/打包授权。

上述三个可选050计划提醒随039正常批次同步到计划，未实施050或改预算；A06/A07仍未通过。039开工基线为6f62d8f，不复用CRLF旧树oracle。
