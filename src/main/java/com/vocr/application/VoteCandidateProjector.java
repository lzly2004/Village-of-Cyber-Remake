package com.vocr.application;

import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 从权威对局状态推导当前三种投票方式的候选项。 */
final class VoteCandidateProjector
{
    private VoteCandidateProjector() { }

    static List<LegalVoteAction> project(GameContextView context)
    {
        List<Integer> alive = alivePlayers(context);
        List<LegalVoteAction> actions = new ArrayList<>();
        actions.add(new LegalVoteAction(VotingMethod.FREE, alive));

        List<Integer> grey = greyPlayers(context);
        if (!grey.isEmpty())
            actions.add(new LegalVoteAction(VotingMethod.GREY, grey));

        List<Integer> designated = alive.stream()
                .filter(player -> context.isSelectedVoteTarget(player, context.getGameDay()))
                .toList();
        if (!designated.isEmpty())
            actions.add(new LegalVoteAction(VotingMethod.DESIGNATED, designated));
        return List.copyOf(actions);
    }

    private static List<Integer> alivePlayers(GameContextView context)
    {
        List<Integer> alive = new ArrayList<>();
        for (int player = 1; player <= context.getPlayerSum(); player++)
            if (context.isAlive(player))
                alive.add(player);
        return List.copyOf(alive);
    }

    private static List<Integer> greyPlayers(GameContextView context)
    {
        Set<Integer> divinedPlayers = new HashSet<>();
        for (int day = 1; day < context.getGameDay(); day++)
        {
            for (int player = 1; player <= context.getPlayerSum(); player++)
            {
                if (context.getClaimedRole(player) != Role.zhan.ordinal())
                    continue;
                int target = context.getSkillTarget(player, day);
                if (target > context.getPlayerSum())
                    target -= context.getPlayerSum();
                divinedPlayers.add(target);
            }
        }

        List<Integer> grey = new ArrayList<>();
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            int claimedRole = context.getClaimedRole(player);
            if (context.isAlive(player)
                    && (claimedRole == Role.NONE.ordinal() || claimedRole == Role.wu.ordinal())
                    && !divinedPlayers.contains(player))
            {
                grey.add(player);
            }
        }
        return List.copyOf(grey);
    }
}
