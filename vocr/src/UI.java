import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.List;
//****************前言*******************
//后悔一开始只顾着写，没想过代码复用了
//代码越来越臃肿，还好整个项目的体量不算很大
//下次一定要先规划好了再写，不能想着先写再说
//每次修bug都要被自己这一大块代码整笑了
//明明其中有很多功能可以整合成一个类来多次使用
//但是我却没有这么做，只能说写昏头了
//对于想理解我代码的人说声抱歉，很多注释写的不够详细。
//一开始当成平时上课做的小作业随心所欲了
//后面我想改的时候已经积重难返了（太多了）
//希望以后我能吸取第一次开发项目的教训，重视这些问题
public class UI implements UIInterface
{
    public LinkedList<Event> getEvents()
    {
        return events;
    }
    public enum Scene  //定义界面枚举类型,当前处于什么界面
    {
        DIALOGUE_AFTERNOON(null, 0, 0),//下午
        DIALOGUE_DEATH(null, 0, 0),//白天死亡或和平
        DIALOGUE_CHUXING(null, 0, 0),//处刑
        DIALOGUE_DAY(null, 0, 0),//白天对话
        START_SCENE(null, 0, 0),//开始界面
        INFO_SCENE(null, 0, 0),//信息界面
        INFO_SCENE_1(INFO_SCENE, 1, 7),//信息下一页界面
        INFO_SCENE_1_1(INFO_SCENE_1, 0, 0),//信息下一页界面
        INFO_SCENE_1_2(INFO_SCENE_1, 0, 0),//新增
        INFO_SCENE_1_3(INFO_SCENE_1, 0, 0),//新增
        INFO_SCENE_1_4(INFO_SCENE_1, 0, 0),//新增
        INFO_SCENE_1_5(INFO_SCENE_1, 0, 0),//新增
        INFO_SCENE_1_6(INFO_SCENE_1, 0, 0),//新增
        INFO_SCENE_1_7(INFO_SCENE_1, 0, 0),//新增
        INFO_SCENE_2(INFO_SCENE, 2, 6),//信息下一页界面
        INFO_SCENE_2_1(INFO_SCENE_2, 0, 0),//信息下一页界面
        INFO_SCENE_2_2(INFO_SCENE_2, 0, 0),//新增
        INFO_SCENE_2_3(INFO_SCENE_2, 0, 0),//新增
        INFO_SCENE_2_4(INFO_SCENE_2, 0, 0),//新增
        INFO_SCENE_2_5(INFO_SCENE_2, 0, 0),//新增
        INFO_SCENE_2_6(INFO_SCENE_2, 0, 0),//新增
        INFO_SCENE_3(INFO_SCENE, 3, 6),//信息下一页界面
        INFO_SCENE_3_1(INFO_SCENE_3, 0, 0),//信息下一页界面
        INFO_SCENE_3_2(INFO_SCENE_3, 0, 0),//新增
        INFO_SCENE_3_3(INFO_SCENE_3, 0, 0),//新增
        INFO_SCENE_3_4(INFO_SCENE_3, 0, 0),//新增
        INFO_SCENE_3_5(INFO_SCENE_3, 0, 0),//新增
        INFO_SCENE_3_6(INFO_SCENE_3, 0, 0),//新增
        INFO_SCENE_4(INFO_SCENE, 4, 8),//信息下一页界面
        INFO_SCENE_4_1(INFO_SCENE_4, 0, 0),//信息下一页界面
        INFO_SCENE_4_2(INFO_SCENE_4, 0, 0),//新增
        INFO_SCENE_4_3(INFO_SCENE_4, 0, 0),//新增
        INFO_SCENE_4_4(INFO_SCENE_4, 0, 0),//新增
        INFO_SCENE_4_5(INFO_SCENE_4, 0, 0),//新增
        INFO_SCENE_4_6(INFO_SCENE_4, 0, 0),//新增
        INFO_SCENE_4_7(INFO_SCENE_4, 0, 0),//新增
        INFO_SCENE_4_8(INFO_SCENE_4, 0, 0),//新增
        INFO_SCENE_5(INFO_SCENE, 5, 9),//信息下一页界面
        INFO_SCENE_5_1(INFO_SCENE_5, 0, 0),//新增
        INFO_SCENE_5_2(INFO_SCENE_5, 0, 0),//新增
        INFO_SCENE_5_3(INFO_SCENE_5, 0, 0),//新增
        INFO_SCENE_5_4(INFO_SCENE_5, 0, 0),//新增
        INFO_SCENE_5_5(INFO_SCENE_5, 0, 0),//新增
        INFO_SCENE_5_6(INFO_SCENE_5, 0, 0),//新增
        INFO_SCENE_5_7(INFO_SCENE_5, 0, 0),//新增
        INFO_SCENE_5_8(INFO_SCENE_5, 0, 0),//新增
        INFO_SCENE_5_9(INFO_SCENE_5, 0, 0),//新增
        GAME_SCENE_VOTE(null, 0, 0),//投票主界面
        GAME_SCENE_SELECT(null, 0, 0),//选择关卡界面
        GAME_SCENE_DAY(null, 0, 0),//进入白天
        GAME_SCENE_NIGHT(null, 0, 0),//入夜
        END_VILLAGE(null, 0, 0),//村人获胜
        END_WOLF(null, 0, 0),//狼人获胜
        END_FOX(null, 0, 0),//妖狐获胜
        END_ANIME(null, 0, 0),//结束动画场景
        RECORD_SCENE(null, 0, 0),//战绩统计界面
        ;

        private final Scene parent;
        private final int firstInfoNum;
        private final int subInfoSum;

        Scene(Scene parent, int firstInfoNum, int subInfoSum) {
            this.parent = parent;
            this.firstInfoNum = firstInfoNum;
            this.subInfoSum = subInfoSum;
        }

        public Scene FatherScene() {
            return parent != null ? parent : INFO_SCENE;
        }

        @Override
        public String toString()    //toString方法，获取二级帮助目录的对应的文本文件名字符串
        {
            String originalName = super.toString();
            if (originalName.startsWith("INFO_SCENE"))
            {
                String infoPart = originalName.replace("INFO_SCENE", "Info")
                        .replaceFirst("_", "")
                        .replace("_", "-");
                return infoPart + "-1.txt";
            }
            return originalName;
        }

        public int SubInfoSum() { return subInfoSum; }

        public int FirstInfoNum() { return firstInfoNum; }
    }
    Scene currentScene;//当前是什么场景
    int recordPage = 0; // 战绩页面：0=総合, 1~7=各配役村

    JFrame jFrame;//窗口
    JPanel jPanel;//容器
    JPanel diaPanel;//对话容器

    //定义当前待处理的事件
    UI() {}
    GameContextView ctx;//封装后的游戏状态只读访问
    ResourcesInterface resources;//资源接口
    MainLogicInterface mainLogic;//主逻辑接口
    UIComponentFactory uiComponentFactory;

    /** 获取玩家职业图标文本（便捷转发） */
    public String getJobText(int player) { return uiComponentFactory.getJobText(ctx.getCharacterNumber(player)); }

    private DialogueBox dialogueBox;//对话框封装逻辑
    private Map<Scene, SceneHandler> sceneHandlers = new HashMap<>();//场景处理器注册表
    // 在UI类中添加这个方法
    public void init()
    {
        uiComponentFactory = new UIComponentFactory();
        dialogueBox = new DialogueBox(jPanel);//对话框封装逻辑初始化
        sceneHandlers.put(Scene.START_SCENE, new StartSceneHandler());//注册开始场景处理器
        // 注册所有信息界面场景处理器
        InfoSceneHandler infoHandler = new InfoSceneHandler();
        for (Scene s : Scene.values()) {
            if (s.name().startsWith("INFO_SCENE")) {
                sceneHandlers.put(s, infoHandler);
            }
        }
        // 注册游戏场景处理器
        GameSceneHandler gameSceneHandler = new GameSceneHandler();
        sceneHandlers.put(Scene.GAME_SCENE_SELECT, gameSceneHandler);
        sceneHandlers.put(Scene.GAME_SCENE_NIGHT, gameSceneHandler);
        sceneHandlers.put(Scene.GAME_SCENE_DAY, gameSceneHandler);
        // 注册对话场景处理器
        sceneHandlers.put(Scene.DIALOGUE_DEATH, new DialogueDayDeathHandler());
        sceneHandlers.put(Scene.DIALOGUE_AFTERNOON, new DialogueAfternoonHandler());
        sceneHandlers.put(Scene.GAME_SCENE_VOTE, new GameSceneVoteHandler());
        sceneHandlers.put(Scene.DIALOGUE_DAY, new DialogueDayHandler());
        sceneHandlers.put(Scene.DIALOGUE_CHUXING, new DialogueChuxingHandler());
        sceneHandlers.put(Scene.END_VILLAGE, new EndGameHandler());
        sceneHandlers.put(Scene.END_WOLF, new EndGameHandler());
        sceneHandlers.put(Scene.END_FOX, new EndGameHandler());
        sceneHandlers.put(Scene.END_ANIME, new EndAnimeHandler());
        sceneHandlers.put(Scene.RECORD_SCENE, new RecordSceneHandler());
        currentScene = Scene.START_SCENE;//初始为开始界面
        jFrame = new JFrame(GameConstants.WINDOW_TITLE);
        jFrame.setResizable(false);
        jFrame.setSize(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLocationRelativeTo(null);
        ImageIcon frameIcon = new ImageIcon("vocr/resources/images/Icon2.png");
        jFrame.setIconImage(frameIcon.getImage());
        jPanel = PanelSimpleFactory.createSimplePanel(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT, true, false);
        diaPanel = PanelSimpleFactory.createSimplePanel(GameConstants.WINDOW_WIDTH, GameConstants.WINDOW_HEIGHT, false, false);
        jFrame.add(jPanel);
    }//初始化
    public void resizeComponents()
    {
        jPanel.revalidate();
        jPanel.repaint();
    } //调整所有组件大小和位置
    public void run()
    {
        resources = Game.getInstance().getResources();
        mainLogic = Game.getInstance().getMainLogic();
        ctx = mainLogic.getGameContext();

        // 优先走 Handler 分发
        SceneHandler handler = sceneHandlers.get(currentScene);
        if (handler != null) {
            handler.render(this);
            return;
        }

    }//运行，每次run都会到一个场景

    /** 播放点击音效并跳转到指定场景 */
    public void transitionTo(Scene scene) {
        resources.playSound("click.wav");
        currentScene = scene;
        run();
    }

    private LinkedList<Event> events = new LinkedList<>();//作为事件队列

    public void addEvent(Event event)
    {
        events.add(event);
        if(event != null && !events.isEmpty())DebugLogger.log("事件添加成功且不为空");
    }//添加event
    public void testBtn()
    {
        //测试用按钮，显示信息
        JButton test = new JButton("点我进入");
        test.setBounds(0,0,60,30);
        test.addActionListener(e -> {
            for(int i = 1;i <= ctx.getPlayerSum();++i)
            {
                DebugLogger.log("编号"+i+" "+uiComponentFactory.getJobText(ctx.getCharacterNumber(i)) + " 真实职业："+uiComponentFactory.getZY(ctx.getActualRole(i))+" 声称职业："+uiComponentFactory.getZY(ctx.getClaimedRole(i))
                +" 死亡日期"+ctx.getDeathDay(i) + " 死亡原因" + uiComponentFactory.getWhyDie(ctx.getDeathReason(i)) + " 怀疑度：");
                for(int j = 1;j <= ctx.getPlayerSum();++j)
                {
                    DebugLogger.print(uiComponentFactory.getJobText(ctx.getCharacterNumber(j))+" 为"+ctx.getSuspicionValue(i, j)+" ");
                }
                DebugLogger.log("");
                DebugLogger.print(uiComponentFactory.getJobText(ctx.getCharacterNumber(i)) + " 怀疑前三为" + ctx.getTop3SuspectedPlayer(i, 1, ctx.getGameDay()) + " "+ ctx.getTop3SuspectedPlayer(i, 2, ctx.getGameDay()) + " "+ ctx.getTop3SuspectedPlayer(i, 3, ctx.getGameDay()) + " ");
                DebugLogger.log("");
            }
        });
        jPanel.add(test);
        jPanel.setComponentZOrder(test,0);
    }//设置测试按钮，显示信息
    String levelName = "";//当前关卡职业配置图片，用于投票履历中显示
    List<ImageIcon> linkIcon = new ArrayList<>();//记录接连事件图像
    boolean[] specialEvent ={false};//是不是特殊事件，也就是三个连续的事件组，有特殊显示方式


    int count = 0;//切歌计数
    String name = "";//歌名
    String text = "";//文本
    boolean isAvoid = true;//是否回避，默认开启
    boolean isCo = false;//是否询问co
    int[][] skillTargetPeople;//用于存技能使用对象
    String[][] skillTargetNames;//用于存技能使用对象对应的图标
    int[][] skillTargetOrder;//用于存技能使用的顺序
    int[][] claimedRolenum;//用于存职业编号//[人数+1][天数]用于发球
    ArrayList<Integer> voteChosen = new ArrayList<>();//被指定投票了的
    ArrayList<Integer> zhanChosen = new ArrayList<>();//被指定占卜了的
    ArrayList<Integer> huChosen = new ArrayList<>();//被指定护卫了的
    //三个是用于保存指定的信息，再次进入时不会消失
    boolean[] isVote = {false};//是否指定投票
    boolean[] isZhan = {false};//是否指定占卜
    boolean[] isHu = {false};//是否指定护卫
    //三个都用于投票时指定内容的显示

    // ==================== 第3步: 统一选择面板配置 ====================

    /** 指定面板类型 */

    JTextArea piaoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, GameConstants.FONT_SIZE_VOTE, "");
    JTextArea piaoText1 = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK, GameConstants.FONT_SIZE_VOTE, "");

    int chuxingWho = 0;//记录到底处刑的是谁，用于处刑人名显示
    List<Integer> voteRounds = new ArrayList<>();//记录每天投票是第几轮的票
    List<Integer> voteMethods = new ArrayList<>();//记录每天投票是什么方式
    int[][] greyCharas;//存储每天的灰角色[角色+1][天数]
    int[][] isSelectedVoteTargetCharas;//存储每天的指定角色[角色+1][天数]

    //信息场景
    //InfoScene_A_B 其实就是指从最初的info主界面开始 点击第A个按钮进入的场景中，点击第B个按钮后进入的场景。
}