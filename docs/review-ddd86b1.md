# ddd86b1复评与主线整合准备

2026-10-25收到外部评审：0b28449/3b8641c/ddd86b1通过，无阻断。评审者复算的617/189/136/15与上轮云端证据是引用，不是本轮重跑。

用户决定保留全部小提交，其余方案由coder选择。采用merge commit，保持169个历史提交；合流前只读实时核对PR #1仍head=3b8641c、base=6163620、draft=true、mergeable_state=clean、两个verify均success。不把本地归档ddd86b1或本轮未推工具补修冒充PR已测head。

## 评审建议处置

- P3-1采纳：bootstrap只读记录Git版本及有效diff.renameLimit；未显式配置记null。错误返回码或非法数值明确拒绝，不改本机/runner Git配置，不改diff-check命令。
- P3-2采纳：新assembly_revision 4在无result分支输出两个空列表。旧revision 1/2/3规则不变，下载按包内修订精确重算。不能为补字段静默改写旧取消包；空数组也不等于执行完成。
- JAR观察采纳为同环境旁证：本轮外部报告称修复前后云端记录均d7232b8c…，结合独立3文件24行−240字节证明，支持空白无逻辑变化。未下载724MB云端JAR，该hash是wrapper/证据记录，不夸大成亲自逐字节比较，也不把跨工具链本机2e9a…差异当行为变化。
- 英文机器摘要保持，不新增本地化。
- 旧包兼容文字更正：revision 2保留旧null；revision 3识别Git诊断。报告结论段“旧失败包按rev3复算”不能据此覆盖已发布rev2字段。

## 本轮实际轻量验证

python3 -B tools/tests/test_ci_workflow.py -v：55/55（0.054s），其中证据29项为子集。新增Git版本/未设置/显式0与10000/读取错误/非法值，以及取消无result在rev1/2/3/4逐字段下载重算，保留failed、无伪造套件。工具成功启动命令/并发/内存监护未变，不启动本机JVM、游戏或完整Java/浏览器验收。

用当前下载器重新核验保存的真实rev3包：PR40153989566 passed、红灯40153657465 failed、push40153986129 attempt2 passed，三份完整报告均精确重算。此为包解析重跑，不是其测试重跑。

## 主线操作边界与待办

GitHub自动审批拒绝“Ready for review”：理由是此前保持草稿限制，当前仅明确选择merge commit仍不足以覆盖PR状态变更。已请求明确授权Ready→merge commit→main首次CI；未绕过拒绝，不通过Git/API间接合流。收到授权前PR/main保持原状。

P3补修独立本地提交，不塞入已审PR head；合流获准后先验证原3b8641c对应合并提交的main完整CI，再以新分支/PR交付工具补修。required check暂不调整远端设置，当前同一verify名称覆盖不同事件，需后续单独配置方案；不靠此项阻塞已审代码的手动合流。WF-006/007保持PLANNED，WEB-036在main首次CI通过后才恢复；不在本轮合流之前混入功能开发。

人工、容量、真实fork、一方认证下载、runner镜像内部和发布门槛仍开放。原137项工作区差异保持，禁止整体暂存或全仓换行处理。
