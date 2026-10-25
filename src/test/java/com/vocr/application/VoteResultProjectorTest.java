package com.vocr.application;

import com.vocr.domain.GameContextView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoteResultProjectorTest
{
    @Test
    void projectHistory_shouldExposeAuthoritativeTieAndFinalResultWithoutSecrets()
    {
        int[][] targets = new int[5][3];
        targets[1][1] = 3;
        targets[2][1] = 4;
        targets[3][1] = 4;
        targets[4][1] = 3;
        targets[1][2] = 3;
        targets[2][2] = 3;
        targets[3][2] = 4;
        targets[4][2] = 3;

        List<VoteResultSnapshot> history = VoteResultProjector.projectHistory(
                context(targets, 1, List.of(1, 2, 3, 4)));

        assertEquals(1, history.size());
        VoteResultSnapshot result = history.get(0);
        assertEquals(2, result.day());
        assertEquals(VotingMethod.GREY, result.method());
        assertEquals(List.of(1, 2, 3, 4), result.candidates());
        assertEquals(2, result.rounds().size());
        assertEquals(List.of(3, 4), result.rounds().get(0).leaders());
        assertTrue(result.rounds().get(0).tied());
        assertEquals(List.of(
                        new VoteLineSnapshot(1, 0, 3),
                        new VoteLineSnapshot(2, 0, 4),
                        new VoteLineSnapshot(3, 2, 4),
                        new VoteLineSnapshot(4, 2, 3)),
                result.rounds().get(0).lines());
        assertEquals(3, result.finalRound().maximumVotes());
        assertEquals(3, result.executedPlayer());
        assertThrows(UnsupportedOperationException.class,
                () -> result.rounds().add(result.finalRound()));

        List<String> publicFields = List.of(
                VoteResultSnapshot.class,
                VoteRoundSnapshot.class,
                VoteLineSnapshot.class).stream()
                .flatMap(type -> List.of(type.getRecordComponents()).stream())
                .map(component -> component.getName())
                .toList();
        assertTrue(publicFields.stream().noneMatch(name ->
                name.contains("actualRole") || name.contains("suspicion")
                        || name.contains("nonHuman")),
                "公开投票投影不能包含真实身份、怀疑度或非人计划");
    }

    @Test
    void votingMethod_shouldMapAllLegacyRulesAndRejectUnknownValues()
    {
        assertEquals(VotingMethod.FREE, VotingMethod.fromLegacyCode(0));
        assertEquals(VotingMethod.GREY, VotingMethod.fromLegacyCode(1));
        assertEquals(VotingMethod.DESIGNATED, VotingMethod.fromLegacyCode(2));
        assertThrows(IllegalArgumentException.class,
                () -> VotingMethod.fromLegacyCode(3));
    }

    private static GameContextView context(
            int[][] targets, int votingRule, List<Integer> candidates)
    {
        return (GameContextView) Proxy.newProxyInstance(
                GameContextView.class.getClassLoader(),
                new Class<?>[]{GameContextView.class},
                (proxy, method, arguments) -> switch (method.getName())
                {
                    case "getPlayerSum" -> 4;
                    case "getEffectiveGameDay" -> 3;
                    case "getVoteRoundCount" -> (Integer) arguments[0] == 2 ? 2 : 0;
                    case "getVotingRule" -> votingRule;
                    case "getVotingCandidates" -> candidates;
                    case "getVoteTarget" -> targets[(Integer) arguments[0]][(Integer) arguments[2]];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
