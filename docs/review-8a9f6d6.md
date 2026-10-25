# PR #2评审处置与merge commit合流

2026-10-25用户转交PR #2评审，通过且无阻断。Reviewer已撤回“本地没有8ef3240对象”的错误判断；本轮接受更正，不将其沿用为缺对象结论。前轮本机55/32/14及PR run40175462834的617/189/136/15/55均是对应批次证据，不计本轮本机重跑。源代码范围与迁移事实见[维护记录](review-dea2a94.md)。

## 决策、授权与合流

采纳合流，保留4提交并用merge commit。自动审批最初仍拒绝Ready，认为泛化“自行判断”不解除此前草稿限制；未绕过，随后用户明确授权“PR #2转Ready、merge commit合流及main CI验证”，不含设置或自动合并。转Ready前一方API核对head=8a9f6d6、base=8ef3240、唯一verify success、draft=true、mergeable_state=clean、4提交/12文件；没有虚构第二个check。

PR #2已合入main@dbbf884dc0d713c9baec98cee5e565fee4240ff2。SSH fetch独立验证父提交依次为8ef3240cd2dd99810f4dc1cf738492e8c67045d8和8a9f6d6d125a5ebd77aa9d01e33f63da80a880ff，4提交全保留；树c7a094278e7d2263de82ece41219d340e8ab842e与已审8a9f6d6完全相同，未冲突编辑、未squash/rebase。未删除分支，不改本地main@1332d3b，不检出覆盖原脏工作区。

main真实push run40177191539为本轮统一验收入口，不拿PR旧绿灯替代。公共REST API合流后遇403 remaining=0，记录reset_epoch=1792918546；后续以一方GitHub页面读取进度/产物身份和SSH核对提交，最终来源按实际复核结果追加，不把UI转录称为REST元数据或认证ZIP下载。

## 评审建议处置

- 维护时长说明采纳：tools/ci未知→full是有意保守政策，未改verify-tiers/workflow。已观察verify 604秒（首轮main）与911秒（PR #2）、对应run 608/914秒；缓存、runner等条件会影响时长，近期维护预期约10–16分钟，仅作排程参考，不作为固定性能承诺。失败/超时先分类，不因耗时降门槛。
- 合流后build_policy使用44文件9ceeeb0370e361ad7d2d680a67e1f98789f2b5edcfacc763387be57020b6248e；不再拿f355d13a当当前政策。Java ff7cb300、frozen6ce8aa54、inputs3d4c43fa不变，仍需按最终原始输入核对oracle。
- revision4已在真实PR成功包运行，Git版本/renameLimit字段有真实云端证据；“无result”新字段分支仍主要由29项证据测试中的取消夹具证明，本轮不为演示故意取消正式CI，不把普通成功包说成真实取消分支验收。
- oracle-generate为full计划中被java-full涵盖的策略跳过项，不是617测试被skip；继续区分policy_skipped与测试skipped=0。
- required checks仍未配置；准备独立可审设置方案与明确授权，不把CI绿灯当强制保护已启用。未启用自动合并/Auto-Fix，WF-006/007保持PLANNED；fork、一方认证ZIP下载、JAR独立字节计算、runner内部、人工/容量/发布门槛仍开放。

## 本轮边界

本轮只合流已审内容、核验云端main并补纯文档；不改生产Java/规则/随机源/存储/协议/依赖，不启动本机JVM/全量。归档来自main干净分支docs/wf005-maintenance-merge，保留原137项porcelain与136文件hash；后续随正常开发批次同步文档，不为每次补一段验收结果循环创建维护PR。

## 本轮main最终验收

[run40177191539](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40177191539)，attempt 1，push/main@dbbf884，completed/success；页面总时长15分46秒（946s）、verify任务15分42秒（942s）。证据base_source=push.before、base=8ef3240、head/tested=dbbf884，tier=full。不是合并前PR run40175462834的旧结果。

20项suite/internal全部passed：diff-check；guarded15、audit14、verify32、ci-tests55、image-tool17、audio-tool18、exchange3、frontend-unit189；web-build；assets4、audio27、files-repeat30、default-e2e136；browser-file；java-full617、java-results、oracle-audit、gate15、desktop-exchange1。测试failures/errors/skipped/cancelled/flaky/interrupted全0；first_failure=null、unexecuted=[]，六项manual门槛保持not_covered。policy_skipped中的oracle-generate不等于跳过实际测试，full的java-full已生成当次oracle。

主artifact ID12593123421，ZIP1827027字节，SHA256 e20f1cb1d3752aaf8bf0a3d508eae93da2158366c6f52a86bf39108ed48ccf34；receipt ID12592908811，ZIP465字节，SHA256 600619b30aaea09563ffef2d6d46fc29346c86cd76fedc3dbf5ef64b11bcbb2a。

本轮REST限流尚未解除，未提取token、未修改凭据。以上ID/产物名/完整digest与push/main/成功/head均由已登录GitHub一方运行页面观察，保存AX文本和截图；ZIP经公共nightly.link下载，逐个SHA256等于页面公布值。元数据标注UI转录，不冒充REST响应，字节长度来自下载ZIP，不宣称API独立长度核对。用当前仓库download.validate按revision4完整重组，包内evidence.json全部字段精确一致；再严格核对20项数量/零错误/四组指纹以及回执run/attempt/head/artifact ID/digest/URL关联。SHA256身份与Git对象主线证明独立于nightly.link；一方认证ZIP端点、合流后REST独立读取仍未验证，限流解除后可由独立审查按run ID补核。

Java225文件指纹ff7cb30010c0ad6ffc53c890346d134d2ab0f45c7da22c34960f7b81df6f1741，ref/worktree一致；frozen9/6ce8aa54、inputs1979/3d4c43fa不变；build_policy44/9ceeeb03与PR #2最终干净树完全一致。tools.json记录git version 2.55.0、git_diff_rename_limit=null。FatJAR记录724197819字节、d7232b8cc37d0c761924936b496c3b055866021aef372ae18681d31bd94d18a0，与前轮云端记录相同；未下载JAR，不宣称亲自计算其字节hash。

本轮本机只做Git对象证明、文档事实/链接/diff-check与工作区保护核对，没有重跑55/32/14或启动JVM/完整浏览器；这些前轮本机结果仅引用。脚本、UI元数据/原文、ZIP、重算报告和receipt在源仓库忽略目录build/wf005-maintenance-merge；当前文档单独本地归档，未为收尾再推一次full。源工作区137项状态与136文件hash保持原值。

下一项仍为WEB-036（READY，未实施）。维护PR已合流，主线已包含归档和工具补修；required checks待独立具体设置授权，WF-006/007及人工/容量/发布门槛不变。收尾文档后续随正常开发批次同步，不循环创建仅补一个验收状态的维护PR。
