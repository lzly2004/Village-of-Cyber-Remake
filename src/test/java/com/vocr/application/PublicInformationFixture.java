package com.vocr.application;

import com.vocr.core.MainLogicInterface;
import com.vocr.domain.*;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

/** Synthetic read-only fixture shared by projector, Swing parity and real HTTP serialization tests. */
public final class PublicInformationFixture
{
    public int day = 4, cat, fox, deviant;
    public GameResult result = GameResult.NONE;
    public final int[] roles = {0, 1, 2, 3, 0, 1, 6, 4, 6, 6, 6};
    public final int[] actual = {0, 1, 2, 3, 1, 8, 10, 4, 6, 6, 6};
    public final int[] deathDays = new int[11];
    public final whyDie[] deaths = new whyDie[11];
    public final boolean[] marked = new boolean[11];
    public final int[][] skills = new int[11][16], tops = new int[11][3], scores = new int[11][11];

    public PublicInformationFixture()
    {
        Arrays.fill(deaths, whyDie.NONE);
        deaths[5] = whyDie.chuxing; deathDays[5] = 3;
        deaths[6] = whyDie.zhousha; deathDays[6] = 2;
        skills[1][1] = 2; skills[1][2] = 13; skills[1][3] = 4; skills[1][4] = 8;
        skills[2][1] = 11; skills[2][2] = 13; skills[2][3] = 4;
        skills[3][1] = 6; skills[3][2] = 2; skills[3][3] = 5;
        skills[4][1] = 12;
        skills[5][1] = 6; skills[5][2] = 11; skills[5][3] = 2;
        marked[1] = true; marked[4] = true; marked[5] = true;
        tops[4] = new int[]{1, 0, 2};
        scores[4][1] = 100; scores[4][2] = 95;
    }

    public GameContextView context()
    {
        return (GameContextView) Proxy.newProxyInstance(GameContextView.class.getClassLoader(),
                new Class<?>[]{GameContextView.class}, (proxy, method, arguments) -> {
                    int player = arguments == null || arguments.length == 0 ? 0 : (Integer) arguments[0];
                    return switch (method.getName()) {
                        case "getPlayerSum" -> 10;
                        case "getGameDay", "getEffectiveGameDay" -> day;
                        case "getEndResult" -> result;
                        case "getPeiyi" -> peiyi.tongchang;
                        case "getAliveCounter" -> (int) java.util.stream.IntStream.rangeClosed(1, 10).filter(i -> deaths[i] == whyDie.NONE).count();
                        case "getDeathCounter" -> (int) java.util.stream.IntStream.rangeClosed(1, 10).filter(i -> deaths[i] != whyDie.NONE).count();
                        case "getCharacterNumber" -> player;
                        case "getActualRole" -> actual[player];
                        case "getClaimedRole" -> roles[player];
                        case "getClaimedRoleOrder" -> roles[player] > 0 && roles[player] < 6 ? player : 0;
                        case "getComingOutDay" -> player == 1 ? 3 : roles[player] > 0 ? 1 : 0;
                        case "getDeathReason" -> deaths[player];
                        case "getDeathDay" -> deathDays[player];
                        case "isAlive" -> deaths[player] == whyDie.NONE;
                        case "isDead" -> deaths[player] != whyDie.NONE;
                        case "getSkillTarget" -> skills[player][(Integer) arguments[1]];
                        case "isNonHumanMarked" -> marked[player];
                        case "getTop3SuspectedPlayer" -> tops[player][(Integer) arguments[1] - 1];
                        case "getSuspicionValue" -> scores[player][(Integer) arguments[1]];
                        case "getCat" -> cat;
                        case "getFox" -> fox;
                        case "getDeviant" -> deviant;
                        case "isSelectedVoteTarget", "isHiddenSeerScheduled", "isHiddenHunterScheduled", "isClaimedRoleScheduled" -> false;
                        case "getVoteRoundCount" -> 0;
                        case "getVotingCandidates" -> List.of();
                        case "getVotingRule" -> -1;
                        default -> throw new UnsupportedOperationException(method.getName());
                    };
                });
    }

    public GameSession session(String id)
    {
        var context = context();
        var logic = (MainLogicInterface) Proxy.newProxyInstance(MainLogicInterface.class.getClassLoader(),
                new Class<?>[]{MainLogicInterface.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "getGameContext" -> context;
                    case "start", "askCo", "getRecorder" -> null;
                    default -> throw new UnsupportedOperationException(method.getName());
                });
        return new GameSession(id, logic, new GameEventBuffer());
    }

    public GameUpdate update()
    {
        var session = session("public-fixture");
        session.startGame(new StartGameCommand(0, peiyi.tongchang));
        return session.currentUpdate(0);
    }
}
