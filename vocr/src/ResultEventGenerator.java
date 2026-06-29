public class ResultEventGenerator
{
    private final GameContext ctx;

    public ResultEventGenerator(GameContext ctx)
    {
        this.ctx = ctx;
    }

    public void addEvent(EventName name, int... players)
    {
        if (players.length == 0)
            ctx.eventarray.add(new Event(name));
        else if (players.length == 1)
            ctx.eventarray.add(new Event(name, ctx.getCharacterName(players[0])));
        else
            ctx.eventarray.add(new Event(name, ctx.getCharacterName(players[0]),
                    ctx.getCharacterName(players[1])));
    }

    public void addEventWithEnglishName(EventName name, int player)
    {
        ctx.eventarray.add(new Event(name,
                CharacterEnglishName.values()[ctx.getCharacterNumber(player)]));
    }
}