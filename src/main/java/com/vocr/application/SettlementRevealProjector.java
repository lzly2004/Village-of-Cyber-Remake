package com.vocr.application;

import com.vocr.domain.GameContextView;
import com.vocr.domain.GameResult;
import com.vocr.domain.Role;

import java.util.ArrayList;
import java.util.List;

/** 从已结束对局构造真实职业揭晓数据。 */
final class SettlementRevealProjector
{
    private SettlementRevealProjector() { }

    static List<RevealedPlayerRole> project(GameContextView context)
    {
        if (context.getEndResult() == GameResult.NONE)
            throw new IllegalStateException("对局结束前不能构造真实职业揭晓数据");
        List<RevealedPlayerRole> roles = new ArrayList<>(context.getPlayerSum());
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            roles.add(new RevealedPlayerRole(
                    player, Role.values()[context.getActualRole(player)]));
        }
        return List.copyOf(roles);
    }
}
