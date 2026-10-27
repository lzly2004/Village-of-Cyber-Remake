# WEB-040 首审及主线轻量核验处置

范围：42e71da→4257d93（4250276、4257d93，PR #8，2提交/17唯一文件）；merge commit 4ec4758，父42e71da和4257d93，树e8a924e280a49f0d2f3d8487c83bce31484f3efd等于已审head。首审后无追加提交，首审及主线核验均无阻断。

## 意见处置
- 阶段白名单：核对BusinessPhase实际仅DAY_DECISION/FINISHED，不能与PresentedEvent画面阶段混用。未来扩展业务阶段须同步服务端、客户端及契约测试，046/049复核。
- 成功状态：API路由表已有创建201、删除204；041集中补充方法/状态总规则，供042/043代理复核。不改既有成功状态。
- 专门409 INVALID_STATE浏览器断言原先缺失，单测和STALE_REVISION不冒充该覆盖；041正常批次补受控拒绝→单次同步、不重放的用例，其实际结果在041记录，不写成040已跑。
- build_policy由de3ed92f变为6d6d9721，package测试入口是策略输入；inputs3d4c43fa不含web-client。无须修改分级策略。Java未变，628是full政策重跑，不是040新增Java测试。
- 主线观察1：本批更新040状态为已随PR #8合入main4ec4758。归档随041正常批次入仓，不在040首审后追加未审head。

## 当次主线证据（不是041重跑）

[run40441562284](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40441562284)，push.before42e71da→tested4ec4758，attempt1无重试，full21套件全passed、revision4、first_failure=null、unexecuted=[]。Java62XML独立重算628/0失败/0错误/0跳过，前端199、默认176、gate15、assets4/audio27/files30/production1/desktop-exchange1及工具套件通过。

主ZIP id12707113308，3,820,335字节，SHA-256 e7d3232bb6c6ac055a5016671e2bc535b30ad320ac0034552e581560e95d849f；回执id12706659877，463字节，SHA-256 8807d626422b3cf4f90a77fe891d4cf3555137b7b2ba88b9cf965bf1951d750a；身份与内容绑定一致。Coder取得REST元数据、公开ZIP并用download.py在线元数据路径复算；CR因REST403另用页面元数据、公开ZIP及离线重算，包内证据逐字段一致，不混称认证ZIP端点验证。

Java3f33b50e（226）、frozen6ce8aa54（9）、inputs3d4c43fa（1979）、policy6d6d9721（45）与PR head相同；FatJAR报告724,197,819字节/d7232b8cc37d0c761924936b496c3b055866021aef372ae18681d31bd94d18a0，未下载724MB云端JAR独立重算。main源未变、web源集不进入桌面包；不确定归因本机/云端hash差异。

job总1649秒；Coder保存jobs-final.json中字体步骤02:59:53Z→03:15:57Z（964秒），曾长时间无新增输出但最终自行完成，无取消/重试。CR仅独立核对总时长，未分解字体步骤；其报告称套件“合计约200秒”不作为精确总量依据，原日志default-e2e单项即208.042秒，另有gate181.351秒及其他串行套件，应按各套件字段分别求和，不用该概括估算准备时间。

Coder原工作区137 porcelain/142展开项、136文件SHA保持，无checkout/reset，只对齐未检出的main/origin-main，已检查无工具链残留。CR因/home限制未读本机包、未复跑/抽样保全，模型不能读图；不把文件存在/hash核对写成独立目检。原始包位于build/web040/main-merge-ci/main-handoff.md及原项目忽略目录镜像。

## 开放项
fork、认证ZIP端点、云端JAR独立字节、真实长断网、100容量/挂起请求、代理限流与日志仍待；A06/A07/A08、050、046/047/049人工容量发布门不放行。用户在040主线核验通过后指示进入041；合流后未另加提交，本归档属于041正常批次。
