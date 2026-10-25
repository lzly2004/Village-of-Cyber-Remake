# WEB-022：图片选集、转换与资源门禁

本批基线58453f9。仅建立静态图片产物与构建门禁，不在页面接入正式头像、立绘、场景、菜单或音频，不放行A06/A07/A08。来源及小范围自娱使用约定沿用[020目录](web-asset-source-catalog.md)，不另追索授权材料。

## 选集与数据契约

- 与020共用`tools/audit-web-source-assets.py`的`image_reference_catalog`，合并固定UiImage、事件映射（含special/数组全部成员与frame元数据）、44人的头像/称号及已审计动态图族，**1171个逻辑路径**。只读`src/main/resources/images/`；不从资源根目录换用同名图，不把其余667张图全目录打包。
- `web-client/public/assets/images/manifest.json`按原`images/文件名.png`查找；每项记录源SHA-256/字节、用途集合、产物SHA-256/字节、原尺寸、是否无损、质量参数与黑/白背景合成PSNR。URL为`/assets/images/`加`file`字段；文件名是完整64位内容哈希加`.webp`，只对字节相同的产物共享文件，保留全部逻辑路径。
- 清单记录schemaVersion、转换策略版本、Pillow/libwebp版本、UiImage和原映射JSON的UTF-8/LF文本指纹、边界样本存在性及精确计数。PNG源文件及WebP产物按原始字节哈希；仅这两个文本输入经读取时统一换行再计算指纹（不改写文件），兼容CRLF/LF checkout，其余文本改变仍拒绝旧清单。
- 不在022解释事件选图或重排数组；024仍须按Resources与权威公开事件核对special和多图顺序。清单可公开枚举全部素材，不能充当身份保密层；023—025继续遵守真实身份/公开技能/终局揭晓门槛。
- 清单及哈希文件是当前可追溯静态产物，不包含绝对路径、时间戳或会话数据；没有音频/帮助原文的重复副本。

## 编码与画质

保留全部原像素尺寸，不缩小1280×720场景，不裁切或补造缺图。原PNG保持不变。使用WebP一种格式，无隐藏PNG或备用编码副本。

小头像、称号、动态文字及UiImage保持无损RGBA（含全透明像素内RGB）；仅`title_base_resized.png`为经试转码确认的巨大背景例外。其他纯事件立绘/背景从quality90尝试，不满足黑/白底最小合成PSNR35dB时改96，仍不满足则无损；方法6、exact=true、alpha_quality=100。所有产物完整解码后校验尺寸和逐像素alpha相同，无损分支额外校验全部RGBA；99为完美一致的有限PSNR标记。PSNR是数值底线，不代替目视核验，亦不等于024场景组合验收。

参数语义参见[Pillow WebP官方文档](https://pillow.readthedocs.io/en/stable/handbook/image-file-formats.html#webp)。转换移除PNG的dpi等容器元数据；选集无ICC/gamma，两个cHRM样本为标准sRGB原色/白点，不另做颜色空间变换。

## 使用与失败边界

日常`check`/`distribution`（包括npm构建前后门禁）只需Python3与支持WebP解码的Pillow；022 reviewer已实测系统Python3.8/Pillow7.0.0通过。这不等于该编码器可重建同一产物。重新生成的`build`要求Pillow≥10+WebP；为同版本复现使用`tools/requirements-web-images.txt`固定的Pillow12.3.0及libwebp1.6.0。仓库不新增Vue/Java生产依赖。先在隔离Python环境安装开发依赖；本机也可用已配置的Codex bundled Python（完整路径见本轮验收）。

仓库根执行：

```sh
python3 -B tools/build-web-images.py build --output /tmp/vocr-images-new
python3 -B tools/build-web-images.py check --output /tmp/vocr-images-new
python3 -B tools/build-web-images.py check
python3 -B -m unittest discover -s tools/tests -p test_web_images.py -v
```

`build`仅接受不存在的新目录，拒绝写进原资源目录，不覆盖现有输出。它先在同级自建临时目录逐张处理、完整检查，通过后才发布目录；失败只清理本次创建的临时目录，不删除原素材或旧产物。重新生成先用新目录核验，再有意识地替换仅本工具负责的图片目录；不能整体清理public或原资源。相同输入/策略/编码器生成相同内容地址，无损/有损策略与编码器版本变更应重建并复检。

`check`只读校验选集、源/输出哈希、大小写、尺寸、透明度、数值画质及预算，拒绝缺图/损坏/LFS文本、路径越界、符号链接、源漂移、残留输出、清单与摘要不一致。没有“跳过坏图”或默认返回frame的路径。超预算明确失败，不能自动删角色、场景或放宽质量底线。原日期/顺位边界（14/50日及后续顺位）仍明确存在于manifest，不将其当作已引用缺图或补图；023/033/034实现文字回退，024保持NONE/wsw系统特例。

在web-client执行`npm run build`时，prebuild检查public资源，Vite复制，postbuild检查dist资源与原清单逐字一致，并统计**所有HTML/JS/CSS（含台词）**的原始及gzip总量。单图≤512KiB、全部唯一图片≤32MiB、清单≤1MiB、代码≤1MiB且gzip≤512KiB、当前静态产物总和≤100MiB。该总和只是目前已有分组，BGM/音效/帮助和备用编码后续仍占原100MiB预算，不能占用未完成组的额度冒充全量达标。

`npm run test:assets`使用独立Playwright配置，1 worker、retries=0，仅启动静态preview，无Java；验证大厅尚无玩家卡片时不请求图片/清单、开村后仅可视附近公开玩家资源加载且满足传输预算、每个唯一产物在Chromium完整解码且尺寸正确、未知哈希不能解码为图片。开发preview可能对未知路径返回SPA HTML，不能把HTTP200误认加载成功。正式404/cache-control/部署路径归042—045，后续UI加载失败提示归023—025。内容哈希支持缓存失效，但本轮不声称已配置生产immutable响应头。

022没有修改App/GameStage、游标、恢复、确认按钮、CO中断或身份揭晓行为；共享结构未变，022未机械重跑默认65项或七配役gate。021/D1旧回归仅作为022已接受基线；022实际命令、体积、画质抽样与失败修正见[本轮记录](review-58453f9.md)。冷首页1.5MiB和首舞台新增3MiB目标留待正式接入时按实际网络复核；本轮静态存在不等于首页预载。024/025必须实测事件切换时重新创建图片节点的加载、解码和首帧闪烁。

023—025直接引用当前manifest和内容寻址文件，不重复生成入库。调整质量/格式前评估旧产物去留与Git历史增量；普通资源接入不重写历史。023玩家素材加载契约见[玩家展示](web-player-visuals.md)，本页编码清单/预算仍为022基线。
