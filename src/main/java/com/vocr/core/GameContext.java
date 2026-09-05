package com.vocr.core;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Event;
import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.peiyi;
import com.vocr.domain.Role;
import com.vocr.domain.whyDie;

import com.vocr.config.GameConstants;

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
    private final SuspicionSystem suspicion;

    // === 核心状态数组 ===
    private final boolean[] isDoubleDeathOccurred;
    private final int[] nightDeathCount;
    final int[] actualRoleindex;
    final int[] gyindex;
    final int[] rlindex;
    private final int[] claimedRoleAskDays;
    private final int[] claimedRoleOrderCounts;
    final int[] nonHumanPlan;
    private final SkillState skillState;
    private final VoteState voteState;
    final int[][] lined;
    private final ArrayList<Integer> seerCandidates;
    private final ArrayList<Integer> mediumCandidates;
    private final ArrayList<Integer> hunterCandidates;
    private final ArrayList<Integer> catCandidates;
    private final ArrayList<Integer> diebody;
    final ArrayList<Event> eventarray;

    // === 标量状态 ===
    int kyojin;
    int initialWolfCount;
    int initialNonHumanCount;
    private boolean wolfClaimedHunter;
    private boolean wolfClaimedCat;
    int kz;

    GameContext(GameStatus gs)
    {
        this.gs = gs;

        int n = gs.getPlayerSum();
        this.isDoubleDeathOccurred = new boolean[GameConstants.MAX_GAME_DAYS + 1];
        this.nightDeathCount = new int[GameConstants.MAX_GAME_DAYS + 1];
        this.actualRoleindex = new int[Role.values().length + 1];
        this.gyindex = new int[3];
        this.rlindex = new int[n + 1];
        this.claimedRoleAskDays = new int[12];
        this.claimedRoleOrderCounts = new int[12];
        this.nonHumanPlan = new int[n + 1];
        this.skillState = new SkillState(n);
        this.voteState = new VoteState(n);
        this.lined = new int[n + 1][n + 1];

        this.seerCandidates = new ArrayList<>();
        this.mediumCandidates = new ArrayList<>();
        this.hunterCandidates = new ArrayList<>();
        this.catCandidates = new ArrayList<>();
        this.diebody = new ArrayList<>();
        this.eventarray = new ArrayList<>();

        this.suspicion = new SuspicionSystem(this);
    }

    // ==================== 边界检查 ====================

    /** 验证玩家索引在有效范围内 (1..playerSum) */
    public boolean isValidPlayerIndex(int player)
    {
        return player >= 1 && player <= getPlayerSum();
    }

    /** 验证角色索引在有效范围内 (1..RoleCount) */
    public boolean isValidRoleIndex(int role)
    {
        return role >= 1 && role < Role.values().length;
    }

    /** 验证天数在有效范围内 (1..MAX_GAME_DAYS) */
    public boolean isValidDayIndex(int day)
    {
        return day >= 1 && day <= GameConstants.MAX_GAME_DAYS;
    }

    // ==================== GameContextView 实现 ====================

    // --- 玩家状态 ---
    public int getPlayerSum() { return gs.getPlayerSum(); }
    public boolean isAlive(int player) { return player >= 1 && player <= gs.getPlayerSum() && gs.gc[player].isAlive(); }
    public boolean isDead(int player) { return player >= 1 && player <= gs.getPlayerSum() && !gs.gc[player].isAlive(); }
    public whyDie getDeathReason(int player) { return gs.gc[player].getDeathReason(); }
    public int getDeathDay(int player) { return gs.gc[player].getDeathDay(); }
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

    public int getClaimedRole(int player) { return gs.gc[player].getClaimedRole(); }
    public int getComingOutDay(int player) { return gs.gc[player].getComingOutDay(); }
    public int getClaimedRoleOrder(int player) { return gs.gc[player].getClaimedRoleOrder(); }
    public int getClaimedRoleOrderCount(int role) { return claimedRoleOrderCounts[role]; }
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
    public List<Integer> getZhans() { return Collections.unmodifiableList(seerCandidates); }
    public List<Integer> getLings() { return Collections.unmodifiableList(mediumCandidates); }
    public List<Integer> getLies() { return Collections.unmodifiableList(hunterCandidates); }
    public List<Integer> getMaos() { return Collections.unmodifiableList(catCandidates); }

    // --- 技能 ---
    public int getSkillTarget(int player, int day)
    {
        return skillState.getSkillTarget(player, day);
    }
    public int getVoteTarget(int player, int day, int round)
    {
        return voteState.getVoteTarget(player, day, round);
    }
    void recordVoteRound(int day, int round, int[] targets)
    {
        voteState.recordVoteRound(day, round, targets);
    }

    public boolean isSelectedVoteTarget(int player, int day)
    {
        return voteState.isSelectedVoteTarget(player, day);
    }
    public int getVoteRoundCount(int day) { return voteState.getCompletedRoundCount(day); }
    public int getVotingRule(int day) { return voteState.getVotingRule(day); }
    public List<Integer> getVotingCandidates(int day) { return voteState.getVotingCandidates(day); }

    // --- 怀疑度 ---
    public int getSuspicionValue(int subject, int target)
    {
        assert isValidPlayerIndex(subject) : "主体玩家索引越界: " + subject;
        assert isValidPlayerIndex(target) : "目标玩家索引越界: " + target;
        return gs.gc[subject].suspicionValue[target];
    }
    public void setSuspicionValue(int subject, int target, int value)
    {
        assert isValidPlayerIndex(subject) : "主体玩家索引越界: " + subject;
        assert isValidPlayerIndex(target) : "目标玩家索引越界: " + target;
        gs.gc[subject].suspicionValue[target] = value;
    }
    public void addSuspicionValue(int subject, int target, int delta)
    {
        assert isValidPlayerIndex(subject) : "主体玩家索引越界: " + subject;
        assert isValidPlayerIndex(target) : "目标玩家索引越界: " + target;
        gs.gc[subject].suspicionValue[target] += delta;
    }
    public int getTop3SuspectedPlayer(int player, int rank, int day) { return gs.gc[player].top3SuspectedPlayers[rank][day]; }
    public void setTop3SuspectedPlayer(int player, int rank, int day, int value) { gs.gc[player].top3SuspectedPlayers[rank][day] = value; }
    public int[] getSuspicionValueArray(int player) { return gs.gc[player].suspicionValue; }
    public int getLazySuspicionValue(int player) { return suspicion.getLazySuspicionValue()[player]; }
    public void addLazySuspicionValue(int player, int delta) { suspicion.addLazySuspicionValue(player, delta); }
    public void setLazySuspicionValue(int player, int value) { suspicion.setLazySuspicionValue(player, value); }
    public int[] getTop3SuspectedPlayers(int player, int day) { return gs.gc[player].top3SuspectedPlayers[day]; }
    public boolean isAckWhite(int player) { return suspicion.isAckWhite(player); }

    public SuspicionSystem getSuspicion() { return suspicion; }

    // --- 游戏状态 ---
    public int getGameDay() { return gs.gameDay; }
    /** 增加一天时间（游戏日推进） */
    public void incrementGameDay() { gs.gameDay++; }
    public int getAliveCounter() { return gs.getAliveCounter(); }
    public int getDeathCounter() { return gs.getDeathCounter(); }
    public GameResult getEndResult() { return gs.end; }
    public void setEndResult(GameResult result) { gs.end = result; }
    /** 兼容旧接口：返回游戏结果的整数值 */
    public int getEndResultValue() { return gs.end.getValue(); }
    /** 获取有效游戏天数（游戏进行中返回 getGameDay()-1，结束后返回 getGameDay()） */
    public int getEffectiveGameDay() {
        return gs.end == GameResult.NONE ? gs.gameDay - 1 : gs.gameDay;
    }
    public peiyi getPeiyi() { return gs.p; }
    public boolean isDoubleDeathOccurred(int day) { return isDoubleDeathOccurred[day]; }

    // --- 非人策略 ---
    public int getNonHumanPlan(int player)
    {
        assert isValidPlayerIndex(player) : "玩家索引越界: " + player;
        return nonHumanPlan[player];
    }
    int getPlannedDivinationResult(int player)
    {
        return skillState.getPlannedDivinationResult(player);
    }
    int getPreliminaryDivinationTarget(int player)
    {
        return skillState.getPreliminaryDivinationTarget(player);
    }

    // --- 预告 ---
    public boolean isClaimedRoleScheduled(int player, int target, int day)
    {
        return skillState.isClaimedRoleScheduled(player, target, day);
    }
    public boolean isHiddenSeerScheduled(int target, int day) { return skillState.isHiddenSeerScheduled(target, day); }
    public boolean isHiddenHunterScheduled(int target, int day) { return skillState.isHiddenHunterScheduled(target, day); }

    // --- 共有者 ---
    public int getGyIndex(int n) { return gyindex[n]; }

    // --- 人狼 ---
    public int getRlIndex(int n) { return rlindex[n]; }
    public int getInitialWolfCount() { return initialWolfCount; }
    public boolean isWolfClaimedHunter() { return wolfClaimedHunter; }
    public boolean isWolfClaimedCat() { return wolfClaimedCat; }
    public int getKyojin() { return kyojin; }

    // --- CO询问 ---
    public int getClaimedRoleAskDay(int role) { return claimedRoleAskDays[role]; }

    // --- 占灵连线 ---
    public int getLined(int zhan, int ling) { return lined[zhan][ling]; }

    // --- 死体 ---
    public List<Integer> getDiebody() { return List.copyOf(diebody); }

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
        return Math.max(0, seerCandidates.size() - 1) + Math.max(0, mediumCandidates.size() - 1)
             + Math.max(0, hunterCandidates.size() - 1) + Math.max(0, catCandidates.size() - 1);
    }

    // --- 便捷查询 ---
    public List<Integer> getClaimedRole(int role, boolean mustAlive)
    {
        List<Integer> result = new ArrayList<>();
        for (int i = 1; i <= gs.getPlayerSum(); i++)
            if (gs.gc[i].getClaimedRole() == role && (!mustAlive || gs.gc[i].isAlive()))
                result.add(i);
        return result;
    }
    // ==================== Action API（状态修改） ====================

    /** 标记玩家为非人破绽 */
    void markNonHuman(int player) { gs.gc[player].nonHumanMarker = true; }

    /** 设置当天的指定投票候补。 */
    public void setSelectedVoteTarget(int player, int day, boolean value)
    {
        voteState.setSelectedVoteTarget(player, day, value);
    }

    void completeVoting(int day, int votingRule, List<Integer> candidates, int roundCount)
    {
        voteState.completeVoting(day, votingRule, candidates, roundCount);
    }

    /** 记录技能目标或带颜色编码的技能结果；0 表示清空。 */
    void recordSkillTarget(int player, int day, int target)
    {
        skillState.recordSkillTarget(player, day, target);
    }

    /** 记录非人预备占文，仅允许实际玩家目标。 */
    void recordPreliminaryDivinationTarget(int player, int target)
    {
        skillState.recordPreliminaryDivinationTarget(player, target);
    }

    /** 记录非人计划占文，支持以 playerSum 偏移编码的黑结果和 0 空值。 */
    void recordPlannedDivinationResult(int player, int target)
    {
        skillState.recordPlannedDivinationResult(player, target);
    }

    /**
     * 原子登记一次主动 CO：职业、首次 CO 日、同职业顺位和候补列表一起更新。
     */
    void registerClaim(int player, int role)
    {
        validateClaim(player, role, getGameDay());
        GameCharacter character = gs.gc[player];
        character.recordClaimIdentity(role, getGameDay());

        ArrayList<Integer> candidates = mutableClaimCandidates(role);
        if (candidates != null)
        {
            if (character.getClaimedRoleOrder() == 0)
                character.assignClaimOrder(++claimedRoleOrderCounts[role]);
            if (!candidates.contains(player))
                candidates.add(player);
        }

        if (character.actualRole == 7 && (role == 3 || role == 5))
            markWolfClaimedRole(role);
    }

    /** 强制公开职业身份，不把玩家登记为主动 CO 候补。 */
    void revealClaim(int player, int role, int day)
    {
        validateClaim(player, role, day);
        gs.gc[player].recordClaimIdentity(role, day);
    }

    /** 首次记录某职业被询问 CO；已经记录时保持原日期并返回 false。 */
    boolean tryRecordClaimRequest(int role, int day)
    {
        if (!isValidClaimRole(role))
            throw new IllegalArgumentException("可声明职业越界: " + role);
        if (!isValidDayIndex(day))
            throw new IllegalArgumentException("CO 询问日期越界: " + day);
        if (claimedRoleAskDays[role] != 0)
            return false;
        claimedRoleAskDays[role] = day;
        return true;
    }

    /** 添加一个尚未正式 CO 的职业候补。 */
    void addClaimCandidate(int role, int player)
    {
        if (!isValidPlayerIndex(player))
            throw new IllegalArgumentException("玩家索引越界: " + player);
        ArrayList<Integer> candidates = requiredMutableClaimCandidates(role);
        if (!candidates.contains(player))
            candidates.add(player);
    }

    /** 移除一个尚未正式 CO 的职业候补。 */
    void removeClaimCandidate(int role, int player)
    {
        if (!isValidPlayerIndex(player))
            throw new IllegalArgumentException("玩家索引越界: " + player);
        requiredMutableClaimCandidates(role).remove(Integer.valueOf(player));
    }

    /** 记录非人阵营已经规划或完成猎人/猫又 CO。 */
    void markWolfClaimedRole(int role)
    {
        if (role == 3) wolfClaimedHunter = true;
        else if (role == 5) wolfClaimedCat = true;
        else throw new IllegalArgumentException("狼方标记只支持猎人或猫又 CO: " + role);
    }

    private void validateClaim(int player, int role, int day)
    {
        if (!isValidPlayerIndex(player))
            throw new IllegalArgumentException("玩家索引越界: " + player);
        if (!isValidClaimRole(role))
            throw new IllegalArgumentException("可声明职业越界: " + role);
        if (!isValidDayIndex(day))
            throw new IllegalArgumentException("CO 日期越界: " + day);
    }

    private boolean isValidClaimRole(int role)
    {
        return role >= 1 && role <= 6;
    }

    private ArrayList<Integer> mutableClaimCandidates(int role)
    {
        return switch (role)
        {
            case 1 -> seerCandidates;
            case 2 -> mediumCandidates;
            case 3 -> hunterCandidates;
            case 5 -> catCandidates;
            default -> null;
        };
    }

    private ArrayList<Integer> requiredMutableClaimCandidates(int role)
    {
        ArrayList<Integer> candidates = mutableClaimCandidates(role);
        if (candidates == null)
            throw new IllegalArgumentException("该职业没有 CO 候补列表: " + role);
        return candidates;
    }

    /**
     * 原子记录玩家死亡：死因、死亡日和生死计数必须在同一次调用中完成。
     */
    void recordDeath(int player, whyDie reason)
    {
        if (!isValidPlayerIndex(player))
            throw new IllegalArgumentException("玩家索引越界: " + player);
        if (reason == null || reason == whyDie.NONE)
            throw new IllegalArgumentException("死亡原因必须是有效的非 NONE 值");
        if (!isValidDayIndex(getGameDay()))
            throw new IllegalStateException("当前游戏天数越界: " + getGameDay());

        gs.recordDeath(player, reason, getGameDay());
    }

    /** 设置共有者下标 */
    void setGyIndex(int n, int player) { gyindex[n] = player; }

    /** 设置人狼下标 */
    void setRlIndex(int n, int player) { rlindex[n] = player; }

    /** 设置单人职业下标 */
    void setActualRoleIndex(int role, int player) { actualRoleindex[role] = player; }

    /** 设置已 CO 职业的技能预定目标。 */
    public void setClaimedRoleScheduled(int player, int target, int day, boolean value)
    {
        skillState.setClaimedRoleScheduled(player, target, day, value);
    }

    /** 设置潜伏占卜师的技能预定目标。 */
    public void setHiddenSeerScheduled(int target, int day, boolean value)
    {
        skillState.setHiddenSeerScheduled(target, day, value);
    }

    /** 设置潜伏猎人的技能预定目标。 */
    public void setHiddenHunterScheduled(int target, int day, boolean value)
    {
        skillState.setHiddenHunterScheduled(target, day, value);
    }

    /**
     * 原子替换本夜尸体快照，并同步夜间死亡数和双死标记。
     */
    void recordNightDeaths(List<Integer> deaths)
    {
        if (deaths == null)
            throw new IllegalArgumentException("夜间死亡列表不能为空");

        int day = getGameDay();
        if (!isValidDayIndex(day))
            throw new IllegalStateException("当前游戏天数越界: " + day);

        ArrayList<Integer> snapshot = new ArrayList<>(deaths.size());
        for (Integer player : deaths)
        {
            if (player == null || !isValidPlayerIndex(player))
                throw new IllegalArgumentException("夜间死亡玩家索引无效: " + player);
            if (snapshot.contains(player))
                throw new IllegalArgumentException("夜间死亡列表包含重复玩家: " + player);
            if (isAlive(player) || getDeathDay(player) != day || !getDeathReason(player).isNightDeath())
                throw new IllegalStateException("玩家没有在本夜以夜间原因死亡: " + player);
            snapshot.add(player);
        }

        diebody.clear();
        diebody.addAll(snapshot);
        nightDeathCount[day] = snapshot.size();
        isDoubleDeathOccurred[day] = snapshot.size() > 1;
    }

    int getNightDeathCount(int day) { return nightDeathCount[day]; }

    /** 判断猫又是否明确出局 */
    public boolean isCatDefinitivelyOut()
    {
        int cat = getCat();
        if (cat == 0) return true;
        
        whyDie deathReason = getDeathReason(cat);
        
        // 条件1: 猫又白天被处刑并发动猫咒（chuxing或daymaozhou）
        if (deathReason == whyDie.chuxing || deathReason == whyDie.daymaozhou)
            return true;
        
        // 条件2: 累计两晚以上夜间死者不止一个
        int doubleDeathCount = 0;
        for (int day = 1; day <= getGameDay(); day++)
        {
            if (isDoubleDeathOccurred[day])
                doubleDeathCount++;
        }
        if (doubleDeathCount >= 2)
            return true;
        
        // 条件3: 某个夜间死亡4人
        for (int day = 1; day <= getGameDay(); day++)
        {
            if (nightDeathCount[day] >= 4)
                return true;
        }
        
        return false;
    }

    void shuffleZhans() { ConstNum.shuffle(seerCandidates); }
    void shuffleLings() { ConstNum.shuffle(mediumCandidates); }

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
