public class Game
{   //单例模式
    //公共成员，三个类共同维护的外部类的数据成员
    //...
    private static Game instance = null;
    UI ui;
    Resources resources;
    MainLogic mainlogic;
    ReplayManager replayManager;
    GameRecordManager gameRecordManager;
    private Game()
    {
        this.resources = new Resources();
        this.mainlogic = new MainLogic();
        this.replayManager = new ReplayManager();
        this.gameRecordManager = new GameRecordManager();
        this.ui = new UI();
    }
    public static Game getInstance()
    {
        if (instance == null) instance = new Game();
        return instance;
    }
    public void init()
    {
        resources.init();
        ui.init();
    }
    public void run()
    {
        try {
            init();
            DebugLogger.info("Village of Cyber Remake - 游戏启动");
            resources.run();
            ui.run();
        } catch (Exception e) {
            DebugLogger.error("未捕获异常，游戏崩溃: " + e.getMessage());
            e.printStackTrace();
            if (mainlogic != null && mainlogic.getRecorder() != null) {
                try {
                    mainlogic.getRecorder().endGame(0, 0);
                    DebugLogger.info("崩溃时已尝试保存Replay数据（endResult=0, gameDay=0）");
                } catch (Exception ex) {
                    DebugLogger.error("保存Replay数据失败: " + ex.getMessage());
                }
            }
            javax.swing.JOptionPane.showMessageDialog(null,
                "游戏发生错误，请查看日志。\n错误信息: " + e.getMessage(),
                "错误", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
    public static void main(String[] args)
   {
       Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
           DebugLogger.error("线程[" + t.getName() + "]未捕获异常: " + e.getMessage());
           e.printStackTrace();
       });
       getInstance().run();
   }
    public UIInterface getUI()
    {
        return ui;
    }
    public MainLogicInterface getMainLogic()
    {
        return mainlogic;
    }
    public ResourcesInterface getResources()
    {
        return resources;
    }
    public ReplayManager getReplayManager()
    {
        return replayManager;
    }
    public GameRecordManager getGameRecordManager()
    {
        return gameRecordManager;
    }
}