# WF-005：证据包与独立CR交接

当前：WF-005完成。本轮0b28449真实PR红灯已给出24处/3文件摘要；独立空白提交3b8641c在真实PR run40153989566完整通过，原取消push补验run40153986129 attempt 2也完整通过，两个最新verify均success、PR状态clean。[草稿PR #1](https://github.com/lzly2004/Village-of-Cyber-Remake/pull/1)仍未合并，main仍6163620。证据包/回执与逐项处置见[本轮归档](review-f089bda.md)；历史红灯/交付见[前轮归档](review-2fb4edf.md)。流程与绿灯不等于主线合流或发布放行；WF-006/007仍PLANNED，WEB-036仍暂停。

## 产物与身份契约（schema 1）

统一入口仍生成原result.json，不修改它的schema。CI在同job结束后生成build/ci/evidence.json、summary.md，随后和原context/plan/tools/outcome/result/log/XML/交换文件一起上传verify-<run_id>-<attempt>（14天）。只读取明确列出的证据输入，不扫描工具缓存；不上传JAR、.git、用户数据或环境文件。

- base_sha、head_sha、tested_sha、trigger、base_source必须与context/plan/result一致；status=passed还要求outcome通过、计划项全部按序通过、result与stdout相同、result/log哈希一致。缺/坏/错SHA/错顺序/缺项明确拒绝；中止/准备失败没有实际测试结果时不填零项通过。
- suites保留argv、退出状态、耗时、run相对log路径/hash；counts注明node-tap-log、python-unittest-log、playwright-log或Java XML来源。无法可靠解析时为null，构建/内部检查只证明退出状态，不伪造测试数量。Java从TEST-*.xml重新统计，并核对testcase与失败/错误/跳过子项，不只相信根节点计数。
- first_failure提供suite、可识别的首条用例、首行错误；识别不了为null，原日志仍在。摘要截断、转义、移除URL，不把日志当指令。unexecuted_suites与路径策略未触发项分开，不能把未跑当skip=0或通过。
- fingerprints与eol_policy照录输入审计；LFS保留声明OID/大小和ASCII排序的`oid TAB bytes LF`聚合、指针/唯一payload口径。HTTP请求计数、账户计费未知为null，不能用对象数替代；工作目录有效payload也不冒充独立远端证明。存储路径为CI使用的.git/lfs/objects。
- manual_gates固定为A06/A07/A08、046正式浏览器/DPI/辅助技术/人工音频、049发布前人工复检、047容量/长局的not_covered。即使原输入报告声称通过，本工具也不自动关闭这些门槛。

新生成的schema 1证据使用`assembly_revision: 4`：无result的准备失败/取消分支也输出空的unexecuted_suites与policy_skipped_suites（空数组表示该分支无可核验result清单，不表示所有计划已执行）。revision 1/2/3各按原规则精确复算，revision 3的旧准备失败包继续保留原缺键。revision 3引入：not_covered按首次出现顺序去重、Markdown链接为run→artifact→ZIP digest，另对diff-check套件聚合Git的尾随空白、Tab前空格及EOF空行诊断，记录首个文件:行号、问题数及文件数。同一行合并原因也可识别；输出有界且经摘要转义。revision 2保留原失败摘要（包括diff-check的null），缺修订字段的revision 1保留原重复项及无新增字段。下载器按明确修订规则重组，与包内报告全部字段完全比对；不先归一化或忽略旧字段，未知修订拒绝。没有包内报告的WF-004包使用当前规则重算。

上传后才获得artifact ID/URL/digest。因此使用单独receipt-<run_id>-<attempt>保存receipt.json，并写入job summary；不把ZIP自身hash嵌回该ZIP造成自引用。回执明确关联run、attempt、tested head与verify artifact，消费者仍须从GitHub API读取digest并核对实际ZIP。receipt文件不是游戏/Replay格式。

## 进度与失败

子套件退出码不做全局语义映射：例如Git diff --check的exit2记为该suite的failed，不等于统一入口的“拒绝”exit2；应结合suite状态、reason及命令解释。下载器对合法失败证据返回1，对完整性/输入拒绝返回2。

CI wrapper保持一次串行调用verify.sh；子套件输出仍进原日志。仅开启VOCR_VERIFY_PROGRESS=1时，verify在stderr输出suite_started/suite_finished及verification_finished JSON，stdout保持最终JSON。开始项先落盘为running；CI wrapper实时tee stderr到控制台与verify.stderr，保留原退出码，不重试、不放宽断言、不自动改源码。进度不是套件通过证明。

证据汇总与原产物上传为always；已有suite失败仍保留失败，不会因成功打包证据而改成绿色。汇总自身校验失败会写失败诊断并返回非零。checkout/工具安装等在汇总脚本可用之前就失败时，不能保证一定有完整artifact；以真实job步骤日志为准。上传失败也不制造回执。

VOCR_APPROVED_FULL_CHECK只在Execute once step env，其他准备/上传步骤不继承。该step的测试子进程仍会继承，因此拒绝夹具继续显式清除。现有安全入口、单worker、2GiB门槛与本机逐轮全量授权不变。

## 一方读取与离线复核

在线例子（EXPECTED_SHA必须来自此次要审的提交，不能拿旧绿灯自动代替当前HEAD）：

```bash
python3 -B tools/ci/download.py \
  --repository lzly2004/Village-of-Cyber-Remake \
  --run-id RUN_ID --expected-head EXPECTED_SHA \
  --output build/review/UNIQUE_RUN_DIRECTORY
```

自动下载只走GitHub REST；读取已有GH_TOKEN/GITHUB_TOKEN，所需为该仓库Actions只读。SSH身份只支持Git操作，不能替代API认证。工具不创建token、不提取浏览器凭据、不安装gh、不自动回退第三方。401/403、限流、410过期、网络失败各有明确诊断；不要把token贴进聊天或放进命令参数/仓库。

如果已有GitHub网页或gh下载的ZIP，可追加`--zip LOCAL_ZIP`：仍从GitHub API实时读run/artifact digest和当前源分支HEAD，但不调用需要token的ZIP下载端点。HEAD已推进、run未完成、attempt不符或digest不一致均拒绝；复核后的合并/发布前仍应再读最新HEAD。

完全离线模式必须同时提供`--zip ZIP --run-json RUN_JSON --artifact-json ARTIFACT_JSON`，后者为单个Get-an-artifact对象，不是列表响应。离线仅证明“给定ZIP与给定元数据一致”，**不证明元数据来自GitHub，也不证明当前远端HEAD**；输出明确标注metadata_origin和head_freshness。旧WF-004 artifact没有evidence.json时也可重算，属于历史证据复核，不是重跑那些测试。

ZIP有压缩/解压总量、单项、条目数上限，拒绝越界、重复/大小写冲突、符号链接、加密及意外目录。只在内存读取指定数据项，不提取、不执行其中代码；GitHub认证不随重定向转发到blob主机。输出目录必须不存在，失败后保留已写证据；重试用新目录，不覆盖旧报告。退出0=所请求SHA证据通过，1=合法失败证据，2=认证/网络/完整性/输入拒绝；原CI失败不会被下载工具变成通过。

在线输出run.json、artifact.json、evidence.json、handoff.md和（自动下载时）artifact.zip。所有计数均可回到来源日志/XML；API ZIP digest与内部日志hash各自校验。API/凭据限制不等于测试失败，缺证据也不等于测试通过。

官方接口：[GitHub artifact API](https://docs.github.com/en/rest/actions/artifacts)、[固定上传Action](https://github.com/actions/upload-artifact/tree/cf430e030ddbb5b0abf93d22962f4752f3646cd9)。API下载权限与302限时跳转已核对；实现保持现有contents:read CI权限，不为下载器暗增远端写权限。

## CR和PR流程

1. Codex完成代码/适用定向测试，生成本地PR正文草案（问题、最终行为、实际验证/未覆盖、真实base/head）。取得具体分支/提交推送范围授权后才发布；本机Java全量仍另批。
2. 由真实push或PR的base/head触发分级CI。读取准确run/attempt，核对最新HEAD与tested SHA；完整与docs等不同层级分别说明。不能用旧完整结果假称最新文档提交重跑了全门。
3. 独立reviewer读diff、summary、原日志/XML/输入指纹，必要时定向重跑或反向验证；自动化绿灯不代替语义审查。可在CI失败时先审范围与代码。
4. 用户批准具体PR创建后，才创建草稿PR并提交已审的正文；仅附摘要和证据入口，不复制全日志。当前工具不发评论、不调用AI、不生成/提交补丁、不合并。
5. 补修后CI再验，检查当前HEAD；最后由用户决定合并。真实PR/fork事件的base_source应为pull_request.base.sha，不能换成HEAD^或伪造main基线。

main仍是旧基线，准备分支包含已审长历史。首次真实PR的3文件24处空白红灯证据保留；用户已另行授权本轮独立空白修复、本机完整验收及准备分支推送，最新3b8641c真实PR完整通过，未放宽diff-check。PR原正文已更新，不额外发评论、邀请reviewer或合并；主线整合仍由用户决定。

同SHA三次full是WF-007 Auto-Fix前置，不把一次成功当无flake证明；尚未执行。首次真实frontend层CI时记录实际成本，不靠人工触碰生产路径凑样本。以上未完成项不得勾选为实测通过。

工具环境记录git --version和有效的diff.renameLimit（未显式配置为null，不猜测内置默认值；0保留其真实语义）。只读查询，不修改配置。跨历史区间diff-check以当前CI的Git环境为验收依据；本地不一致时先比版本与重命名配置，保留差异证据，不改门禁或用旧绿灯替代新head。
