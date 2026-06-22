public class Game
{   //单例模式
    //公共成员，三个类共同维护的外部类的数据成员
    //...
    private static Game instance = null;
    UI ui;
    Resources resources;
    MainLogic mainlogic;
    private Game()
    {
        this.resources = new Resources();
        this.mainlogic = new MainLogic();
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
        init();
        DebugLogger.info("Village of Cyber Remake - 游戏启动");
        resources.run();
        ui.run();
    }
    public static void main(String[] args)
   {
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
}