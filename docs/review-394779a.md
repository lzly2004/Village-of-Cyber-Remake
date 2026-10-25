# 主线merge commit合流与首次CI验收

## 授权和范围

2026-10-25用户明确批准“把PR #1从草稿转Ready，使用merge commit保留全部169提交合入main，核验main首次完整CI；不改仓库设置、不启用自动合并”。这是自动审批拒绝转Ready后获得的新增明确授权，没有绕过该拒绝。前轮P3补修394779a与归档ddd86b1仍仅本地，不混入已审PR head。

本轮为已审历史合流及文档收尾，不新增生产Java/规则/协议/存储或依赖改动，不执行本机全量。云端确定性完整验收沿用已获授权的既有安全入口、单worker/串行/2GiB门槛；未修改workflow或门禁。

## 合流证明

转Ready前实时一方API确认PR #1 head=3b8641c、base=6163620、draft=true、mergeable_state=clean，最新两个verify均success；随后按界面“Create a merge commit”完成Ready→Confirm merge。页面明确“Pull request successfully merged and closed”，未删除准备分支、未改设置/secret或开启自动合并。

main合并提交8ef3240cd2dd99810f4dc1cf738492e8c67045d8。通过SSH fetch只更新本地origin/main跟踪引用，独立Git核验两个父提交依次为616362073999cb280ef8891a8f69272541904357和3b8641c3bf6e4bcc98cda8fa738862d702c738c0。第二父相对第一父的169提交全部保留；合并树c9c76a936e98efc4e8f572f87e9ff166888ed5ec与已审3b8641c的树完全相同。没有squash、rebase或强推，没有检出覆盖原脏工作区。本地工作分支保留其未推归档和P3提交，不冒充已经同步main。

## main首次完整CI：本轮实际云端执行

[run40157766560](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40157766560)，attempt 1，push/main@8ef3240，成功。页面总时长10分8秒，verify任务10分4秒。证据base_source=push.before，base=6163620、head/tested=8ef3240，完整旧main→新main区间的原diff-check通过，不引用准备分支的旧绿灯替代。

20项全部passed：diff-check；guarded15、audit14、verify32、ci-tests53、image-tool17、audio-tool18、exchange3、frontend-unit189；web-build；assets4、audio27、files-repeat30、default-e2e136；browser-file；java-full617、java-results、oracle-audit、gate15、desktop-exchange1。各测试失败/错误/跳过/取消/flaky/interrupted均0；first_failure=null，unexecuted=[]。这次是main实跑；本机没有重跑这些完整套件。main工具为revision 3，394779a的55项/revision 4仍是前轮本地P3结果，不误写成此次云端53项的版本。

主artifact12584853073，2236258字节，SHA256 70b03729b026f857d037129d0d63a6b8e2e2f8b562a24c73197251219637b1b5；receipt12584703183，467字节，SHA256 d63697e5b077b106791b5ae2644c736de85a4e49c89d8ac6b29a11715f774031。

合流后公共REST API返回403、remaining=0（reset_epoch=1792899413）；未提取浏览器token或绕开限流。改从已登录GitHub一方运行页观察main/head/成功/attempt产物名、两个artifact ID与完整SHA256，并保存AX文本和截图。ZIP从公共nightly.link取得，逐个SHA256严格等于一方页面公布值；元数据标注UI转录，不伪装成GitHub REST响应。size字段来自下载ZIP本身，不称为REST独立长度核对；内容身份由一方UI的SHA256保证。当前下载器validate严格检查有界ZIP/全部包内hash，并按原revision 3逐字段重组evidence.json；20项计数/错误/指纹全部核对，回执run/attempt/head/artifact id/digest/URL关联一致。

补核：用户随后要求继续，限流已解除。新鲜一方REST run/artifact元数据确认main push@8ef3240 completed/success、attempt 1，两份ZIP精确字节长度与API digest均匹配。直接用保存ZIP和新REST元数据再次validate，包内报告完整逐字段重组与UI路径结果完全一致（仅download_proof来源说明不同），20项计数/指纹/回执关联复核通过；最新API确认PR merged=true、draft=false、merge_commit_sha=8ef3240，main ref仍为8ef3240。run开始UTC03:19:13、结束03:29:21，即北京时间11:19:13–11:29:21。一方认证ZIP下载仍未验证；实际ZIP来自公共nightly.link，不能混称。原始ZIP、UI转录及REST元数据、源码证明、校验脚本和两次复算结果保存在忽略目录build/wf006-main-integration。

## 指纹、oracle与工作区保护

云端与本机既有干净树的四组指纹完全一致：Java ff7cb30010c0ad6ffc53c890346d134d2ab0f45c7da22c34960f7b81df6f1741；frozen 6ce8aa549a2f49923b763b04d819d3ed29870c9bbd0c30745e97c08c3beca8b8；inputs 3d4c43fae21fbb7db0a68cdaa44db66aeaa8478760853479f6a539abfcabbe4a；build_policy f355d13a9958e7dbe878f9920d950497da5d3c6e774e61db5e9e463662d264b2。本轮本机只读require-clean/LFS/oracle审计通过，15份本地oracle全部matches_ref=ff7cb300；结合main与其3b8641c树完全相同，不需再因纯merge提交重生成同字节源码oracle。

云端FatJAR记录724197819字节、d7232b8cc37d0c761924936b496c3b055866021aef372ae18681d31bd94d18a0，与前轮云端记录相同；未下载724MB JAR，不声称独立计算其字节hash。六项manual_gates仍not_covered。

源树137项porcelain及136文件hash与原基线完全相同，索引只选择性提交文档；不整体暂存、不全仓规范化、不结束用户应用。本轮未启动本机JVM。

## 决策与下一步

主线现在已拥有并实际执行新工作流。保留原准备分支，required check/分支保护暂不调整：本次授权明确不含仓库设置，后续需单独给出配置方案。WF-006/007可选试点仍PLANNED，不把三个不同提交/触发的绿灯当WF-007三次同SHA观察，不启用Auto-Fix。

WEB-036恢复原READY顺序，但本轮不混入功能实现。开始下一轮功能前，应从main干净分支开展；394779a及本地归档需独立审查后以维护PR同步，不直接推main、不将它们误认为已合流。人工音频/物理设备/正式浏览器DPI、容量、fork及发布批准仍开放，新工作流投入使用不等于产品发布放行。

收尾自纠：最初工作区保护断言因本轮解析器导入产生两个未跟踪__pycache__字节码而拒绝；逐个核实后只删除这两个本轮新产物，137项状态和136文件hash恢复原值，未删除用户文件。临时/tmp入口已不存在，改用持久证据目录脚本；补核时已有ZIP已匹配REST，为免冗余下载，精确识别并终止自身下载脚本（exit143，非测试结果），随后仅用保存ZIP和新REST元数据离线复算通过，不把被终止脚本算通过。
