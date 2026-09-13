package com.vocr.core;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.Event;
import com.vocr.domain.EventName;

class ResultEventGenerator
{
    private final GameContext ctx;

    public ResultEventGenerator(GameContext ctx)
    {
        this.ctx = ctx;
    }

    public void addEvent(EventName name, int... players)
    {
        if (players.length == 1)
            ctx.queueEvent(new Event(name, ctx.getCharacterName(players[0])));
        else
            ctx.queueEvent(new Event(name, ctx.getCharacterName(players[0]),
                    ctx.getCharacterName(players[1])));
    }

    public void addEventWithEnglishName(EventName name, int player)
    {
        ctx.queueEvent(new Event(name,
                CharacterEnglishName.values()[ctx.getCharacterNumber(player)]));
    }
}
