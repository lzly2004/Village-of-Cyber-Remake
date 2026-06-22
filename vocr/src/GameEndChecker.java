public class GameEndChecker
{
    private final GameContext ctx;

    public GameEndChecker(GameContext ctx)
    {
        this.ctx = ctx;
    }

    public int check()
    {
        int humancnt = 0, wolfcnt = 0;
        for (int i = 1; i <= ctx.getPlayerSum(); i++)
        {
            if (!ctx.isAlive(i)) continue;
            if (ctx.getActualRole(i) == 7)
            {
                wolfcnt++;
                continue;
            }
            if (ctx.getActualRole(i) == 10) continue;
            humancnt++;
        }
        if (ctx.getActualRoleIndex(10) > 0 && ctx.isAlive(ctx.getActualRoleIndex(10))
                && (wolfcnt == 0 || wolfcnt >= humancnt))
        {
            return 3;
        }
        if (wolfcnt == 0)
        {
            return 1;
        }
        if ((ctx.getActualRoleIndex(8) > 0 && ctx.isAlive(ctx.getActualRoleIndex(8)))
                || (ctx.getActualRoleIndex(9) > 0 && ctx.isAlive(ctx.getActualRoleIndex(9))))
        {
            wolfcnt++;
            humancnt--;
        }
        if ((ctx.getActualRoleIndex(10) < 1 || !ctx.isAlive(ctx.getActualRoleIndex(10)))
                && wolfcnt >= humancnt)
            return 2;
        return 0;
    }
}