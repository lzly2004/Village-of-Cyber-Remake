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
    private ImageIcon scaleIcon(ImageIcon originalIcon, int width, int height)
    {
        if (originalIcon == null) return null;
        Image scaledImage = originalIcon.getImage().getScaledInstance(
                width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    // ==================== 第2步: 公共辅助方法 ====================

    /** 获取当前游戏玩家总数 */
    private int playerCount() { return gs.gc.length - 1; }

    /** 角色头像网格: 计算X坐标 */
    private int charGridX(int i, int baseX, int spacing) {
        int half = (playerCount() + 1) / 2;
        return baseX + spacing * (i <= half ? i : i - playerCount() / 2);
    }

    /** 角色头像网格: 计算Y坐标(上行/下行) */
    private int charGridY(int i, int yTop, int yBottom) {
        return i <= (playerCount() + 1) / 2 ? yTop : yBottom;
    }

    /** 角色头像网格: 是否为上行 */
    private boolean isUpperRow(int i) {
        return i <= (playerCount() + 1) / 2;
    }

    /**
     * 绑定打字效果到文本框和Next按钮 — 消除7处重复代码。
     * @param target 显示文本的JTextArea
     * @param fullText 完整文本
     * @param nextBtn "下一步"按钮
     * @param onComplete 文本播放完毕后点击的回调
     * @return 已创建的打字Timer(已start)
     */
    public Timer bindTypewriter(JTextArea target, String fullText,
                                  JButton nextBtn, Runnable onComplete) {
        final int[] index = {0};
        Timer timer = new Timer(GameConstants.TYPEWRITER_DELAY_MS, e -> {
            if (index[0] < fullText.length()) {
                target.append(String.valueOf(fullText.charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText.length()) {
                target.setText(fullText);
                index[0] = fullText.length();
                timer.stop();
            } else {
                onComplete.run();
            }
        });
        return timer;
    }

    /**
     * 按钮批量显示/隐藏: 只显示visibleButtons中的按钮, 隐藏hideButtons中的按钮。
     * 用于简化投票界面中大量的btn.setVisible()调用。
     */
    private void switchButtons(JButton[] hide, JButton[] show) {
        for (JButton b : hide) b.setVisible(false);
        for (JButton b : show) b.setVisible(true);
    }

    /** 隐藏一组按钮 */
    public void hideButtons(JButton... buttons) {
        for (JButton b : buttons) b.setVisible(false);
    }
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

    public void StartScene()
    {
        count = 0;//切歌计数
        name = "";//歌名
        text = "";//文本
        isAvoid = true;//是否回避，默认开启
        isCo = false;//是否询问co
        isVote[0] = false;
        isZhan[0] = false;
        isHu[0] = false;
        voteRounds.clear();
        voteMethods.clear();
        greyCharas = new int[21][50];
        isSelectedVoteTargetCharas = new int[21][50];
        voteChosen.clear();
        zhanChosen.clear();
        huChosen.clear();

        events.clear();
        if(DebugLogger.getInstance().isEnabled())
        {
            if(events.isEmpty())
            {
                DebugLogger.log("事件成功清空");
            }
        }
        jPanel.removeAll();
        resources.playBgm("start_menu.wav");

        JLabel titleLabel = LabelSimpleFactory.makeLabel(LabelConst.Black_Label,600,200,554,138,resources.getImage("titleLogo.png"));
        // 设置绝对位置和大小
        jPanel.add(titleLabel);

        // 按钮属性
        int x = GameConstants.START_BTN_X;
        int y = GameConstants.START_BTN_Y;
        int width = GameConstants.START_BTN_W;
        int height = GameConstants.START_BTN_H;
        int x_div = GameConstants.START_BTN_X_GAP;
        int y_div = GameConstants.START_BTN_Y_GAP;

        // 新游戏按钮
        JButton btnStart = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x,y,width,height,resources.getImage("startButton.png"));
        btnStart.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.GAME_SCENE_SELECT;
            run();
        });
        jPanel.add(btnStart);

        // 继续游戏按钮
        JButton btnContinue = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x+x_div,y,width,height,resources.getImage("continueButton.png"));
        btnContinue.addActionListener(e -> resources.playSound("click.wav"));
        jPanel.add(btnContinue);

        // 选择存档按钮
        JButton btnSave = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x,y+y_div,width,height,resources.getImage("replayButton.png"));
        btnSave.addActionListener(e -> resources.playSound("click.wav"));
        jPanel.add(btnSave);

        // 数据统计按钮
        JButton btnRecord = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x+x_div,y+y_div,width,height,resources.getImage("recordButton.png"));
        btnRecord.addActionListener(e -> resources.playSound("click.wav"));
        jPanel.add(btnRecord);

        // 信息查看按钮
        JButton btnInfo = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x,y+y_div*2,width,height,resources.getImage("infoButton.png"));
        btnInfo.addActionListener(e ->{
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE;
            resources.playBgm("Info.wav");
            run();
        });
        jPanel.add(btnInfo);

        // 角色收集按钮
        JButton btnCollections = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x+x_div,y+y_div*2,width,height,resources.getImage("collectionsButton.png"));
        btnCollections.addActionListener(e -> resources.playSound("click.wav"));
        jPanel.add(btnCollections);

        // 背景图片（放在最后添加，确保在最底层）
        //JLabel background = new JLabel(resources.getImage("title_base_resized.png"));
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,GameConstants.WINDOW_WIDTH,GameConstants.WINDOW_HEIGHT,
                resources.getImage("title_base_resized.png"));
        jPanel.add(background);
        resizeComponents();
        jFrame.setVisible(true);
    }//开始界面

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
    public enum SelectionType { VOTE, DIVINATION, GUARD }

    /**
     * 统一创建角色选择面板 — 消除指定投票/指定占卜/指定护卫三处~1170行重复代码。
     * @param type 面板类型
     * @param panel 目标面板(infoPanel/infoZhanPanel/infoHuPanel)
     * @param chosenList 已选列表(voteChosen/zhanChosen/huChosen)
     * @param flag 对应的标记数组(isVote/isZhan/isHu)
     */
    public void createCharacterSelectionPanel(SelectionType type, JPanel panel,
                                                java.util.List<Integer> chosenList, boolean[] flag,
                                                ImageIcon boardIcon) {
        panel.setVisible(false);
        panel.removeAll();
        jPanel.add(panel);
        jPanel.setComponentZOrder(panel, 0);

        // 根据类型确定图标和角色过滤
        String frameIconName = switch (type) {
            case VOTE        -> "frameSBlue.png";
            case DIVINATION  -> "frameSRed.png";
            case GUARD       -> "frameOrange.png";
        };
        String resultAllIconName = switch (type) {
            case VOTE        -> "result2_all.png";
            case DIVINATION  -> "result1_all.png";
            case GUARD       -> "result3_all.png";
        };
        String dragAllIconName = switch (type) {
            case VOTE        -> "touhyou.png";
            case DIVINATION  -> "uranaiAll.png";
            case GUARD       -> "goeiAll.png";
        };
        int claimedRoleFilter = switch (type) {
            case VOTE        -> 0;
            case DIVINATION  -> 1;
            case GUARD       -> 3;
        };

        // 获取对应职业的候补列表
        java.util.List<Integer> roleNums = new java.util.ArrayList<>();  // 已co的编号
        java.util.List<Integer> roleOrders = new java.util.ArrayList<>(); // 职业顺序
        java.util.List<Integer> trueNums = new java.util.ArrayList<>();   // 包含潜伏
        for (int i = 1; i <= gs.gc.length - 1; i++) {
            if (gs.gc[i].claimedRole == claimedRoleFilter && gs.gc[i].whyDie == whyDie.NONE) {
                roleNums.add(i);
                roleOrders.add(gs.gc[i].claimedRoleorder);
            }
            if ((gs.gc[i].actualRole == claimedRoleFilter || gs.gc[i].claimedRole == claimedRoleFilter)
                    && gs.gc[i].whyDie == whyDie.NONE) {
                trueNums.add(i);
            }
        }

        java.util.List<JLabel> targetLabels = new java.util.ArrayList<>();
        java.util.List<JLabel> frameLabels = new java.util.ArrayList<>();
        java.util.List<JLabel> resultLabels = new java.util.ArrayList<>();
        java.util.List<JLabel> zbLabels = new java.util.ArrayList<>();

        // 遍历所有角色创建头像网格
        for (int i = 1; i <= gs.gc.length - 1; i++) {
            // 头像命名
            StringBuilder imageName = new StringBuilder();
            if (gs.gc[i].number <= 9) imageName.append("0");
            imageName.append(gs.gc[i].number);
            if (gs.gc[i].whyDie != whyDie.NONE) imageName.append("g");
            imageName.append("s.png");

            // 职业图标
            if (gs.gc[i].claimedRole > 0 && gs.gc[i].claimedRole < 6) {
                StringBuilder crName = new StringBuilder("yaku");
                if (gs.gc[i].claimedRole <= 3)
                    crName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder).append(".png");
                else
                    crName.append(gs.gc[i].claimedRole).append(".png");
                JLabel crLabel = new JLabel(resources.getImage(crName.toString()));
                crLabel.setBounds(charGridX(i, 60, 74), charGridY(i, 20, 128),
                        crLabel.getIcon().getIconWidth(), crLabel.getIcon().getIconHeight());
                panel.add(crLabel);
            }

            // 选择框
            JLabel chooseLabel = new JLabel(resources.getImage(frameIconName));
            frameLabels.add(chooseLabel);
            chooseLabel.setBounds(charGridX(i, 60, 74), charGridY(i, 20, 128), 64, 98);
            panel.add(chooseLabel);
            chooseLabel.setVisible(chosenList.contains(i));

            // 投票标记 (仅投票类型)
            if (type == SelectionType.VOTE) {
                JLabel voteLabel = new JLabel(resources.getImage("result2_all.png"));
                voteLabel.setBounds(charGridX(i, 65, 74), charGridY(i, 20, 128),
                        voteLabel.getIcon().getIconWidth(), voteLabel.getIcon().getIconHeight());
                panel.add(voteLabel);
                panel.setComponentZOrder(voteLabel, 0);
                voteLabel.setVisible(gs.gc[i].isSelectedVoteTarget[gs.gameDay]);
            }

            // 结果标记
            JLabel resultLabel = new JLabel(resources.getImage(resultAllIconName));
            resultLabels.add(resultLabel);
            resultLabel.setBounds(charGridX(i, 65, 74), charGridY(i, 40, 148),
                    resultLabel.getIcon().getIconWidth(), resultLabel.getIcon().getIconHeight());
            panel.add(resultLabel);
            resultLabel.setVisible(chosenList.contains(i));

            // 逐占卜/猎人顺序标记 (仅占卜和护卫)
            if (type != SelectionType.VOTE) {
                String zbIconPrefix = (type == SelectionType.DIVINATION) ? "result1_" : "result3_";
                String zbSuffix = (type == SelectionType.DIVINATION) ? "white.png" : ".png";
                for (int r = 0; r < roleNums.size(); r++) {
                    JLabel zbLabel = new JLabel(resources.getImage(zbIconPrefix + roleOrders.get(r) + zbSuffix));
                    zbLabels.add(zbLabel);
                    zbLabel.setBounds(charGridX(i, 65 + zbLabel.getIcon().getIconWidth(), 74),
                            charGridY(i, 20 + zbLabel.getIcon().getIconHeight() * roleOrders.get(r),
                                    128 + zbLabel.getIcon().getIconHeight() * roleOrders.get(r)),
                            zbLabel.getIcon().getIconWidth(), zbLabel.getIcon().getIconHeight());
                    panel.add(zbLabel);
                    zbLabel.setVisible(chosenList.contains(i));
                }
            }

            // 头像
            ImageIcon charImg = resources.getImage(imageName.toString());
            JLabel label = new JLabel(charImg);
            targetLabels.add(label);
            label.setBounds(charGridX(i, 60, charImg.getIconWidth() + 10),
                    charGridY(i, 20, 30 + charImg.getIconHeight()),
                    charImg.getIconWidth(), charImg.getIconHeight());
            panel.add(label);
        }

        // 技能目标图标 (共用逻辑)
        for (int k = 2; k <= gs.gameDay; ++k) {
            for (int j = 1; j < gs.gc.length; ++j) {
                if (skillTargetPeople[j][k] == 0) continue;
                int i1 = skillTargetPeople[j][k];
                int zynum = claimedRolenum[j][k];
                if (zynum == 3) continue;
                if (zynum == 1 && gs.gc[j].dieDay != 0 && gs.gc[j].dieDay < k) continue;
                if (zynum == 2 && gs.gc[j].dieDay != 0 && gs.gc[j].dieDay < k) continue;
                JLabel stLabel = new JLabel(resources.getImage(skillTargetNames[j][k]));
                stLabel.setBounds(charGridX(i1, 50 + 74, 74) - stLabel.getIcon().getIconWidth() * zynum,
                        charGridY(i1, 20 + (skillTargetOrder[j][k] - 1) * stLabel.getIcon().getIconHeight(),
                                128 + (skillTargetOrder[j][k] - 1) * stLabel.getIcon().getIconHeight()),
                        stLabel.getIcon().getIconWidth(), stLabel.getIcon().getIconHeight());
                panel.add(stLabel);
                panel.setComponentZOrder(stLabel, 0);
            }
        }

        panel.setBounds(GameConstants.INFO_PANEL_X, GameConstants.INFO_PANEL_Y,
                200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight());
        panel.add(LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0,
                200 + boardIcon.getIconWidth(), 50 + boardIcon.getIconHeight(), boardIcon));

        // === "全部"拖拽按钮 ===
        JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button, 250, 350,
                resources.getImage(dragAllIconName).getIconWidth() / 2,
                resources.getImage(dragAllIconName).getIconHeight() / 2,
                resources.getImage(dragAllIconName));
        dragBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                java.awt.Container parent = dragBtn.getParent();
                if (parent == null || targetLabels.isEmpty()) return;
                java.awt.Rectangle btnRect = dragBtn.getBounds();
                int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                for (JLabel label : targetLabels) {
                    if (label.getParent() != parent || !label.isVisible()) continue;
                    if (!label.getBounds().contains(cx, cy)) continue;
                    int idx = targetLabels.indexOf(label);
                    if (gs.gc[idx + 1].whyDie != whyDie.NONE) break;
                    resources.playSound("click.wav");

                    switch (type) {
                        case VOTE -> {
                            gs.gc[idx + 1].isSelectedVoteTarget[gs.gameDay] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                            updateFlagFromVoteTargets(flag);
                        }
                        case DIVINATION -> {
                            gs.hiddenSeerScheduledSkillTargets[idx + 1][gs.gameDay] = true;
                            for (int a = 0; a < trueNums.size(); a++)
                                gs.gc[trueNums.get(a)].claimedRoleScheduledSkillTargets[idx + 1][gs.gameDay] = true;
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                        }
                        case GUARD -> {
                            gs.hiddenHunterScheduledSkillTargets[idx + 1][gs.gameDay] = true;
                            for (int a = 0; a < trueNums.size(); a++)
                                gs.gc[trueNums.get(a)].claimedRoleScheduledSkillTargets[idx + 1][gs.gameDay] = true;
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                        }
                    }
                    resultLabels.get(idx).setVisible(true);
                    frameLabels.get(idx).setVisible(true);
                    frameLabels.get(idx).repaint();
                    jPanel.repaint(label.getBounds());
                    break;
                }
                dragBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            }
        });
        panel.add(dragBtn);
        panel.setComponentZOrder(dragBtn, 0);

        // === 逐职业拖拽按钮 (仅占卜和护卫) ===
        if (type != SelectionType.VOTE) {
            String perIconPrefix = (type == SelectionType.DIVINATION) ? "uranai" : "goei";
            for (int r = 0; r < roleNums.size(); r++) {
                final int cur = r;
                JButton perBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,
                        roleOrders.get(r) * (type == SelectionType.DIVINATION ? 100 : 150)
                                + (type == SelectionType.DIVINATION ? 150 : 250),
                        350,
                        resources.getImage(perIconPrefix + roleOrders.get(r) + ".png").getIconWidth() / 2,
                        resources.getImage(perIconPrefix + roleOrders.get(r) + ".png").getIconHeight() / 2,
                        resources.getImage(perIconPrefix + roleOrders.get(r) + ".png"));
                perBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseReleased(java.awt.event.MouseEvent e) {
                        java.awt.Container parent = perBtn.getParent();
                        if (parent == null || targetLabels.isEmpty()) return;
                        java.awt.Rectangle btnRect = perBtn.getBounds();
                        int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                        for (JLabel label : targetLabels) {
                            if (label.getParent() != parent || !label.isVisible()) continue;
                            if (!label.getBounds().contains(cx, cy)) continue;
                            int idx = targetLabels.indexOf(label);
                            if (gs.gc[idx + 1].whyDie != whyDie.NONE) break;
                            resources.playSound("click.wav");
                            gs.gc[roleNums.get(cur)].claimedRoleScheduledSkillTargets[idx + 1][gs.gameDay] = true;
                            flag[0] = true;
                            if (!chosenList.contains(idx + 1)) chosenList.add(idx + 1);
                            zbLabels.get(idx + cur * (gs.gc.length - 1)).setVisible(true);
                            frameLabels.get(idx).setVisible(true);
                            frameLabels.get(idx).repaint();
                            jPanel.repaint(label.getBounds());
                            break;
                        }
                        perBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
                    }
                });
                panel.add(perBtn);
                panel.setComponentZOrder(perBtn, 0);
            }
        }

        // === 删除按钮 ===
        JButton delBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,
                (type == SelectionType.VOTE) ? 500 : 800, 350,
                resources.getImage("delete.png").getIconWidth() / 2,
                resources.getImage("delete.png").getIconHeight() / 2,
                resources.getImage("delete.png"));
        delBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                java.awt.Container parent = delBtn.getParent();
                if (parent == null || targetLabels.isEmpty()) return;
                java.awt.Rectangle btnRect = delBtn.getBounds();
                int cx = btnRect.x + btnRect.width / 2, cy = btnRect.y + btnRect.height / 2;
                for (JLabel label : targetLabels) {
                    if (label.getParent() != parent || !label.isVisible()) continue;
                    if (!label.getBounds().contains(cx, cy)) continue;
                    int idx = targetLabels.indexOf(label);
                    if (gs.gc[idx + 1].whyDie != whyDie.NONE) break;
                    resources.playSound("click.wav");
                    chosenList.remove(Integer.valueOf(idx + 1));

                    switch (type) {
                        case VOTE -> {
                            gs.gc[idx + 1].isSelectedVoteTarget[gs.gameDay] = false;
                            updateFlagFromVoteTargets(flag);
                        }
                        case DIVINATION -> {
                            gs.hiddenSeerScheduledSkillTargets[idx + 1][gs.gameDay] = false;
                            for (int a = 0; a < trueNums.size(); a++)
                                gs.gc[trueNums.get(a)].claimedRoleScheduledSkillTargets[idx + 1][gs.gameDay] = false;
                            updateFlagFromScheduledTargets(trueNums, flag, true);
                        }
                        case GUARD -> {
                            gs.hiddenHunterScheduledSkillTargets[idx + 1][gs.gameDay] = false;
                            for (int a = 0; a < trueNums.size(); a++)
                                gs.gc[trueNums.get(a)].claimedRoleScheduledSkillTargets[idx + 1][gs.gameDay] = false;
                            updateFlagFromScheduledTargets(trueNums, flag, false);
                        }
                    }
                    // 隐藏逐职业标记
                    if (type != SelectionType.VOTE) {
                        for (int u = 0; u < roleNums.size(); u++)
                            zbLabels.get(idx + u * (gs.gc.length - 1)).setVisible(false);
                    }
                    resultLabels.get(idx).setVisible(false);
                    frameLabels.get(idx).setVisible(false);
                    frameLabels.get(idx).repaint();
                    jPanel.repaint(label.getBounds());
                    break;
                }
                delBtn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
            }
        });
        panel.add(delBtn);
        panel.setComponentZOrder(delBtn, 0);

        panel.setVisible(true);
        panel.revalidate();
        panel.repaint();
        resizeComponents();
    }

    /** 辅助: 从 isSelectedVoteTarget 更新 isVote 标记 */
    private void updateFlagFromVoteTargets(boolean[] flag) {
        for (int y = 1; y < gs.gc.length; ++y) {
            if (gs.gc[y].isSelectedVoteTarget[gs.gameDay]) { flag[0] = true; return; }
        }
        flag[0] = false;
    }

    /** 辅助: 从 scheduledSkillTargets 更新 isZhan/isHu 标记 */
    private void updateFlagFromScheduledTargets(java.util.List<Integer> trueNums, boolean[] flag, boolean isSeer) {
        for (int b = 0; b < trueNums.size(); ++b) {
            for (int j = 1; j < gs.gc.length; ++j) {
                if (gs.gc[trueNums.get(b)].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                    flag[0] = true; return;
                }
            }
        }
        for (int y = 1; y < gs.gc.length; ++y) {
            if (isSeer && gs.hiddenSeerScheduledSkillTargets[y][gs.gameDay]) { flag[0] = true; return; }
            if (!isSeer && gs.hiddenHunterScheduledSkillTargets[y][gs.gameDay]) { flag[0] = true; return; }
        }
        flag[0] = false;
    }


    JTextArea piaoText = new JTextArea();//票型/怀疑文本
    JTextArea piaoText1 = new JTextArea();//票型/怀疑文本
    int chuxingWho = 0;//记录到底处刑的是谁，用于处刑人名显示
    List<Integer> voteRounds = new ArrayList<>();//记录每天投票是第几轮的票
    List<Integer> voteMethods = new ArrayList<>();//记录每天投票是什么方式
    int[][] greyCharas;//存储每天的灰角色[角色+1][天数]
    int[][] isSelectedVoteTargetCharas;//存储每天的指定角色[角色+1][天数]

    public void createTishi(String str)
    {
        JTextArea tishiText = TextareaSimpleFactory.createTranslucentTipTextArea(str);
        tishiText.setBounds(300,300,500,200);
        jPanel.add(tishiText);
        jPanel.setComponentZOrder(tishiText,0);
        Timer timer = new Timer(GameConstants.TRANSITION_SHORT_MS, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tishiText.setVisible(false); // 隐藏组件
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start(); // 启动定时器
        resizeComponents();
    }//创建提示窗口，用于co指定
    public void createDayPiao(int round,int gameDay,int dailyVotingRule){
        //str是最上面的字符串
        piaoText.setVisible(true);
        piaoText1.setVisible(true);
        int[][] voteTotal = new int[gs.gc.length][4];//获取每轮每个角色有几票
        for (int i = 1; i < gs.gc.length; ++i)
        {
            voteTotal[gs.gc[i].voteTarget[gameDay][round]][round]++;//对应人票数++
        }
        StringBuilder extraText = new StringBuilder();
        switch(dailyVotingRule){
            case 0:
                extraText.append(GameStrings.VOTE_FREE);//自由投票
                break;
            case 1://灰随机
                extraText.append(GameStrings.VOTE_GREY);
                for(int i = 0;i < gs.gc.length;++i){
                    if(greyCharas[i][gameDay] != 0){
                        extraText.append(uiComponentFactory.getJobText(gs.gc[greyCharas[i][gameDay]].number));
                    }
                }
                break;
            case 2://指定
                extraText.append(GameStrings.VOTE_DESIGNATED);
                for(int i = 0;i < gs.gc.length;++i){
                    if(isSelectedVoteTargetCharas[i][gameDay] != 0){
                        extraText.append(uiComponentFactory.getJobText(gs.gc[isSelectedVoteTargetCharas[i][gameDay]].number)).append(",");
                    }
                }
                break;
        }
        StringBuilder leftPiao = new StringBuilder("-投票結果/"+gameDay+"日目-第"+round+"轮"+extraText+"\n");
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        for(int i = 1;i < gs.gc.length;++i){
            if(gs.gc[i].whyDie == whyDie.NONE || gs.gc[i].dieDay >= gameDay){
                //没死 或者第gameDay还没死
                if(leftCnt >= 10){
                    rightPiao.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("：").append( voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    //有10个了就去右边
                }
                else {
                    leftPiao.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    leftCnt++;
                }
            }

        }
        if(DebugLogger.getInstance().isEnabled()) {
            DebugLogger.log(leftPiao);
            DebugLogger.log(rightPiao);
        }
        piaoText.setText(leftPiao.toString());
        piaoText.setForeground(Color.black);
        piaoText.setFont(new Font(GameConstants.FONT_FAMILY,Font.BOLD, GameConstants.FONT_SIZE_VOTE));
        piaoText.setLineWrap(true);       // 自动换行
        piaoText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText.setEditable(false);
        piaoText.setOpaque(false);
        piaoText.setBackground(GameConstants.COLOR_TRANSPARENT);
        piaoText.setBorder(BorderFactory.createEmptyBorder());
        piaoText.setBounds(40,228,1000,430);
        jPanel.add(piaoText);
        jPanel.setComponentZOrder(piaoText,0);

        piaoText1.setText(rightPiao.toString());
        piaoText1.setForeground(Color.black);
        piaoText1.setFont(new Font(GameConstants.FONT_FAMILY,Font.BOLD, GameConstants.FONT_SIZE_VOTE));
        piaoText1.setLineWrap(true);       // 自动换行
        piaoText1.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText1.setEditable(false);
        piaoText1.setOpaque(false);
        piaoText1.setBackground(GameConstants.COLOR_TRANSPARENT);
        piaoText1.setBorder(BorderFactory.createEmptyBorder());
        piaoText1.setBounds(400,228,450,430);
        jPanel.add(piaoText1);
        jPanel.setComponentZOrder(piaoText1,0);
        resizeComponents();
    }//显示历史票型，需要传入第几轮投票，第几天的，以及处刑方式
    public void createPiao(String str,int round,boolean[] isReVote) {
        //str是最上面的字符串
        if(DebugLogger.getInstance().isEnabled()) {
            DebugLogger.log("******************************当前gameDay等于" + gs.gameDay + "******************************************");
        }
        piaoText.setVisible(true);
        piaoText1.setVisible(true);
        int gameDay = 0;//到底是哪一天 因为结束不会++
        if (gs.end == 0) {
            gameDay = gs.gameDay - 1;

        } else {
            gameDay = gs.gameDay;
        }
        int[][] voteTotal = new int[gs.gc.length][4];//获取每轮每个角色有几票
        for (int i = 1; i < gs.gc.length; ++i) {
            if (gs.gc[i].whyDie == whyDie.NONE || gs.gc[i].dieDay == gameDay) {
                //不死或者今天才死但还没死
                voteTotal[gs.gc[i].voteTarget[gameDay][round]][round]++;//对应人票数++
            }
        }
        StringBuilder leftPiao = new StringBuilder(str);
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        int max = voteTotal[1][round];
        for (int i = 2; i < gs.gc.length; ++i) {
            if (voteTotal[i][round] > max) {
                max = voteTotal[i][round];
            }
        }
        int maxCnt = 0;
        List<Integer> maxPos = new ArrayList<>();
        for (int i = 1; i < gs.gc.length; ++i) {
            if (voteTotal[i][round] == max) {
                maxCnt++;
                maxPos.add(i);
            }
        }
        for (int i = 1; i < gs.gc.length; ++i) {
            if (gs.gc[i].whyDie == whyDie.NONE || gs.gc[i].dieDay == gameDay) {
                //没死或者今天才死，但是还是能投票
                if (leftCnt >= 10) {
                    rightPiao.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    //有10个了就去右边
                } else {
                    leftPiao.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    leftCnt++;
                }
            }


            if(i == gs.gc.length - 1) {
                for (int i1 = 0; i1 < 10 - leftCnt; ++i1) {
                    leftPiao.append("\n");
                }
                //4 票で、剣士さんが処刑されました。
                if (maxCnt == 1) {
                    leftPiao.append(max).append("票で").append(uiComponentFactory.getJobText(gs.gc[maxPos.get(0)].number)).append("さんが処刑されました。");
                    isReVote[0] = false;
                    chuxingWho = maxPos.get(0);
                    maxPos.clear();
                }
                else{
                    leftPiao.append("投票が同点となりました。再投票を行います。");
                    isReVote[0] = true;
                    maxPos.clear();
                }
            }

        }
        DebugLogger.log(leftPiao);
        DebugLogger.log(rightPiao);

        piaoText.setText(leftPiao.toString());
        piaoText.setForeground(Color.black);
        piaoText.setFont(new Font(GameConstants.FONT_FAMILY,Font.BOLD, GameConstants.FONT_SIZE_VOTE));
        piaoText.setLineWrap(true);       // 自动换行
        piaoText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText.setEditable(false);
        piaoText.setFocusable(false);
        piaoText.setOpaque(false);
        piaoText.setBackground(GameConstants.COLOR_TRANSPARENT);
        piaoText.setBorder(BorderFactory.createEmptyBorder());
        piaoText.setBounds(40,228,900,430);
        jPanel.add(piaoText);
        jPanel.setComponentZOrder(piaoText,0);

        piaoText1.setText(rightPiao.toString());
        piaoText1.setForeground(Color.black);
        piaoText1.setFont(new Font(GameConstants.FONT_FAMILY,Font.BOLD, GameConstants.FONT_SIZE_VOTE));
        piaoText1.setLineWrap(true);       // 自动换行
        piaoText1.setWrapStyleWord(true);// 按单词拆分换行（避免单词截断）
        piaoText1.setEditable(false);
        piaoText1.setFocusable(false);
        piaoText1.setOpaque(false);
        piaoText1.setBackground(GameConstants.COLOR_TRANSPARENT);
        piaoText1.setBorder(BorderFactory.createEmptyBorder());
        piaoText1.setBounds(530,228,450,430);
        jPanel.add(piaoText1);
        jPanel.setComponentZOrder(piaoText1,0);
        resizeComponents();
    }//显示票型，需要传入首行内容，是第几轮投票，是否重投
    public void createDoubt()
    {
        //str是最上面的字符串
        piaoText.setVisible(true);
        piaoText1.setVisible(true);
        StringBuilder leftPiao = new StringBuilder("- 疑い先 - 全体表示\n\n");
        StringBuilder rightPiao = new StringBuilder("\n\n");
        int leftCnt = 0;
        for(int i = 1;i < gs.gc.length;++i){
            if(gs.gc[i].whyDie == whyDie.NONE){
                //没死
                ArrayList<Integer> charas = new ArrayList<>();
                for(int t = 1;t <= 3;++t){
                    if(t < gs.gc[i].top3SuspectedPlayers.length&& gs.gc[i].top3SuspectedPlayers[t][gs.gameDay]!=0){
                        charas.add(gs.gc[i].top3SuspectedPlayers[t][gs.gameDay]);//记录怀疑前三的是谁
                    }
                }
                ArrayList<String> cmps = new ArrayList<>();
                for(int t = 0;t <= charas.size() - 2;++t){
                    //三人，比较两次
                    //二人，比较一次
                    int temp = gs.gc[i].suspicionValue[charas.get(t)] - gs.gc[i].suspicionValue[charas.get(t+1)];
                    //怀疑度因为123是降序所以不用担心正负
                    if(temp <= 2){
                        cmps.add("=");
                    }
                    else if(temp <=5){
                        cmps.add("≧");
                    }
                    else if(temp <= 10){
                        cmps.add(">");
                    }
                    else{
                        cmps.add("≫");
                    }
                }
                if(leftCnt >= 10){
                    for(int u = 1; u < 4;++u) {
                        if(u < gs.gc[i].top3SuspectedPlayers.length&& gs.gc[i].top3SuspectedPlayers[u][gs.gameDay]!=0) {
                            if (u == 1) rightPiao.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("：");

                            rightPiao.append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].top3SuspectedPlayers[u][gs.gameDay]].number));
                            if(!cmps.isEmpty()){
                                rightPiao.append(cmps.getFirst());
                                cmps.removeFirst();
                            }
                        }
                    }
                    rightPiao.append("\n");

                }
                else {
                    for(int u = 1; u < 4;++u) {
                        if(u < gs.gc[i].top3SuspectedPlayers.length && gs.gc[i].top3SuspectedPlayers[u][gs.gameDay]!=0){
                            //如果存在怀疑对象
                            if(u == 1)leftPiao.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("：");
                            leftPiao.append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].top3SuspectedPlayers[u][gs.gameDay]].number));
                            if(!cmps.isEmpty()){
                                leftPiao.append(cmps.getFirst());
                                cmps.removeFirst();
                            }
                        }
                    }
                    leftPiao.append("\n");
                    leftCnt++;
                    //有10个了就去右边
                }
            }
        }
        DebugLogger.log(leftPiao);
        DebugLogger.log(rightPiao);

        piaoText.setText(leftPiao.toString());
        piaoText.setForeground(Color.black);
        piaoText.setFont(new Font(GameConstants.FONT_FAMILY,Font.BOLD, GameConstants.FONT_SIZE_VOTE));
        piaoText.setLineWrap(true);       // 自动换行
        piaoText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText.setEditable(false);
        piaoText.setOpaque(false);
        piaoText.setBackground(GameConstants.COLOR_TRANSPARENT);
        piaoText.setBorder(BorderFactory.createEmptyBorder());
        piaoText.setBounds(40,228,900,430);
        jPanel.add(piaoText);
        jPanel.setComponentZOrder(piaoText,0);

        piaoText1.setText(rightPiao.toString());
        piaoText1.setForeground(Color.black);
        piaoText1.setFont(new Font(GameConstants.FONT_FAMILY,Font.BOLD, GameConstants.FONT_SIZE_VOTE));
        piaoText1.setLineWrap(true);       // 自动换行
        piaoText1.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText1.setEditable(false);
        piaoText1.setOpaque(false);
        piaoText1.setBackground(GameConstants.COLOR_TRANSPARENT);
        piaoText1.setBorder(BorderFactory.createEmptyBorder());
        piaoText1.setBounds(530,228,450,430);
        jPanel.add(piaoText1);
        jPanel.setComponentZOrder(piaoText1,0);
        resizeComponents();
    }//显示怀疑度



    //信息场景
    //InfoScene_A_B 其实就是指从最初的info主界面开始 点击第A个按钮进入的场景中，点击第B个按钮后进入的场景。
}