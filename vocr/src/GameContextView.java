import java.util.List;

/**
 * 游戏上下文访问接口 —— UI层通过此接口查询游戏状态。
 * 以只读查询为主，仅保留投票/CO预约两个必要的写操作。
 * 由 GameContext（Phase 2创建）实现。
 */
interface GameContextView
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
    int getActualRoleIndex(int role);

    // === 职业候补列表（只读）===
    List<Integer> getZhans();
    List<Integer> getLings();
    List<Integer> getLies();
    List<Integer> getMaos();

    // === 技能结果查询 ===
    int getSkillTarget(int player, int day);
    int getVoteTarget(int player, int day, int round);
    boolean isSelectedVoteTarget(int player, int day);

    // === 怀疑度查询 ===
    int getSuspicionValue(int subject, int target);
    int getLazySuspicionValue(int player);
    int getTop3SuspectedPlayer(int player, int rank, int day);
    boolean isAckWhite(int player);

    // === 游戏状态查询 ===
    int getGameDay();
    int getAliveCounter();
    int getDeathCounter();
    int getEndResult();
    peiyi getPeiyi();
    boolean isDoubleDeathOccurred(int day);

    // === 非人策略查询 ===
    int getNonHumanPlan(int player);
    int getZW(int player);
    int getYBZW(int player);

    // === 预告查询 ===
    boolean isClaimedRoleScheduled(int player, int target, int day);
    boolean[][] getClaimedRoleScheduledSkillTargets(int player);
    boolean[][] getHiddenSeerScheduledSkillTargets();
    boolean[][] getHiddenHunterScheduledSkillTargets();

    // === UI层写操作（投票/CO预约）===
    void setIsSelectedVoteTarget(int player, int day, boolean value);
    void setClaimedRoleScheduled(int player, int target, int day, boolean value);

    // === 共有者查询 ===
    int getGyIndex(int n);
    int getExposureProgress();

    // === 人狼查询 ===
    int getRlIndex(int n);
    int getInitialWolfCount();
    boolean isWolfClaimedHunter();
    boolean isWolfClaimedCat();
    int getKyojin();

    // === CO询问查询 ===
    int getClaimedRoleAskDay(int role);

    // === 占灵连线 ===
    int getLined(int zhan, int ling);

    // === 死体查询 ===
    List<Integer> getDiebody();
    int getDiePlayerNum(whyDie why, int day);

    // === 便捷查询 ===
    List<Integer> getClaimedRole(int role, boolean mustAlive);
}