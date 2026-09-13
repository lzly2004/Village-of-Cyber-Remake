package com.vocr.replay;

import com.vocr.logging.DebugLogger;

// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\src\main\java\com\vocr\replay\ReplaySave.java
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReplaySave {
    public static final int CURRENT_VERSION = 3;

    public int version = CURRENT_VERSION;
    public int slotIndex;
    public String saveTime;
    public String peiyiName;
    public int peiyiOrdinal;
    public int peiyiVillageCount;
    public int totalVillageCount;
    public int totalDays;
    public int endResult;
    public int totalEvents;

    public List<DaySnapshot> daySnapshots = new ArrayList<>();
    public List<String> rawRecords = new ArrayList<>();

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public ReplaySave() {}

    public static ReplaySave fromRecords(int slot, List<String> records) {
        ReplaySave save = new ReplaySave();
        save.slotIndex = slot;
        save.saveTime = LocalDateTime.now().format(DT_FMT);
        save.rawRecords = new ArrayList<>(records);
        save.totalEvents = save.rawRecords.size();
        
        DebugLogger.info("[ReplaySave] 开始从录像记录构建: records=" + save.totalEvents);

        boolean foundJsonMetadata = false;
        ObjectMapper mapper = new ObjectMapper();

        for (String line : save.rawRecords) {
            if (line.startsWith("JSON|")) {
                try {
                    String jsonStr = line.substring(5);
                    JsonNode root = mapper.readTree(jsonStr);
                    String type = root.has("type") ? root.get("type").asText() : "";
                    if ("GAME_METADATA".equals(type)) {
                        save.peiyiName = root.has("peiyiName") ? root.get("peiyiName").asText() : "unknown";
                        save.peiyiOrdinal = root.has("peiyiOrdinal") ? root.get("peiyiOrdinal").asInt(-1) : -1;
                        save.peiyiVillageCount = root.has("peiyiVillageCount") ? root.get("peiyiVillageCount").asInt(-1) : -1;
                        save.totalVillageCount = root.has("totalVillageCount") ? root.get("totalVillageCount").asInt(-1) : -1;
                        foundJsonMetadata = true;
                        DebugLogger.info("[ReplaySave] 从JSON元数据提取: peiyi=" + save.peiyiName + ", village=" + save.peiyiVillageCount + "/" + save.totalVillageCount);
                    }
                } catch (Exception e) {
                    DebugLogger.warn("[ReplaySave] JSON解析失败: " + e.getMessage());
                }
            }

            if (!foundJsonMetadata) {
                if (line.startsWith("# VillageCount:")) {
                    String countStr = line.substring("# VillageCount:".length()).trim();
                    String[] parts = countStr.split("/");
                    if (parts.length == 2) {
                        try { save.peiyiVillageCount = Integer.parseInt(parts[0].trim()); } catch (Exception e) {}
                        try { save.totalVillageCount = Integer.parseInt(parts[1].trim()); } catch (Exception e) {}
                    }
                    DebugLogger.info("[ReplaySave] 提取到村数: peiyiVillage=" + save.peiyiVillageCount + ", totalVillage=" + save.totalVillageCount);
                }
                if (line.startsWith("# Peiyi:")) {
                    String peiyiStr = line.substring("# Peiyi:".length()).trim();
                    int ordStart = peiyiStr.indexOf("(ordinal=");
                    if (ordStart > 0) {
                        save.peiyiName = peiyiStr.substring(0, ordStart).trim();
                        String ordStr = peiyiStr.substring(ordStart + "(ordinal=".length());
                        ordStr = ordStr.replace(")", "").trim();
                        try { save.peiyiOrdinal = Integer.parseInt(ordStr); } catch (Exception e) {}
                    } else {
                        save.peiyiName = peiyiStr;
                    }
                    DebugLogger.info("[ReplaySave] 提取到peiyi: name=" + save.peiyiName + ", ordinal=" + save.peiyiOrdinal);
                }
            }
            
            if (line.startsWith("END|")) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String[] resultParts = parts[1].split("=");
                    if (resultParts.length >= 2) {
                        String resultStr = resultParts[1].trim();
                        switch (resultStr) {
                            case "VILLAGE_WIN": save.endResult = 1; break;
                            case "WOLF_WIN": save.endResult = 2; break;
                            case "FOX_WIN": save.endResult = 3; break;
                            default: save.endResult = 0;
                        }
                    }
                    if (parts.length >= 3) {
                        String[] dayParts = parts[2].split("=");
                        if (dayParts.length >= 2) {
                            try { save.totalDays = Integer.parseInt(dayParts[1].trim()); } catch (Exception e) {}
                        }
                    }
                }
            }
            if (line.startsWith("DAILY_SNAPSHOT|")) {
                DaySnapshot snapshot = DaySnapshot.parseFromRecord(line, save.rawRecords);
                if (snapshot != null) save.daySnapshots.add(snapshot);
            }
        }
        
        // 容错：如果没有快照数据，尝试从STATE行重建
        if (save.daySnapshots.isEmpty()) {
            DebugLogger.warn("[ReplaySave] daySnapshots为空，尝试从STATE行重建...");
            for (String line : save.rawRecords) {
                if (line.startsWith("STATE|")) {
                    try {
                        String dayStr = line.substring(line.indexOf("day=") + 4);
                        dayStr = dayStr.substring(0, dayStr.indexOf('|'));
                        int dayNum = Integer.parseInt(dayStr.trim());
                        
                        DaySnapshot snapshot = DaySnapshot.parseFromRecord(
                            "DAILY_SNAPSHOT|day=" + dayNum + "|alive=0", save.rawRecords);
                        if (snapshot != null && !containsDay(save.daySnapshots, dayNum)) {
                            save.daySnapshots.add(snapshot);
                        }
                    } catch (Exception e) {
                        DebugLogger.warn("[ReplaySave] STATE行解析失败: " + e.getMessage());
                    }
                }
            }
        }
        
        // 最终安全检查
        if (save.peiyiName == null || save.peiyiName.isEmpty()) {
            save.peiyiName = "unknown";
            save.peiyiOrdinal = -1;
        }
        if (save.peiyiVillageCount <= 0 || save.totalVillageCount <= 0) {
            save.peiyiVillageCount = -1;  // 向后兼容：旧存档没有此数据
            save.totalVillageCount = -1;
        }
        int maxSnapshotDay = 0;
        for (DaySnapshot snap : save.daySnapshots) {
            if (snap != null && snap.dayNumber > maxSnapshotDay) {
                maxSnapshotDay = snap.dayNumber;
            }
        }
        if (save.totalDays <= 0) {
            save.totalDays = maxSnapshotDay;
        } else {
            save.totalDays = Math.max(save.totalDays, maxSnapshotDay);
        }

        DebugLogger.info("[ReplaySave] 从录像记录构建完成: slot=" + slot +
                ", peiyi=" + save.peiyiName + ", days=" + save.totalDays +
                ", snapshots=" + save.daySnapshots.size() +
                ", result=" + save.endResult);
        return save;
    }
    
    private static boolean containsDay(List<DaySnapshot> list, int day) {
        for (DaySnapshot snap : list) {
            if (snap != null && snap.dayNumber == day) return true;
        }
        return false;
    }

    /**
     * 返回在指定复盘日期已经可见的技能记录。
     *
     * v3 优先读取不晚于 visibleDay + 1 的最新完整历史，使技能日当夜写入的记录
     * 能从次日快照稳定还原；旧录像没有完整历史时，退回原来的“技能日次日 st”读取方式。
     */
    public int getVisibleSkillTarget(int playerIndex, int skillDay, int visibleDay)
    {
        if (daySnapshots == null || playerIndex < 1 || skillDay < 1 || visibleDay < 1) return 0;

        int latestHistoryDay = -1;
        int historyTarget = 0;
        int historyCutoffDay = visibleDay + 1;
        for (DaySnapshot snapshot : daySnapshots)
        {
            if (snapshot == null || snapshot.dayNumber > historyCutoffDay
                    || snapshot.dayNumber <= latestHistoryDay) continue;
            DaySnapshot.PlayerStatus player = findPlayer(snapshot, playerIndex);
            if (player == null || player.skillTargets == null
                    || skillDay >= player.skillTargets.length) continue;
            latestHistoryDay = snapshot.dayNumber;
            historyTarget = player.skillTargets[skillDay];
        }
        if (latestHistoryDay >= 0) return historyTarget;

        DaySnapshot legacySnapshot = findSnapshot(skillDay + 1);
        DaySnapshot.PlayerStatus legacyPlayer = findPlayer(legacySnapshot, playerIndex);
        return legacyPlayer != null ? legacyPlayer.skillTarget : 0;
    }

    private DaySnapshot findSnapshot(int day)
    {
        for (DaySnapshot snapshot : daySnapshots)
            if (snapshot != null && snapshot.dayNumber == day) return snapshot;
        return null;
    }

    private DaySnapshot.PlayerStatus findPlayer(DaySnapshot snapshot, int playerIndex)
    {
        if (snapshot == null || snapshot.players == null) return null;
        for (DaySnapshot.PlayerStatus player : snapshot.players)
            if (player != null && player.playerIndex == playerIndex) return player;
        return null;
    }

    public String getPeiyiDisplayName() {
        if (peiyiName == null || peiyiName.isEmpty()) return "未知";
        switch (peiyiName.toLowerCase()) {
            case "jianyi": return "简易村";
            case "tongchang": return "通常村";
            case "yaoohu": return "妖狐村";
            case "kuangxin": return "狂信村";
            case "beide": return "背德村";
            case "maoyou": return "猫又村";
            case "daxing": return "大型村";
            default: return peiyiName;
        }
    }

    public String getResultDisplayName() {
        switch (endResult) {
            case 1: return "村人勝利";
            case 2: return "人狼勝利";
            case 3: return "妖狐勝利";
            default: return "未完";
        }
    }
}
