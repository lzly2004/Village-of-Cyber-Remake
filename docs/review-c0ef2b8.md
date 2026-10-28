# WEB-042 首审与主线核验归档

## 范围、结论与来源

用户转交的独立CR首审范围为main7126569→PR #10 c0ef2b8（bf1c4b2/7d787eb/c0ef2b8，3提交/25文件），主线核验对象为merge commit5ffc627；两次结论均通过、无阻断。CR通过官方页面/REST及公开ZIP核对身份、hash、清单、回执，并从patch重建web_release.py运行check_release、1262项sha256sum；未运行仓库download.py离线复算，不能归档成CR已运行该工具。

首轮PR run40505760373失败记录保留；c0仅测试/文档补修两个易变墙钟字段，受控反向例证明其余内容仍严格比较。最终PR run40508433772 attempt1为full22通过。用户随后明确授权Ready、merge commit及mainCI，合流前无新增提交。三个小提交保留，父链为7126569+c0ef2b8；merge tree与已审head同为d5566dc54c983eecda6ba1803eec519a36aef59a，零合流编辑。

## main实际证据

- main：[5ffc627](https://github.com/lzly2004/Village-of-Cyber-Remake/commit/5ffc6272763c9f70a9804393004352ae7e0c0299)；[run40611885642](https://github.com/lzly2004/Village-of-Cyber-Remake/actions/runs/40611885642)为push/attempt1/success，base_source=push.before，full22/22通过，job901s、整体904s，无重试。
- Java650（65XML、0失败/错误/跳过）、前端200、默认180、gate15、assets4/audio27/files30/production1/desktop1、工具15/14/35/65/16/17/18/3；新包实际解包整局冒烟1/1。Java指纹9809ec71、policy49文件281e793b，与已审PR一致；本机旧快照f4d94f7a不混用。
- verify id12784001031，4,544,538B / bd06cf495f7d81dfec5c8ec8beb62af60681f7956eb905954fe6db34fa0cbe0d；receipt id**12783846563**，464B / 0a8a80413e90d853a4a82e9b1fa31c688a85f6fc1141a9084f953308b206030d；release id12784095822，85,991,156B / 5d8212b7ea25d5e18da0463566fba49e477fe0559f2b2504bb4024762307aee6。CR主线报告receipt id的“1278400856x”为笔误，以实际REST元数据与回执为准；不影响其hash/head绑定结论。
- 内层main ZIP86,044,660B / e005868fbafd9925d5466d6f73b71906bcb485f284f3ce03838d1a5cedf47779。相对PR包增加7B不构成内容漂移：source.sha与验证目录不同，前后端文件集及预算一致。该差异已由CR逐项核对。
- Coder另用仓库download.py在线REST复算通过、回执绑定及发布二进制只读check通过；未在本机执行下载的云端二进制。FatJAR724,197,819B/d7232b8c来自本次云端报告，非独立下载724MB字节复算。

## 非阻断项处置

| 项 | 决定及归属 |
|---|---|
| P3-A run.cmd端口语义 | 保留候选，045接触Windows操作说明/回滚时对齐1—65535；043不修改已审launcher，原生Windows执行仍未验证 |
| P3-B 静态服务与HTTPS部署顺序 | 043实施：后端回环→静态前端/同源代理→本地TLS→origin隔离；用户已选localhost，不购买或暴露公网 |
| P3-C runtime声明信息 | 043文档明确版本/依赖声明与文件SHA、runtime stamp、同次验证/上传身份的区别，不宣称check_release已校验所有声明字段 |
| P3-D sticky输出记录 | 现有构包前FRONTEND_OUTPUT_DRIFT拒绝保持，非问题 |
| 合流前状态文本 | 42行在本批同步已合流5ffc627，不为归档追加未经增审的合流前head |
| 指纹时点 | 一律以同run生成的指纹为准，本机快照/PR/main的tested/source不互相改写 |

## 保全与未验证

Coder核对原工作区137porcelain/142展开/136文件SHA不变，仅对齐main/origin-main引用，未检出覆盖；CR本机无挂载，未独立核验这些保全或本机full。截图Coder可目检，CR模型不能读图，不写成独立视觉验收。认证ZIP端点、fork、原生Windows、真实公网TLS/DNS/代理、人工音频/视觉/DPI/辅助技术、容量与长局未验。A06/A07/A08、Q04及046/047/049保持开放。

详细Coder日志/JSON/ZIP/截图/完整核验包集中于忽略目录build/web042；当前043是最前READY，完整验收与发布仍按本轮具体授权纪律执行。
