# PR #3评审处置与WEB-036接续

本轮采纳PR #3通过结论；合流d04b2de、双亲与树相等、docs层PR/main证据复算均为上一轮已验事实，不记本轮重跑。耗时区分job 29/27秒与run 32/30秒，不矛盾。Reviewer更正“过去全部full”：docs早已实跑，tooling/frontend独立真实路径仍未全覆盖。

P3-1采纳：设置页历史AX原文和SHA-256随workflow-protection-evidence.json入仓，包含观察时间、来源、设置摘要和截图hash。公开API401不能独立核验保护；导出不是GitHub签名证据，也不冒充实时API。未故意制造主线红灯。

用户指出上一轮仅回复未修改指南，属实际遗漏。本轮先修改workflow-daily-guide.md：角色明确到节点，补用户→Coder→用户→CR→用户→Coder交接表、首审/补修/合流包必备字段、复评及合流后CR核验；不将Coder自查当独立CR。后续按此交付，不自行合并WEB-036。

按原顺序实施WEB-036浏览器累计战绩存储；WEB-037统计页面不提前实施。原NTFS脏树不改，从main@d04b2de干净LF分支实施。
