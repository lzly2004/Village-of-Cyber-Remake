public class ResultEventGenerator
{
    private final GameContext ctx;

    public ResultEventGenerator(GameContext ctx)
    {
        this.ctx = ctx;
    }

    public void addWz17(int num)
    {
        ctx.eventarray.add(new Event(EventName.wz17, ctx.getCharacterName(num)));
    }

    public void addZjgh8b(int num, int target)
    {
        ctx.eventarray.add(new Event(EventName.zjgh8b, ctx.getCharacterName(num),
                ctx.getCharacterName(target)));
    }

    public void addZjgb8(int num, int target)
    {
        ctx.eventarray.add(new Event(EventName.zjgb8, ctx.getCharacterName(num),
                ctx.getCharacterName(target)));
    }

    public void addJbdh8r(int target, int num)
    {
        ctx.eventarray.add(new Event(EventName.jbdh8r,
                ctx.getCharacterName(target),
                ctx.getCharacterName(num)));
    }

    public void addZbdxsw10(int num, int target)
    {
        ctx.eventarray.add(new Event(EventName.zbdxsw10,
                ctx.getCharacterName(num),
                ctx.getCharacterName(target)));
    }

    public void addZspz15ByName(int num)
    {
        ctx.eventarray.add(new Event(EventName.zspz15,
                ctx.getCharacterName(num)));
    }

    public void addZspz15ByEnglish(int num)
    {
        ctx.eventarray.add(new Event(EventName.zspz15,
                CharacterEnglishName.values()[ctx.getCharacterNumber(num)]));
    }

    public void addZs14(int num, int target)
    {
        ctx.eventarray.add(new Event(EventName.zs14,
                ctx.getCharacterName(num),
                ctx.getCharacterName(target)));
    }

    public void addSzsm16(int num, int target)
    {
        ctx.eventarray.add(new Event(EventName.szsm16,
                ctx.getCharacterName(num),
                ctx.getCharacterName(target)));
    }

    public void addJhdh8b(int target, int num)
    {
        ctx.eventarray.add(new Event(EventName.jhdh8b,
                ctx.getCharacterName(target),
                ctx.getCharacterName(num)));
    }

    public void addGprz11r(int target, int num)
    {
        ctx.eventarray.add(new Event(EventName.gprz11r,
                ctx.getCharacterName(target),
                ctx.getCharacterName(num)));
    }

    public void addGprz11p(int other, int target)
    {
        ctx.eventarray.add(new Event(EventName.gprz11p,
                ctx.getCharacterName(other),
                ctx.getCharacterName(target)));
    }

    public void addLjgh19b(int num, int diePlayer)
    {
        ctx.eventarray.add(new Event(EventName.ljgh19b,
                ctx.getCharacterName(num),
                ctx.getCharacterName(diePlayer)));
    }

    public void addLjgb19(int num, int diePlayer)
    {
        ctx.eventarray.add(new Event(EventName.ljgb19,
                ctx.getCharacterName(num),
                ctx.getCharacterName(diePlayer)));
    }
}
