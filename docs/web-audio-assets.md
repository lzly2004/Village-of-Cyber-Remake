# WEB-026：音频资源、分包与按需接口

基线f5460eb。只转换既有WAV、建立资源目录/内容哈希与显式按需读取接口；没有在App接入播放、自动切歌、音量或设置，不修改Java、规则、API、随机源或存储。027/028负责播放生命周期与实际听感验收，A06/A07/A08未放行。复用020确认的小范围自娱使用范围，不新增外部音频素材。

## 来源与完整映射

`tools/build-web-audio.py`解析BgmTrack/SoundEffect实际枚举构造参数，未解析成员即失败，不用手写曲名表。读取23个BGM、9个音效及13个可选说明；保留BgmTrack中NORMAL_GAME_PLAYLIST的13项与LATE_GAME_PLAYLIST的4项顺序。六个固定用途（START_MENU/INFO/DEFEAT_SCREEN/VICTORY_SCREEN/REPLAY_PLAYER/REPLAY_BROWSER）同样完整入包，不只转换可轮换列表。

每个语义ID在`web-client/public/assets/audio/manifest.json`记录原相对路径、WAV原始字节SHA-256/长度、声道/采样率/样本数/时长、产物SHA-256/长度/码率和解码测量。13份说明保持源文字及Unicode，统一UTF-8/LF；Java枚举输入指纹也仅统一换行，WAV/MP3哈希按原字节。说明不是许可文本，也不猜作者。文件名及内容路径只取实际枚举引用，拒绝越界、大小写不符、符号链接、缺失、截断PCM及LFS指针。

“分包”指每首BGM/每个音效独立HTTP文件：`bgm/<sha256>.mp3`、`sounds/<sha256>.mp3`；说明独立为`descriptions/<sha256>.txt`。32个语义条目对应31个唯一音频文件，两项SAMIDARE_VIVARIDE/REMIX共用相同编码内容，不合并语义ID/说明/播放列表。加13份说明及manifest共45文件，原WAV不复制到网页、不改写、不删除。

## 编码与预算

使用MP3一种格式，保留源双声道和44.1/48kHz，不裁切、重采样、变速、混音或人为归一化音量。去除源容器标签，保留Xing/LAME样本延迟信息。FFmpeg/libmp3lame以单线程顺序转换：BGM从128kbit/s起，数值SDR不足14dB时试160/192；仍须同时满足单首及整包预算，否则不发布。音效192kbit/s。最终只有EMOTIONALLY_UNSTABLE提升到160，其余BGM128。没有备用编码；今后添加格式必须合计预算，不可只统计选中格式。

使用同一FFmpeg将MP3完整解码，以astats核对样本数（与源精确相等）、有限峰值/RMS、无NaN/Inf；源与产物统一fltp再用asdr逐声道比较，最低14dB是本项目筛查底线，不是主观音质评级或无损承诺。相关滤镜定义见[FFmpeg官方文档](https://ffmpeg.org/ffmpeg-filters.html#asdr)。不采用试验阶段apsnr输出的高数值作为音质证据。

| 项目 | 最终字节 | 预算 |
|---|---:|---:|
| 唯一BGM（23条目/22文件） | 54,890,214 | 64MiB |
| 最大BGM | 5,093,249 | 5MiB |
| 音效（9文件） | 498,275 | 1MiB |
| 音频manifest/说明 | 28,169 / 264 | 与图片清单、帮助合计1MiB |
| 全站JSON/TXT | 492,691 | 1MiB |
| 当前全部静态文件 | 77,647,695 | 100MiB |

最低实测SDR14.5543dB；解码浮点峰值最高+3.85918dBFS，**不能宣称满音量无削波**。原PCM音量未归一化，027/028必须为解码峰值和混音预留余量，并实际听审/核对音量。当前只完成数值和解码验收，没有逐曲人工听审；无缝循环、浏览器播放解锁/切换及主观听感不以文件解码成功替代，留在027/028及A08统一验收。

## 按需资源接口

`src/audioAssets.js`是未接入App的独立资源层；导入不请求网络、不创建Audio/AudioContext、不推断场景。`audioResources.manifest()`显式读一次共享目录；`load('bgm'|'sounds', id, {signal})`只下载所选文件、核对长度及SHA-256后返回描述符和ArrayBuffer；不保留整个音频库或隐式预取后续曲目。`description(id,{signal})`只按需取该说明，严格UTF-8纯文本；没有说明返回null。

目录15秒超时、异常保持拒绝，不循环重试或回退原WAV；未知ID/外部URL/原型键/非法文件名、HTTP错误、HTML伪200、损坏和哈希不符都有明确错误码。调用者可取消等待共享目录及当前下载，取消一方不取消其他使用者的共享目录。播放器仍须在027/028自行释放返回数据/解码缓冲、处理版本竞争与设备状态，026没有替未来播放器承诺内存上限。

静态音频可枚举；内容哈希与按需加载不构成身份保密层。027/028只能按当前公开展示和用户选择消费，不能依据隐藏身份、未来事件或私有技能提前选择音效。

## 工具与验收入口

开发机需要完整FFmpeg（含MP3解码、libmp3lame、astats/asdr；本轮7.0.2），通过FFMPEG环境变量或`--ffmpeg`指定。Playwright内置视频专用FFmpeg不满足要求。FFmpeg只用于开发转换/校验，不进入浏览器或Java运行依赖。固定开发包装器见[PyPI imageio-ffmpeg 0.6.0](https://pypi.org/project/imageio-ffmpeg/0.6.0/)和`tools/requirements-web-audio.txt`，使用Python≥3.9隔离环境即可，不需系统安装；清单记录实际二进制SHA-256及版本。

```sh
python3 -m venv /tmp/vocr-web-audio-tools
/tmp/vocr-web-audio-tools/bin/python -m pip install -r tools/requirements-web-audio.txt
export FFMPEG="$(/tmp/vocr-web-audio-tools/bin/python -c 'import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())')"
python3 -B tools/build-web-audio.py build --output /tmp/vocr-audio-new
python3 -B tools/build-web-audio.py check
python3 -B -m unittest discover -s tools/tests -p test_web_audio.py -v
```

build拒绝已有输出及源目录，先在本次临时目录转换和完整check，全部通过才原子发布；失败只清理自己的临时目录。check只读，核对源/清单/产物/帧头/完整解码/测量及所有预算，拒绝残留文件。日常check也需要完整FFmpeg；它不是只读头或仅验文件名的轻量替代。

在web-client运行`npm run audio:check`、`npm run build`（图片/音频prebuild及postbuild门禁）、`npm run test:unit`、`npm run test:audio`、`npm run test:assets`。音频浏览器套件独立静态preview、单worker、无JVM；31文件逐个新文档完整OfflineAudioContext解码并关闭文档，避免一次积累58分钟解码缓冲。允许浏览器与源相差不超过一个1152样本MPEG帧，该容差不等于无缝循环验收。资源模块通过测试显式注入浏览器单独验证，不能写成App已接入音频；模拟开村只验证零音频请求，不作为真实规则局验证。

资源层不改变公共页面/恢复交互，因此026不重复默认93项或七配役15项，更不启动Java全量/打包。实际命令、迭代、数字、覆盖和剩余门槛见[review-f5460eb.md](review-f5460eb.md)。后续直接引用此清单，勿反复重编码入Git；需改策略时先评估历史增量和旧产物去留。
