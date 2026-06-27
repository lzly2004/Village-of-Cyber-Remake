import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 游戏上下文 —— 游戏状态的唯一权威来源。
 * 实现 GameContextView（只读查询），并提供 Action API（状态修改）。
 * Phase 2: 封装 GameStatus + 核心数组，逐步替代 MainLogic 中的裸字段。
 */
class GameContext implements GameContextView
{
    private final GameStatus gs;
    private SuspicionSystem suspicion;

    // === 核心状态数组 ===
    final boolean[] isDoubleDeathOccurred;
    final int[] actualRoleindex;
    final int[] gyindex;
    final int[] rlindex;
    final int[] claimedRoleaskday;
    final int[] claimedRoleorder;
    final int[] nonHumanPlan;
    final int[] ybzw;
    final int[] zw;
    final int[][] lined;
    final ArrayList<Integer> zhans;
    final ArrayList<Integer> lings;
    final ArrayList<Integer> lies;
    final ArrayList<Integer> maos;
    final ArrayList<Integer> diebody;
    final ArrayList<Event> eventarray;

    // === 标量状态 ===
    int kyojin;
    int exposureProgress;
    int initialWolfCount;
    int initialNonHumanCount;
    boolean rlsl;
    boolean rlsm;
    int kz;

    GameContext(GameStatus gs, SuspicionSystem suspicion)
    {
        this.gs = gs;
        this.suspicion = suspicion;

        int n = gs.getPlayerSum();
        this.isDoubleDeathOccurred = new boolean[GameConstants.MAX_GAME_DAYS + 1];
        this.actualRoleindex = new int[Role.values().length + 1];
        this.gyindex = new int[3];
        this.rlindex = new int[n + 1];
        this.claimedRoleaskday = new int[12];
        this.claimedRoleorder = new int[12];
        this.nonHumanPlan = new int[n + 1];
        this.ybzw = new int[n + 1];
        this.zw = new int[n * 2 + 1];
        this.lined = new int[n + 1][n + 1];

        this.zhans = new ArrayList<>();
        this.lings = new ArrayList<>();
        this.lies = new ArrayList<>();
        this.maos = new ArrayList<>();
        this.diebody = new ArrayList<>();
        this.eventarray = new ArrayList<>();
    }

    // ==================== GameContextView 实现 ====================

    // --- 玩家状态 ---
    public int getPlayerSum() { return gs.getPlayerSum(); }
    public boolean isAlive(int player) { return player >= 1 && player <= gs.getPlayerSum() && gs.gc[player].whyDie == whyDie.NONE; }
    public boolean isDead(int player) { return player >= 1 && player <= gs.getPlayerSum() && gs.gc[player].whyDie != whyDie.NONE; }
    public whyDie getDeathReason(int player) { return gs.gc[player].whyDie; }
    public int getDeathDay(int player) { return gs.gc[player].dieDay; }
    public boolean isNonHumanMarked(int player) { return gs.gc[player].nonHumanMarker; }

    // --- 角色 ---
    public int getActualRole(int player) { return gs.gc[player].actualRole; }
    public int getCharacterNumber(int player) { return gs.gc[player].number; }
    public CharacterEnglishName getCharacterName(int player) { return CharacterEnglishName.values()[gs.gc[player].number]; }

    /** 灵能者（役职2） */
    public int getMedium() { return actualRoleindex[2]; }
    /** 猎人（役职3） */
    public int getHunter() { return actualRoleindex[3]; }
    /** 猫又（役职5） */
    public int getCat() { return actualRoleindex[5]; }
    /** 妖狐（役职10） */
    public int getFox() { return actualRoleindex[10]; }
    /** 背德者（役职11） */
    public int getDeviant() { return actualRoleindex[11]; }

    public int getClaimedRole(int player) { return gs.gc[player].claimedRole; }
    public int getComingOutDay(int player) { return gs.gc[player].comingOutDay; }
    public int getClaimedRoleOrder(int player) { return gs.gc[player].claimedRoleorder; }
    public int getClaimedRoleOrderCount(int role) { return claimedRoleorder[role]; }
    public int getActualRoleIndex(int role) { return actualRoleindex[role]; }
    public int getDiePlayerNum(whyDie why, int day)
    {
        int num = -1;
        for (int i = 1; i <= gs.getPlayerSum(); i++)
        {
            if (getDeathReason(i) != why || getDeathDay(i) != day) continue;
            if (num > -1) return -1;
            num = i;
        }
        return num;
    }

    // --- 职业候补 ---
    public List<Integer> getZhans() { return Collections.unmodifiableList(zhans); }
    public List<Integer> getLings() { return Collections.unmodifiableList(lings); }
    public List<Integer> getLies() { return Collections.unmodifiableList(lies); }
    public List<Integer> getMaos() { return Collections.unmodifiableList(maos); }

    // --- 技能 ---
    public int getSkillTarget(int player, int day) { return gs.gc[player].skillTarget[day]; }
    public int getVoteTarget(int player, int day, int round) { return gs.gc[player].voteTarget[day][round]; }
    void setVoteTarget(int player, int day, int round, int target) { gs.gc[player].voteTarget[day][round] = target; }

    public boolean isSelectedVoteTarget(int player, int day) { return gs.gc[player].isSelectedVoteTarget[day]; }

    // --- 怀疑度 ---
    public int getSuspicionValue(int subject, int target) { return gs.gc[subject].suspicionValue[target]; }
    public void setSuspicionValue(int subject, int target, int value) { gs.gc[subject].suspicionValue[target] = value; }
    public void addSuspicionValue(int subject, int target, int delta) { gs.gc[subject].suspicionValue[target] += delta; }
    public int getTop3SuspectedPlayer(int player, int rank, int day) { return gs.gc[player].top3SuspectedPlayers[rank][day]; }
    public void setTop3SuspectedPlayer(int player, int rank, int day, int value) { gs.gc[player].top3SuspectedPlayers[rank][day] = value; }
    public int[] getSuspicionValueArray(int player) { return gs.gc[player].suspicionValue; }
    public int getLazySuspicionValue(int player) { return suspicion != null ? suspicion.getLazySuspicionValue()[player] : 0; }
    public void addLazySuspicionValue(int player, int delta) { if (suspicion != null) suspicion.addLazySuspicionValue(player, delta); }
    public void setLazySuspicionValue(int player, int value) { if (suspicion != null) suspicion.setLazySuspicionValue(player, value); }
    public int[] getTop3SuspectedPlayers(int player, int day) { return gs.gc[player].top3SuspectedPlayers[day]; }
    public boolean isAckWhite(int player) { return suspicion != null && suspicion.isAckWhite(player, maos, isDoubleDeathOccurred, claimedRoleaskday); }

    void setSuspicion(SuspicionSystem s) { this.suspicion = s; }

    // --- 游戏状态 ---
    public int getGameDay() { return gs.gameDay; }
    public int getAliveCounter() { return gs.aliveCounter; }
    public int getDeathCounter() { return gs.deathCounter; }
    public void decrementAliveCounter() { gs.aliveCounter--; }
    public void incrementDeathCounter() { gs.deathCounter++; }
    public int getEndResult() { return gs.end; }
    public void setEndResult(int result) { gs.end = result; }
    public peiyi getPeiyi() { return gs.p; }
    public boolean isDoubleDeathOccurred(int day) { return isDoubleDeathOccurred[day]; }

    // --- 非人策略 ---
    public int getNonHumanPlan(int player) { return nonHumanPlan[player]; }
    public int getZW(int player) { return zw[player]; }
    public int getYBZW(int player) { return ybzw[player]; }

    // --- 预告 ---
    public boolean isClaimedRoleScheduled(int player, int target, int day) { return gs.gc[player].claimedRoleScheduledSkillTargets[target][day]; }
    public boolean[][] getClaimedRoleScheduledSkillTargets(int player) { return gs.gc[player].claimedRoleScheduledSkillTargets; }
    public boolean[][] getHiddenSeerScheduledSkillTargets() { return gs.hiddenSeerScheduledSkillTargets; }
    public boolean[][] getHiddenHunterScheduledSkillTargets() { return gs.hiddenHunterScheduledSkillTargets; }

    // --- 共有者 ---
    public int getGyIndex(int n) { return gyindex[n]; }
    public int getExposureProgress() { return exposureProgress; }

    // --- 人狼 ---
    public int getRlIndex(int n) { return rlindex[n]; }
    public int getInitialWolfCount() { return initialWolfCount; }
    public boolean isWolfClaimedHunter() { return rlsl; }
    public boolean isWolfClaimedCat() { return rlsm; }
    public int getKyojin() { return kyojin; }

    // --- CO询问 ---
    public int getClaimedRoleAskDay(int role) { return claimedRoleaskday[role]; }

    // --- 占灵连线 ---
    public int getLined(int zhan, int ling) { return lined[zhan][ling]; }

    // --- 死体 ---
    public List<Integer> getDiebody() { return Collections.unmodifiableList(diebody); }

    /** 计算夜间死亡对白牌数量的扣除（占灵共用逻辑） */
    public int computeNightDeathDecrement() {
        int dec = 0;
        for (int i = 1; i < gs.gameDay; i++) {
            ArrayList<Integer> array = dieAtNight(i);
            dec += (array.size() < 2) ? array.size() : array.size() - 1;
        }
        return dec;
    }

    /** 计算职业候补对非人阵营的计数（占灵共用逻辑） */
    public int computeFeisum() {
        return Math.max(0, zhans.size() - 1) + Math.max(0, lings.size() - 1)
             + Math.max(0, lies.size() - 1) + Math.max(0, maos.size() - 1);
    }

    // --- 便捷查询 ---
    public List<Integer> getClaimedRole(int role, boolean mustAlive)
    {
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i <= gs.getPlayerSum(); i++)
            if (gs.gc[i].claimedRole == role && (!mustAlive || gs.gc[i].whyDie == whyDie.NONE))
                result.add(i);
        return result;
    }
    // ==================== Action API（状态修改） ====================

    /** 标记玩家为非人破绽 */
    void markNonHuman(int player) { gs.gc[player].nonHumanMarker = true; }

    /** 设置指定投票目标 */
    public void setIsSelectedVoteTarget(int player, int day, boolean value) { gs.gc[player].isSelectedVoteTarget[day] = value; }

    /** 设置技能目标 */
    void setSkillTarget(int player, int day, int target) { gs.gc[player].skillTarget[day] = target; }

    /** 设置声称职业 */
    void setClaimedRole(int player, int role) { gs.gc[player].claimedRole = role; }

    /** 设置CO日 */
    void setComingOutDay(int player, int day) { gs.gc[player].comingOutDay = day; }

    /** 设置死亡日 */
    void setDeathDay(int player, int day) { gs.gc[player].dieDay = day; }

    /** 设置死因 */
    void setDeathReason(int player, whyDie reason) { gs.gc[player].whyDie = reason; }

    /** 设置共有者下标 */
    void setGyIndex(int n, int player) { gyindex[n] = player; }

    /** 设置人狼下标 */
    void setRlIndex(int n, int player) { rlindex[n] = player; }

    /** 设置单人职业下标 */
    void setActualRoleIndex(int role, int player) { actualRoleindex[role] = player; }

    /** 递增职业位次 */
    int incrementClaimedRoleOrder(int role) { return ++claimedRoleorder[role]; }

    /** 设置玩家职业位次 */
    void setClaimedRoleOrder(int player, int order) { gs.gc[player].claimedRoleorder = order; }

    /** 设置CO技能预定目标 */
    public void setClaimedRoleScheduled(int player, int target, int day, boolean value) { gs.gc[player].claimedRoleScheduledSkillTargets[target][day] = value; }

    /** 设置双死标记 */
    void setDoubleDeathOccurred(int day) { isDoubleDeathOccurred[day] = true; }

    /** 添加占候补 */
    void addZhan(int player) { if (!zhans.contains(player)) zhans.add(player); }
    /** 添加灵候补 */
    void addLing(int player) { if (!lings.contains(player)) lings.add(player); }
    /** 添加猎候补 */
    void addLie(int player) { if (!lies.contains(player)) lies.add(player); }
    /** 添加猫候补 */
    void addMao(int player) { if (!maos.contains(player)) maos.add(player); }
    void shuffleZhans() { java.util.Collections.shuffle(zhans); }
    void shuffleLings() { java.util.Collections.shuffle(lings); }

    /** 获取指定日期夜间死亡玩家列表 */
    public ArrayList<Integer> dieAtNight(int day)
    {
        ArrayList<Integer> array = new ArrayList<>();
        for (int i = 1; i <= gs.getPlayerSum(); i++)
            if (getDeathDay(i) == day && getDeathReason(i) != whyDie.chuxing
                    && getDeathReason(i) != whyDie.dayhouzhui
                    && getDeathReason(i) != whyDie.daymaozhou)
                array.add(i);
        return array;
    }

    /** 根据配役初始化狼数量、非人数量、狂人标记 */
    void initFromPeiyi(peiyi p)
    {
        switch (p)
        {
            case jianyi:  initialWolfCount = 2; initialNonHumanCount = 3; kyojin = 1; break;
            case tongchang: initialWolfCount = 3; initialNonHumanCount = 4; kyojin = 1; break;
            case yaoohu: initialWolfCount = 3; initialNonHumanCount = 5; kyojin = 1; break;
            case kuangxin: initialWolfCount = 3; initialNonHumanCount = 5; kyojin = 0; break;
            case beide:  initialWolfCount = 3; initialNonHumanCount = 6; kyojin = 1; break;
            case maoyou: initialWolfCount = 4; initialNonHumanCount = 6; kyojin = 1; break;
            case daxing:  initialWolfCount = 4; initialNonHumanCount = 7; kyojin = 0; break;
        }
    }

    /** 获取非人统领（狂人或狂信者）的职业编号 */
    public int getNonHumanLeader() { return actualRoleindex[9 - kyojin]; }

    /** 判断技能结果是否是"黒"（结果编号 > 玩家总数 = 黒判定） */
    public boolean isBlackResult(int skillTarget) { return skillTarget > gs.getPlayerSum(); }

    /** 获取实际目标编号（黒判定时自动减去玩家总数，白判定时不变） */
    public int getActualTarget(int skillTarget) { return skillTarget > gs.getPlayerSum() ? skillTarget - gs.getPlayerSum() : skillTarget; }
}