class GameInfo
{//游戏信息封装类，用于游戏存档与对局查看
    int days;//游戏一共进行多少天
    GameStatus gs[];
    GameInfo(GameStatus ggs[],int days)
    {
        //赋值
        this.days = days;
        gs = new GameStatus[days + 1];
        for(int i=0;i<=days;i++)
            gs[i] = ggs[i];
    }
}