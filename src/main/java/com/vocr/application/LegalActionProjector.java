package com.vocr.application;

import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;
import com.vocr.domain.SelectionType;

import java.util.ArrayList;
import java.util.List;

/** 构造 CO 询问和目标预约所需的白名单合法选项。 */
final class LegalActionProjector
{
    private static final List<Role> CLAIMABLE_ROLES = List.of(
            Role.zhan, Role.ling, Role.lie, Role.gong, Role.mao);

    private LegalActionProjector() { }

    static ClaimInquiryOptions claimInquiry(GameContextView context)
    {
        return new ClaimInquiryOptions(CLAIMABLE_ROLES, alivePlayers(context));
    }

    static List<LegalTargetSelection> targetSelections(GameContextView context)
    {
        List<Integer> targets = alivePlayers(context);
        return List.of(
                new LegalTargetSelection(SelectionType.VOTE, targets, List.of()),
                new LegalTargetSelection(
                        SelectionType.DIVINATION, targets, claimedActors(context, Role.zhan)),
                new LegalTargetSelection(
                        SelectionType.GUARD, targets, claimedActors(context, Role.lie)));
    }

    private static List<Integer> alivePlayers(GameContextView context)
    {
        List<Integer> players = new ArrayList<>();
        for (int player = 1; player <= context.getPlayerSum(); player++)
            if (context.isAlive(player))
                players.add(player);
        return List.copyOf(players);
    }

    private static List<ClaimedRoleActor> claimedActors(GameContextView context, Role role)
    {
        List<ClaimedRoleActor> actors = new ArrayList<>();
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            if (context.isAlive(player) && context.getClaimedRole(player) == role.ordinal())
            {
                actors.add(new ClaimedRoleActor(
                        player, context.getClaimedRoleOrder(player)));
            }
        }
        return List.copyOf(actors);
    }
}
