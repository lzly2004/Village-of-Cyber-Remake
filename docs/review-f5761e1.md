# WEB-041 首审与 PR #9 主线核验

首审范围 `4ec4758 → f5761e1`（3607ccf、f5761e1，2提交/19唯一文件）；主线核验范围为 merge `7126569c34fa5fdeeb8daaff69bd81c2b5399854`。两次独立CR均无阻断。本文随042正常批次归档，不向已审分支追加提交。

## 事实与来源

- PR #9 以 merge commit 保留两个提交；父依次为4ec4758和f5761e1，树 `ae52dd87fb5aaec8ccf5e1748b6579435543781c` 与已审head相同。CR另以PR/merge diff逐字节比较（66,747字节）交叉核验，无合流编辑或首审后新提交。
- 首审run `40470077004` attempt1，pull_request，full21项通过；主artifact id12718169712，2,390,357字节，SHA-256 `937c89421a1d79eec3a76dacde45289f540e0fe6c4ba4f25fdc9c38a58d104cc`；回执id12718621904，465字节，SHA-256 `a418ae757c97c79f65369b11578f1b567d6368a22c007fdca1d80616ed40e5fe`。
- main run [40479202599](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40479202599) attempt1，push，base_source=push.before，base4ec4758、tested7126569，job1001秒（16m41s），总运行页1005秒。无取消或重试。
- main主artifact id12723435812，2,393,057字节，SHA-256 `7d278169e1945a7f5128e5b710e099843c8f36d7b4cc896475abb7e10dfcda4a`；回执id12723142059，466字节，SHA-256 `1f3116dd394cecc8a861586760834e4c66ac088043c84c68fc91a7a3685b23e6`。回执绑定run/attempt/id/digest/tested_head/URL，Coder及CR均复算通过。
- main full21项通过：Java648（65XML，0失败/错误/跳过）、前端200、默认180、gate15、assets4、audio27、files-repeat30、production1、desktop-exchange1，工具15/14/33/56/17/18/3。首审与main计数一致；628/199/176为040旧口径，不能记成041结果。
- Java231文件指纹f4d94f7a…9ae2、frozen6ce8aa54、inputs3d4c43fa、policy45文件6d6d9721，main与已审head相同。云端FatJAR724,197,819字节/d7232b8c…，值来自CI报告，未独立下载724MB文件计算。
- Coder首审取得公开REST元数据；main最终REST读取遭403，改用一方页面id/完整digest、公开nightly.link ZIP、供应元数据离线重算及SSH实时main核对。CR两轮均因REST403使用页面元数据/公开ZIP离线复算。页面转录不是REST响应或签名，离线工具不验证实时head；没有冒充认证ZIP端点验证。
- Coder在本地Git对象核对父链/树，安全对齐main/origin-main引用，不checkout原工作区。137 porcelain/142展开项及136文件hash不变，无本工具链残留。初次本地fetch拒绝未公开裸SHA，改用命名引用完成，未放宽Git设置；不属于CI或测试重试。

## 首审建议处置

| 项 | 核验与处置 |
|---|---|
| P3-A 未知路由404名额归还 | 当前由固定Javalin7.2.3 after语义及已有异常路径测试支持；CR只读核过sources。采纳直接回归候选：连续超过全局并发上限的未知路径404后，正常请求仍可进入。归047或下一次框架/HTTP准入改动，不谎称041已直接测试该序列。 |
| P3-B 429代理来源边界 | 采纳部署前约束，归043/044：代理不得仿造REQUEST_LIMIT业务码，不能把可能已执行请求改写成“未执行”；代理限流使用独立错误形态。same-origin或文案不能证明业务响应来源。尚未部署验证。 |
| P3-C lifecycle历史措辞 | 确认039“未修改全局日志配置”有时点歧义；本次正常文档批次补039历史时点与041安全文档链接。未在合流前修改已审head。 |
| P3-D HEAD说明 | 固定框架HEAD200空体、不执行GET成本handler已测；不扩大推断为所有HEAD都免受全局/容器限制。未来框架升级复核before/after和wrapper。 |

main核验另建议任务清单41改为已合流，本次随042同步。CR未提出需要补交的阻断材料。

## 未验证与后续

CR因本机挂载/沙箱限制未复核Coder本机full/打包或原工作区保全，模型无图像能力，未目检截图；不能把Coder证据写成CR独立验证。043/044代理/HTTPS/日志、047真实100人/256MiB堆/挂起连接/8MiB Replay读取、050外观/日文化、046/049人工与发布门仍开放，Q04未放行。fork、认证ZIP端点、云端JAR独立字节、真实长断网未覆盖。

原始证据与逐项处置位于忽略目录build/web041（含main-merge-ci、review-disposition）；本归档不将旧648/200/180/15说成042重跑。用户收到主线通过报告后授权继续042，042从main7126569干净LF分支实施。
