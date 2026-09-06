package com.vocr.core;

import com.vocr.config.GameConstants;
import com.vocr.domain.Role;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** CO 过程的共享状态；统一管理询问、顺位、候补和狼方职业占位。 */
final class ClaimState
{
    private static final int SEER = 1;
    private static final int MEDIUM = 2;
    private static final int HUNTER = 3;
    private static final int CAT = 5;

    private final int playerSum;
    private final int[] requestDays;
    private final int[] orderCounts;
    private final ArrayList<Integer> seerCandidates = new ArrayList<>();
    private final ArrayList<Integer> mediumCandidates = new ArrayList<>();
    private final ArrayList<Integer> hunterCandidates = new ArrayList<>();
    private final ArrayList<Integer> catCandidates = new ArrayList<>();
    private boolean wolfClaimedHunter;
    private boolean wolfClaimedCat;

    ClaimState(int playerSum)
    {
        if (playerSum < 1)
            throw new IllegalArgumentException("玩家人数必须为正数: " + playerSum);
        this.playerSum = playerSum;
        this.requestDays = new int[Role.values().length];
        this.orderCounts = new int[Role.values().length];
    }

    int getRequestDay(int role)
    {
        validateRole(role);
        return requestDays[role];
    }

    boolean tryRecordRequest(int role, int day)
    {
        validateClaimRole(role);
        if (day < 1 || day > GameConstants.MAX_GAME_DAYS)
            throw new IllegalArgumentException("CO 询问日期越界: " + day);
        if (requestDays[role] != 0)
            return false;
        requestDays[role] = day;
        return true;
    }

    int getOrderCount(int role)
    {
        validateRole(role);
        return orderCounts[role];
    }

    int nextOrder(int role)
    {
        requiredCandidates(role);
        return ++orderCounts[role];
    }

    boolean hasCandidateList(int role)
    {
        validateClaimRole(role);
        return mutableCandidates(role) != null;
    }

    List<Integer> getCandidates(int role)
    {
        return Collections.unmodifiableList(requiredCandidates(role));
    }

    void addCandidate(int role, int player)
    {
        validatePlayer(player);
        ArrayList<Integer> candidates = requiredCandidates(role);
        if (!candidates.contains(player))
            candidates.add(player);
    }

    void removeCandidate(int role, int player)
    {
        validatePlayer(player);
        requiredCandidates(role).remove(Integer.valueOf(player));
    }

    void shuffleSeerCandidates()
    {
        ConstNum.shuffle(seerCandidates);
    }

    void shuffleMediumCandidates()
    {
        ConstNum.shuffle(mediumCandidates);
    }

    int getNonHumanCandidateCount()
    {
        return Math.max(0, seerCandidates.size() - 1)
                + Math.max(0, mediumCandidates.size() - 1)
                + Math.max(0, hunterCandidates.size() - 1)
                + Math.max(0, catCandidates.size() - 1);
    }

    boolean isWolfClaimedHunter()
    {
        return wolfClaimedHunter;
    }

    boolean isWolfClaimedCat()
    {
        return wolfClaimedCat;
    }

    void markWolfClaimedRole(int role)
    {
        if (role == HUNTER) wolfClaimedHunter = true;
        else if (role == CAT) wolfClaimedCat = true;
        else throw new IllegalArgumentException("狼方标记只支持猎人或猫又 CO: " + role);
    }

    private ArrayList<Integer> requiredCandidates(int role)
    {
        ArrayList<Integer> candidates = mutableCandidates(role);
        if (candidates == null)
            throw new IllegalArgumentException("该职业没有 CO 候补列表: " + role);
        return candidates;
    }

    private ArrayList<Integer> mutableCandidates(int role)
    {
        return switch (role)
        {
            case SEER -> seerCandidates;
            case MEDIUM -> mediumCandidates;
            case HUNTER -> hunterCandidates;
            case CAT -> catCandidates;
            default -> null;
        };
    }

    private void validatePlayer(int player)
    {
        if (player < 1 || player > playerSum)
            throw new IllegalArgumentException("玩家索引越界: " + player);
    }

    private static void validateClaimRole(int role)
    {
        if (role < 1 || role > 6)
            throw new IllegalArgumentException("可声明职业越界: " + role);
    }

    private static void validateRole(int role)
    {
        if (role < 0 || role >= Role.values().length)
            throw new IllegalArgumentException("职业索引越界: " + role);
    }
}
