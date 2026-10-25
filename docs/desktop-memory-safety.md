# Ubuntu桌面内存事故与构建保护（2026-10-22）

> 严重更正：旧版清理脚本并不安全，AWK数组读取副作用使它误选无关桌面进程。三次会话退出与构建结束/桌面SIGTERM吻合；本助手承担引入此缺陷的责任。现已删除全机扫描与批量kill，改为内核确认的自身未回收子进程监护，详见[事故与隔离验证](incident-2026-10-22-process-cleanup.md)。下面旧试验中的“无Java残留”不能作为清理安全证据。

> 系统状态更新：13:09外部操作已增加8GiB swap并写入fstab，swappiness=10、vfs_cache_pressure=50，earlyoom启动（核验时仅-r 60）。这些不是本助手执行的操作；后文“未执行”指更早的历史状态，不应据此重复创建swap文件。

## 已核验证据

- `journalctl -k -b -1`确认11:36—12:01六次全局OOM，全部Free swap=0，五次ChatGPT、一次code，受害者oom_score_adj=300。不是单凭风扇/输入卡顿推断硬件故障。
- 本次重启后初查MemAvailable约8.3GiB、swap使用0；无Java进程。故当前恢复与重启有关，不能把这8GiB归功于项目配置。
- Windows-SSD未挂载，已用udisksctl恢复原分区；未改fstab。项目路径为`/media/noeticuser/Windows-SSD/Users/Lenovo/Desktop/电脑村/电脑村重制相关文件/Village of Cyber Remake`，不是/home/noeticuser。
- 现有swap仅2GiB；根分区可用29GiB。sudo -n true需要密码；本轮没有提权密码，未扩swap、安装earlyoom或改sysctl。
- systemd245，memory控制器在legacy cgroup，不能只看MemoryMax属性就声称用户级硬限额有效。

## 对外部方案的修正

根因方向合理；内核按评分选择目标并非随机“杀错”。earlyoom默认仍按oom_score选目标，RSS排序需版本支持的`--sort-by-rss`，不是其默认行为；见[官方手册](https://github.com/rfjakob/earlyoom/blob/master/MANPAGE.md)。不盲目安装并配置全机优先杀java，因为可能影响其他项目。

扩大swap可缓冲峰值，不能解决持续超额或保证桌面流畅；swappiness是匿名页与文件页回收的相对代价，不能把10视为通用治愈值，见[内核说明](https://www.kernel.org/doc/html/latest/admin-guide/sysctl/vm.html)。本轮未改swappiness/vfs_cache_pressure。

本机用户级cgroup限制有legacy层级限制，见[systemd说明](https://www.freedesktop.org/software/systemd/man/249/systemd.resource-control.html)。未切换cgroup模式或改启动参数；这是另一个有重启风险的系统变更，不混入功能开发。

## 已落地保护

- Gradle堆512MiB、metaspace256MiB、ActiveProcessorCount=2，单worker、非并行；测试与JavaExec堆256MiB、metaspace192MiB。保留原file.encoding=COMPAT，不添加项目不存在的Kotlin配置，不全局禁用daemon。
- `bash tools/run-gradle-safe.sh`先停止选定工具链daemon、检查free -h，低于2GiB拒绝；flock避免同一用户的安全入口同时构建，检查现存Gradle/Kotlin进程。不终止未知用户应用。
- 新监护程序检查MemAvailable，低于2GiB只清理内核验证属于自己的未回收子进程；不遍历全机进程或向进程组发信号。构建子树继续继承oom_score_adj=500。不是硬cgroup限额，不能保证任意突发分配都来得及阻止。
- 通过Linux子进程收养机制处理另建会话的后代，锁不传给构建子进程；不支持则拒绝启动。先通过15项隔离回归，用户批准恢复后再通过18项真实定向、540项联合全量及41项浏览器回归；退出无Java残留，GNOME shell PID保持35882。不将“无残留”单独当作安全证明，归属/外部哨兵回归也是必要证据。
- 历史试验中，两次手动终止核对过归属的三个JVM；但新增自动清理逻辑另有误杀漏洞，不能用手动清理安全或“无Java残留”替自动逻辑背书。旧版自动清理的安全声明撤回，见事故记录。
- 全量与打包须显式另行批准。环境失败最多重试一次，不在低内存时靠连续重试推进；项目AGENTS已固化。监控尚未通过故意耗尽整机内存的压力试验，不为验证而再次使桌面OOM。

## 当前可复现入口

```sh
export JAVA_HOME=/home/noeticuser/Desktop/.vocr-toolchain/java/jdk-21.0.12.1+1
export GRADLE_USER_HOME=/home/noeticuser/Desktop/.vocr-toolchain/gradle-home
export VOCR_GRADLE=/home/noeticuser/Desktop/.vocr-toolchain/gradle-8.8/bin/gradle
bash tools/run-gradle-safe.sh test --tests 'com.vocr.web.GuestGameServerTest' --offline
```

选择缓存的Gradle是因为当前gradlew有用户既有CRLF修改，Linux直接执行会失败；不擅自覆盖用户换行改动。

## 系统缓冲的当前事实

13:09的外部操作已创建并启用/swapfile2（8GiB），写入fstab，并设置swappiness=10/vfs_cache_pressure=50、启动earlyoom。总swap约10GiB，不要重复创建或覆盖。核验时earlyoom仅使用-r 60，并没有报告建议中的优先Java参数；不要把它宣传成已配置优先杀JVM。

这些系统操作不是本助手执行的，本次恢复验收也未修改它们。14:24验收后约5.0GiB可用、swap约1.0GiB、无Java残留，本轮内核未见新增OOM。快照不是峰值或长期保证，其他窗口占用变化后仍必须重新检查；系统内存方案也不能替代修复进程误杀漏洞。
