import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一调试日志系统 — 第0步重构核心基础设施。
 * 集中管理项目中所有调试输出,替代分散的 isTest/istest 标志和 System.out/err 调用。
 *
 * 功能:
 *   1. 全局开关: enabled 控制所有调试输出
 *   2. 分级日志: DEBUG(详细) / INFO(关键事件) / WARN(警告) / ERROR(错误)
 *   3. 双重输出: 控制台 + 文件(可选)
 *   4. 结构化游戏状态录制: 用于回归测试基线
 */
public class DebugLogger
{
    // ==================== 单例 ====================
    private static DebugLogger instance = null;

    public static DebugLogger getInstance()
    {
        if (instance == null)
            instance = new DebugLogger();
        return instance;
    }

    /**
     * 便捷静态方法 — 替代 logicTools.log() 和 System.out.println()
     */
    public static void log(String message) { getInstance().debug(message); }
    /** 接受任意对象的log重载 — 替代 System.out.println(obj) */
    public static void log(Object obj) { getInstance().debug(String.valueOf(obj)); }
    /** 无参数log — 替代 System.out.println() */
    public static void log() { getInstance().debug(""); }

    public static void info(String message) { getInstance().info0(message); }

    public static void warn(String message) { getInstance().warn0(message); }

    public static void error(String message) { getInstance().error0(message); }

    // ==================== 实例字段 ====================
    private boolean enabled = true;           // 总开关
    private LogLevel minConsoleLevel = LogLevel.DEBUG;  // 控制台最低输出级别
    private LogLevel minFileLevel = LogLevel.INFO;      // 文件最低输出级别
    private PrintWriter fileWriter = null;    // 文件输出
    private String currentLogFilePath = null;

    // 结构化事件录制(用于回归测试)
    private boolean recordingEnabled = false;
    private final List<String> recordedEvents = new ArrayList<>();

    // 时间戳格式化
    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // ==================== 日志级别枚举 ====================
    public enum LogLevel
    {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR");

        public final int level;
        public final String label;

        LogLevel(int level, String label)
        {
            this.level = level;
            this.label = label;
        }
    }

    // ==================== 配置方法 ====================

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isEnabled() { return enabled; }

    public void setMinConsoleLevel(LogLevel level) { this.minConsoleLevel = level; }

    public void setMinFileLevel(LogLevel level) { this.minFileLevel = level; }

    /**
     * 开始将日志写入文件。用于录制完整一局游戏的输出。
     * @param filePath 日志文件路径(如 "logs/baseline_2026-06-15.log")
     */
    public void startFileLogging(String filePath)
    {
        try
        {
            // 确保目录存在
            java.io.File dir = new java.io.File(filePath).getParentFile();
            if (dir != null && !dir.exists())
                dir.mkdirs();

            fileWriter = new PrintWriter(new FileWriter(filePath, true)); // 追加模式
            currentLogFilePath = filePath;
            info0("=== 日志文件已开启: " + filePath + " ===");
        }
        catch (IOException e)
        {
            System.err.println("无法创建日志文件: " + filePath + " - " + e.getMessage());
        }
    }

    /**
     * 停止文件日志并关闭文件
     */
    public void stopFileLogging()
    {
        if (fileWriter != null)
        {
            info0("=== 日志文件已关闭 ===");
            fileWriter.flush();
            fileWriter.close();
            fileWriter = null;
            currentLogFilePath = null;
        }
    }

    public String getCurrentLogFilePath() { return currentLogFilePath; }

    // ==================== 结构化录制(回归测试用) ====================

    public void startRecording()
    {
        recordingEnabled = true;
        recordedEvents.clear();
        debug("=== 结构化录制已开始 ===");
    }

    public void stopRecording()
    {
        recordingEnabled = false;
        debug("=== 结构化录制已停止,共录制 " + recordedEvents.size() + " 条事件 ===");
    }

    /**
     * 录制一条结构化事件。用于回归基线对比。
     * 格式: "EVENT|eventName|ch1|ch2|gameDay|extra"
     */
    public void recordGameEvent(String category, String details)
    {
        if (!recordingEnabled) return;
        String entry = category + "|" + details;
        recordedEvents.add(entry);
        debug("[REC] " + entry);
    }

    /**
     * 录制游戏状态快照
     */
    public void recordGameSnapshot(String label, GameStatus gs)
    {
        if (!recordingEnabled) return;
        StringBuilder sb = new StringBuilder();
        sb.append("SNAPSHOT|").append(label).append("|");
        sb.append("day=").append(gs.gameDay).append("|");
        sb.append("alive=").append(gs.aliveCounter).append("|");
        sb.append("end=").append(gs.end).append("|");
        for (int i = 1; i <= gs.getPlayerSum(); i++)
        {
            GameCharacter gc = gs.gc[i];
            sb.append("[")
                    .append(gc.number).append(":")
                    .append("role=").append(gc.actualRole).append(",")
                    .append("claimed=").append(gc.claimedRole).append(",")
                    .append("dieDay=").append(gc.dieDay).append(",")
                    .append("whyDie=").append(gc.whyDie).append(",")
                    .append("nonHumanMarker=").append(gc.nonHumanMarker)
                    .append("]");
        }
        recordedEvents.add(sb.toString());
        debug("[REC] Snapshot: " + label);
    }

    public List<String> getRecordedEvents() { return new ArrayList<>(recordedEvents); }

    public void saveRecordedEvents(String filePath)
    {
        try
        {
            java.io.File dir = new java.io.File(filePath).getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            try (PrintWriter pw = new PrintWriter(new FileWriter(filePath)))
            {
                pw.println("# Game Event Recording — Baseline");
                pw.println("# Generated: " + LocalDateTime.now().format(TIME_FMT));
                pw.println("# Total events: " + recordedEvents.size());
                pw.println("# ========================================");
                for (String entry : recordedEvents)
                    pw.println(entry);
            }
            info("录制事件已保存至: " + filePath);
        }
        catch (IOException e)
        {
            error("保存录制事件失败: " + e.getMessage());
        }
    }

    // ==================== 核心日志方法 ====================

    public void debug(String message) { log(LogLevel.DEBUG, message); }

    public void info0(String message) { log(LogLevel.INFO, message); }

    public void warn0(String message) { log(LogLevel.WARN, message); }

    public void error0(String message) { log(LogLevel.ERROR, message); }

    // ==================== 格式化输出(用于 MainLogic 调试) ====================

    /**
     * 格式化输出 — 用于替换 System.out.print/println (静态便捷方法)
     */
    public static void print(String message) { getInstance().print0(LogLevel.DEBUG, message); }

    public static void println(String message) { getInstance().print0(LogLevel.DEBUG, message + "\n"); }

    public static void printf(String format, Object... args)
    {
        getInstance().print0(LogLevel.DEBUG, String.format(format, args));
    }

    // 实例版本的print (内部使用)
    public void print0(LogLevel level, String message)
    {
        if (!enabled) return;
        if (level.level >= minConsoleLevel.level)
            System.out.print(message);
        if (fileWriter != null && level.level >= minFileLevel.level)
        {
            fileWriter.print(message);
            fileWriter.flush();
        }
    }

    // ==================== 内部实现 ====================

    private void log(LogLevel level, String message)
    {
        if (!enabled) return;

        // 控制台输出
        if (level.level >= minConsoleLevel.level)
        {
            String formatted = formatMessage(level, message);
            if (level == LogLevel.ERROR)
                System.err.println(formatted);
            else
                System.out.println(formatted);
        }

        // 文件输出
        if (fileWriter != null && level.level >= minFileLevel.level)
        {
            fileWriter.println(formatMessage(level, message));
            fileWriter.flush();
        }
    }

    private String formatMessage(LogLevel level, String message)
    {
        String timestamp = LocalDateTime.now().format(TIME_FMT);
        return String.format("[%s] [%s] %s", timestamp, level.label, message);
    }
}