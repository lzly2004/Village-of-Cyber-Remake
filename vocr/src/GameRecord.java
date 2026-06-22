class GameRecord
{
    //各数组的有效下标：1~7，表示各个村的历史游玩情况
    int[] playcnt;//游玩计数
    double[] winrate;//胜率
    int[] villageWincnt;//村胜利计数
    int[] wolfWincnt;//狼胜利计数
    int[] foxWincnt;//狐胜利计数
    int[] wincnt;//连胜计数
    GameRecord()
    {
        playcnt = new int[8];
        winrate = new double[8];
        villageWincnt = new int[8];
        wolfWincnt = new int[8];
        foxWincnt = new int[8];
        wincnt = new int[8];
    }
}