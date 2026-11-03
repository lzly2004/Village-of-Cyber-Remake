# WF-008 设计评审处置

范围：用户转交的WF-008设计评审，设计提交aa37aa5；结论为“可以开动、无阻断”，要求首版实施计划固化8项。用户随后指示无新增必须决策即直接实施，并已逐轮授权本机最终验收与打包。本记录不表示独立CR已审实施代码，也不表示用户整局试玩已完成。

| CR要求 | 本轮处置与落点 |
|---|---|
| preview schema与full互斥 | `playtest_candidate.py`定义schema1、kind/文件集/hash/runtime/result绑定及双向拒绝；新工具测试覆盖坏包和冒充，正式full契约不降级 |
| 端口与互斥矩阵 | 18500/17075固定、与043共享上游互斥；`PORT_IN_USE`不换端口或结束占用者，见`web-playtest.md` |
| 数据路径与键 | 私有home、full/preview+sourceSHA+archiveSHA，独立持久profile；不同候选不复制、迁移或删除 |
| 只读就绪请求 | GET `/api/v1/__playtest_ready__`，严格404/HTTP_ERROR并核对自身Java监听和静态index；新增浏览器回归核对活局/存储不变 |
| Caddy来源 | 复用`hosting-toolchain.json`与`checked_caddy`；缺失仅提示显式准备，启动不安装或联网下载 |
| 状态/锁/退出码 | 终端状态机、同UID flock/Unix socket；只向内核确认的自有未回收子进程发信号，不使用磁盘PID清理；浏览器独立会话 |
| 反馈schema及附件 | schema1，完整/中断与画面/声音/操作由用户填写；默认未验，附件仅显式选择，不采集凭据或自动上传 |
| HTTP边界与归档 | 明示不覆盖043 HTTPS/SAN/信任或公网；043首审及主线归档区分CR实测与Coder侧证据，run.cmd端口留045 |

P3端口并存、Caddy来源、控制面遮挡均纳入上述计划和实现。终端为独立控制入口，不注入浏览器覆盖层、不强制置顶；050最终游戏窗口仍由050实施。用户已明确每轮正常完整游玩至少一局，不能用短片段替代；已实现入口均保留，不宣称启动器补齐尚未完成的桌面外观/语言差异。

完整计划见[实施计划](superpowers/plans/2026-10-28-wf008-playtest.md)，用户操作与边界见[自助试玩](web-playtest.md)。实现首审、用户最终候选整局反馈、云端CI及合流仍按日常流程逐项完成，不由本设计评审替代。
