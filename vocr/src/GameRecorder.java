import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏录像/回归测试录制器 — 第0步核心基础设施。
 *
 * 录制一局完整游戏的所有关键事件和状态,用于:
 *   1. 重构前后行为对比(回归测试)
 *   2. 游戏回放/复盘
 *   3. Bug复现
 *
 * 使用方式:
 *   GameRecorder recorder = new GameRecorder("baseline/peiyi_1_seed_42");
 *   recorder.startGame(peiyi, seed, playerCount);
 *   // ... 游戏进行中,在关键节点调用 recorder.recordXxx() ...
 *   recorder.endGame(endResult);
 *   recorder.save(); // 保存到文件
 */
public class GameRecorder
{
    private final String runLabel;           // 本次运行的标签
    private final List<String> records;      // 结构化记录
    public boolean active = false;          // 是否正在录制
    private peiyi gamePeiyi = null;
    private long gameSeed = 0;
    private int playerCount = 0;
    private int gameDurationDays = 0;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public GameRecorder(String runLabel)
    {
        this.runLabel = runLabel;
        this.records = new ArrayList<>();
    }

    // ==================== 游戏生命周期 ====================

    public void startGame(peiyi p, long seed, int playerCount, GameStatus gs)
    {
        this.active = true;
        this.gamePeiyi = p;
        this.gameSeed = seed;
        this.playerCount = playerCount;
        this.records.clear();

        records.add("# ==========================================");
        records.add("# Game Baseline Recording: " + runLabel);
        records.add("# Time: " + LocalDateTime.now().format(DT_FMT));
        records.add("# Peiyi: " + p + " (ordinal=" + p.ordinal() + ")");
        records.add("# Seed: " + seed);
        records.add("# PlayerCount: " + playerCount);
        try {
            GameRecord record = GameRecordManager.getInstance().getRecord();
            int peiyiVillage = (p.ordinal() >= 1 && p.ordinal() <= 7) ? record.playcnt[p.ordinal()] + 1 : -1;
            int totalVillage = record.totalPlayCnt + 1;
            records.add("# VillageCount: " + peiyiVillage + "/" + totalVillage);
        } catch (Exception e) {
            DebugLogger.warn("[GameRecorder] 获取村数信息失败: " + e.getMessage());
            records.add("# VillageCount: -1/-1");
        }
        records.add("# ==========================================");

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
        this.gameDurationDays = gameDay;
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

    // ==================== 关键事件录制 ====================

    public void recordEvent(String category, String details)
    {
        if (!active) return;
        records.add("EVENT|" + category + "|" + details);
    }

    public void recordNightStart(int day, int aliveCount)
    {
        if (!active) return;
        records.add("NIGHT_START|day=" + day + "|alive=" + aliveCount);
    }

    public void recordNightDeaths(int day, List<Integer> dieBody, String causes)
    {
        if (!active) return;
        StringBuilder sb = new StringBuilder("NIGHT_DEATHS|day=" + day + "|");
        for (int i = 0; i < dieBody.size(); i++)
        {
            if (i > 0) sb.append(",");
            sb.append(dieBody.get(i));
        }
        sb.append("|causes=").append(causes);
        records.add(sb.toString());
    }

    public void recordDayStart(int day, int aliveCount)
    {
        if (!active) return;
        records.add("DAY_START|day=" + day + "|alive=" + aliveCount);
    }

    public void recordSkillUse(int day, int actorNumber, int actorActualRole,
                               int claimedRole, int target, String result)
    {
        if (!active) return;
        records.add(String.format("SKILL|day=%d|actor=%d|actualRole=%d|claimedRole=%d|target=%d|result=%s",
                day, actorNumber, actorActualRole, claimedRole, target, result));
    }

    public void recordSeerResult(int day, int seerNumber, int target, boolean isBlack)
    {
        if (!active) return;
        records.add(String.format("SEER|day=%d|seer=%d|target=%d|result=%s",
                day, seerNumber, target, isBlack ? "BLACK" : "WHITE"));
    }

    public void recordMediumResult(int day, int mediumNumber, int executedTarget, boolean isBlack)
    {
        if (!active) return;
        records.add(String.format("MEDIUM|day=%d|medium=%d|executed=%d|result=%s",
                day, mediumNumber, executedTarget, isBlack ? "BLACK" : "WHITE"));
    }

    public void recordWolfBite(int day, int wolfNumber, int target)
    {
        if (!active) return;
        records.add(String.format("WOLF_BITE|day=%d|wolf=%d|target=%d", day, wolfNumber, target));
    }

    public void recordHunterGuard(int day, int hunterNumber, int target)
    {
        if (!active) return;
        records.add(String.format("HUNTER_GUARD|day=%d|hunter=%d|target=%d", day, hunterNumber, target));
    }

    public void recordVoteResult(int day, int round, String voteMethod,
                                  int executedPlayer, int voteCount, boolean isTie)
    {
        if (!active) return;
        records.add(String.format("VOTE|day=%d|round=%d|method=%s|executed=%d|votes=%d|tie=%s",
                day, round, voteMethod, executedPlayer, voteCount, isTie ? "true" : "false"));
    }

    public void recordCoClaim(int day, int playerNumber, int claimedRole, int claimedRoleOrder)
    {
        if (!active) return;
        records.add(String.format("CO|day=%d|player=%d|claimedRole=%d|order=%d",
                day, playerNumber, claimedRole, claimedRoleOrder));
    }

    public void recordAvoidCo(int day, int playerNumber, String avoidType)
    {
        if (!active) return;
        records.add(String.format("AVOID_CO|day=%d|player=%d|type=%s", day, playerNumber, avoidType));
    }

    public void recordNonHumanMarker(int day, int playerNumber, String reason)
    {
        if (!active) return;
        records.add(String.format("MARKER|day=%d|player=%d|reason=%s", day, playerNumber, reason));
    }

    // ==================== 游戏事件录制(Event队列) ====================

    public void recordGameEvent(int day, Event evt)
    {
        if (!active || evt == null) return;
        records.add(String.format("GAME_EVENT|day=%d|event=%s|ch1=%s|ch2=%s|ch3=%s",
                day,
                evt.eventname != null ? evt.eventname.name() : "null",
                evt.ch1 != null ? evt.ch1.name() : "null",
                evt.ch2 != null ? evt.ch2.name() : "null",
                evt.ch3 != null ? evt.ch3.name() : "null"));
    }

    // ==================== 状态快照 ====================

    public void recordStateSnapshot(int day, GameStatus gs)
    {
        if (!active) return;
        StringBuilder sb = new StringBuilder("STATE|day=" + day + "|");
        for (int i = 1; i <= gs.getPlayerSum(); i++)
        {
            GameCharacter gc = gs.gc[i];
            sb.append("{").append(i).append(":")
                    .append("n=").append(gc.number)
                    .append(",r=").append(gc.actualRole)
                    .append(",cr=").append(gc.claimedRole)
                    .append(",cro=").append(gc.claimedRoleorder)
                    .append(",dd=").append(gc.dieDay)
                    .append(",wd=").append(gc.whyDie.ordinal())
                    .append(",nm=").append(gc.nonHumanMarker)
                    .append("}");
        }
        records.add(sb.toString());
    }

    /**
     * 记录每日完整快照（包含存活/死亡状态、技能结果、投票明细）
     * 用于Replay系统的回放数据采集
     */
    public void recordDailySnapshot(int day, GameContext ctx)
    {
        if (!active) return;

        int playerSum = ctx.getPlayerSum();

        // 基础状态快照
        int aliveCount = 0;
        for (int i = 1; i <= playerSum; i++) {
            if (ctx.isAlive(i)) aliveCount++;
        }
        records.add("DAILY_SNAPSHOT|day=" + day + "|alive=" + aliveCount);

        // 详细角色状态
        StringBuilder sb = new StringBuilder("STATE|day=" + day + "|");
        for (int i = 1; i <= playerSum; i++) {
            int charNum = ctx.getCharacterNumber(i);
            if (charNum == 0) {
                DebugLogger.warn("[GameRecorder] ⚠️ 玩家" + i + "的characterNumber=0!");
            }
            sb.append("{").append(i).append(":")
                    .append("n=").append(charNum)
                    .append(",r=").append(ctx.getActualRole(i))
                    .append(",cr=").append(ctx.getClaimedRole(i))
                    .append(",cro=").append(ctx.getClaimedRoleOrder(i))
                    .append(",dd=").append(ctx.getDeathDay(i))
                    .append(",wd=").append(ctx.getDeathReason(i).ordinal())
                    .append(",nm=").append(ctx.isNonHumanMarked(i) ? 1 : 0)
                    .append(",cod=").append(ctx.getComingOutDay(i))
                    .append(",st=").append(ctx.getSkillTarget(i, day - 1))
                    .append("}");
        }
        String stateLine = sb.toString();
        records.add(stateLine);
        DebugLogger.info("[GameRecorder] STATE行: " + stateLine);

        DebugLogger.info("[GameRecorder] 每日快照已记录: day=" + day + " alive=" + aliveCount);
    }

    /**
     * 在投票完成后单独记录投票明细数据
     * 必须在 VoteSelector.select() 执行后调用，此时 voteTarget 已被填充
     */
    public void recordVoteData(int day, GameContext ctx, int dailyVotingRule, List<Integer> greyTargets) {
        if (!active) return;

        // 记录投票方式额外信息
        StringBuilder methodSb = new StringBuilder("VOTE_METHOD|day=" + day + "|rule=" + dailyVotingRule);
        if (greyTargets != null && !greyTargets.isEmpty()) {
            methodSb.append("|grey=");
            for (int g : greyTargets) methodSb.append(g).append(",");
            methodSb.setLength(methodSb.length() - 1);
        }
        // 指定投票対象从ctx读取
        StringBuilder designSb = new StringBuilder();
        for (int i = 1; i <= ctx.getPlayerSum(); i++) {
            if (ctx.isSelectedVoteTarget(i, day)) {
                designSb.append(ctx.getCharacterNumber(i)).append(",");
            }
        }
        if (designSb.length() > 0) {
            designSb.setLength(designSb.length() - 1);
            methodSb.append("|design=").append(designSb);
        }
        records.add(methodSb.toString());
        DebugLogger.info("[GameRecorder] " + methodSb.toString());

        int playerSum = ctx.getPlayerSum();

        // 投票明细（多轮）- 从ctx读取实际的voteTarget数据
        for (int round = 1; round <= 3; round++) {
            boolean hasVote = false;
            int validVoteCnt = 0;
            StringBuilder voteSb = new StringBuilder("VOTE_DETAIL|day=" + day + "|round=" + round + "|");
            for (int i = 1; i <= playerSum; i++) {
                int target = ctx.getVoteTarget(i, day, round);
                if (target > 0 || ctx.getDeathDay(i) == day) {
                    voteSb.append(i).append("->").append(target).append(",");
                    hasVote = true;
                    if (target > 0) validVoteCnt++;
                }
            }
            if (hasVote && validVoteCnt > 0) {
                voteSb.setLength(voteSb.length() - 1);
                records.add(voteSb.toString());
                DebugLogger.info("[GameRecorder] VOTE_DETAIL: " + voteSb.toString());
            }
        }
    }

    // ==================== 保存 ====================

    public void save(String filePath)
    {
        records.add("# ==========================================");
        records.add("# End of recording: " + runLabel);
        records.add("# Duration: " + gameDurationDays + " days");
        records.add("# Saved: " + LocalDateTime.now().format(DT_FMT));
        records.add("# ==========================================");

        try
        {
            java.io.File dir = new java.io.File(filePath).getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();

            try (PrintWriter pw = new PrintWriter(new FileWriter(filePath)))
            {
                for (String line : records)
                    pw.println(line);
            }
            DebugLogger.info("[GameRecorder] 录制文件已保存: " + filePath +
                    " (" + records.size() + " 条记录)");
        }
        catch (IOException e)
        {
            DebugLogger.error("[GameRecorder] 保存录制文件失败: " + e.getMessage());
        }
    }

    public void save() { save("baseline/" + runLabel + ".txt"); }

    // ==================== 获取器 ====================

    public List<String> getRecords() { return new ArrayList<>(records); }
    public boolean isActive() { return active; }
    public String getRunLabel() { return runLabel; }
}