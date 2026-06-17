import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.sound.sampled.*;
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
    public GameStatus getGameStatus()
    {//提供给MainLogic类，得到当前的游戏状态
        return gs;
    }//返回给mainLogic类，方便其获取gs
    public LinkedList<Event> getEvents()
    {
        return events;
    }
    public enum Scene  //定义界面枚举类型,当前处于什么界面
    {
        DIALOGUE_AFTERNOON,//下午
        DIALOGUE_DEATH,//白天死亡或和平
        DIALOGUE_CHUXING,//处刑
        DIALOGUE_DAY,//白天对话
        START_SCENE,//开始界面
        INFO_SCENE,//信息界面
        INFO_SCENE_1,//信息下一页界面
        INFO_SCENE_1_1,//信息下一页界面
        INFO_SCENE_1_2,//新增
        INFO_SCENE_1_3,//新增
        INFO_SCENE_1_4,//新增
        INFO_SCENE_1_5,//新增
        INFO_SCENE_1_6,//新增
        INFO_SCENE_1_7, //新增
        INFO_SCENE_2,//信息下一页界面
        INFO_SCENE_2_1,//信息下一页界面
        INFO_SCENE_2_2,//新增
        INFO_SCENE_2_3,//新增
        INFO_SCENE_2_4,//新增
        INFO_SCENE_2_5,//新增
        INFO_SCENE_2_6,//新增
        INFO_SCENE_3,//信息下一页界面
        INFO_SCENE_3_1,//信息下一页界面
        INFO_SCENE_3_2,//新增
        INFO_SCENE_3_3,//新增
        INFO_SCENE_3_4,//新增
        INFO_SCENE_3_5,//新增
        INFO_SCENE_3_6,//新增
        INFO_SCENE_4,//信息下一页界面
        INFO_SCENE_4_1,//信息下一页界面
        INFO_SCENE_4_2,//新增
        INFO_SCENE_4_3,//新增
        INFO_SCENE_4_4,//新增
        INFO_SCENE_4_5,//新增
        INFO_SCENE_4_6,//新增
        INFO_SCENE_4_7,//新增
        INFO_SCENE_4_8,//新增
        INFO_SCENE_5,//信息下一页界面
        INFO_SCENE_5_1,//新增
        INFO_SCENE_5_2,//新增
        INFO_SCENE_5_3,//新增
        INFO_SCENE_5_4,//新增
        INFO_SCENE_5_5,//新增
        INFO_SCENE_5_6,//新增
        INFO_SCENE_5_7,//新增
        INFO_SCENE_5_8,//新增
        INFO_SCENE_5_9,//新增
        GAME_SCENE_VOTE,//投票主界面
        GAME_SCENE_SELECT,//选择关卡界面
        GAME_SCENE_DAY,//进入白天
        GAME_SCENE_NIGHT,//入夜
        END_VILLAGE,//村人获胜
        END_WOLF,//狼人获胜
        END_FOX,//妖狐获胜
        END_ANIME,//结束动画场景
        ;
        public Scene FatherScene(Scene scene)   //得到当前场景的父亲场景
        {
            //目前仅限于帮助文本的5个父亲场景
            if(scene.ordinal() < 14) return INFO_SCENE_1;
            if(scene.ordinal() < 21) return INFO_SCENE_2;
            if(scene.ordinal() < 28) return INFO_SCENE_3;
            if(scene.ordinal() < 37) return INFO_SCENE_4;
            if(scene.ordinal() < 47) return INFO_SCENE_5;
            return INFO_SCENE_1;//非法情况
        }
        @Override
        public String toString()    //toString方法，获取二级帮助目录的对应的文本文件名字符串
        {
            // 获取枚举常量的原始名称（如 INFO_SCENE_4_3）
            String originalName = super.toString();

            // 只处理 INFO_SCENE_* 开头的枚举，其他保持原名称
            if (originalName.startsWith("INFO_SCENE"))
            {
                // 替换前缀 + 处理下划线，生成 InfoX-X 格式
                String infoPart = originalName.replace("INFO_SCENE", "Info")
                        .replaceFirst("_", "")  // 第一个下划线替换为 -
                        .replace("_", "-");      // 剩余下划线也替换为 -
                // 拼接固定后缀 -1.txt
                return infoPart + "-1.txt";
            }

            // 非信息类场景，返回原始名称（可根据需求修改）
            return originalName;
        }
        public int SubInfoSum(Scene scene) //帮助界面一级目录下的子目录数量
        {
            if(scene == INFO_SCENE_1) return 7;
            if(scene == INFO_SCENE_2) return 6;
            if(scene == INFO_SCENE_3) return 6;
            if(scene == INFO_SCENE_4) return 8;
            if(scene == INFO_SCENE_5) return 9;
            return -1;//未知情况
        }
        public int FirstInfoNum(Scene scene)    //帮助界面一级目录的编号
        {
            if(scene == INFO_SCENE_1) return 1;
            if(scene == INFO_SCENE_2) return 2;
            if(scene == INFO_SCENE_3) return 3;
            if(scene == INFO_SCENE_4) return 4;
            if(scene == INFO_SCENE_5) return 5;
            return -1;//未知情况
        }
    }
    Scene currentScene;//当前是什么场景

    JFrame jFrame;//窗口
    JPanel jPanel;//容器
    JPanel diaPanel;//对话容器

    //定义当前待处理的事件
    UI() {}
    GameStatus gs;//从主逻辑类拿到的游戏状态
    ResourcesInterface resources;//资源接口
    MainLogicInterface mainLogic;//主逻辑接口
    UIComponentFactory uiComponentFactory;
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
        currentScene = Scene.START_SCENE;//初始为开始界面
        jFrame = new JFrame("Village of Cyber:Remake v1.0.3.1");
        jFrame.setResizable(false);
        jFrame.setSize(1280,720);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLocationRelativeTo(null);
        ImageIcon frameIcon = new ImageIcon("vocr/resources/images/Icon2.png");
        jFrame.setIconImage(frameIcon.getImage());
        jPanel = PanelSimpleFactory.createSimplePanel(1280,720,true,false);
        diaPanel = PanelSimpleFactory.createSimplePanel(1280,720,false,false);
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

        // 优先走 Handler 分发
        SceneHandler handler = sceneHandlers.get(currentScene);
        if (handler != null) {
            handler.render(this);
            return;
        }

    }//运行，每次run都会到一个场景

    public LinkedList<Event> events = new LinkedList<>();//作为事件队列

    public void addEvent(Event event)
    {
        events.add(event);
        if(DebugLogger.getInstance().isEnabled() && event != null && !events.isEmpty())DebugLogger.log("事件添加成功且不为空");
    }//添加event
    public void testBtn()
    {
        //测试用按钮，显示信息
        JButton test = new JButton("点我进入");
        test.setBounds(0,0,60,30);
        test.addActionListener(e -> {
            for(int i = 1;i < gs.gc.length;++i)
            {
                DebugLogger.log("编号"+i+" "+uiComponentFactory.getJobText(gs.gc[i].number) + " 真实职业："+uiComponentFactory.getZY(gs.gc[i].actualRole)+" 声称职业："+uiComponentFactory.getZY(gs.gc[i].claimedRole)
                +" 死亡日期"+gs.gc[i].dieDay + " 死亡原因" + uiComponentFactory.getwhyDie(gs.gc[i].whyDie) + " 怀疑度：");
                for(int j = 1;j < gs.gc.length;++j)
                {
                    DebugLogger.print(uiComponentFactory.getJobText(gs.gc[j].number)+" 为"+gs.gc[i].suspicionValue[j]+" ");
                }
                DebugLogger.log("");
                DebugLogger.print(uiComponentFactory.getJobText(gs.gc[i].number) + " 怀疑前三为" + gs.gc[i].top3SuspectedPlayers[1][gs.gameDay] + " "+ gs.gc[i].top3SuspectedPlayers[2][gs.gameDay] + " "+ gs.gc[i].top3SuspectedPlayers[3][gs.gameDay] + " ");
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

    JTextArea piaoText = new JTextArea();//票型/怀疑文本
    JTextArea piaoText1 = new JTextArea();//票型/怀疑文本

    int chuxingWho = 0;//记录到底处刑的是谁，用于处刑人名显示
    List<Integer> voteRounds = new ArrayList<>();//记录每天投票是第几轮的票
    List<Integer> voteMethods = new ArrayList<>();//记录每天投票是什么方式
    int[][] greyCharas;//存储每天的灰角色[角色+1][天数]
    int[][] isSelectedVoteTargetCharas;//存储每天的指定角色[角色+1][天数]

    //信息场景
    //InfoScene_A_B 其实就是指从最初的info主界面开始 点击第A个按钮进入的场景中，点击第B个按钮后进入的场景。
}