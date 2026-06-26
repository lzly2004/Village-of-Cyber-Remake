import javax.swing.*;
import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;

public class Resources implements ResourcesInterface {
    // 音频相关
    private Clip currentBgm;
    private String currentBgmName;
    // 台词资源：角色名 → 事件名 → 台词数据
    private Map<String, Map<String, LineData>> lineResource;
    // 台词参数占位符正则（<param=xxx>）
    private static final Pattern PARAM_PATTERN = Pattern.compile("<param=(\\w+)>");
    // Jackson JSON解析器（全局复用）
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    //台词配置路径
    private static final String LINE_JSON_PATH = "/lines/event_lines.json";

    //图片配置
    // 图片映射配置：角色名 → 事件名 → 图片数据（支持单/多图）
    private Map<String, Map<String, ImageLineData>> eventImageMapping;
    // 图片配置文件路径
    private static final String IMAGE_MAPPING_PATH = "/config/event_image_mapping.json";

    // 构造器
    public Resources() {
    }

    public void init() {
        loadLineResource();   // 加载台词JSON
        loadEventImageMapping(); // 加载图片映射配置
    }

    public void run() {
        DebugLogger.log("资源类已启动，进入常态运行");
    }

    //事件图片获取（返回ImageIcon数组）
    @Override
    public ImageIcon[] getEventImage(Event event) {
        Object[] lookup = resolveResourceLookup(eventImageMapping, event, "图片");
        if (lookup == null) return new ImageIcon[]{getGlobalDefaultImage()};
        String ch2Name = (String) lookup[0];
        ImageLineData imageLineData = (ImageLineData) lookup[1];

        String[] targetImageNames = parseImageNames(
            resolveSpecialOrDefault(imageLineData, ch2Name, ImageLineData::getSpecial, ImageLineData::getDefaultImage));
        if (targetImageNames == null || targetImageNames.length == 0) {
            DebugLogger.error("角色" + event.ch1.name() + "事件" + event.eventname.name() + "无默认图片，返回全局兜底图数组");
            return new ImageIcon[]{getGlobalDefaultImage()};
        }

        ImageIcon[] imageIcons = new ImageIcon[targetImageNames.length];
        for (int i = 0; i < targetImageNames.length; i++) {
            ImageIcon icon = getImage(targetImageNames[i]);
            imageIcons[i] = (icon != null) ? icon : getGlobalDefaultImage();
        }
        return imageIcons;
    }



    /**
     * 解析事件资源的核心查找逻辑：角色配置 → 事件配置 → 返回[ch2Name, eventConfig]
     */
    private Object[] resolveResourceLookup(Map<String, ? extends Map<String, ?>> resourceMap, Event event, String ctx) {
        if (event == null || event.ch1 == null || event.eventname == null) {
            DebugLogger.error(ctx + "：事件/角色/事件名为空");
            return null;
        }
        String ch1Name = event.ch1.name();
        Map<String, ?> charConfig = resourceMap.get(ch1Name);
        if (charConfig == null) {
            DebugLogger.error(ctx + "：角色" + ch1Name + "无配置");
            return null;
        }
        Object eventConfig = charConfig.get(event.eventname.name());
        if (eventConfig == null) {
            DebugLogger.error(ctx + "：角色" + ch1Name + "无事件" + event.eventname.name() + "的配置");
            return null;
        }
        String ch2Name = event.ch2 != null ? event.ch2.name() : "";
        return new Object[]{ch2Name, eventConfig};
    }

    /** 优先匹配special(ch2)，否则返回default */
    private <T> Object resolveSpecialOrDefault(T eventConfig, String ch2Name,
            Function<T, Map<String, ?>> getSpecial,
            Function<T, ?> getDefault) {
        Map<String, ?> specials = getSpecial.apply(eventConfig);
        if (!ch2Name.isEmpty() && specials != null && specials.containsKey(ch2Name))
            return specials.get(ch2Name);
        return getDefault.apply(eventConfig);
    }

    private String[] parseImageNames(Object imageObj) {
        if (imageObj == null) {
            return new String[0];
        }
        // 情况1：单图（字符串格式）
        if (imageObj instanceof String) {
            return new String[]{(String) imageObj};
        }
        // 情况2：多图（数组格式）
        if (imageObj instanceof List<?>) {
            List<?> list = (List<?>) imageObj;
            String[] arr = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                arr[i] = list.get(i).toString();
            }
            return arr;
        }
        // 未知类型
        return new String[0];
    }

    private ImageIcon getGlobalDefaultImage() {
        // 全局default不是ImageLineData，直接取"all"对应的字符串
        Object defaultObj = eventImageMapping.get("default");
        String defaultImageName = "frame.png";

        if (defaultObj instanceof Map<?, ?>) {
            Map<?, ?> defaultMap = (Map<?, ?>) defaultObj;
            // 直接取"all"字段的值
            if (defaultMap.containsKey("all") && defaultMap.get("all") instanceof String) {
                defaultImageName = (String) defaultMap.get("all");
            }
        }
        return getImage(defaultImageName);
    }

    //基础资源加载方法
    @Override
    public ImageIcon getImage(String imageName) {
        try {
            // 拼接路径：/image/ + 图片名（适配resources/image目录）
            URL imageUrl = getClass().getResource("/images/" + imageName);
            if (imageUrl == null) {
                DebugLogger.error("未找到图片" + imageName);
                return getImage("frame.png");
            }
            return new ImageIcon(imageUrl);
        } catch (Exception e) {
            DebugLogger.error("图片" + imageName + "加载失败");
            e.printStackTrace();
            return getImage("frame.png");
        }
    }

    private void loadLineResource() {
        try (InputStream is = getClass().getResourceAsStream(LINE_JSON_PATH)) {
            if (is == null) {
                throw new IOException("台词JSON文件未找到：" + LINE_JSON_PATH + "（请检查resources/lines目录）");
            }

            // 解析为嵌套Map：角色名 → 事件名 → LineData
            lineResource = OBJECT_MAPPER.readValue(
                    is,
                    new TypeReference<Map<String, Map<String, LineData>>>() {}
            );
            DebugLogger.log("成功加载 " + lineResource.size() + " 个角色的台词配置");
        } catch (IOException e) {
            DebugLogger.error("加载失败！");
            e.printStackTrace();
            lineResource = Map.of(); // 初始化空Map避免NPE
        }
    }

    private void loadEventImageMapping() {
        try (InputStream is = getClass().getResourceAsStream(IMAGE_MAPPING_PATH)) {
            if (is == null) {
                throw new IOException("图片映射配置未找到：" + IMAGE_MAPPING_PATH + "（请检查resources/config目录）");
            }

            // 解析为嵌套Map：角色名 → 事件名 → ImageLineData
            eventImageMapping = OBJECT_MAPPER.readValue(
                    is,
                    new TypeReference<Map<String, Map<String, ImageLineData>>>() {}
            );
            DebugLogger.log("成功加载 " + eventImageMapping.size() + " 个角色的图片映射配置");
        } catch (IOException e) {
            DebugLogger.error("加载失败！");
            e.printStackTrace();
            eventImageMapping = Map.of(); // 初始化空Map避免NPE
        }
    }

 //音频相关方法
    @Override
    public boolean playBgm(String bgmName) {
        if (bgmName == null || bgmName.trim().isEmpty()) {
            if (currentBgm != null) {
                currentBgm.stop();
                currentBgm.close();
            }
            currentBgmName = null;
            return true; // 停止操作视为成功
        }

        // 停止并释放当前BGM
        if (currentBgm != null) {
            currentBgm.stop();
            currentBgm.close();
        }

        try {
            // 加载BGM文件（resources/BGM/目录）
            URL bgmUrl = getClass().getResource("/BGM/" + bgmName);
            if (bgmUrl == null) {
                DebugLogger.error("BGM " + bgmName + " 未找到资源! 路径=/BGM/" + bgmName);
                return false;
            }
            DebugLogger.info("[BGM] 找到资源: " + bgmUrl);
            AudioInputStream ais = AudioSystem.getAudioInputStream(bgmUrl);
            AudioFormat originalFormat = ais.getFormat();
            DebugLogger.info("[BGM] 音频格式: " + originalFormat);

            // 如果不是标准PCM格式，自动转码
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    originalFormat.getSampleRate(),
                    16,
                    originalFormat.getChannels(),
                    originalFormat.getChannels() * 2,
                    originalFormat.getSampleRate(),
                    false
            );
            if (!originalFormat.matches(targetFormat)) {
                DebugLogger.info("[BGM] 转码: " + originalFormat.getEncoding() + " -> PCM_SIGNED");
                ais = AudioSystem.getAudioInputStream(targetFormat, ais);
            }

            currentBgm = AudioSystem.getClip();
            currentBgm.open(ais);
            ais.close();
            currentBgm.loop(Clip.LOOP_CONTINUOUSLY); // 循环播放
            currentBgm.start();
            currentBgmName = bgmName;
            DebugLogger.info("[BGM] 播放成功: " + bgmName);
            return true;
        } catch (Exception e) {
            DebugLogger.error("BGM " + bgmName + " 播放失败: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public String getCurrentBgmName() {
        return currentBgmName;
    }

    @Override
    public boolean playSound(String soundName) {
        try {
            URL soundUrl = getClass().getResource("/sounds/" + soundName);
            if (soundUrl == null) {
                DebugLogger.error("未找到音效文件：" + soundName);
                return false;
            }

            AudioInputStream ais = AudioSystem.getAudioInputStream(soundUrl);
            Clip clip = AudioSystem.getClip();
            // 添加监听器：播放结束后释放资源
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    try {
                        ais.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                clip.open(ais);
                clip.start();
            } catch (Exception openEx) {
                clip.close();
                try { ais.close(); } catch (IOException ex) { ex.printStackTrace(); }
                throw openEx;
            }
            return true;
        } catch (Exception e) {
            DebugLogger.error("音效 " + soundName + " 播放失败");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getEventText(Event event) {
        Object[] lookup = resolveResourceLookup(lineResource, event, "台词");
        if (lookup == null) return "错误：" + (event == null ? "事件对象为空" : event.ch1 == null ? "说话角色为空" : "事件名为空");
        String ch2Name = (String) lookup[0];
        LineData lineData = (LineData) lookup[1];

        String targetLine = (String) resolveSpecialOrDefault(lineData, ch2Name, LineData::getSpecial, LineData::getDefaultLine);
        if (targetLine == null || targetLine.isEmpty())
            return String.format("角色%s事件%s无可用台词", event.ch1.name(), event.eventname.name());
        return replaceParams(targetLine, event.ch1, event.ch2);
    }

    //枚举→日文名转换方法
    private String getKatakanaName(CharacterEnglishName englishName) {
        if (englishName == null || englishName == CharacterEnglishName.NONE) {
            return "";
        }
        try {
            // 利用序号一致，直接通过ordinal()获取对应日文枚举
            CharacterKatakanaName kanaName = CharacterKatakanaName.values()[englishName.ordinal()];
            return kanaName.name(); // 枚举name()即为日文名（如ベッティ）
        } catch (ArrayIndexOutOfBoundsException e) {
            DebugLogger.error("角色枚举序号匹配失败：" + englishName.name());
            return "";
        }
    }

    private String replaceParams(String line, CharacterEnglishName talkerEnum, CharacterEnglishName targetEnum) {
        // 日志排查（可选，方便验证）
        DebugLogger.log("原始台词：" + line);
        String targetKana = getKatakanaName(targetEnum); // ch2的日文名
        DebugLogger.log("目标角色(ch2)日文名：" + targetKana);

        Matcher matcher = PARAM_PATTERN.matcher(line);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String paramKey = matcher.group(1);
            // 所有占位符（talkerName/targetName/partnerName）都替换为ch2的日文名
            String replacement = switch (paramKey) {
                case "talkerName", "targetName", "partnerName" -> targetKana;
                default -> "";
            };
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        DebugLogger.log("最终台词：" + result.toString());
        return result.toString();
    }


    @Override
    public String getHelpText(String helpTitle) {
        //资源路径（适配resources/help/xxx.txt，路径必须以/开头）
        String resourcePath = "/help/" + helpTitle;
        // 当前类的classpath（确认资源是否在classpath中）
        URL classPathUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
        //资源的实际URL（核心排查：非null才表示找到资源）
        URL resourceUrl = getClass().getResource(resourcePath);
        //读取资源流（适配同级resources目录）
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            // 校验资源是否存在
            if (inputStream == null) {
                return "未找到资源文件！\n" +
                        "请检查：resources/help/ 下是否有" + helpTitle + "\n" +
                        "当前classpath：" + classPathUrl;
            }

            // 读取文本内容（极简方式，避免逐行读取问题）
            byte[] contentBytes = inputStream.readAllBytes();
            String content = new String(contentBytes, StandardCharsets.UTF_8);
            return content.isEmpty() ? "文件内容为空：" + helpTitle : content;

        } catch (Exception e) {
            DebugLogger.error("读取异常：");
            e.printStackTrace();
            return "加载失败：" + e.getMessage();
        }
    }
    @Override
    public void save(GameInfo GameInfo, int cnt) {
        // 存档功能暂未实现
        DebugLogger.log("[Resources.save] 存档功能暂未实现");
    }

    @Override
    public GameInfo load(int cnt) {
        // 读档功能暂未实现
        DebugLogger.log("[Resources.load] 读档功能暂未实现，返回null");
        return null;
    }

    @Override
    public GameRecord getRecord() {
        // 获取游戏记录功能暂未实现
        DebugLogger.log("[Resources.getRecord] 获取游戏记录功能暂未实现，返回null");
        return null;
    }

    // 内部类：JSON映射实体
    private static class LineData {
        @JsonProperty("default")
        private String defaultLine; // 默认台词
        private Map<String, String> special; // 特殊角色→特殊台词

        // Getter
        public String getDefaultLine() { return defaultLine; }
        public Map<String, String> getSpecial() { return special; }
    }

    // 新增@JsonIgnoreProperties注解，忽略JSON中未匹配的字段（如all）
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ImageLineData {
        @JsonProperty("default")
        private Object defaultImage; // 默认图片名（String/List<String>）
        private Map<String, Object> special; // 特殊角色→特殊图片名（String/List<String>）
        // Getter
        public Object getDefaultImage() { return defaultImage; }
        public Map<String, Object> getSpecial() { return special; }
    }
}