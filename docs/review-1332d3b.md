# 1332d3b复评与工作流准备处置

范围d8b8e3e..1332d3b，5文件；P-1至P-4补修获接受，无阻断。reviewer独立189单测、文件9/9/重复27/27、gate15/15是原提交证据；默认134/135及脏环境42/45保留，不用coder旧135/135覆盖。漏release-baseline导致的首次gate失败由reviewer纠正，不视为产品缺陷；其未跑FFmpeg门禁与Java全量、choom shim未生效的边界保持。

## F-1前置补修（5e615dd）

已确认selectAll在候选尚未渲染时直接count可取0。改为明确预期2/9/2，等待候选数量后逐个check，再严格断言确认按钮enabled；digest hook等待设5s，未提高测试总超时。没有sleep、重试绿灯、削弱原库/容量/去重断言。

新增受控文件读取暂停用例：读取被挂起时预览不存在，先启动selectAll，再显式释放，必须两项均勾选后确认并验证去重。将同一用例跑在旧helper上，确定失败于Expected checked / Received unchecked（不是环境超时）；finally恢复修复版。最终文件10项repeat-each=3：30/30（26s，retries=0）。日志build/wf001/f1-before.log、f1-final.log。纯测试改动，不启动全量Java/打包、默认全套或gate；后续干净检出校准另做。

## 工作流意见的采纳与边界

采纳：三类产物分开、输入指纹、干净检出、现有Playwright配置/串行端口、浏览器实际导出传递、环境失败分类、完整证据及人工门槛。完整计划与本轮第一步见[工作流计划](workflow-plan.md)。

不采纳在原工作区清除134项CRLF/缩进差异或整体规范化。原137条porcelain记录（含未跟踪目录折叠）及内容hash保留，独立ext4检出作为校准对象。EOL方案在隔离检出使用仓库局部core.autocrlf=false/core.eol=lf；不改用户全局配置，也不在本批改已有脏.gitattributes/.gitignore。不笼统忽略.trae，其中含规则skill；.idea已在提交版gitignore中，但已跟踪文件不会因忽略而消失。

临时oracle同源验证只证明来源，不证明语义；仍由冻结样本/黄金测试和三层对等断言约束。不要求手写声明代替检查，也不允许Auto-Fix修改黄金/指纹。冻结文件“变更须评审”不等于永远禁止经批准的样本扩充。

LFS数量与额度须重新核对，不能沿用“20文件、1GB免费”的未经核实预算。当前提交扫描为32个指针（23 BGM/9音效），声明payload合计620138120字节；原工作区payload与本地对象逐一hash/长度通过，不代表远端已验证。2026-10-24[GitHub官方计费](https://docs.github.com/en/billing/concepts/product-billing/git-lfs)列Free含10GiB存储/10GiB带宽；账户实际剩余额度未查询，仍需缓存及按需拉取，未擅自下载整包或启用付费。

“同SHA连续3绿”作为Auto-Fix试点前观察条件，不构成无flake或安全证明；后续还需失败注入、受限路径与陈旧SHA检查。Chat触发条件保留为复杂任务建议，先试点量测再固定，不因评审建议立即增加每轮强制流程。
