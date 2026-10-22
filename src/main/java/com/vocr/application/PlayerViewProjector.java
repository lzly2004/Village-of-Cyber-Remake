package com.vocr.application;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;
import com.vocr.domain.GameResult;

import java.util.ArrayList;
import java.util.List;

/** 从权威上下文逐字段构造普通玩家白名单视图。 */
final class PlayerViewProjector
{
    private PlayerViewProjector() { }

    static PlayerViewSnapshot project(GameContextView context)
    {
        List<VisiblePlayer> players = new ArrayList<>(context.getPlayerSum());
        for (int player = 1; player <= context.getPlayerSum(); player++)
        {
            players.add(new VisiblePlayer(
                    player,
                    CharacterEnglishName.values()[context.getCharacterNumber(player)],
                    context.isAlive(player),
                    PublicDeathReason.project(context.getDeathReason(player), context.getEndResult() == GameResult.NONE
                            ? BusinessPhase.DAY_DECISION : BusinessPhase.FINISHED),
                    context.getDeathDay(player),
                    Role.values()[context.getClaimedRole(player)],
                    context.getClaimedRoleOrder(player),
                    Math.max(0, context.getComingOutDay(player))));
        }
        return new PlayerViewSnapshot(
                context.getPeiyi(),
                context.getGameDay(),
                context.getAliveCounter(),
                context.getDeathCounter(),
                context.getEndResult(),
                players);
    }
}
