# WEB-039 首审及主线轻量核验处置

范围：6f62d8f → 39ff007（PR #7，2提交/19文件），随后merge commit 42e71da。首审与主线核验均无阻断。首审报告18文件为计数笔误，实际19；生产close调用shutdownNow，awaitTermination是测试侧等待，不冒充生产关闭逻辑。

## 意见处置

- P3-A：采纳桌面FatJAR来源说明。build.gradle仅将main输出和桌面依赖纳入FatJAR，启用可复现归档；039生产Java变化在web源集，云端桌面包与038同hash不表示Web漏编。Web编译/测试另有证据。本机与云端JAR哈希不同的具体成因未经逐字节比较，工具链差异仅是可能原因，不采纳报告将其确定归因或将云端JAR称作已独立下载重算的措辞。两边均保留来源限制。
- P3-B：接受当前应用层400/409拒绝也续空闲期、24h绝对上限不延长；041限流时复核。
- P3-C：注册表扫描O(n)、n≤100；047量测请求/定时扫描锁竞争和回收延迟，不提前优化或承诺100并发SLA。
- P3-D/主线观察1：本批更新039任务状态为已随PR #7合入main 42e71da；不为首审后归档追加未审head。

## 合流和实际证据

父提交6f62d8f、39ff007，2cc40cc和39ff007完整保留；合并树30479c4ecd06f8c4752bbb485e9ae8d6e195e8e6等于已审head，零合流编辑。Coder以REST和本地对象核对；CR另用PR/merge diff逐字节、父链与指纹交叉核验。

main run[40353523839](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40353523839) attempt1，push.before=6f62d8f，tested=42e71da，full21项全部passed。Java62份XML独立重算628/0失败/0错误/0跳过，前端192、默认154、gate15；生产1、assets4、audio27、files30、桌面交换1及工具套件通过。本条为039当次证据，**不是040重跑**。

主ZIP id12665119410，3,875,604字节，SHA-256 d7202d344bb0ab4b13119af8dcf8a6c64c23285841a64b2858a747d66431d545；回执id12666315876，462字节，SHA-256 44dcc47a78323e7fa2627c891547b7b493d1727d661c3adbcea3ef647554b1c0，绑定id/digest/head/run/attempt。Coder和CR分别从公开ZIP及一方页面元数据运行仓库download.py离线重算通过，包内证据逐字段一致；最终REST403，元数据转录不是REST响应或签名，认证ZIP端点未验证。

Java指纹3f33b50e（226文件）、frozen6ce8aa54（9）、inputs3d4c43fa（1979）、policy de3ed92f（45）与已审head一致。FatJAR报告724,197,819字节/d7232b8cc37d0c761924936b496c3b055866021aef372ae18681d31bd94d18a0，未独立下载云端724MB文件核验。

本机主线核验包及原项目忽略目录镜像：build/web039/main-merge-ci/main-handoff.md。Coder核对137 porcelain/142展开路径、136文件SHA不变，并只更新main/origin-main引用；原HEAD未检出。CR因/home和挂载限制未读本机包、未抽样保全、未复跑本机039 full；其结论来自重新下载的独立外部证据。CR模型不能读图，不把PNG存在/hash核验写成目视审查。

## 保持开放

fork、认证ZIP端点、真实容量100/多用户及60s回收等待、Q04生产安全、A06/A07/A08、050实施及046/047/049人工容量发布门仍开放。主线核验通过后用户已指示进入040，039合流后未另加提交；本归档随040正常批次提交。
