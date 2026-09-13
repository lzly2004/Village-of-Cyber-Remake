package com.vocr.core;

import com.vocr.logging.DebugLogger;

import com.vocr.domain.peiyi;

import com.vocr.history.GameRecord;
import com.vocr.history.GameRecordManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 游戏录像/回归测试录制器 — 第0步核心基础设施。
 *
 * 录制一局完整游戏的初始化信息、每日状态、投票和终局记录,用于:
 *   1. 重构前后行为对比(内存轨迹摘要测试)
 *   2. 游戏回放/复盘
 *   3. Bug复现
 *
 * 使用方式:
 *   GameRecorder recorder = new GameRecorder("replay_peiyi_1");
 *   recorder.startGame(peiyi, seed, playerCount, gameStatus);
 *   // ... 游戏进行中调用每日快照和投票记录方法 ...
 *   recorder.endGame(endResult, gameDay);
 *   List<String> records = recorder.getRecords();
 */
public class GameRecorder
{
    private final String runLabel;           // 本次运行的标签
    private final List<String> records;      // 结构化记录
    private final GameSnapshotRecordWriter snapshotWriter;
    private boolean active = false;          // 是否正在录制
    private final GameRecordManager gameRecordManager;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GameRecorder(String runLabel, GameRecordManager gameRecordManager)
    {
        this.runLabel = runLabel;
        this.records = new ArrayList<>();
        this.snapshotWriter = new GameSnapshotRecordWriter(records);
        this.gameRecordManager = gameRecordManager;
    }

    // ==================== 游戏生命周期 ====================

    void startGame(peiyi p, long seed, int playerCount, GameStatus gs)
    {
        this.active = true;
        this.records.clear();

        records.add("# ==========================================");
        records.add("# Game Baseline Recording: " + runLabel);
        records.add("# Time: " + LocalDateTime.now().format(DT_FMT));
        records.add("# Peiyi: " + p + " (ordinal=" + p.ordinal() + ")");
        records.add("# Seed: " + seed);
        records.add("# PlayerCount: " + playerCount);
        int peiyiVillage = -1, totalVillage = -1;
        try {
            GameRecord record = gameRecordManager.getRecord();
            peiyiVillage = (p.ordinal() >= 1 && p.ordinal() <= 7) ? record.playcnt[p.ordinal()] + 1 : -1;
            totalVillage = record.totalPlayCnt + 1;
            records.add("# VillageCount: " + peiyiVillage + "/" + totalVillage);
        } catch (Exception e) {
            DebugLogger.warn("[GameRecorder] 获取村数信息失败: " + e.getMessage());
            records.add("# VillageCount: -1/-1");
        }
        records.add("# ==========================================");

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("type", "GAME_METADATA");
            meta.put("version", 3);
            meta.put("peiyiName", p.name());
            meta.put("peiyiOrdinal", p.ordinal());
            meta.put("peiyiVillageCount", peiyiVillage);
            meta.put("totalVillageCount", totalVillage);
            meta.put("seed", seed);
            meta.put("playerCount", playerCount);
            records.add("JSON|" + mapper.writeValueAsString(meta));
        } catch (Exception e) {
            DebugLogger.warn("[GameRecorder] 记录结构化元数据失败: " + e.getMessage());
        }

        // 录制初始角色分配
        StringBuilder sb = new StringBuilder("INIT|roles|");
        for (int i = 1; i <= playerCount; i++)
        {
            GameCharacter gc = gs.gc[i];
            sb.append(i).append(":").append(gc.number).append(":").append(gc.actualRole);
            if (i < playerCount) sb.append(",");
        }
        records.add(sb.toString());

        DebugLogger.info("[GameRecorder] 开始录制游戏: " + runLabel +
                " (peiyi=" + p + ", seed=" + seed + ", players=" + playerCount + ")");
    }

    public void endGame(int endResult, int gameDay)
    {
        this.active = false;
        String resultStr = switch (endResult) {
            case 0 -> "UNFINISHED";
            case 1 -> "VILLAGE_WIN";
            case 2 -> "WOLF_WIN";
            case 3 -> "FOX_WIN";
            default -> "UNKNOWN";
        };
        records.add("END|result=" + resultStr + "|day=" + gameDay);
        DebugLogger.info("[GameRecorder] 游戏结束: " + resultStr + " at day " + gameDay);
    }

    // ==================== 状态快照 ====================

    /**
     * 记录每日完整快照（包含存活/死亡状态、技能结果、投票明细）
     * 用于Replay系统的回放数据采集
     */
    public void recordDailySnapshot(int day, GameContext ctx)
    {
        if (!active) return;
        snapshotWriter.writeDailySnapshot(day, ctx);
    }

    /**
     * 在投票完成后单独记录投票明细数据
     * 必须在 VoteSelector.select() 执行后调用，此时对应轮次票型已被完整记录
     */
    public void recordVoteData(int day, GameContext ctx) {
        if (!active) return;
        snapshotWriter.writeVoteData(day, ctx);
    }

    // ==================== 获取器 ====================

    public List<String> getRecords() { return new ArrayList<>(records); }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
