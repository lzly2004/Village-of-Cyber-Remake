package com.vocr.domain;

import java.util.List;

/**
 * 游戏上下文访问接口 —— UI层通过此接口查询游戏状态。
 * 以只读查询为主；迁移期保留的投票和技能预约写操作只允许应用层调用。
 * 由 GameContext（Phase 2创建）实现。
 */
public interface GameContextView
{
    // === 玩家状态查询 ===
    int getPlayerSum();
    boolean isAlive(int player);
    boolean isDead(int player);
    whyDie getDeathReason(int player);
    int getDeathDay(int player);
    boolean isNonHumanMarked(int player);

    // === 角色查询 ===
    int getActualRole(int player);
    int getCharacterNumber(int player);
    int getClaimedRole(int player);
    int getComingOutDay(int player);
    int getClaimedRoleOrder(int player);
    int getClaimedRoleOrderCount(int role);
    int getMedium();
    int getHunter();
    int getCat();
    int getFox();
    int getDeviant();

    // === 职业候补列表（只读）===
    List<Integer> getZhans();
    List<Integer> getLings();
    List<Integer> getLies();
    List<Integer> getMaos();

    // === 技能结果查询 ===
    int getSkillTarget(int player, int day);
    int getVoteTarget(int player, int day, int round);
    boolean isSelectedVoteTarget(int player, int day);
    int getVoteRoundCount(int day);
    int getVotingRule(int day);
    List<Integer> getVotingCandidates(int day);

    // === 怀疑度查询 ===
    int getSuspicionValue(int subject, int target);
    int getTop3SuspectedPlayer(int player, int rank, int day);

    // === 游戏状态查询 ===
    int getGameDay();
    int getEffectiveGameDay();
    int getAliveCounter();
    int getDeathCounter();
    GameResult getEndResult();
    peiyi getPeiyi();
    boolean isDoubleDeathOccurred(int day);

    // === 预告查询 ===
    boolean isClaimedRoleScheduled(int player, int target, int day);
    boolean isHiddenSeerScheduled(int target, int day);
    boolean isHiddenHunterScheduled(int target, int day);

    // === 应用层写操作（投票/技能预约，迁移期保留）===
    void setSelectedVoteTarget(int player, int day, boolean value);
    void setClaimedRoleScheduled(int player, int target, int day, boolean value);
    void setHiddenSeerScheduled(int target, int day, boolean value);
    void setHiddenHunterScheduled(int target, int day, boolean value);

    // === 人狼查询 ===
    boolean isWolfClaimedHunter();
    boolean isWolfClaimedCat();

    // === CO询问查询 ===
    int getClaimedRoleAskDay(int role);

    // === 死体查询 ===
    List<Integer> getDiebody();
    int getDiePlayerNum(whyDie why, int day);

    // === 声称角色查询 ===
    List<Integer> getClaimedRole(int role, boolean mustAlive);
}
