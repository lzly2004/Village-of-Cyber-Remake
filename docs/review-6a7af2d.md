# 工作流最终收尾与日常交接

2026-10-25用户要求完成全部必要收尾并说明所有日常分支。本轮仅文档/引用/远端治理收尾，不混入WEB-036、生产Java/依赖/规则/协议/存储变化，不增加新工作流子系统。

## 评审采纳与校正

PR #2合流通过、4提交保留、main@dbbf884、run40177191539及revision4/Java617/前端189/默认136/gate15/CI55、policy9ceeeb03均沿用已审事实，不记本轮重跑。未推归档6a7af2d迁移为f3353c4，从最新main建立docs/workflow-closeout，补本轮简明记录和日常流程指南。

本地main此前1332d3b，本轮核对未检出且可快进，先保存refs/archive/workflow-closeout/main-before-alignment，再以预期旧值更新到已验dbbf884；原脏工作分支/137状态/136hash不动。不强删含未推历史或正在使用的分支，已完成的分支保留archive命名；master和未知分支不动。

评审“所有真实CI均full”并不完整：前轮已有docs层push证据（如40082353246/40077922182，既有归档记有docs结果）。本轮仍用真实新增纯文档PR和main push再次验证增量docs路径，而不是造假前端改动凑层级。tooling/frontend层已有契约，首次独立真实云端执行仍按实际工具/前端任务触发，不能在没跑WEB-036时称其已验；不因此阻塞文档层闭环。

## 本轮治理目标与权限

准备main经典保护：Require PR、Require GitHub Actions verify、Require up-to-date、Do not allow bypass（含管理员）；审批人数0，不开签名/线性历史/部署门/自动合并/force push/deletion。官方语义已核对；这既防忘看红灯，也配合必须PR限制直推。保存前由用户批准具体范围，保存后以一方页面复读和真实绿灯PR合流核验，若无权限/失败则不声称生效，不绕过审批。

文档和治理需要一次明确发布/设置/合流授权；不复用PR #2只合流的旧范围。新文档分支不匹配自动push事件，真实PR触发docs；合流后main push再验docs。仍不更改verify-tiers或启动本机JVM/浏览器验收。

交接采用[日常工作流](workflow-daily-guide.md)，覆盖范围/权限/环境、分级、CI通过失败取消、证据不可读、独立CR、过时base、合流后失败、可选规划/Auto-Fix与人工发布门槛。最终run/设置截图与回执后续附在本轮证据目录，不为追加最后一行再创建下一份收尾PR。
