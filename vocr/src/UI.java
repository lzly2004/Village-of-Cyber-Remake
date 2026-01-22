import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;
import javax.sound.sampled.*;
import java.util.List;
import javax.swing.*;
import java.awt.*;
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
//以上
//***************************************
public class UI implements UIInterface
{
    public GameStatus getGameStatus()
    {//提供给MainLogic类，得到当前的游戏状态
        return gs;
    }//返回给mainLogic类，方便其获取gs
    private enum Scene  //定义界面枚举类型,当前处于什么界面
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
    static GameStatus gs;//从主逻辑类拿到的游戏状态
    ResourcesInterface resources;//资源接口
    MainLogicInterface mainLogic;//主逻辑接口
    UIComponentFactory uiComponentFactory;
    boolean isTest = true;//测试，false时不显示测试内容
    // 在UI类中添加这个方法
    private ImageIcon scaleIcon(ImageIcon originalIcon, int width, int height)
    {
        if (originalIcon == null) return null;
        Image scaledImage = originalIcon.getImage().getScaledInstance(
                width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }
    public void init()
    {
        uiComponentFactory = new UIComponentFactory();
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
    private void resizeComponents()
    {
        jPanel.revalidate();
        jPanel.repaint();
    } //调整所有组件大小和位置
    public void run()
    {
        resources = Game.getInstance().getResources();
        mainLogic = Game.getInstance().getMainLogic();
        switch (currentScene)
        {
            case DIALOGUE_AFTERNOON:
                dialogue_afternoon();
                break;
            case DIALOGUE_DEATH:
                dialogue_day_death();
                break;
            case DIALOGUE_CHUXING:
                dialogue_chuxing();
                break;
            case DIALOGUE_DAY:
                dialogue_day();
                break;
            case START_SCENE:
                StartScene();
                break;
            case GAME_SCENE_SELECT:
                GameScene_select();
                break;
            case GAME_SCENE_VOTE:
                GameScene_vote();
                break;
            case GAME_SCENE_NIGHT:
                GameScene_night();
                break;
            case GAME_SCENE_DAY:
                GameScene_day();
                break;
            case END_VILLAGE:
            case END_WOLF:
            case END_FOX:
                end_game(currentScene);
                break;
            case END_ANIME:
                end_anime();
                break;
            case INFO_SCENE:
                InfoScene();
                break;
            case INFO_SCENE_1:
            case INFO_SCENE_2:
            case INFO_SCENE_3:
            case INFO_SCENE_4:
            case INFO_SCENE_5:
              InfoScene_First(currentScene);
                break;
            case INFO_SCENE_1_1:
            case INFO_SCENE_1_2:
            case INFO_SCENE_1_3:
            case INFO_SCENE_1_4:
            case INFO_SCENE_1_5:
            case INFO_SCENE_1_6:
            case INFO_SCENE_1_7:
            case INFO_SCENE_2_1:
            case INFO_SCENE_2_2:
            case INFO_SCENE_2_3:
            case INFO_SCENE_2_4:
            case INFO_SCENE_2_5:
            case INFO_SCENE_2_6:
            case INFO_SCENE_3_1:
            case INFO_SCENE_3_2:
            case INFO_SCENE_3_3:
            case INFO_SCENE_3_4:
            case INFO_SCENE_3_5:
            case INFO_SCENE_3_6:
            case INFO_SCENE_4_1:
            case INFO_SCENE_4_2:
            case INFO_SCENE_4_3:
            case INFO_SCENE_4_4:
            case INFO_SCENE_4_5:
            case INFO_SCENE_4_6:
            case INFO_SCENE_4_7:
            case INFO_SCENE_4_8:
            case INFO_SCENE_5_1:
            case INFO_SCENE_5_2:
            case INFO_SCENE_5_3:
            case INFO_SCENE_5_4:
            case INFO_SCENE_5_5:
            case INFO_SCENE_5_6:
            case INFO_SCENE_5_7:
            case INFO_SCENE_5_8:
            case INFO_SCENE_5_9:
              InfoScene_Second(currentScene);
              break;
        }
    }//运行，每次run都会到一个场景

    public static LinkedList<Event> events = new LinkedList<>();//作为事件队列

    public void addEvent(Event event)
    {
        events.add(event);
        if(isTest && event != null && !events.isEmpty())System.out.println("事件添加成功且不为空");
    }//添加event
    public void testBtn()
    {
        //测试用按钮，显示信息
        JButton test = new JButton("点我进入");
        test.setBounds(0,0,60,30);
        test.addActionListener(e -> {
            for(int i = 1;i < gs.gc.length;++i)
            {
                System.out.println("编号"+i+" "+uiComponentFactory.getJobText(gs.gc[i].number) + " 真实职业："+uiComponentFactory.getZY(gs.gc[i].actualRole)+" 声称职业："+uiComponentFactory.getZY(gs.gc[i].claimedRole)
                +" 死亡日期"+gs.gc[i].dieDay + " 死亡原因" + uiComponentFactory.getwhyDie(gs.gc[i].whyDie) + " 怀疑度：");
                for(int j = 1;j < gs.gc.length;++j)
                {
                    System.out.print(uiComponentFactory.getJobText(gs.gc[j].number)+" 为"+gs.gc[i].suspicionValue[j]+" ");
                }
                System.out.println();
                System.out.print(uiComponentFactory.getJobText(gs.gc[i].number) + " 怀疑前三为" + gs.gc[i].top3SuspectedPlayers[1][gs.gameDay] + " "+ gs.gc[i].top3SuspectedPlayers[2][gs.gameDay] + " "+ gs.gc[i].top3SuspectedPlayers[3][gs.gameDay] + " ");
                System.out.println();
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
        if(isTest)
        {
            if(events.isEmpty())
            {
                System.out.println("事件成功清空");
            }
        }
        jPanel.removeAll();
        resources.playBgm("start_menu.wav");

        JLabel titleLabel = LabelSimpleFactory.makeLabel(LabelConst.Black_Label,600,200,554,138,resources.getImage("titleLogo.png"));
        // 设置绝对位置和大小
        jPanel.add(titleLabel);

        // 按钮属性
        int x = 660;
        int y = 400;
        int width = 309;
        int height = 68;
        int x_div = 300;
        int y_div = 80;

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
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("title_base_resized.png"));
        jPanel.add(background);
        resizeComponents();//强制重置一次
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }//开始界面
    public void GameScene_select()
    {
        jPanel.removeAll();
        int x = 30;
        int y = 20;
        int width = 435;//512 162
        int height = 138;
        int x_div = 30 + width;
        int y_div = 30 + height;
        //简易村
        ImageIcon village1 = resources.getImage("game1.png");
        JButton btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x,y,width,height
                ,resources.getImage("game1.png"));
        jPanel.add(btn1);
        btn1.addActionListener(e -> {
            levelName="game1 #17747.png";
            resources.playSound("click.wav");
            gs = mainLogic.start(peiyi.jianyi);//先初始化gs
            skillTargetPeople = new int[gs.gc.length][50];
            skillTargetNames = new String[gs.gc.length][50];
            skillTargetOrder = new int[gs.gc.length][50];
            claimedRolenum = new int[gs.gc.length][50];
            if(isTest) {
                // 检查 mainLogic 是否自动添加了事件
                System.out.println("启动后事件数量: " + events.size());

                // 如果事件为空，手动添加测试事件
                if (events.isEmpty()) {
                    System.out.println("events为空，添加测试事件");
                    Event testEvent = new Event(EventName.yjsw, CharacterEnglishName.Beatrice);
                    addEvent(testEvent);
                }
            }
            currentScene = Scene.GAME_SCENE_NIGHT;
            run();
        });
        //通常村
        btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x,y+y_div,width,height
                ,resources.getImage("game2.png"));
        btn1.addActionListener(e -> {
            levelName="game2 #17977.png";
            resources.playSound("click.wav");
            gs = mainLogic.start(peiyi.tongchang);//先初始化gs
            currentScene = Scene.GAME_SCENE_NIGHT;
            skillTargetPeople = new int[gs.gc.length][50];
            skillTargetNames = new String[gs.gc.length][50];
            skillTargetOrder = new int[gs.gc.length][50];
            claimedRolenum = new int[gs.gc.length][50];
            run();
        });
        jPanel.add(btn1);
        //妖狐村
        btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x,y+2*y_div,width,height
                , resources.getImage("game3.png"));
        btn1.addActionListener(e -> {
            levelName="game3 #18179.png";
            resources.playSound("click.wav");
            gs = mainLogic.start(peiyi.yaoohu);//先初始化gs
            currentScene = Scene.GAME_SCENE_NIGHT;
            skillTargetPeople = new int[gs.gc.length][50];
            skillTargetNames = new String[gs.gc.length][50];
            skillTargetOrder = new int[gs.gc.length][50];
            claimedRolenum = new int[gs.gc.length][50];
            run();
        });
        jPanel.add(btn1);
        //狂信村
        btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x,y+3*y_div,width,height
                ,resources.getImage("game4 #19067.png"));
        btn1.addActionListener(e -> {
            levelName="game4.png";
            resources.playSound("click.wav");
            gs = mainLogic.start(peiyi.kuangxin);//先初始化gs
            skillTargetPeople = new int[gs.gc.length][50];
            skillTargetNames = new String[gs.gc.length][50];
            skillTargetOrder = new int[gs.gc.length][50];
            claimedRolenum = new int[gs.gc.length][50];
            currentScene = Scene.GAME_SCENE_NIGHT;
            run();
        });
        jPanel.add(btn1);
        //背德村
        btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x+x_div,y,width,height,resources.getImage("game5.png"));
        btn1.addActionListener(e -> {
            levelName="game5 #17265.png";
            resources.playSound("click.wav");
            gs = mainLogic.start(peiyi.beide);//先初始化gs
            skillTargetPeople = new int[gs.gc.length][50];
            skillTargetNames = new String[gs.gc.length][50];
            skillTargetOrder = new int[gs.gc.length][50];
            claimedRolenum = new int[gs.gc.length][50];
            currentScene = Scene.GAME_SCENE_NIGHT;
            run();
        });
        jPanel.add(btn1);
        //猫又村
        btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x+x_div,y+y_div,width,height,
                resources.getImage("game6.png"));
        btn1.addActionListener(e -> {
            levelName="game6 #17098.png";
            resources.playSound("click.wav");
            gs = mainLogic.start(peiyi.maoyou);//先初始化gs
            skillTargetPeople = new int[gs.gc.length][50];
            skillTargetNames = new String[gs.gc.length][50];
            skillTargetOrder = new int[gs.gc.length][50];
            claimedRolenum = new int[gs.gc.length][50];
            currentScene = Scene.GAME_SCENE_NIGHT;
            run();
        });
        jPanel.add(btn1);
        //大型村
        btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,x+x_div,y+y_div*2,width,height,
                resources.getImage("game7.png"));
        btn1.addActionListener(e -> {
            levelName="game7 #18147.png";
            resources.playSound("click.wav");
            gs = mainLogic.start(peiyi.daxing);//先初始化gs
            skillTargetPeople = new int[gs.gc.length][50];
            skillTargetNames = new String[gs.gc.length][50];
            skillTargetOrder = new int[gs.gc.length][50];
            claimedRolenum = new int[gs.gc.length][50];
            currentScene = Scene.GAME_SCENE_NIGHT;
            run();
        });
        jPanel.add(btn1);
        //返回按钮
        btn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,ConstNum.WINDOW_WIDTH - ConstNum.RETURN_WIDTH - 60,ConstNum.WINDOW_HEIGHT - 60 - ConstNum.RETURN_HEIGHT,
                ConstNum.RETURN_WIDTH,ConstNum.RETURN_HEIGHT,resources.getImage("return.png"));
        btn1.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        jPanel.add(btn1);

        // 背景图片（放在最后添加，确保在最底层）
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("title_base_resized.png"));
        jPanel.add(background);
        // 强制触发一次大小调整
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }//关卡选择界面
    public void GameScene_night()
    {
        //用于控制台显示角色和对应职业
        specialEvent[0] = false;
        isVote[0] = false;
        isZhan[0] = false;
        isHu[0] = false;
        voteChosen.clear();
        zhanChosen.clear();
        huChosen.clear();
        if(!events.isEmpty()){
            System.out.println("事件不为空");
        }
        jPanel.removeAll();
        //背景
        //JLabel label = new JLabel(resources.getImage("komorebi000night01.png"));
        JLabel label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("komorebi000night01.png"));
        jPanel.add(label);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
        //播放音效，音效结束就进入下一界面
        resources.playBgm("");//空 停止上一首
        resources.playSound("入夜音效.wav");//替换成入夜音效
        Timer timer = new Timer(7000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                switch(gs.end) {
                    case 0://没有结束
                        currentScene = Scene.GAME_SCENE_DAY;
                        break;
                    case 1://村胜
                        currentScene = Scene.END_VILLAGE;
                        break;
                    case 2://狼胜
                        currentScene = Scene.END_WOLF;
                        break;
                    case 3://狐胜
                        currentScene = Scene.END_FOX;
                        break;
                }
                // 停止定时器（可选，避免重复触发）
                ((Timer) e.getSource()).stop();
                run();
            }
        });
        timer.start();
    }//入夜界面
    public void GameScene_day()
    {
        gs = mainLogic.getGameStatus();//新一天获取新gs
        switch(gs.end)
        {
            case 1:
                //村胜
                jPanel.removeAll();
                jPanel.revalidate();
                jPanel.repaint();
                currentScene = Scene.END_VILLAGE;
                run();
                break;
            case 2:
                //狼胜
                jPanel.removeAll();
                jPanel.revalidate();
                jPanel.repaint();
                currentScene = Scene.END_WOLF;
                run();
                break;
            case 3:
                //狐胜
                jPanel.removeAll();
                jPanel.revalidate();
                jPanel.repaint();
                currentScene = Scene.END_FOX;
                run();
                break;
        }
        jPanel.removeAll();

        // 背景图片
        JLabel background =LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("komorebi002.png"));
        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = PanelSimpleFactory.createSimplePanel(260,450,760,230,false,true);
        jPanel.add(dialogPanel);     // 顶层：对话框面板
        //对话框背
        JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,760,230,backIcon);
        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
        dialogText.setBounds(20,50,710,200);
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,0,0,760,230,null,null);
        // 文本逐字打印逻辑

        final String[] fullText = {gs.gameDay+"日目になりました。"};
        final int[] index = {0};
        Timer typeTimer = new Timer(50, e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.append(String.valueOf(fullText[0].charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        dialogPanel.setVisible(false);
        // 按钮点击事件
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.setText(fullText[0]);
                index[0] = fullText[0].length();
                typeTimer.stop();
            } else {
                currentScene = Scene.DIALOGUE_DEATH;
                run();
            }
        });
        dialogPanel.add(nextBtn);  //添加到对话框面板
        dialogPanel.add(dialogText);  //添加到对话框面板
        dialogPanel.add(back);//添加对话框背景
        resources.playSound("狼嚎音效.wav");
        Timer timer = new Timer(2500, e -> {
            dialogPanel.setVisible(true);
            typeTimer.start();
            ((Timer)e.getSource()).stop();
        });
        timer.start();
        jPanel.add(dialogPanel);
        jPanel.add(background); // 最底层：背景
        // 强制触发一次大小调整
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }//白天界面
    public void dialogue_day_death()
    {
        if(events.getFirst().eventname == EventName.wsw)
        {
            //如果不是夜间死亡事件，则和平
            resources.playSound("平和音效.wav");
            //无死亡就播放平和
            diaPanel.removeAll();
            diaPanel.setVisible(true);
            //背景图片
            JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT
                    ,resources.getImage("haikei3.png"));

            // 对话框背景（添加到对话框面板）
            ImageIcon backIcon = resources.getImage("messageframe.png");
            // 对话框面板
            JPanel dialogPanel = PanelSimpleFactory.makePanel(PanelConst.Simple_Panel,260,450,backIcon.getIconWidth(),backIcon.getIconHeight()
            ,false,true);
            diaPanel.add(dialogPanel);     // 顶层：对话框面板

            JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,backIcon.getIconWidth(),backIcon.getIconHeight(),backIcon);
            // 文本显示区域（添加到对话框面板）
            JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
            dialogText.setBounds(20,50,710,200);

            JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,0,0,backIcon.getIconWidth(),backIcon.getIconHeight(),null,null);

            // 文本逐字打印逻辑
            String text = resources.getEventText(events.poll());
            final String[] fullText = {"犠牲者はいませんでした。\n"};
            final int[] index = {0};
            Timer typeTimer = new Timer(50, e -> {
                if (index[0] < fullText[0].length()) {
                    dialogText.append(String.valueOf(fullText[0].charAt(index[0])));
                    index[0]++;
                } else {
                    ((Timer) e.getSource()).stop();
                }
            });
            dialogPanel.setVisible(true);
            typeTimer.start();

            // 按钮点击事件
            nextBtn.addActionListener(e -> {
                if (index[0] < fullText[0].length()) {
                    dialogText.setText(fullText[0]);
                    index[0] = fullText[0].length();
                    typeTimer.stop();
                } else {
                    if((gs.aliveCounter - 1)/2 == 1){
                        resources.playBgm("西江紫堂 - 灯り無き眼光.wav");
                    }
                    else {
                        resources.playBgm("Emotionally Unstable.wav");
                    }
                    currentScene = Scene.DIALOGUE_DAY;
                    run();
                }
            });
            dialogPanel.add(nextBtn);  // 添加到对话框面板
            dialogPanel.add(dialogText);  // 添加到对话框面板
            dialogPanel.add(back);  // 添加到对话框面板
            diaPanel.add(background);          // 最底层：背景
            jPanel.add(diaPanel);

            // 强制触发一次大小调整
            jPanel.setComponentZOrder(diaPanel, 0);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();
            diaPanel.setVisible(true);
            jPanel.revalidate();
            jPanel.repaint();
        }
        else
        {
            Event event = events.poll();
            if (event == null)
            {
                currentScene = Scene.GAME_SCENE_VOTE;
                run();
            }
            jPanel.removeAll();
            diaPanel.removeAll();
            diaPanel.setVisible(true);
            //背景图片
            JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                    resources.getImage("haikei3.png"));

            // 对话框背景（添加到对话框面板）
            ImageIcon backIcon = resources.getImage("messageframe.png");
            // 对话框面板
            JPanel dialogPanel = PanelSimpleFactory.makePanel(PanelConst.Simple_Panel,260,450,backIcon.getIconWidth(),backIcon.getIconHeight(),
                    false,true);
            diaPanel.add(dialogPanel);     // 顶层：对话框面板

            //人物立绘
            ImageIcon[] CharIcon = resources.getEventImage(event);
            JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,backIcon.getIconWidth(),backIcon.getIconHeight(),backIcon);

            // 角色名称标签（添加到对话框面板）
            JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,40,10,1000,30,
                    uiComponentFactory.getCharacterFullName(event.ch1));
            dialogPanel.add(nameLabel);  // 添加到对话框面板

            // 文本显示区域（添加到对话框面板）
            JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
            dialogText.setBounds(20,50,710,200);

            JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,0,0,backIcon.getIconWidth(),backIcon.getIconHeight(),null,null);
            nextBtn.setVisible(false);
            nextBtn.setEnabled(false);

            // 文本逐字打印逻辑
            String text = resources.getEventText(event);
            final String[] fullText = {text};
            final int[] index = {0};
            Timer typeTimer = new Timer(50, e -> {
                if (index[0] < fullText[0].length()) {
                    dialogText.append(String.valueOf(fullText[0].charAt(index[0])));
                    index[0]++;
                } else {
                    ((Timer) e.getSource()).stop();
                }
            });
            dialogPanel.setVisible(true);
            typeTimer.start();
            //不是空直接显示
            if (CharIcon.length != 0) {
                typeTimer.stop();
                dialogPanel.setVisible(false);
                JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,(1280 - CharIcon[0].getIconWidth()) / 2,720 - CharIcon[0].getIconHeight() - 30,
                        CharIcon[0].getIconWidth(),CharIcon[0].getIconHeight(),CharIcon[0]);
                diaPanel.add(Chara);
                Timer t1 = new Timer(1000, e -> {
                    //过一会显示死亡
                    resources.playSound("夜间死亡音效.wav");
                    Chara.setVisible(false);
                    JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,(1280 - CharIcon[1].getIconWidth()) / 2,720 - CharIcon[1].getIconHeight() - 30,
                            CharIcon[1].getIconWidth(),CharIcon[1].getIconHeight(),CharIcon[1]);
                    diaPanel.add(Chara2);
                    diaPanel.setComponentZOrder(Chara2, 1);
                    resizeComponents();
                    diaPanel.revalidate();
                    diaPanel.repaint();
                    ((Timer) e.getSource()).stop();
                });
                t1.start();
                Timer t2 = new Timer(2000, e -> {
                    dialogPanel.setVisible(true);
                    nextBtn.setVisible(true);
                    nextBtn.setEnabled(true);
                    typeTimer.start();
                    ((Timer) e.getSource()).stop();
                });
                t2.start();
            }
            // 按钮点击事件
            nextBtn.addActionListener(e -> {
                if (index[0] < fullText[0].length()) {
                    dialogText.setText(fullText[0]);
                    index[0] = fullText[0].length();
                    typeTimer.stop();
                } else {
                    if (events.isEmpty() || events.getFirst().eventname != EventName.yjsw) {
                        //如果为空，进入下一阶段，如果不是夜间死亡，则进入下一阶段
                        if((gs.aliveCounter - 1)/2 == 1){
                            resources.playBgm("西江紫堂 - 灯り無き眼光.wav");
                        }
                        else {
                            resources.playBgm("Emotionally Unstable.wav");
                        }
                        currentScene = Scene.DIALOGUE_DAY;
                    }
                    run();
                }
            });
            dialogPanel.add(nextBtn);  // 添加到对话框面板
            dialogPanel.add(dialogText);  // 添加到对话框面板
            dialogPanel.add(back);  // 添加到对话框面板
            nextBtn.setVisible(false);

            diaPanel.add(background);          // 最底层：背景
            jPanel.add(diaPanel);

            // 强制触发一次大小调整
            jPanel.setComponentZOrder(diaPanel, 0);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();
            diaPanel.setVisible(true);
            jPanel.revalidate();
            jPanel.repaint();
        }
    }//白天死亡对话
    public void dialogue_chuxing()
    {
        resources.playBgm("");
        Event event = events.poll();
        jPanel.removeAll();
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        // 背景图片
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("haikei.png"));

        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = PanelSimpleFactory.createSimplePanel(260,450,760,230,false,true);
        diaPanel.add(dialogPanel);         // 顶层：对话框面板

        //人物立绘
            ImageIcon[] CharIcon = resources.getEventImage(event);
            JLabel Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,(1280 - CharIcon[0].getIconWidth()) / 2,
                    720 - CharIcon[0].getIconHeight() - 30,CharIcon[0].getIconWidth(),CharIcon[0].getIconHeight(),CharIcon[0]);
            diaPanel.add(Chara);
        //对话框背景
        JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,backIcon.getIconWidth(),backIcon.getIconHeight(),backIcon);

        // 角色名称标签（添加到对话框面板）
        
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,40,10,1000,30,uiComponentFactory.getCharacterFullName(event.ch1));
        dialogPanel.add(nameLabel);  // 添加到对话框面板

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
        dialogText.setBounds(20,50,710,200);
        
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,0,0,760,230,null,null);

        // 文本逐字打印逻辑
        String text = resources.getEventText(event);
        final String[] fullText = {text};
        final int[] index = {0};
        Timer typeTimer = new Timer(50, e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.append(String.valueOf(fullText[0].charAt(index[0])));
                index[0]++;
            } else {
                //显示完了停止
                ((Timer) e.getSource()).stop();
            }
        });

        // 按钮点击事件
        boolean[] isNext = {false};
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.setText(fullText[0]);
                index[0] = fullText[0].length();
                typeTimer.stop();
            } else {
                //按钮和对话框都停用，两秒后才能用
                dialogPanel.setVisible(false);
                nextBtn.setVisible(false);
                Timer timer = new Timer(2000,e1->{
                    dialogText.setText("");
                    nextBtn.setVisible(true);
                    CharacterKanjiName[] values = CharacterKanjiName.values();
                    if(event.eventname == EventName.cxs)
                    {
                        fullText[0] = "投票の結果、" + values[gs.gc[chuxingWho].number].name() + "は処刑されました。";
                    }
                    else if(event.eventname == EventName.hzsw)
                    {
                        int num = 0;
                        for(int r = 1;r < gs.gc.length;++r)
                        {
                            if(gs.gc[r].whyDie == whyDie.dayhouzhui)
                            {
                                num = gs.gc[r].number;
                            }
                        }
                        fullText[0] = "" + values[num].name() + "後追いで死亡した。";
                    }
                    else if(event.eventname == EventName.mzsw)
                    {
                        int num = 0;
                        for(int r = 1;r < gs.gc.length;++r)
                        {
                            if(gs.gc[r].whyDie == whyDie.daymaozhou)
                            {
                                num = gs.gc[r].number;
                            }
                        }
                        fullText[0] = "" + values[num].name() + "猫の呪いによって死亡した。";
                    }
                    index[0] = 0;
                    typeTimer.start();
                    dialogPanel.setVisible(true);
                    isNext[0] = true;
                    ((Timer) e1.getSource()).stop();
                });
                timer.start();

                //后追猫咒没有音效
                if(!isNext[0])resources.playSound("白天处刑音效.wav");
                Chara.setVisible(false);
                JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,(1280 - CharIcon[1].getIconWidth()) / 2,
                        720 - CharIcon[1].getIconHeight() - 30,CharIcon[1].getIconWidth(),CharIcon[1].getIconHeight(),CharIcon[1]);
                if(isNext[0])
                {
                    if(events.isEmpty()||(events.getFirst().eventname != EventName.cxs && events.getFirst().eventname != EventName.hzsw &&events.getFirst().eventname != EventName.mzsw)){
                        //如果事件不是处刑不是后追不是猫咒死亡，就直接到入夜
                        currentScene = Scene.GAME_SCENE_NIGHT;
                    }
                    Chara2.setVisible(false);
                    run();
                }
                diaPanel.add(Chara2);
                resizeComponents();
                diaPanel.revalidate();
                diaPanel.repaint();
                diaPanel.setComponentZOrder(Chara2, 1);
            }
        });
        dialogPanel.add(nextBtn);  // 添加到对话框面板
        dialogPanel.add(dialogText);  // 添加到对话框面板
        dialogPanel.add(back);  // 添加到对话框面板
        diaPanel.add(background);          // 最底层：背景
        jPanel.add(diaPanel);
        typeTimer.start();
        // 强制触发一次大小调整
        jPanel.setComponentZOrder(diaPanel,0);
        resizeComponents();
        diaPanel.revalidate();
        diaPanel.repaint();
        diaPanel.setVisible(true);
        jPanel.revalidate();
        jPanel.repaint();
    }//处刑对话

    List<ImageIcon> linkIcon = new ArrayList<>();//记录接连事件图像
    boolean[] specialEvent ={false};//是不是特殊事件，也就是三个连续的事件组，有特殊显示方式

    public void dialogue_day()
    {
        Event event = events.poll();
        if(event == null)
        {
            //如果显示死亡后没有其他事件直接到vote
            currentScene = Scene.GAME_SCENE_VOTE;
            run();
        }
        boolean isConnect = true;//是否为接连发生事件的下一个
        switch(event.eventname)
        {
            case jhdh8b:
            case gyfo1r:
            case qfjcqr5r:
            case gprz11p:
            case jbdh8r:
            case gprz11r:
            case zcrh12r:
                //不变
                if(isTest){
                    System.out.println("******************不变");
                }
                break;
            default:
                isConnect = false;
                //变为假
                break;
        }
        if(!linkIcon.isEmpty()&&!isConnect)
        {
            //前一个是，而后一个不是，直接移除不管正常中间显示
            if(isTest) {
                System.out.println("进入了isConnect");
            }
            linkIcon.remove(0);
        }
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        //背景图片
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("haikei3.png"));
        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = PanelSimpleFactory.createSimplePanel(260,450,760,230,false,true);
        diaPanel.add(dialogPanel);     // 顶层：对话框面板
        JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,760,230,backIcon);
        // 角色名称标签（添加到对话框面板）
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,40,10,1000,30,uiComponentFactory.getCharacterFullName(event.ch1));
        dialogPanel.add(nameLabel);  // 添加到对话框面板
        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
        dialogText.setBounds(20,50,710,200);
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,0,0,760,230,null,null);
        // 文本逐字打印逻辑
        String text = resources.getEventText(event);
        final String[] fullText = {text};
        final int[] index = {0};
        Timer typeTimer = new Timer(50, e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.append(String.valueOf(fullText[0].charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        dialogPanel.setVisible(true);
        typeTimer.start();
        //人物立绘
        ImageIcon[] CharIcon = resources.getEventImage(event);
        JLabel Chara = new JLabel();
        //此时判断本次事件是不是接连事件
        if(!linkIcon.isEmpty())
        {
            //是接连发生的事件则一左一右
            //待修改
            //展示第一个，话说完了点击再展示第二个
            if(!specialEvent[0]) {
                if(isTest) {
                    System.out.println("**********不为空且不是特殊事件");
                }
                Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,650,720 - CharIcon[0].getIconHeight() - 30,CharIcon[0].getIconWidth(),
                        CharIcon[0].getIconHeight(),CharIcon[0]);

                diaPanel.add(Chara);
                resizeComponents();
                diaPanel.revalidate();
                diaPanel.repaint();
                
                JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,300,720 - linkIcon.get(0).getIconHeight() - 30,
                        linkIcon.get(0).getIconWidth(),linkIcon.get(0).getIconHeight(),linkIcon.get(0));
                diaPanel.add(Chara2);
                diaPanel.setComponentZOrder(Chara2, 1);
                linkIcon.remove(0);
                if(event.eventname == EventName.gprz11r)
                {
                    //共有认证
                    System.out.println("共有认证rrrr");
                    if (events.getFirst().eventname == EventName.gprz11p)
                    {
                        if (isTest)
                        {
                            System.out.println("共有认证pppp");
                        }
                        linkIcon.add(CharIcon[0]);
                        specialEvent[0] = true;
                    }
                    else
                    {
                        if (isTest)
                        {
                            System.out.println("共有认证失败");
                        }
                    }
                }
            }
            else
            {
                Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,650,720 - linkIcon.get(0).getIconHeight() - 30,linkIcon.get(0).getIconWidth() ,
                        linkIcon.get(0).getIconHeight(),linkIcon.get(0));
                diaPanel.add(Chara);
                resizeComponents();
                diaPanel.revalidate();
                diaPanel.repaint();
                JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,300,720 - CharIcon[0].getIconHeight() - 30,
                        CharIcon[0].getIconWidth(), CharIcon[0].getIconHeight(),CharIcon[0]);
                diaPanel.add(Chara2);
                diaPanel.setComponentZOrder(Chara2, 1);
                linkIcon.remove(0);
                specialEvent[0] = false;
            }
        }
        else{
            if(isTest)
            {
                System.out.println("linkIcon是空");
            }
            switch(event.eventname)
            {
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case zcrh12:
                    Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,300,720 - CharIcon[0].getIconHeight() - 30,CharIcon[0].getIconWidth(),
                            CharIcon[0].getIconHeight(),CharIcon[0]);
                    linkIcon.add(CharIcon[0]);
                    break;
                default:
                    Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,(1280 - CharIcon[0].getIconWidth()) / 2,
                            720 - CharIcon[0].getIconHeight() - 30, CharIcon[0].getIconWidth(),CharIcon[0].getIconHeight(),CharIcon[0]);
                    break;
            }
            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();
        }
        // 按钮点击事件
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText[0].length())
            {
                dialogText.setText(fullText[0]);
                index[0] = fullText[0].length();
                typeTimer.stop();
            }
            else
            {
                if(events.isEmpty())
                {
                    //如果为空，进入下一阶段
                    linkIcon.clear();
                    currentScene = Scene.GAME_SCENE_VOTE;
                }
                run();
            }
        });
        dialogPanel.add(nextBtn);  // 添加到对话框面板
        dialogPanel.add(dialogText);  // 添加到对话框面板
        dialogPanel.add(back);  // 添加到对话框面板
        diaPanel.add(background);          // 最底层：背景
        jPanel.add(diaPanel);
        // 强制触发一次大小调整
        jPanel.setComponentZOrder(diaPanel,0);
        resizeComponents();
        diaPanel.revalidate();
        diaPanel.repaint();
        diaPanel.setVisible(true);
        jPanel.revalidate();
        jPanel.repaint();
    }//白天对话，用于co
    public void dialogue_afternoon()
    {
        Event event = events.poll();
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        //背景图片
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("haikei.png"));
        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = PanelSimpleFactory.createSimplePanel(260,450,760,230,false,true);
        diaPanel.add(dialogPanel);     // 顶层：对话框面板
        JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,760,230,backIcon);
        // 角色名称标签（添加到对话框面板）
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,40,10,1000,30,uiComponentFactory.getCharacterFullName(event.ch1));
        dialogPanel.add(nameLabel);  // 添加到对话框面板
        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
        dialogText.setBounds(20,50,710,200);
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,0,0,760,230,null,null);
        // 文本逐字打印逻辑
        String text = resources.getEventText(event);
        final String[] fullText = {text};
        final int[] index = {0};
        Timer typeTimer = new Timer(50, e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.append(String.valueOf(fullText[0].charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        dialogPanel.setVisible(true);
        typeTimer.start();
        //人物立绘
        ImageIcon[] CharIcon = resources.getEventImage(event);
        JLabel Chara = new JLabel();
        //此时判断本次事件是不是接连事件
        if(!linkIcon.isEmpty())
        {
            //是接连发生的事件则一左一右
            //待修改
            //展示第一个，话说完了点击再展示第二个
            Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,650,720 - CharIcon[0].getIconHeight() - 30,CharIcon[0].getIconWidth(),
                    CharIcon[0].getIconHeight(),CharIcon[0]);
            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();
            JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,300,720 - linkIcon.get(0).getIconHeight() - 30,
                    linkIcon.get(0).getIconWidth(),linkIcon.get(0).getIconHeight(),linkIcon.get(0));
            diaPanel.add(Chara2);
            diaPanel.setComponentZOrder(Chara2, 1);
            linkIcon.remove(0);
        }
        //其他则中间
        else{
            switch(event.eventname)
            {
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case gprz11p:
                case zcrh12:
                    //isLinked = true;
                    Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,300,720 - CharIcon[0].getIconHeight() - 30,CharIcon[0].getIconWidth(),
                            CharIcon[0].getIconHeight(),CharIcon[0]);
                    linkIcon.add(CharIcon[0]);
                    break;
                default:
                    Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,(1280 - CharIcon[0].getIconWidth()) / 2,720 - CharIcon[0].getIconHeight() - 30,
                            CharIcon[0].getIconWidth(),CharIcon[0].getIconHeight(),CharIcon[0]);
                    break;
            }
            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();
        }

        // 按钮点击事件
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.setText(fullText[0]);
                index[0] = fullText[0].length();
                typeTimer.stop();
            } else {
                if(events.isEmpty()) {
                    //如果为空，进入下一阶段
                    currentScene = Scene.GAME_SCENE_VOTE;
                }
                run();
            }
        });
        dialogPanel.add(nextBtn);  // 添加到对话框面板
        dialogPanel.add(dialogText);  // 添加到对话框面板
        dialogPanel.add(back);  // 添加到对话框面板
        diaPanel.add(background);          // 最底层：背景
        jPanel.add(diaPanel);

        // 强制触发一次大小调整
        jPanel.setComponentZOrder(diaPanel,0);
        resizeComponents();
        diaPanel.revalidate();
        diaPanel.repaint();
        diaPanel.setVisible(true);
        jPanel.revalidate();
        jPanel.repaint();
    }//下午对话，用于回避co

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

    public void GameScene_vote()    //白天事件播放完毕之后自动停留的界面
    {
        jPanel.removeAll();
        if(isTest)
        {
            testBtn();//测试按钮
        }
        // 背景图片
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("komorebi002yuu.png"));
        //按钮
        if((gs.aliveCounter - 1)/2 == 1)
        {
            ImageIcon musicBtnIcon = resources.getImage("musicBtn.png");
            JButton btnMusic = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,15,35,ConstNum.RETURN_WIDTH,ConstNum.RETURN_HEIGHT,musicBtnIcon);
            btnMusic.addActionListener(e -> {
                int cur = count % 4;
                switch(cur){
                    case 0:
                        name = "真甜呢丨先来一斤吧 - ラストボス02.wav";
                        break;
                    case 1:
                        name = "西江紫堂 - 黒き明日、清き闇.wav";
                        break;
                    case 2:
                        name = "西江紫堂 - BLOOD.wav";
                        break;
                    case 3:
                        name = "西江紫堂 - 灯り無き眼光.wav";
                        break;
                }
                count++;
                resources.playBgm(name);
            });
            jPanel.add(btnMusic);
        }
        else {
            ImageIcon musicBtnIcon = resources.getImage("musicBtn.png");
            JButton btnMusic = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,15,35,ConstNum.RETURN_WIDTH,ConstNum.RETURN_HEIGHT,musicBtnIcon);
            btnMusic.addActionListener(e -> {
                int cur = count % 13;
                switch(cur){
                    case 0:
                        name = "Death Impact.wav";
                        text = "Death Impact.txt";
                        break;
                    case 1:
                        name = "永久色五色.wav";
                        text = "永久色五色.txt";
                        break;
                    case 2:
                        name = "Emotionally Unstable.wav";
                        text = "Emotionally Unstable.txt";
                        break;
                    case 3:
                        name = "FELT LOVE.wav";
                        text = "FELT LOVE.txt";
                        break;
                    case 4:
                        name = "Just Complex.wav";
                        text = "Just Complex.txt";
                        break;
                    case 5:
                        name = "Peace of lost puzzle.wav";
                        text = "Peace of lost puzzle.txt";
                        break;
                    case 6:
                        name = "TEEK TEEK TEEK.wav";
                        text = "TEEK TEEK TEEK.txt";
                        break;
                    case 7:
                        name = "ダンジョン09.wav";
                        text = "ダンジョン09.txt";
                        break;
                    case 8:
                        name = "ライアービジネス.wav";
                        text = "ライアービジネス.txt";
                        break;
                    case 9:
                        name = "五月雨Vivaride.wav";
                        text = "五月雨Vivaride.txt";
                        break;
                    case 10:
                        name = "村08.wav";
                        text = "村08.txt";
                        break;
                    case 11:
                        name = "真甜呢丨先来一斤吧 - ダンジョン14.wav";
                        text = "真甜呢丨先来一斤吧 - ダンジョン14.txt";
                        break;
                    case 12:
                        name = "西江紫堂 - 五月雨Vivaride-Samidare bibaraido-.wav";
                        text = "西江紫堂 - 五月雨Vivaride-Samidare bibaraido-.txt";
                        break;

                }
                count++;
                resources.playBgm(name);
            });
            jPanel.add(btnMusic);
        }
        for (int i = 1; i <= gs.gc.length - 1; i++)
        {
            // 获取第i个角色对象
            //头像命名规则01s.png 01gs.png
            StringBuilder xName = new StringBuilder();
            StringBuilder imageName = new StringBuilder();
            if(gs.gc[i].number <=9)imageName.append("0");
            imageName.append(gs.gc[i].number);
            switch(gs.gc[i].whyDie){
                case NONE:
                    break;
                case chuxing:
                    imageName.append("g");
                    xName.append("turi.png");
                    break;
                case daymaozhou:
                    imageName.append("g");
                    xName.append("noroi.png");
                    break;
                case dayhouzhui:
                    imageName.append("g");
                    xName.append("atooi.png");
                    break;
                default:
                    imageName.append("g");
                    xName.append("kami.png");
                    break;
            }
            imageName.append("s.png");
            String textName = gs.gc[i].number + "job.png";//文本
            StringBuilder claimedRoleName = new StringBuilder("yaku");
            StringBuilder skillTargetName = new StringBuilder("result");
            if(gs.gc[i].claimedRole > 0 && gs.gc[i].claimedRole < 6)
            {
                //有职业则进入
                if(gs.gc[i].claimedRole <= 3)
                {
                    //职业图标
                    claimedRoleName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder).append(".png");
                    //技能结果
                    skillTargetName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder);
                    int a = 0;
                    for(int day = 1;day < gs.gameDay;day++)
                    {
                        a = gs.gc[i].skillTarget[day];
                    }
                    if(a != 0)
                    {
                        claimedRolenum[i][gs.gameDay] = gs.gc[i].claimedRole;
                        //有技能使用对象
                        //添加职业顺序
                        skillTargetOrder[i][gs.gameDay] = gs.gc[i].claimedRoleorder;
                        if(gs.gc[i].claimedRole != 3){
                            if(a >= gs.gc.length){
                                //黑球
                                skillTargetName.append("black.png");
                                skillTargetPeople[i][gs.gameDay] = (a + 1 -gs.gc.length);
                            }
                            else{
                                //白球
                                skillTargetName.append("white.png");
                                skillTargetPeople[i][gs.gameDay] = (a);
                            }
                        }
                        else{
                            if(a >= gs.gc.length){
                                //猎人标签
                                skillTargetName.append(".png");
                                skillTargetPeople[i][gs.gameDay] =(a + 1 -gs.gc.length);
                            }
                            else{
                                //白球
                                skillTargetName.append(".png");
                                skillTargetPeople[i][gs.gameDay] =(a);
                            }
                        }
                        skillTargetNames[i][gs.gameDay] =(skillTargetName.toString());
                    }
                }
                else
                {
                    claimedRoleName.append(gs.gc[i].claimedRole).append(".png");
                }
                ImageIcon claimedRoleIcon = resources.getImage(claimedRoleName.toString());
                JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                //职业
                if(i <= gs.gc.length / 2)
                    claimedRoleLabel.setBounds(160+64 * i,0,claimedRoleIcon.getIconWidth(),claimedRoleIcon.getIconHeight());
                else
                    claimedRoleLabel.setBounds(160+64 * (i  - ((gs.gc.length - 1+1)/2)),98,claimedRoleIcon.getIconWidth(),claimedRoleIcon.getIconHeight());
                jPanel.add(claimedRoleLabel);
            }
            if(!xName.isEmpty())
            {
                //不为空说明有死亡
                ImageIcon deathImage = resources.getImage(xName.toString());
                JLabel deathLabel = new JLabel(deathImage);
                //死亡叉叉
                if(i <= (gs.gc.length - 1 + 1)/2)
                    deathLabel.setBounds(165+64 * i,10,deathImage.getIconWidth(),deathImage.getIconHeight());
                else
                    deathLabel.setBounds(165+64 * (i  - (gs.gc.length / 2)),108,deathImage.getIconWidth(),deathImage.getIconHeight());
                jPanel.add(deathLabel);
            }

            ImageIcon characterImage = resources.getImage(imageName.toString());
            ImageIcon characterText = resources.getImage(textName);
            JLabel label = new JLabel(characterImage);
            JLabel textLabel;
            if(i <= (gs.gc.length - 1 + 1)/2)
            {
                label.setBounds(160+characterImage.getIconWidth() * i,0,characterImage.getIconWidth(),characterImage.getIconHeight());
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,175+characterImage.getIconWidth() * i,characterImage.getIconHeight()-characterText.getIconHeight()/2,
                        characterText.getIconWidth() / 2,characterText.getIconHeight()/2,characterText);
            }
            else
            {
                label.setBounds(160+characterImage.getIconWidth() * (i - gs.gc.length /2),characterImage.getIconHeight(),
                        characterImage.getIconWidth(),characterImage.getIconHeight());
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,175+characterImage.getIconWidth() * (i - gs.gc.length /2),2* characterImage.getIconHeight()-characterText.getIconHeight()/2,
                        characterText.getIconWidth() / 2,characterText.getIconHeight()/2,characterText);
            }
            jPanel.add(textLabel);
            jPanel.add(label);
        }

        for(int k = 2;k <= gs.gameDay;++k){
            for(int j = 1;j < gs.gc.length;++j) {
                if(skillTargetPeople[j][k] == 0){
                    continue;
                }
                int i = skillTargetPeople[j][k];
                int zynum = claimedRolenum[j][k];
                String name = skillTargetNames[j][k];
                int order = skillTargetOrder[j][k];
                if (zynum == 3) continue;//是猎人就不显示
                if(zynum == 1&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//占卜
                if(zynum == 2&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//灵能
                //如果该人物是被使用技能的
                ImageIcon skillTargetIcon = resources.getImage(name);
                JLabel skillTargetLabel = new JLabel(skillTargetIcon);
                if (i <= (gs.gc.length - 1 + 1) / 2)
                {
                    //技能
                    skillTargetLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,160 + 64 * (i + 1) - skillTargetIcon.getIconWidth() * zynum,
                             (order - 1) * skillTargetIcon.getIconHeight(),skillTargetIcon.getIconWidth(),skillTargetIcon.getIconHeight(),
                            skillTargetIcon);
                }
                else
                {
                    skillTargetLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            160 + 64 * (i + 1 - ((gs.gc.length - 1 + 1) / 2)) - skillTargetIcon.getIconWidth() * zynum,
                            98 + (order - 1) * skillTargetIcon.getIconHeight(),
                            skillTargetIcon.getIconWidth(),skillTargetIcon.getIconHeight(),skillTargetIcon);
                }
                jPanel.add(skillTargetLabel);
                jPanel.setComponentZOrder(skillTargetLabel, 0);
            }
        }

        //显示数据
        JLabel data = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,880,10,resources.getImage("hiduke.png"));
        //数据文本
        JTextArea dataText = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE,20,
                "    "+gs.gameDay+"日目\n 生存者:" +gs.aliveCounter+"\n 死亡者:"+gs.deathCounter+"\n 吊り縄:"+(gs.aliveCounter - 1)/2,true);
        dataText.setBounds(890,25,100,130);
        jPanel.add(dataText);
        jPanel.add(data);
        // 文本显示区域
        //内容
        //从gsgc获取，先for循环所有角色，如果有声称的职业，则获取第i天的使用技能对象和结果，并输出
        // 从上到下是 占卜， 灵能， 处刑， 尸体，猎人
        // 用多个string来分别获取 \n来换行
        //最后拼在一起，用textArea来展示
        StringBuilder zhanbu = new StringBuilder();
        StringBuilder lingneng = new StringBuilder();
        StringBuilder chuxing = new StringBuilder();
        StringBuilder shiti = new StringBuilder();
        StringBuilder lieren = new StringBuilder();
        //测试用
        //循环
        List<Boolean> isPeace = new ArrayList<>();
        for(int i = 1; i <= gs.gc.length - 1;i++)
        {
            for(int j = 1;j < gs.gameDay;++j)
            {
                if (gs.gc[i].dieDay == j)
                {
                    isPeace.add(false);//只要有一个的死亡日期是昨天，就不平和
                }
            }
        }
        List<Integer> peacePos = new ArrayList<>();
        for(int j = 1;j < gs.gameDay;++j)
        {
            int pos = 0;
            if(!isPeace.isEmpty()&&isPeace.get(j-1))
            {
                //第j天平和，则找出位置
                for(int i = 1; i <= gs.gc.length - 1;i++)
                {
                    if(gs.gc[i].dieDay!= 0 && gs.gc[i].dieDay < j)
                    {
                        pos++;
                    }
                }
                peacePos.add(pos);
            }
        }
        //占灵猎，处刑死体这一块的文本显示准备
        for(int k = 1; k < gs.gameDay;++k)
        {
            int shitiCnt = 0;
            ArrayList<Integer> shitiNum = new ArrayList<>();
            for(int i = 1; i <= gs.gc.length - 1;i++)
            {
                //注意是宣称有这个职业才要显示
                //死亡了显示到死亡前
                if(k == 1) switch(gs.gc[i].claimedRole)
                {
                    case 1://占卜
                        zhanbu.append(uiComponentFactory.getJobText(gs.gc[i].number)).append(" : ");
                        for(int j = 1;j < gs.gameDay;++j)
                        {
                            //第一天占卜，在第二天才能看，此时只有第一天有，第二天没有
                            if(gs.gc[i].dieDay != 0&&j >= gs.gc[i].dieDay)
                            {
                                //如果超出范围
                                break;
                            }
                            else
                            {

                                if(gs.gc[i].skillTarget[j] > (gs.gc.length - 1))
                                {
                                    //黑球
                                    zhanbu.append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].skillTarget[j] - (gs.gc.length-1)].number)).append("●");
                                    zhanbu.append("→");
                                }
                                else if((gs.gc[i].skillTarget[j] > 0))
                                {
                                    zhanbu.append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].skillTarget[j]].number)).append("○");
                                    zhanbu.append("→");
                                }
                            }
                        }
                        if(gs.gc[i].nonHumanMarker)
                        {
                            zhanbu.append("破绽");
                            zhanbu.append("→");
                        }
                        zhanbu.setLength(zhanbu.length() - 1);
                        zhanbu.append("\n");//换行
                        break;
                    case 2://灵能
                        lingneng.append(uiComponentFactory.getJobText(gs.gc[i].number)).append(" : ");
                        for(int j = 2;j < gs.gameDay;++j)
                        {
                            if(gs.gc[i].dieDay != 0&&j >= gs.gc[i].dieDay)
                            {
                                //如果超出范围
                                break;
                            }
                            else
                            {
                                if(gs.gc[i].skillTarget[j] > (gs.gc.length - 1))
                                {
                                    //黑球
                                    lingneng.append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].skillTarget[j] - (gs.gc.length-1)].number)).append("●");
                                    lingneng.append("→");
                                }
                                else if((gs.gc[i].skillTarget[j] > 0))
                                {
                                    lingneng.append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].skillTarget[j]].number)).append("○");
                                    lingneng.append("→");
                                }
                            }
                        }
                        if(gs.gc[i].nonHumanMarker)
                        {
                            lingneng.append("破绽");
                            lingneng.append("→");
                        }
                        lingneng.setLength(lingneng.length()-1);
                        lingneng.append("\n");//换行
                        break;
                    case 3://猎人
                        lieren.append(uiComponentFactory.getJobText(gs.gc[i].number)).append(" : ");
                        for(int j = 2;j < gs.gameDay;++j)
                        {
                            if(gs.gc[i].dieDay != 0&&j >= gs.gc[i].dieDay)
                            {
                                //如果超出范围
                                break;
                            }
                            if(gs.gc[i].skillTarget[j] != 0)
                            {
                                //没有对象
                                lieren.append(uiComponentFactory.getJobText(gs.gc[gs.gc[i].skillTarget[j]].number));
                                lieren.append("→");
                            }
                        }
                        if(gs.gc[i].nonHumanMarker)
                        {
                            lieren.append("破绽");
                            lieren.append("→");
                        }
                        lieren.setLength(lieren.length()-1);
                        lieren.append("\n");//换行
                        break;
                }
                switch(gs.gc[i].whyDie)
                {
                    case whyDie.chuxing:
                        if(gs.gc[i].actualRole == 10)
                        {
                            for (int j = 1; j <= gs.gc.length - 1; j++)
                            {
                                //循环找一下谁是背德
                                if (gs.gc[j].actualRole == 11 && gs.gc[j].whyDie !=whyDie.NONE && gs.gc[i].dieDay == k && gs.gc[j].dieDay < gs.gc[i].dieDay)
                                {
                                    //如果背德已经死了且死亡在妖狐前，就正常显示
                                    chuxing.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("→");
                                    break;
                                }
                                //没死的话就不做处理，等后面有dayhouzhui的时候一起处理
                            }
                        }
                        else if(gs.gc[i].actualRole == 5)
                        {

                        }
                        else
                        {
                            if(gs.gc[i].dieDay == k)
                            {
                            chuxing.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("→");
                            }
                        }
                        break;
                    case whyDie.daymaozhou:
                        if(gs.gc[i].dieDay == k)
                        {
                            for (int j = 1; j <= gs.gc.length - 1; j++)
                            {
                                //循环找一下谁是猫
                                if (gs.gc[j].actualRole == 5)
                                {
                                    chuxing.append(uiComponentFactory.getJobText(gs.gc[j].number)).append("+");
                                    break;
                                }
                            }
                            //实现 猫+受猫害者
                            chuxing.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("(猫呪)").append("→");
                        }
                        break;
                    case whyDie.dayhouzhui:
                        if(gs.gc[i].dieDay == k)
                        {
                            for (int j = 1; j <= gs.gc.length - 1; j++)
                            {
                                //循环找一下谁是妖狐
                                if (gs.gc[j].actualRole == 10)
                                {
                                    chuxing.append(uiComponentFactory.getJobText(gs.gc[j].number)).append("+");
                                    break;
                                }
                            }
                            //实现 狐+背德
                            chuxing.append(uiComponentFactory.getJobText(gs.gc[i].number)).append("(後追)").append("→");
                        }
                        break;
                    default:
                        //其他情况，即夜晚被杀
                        if(gs.gc[i].dieDay == k)
                        {
                            shitiCnt++;
                            shitiNum.add(gs.gc[i].number);
                        }
                        break;
                }
                if(i == gs.gc.length - 1&& shitiCnt == 0)
                {
                    //当第k日循环到最后一个角色shitiCnt都为0说明没死
                    shiti.append("平和→");
                }
            }
            if(shitiCnt == 1)
            {
                shiti.append(uiComponentFactory.getJobText(shitiNum.get(0))).append("→");
            }
            else
            {
                for (int l = 0; l < shitiNum.size(); ++l)
                {
                    shiti.append(uiComponentFactory.getJobText(shitiNum.get(l))).append("+");
                }
                shiti.setLength(shiti.length()-1);
                shiti.append("→");
            }
        }
        if(shiti.length()>2)
        {
            shiti.setLength(shiti.length() - 1);
        }
        if(chuxing.length()>2)
        {
            chuxing.setLength(chuxing.length() - 1);
        }
        StringBuilder result = new StringBuilder();
        result.append("[占い師]\n").append(zhanbu).append("[霊能者]\n").append(lingneng).append("[処刑]\n").append(chuxing).append("\n[死体]\n").append
                (shiti).append("\n[護衛先]\n").append(lieren);
        //背景
        ImageIcon boardImage = resources.getImage("frame #19252.png");
        JLabel board = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,198,200 + boardImage.getIconWidth(),50 + boardImage.getIconHeight(),boardImage);
        JTextArea infoText = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK,24,result.toString(),false);
        // 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(infoText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 不显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scrollPane.setBounds(40,228,200 + boardImage.getIconWidth() - 80,50 + boardImage.getIconHeight() - 60);
        //按钮
        //投票
        JButton voteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126,resources.getImage("goTohyo.png"));

        jPanel.add(voteBtn);
        //记录确认
        JButton recordBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 2 - 10,resources.getImage("check.png"));
        jPanel.add(recordBtn);
        //指示按钮
        JButton pointBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("shiji.png"));
        jPanel.add(pointBtn);
        //回避按钮(关)
        JButton avoidBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 4 - 30,resources.getImage("关闭回避.png"));
        jPanel.add(avoidBtn);
        //回避按钮(开)
        JButton avoidBtn1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 4 - 30,resources.getImage("开启回避.png"));
        avoidBtn1.setVisible(false);
        jPanel.add(avoidBtn1);
        //退出按钮
        JButton menuBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 5 - 40,resources.getImage("IntroTitle.png"));
        menuBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        jPanel.add(menuBtn);
        //投票第二层
        //灰随机
        JButton greyBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("tohyoGrey.png"));
        greyBtn.setVisible(false);
        jPanel.add(greyBtn);
        //自由投票
        JButton freeBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 2 - 10,resources.getImage("tohyoFree.png"));
        freeBtn.setVisible(false);
        jPanel.add(freeBtn);

        //记录确认第二层
        //怀疑度
        JButton doubtBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("checkUtagai.png"));
        doubtBtn.setVisible(false);
        jPanel.add(doubtBtn);
        //投票履历
        JButton votehisBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 2 - 10,resources.getImage("checkTohyo.png"));
        votehisBtn.setVisible(false);
        jPanel.add(votehisBtn);

        //指示按钮第二层
        //co指示
        JButton coBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("doCO.png"));
        coBtn.setVisible(false);
        jPanel.add(coBtn);
        //指定指示
        JButton ppBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 2 - 10,resources.getImage("doShitei.png"));
        ppBtn.setVisible(false);
        jPanel.add(ppBtn);
        //返回按钮
        JButton returnBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126,resources.getImage("return.png"));
        returnBtn.setVisible(false);
        jPanel.add(returnBtn);
        //下一天按钮
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 ,resources.getImage("nextDay.png"));
        nextBtn.setVisible(false);
        jPanel.add(nextBtn);
        //同票再次投票按钮
        JButton againBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126,resources.getImage("goTohyo.png"));
        againBtn.setVisible(false);
        jPanel.add(againBtn);
        //指定投票按钮
        //指定投票
        JButton readyVoteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("tohyoShitei.png"));
        readyVoteBtn.setVisible(false);
        jPanel.add(readyVoteBtn);
        //co按钮第三层
        //询问co
        JButton askCoBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 4 - 30,resources.getImage("询问CO.png"));
        askCoBtn.setVisible(false);
        jPanel.add(askCoBtn);
        //灵能指示
        JButton reiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("reiCO.png"));
        reiBtn.setVisible(false);
        jPanel.add(reiBtn);
        //猎人指示
        JButton kariBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 2 - 10,resources.getImage("kariCO.png"));
        kariBtn.setVisible(false);
        jPanel.add(kariBtn);

        //占卜指示
        JButton uranaiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060 - 194 - 30,720 - 40 - 126 * 3 - 20,resources.getImage("uranaiCO.png"));
        uranaiBtn.setVisible(false);
        jPanel.add(uranaiBtn);
        //共有指示
        JButton kyouyuBtn  = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060 - 194 - 30,720 - 40 - 126 * 2 - 10,resources.getImage("kyouyuCO.png"));
        kyouyuBtn.setVisible(false);
        jPanel.add(kyouyuBtn);
        //猫又指示
        JButton catBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060 - 194 - 30,720 - 40 - 126,resources.getImage("catCO.png"));
        catBtn.setVisible(false);
        jPanel.add(catBtn);
        //指定指示按钮第三层
        //指定投票
        JButton fixedVoteBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("tohyoShitei.png"));
        fixedVoteBtn.setVisible(false);
        jPanel.add(fixedVoteBtn);
        //指定占卜
        JButton fixedUranaiBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 2 - 10,resources.getImage("shiteiUranai.png"));
        fixedUranaiBtn.setVisible(false);
        jPanel.add(fixedUranaiBtn);

        //指定护卫
        JButton protectBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126,resources.getImage("shiteiGoei.png"));
        protectBtn.setVisible(false);
        jPanel.add(protectBtn);

        if(isAvoid) {
            avoidBtn.setVisible(true);
            avoidBtn1.setVisible(false);
        }
        else{
            avoidBtn1.setVisible(true);
            avoidBtn.setVisible(false);
        }

        JScrollPane scrollPane1 = new JScrollPane();//指定信息的容器
        if (!scrollPane1.isAncestorOf(jPanel)) { // 避免重复添加
            jPanel.add(scrollPane1);
        }


        List<Integer> cxList = new ArrayList<>();
        List<Integer> beiZhan1 = new ArrayList<>();
        for(int j = 1;j<gs.gameDay;++j)
        {
            //第j天
            for(int k = 1;k<gs.gc.length;++k)
            {
                if(gs.gc[k].claimedRole == 1)
                {
                    //如果有占卜师
                    int num = gs.gc[k].skillTarget[j];

                    if(num > gs.gc.length - 1)
                    {
                        num -= gs.gc.length- 1;
                    }

                    if(!beiZhan1.contains(num))
                    {
                        beiZhan1.add(num);
                        System.out.println(num);
                    }
                }
            }
        }

        for(int i = 1;i<gs.gc.length;++i)
        {
            //灰投票是获取无球无职活着的人
            if(isTest)
            {
                System.out.println("已进入灰循环" + uiComponentFactory.getJobText(gs.gc[i].number) + " " +gs.gc[i].whyDie+" "+ gs.gc[i].claimedRole);
            }
            if(gs.gc[i].whyDie == whyDie.NONE&&(gs.gc[i].claimedRole == 0 || gs.gc[i].claimedRole == 6))
            {
                //活着
                //不能有声称的职业
                //村人和未声明的
                if(beiZhan1.contains(i))
                {
                    if(isTest)
                    {
                        System.out.println(uiComponentFactory.getJobText(gs.gc[i].number) + "被占卜过了，不是灰");
                    }
                    continue;
                }
                if(isTest)
                {
                    System.out.println(uiComponentFactory.getJobText(gs.gc[i].number) + "是灰");
                }
                cxList.add(i);

            }
        }
        //怀疑度
        doubtBtn.addActionListener(e ->
        {
            resources.playSound("click.wav");
            doubtBtn.setVisible(false);
            votehisBtn.setVisible(false);
            infoText.setVisible(false);
            createDoubt();
        });
        //投票履历
        
        JPanel hisPanel = PanelSimpleFactory.createSimplePanel(0,0,false,false);
        hisPanel.setBounds(0,198,200 + boardImage.getIconWidth(),50 + boardImage.getIconHeight());

        jPanel.add(hisPanel);
        hisPanel.setVisible(false);
        ImageIcon levelIcon = resources.getImage(levelName);
        JLabel levellb = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,700,600,levelIcon);
        levellb.setVisible(false);
        jPanel.add(levellb);
        //投票履历
        boolean[] isVotehis = {false};
        votehisBtn.addActionListener(e -> {
            isVotehis[0] = true;//只要进来就是true
            resources.playSound("click.wav");
            doubtBtn.setVisible(false);
            votehisBtn.setVisible(false);
            infoText.setVisible(false);

            hisPanel.setVisible(true);
            hisPanel.removeAll();
            jPanel.setComponentZOrder(levellb,0);
            levellb.setVisible(true);

            for(int i = 0;i < voteRounds.size();++i)
            {
                int gameday = i+2;
                int roundMax = voteRounds.get(gameday-2);
                //前结果
                JButton backResult = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("rirekiBack.png"));

                backResult.setVisible(false);
                jPanel.add(backResult);
                jPanel.setComponentZOrder(backResult,0);
                //次结果

                JButton nextResult = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 2 - 10,resources.getImage("rirekiNext.png"));

                nextResult.setVisible(false);
                jPanel.add(nextResult);
                jPanel.setComponentZOrder(nextResult,0);
                //前结果
                JButton backResult1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 3 - 20,resources.getImage("rirekiBack.png"));

                backResult1.setVisible(false);
                jPanel.add(backResult1);
                jPanel.setComponentZOrder(backResult1,0);
                //次结果

                JButton nextResult1 = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1060,720 - 40 - 126 * 2 - 10,resources.getImage("rirekiNext.png"));

                nextResult1.setVisible(false);
                jPanel.add(nextResult1);
                jPanel.setComponentZOrder(nextResult1,0);

                nextResult.addActionListener(e1 -> {
                    createDayPiao(2,gameday,voteMethods.get(gameday-2));
                    backResult.setVisible(true);
                    nextResult.setVisible(false);
                    if(roundMax == 3)nextResult1.setVisible(true);
                });
                backResult.addActionListener(e1 -> {
                    createDayPiao(1,gameday,voteMethods.get(gameday-2));
                    backResult.setVisible(false);
                    nextResult.setVisible(true);
                    nextResult1.setVisible(false);
                });
                nextResult1.addActionListener(e1 -> {
                    createDayPiao(3,gameday,voteMethods.get(gameday-2));
                    backResult1.setVisible(true);
                    nextResult1.setVisible(false);
                    backResult.setVisible(false);
                });
                backResult1.addActionListener(e1 -> {
                    createDayPiao(2,gameday,voteMethods.get(gameday-2));
                    backResult1.setVisible(false);
                    nextResult1.setVisible(true);
                    backResult.setVisible(true);
                });
                ImageIcon dayIcon = resources.getImage(gameday + "day.png");
                JButton dayBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,dayIcon);
                dayBtn.setSize(dayIcon.getIconWidth(),dayIcon.getIconHeight());
                dayBtn.addActionListener(e1 -> {
                    createDayPiao(1,gameday,voteMethods.get(gameday-2));
                    hisPanel.setVisible(false);
                    if(roundMax != 1){
                       nextResult.setVisible(true);
                    }
                });
                if(i < 5)
                {
                    dayBtn.setLocation((10+dayIcon.getIconWidth())*i,10);
                }
                else if(i < 10)
                {
                    dayBtn.setLocation((10+dayIcon.getIconWidth())*(i -5),dayIcon.getIconHeight()+20);
                }
                else
                {
                    dayBtn.setLocation((10+dayIcon.getIconWidth())*(i -10),dayIcon.getIconHeight()*2+30);
                }
                hisPanel.add(dayBtn);
            }
            hisPanel.setVisible(true);
            jPanel.setComponentZOrder(hisPanel,0);

            resizeComponents();
            jPanel.revalidate();
            jPanel.repaint();

        });
        //投票
        voteBtn.addActionListener(e -> {
            infoText.setVisible(false);
            resources.playSound("click.wav");
            voteBtn.setVisible(false);
            pointBtn.setVisible(false);
            avoidBtn.setVisible(false);
            recordBtn.setVisible(false);
            avoidBtn1.setVisible(false);
            if(isVote[0]){
                readyVoteBtn.setVisible(true);
                greyBtn.setVisible(false);
                freeBtn.setVisible(false);

            }
            else {
                if(!cxList.isEmpty()){
                    //有灰色就设置为true
                    greyBtn.setVisible(true);
                }
                freeBtn.setVisible(true);
                readyVoteBtn.setVisible(false);
            }
            if(cxList.isEmpty()){
                greyBtn.setVisible(false);
            }
            returnBtn.setVisible(true);
            scrollPane1.setVisible(true);

            //投票逻辑
            //-指定内容-
            //[指定投票]
            //騎士
            //[指定占い]
            //潜伏→店員,
            //騎士→研究,店員,
            //[指定護衛]
            //潜伏→召使,洋灯,
            JTextArea isSelectedVoteTargetText = new JTextArea();
            StringBuilder isSelectedVoteTargetResult = new StringBuilder("-指定内容-\n");
            if(isVote[0]) {
                isSelectedVoteTargetResult.append("[指定投票]\n");
                for (int i = 1; i < gs.gc.length; ++i) {
                    if (gs.gc[i].isSelectedVoteTarget[gs.gameDay]) {
                        isSelectedVoteTargetResult.append(uiComponentFactory.getJobText(gs.gc[i].number)+" ");
                    }

                }
                isSelectedVoteTargetResult.append("\n");
            }


            if(isZhan[0]) {
                isSelectedVoteTargetResult.append("[指定占い]\n");
                for (int i = 1; i < gs.gc.length; ++i) {
                    if (gs.gc[i].claimedRole == 1&& gs.gc[i].whyDie == whyDie.NONE) {
                        isSelectedVoteTargetResult.append(uiComponentFactory.getJobText(gs.gc[i].number) + "→");
                        for (int j = 1; j < gs.gc.length; ++j) {
                            if (gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                                isSelectedVoteTargetResult.append(uiComponentFactory.getJobText(gs.gc[j].number) + ",");
                            }

                        }
                        isSelectedVoteTargetResult.append("\n");
                    }
                }
                int cc = 0;
                for (int j = 1; j < gs.gc.length; ++j) {
                    if (gs.hiddenSeerScheduledSkillTargets[j][gs.gameDay]) {
                        if(cc++ == 0)isSelectedVoteTargetResult.append("潜伏→");
                        isSelectedVoteTargetResult.append(uiComponentFactory.getJobText(gs.gc[j].number) + ",");
                    }

                }
                isSelectedVoteTargetResult.append("\n");

            }

            if(isTest) {
                System.out.println("是否护卫"+isHu[0]);
                for (int i = 1; i < gs.gc.length; ++i) {
                    for (int j = 1; j < gs.gc.length; ++j) {
                        if (gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                            System.out.println(uiComponentFactory.getJobText(gs.gc[i].number)+"护卫了"+gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]);
                        }
                    }
                }
            }
            if(isHu[0]) {
                isSelectedVoteTargetResult.append("[指定護衛]\n");

                for (int i = 1; i < gs.gc.length; ++i) {
                    if (gs.gc[i].claimedRole == 3 && gs.gc[i].whyDie == whyDie.NONE) {
                        isSelectedVoteTargetResult.append(uiComponentFactory.getJobText(gs.gc[i].number) + "→");
                        for (int j = 1; j < gs.gc.length; ++j) {
                            if (gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                                isSelectedVoteTargetResult.append(uiComponentFactory.getJobText(gs.gc[j].number) + ",");
                            }
                        }
                        isSelectedVoteTargetResult.append("\n");
                    }
                }
                int vv = 0;
                for (int j = 1; j < gs.gc.length; ++j) {
                    if (gs.hiddenHunterScheduledSkillTargets[j][gs.gameDay]) {
                        if(vv++ == 0) isSelectedVoteTargetResult.append("潜伏→");
                        isSelectedVoteTargetResult.append(uiComponentFactory.getJobText(gs.gc[j].number) + ",");
                    }

                }
                isSelectedVoteTargetResult.append("\n");

            }
            isSelectedVoteTargetText.setText(isSelectedVoteTargetResult.toString());
            isSelectedVoteTargetText.setForeground(Color.BLACK);
            isSelectedVoteTargetText.setFont(new Font("Takao Mincho",Font.BOLD,24));
            isSelectedVoteTargetText.setLineWrap(true);       // 自动换行
            isSelectedVoteTargetText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
            isSelectedVoteTargetText.setEditable(false);
            isSelectedVoteTargetText.setOpaque(false);
            isSelectedVoteTargetText.setBackground(new Color(0, 0, 0, 0));
            isSelectedVoteTargetText.setBorder(BorderFactory.createEmptyBorder());
            // 创建滚动面板，包裹文本区域
            scrollPane1.getViewport().setView(isSelectedVoteTargetText);
            // 隐藏不必要的边框
            scrollPane1.setBorder(BorderFactory.createEmptyBorder());
            // 不显示滚动条
            scrollPane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
            scrollPane1.setOpaque(false);
            scrollPane1.getViewport().setOpaque(false); // 确保滚动面板背景透明
            scrollPane1.setBounds(40,228,200+boardImage.getIconWidth() - 80,50+boardImage.getIconHeight()- 60);

            isSelectedVoteTargetText.setVisible(true);

            jPanel.setComponentZOrder(scrollPane1,0);

            resizeComponents();
            jPanel.revalidate();
            jPanel.repaint();
        });
        int[] round = {1};
        //灰随机
        greyBtn.addActionListener(e -> {
            freeBtn.setVisible(false);
            greyBtn.setVisible(false);
            returnBtn.setVisible(false);

            scrollPane1.setVisible(false);
            StringBuilder greyText = new StringBuilder();
            for(int i = 0;i < cxList.size();++i){
                greyText.append(uiComponentFactory.getJobText(gs.gc[cxList.get(i)].number));
            }
            List<Integer> beiZhan = new ArrayList<>();
            for(int j = 1;j<gs.gameDay;++j){
                //第j天
                for(int k = 1;k<gs.gc.length;++k){
                    if(gs.gc[k].claimedRole == 1) {
                        //如果有占卜师
                        int num = gs.gc[k].skillTarget[j];

                        if(num > gs.gc.length - 1){
                            num -= gs.gc.length- 1;
                        }

                        if(!beiZhan.contains(num)) {
                            beiZhan.add(num);
                            System.out.println(num);
                        }
                    }
                }
            }
            boolean[] isReVote = {false};
            if(isTest){
                if(!cxList.isEmpty()){
                    System.out.println("cxList不为空，且具体为" + cxList);
                }
            }
            if(mainLogic.shokei(1,cxList,isAvoid)){
                //如果成功，则获得处刑event和票型
                //先展示票型
                int trueDay = 0;
                if(gs.end == 0) {
                    trueDay = gs.gameDay-1;
                }
                else{
                    trueDay = gs.gameDay;
                }

                for(int f = 0;f < cxList.size();++f){
                    greyCharas[f][trueDay] = cxList.get(f);
                }
                voteMethods.add(1);//成功就添加
                cxList.clear();
                gs = mainLogic.getGameStatus();
                createPiao("-投票結果/" + (trueDay) + "日目-グレラン：\n" + greyText + "\n", round[0], isReVote);
                //如果同票
                if(isReVote[0]) {
                    againBtn.setVisible(true);
                    nextBtn.setVisible(false);
                    round[0]++;
                }else{
                    nextBtn.setVisible(true);
                    againBtn.setVisible(false);
                }

            }
            else{
                //失败则说明有回避co，获取event并显示，然后回到投票前
                //显示
                //后恢复
                cxList.clear();
                freeBtn.setVisible(true);
                greyBtn.setVisible(true);
                returnBtn.setVisible(true);
                scrollPane1.setVisible(true);

                //回避时禁止其他按钮使用
                votehisBtn.setVisible(false);
                doubtBtn.setVisible(false);
                recordBtn.setVisible(false);
                coBtn.setVisible(false);
                ppBtn.setVisible(false);
                returnBtn.setVisible(false);
                reiBtn.setVisible(false);
                kariBtn.setVisible(false);
                uranaiBtn.setVisible(false);
                kyouyuBtn.setVisible(false);
                catBtn.setVisible(false);
                askCoBtn.setVisible(false);
                greyBtn.setVisible(false);
                freeBtn.setVisible(false);
                readyVoteBtn.setVisible(false);
                nextBtn.setVisible(false);
                againBtn.setVisible(false);

                currentScene = Scene.DIALOGUE_AFTERNOON;
                run();

            }
        });
        //自由投票逻辑
        freeBtn.addActionListener(e -> {

            freeBtn.setVisible(false);
            greyBtn.setVisible(false);
            returnBtn.setVisible(false);
            resources.playSound("click.wav");
            scrollPane1.setVisible(false);
            List<Integer> chuxingList = new ArrayList<>();
            for(int i = 1;i<gs.gc.length;++i){
                if(gs.gc[i].whyDie == whyDie.NONE)chuxingList.add(i);//自由投票就是活着的人
            }
            if(isTest){
                System.out.println("自由投票"+chuxingList);
            }
            boolean[] isReVote = {false};
            if(mainLogic.shokei(0,chuxingList,isAvoid)){
                //如果成功，则获得处刑event和票型
                //先展示票型
                voteMethods.add(0);
                chuxingList.clear();//清理
                gs = mainLogic.getGameStatus();
                if(gs.end == 0) {
                    createPiao("-投票結果/" + (gs.gameDay - 1) + "日目-自由投票\n\n", round[0], isReVote);
                }
                else{
                    createPiao("-投票結果/" + (gs.gameDay) + "日目-自由投票\n\n", round[0], isReVote);
                }
                //如果同票
                if(isReVote[0]) {
                    againBtn.setVisible(true);
                    nextBtn.setVisible(false);
                    round[0]++;
                }else{
                    nextBtn.setVisible(true);
                    againBtn.setVisible(false);

                }

            }
            else{
                //失败则说明有回避co，获取event并显示，然后回到投票前
                //显示
                //后恢复
                chuxingList.clear();
                freeBtn.setVisible(true);
                greyBtn.setVisible(true);
                returnBtn.setVisible(true);
                scrollPane1.setVisible(true);

                //回避时禁止其他按钮使用
                votehisBtn.setVisible(false);
                doubtBtn.setVisible(false);
                recordBtn.setVisible(false);
                coBtn.setVisible(false);
                ppBtn.setVisible(false);
                returnBtn.setVisible(false);
                reiBtn.setVisible(false);
                kariBtn.setVisible(false);
                uranaiBtn.setVisible(false);
                kyouyuBtn.setVisible(false);
                catBtn.setVisible(false);
                askCoBtn.setVisible(false);
                greyBtn.setVisible(false);
                freeBtn.setVisible(false);
                readyVoteBtn.setVisible(false);
                nextBtn.setVisible(false);
                againBtn.setVisible(false);

                currentScene = Scene.DIALOGUE_AFTERNOON;
                run();
            }


        });
        //重票
        againBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            boolean[] isReVote = {false};
            if(gs.end == 0) {
                createPiao("-投票結果/" + (gs.gameDay - 1) + "日目-重新投票\n\n", round[0], isReVote);
            }
            else{
                createPiao("-投票結果/" + (gs.gameDay) + "日目-重新投票\n\n", round[0], isReVote);
            }
            if(isReVote[0]) {
                againBtn.setVisible(true);
                nextBtn.setVisible(false);
                if(round[0] < 3)round[0]++;
            }else{
                nextBtn.setVisible(true);
                againBtn.setVisible(false);
            }

        });
        //下一天
        nextBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            //处刑
            //记录是第几轮
            voteRounds.add(round[0]);
            currentScene = Scene.DIALOGUE_CHUXING;
            run();

        });
        //确认指定投票
        readyVoteBtn.addActionListener(e -> {

            readyVoteBtn.setVisible(false);//自身消失
            returnBtn.setVisible(false);

            scrollPane1.setVisible(false);
            List<Integer> chuxingList = new ArrayList<>();
            int p = 0;
            for(int i = 1;i<gs.gc.length;++i){
                if(gs.gc[i].isSelectedVoteTarget[gs.gameDay] && gs.gc[i].whyDie == whyDie.NONE){
                    chuxingList.add(i);//指定投票就是指定且不死的人
                    isSelectedVoteTargetCharas[p++][gs.gameDay] = i;
                    System.out.println("指定了"+i);
                }
            }

            if(isTest){
                System.out.println("指定投票"+chuxingList);
            }

            boolean[] isReVote = {false};
            if(mainLogic.shokei(2,chuxingList,isAvoid)){
                //如果成功，则获得处刑event和票型
                //先展示票型
                voteMethods.add(2);
                gs = mainLogic.getGameStatus();
                //获取指定了那些人
                int trueDay = 0;
                if(gs.end == 0) {
                    trueDay = gs.gameDay-1;
                }
                else{
                    trueDay = gs.gameDay;
                }

                StringBuilder isSelectedVoteTargetText = new StringBuilder();
                for(int i = 0;i < chuxingList.size();++i){
                    isSelectedVoteTargetText.append(uiComponentFactory.getJobText(gs.gc[chuxingList.get(i)].number)).append(",");
                }
                System.out.println(isSelectedVoteTargetText);
                createPiao("-投票結果/" + trueDay + "日目-指定投票\n"+isSelectedVoteTargetText+"\n", round[0], isReVote);

                chuxingList.clear();
                //如果同票
                if(isReVote[0]) {
                    againBtn.setVisible(true);
                    nextBtn.setVisible(false);
                    round[0]++;
                }else{
                    nextBtn.setVisible(true);
                    againBtn.setVisible(false);

                }

            }
            else{
                //失败则说明有回避co，获取event并显示，然后回到投票前
                //显示
                //后恢复

                chuxingList.clear();
                freeBtn.setVisible(true);
                greyBtn.setVisible(true);
                returnBtn.setVisible(true);
                scrollPane1.setVisible(true);

                //回避时禁止其他按钮使用
                votehisBtn.setVisible(false);
                doubtBtn.setVisible(false);
                recordBtn.setVisible(false);
                coBtn.setVisible(false);
                ppBtn.setVisible(false);
                returnBtn.setVisible(false);
                reiBtn.setVisible(false);
                kariBtn.setVisible(false);
                uranaiBtn.setVisible(false);
                kyouyuBtn.setVisible(false);
                catBtn.setVisible(false);
                askCoBtn.setVisible(false);
                greyBtn.setVisible(false);
                freeBtn.setVisible(false);
                readyVoteBtn.setVisible(false);
                nextBtn.setVisible(false);
                againBtn.setVisible(false);

                currentScene = Scene.DIALOGUE_AFTERNOON;
                run();
            }
            //显示票型
        });
        //记录确认逻辑
        recordBtn.addActionListener(e -> {
            //如果是最开始，则显示提示
            resources.playSound("click.wav");
            if(gs.gameDay == 2){
                createTishi("まだ特に疑い先もなく、\n投票の履歴もないようだ。");

            }
            else {
                voteBtn.setVisible(false);
                pointBtn.setVisible(false);
                avoidBtn.setVisible(false);
                recordBtn.setVisible(false);
                avoidBtn1.setVisible(false);

                votehisBtn.setVisible(true);
                doubtBtn.setVisible(true);
                returnBtn.setVisible(true);
            }
        });
        //回避按钮逻辑

        //关闭
        avoidBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            avoidBtn1.setVisible(true);
            avoidBtn.setVisible(false);
            isAvoid = false;
        });
        //开启
        avoidBtn1.addActionListener(e -> {
            resources.playSound("click.wav");
            avoidBtn1.setVisible(false);
            avoidBtn.setVisible(true);
            isAvoid = true;
        });
        //指示按钮逻辑
        pointBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            voteBtn.setVisible(false);
            pointBtn.setVisible(false);
            avoidBtn.setVisible(false);
            recordBtn.setVisible(false);
            avoidBtn1.setVisible(false);

            coBtn.setVisible(true);
            ppBtn.setVisible(true);
            returnBtn.setVisible(true);
        });
        //co指示逻辑
        coBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            coBtn.setVisible(false);
            ppBtn.setVisible(false);

            reiBtn.setVisible(true);
            kariBtn.setVisible(true);
            askCoBtn.setVisible(true);
            uranaiBtn.setVisible(true);
            kyouyuBtn.setVisible(true);
            catBtn.setVisible(true);
        });
        //指示指定逻辑
        ppBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            coBtn.setVisible(false);
            ppBtn.setVisible(false);
            returnBtn.setVisible(false);

            fixedVoteBtn.setVisible(true);
            fixedUranaiBtn.setVisible(true);
            protectBtn.setVisible(true);

        });

        JPanel infoPanel = PanelSimpleFactory.createSimplePanel(0,0,true,false);//用于存头像按钮
        //new jp()
        //infoPanel.setLayout(null); // 关键！确保手动设置的坐标生效
        JPanel infoZhanPanel = PanelSimpleFactory.createSimplePanel(0,0,true,false);//用于存头像按钮
        //infoZhanPanel.setLayout(null); // 关键！确保手动设置的坐标生效
        JPanel infoHuPanel = PanelSimpleFactory.createSimplePanel(0,0,true,false);//用于存头像按钮
        //infoHuPanel.setLayout(null); // 关键！确保手动设置的坐标生效
        JPanel infoCoPanel = PanelSimpleFactory.createSimplePanel(0,0,true,false);//用于存头像按钮
        //infoCoPanel.setLayout(null); // 关键！确保手动设置的坐标生效
        //返回按钮逻辑
        List<Integer> askList = new ArrayList<>();
        boolean[] isCo = {false};
        returnBtn.addActionListener(e -> {
            if(isVotehis[0]){
                run();
            }
            resources.playSound("click.wav");
            voteBtn.setVisible(true);
            pointBtn.setVisible(true);
            infoText.setVisible(true);
            piaoText.setVisible(false);
            piaoText1.setVisible(false);
            if(isAvoid) {
                avoidBtn.setVisible(true);
                avoidBtn1.setVisible(false);
            }
            else{
                avoidBtn1.setVisible(true);
                avoidBtn.setVisible(false);
            }
            levellb.setVisible(false);
            infoPanel.setVisible(false);
            infoZhanPanel.setVisible(false);
            infoHuPanel.setVisible(false);
            scrollPane1.setVisible(false);
            hisPanel.setVisible(false);
            infoCoPanel.setVisible(false);


            votehisBtn.setVisible(false);
            doubtBtn.setVisible(false);
            recordBtn.setVisible(true);
            coBtn.setVisible(false);
            ppBtn.setVisible(false);
            returnBtn.setVisible(false);
            reiBtn.setVisible(false);
            kariBtn.setVisible(false);
            uranaiBtn.setVisible(false);
            kyouyuBtn.setVisible(false);
            catBtn.setVisible(false);
            askCoBtn.setVisible(false);
            greyBtn.setVisible(false);
            freeBtn.setVisible(false);
            readyVoteBtn.setVisible(false);
            nextBtn.setVisible(false);
            againBtn.setVisible(false);
            if(!askList.isEmpty()&& isCo[0]){
                isCo[0] = false;
                System.out.println(askList);
                mainLogic.askCo(askList);
                //获取事件
                if(!events.isEmpty()){
                    gs = mainLogic.getGameStatus();
                    currentScene = Scene.DIALOGUE_DAY;
                    run();
                }
                else{
                    createTishi("無人CO");
                }
            }
        });
        //询问co逻辑
        askCoBtn.addActionListener(e -> {
            isCo[0] = true;
            reiBtn.setVisible(false);
            kariBtn.setVisible(false);
            uranaiBtn.setVisible(false);
            kyouyuBtn.setVisible(false);
            catBtn.setVisible(false);
            askCoBtn.setVisible(false);
            infoCoPanel.setVisible(true);
            resources.playSound("click.wav");
            //点了后文本消失，用于放人物和按钮，其他不变
            infoText.setVisible(false);
            fixedVoteBtn.setVisible(false);
            fixedUranaiBtn.setVisible(false);
            protectBtn.setVisible(false);
            returnBtn.setVisible(true);

            infoCoPanel.removeAll();

            List<Integer> zhanbuNum = new ArrayList<>();
            List<Integer> zhanbuOrder = new ArrayList<>();
            for (int i = 1; i <= gs.gc.length - 1; i++) {
                if(gs.gc[i].claimedRole == 1&&gs.gc[i].whyDie == whyDie.NONE){
                    //不死且是占卜
                    zhanbuNum.add(i);//获取占卜的编号
                    zhanbuOrder.add(gs.gc[i].claimedRoleorder);//获取占卜的职业顺序
                }
            }
            List<JLabel> targetLabels = new ArrayList<>();
            List<JLabel> frameLabels = new ArrayList<>();
            List<JLabel> resultLabels = new ArrayList<>();
            List<JLabel> zbLabels = new ArrayList<>();
            for (int i = 1; i <= gs.gc.length - 1; i++) {

                //头像命名规则01s.png 01gs.png
                StringBuilder imageName = new StringBuilder();
                if(gs.gc[i].number <=9)imageName.append("0");
                imageName.append(gs.gc[i].number);
                switch(gs.gc[i].whyDie){
                    case NONE:
                        break;
                    default:
                        imageName.append("g");
                        break;
                }
                imageName.append("s.png");
                StringBuilder claimedRoleName = new StringBuilder("yaku");
                if(gs.gc[i].claimedRole > 0 && gs.gc[i].claimedRole < 6){
                    //有职业则进入
                    if(gs.gc[i].claimedRole <= 3){
                        claimedRoleName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder).append(".png");
                    }
                    else{
                        claimedRoleName.append(gs.gc[i].claimedRole).append(".png");
                    }
                    ImageIcon claimedRoleIcon = resources.getImage(claimedRoleName.toString());
                    JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                    //职业
                    if(i <= (gs.gc.length - 1 + 1)/2)
                    {
                        claimedRoleLabel.setBounds(60+74 * i,20,claimedRoleIcon.getIconWidth(),claimedRoleIcon.getIconHeight());
                    }
                    else
                    {
                        claimedRoleLabel.setBounds(60+74 * (i  - ((gs.gc.length - 1+1)/2)),128,claimedRoleIcon.getIconWidth(),claimedRoleIcon.getIconHeight());
                    }
                    infoCoPanel.add(claimedRoleLabel);
                }//职业图标
                //标记
                ImageIcon chooseIcon = resources.getImage("frameSRed.png");
                JLabel chooseLabel = new JLabel(chooseIcon);
                frameLabels.add(chooseLabel);
                if(i <= gs.gc.length / 2)
                {
                      chooseLabel.setBounds(60+74 * i,20,chooseIcon.getIconWidth(),chooseIcon.getIconHeight());
                }
                else
                {
                    chooseLabel.setBounds(60+74 * (i  - ((gs.gc.length - 1+1)/2)),128,chooseIcon.getIconWidth(),chooseIcon.getIconHeight());
                }
                infoCoPanel.add(chooseLabel);
                chooseLabel.setVisible(false);
                //投票标记
                ImageIcon voteIcon = resources.getImage("result2_all.png");
                JLabel voteLabel = new JLabel(voteIcon);

                if(i <= gs.gc.length / 2)
                {
                    //all标记
                    voteLabel.setBounds(60+5+74 * i,20,voteIcon.getIconWidth(),voteIcon.getIconHeight());
                }
                else
                {
                    voteLabel.setBounds(60+5+74 * (i  - gs.gc.length / 2),128,voteIcon.getIconWidth(),voteIcon.getIconHeight());
                }
                infoCoPanel.add(voteLabel);
                infoCoPanel.setComponentZOrder(voteLabel,0);
                voteLabel.setVisible(false);
                if(gs.gc[i].isSelectedVoteTarget[gs.gameDay]) voteLabel.setVisible(true);

                //all标记
                ImageIcon voteAllIcon = resources.getImage("result1_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);
                //all标记
                if(i <= (gs.gc.length - 1 + 1)/2)
                {
                    voteAllLabel.setBounds(60+5+74 * i,40,voteAllIcon.getIconWidth(),voteAllIcon.getIconHeight());
                }
                else
                {
                    voteAllLabel.setBounds(60+5+74 * (i  - gs.gc.length / 2),148,voteAllIcon.getIconWidth(),voteAllIcon.getIconHeight());
                }
                infoCoPanel.add(voteAllLabel);
                resultLabels.add(voteAllLabel);
                voteAllLabel.setVisible(false);

                //占卜标记
                if (i < zhanbuNum.size() + 1)
                {
                    for (int i2 = 1; i2 < gs.gc.length; ++i2)
                    {
                        ImageIcon zbIcon = resources.getImage("result1_" + zhanbuOrder.get(i-1) + "white.png");
                        JLabel zbLabel = new JLabel(zbIcon);
                        zbLabels.add(zbLabel);
                        if (i2 <= (gs.gc.length - 1 + 1) / 2)
                        {
                            zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * i2,20 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1),
                                    zbIcon.getIconWidth(),zbIcon.getIconHeight());
                            //all标记
                        }
                        else
                        {
                            zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * (i2 - gs.gc.length  / 2),
                                    128 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1), zbIcon.getIconWidth(),zbIcon.getIconHeight());
                        }
                        infoCoPanel.add(zbLabel);
                        zbLabel.setVisible(false);
                    }
                }
                //头像
                ImageIcon characterImage = resources.getImage(imageName.toString());
                JLabel label = new JLabel(characterImage);
                targetLabels.add(label);
                if(i <= gs.gc.length / 2)
                {
                    label.setBounds(60+(characterImage.getIconWidth()+10) * i,20,characterImage.getIconWidth(), characterImage.getIconHeight());
                }
                else
                {
                    label.setBounds((60+(characterImage.getIconWidth()+10) * (i - gs.gc.length / 2)),30 + characterImage.getIconHeight(),
                            characterImage.getIconWidth(), characterImage.getIconHeight());
                }
                infoCoPanel.add(label);
            }
            for(int k = 2;k <= gs.gameDay;++k)
            {
                for(int j = 1;j < gs.gc.length;++j)
                {
                    if(skillTargetPeople[j][k] == 0)
                    {
                        continue;
                    }
                    int i1 = skillTargetPeople[j][k];
                    int zynum = claimedRolenum[j][k];
                    String name = skillTargetNames[j][k];
                    int order = skillTargetOrder[j][k];
                    if (zynum == 3) continue;//是猎人就不显示
                    if(zynum == 1&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//占卜
                    if(zynum == 2&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//灵能
                    //如果该人物是被使用技能的
                    ImageIcon skillTargetIcon = resources.getImage(name);
                    JLabel skillTargetLabel = new JLabel(skillTargetIcon);
                    //职业
                    if(i1 <= gs.gc.length / 2)
                    {
                        skillTargetLabel.setBounds(((50+74 * (i1+1))- skillTargetIcon.getIconWidth()*zynum),20 + (order - 1)*skillTargetIcon.getIconHeight(),
                                skillTargetIcon.getIconWidth(),skillTargetIcon.getIconHeight());
                    }
                    else
                    {
                        skillTargetLabel.setBounds(((50+74 * (i1 + 1 - gs.gc.length / 2)) - skillTargetIcon.getIconWidth()*zynum),
                                (128 + (order-1)*skillTargetIcon.getIconHeight()),skillTargetIcon.getIconWidth(),skillTargetIcon.getIconHeight());
                    }
                    infoCoPanel.add(skillTargetLabel);
                    infoCoPanel.setComponentZOrder(skillTargetLabel,0);
                }
            }
            infoCoPanel.setBounds(0,198,200 + boardImage.getIconWidth(),50 + boardImage.getIconHeight());

            JLabel infoBoard = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,200 + boardImage.getIconWidth(),
                    50 + boardImage.getIconHeight(),boardImage);

            ImageIcon dragIcon = resources.getImage("uranaiAll.png");
            JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,250,350,dragIcon.getIconWidth() / 2,
                    dragIcon.getIconHeight() / 2,dragIcon);

            dragBtn.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn.getParent();
                    if (parent == null || targetLabels.isEmpty()) {
                        return;
                    }
                    else{
                        // 获取拖动按钮的边界矩形
                        Rectangle btnRect = dragBtn.getBounds();
                        // 计算按钮中心点（用于方法2）
                        int btnCenterX = btnRect.x + btnRect.width / 2;
                        int btnCenterY = btnRect.y + btnRect.height / 2;

                        // 遍历所有目标 Label，判定重合
                        for (JLabel label : targetLabels) {

                            // 校验 Label 有效且与按钮同父容器
                            if (label.getParent() != parent || !label.isVisible()) {
                                continue;
                            }
                            Rectangle labelRect = label.getBounds();
                            //中心包含（精准）
                            if (labelRect.contains(btnCenterX, btnCenterY)) {
                                int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                resources.playSound("click.wav");

                                if(!askList.contains(index+1)){
                                    askList.add(index+1);
                                }

                                //显示
                                resultLabels.get(index).setVisible(true);
                                frameLabels.get(index).setVisible(true);
                                frameLabels.get(index).repaint();
                                jPanel.repaint(label.getBounds()); // 只重绘目标角色区域
                                break;
                            }

                            // 恢复手型光标
                            dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }
                }
            });
            infoCoPanel.add(dragBtn);

            dragIcon = resources.getImage("delete.png");
            JButton dragBtn_delete = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,800,350,
                    dragIcon.getIconWidth()/2,dragIcon.getIconHeight()/2,dragIcon);

            dragBtn_delete.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn_delete.getParent();
                    if (parent == null || targetLabels.isEmpty()) {
                        return;
                    }
                    else{
                        // 获取拖动按钮的边界矩形
                        Rectangle btnRect = dragBtn_delete.getBounds();
                        // 计算按钮中心点（用于方法2）
                        int btnCenterX = btnRect.x + btnRect.width / 2;
                        int btnCenterY = btnRect.y + btnRect.height / 2;

                        // 遍历所有目标 Label，判定重合
                        for (JLabel label : targetLabels) {

                            // 校验 Label 有效且与按钮同父容器
                            if (label.getParent() != parent || !label.isVisible()) {
                                continue;
                            }
                            Rectangle labelRect = label.getBounds();
                            //中心包含（精准）
                            if (labelRect.contains(btnCenterX, btnCenterY)) {
                                int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                resources.playSound("click.wav");

                                if(askList.contains(index+1)){
                                    int uu = askList.indexOf(index+1);
                                    askList.remove(uu);
                                }
                                //显示
                                for(int u = 0; u < zhanbuNum.size();++u){
                                    zbLabels.get(index + u * (gs.gc.length - 1)).setVisible(false);
                                }
                                resultLabels.get(index).setVisible(false);
                                frameLabels.get(index).setVisible(false);

                                frameLabels.get(index).repaint();
                                jPanel.repaint(label.getBounds()); // 只重绘目标角色区域

                                break;
                            }

                            // 恢复手型光标
                            dragBtn_delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }
                }
            });

            infoCoPanel.add(dragBtn_delete);
            infoCoPanel.add(infoBoard);
            jPanel.add(infoCoPanel);
            jPanel.setComponentZOrder(infoCoPanel,0);

            resizeComponents();
            jPanel.revalidate();
            jPanel.repaint();

        });
        //灵能co逻辑
        reiBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            mainLogic.askCo(Role.ling);
            if(events.isEmpty()){
                createTishi("霊能者はいないようだ");
            }else{
                gs = mainLogic.getGameStatus();
                //此时获取了新的事件，展示后进入用新的gs呈现vote
                currentScene = Scene.DIALOGUE_DAY;
                run();
            }
        });
        //猎人co逻辑
        kariBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            mainLogic.askCo(Role.lie);
            if(events.isEmpty()){
                createTishi("狩人はいないようだ");
            }else{
                gs = mainLogic.getGameStatus();
                //此时获取了新的事件，展示后进入用新的gs呈现vote
                currentScene = Scene.DIALOGUE_DAY;
                run();
            }
        });
        //占卜co逻辑
        uranaiBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            mainLogic.askCo(Role.zhan);
            if(events.isEmpty()){
                createTishi("占い師はいないようだ");
            }else{
                gs = mainLogic.getGameStatus();
                //此时获取了新的事件，展示后进入用新的gs呈现vote
                currentScene = Scene.DIALOGUE_DAY;
                run();
            }

        });
        //共有co逻辑
        kyouyuBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            mainLogic.askCo(Role.gong);
            if(events.isEmpty()){
                createTishi("共有はいないようだ");
            }else{
                gs = mainLogic.getGameStatus();
                //此时获取了新的事件，展示后进入用新的gs呈现vote
                currentScene = Scene.DIALOGUE_DAY;
                run();
            }
        });
        //猫co逻辑
        catBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            mainLogic.askCo(Role.mao);
            if(events.isEmpty()){
                createTishi("猫又はいないようだ");
            }else{
                gs = mainLogic.getGameStatus();
                //此时获取了新的事件，展示后进入用新的gs呈现vote
                currentScene = Scene.DIALOGUE_DAY;
                run();
            }
        });

        //指定投票
        fixedVoteBtn.addActionListener(e -> {
            infoPanel.setVisible(true);
            resources.playSound("click.wav");
            //点了后文本消失，用于放人物和按钮，其他不变
            infoText.setVisible(false);
            fixedVoteBtn.setVisible(false);
            fixedUranaiBtn.setVisible(false);
            protectBtn.setVisible(false);
            returnBtn.setVisible(true);

            infoPanel.removeAll();
            List<JLabel> targetLabels = new ArrayList<>();
            List<JLabel> frameLabels = new ArrayList<>();
            List<JLabel> resultLabels = new ArrayList<>();
            for (int i = 1; i <= gs.gc.length - 1; i++) {


                //头像命名规则01s.png 01gs.png
                StringBuilder imageName = new StringBuilder();
                if(gs.gc[i].number <=9)imageName.append("0");
                imageName.append(gs.gc[i].number);
                switch(gs.gc[i].whyDie){
                    case NONE:
                        break;
                    default:
                        imageName.append("g");
                        break;
                }
                imageName.append("s.png");
                StringBuilder claimedRoleName = new StringBuilder("yaku");
                if(gs.gc[i].claimedRole > 0 && gs.gc[i].claimedRole < 6){
                    //有职业则进入
                    if(gs.gc[i].claimedRole <= 3){
                        claimedRoleName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder).append(".png");
                    }
                    else{
                        claimedRoleName.append(gs.gc[i].claimedRole).append(".png");
                    }
                    ImageIcon claimedRoleIcon = resources.getImage(claimedRoleName.toString());
                    JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                    //职业
                    if(i <= gs.gc.length / 2)
                    {
                        claimedRoleLabel.setBounds(60 + 74 * i ,20, claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                    }
                    else
                    {
                        claimedRoleLabel.setBounds(60+74 * (i  - gs.gc.length / 2),128, claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                    }
                    infoPanel.add(claimedRoleLabel);
                }//职业图标

                //标记 需要记录下来，然后下次进入的时候如果i是标记过的直接设置成true
                ImageIcon chooseIcon = resources.getImage("frameSBlue.png");
                JLabel chooseLabel = new JLabel(chooseIcon);
                frameLabels.add(chooseLabel);
                //职业
                if(i <= (gs.gc.length - 1 + 1)/2)
                {
                   chooseLabel.setBounds(60+74 * i,20,chooseIcon.getIconWidth(), chooseIcon.getIconHeight());
                }
                else
                {
                    chooseLabel.setBounds(60+74 * (i  - gs.gc.length / 2), 128, chooseIcon.getIconWidth(), chooseIcon.getIconHeight());
                }
                infoPanel.add(chooseLabel);
                chooseLabel.setVisible(false);
                if(voteChosen.contains(i))
                {
                    //如果有i，则显示
                    chooseLabel.setVisible(true);
                }
                //all标记
                ImageIcon voteAllIcon = resources.getImage("result2_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);
                //all标记
                if(i <= gs.gc.length / 2)
                {
                     voteAllLabel.setBounds(60+5+74 * i,30,voteAllIcon.getIconWidth(),voteAllIcon.getIconHeight());
                }
                else
                {
                    voteAllLabel.setBounds(60+5+74 * (i  - gs.gc.length / 2),138,voteAllIcon.getIconWidth(),voteAllIcon.getIconHeight());
                }
                infoPanel.add(voteAllLabel);
                infoPanel.setComponentZOrder(voteAllLabel,0);
                resultLabels.add(voteAllLabel);
                voteAllLabel.setVisible(false);
                if(voteChosen.contains(i)){
                    //如果有i，则显示
                    voteAllLabel.setVisible(true);
                }

                //头像
                ImageIcon characterImage = resources.getImage(imageName.toString());
                JLabel label = new JLabel(characterImage);
                targetLabels.add(label);
                if(i <= (gs.gc.length - 1 + 1)/2)
                {
                    label.setBounds(60+(characterImage.getIconWidth()+10) * i,20,characterImage.getIconWidth(),characterImage.getIconHeight());
                }
                else
                {
                    label.setBounds(60+(characterImage.getIconWidth()+10) * (i - gs.gc.length / 2),
                            30+characterImage.getIconHeight(),characterImage.getIconWidth(),characterImage.getIconHeight());
                }
                infoPanel.add(label);
            }
            for(int k = 2;k <= gs.gameDay;++k)
            {
                for(int j = 1;j < gs.gc.length;++j)
                {
                    if(skillTargetPeople[j][k] == 0)
                    {
                        continue;
                    }
                    int i1 = skillTargetPeople[j][k];
                    int zynum = claimedRolenum[j][k];
                    String name = skillTargetNames[j][k];
                    int order = skillTargetOrder[j][k];
                    if (zynum == 3) continue;//是猎人就不显示
                    if(zynum == 1&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//占卜
                    if(zynum == 2&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//灵能
                    //如果该人物是被使用技能的
                    ImageIcon skillTargetIcon = resources.getImage(name);
                    JLabel skillTargetLabel = new JLabel(skillTargetIcon);
                    //职业
                    if(i1 <= (gs.gc.length - 1 + 1)/2)
                    {
                      skillTargetLabel.setBounds((50+74 * (i1+1))- skillTargetIcon.getIconWidth()*zynum,(20 + (order - 1)*skillTargetIcon.getIconHeight()),
                              skillTargetIcon.getIconWidth(),skillTargetIcon.getIconHeight());
                    }
                    else
                    {
                        skillTargetLabel.setBounds((50+74 * (i1 + 1 - gs.gc.length / 2)) - skillTargetIcon.getIconWidth()*zynum,
                                128 + (order-1)*skillTargetIcon.getIconHeight(),
                                skillTargetIcon.getIconWidth(),skillTargetIcon.getIconHeight());
                    }
                    infoPanel.add(skillTargetLabel);
                    infoPanel.setComponentZOrder(skillTargetLabel,0);
                }
            }

            infoPanel.setBounds(0,198,200 + boardImage.getIconWidth(),50 + boardImage.getIconHeight());

            JLabel infoBoard = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,200 + boardImage.getIconWidth(),50 + boardImage.getIconHeight()
            ,boardImage);

            ImageIcon dragIcon = resources.getImage("touhyou.png");
            JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,250,350,dragIcon.getIconWidth() / 2,
                    dragIcon.getIconHeight() / 2,dragIcon);

            dragBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn.getParent();
                    if (parent == null || targetLabels.isEmpty()) {
                        return;
                    }
                    else{
                        // 获取拖动按钮的边界矩形
                        Rectangle btnRect = dragBtn.getBounds();
                        // 计算按钮中心点（用于方法2）
                        int btnCenterX = btnRect.x + btnRect.width / 2;
                        int btnCenterY = btnRect.y + btnRect.height / 2;

                        // 遍历所有目标 Label，判定重合
                        for (JLabel label : targetLabels) {

                            // 校验 Label 有效且与按钮同父容器
                            if (label.getParent() != parent || !label.isVisible()) {
                                continue;
                            }
                            Rectangle labelRect = label.getBounds();
                            //中心包含（精准）
                            if (labelRect.contains(btnCenterX, btnCenterY)) {
                                int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                resources.playSound("click.wav");

                                gs.gc[index+1].isSelectedVoteTarget[gs.gameDay] = true;//被指定
                                if (!voteChosen.contains(index + 1)) {
                                    voteChosen.add(index + 1);
                                }
                                for(int y = 1;y < gs.gc.length;++y){
                                    if(gs.gc[y].isSelectedVoteTarget[gs.gameDay]){
                                        isVote[0] = true;
                                        break;
                                    }
                                    isVote[0] = false;
                                }
                                //显示
                                resultLabels.get(index).setVisible(true);
                                frameLabels.get(index).setVisible(true);
                                frameLabels.get(index).repaint();
                                jPanel.repaint(label.getBounds()); // 只重绘目标角色区域
                                break;
                            }

                            // 恢复手型光标
                            dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                        if(isTest) {
                            for (int a = 1; a < gs.gc.length; ++a) {
                                if (gs.gc[a].isSelectedVoteTarget[gs.gameDay]) System.out.println(uiComponentFactory.getJobText(gs.gc[a].number));
                            }
                        }
                    }
                }
            });
            infoPanel.add(dragBtn);

            dragIcon = resources.getImage("delete.png");
            JButton dragBtn_delete = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,500,350,dragIcon.getIconWidth() / 2,
                    dragIcon.getIconHeight() / 2,dragIcon);

            dragBtn_delete.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn_delete.getParent();
                    if (parent == null || targetLabels.isEmpty()) {
                        return;
                    }
                    else{
                        // 获取拖动按钮的边界矩形
                        Rectangle btnRect = dragBtn_delete.getBounds();
                        // 计算按钮中心点（用于方法2）
                        int btnCenterX = btnRect.x + btnRect.width / 2;
                        int btnCenterY = btnRect.y + btnRect.height / 2;

                        // 遍历所有目标 Label，判定重合
                        for (JLabel label : targetLabels) {

                            // 校验 Label 有效且与按钮同父容器
                            if (label.getParent() != parent || !label.isVisible()) {
                                continue;
                            }
                            Rectangle labelRect = label.getBounds();
                            //中心包含（精准）
                            if (labelRect.contains(btnCenterX, btnCenterY)) {
                                int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                resources.playSound("click.wav");


                                gs.gc[index+1].isSelectedVoteTarget[gs.gameDay] = false;//被指定
                                if(voteChosen.contains(index+1)){
                                    int h = voteChosen.indexOf(index+1);
                                    voteChosen.remove(h);
                                }
                                for(int y = 1;y < gs.gc.length;++y){
                                    if(gs.gc[y].isSelectedVoteTarget[gs.gameDay]){
                                        isVote[0] = true;
                                        break;
                                    }
                                    isVote[0] = false;
                                }
                                //显示
                                resultLabels.get(index).setVisible(false);
                                frameLabels.get(index).setVisible(false);

                                resultLabels.get(index).repaint();
                                frameLabels.get(index).repaint();
                                jPanel.repaint(label.getBounds()); // 只重绘目标角色区域

                                break;
                            }

                            // 恢复手型光标
                            dragBtn_delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                        if(isTest) {
                            for (int a = 1; a < gs.gc.length; ++a) {
                                if (gs.gc[a].isSelectedVoteTarget[gs.gameDay]) System.out.println(uiComponentFactory.getJobText(gs.gc[a].number));
                            }
                        }
                    }
                }
            });

            infoPanel.add(dragBtn_delete);
            infoPanel.add(infoBoard);
            jPanel.add(infoPanel);
            jPanel.setComponentZOrder(infoPanel,0);

            resizeComponents();
            jPanel.revalidate();
            jPanel.repaint();

        });
        //指定占卜
        fixedUranaiBtn.addActionListener(e -> {
            infoZhanPanel.setVisible(true);
            resources.playSound("click.wav");
            //点了后文本消失，用于放人物和按钮，其他不变
            infoText.setVisible(false);
            fixedVoteBtn.setVisible(false);
            fixedUranaiBtn.setVisible(false);
            protectBtn.setVisible(false);
            returnBtn.setVisible(true);
            List<Integer> trueNum = new ArrayList<>();
            List<Integer> zhanbuNum = new ArrayList<>();
            List<Integer> zhanbuOrder = new ArrayList<>();
            for (int i = 1; i <= gs.gc.length - 1; i++) {
                if(gs.gc[i].claimedRole == 1&&gs.gc[i].whyDie == whyDie.NONE){
                    //不死且是占卜
                    zhanbuNum.add(i);//获取占卜的编号
                    zhanbuOrder.add(gs.gc[i].claimedRoleorder);//获取占卜的职业顺序
                }
                if((gs.gc[i].actualRole == 1||gs.gc[i].claimedRole == 1)&&gs.gc[i].whyDie == whyDie.NONE){
                    //不死且是占卜
                    trueNum.add(i);//包含潜伏

                }
            }
            List<JLabel> targetLabels = new ArrayList<>();
            List<JLabel> frameLabels = new ArrayList<>();
            List<JLabel> resultLabels = new ArrayList<>();
            List<JLabel> zbLabels = new ArrayList<>();
            for (int i = 1; i <= gs.gc.length - 1; i++) {
                //头像命名规则01s.png 01gs.png
                StringBuilder imageName = new StringBuilder();
                if(gs.gc[i].number <=9)imageName.append("0");
                imageName.append(gs.gc[i].number);
                switch(gs.gc[i].whyDie){
                    case NONE:
                        break;
                    default:
                        imageName.append("g");
                        break;
                }
                imageName.append("s.png");
                StringBuilder claimedRoleName = new StringBuilder("yaku");
                if(gs.gc[i].claimedRole > 0 && gs.gc[i].claimedRole < 6){
                    //有职业则进入
                    if(gs.gc[i].claimedRole <= 3){
                        claimedRoleName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder).append(".png");
                    }
                    else{
                        claimedRoleName.append(gs.gc[i].claimedRole).append(".png");
                    }
                    ImageIcon claimedRoleIcon = resources.getImage(claimedRoleName.toString());
                    JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                    //职业
                    if(i <= (gs.gc.length - 1 + 1)/2)
                    {
                       claimedRoleLabel.setBounds(60+74 * i, 20, claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                    }
                    else
                    {
                        claimedRoleLabel.setBounds(60+74 * (i  - gs.gc.length / 2),128,
                                claimedRoleIcon.getIconWidth(),claimedRoleIcon.getIconHeight());
                    }
                    infoZhanPanel.add(claimedRoleLabel);
                }//职业图标

                //标记
                ImageIcon chooseIcon = resources.getImage("frameSRed.png");
                JLabel chooseLabel = new JLabel(chooseIcon);
                frameLabels.add(chooseLabel);
                //职业
                if(i <= gs.gc.length / 2)
                {
                    chooseLabel.setBounds(60+74 * i, 20, chooseIcon.getIconWidth(), chooseIcon.getIconHeight());
                }
                else
                {
                    chooseLabel.setBounds(60+74 * (i  - gs.gc.length / 2), 128, chooseIcon.getIconWidth(), chooseIcon.getIconHeight());
                }
                infoZhanPanel.add(chooseLabel);
                chooseLabel.setVisible(false);
                if(zhanChosen.contains(i))
                {
                    //如果有i，则显示
                    chooseLabel.setVisible(true);
                }
                //投票标记
                ImageIcon voteIcon = resources.getImage("result2_all.png");
                JLabel voteLabel = new JLabel(voteIcon);
                //all标记
                if(i <= gs.gc.length / 2)
                {
                    voteLabel.setBounds(60+5+74 * i, 20, voteIcon.getIconWidth(), voteIcon.getIconHeight());
                }
                else
                {
                    voteLabel.setBounds(60+5+74 * (i  - gs.gc.length / 2), 128, voteIcon.getIconWidth(), voteIcon.getIconHeight());
                }
                infoZhanPanel.add(voteLabel);
                infoZhanPanel.setComponentZOrder(voteLabel,0);
                voteLabel.setVisible(false);
                if(gs.gc[i].isSelectedVoteTarget[gs.gameDay]) voteLabel.setVisible(true);
                //all标记
                ImageIcon voteAllIcon = resources.getImage("result1_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);
                resultLabels.add(voteAllLabel);
                //all标记
                if(i <= gs.gc.length / 2)
                {
                   voteAllLabel.setBounds(60 + 5 + 74 * i, 40, voteAllIcon.getIconWidth(), voteAllIcon.getIconHeight());
                }
                else
                {
                    voteAllLabel.setBounds(60 + 5 + 74 * (i - gs.gc.length / 2), 148, voteAllIcon.getIconWidth(), voteAllIcon.getIconHeight());
                }
                infoZhanPanel.add(voteAllLabel);
                voteAllLabel.setVisible(false);
                if(zhanChosen.contains(i))
                {
                    //如果有i，则显示
                    voteAllLabel.setVisible(true);
                }
                //占卜标记
                if(i < zhanbuNum.size()+1)
                {
                    for (int i2 = 1; i2 < gs.gc.length; ++i2)
                    {
                        ImageIcon zbIcon = resources.getImage("result1_" + zhanbuOrder.get(i-1) + "white.png");
                        JLabel zbLabel = new JLabel(zbIcon);
                        zbLabels.add(zbLabel);
                        //all标记
                        if (i2 <= (gs.gc.length - 1 + 1) / 2)
                        {
                            zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * i2, 20 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1),
                                    zbIcon.getIconWidth(), zbIcon.getIconHeight());
                        }
                        else
                        {
                            zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * (i2 - gs.gc.length  / 2),
                                    128 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1),
                                    zbIcon.getIconWidth(), zbIcon.getIconHeight());
                        }
                        infoZhanPanel.add(zbLabel);
                        zbLabel.setVisible(false);
                        if(zhanChosen.contains(i))
                        {
                            //如果有i，则显示
                            zbLabel.setVisible(true);
                        }
                    }
                }
                    //头像
                    ImageIcon characterImage = resources.getImage(imageName.toString());
                    JLabel label = new JLabel(characterImage);
                    targetLabels.add(label);
                    if(i <= gs.gc.length / 2)
                    {
                        label.setBounds(60+(characterImage.getIconWidth()+10) * i,20,characterImage.getIconWidth(),characterImage.getIconHeight());
                    }
                    else
                    {
                        label.setBounds(60+(characterImage.getIconWidth()+10) * (i - gs.gc.length / 2), 30+characterImage.getIconHeight(),
                                characterImage.getIconWidth(),characterImage.getIconHeight());
                    }
                    infoZhanPanel.add(label);
            }
            for(int k = 2;k <= gs.gameDay;++k)
            {
                for(int j = 1;j < gs.gc.length;++j)
                {
                    if(skillTargetPeople[j][k] == 0)
                    {
                        continue;
                    }
                    int i1 = skillTargetPeople[j][k];
                    int zynum = claimedRolenum[j][k];
                    String name = skillTargetNames[j][k];
                    int order = skillTargetOrder[j][k];
                    if (zynum == 3) continue;//是猎人就不显示
                    if(zynum == 1&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//占卜
                    if(zynum == 2&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//灵能
                    //如果该人物是被使用技能的
                    ImageIcon skillTargetIcon = resources.getImage(name);
                    JLabel skillTargetLabel = new JLabel(skillTargetIcon);
                    //职业
                    if(i1 <= gs.gc.length / 2)
                    {
                       skillTargetLabel.setBounds((50+74 * (i1+1))- skillTargetIcon.getIconWidth()*zynum, 20 + (order - 1)*skillTargetIcon.getIconHeight(),
                               skillTargetIcon.getIconWidth(), skillTargetIcon.getIconHeight());
                    }
                    else
                    {
                        skillTargetLabel.setBounds((50+74 * (i1 +1 - gs.gc.length / 2)) - skillTargetIcon.getIconWidth()*zynum,
                                128 + (order-1)*skillTargetIcon.getIconHeight(),skillTargetIcon.getIconWidth(),skillTargetIcon.getIconHeight());
                    }
                    infoZhanPanel.add(skillTargetLabel);
                    infoZhanPanel.setComponentZOrder(skillTargetLabel,0);
                }
            }
            infoZhanPanel.setBounds(0,198,200 + boardImage.getIconWidth(),50+boardImage.getIconHeight());
            JLabel infoBoard = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 0, 0, 200 + boardImage.getIconWidth(),
                    50 + boardImage.getIconHeight(),boardImage);

            ImageIcon dragIcon = resources.getImage("uranaiAll.png");

            JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,150,350,dragIcon.getIconWidth() / 2,
                    dragIcon.getIconHeight() / 2,dragIcon);

            dragBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn.getParent();
                    if (parent == null || targetLabels.isEmpty()) {
                        return;
                    }
                    else{
                        // 获取拖动按钮的边界矩形
                        Rectangle btnRect = dragBtn.getBounds();
                        // 计算按钮中心点（用于方法2）
                        int btnCenterX = btnRect.x + btnRect.width / 2;
                        int btnCenterY = btnRect.y + btnRect.height / 2;

                        // 遍历所有目标 Label，判定重合
                        for (JLabel label : targetLabels) {

                            // 校验 Label 有效且与按钮同父容器
                            if (label.getParent() != parent || !label.isVisible()) {
                                continue;
                            }
                            Rectangle labelRect = label.getBounds();
                            //中心包含（精准）
                            if (labelRect.contains(btnCenterX, btnCenterY)) {
                                int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                resources.playSound("click.wav");

                                gs.hiddenSeerScheduledSkillTargets[index+1][gs.gameDay] = true;//潜伏占true
                                int a = 0;
                                while(a < trueNum.size()){
                                    gs.gc[trueNum.get(a)].claimedRoleScheduledSkillTargets[index+1][gs.gameDay] = true;//其他占也是true
                                    a++;
                                }
                                isZhan[0] = true;
                                if(!zhanChosen.contains(index+1)) {
                                    zhanChosen.add(index + 1);
                                }
                                if(isTest) {
                                    for (int hh = 1; hh < gs.gc.length; ++hh) {
                                        for (int t = 1; t < gs.gc.length; ++t) {
                                            if (gs.gc[hh].claimedRoleScheduledSkillTargets[t][gs.gameDay]) {
                                                System.out.println(uiComponentFactory.getJobText(gs.gc[hh].number) + "预告了" + uiComponentFactory.getJobText(gs.gc[t].number));
                                            }
                                        }
                                    }
                                }
                                resultLabels.get(index).setVisible(true);
                                frameLabels.get(index).setVisible(true);
                                frameLabels.get(index).repaint();
                                jPanel.repaint(label.getBounds()); // 只重绘目标角色区域
                                break;
                            }

                            // 恢复手型光标
                            dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }
                }
            });
            infoZhanPanel.add(dragBtn);


            final int[] arr = {0};

            while(arr[0] < zhanbuNum.size()){
                int cur = arr[0];
                if(isTest) {
                    System.out.println(cur);
                    System.out.println(zhanbuNum.size());
                }
                ImageIcon Icon1 = resources.getImage("uranai" + zhanbuOrder.get(cur)+".png");
                JButton Btn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,zhanbuOrder.get(cur)*100+150, 350,
                        Icon1.getIconWidth() / 2, Icon1.getIconHeight() / 2, Icon1);

                Btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        Container parent = Btn.getParent();
                        if (parent == null || targetLabels.isEmpty()) {
                            return;
                        }
                        else{
                            // 获取拖动按钮的边界矩形
                            Rectangle btnRect = Btn.getBounds();
                            // 计算按钮中心点（用于方法2）
                            int btnCenterX = btnRect.x + btnRect.width / 2;
                            int btnCenterY = btnRect.y + btnRect.height / 2;

                            // 遍历所有目标 Label，判定重合
                            for (JLabel label : targetLabels) {

                                // 校验 Label 有效且与按钮同父容器
                                if (label.getParent() != parent || !label.isVisible()) {
                                    continue;
                                }
                                Rectangle labelRect = label.getBounds();
                                //中心包含（精准）
                                if (labelRect.contains(btnCenterX, btnCenterY)) {
                                    int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                    if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                    resources.playSound("click.wav");
                                    if(!zhanChosen.contains(index+1)) {
                                        zhanChosen.add(index + 1);
                                    }
                                    gs.gc[zhanbuNum.get(cur)].claimedRoleScheduledSkillTargets[index+1][gs.gameDay] = true;//true

                                    isZhan[0] = true;
                                    if(isTest) {
                                        for (int hh = 1; hh < gs.gc.length; ++hh) {
                                            for (int t = 1; t < gs.gc.length; ++t) {
                                                if (gs.gc[hh].claimedRoleScheduledSkillTargets[t][gs.gameDay]) {
                                                    System.out.println(uiComponentFactory.getJobText(gs.gc[hh].number) + "预告了" + uiComponentFactory.getJobText(gs.gc[t].number));
                                                }
                                            }
                                        }
                                    }
                                    //显示
                                    zbLabels.get(index + cur*(gs.gc.length - 1)).setVisible(true);
                                    frameLabels.get(index).setVisible(true);
                                    frameLabels.get(index).repaint();
                                    jPanel.repaint(label.getBounds()); // 只重绘目标角色区域
                                    break;
                                }

                                // 恢复手型光标
                                dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            }
                        }
                    }
                });
                infoZhanPanel.add(Btn);

                arr[0]++;
            }
            dragIcon = resources.getImage("delete.png");
            JButton dragBtn_delete = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,800,350,
                    dragIcon.getIconWidth() / 2 ,dragIcon.getIconHeight() / 2,dragIcon);

            dragBtn_delete.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn_delete.getParent();
                    if (parent == null || targetLabels.isEmpty()) {
                        return;
                    }
                    else{
                        // 获取拖动按钮的边界矩形
                        Rectangle btnRect = dragBtn_delete.getBounds();
                        // 计算按钮中心点（用于方法2）
                        int btnCenterX = btnRect.x + btnRect.width / 2;
                        int btnCenterY = btnRect.y + btnRect.height / 2;

                        // 遍历所有目标 Label，判定重合
                        for (JLabel label : targetLabels) {

                            // 校验 Label 有效且与按钮同父容器
                            if (label.getParent() != parent || !label.isVisible()) {
                                continue;
                            }
                            Rectangle labelRect = label.getBounds();
                            //中心包含（精准）
                            if (labelRect.contains(btnCenterX, btnCenterY)) {
                                int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                resources.playSound("click.wav");

                                if(zhanChosen.contains(index+1)){
                                    int h = zhanChosen.indexOf(index+1);
                                    zhanChosen.remove(h);
                                }
                                gs.hiddenSeerScheduledSkillTargets[index+1][gs.gameDay] = false;//潜伏占false
                                int a = 0;
                                while(a < trueNum.size()){
                                    gs.gc[trueNum.get(a)].claimedRoleScheduledSkillTargets[index+1][gs.gameDay] = false;//其他占也是
                                    a++;
                                }
                                if(isTest){
                                    System.out.println(trueNum);
                                }
                                for(int b = 0;b < trueNum.size();++b) {
                                    for(int j = 1;j < gs.gc.length;++j) {
                                        if (gs.gc[trueNum.get(b)].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                                            isZhan[0] = true;
                                            if (isTest) {
                                                System.out.println("跳出一层循环");
                                            }
                                            break;
                                        }
                                        isZhan[0] = false;
                                    }
                                    if(isZhan[0]){
                                        break;
                                    }
                                }
                                for (int y = 1; y < gs.gc.length; ++y) {
                                    if (gs.hiddenSeerScheduledSkillTargets[y][gs.gameDay]) {
                                        isZhan[0] = true;
                                        break;
                                    }
                                }
                                if(isTest) {
                                    for (int hh = 1; hh < gs.gc.length; ++hh) {
                                        for (int t = 1; t < gs.gc.length; ++t) {
                                            if (gs.gc[hh].claimedRoleScheduledSkillTargets[t][gs.gameDay]) {
                                                System.out.println(uiComponentFactory.getJobText(gs.gc[hh].number) + "预告了" + uiComponentFactory.getJobText(gs.gc[t].number));
                                            }
                                        }
                                    }
                                }
                                //显示
                                for(int u = 0; u < zhanbuNum.size();++u){
                                    zbLabels.get(index + u * (gs.gc.length - 1)).setVisible(false);
                                }
                                resultLabels.get(index).setVisible(false);
                                frameLabels.get(index).setVisible(false);

                                frameLabels.get(index).repaint();
                                jPanel.repaint(label.getBounds()); // 只重绘目标角色区域

                                break;
                            }

                            // 恢复手型光标
                            dragBtn_delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }
                }
            });

            infoZhanPanel.add(dragBtn_delete);
            infoZhanPanel.add(infoBoard);
            jPanel.add(infoZhanPanel);
            jPanel.setComponentZOrder(infoZhanPanel,0);

            resizeComponents();
            jPanel.revalidate();
            jPanel.repaint();

        });
        //指定护卫
        protectBtn.addActionListener(e -> {
            infoHuPanel.setVisible(true);
            resources.playSound("click.wav");
            //点了后文本消失，用于放人物和按钮，其他不变
            infoText.setVisible(false);
            fixedVoteBtn.setVisible(false);
            fixedUranaiBtn.setVisible(false);
            protectBtn.setVisible(false);
            returnBtn.setVisible(true);

            List<Integer> zhanbuNum = new ArrayList<>();
            List<Integer> zhanbuOrder = new ArrayList<>();
            List<Integer> trueNum = new ArrayList<>();//未co的也包含
            for (int i = 1; i <= gs.gc.length - 1; i++)
            {
                if(gs.gc[i].claimedRole == 3&&gs.gc[i].whyDie == whyDie.NONE)
                {
                    //不死且是猎人
                    zhanbuNum.add(i);//获取猎人的编号
                    zhanbuOrder.add(gs.gc[i].claimedRoleorder);//获取猎人的职业顺序
                }
                if((gs.gc[i].actualRole == 3||gs.gc[i].claimedRole == 3)&&gs.gc[i].whyDie == whyDie.NONE){
                    //不死且是猎人
                    trueNum.add(i);//包含潜伏

                }
            }
            List<JLabel> targetLabels = new ArrayList<>();
            List<JLabel> frameLabels = new ArrayList<>();
            List<JLabel> resultLabels = new ArrayList<>();
            List<JLabel> zbLabels = new ArrayList<>();
            for (int i = 1; i <= gs.gc.length - 1; i++)
            {

                //头像命名规则01s.png 01gs.png
                StringBuilder imageName = new StringBuilder();
                if(gs.gc[i].number <=9)imageName.append("0");
                imageName.append(gs.gc[i].number);
                switch(gs.gc[i].whyDie){
                    case NONE:
                        break;
                    default:
                        imageName.append("g");
                        break;
                }
                imageName.append("s.png");
                StringBuilder claimedRoleName = new StringBuilder("yaku");
                if(gs.gc[i].claimedRole > 0 && gs.gc[i].claimedRole < 6)
                {
                    //有职业则进入
                    if(gs.gc[i].claimedRole <= 3)
                    {
                        claimedRoleName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder).append(".png");
                    }
                    else
                    {
                        claimedRoleName.append(gs.gc[i].claimedRole).append(".png");
                    }
                    ImageIcon claimedRoleIcon = resources.getImage(claimedRoleName.toString());
                    JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                    //职业
                    if(i <= gs.gc.length / 2)
                    {
                       claimedRoleLabel.setBounds(60+74 * i, 20, claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                    }
                    else
                    {
                        claimedRoleLabel.setBounds(60+74 * (i  - gs.gc.length / 2), 128, claimedRoleIcon.getIconWidth(), claimedRoleIcon.getIconHeight());
                    }
                    infoHuPanel.add(claimedRoleLabel);
                }//职业图标

                //标记
                ImageIcon chooseIcon = resources.getImage("frameOrange.png");
                JLabel chooseLabel =  new JLabel(chooseIcon);
                //职业
                if(i <= gs.gc.length / 2)
                {
                    chooseLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 60+74 * i, 20, 64,
                            98, chooseIcon);
                }
                else
                {
                    chooseLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label, 60+74 * (i  - gs.gc.length / 2), 128,
                            64, 98, chooseIcon);
                }
                frameLabels.add(chooseLabel);
                infoHuPanel.add(chooseLabel);
                chooseLabel.setVisible(false);

                if(huChosen.contains(i))
                {
                    //如果有i，则显示
                    chooseLabel.setVisible(true);
                }
                //投票标记
                ImageIcon voteIcon = resources.getImage("result2_all.png");
                JLabel voteLabel = new JLabel(voteIcon);
                //all标记
                if(i <= (gs.gc.length - 1 + 1)/2)
                {
                    voteLabel.setBounds(60+5+74 * i, 20, voteIcon.getIconWidth(), voteIcon.getIconHeight());
                }
                else
                {
                    voteLabel.setBounds(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)), 128, voteIcon.getIconWidth(), voteIcon.getIconHeight());
                }
                infoHuPanel.add(voteLabel);
                infoHuPanel.setComponentZOrder(voteLabel,0);
                voteLabel.setVisible(false);
                if(gs.gc[i].isSelectedVoteTarget[gs.gameDay]) voteLabel.setVisible(true);

                //all标记
                ImageIcon voteAllIcon = resources.getImage("result3_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);
                //all标记
                if(i <= (gs.gc.length - 1 + 1)/2)
                {
                   voteAllLabel.setBounds(60+5+74 * i, 40, voteAllIcon.getIconWidth(), voteAllIcon.getIconHeight());
                }
                else
                {
                   voteAllLabel.setBounds(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)), 148, voteAllIcon.getIconWidth(), voteAllIcon.getIconHeight());
                }
                resultLabels.add(voteAllLabel);
                infoHuPanel.add(voteAllLabel);
                voteAllLabel.setVisible(false);
                if(huChosen.contains(i))
                {
                    //如果有i，则显示
                    voteAllLabel.setVisible(true);
                }
                //占卜标记
                if(i < zhanbuNum.size()+1) {
                    for (int i2 = 1; i2 < gs.gc.length; ++i2) {
                        ImageIcon zbIcon = resources.getImage("result3_" + zhanbuOrder.get(i-1) + ".png");
                        JLabel zbLabel = new JLabel(zbIcon);
                        zbLabels.add(zbLabel);
                        //all标记
                        if (i2 <= (gs.gc.length - 1 + 1) / 2)
                        {
                            zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * i2, 20 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1),
                                    zbIcon.getIconWidth(),zbIcon.getIconHeight());
                        }
                        else
                        {
                            zbLabel.setBounds(60 + 5 + zbIcon.getIconWidth() + 74 * (i2 - ((gs.gc.length - 1 + 1) / 2)), 128 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1),
                                    zbIcon.getIconWidth(),zbIcon.getIconHeight());
                        }
                        infoHuPanel.add(zbLabel);
                        zbLabel.setVisible(false);
                        if(huChosen.contains(i)){
                            //如果有i，则显示
                            zbLabel.setVisible(true);
                        }
                    }
                }
                //头像
                ImageIcon characterImage = resources.getImage(imageName.toString());
                JLabel label = new JLabel(characterImage);
                targetLabels.add(label);
                if(i <= (gs.gc.length - 1 + 1)/2)
                {
                    label.setBounds(60+(characterImage.getIconWidth()+10) * i, 20, characterImage.getIconWidth(),characterImage.getIconHeight());
                }
                else
                {
                    label.setBounds(60+(characterImage.getIconWidth()+10) * (i - ((gs.gc.length - 1+1)/2)), 30+characterImage.getIconHeight(),
                            characterImage.getIconWidth(), characterImage.getIconHeight());
                }
                infoHuPanel.add(label);
            }
            for(int k = 2;k <= gs.gameDay;++k)
            {
                for(int j = 1;j < gs.gc.length;++j) {
                    if(skillTargetPeople[j][k] == 0){
                        continue;
                    }
                    int i1 = skillTargetPeople[j][k];
                    int zynum = claimedRolenum[j][k];
                    String name = skillTargetNames[j][k];
                    int order = skillTargetOrder[j][k];
                    if (zynum == 3) continue;//是猎人就不显示
                    if(zynum == 1&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//占卜
                    if(zynum == 2&&gs.gc[j].dieDay !=0 && gs.gc[j].dieDay < k) continue;//灵能
                    //如果该人物是被使用技能的
                    ImageIcon skillTargetIcon = resources.getImage(name);
                    JLabel skillTargetLabel = new JLabel(skillTargetIcon);
                    //职业
                    if(i1 <= (gs.gc.length - 1 + 1)/2)
                    {
                        skillTargetLabel.setBounds((50+74 * (i1+1))- skillTargetIcon.getIconWidth()*zynum, 20 + (order - 1)*skillTargetIcon.getIconHeight(),
                                skillTargetIcon.getIconWidth(), skillTargetIcon.getIconHeight());
                    }
                    else
                    {
                        skillTargetLabel.setBounds((50+74 * (i1 +1 - ((gs.gc.length - 1+1)/2))) - skillTargetIcon.getIconWidth()*zynum,
                                128 + (order-1)*skillTargetIcon.getIconHeight(), skillTargetIcon.getIconWidth(),skillTargetIcon.getIconHeight());
                    }
                    infoHuPanel.add(skillTargetLabel);
                    infoHuPanel.setComponentZOrder(skillTargetLabel,0);
                }
            }
            infoHuPanel.setBounds(0, 198, 200 + boardImage.getIconWidth(), 50 + boardImage.getIconHeight());

            JLabel infoBoard = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,200 + boardImage.getIconWidth(),
                    50+boardImage.getIconHeight(),boardImage);

            ImageIcon dragIcon = resources.getImage("goeiAll.png");
            JButton dragBtn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,250,350,dragIcon.getIconWidth()/2,
                    dragIcon.getIconHeight()/2,dragIcon);

            dragBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn.getParent();
                    if (parent == null || targetLabels.isEmpty()) {
                        return;
                    }
                    else{
                        // 获取拖动按钮的边界矩形
                        Rectangle btnRect = dragBtn.getBounds();
                        // 计算按钮中心点（用于方法2）
                        int btnCenterX = btnRect.x + btnRect.width / 2;
                        int btnCenterY = btnRect.y + btnRect.height / 2;

                        // 遍历所有目标 Label，判定重合
                        for (JLabel label : targetLabels) {

                            // 校验 Label 有效且与按钮同父容器
                            if (label.getParent() != parent || !label.isVisible()) {
                                continue;
                            }
                            Rectangle labelRect = label.getBounds();
                            //中心包含（精准）
                            if (labelRect.contains(btnCenterX, btnCenterY)) {
                                int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                resources.playSound("click.wav");

                                gs.hiddenHunterScheduledSkillTargets[index+1][gs.gameDay] = true;//潜伏占true
                                if(!huChosen.contains(index+1)){
                                    huChosen.add(index+1);
                                }
                                int a = 0;
                                while(a < trueNum.size()){
                                    gs.gc[trueNum.get(a)].claimedRoleScheduledSkillTargets[index+1][gs.gameDay] = true;//其他占也是true
                                    a++;
                                }
                                isHu[0] = true;
                                if(isTest) {
                                    for (int hh = 1; hh < gs.gc.length; ++hh) {
                                        for (int t = 1; t < gs.gc.length; ++t) {
                                            if (gs.gc[hh].claimedRoleScheduledSkillTargets[t][gs.gameDay]) {
                                                System.out.println(uiComponentFactory.getJobText(gs.gc[hh].number) + "预告了" + uiComponentFactory.getJobText(gs.gc[t].number));
                                            }
                                        }
                                    }
                                }
                                //显示
                                resultLabels.get(index).setVisible(true);
                                frameLabels.get(index).setVisible(true);
                                frameLabels.get(index).repaint();
                                jPanel.repaint(label.getBounds()); // 只重绘目标角色区域
                                break;
                            }
                            // 恢复手型光标
                            dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }
                }
            });
            infoHuPanel.add(dragBtn);


            final int[] arr = {0};

            while(arr[0] < zhanbuNum.size())
            {
                int cur = arr[0];
                System.out.println(cur);
                System.out.println(zhanbuNum.size());
                ImageIcon Icon1 = resources.getImage("goei" + zhanbuOrder.get(cur)+".png");
                JButton Btn = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,zhanbuOrder.get(cur)*150+250,
                        350,Icon1.getIconWidth()/2,Icon1.getIconHeight()/2,resources.getImage("goei" + zhanbuOrder.get(cur)+".png"));

                Btn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                        Container parent = Btn.getParent();
                        if (parent == null || targetLabels.isEmpty()) {
                            return;
                        }
                        else{
                            // 获取拖动按钮的边界矩形
                            Rectangle btnRect = Btn.getBounds();
                            // 计算按钮中心点（用于方法2）
                            int btnCenterX = btnRect.x + btnRect.width / 2;
                            int btnCenterY = btnRect.y + btnRect.height / 2;

                            // 遍历所有目标 Label，判定重合
                            for (JLabel label : targetLabels) {

                                // 校验 Label 有效且与按钮同父容器
                                if (label.getParent() != parent || !label.isVisible()) {
                                    continue;
                                }
                                Rectangle labelRect = label.getBounds();
                                //中心包含（精准）
                                if (labelRect.contains(btnCenterX, btnCenterY)) {
                                    int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                    if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                    resources.playSound("click.wav");

                                    gs.gc[zhanbuNum.get(cur)].claimedRoleScheduledSkillTargets[index+1][gs.gameDay] = true;//true
                                    if(!huChosen.contains(index+1)){
                                        huChosen.add(index+1);
                                    }
                                    isHu[0] = true;
                                    if(isTest) {
                                        for (int hh = 1; hh < gs.gc.length; ++hh) {
                                            for (int t = 1; t < gs.gc.length; ++t) {
                                                if (gs.gc[hh].claimedRoleScheduledSkillTargets[t][gs.gameDay]) {
                                                    System.out.println(uiComponentFactory.getJobText(gs.gc[hh].number) + "预告了" + uiComponentFactory.getJobText(gs.gc[t].number));
                                                }
                                            }
                                        }
                                    }
                                    //显示
                                    zbLabels.get(index + cur*(gs.gc.length - 1)).setVisible(true);
                                    frameLabels.get(index).setVisible(true);
                                    frameLabels.get(index).repaint();
                                    jPanel.repaint(label.getBounds()); // 只重绘目标角色区域
                                    break;
                                }

                                // 恢复手型光标
                                dragBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            }
                        }
                    }
                });
                infoHuPanel.add(Btn);

                arr[0]++;
            }
            dragIcon = resources.getImage("delete.png");
            JButton dragBtn_delete = ButtonSimpleFactory.makeButton(ButtonConst.Draggable_Button,800,350,
                    dragIcon.getIconWidth()/2,dragIcon.getIconHeight()/2,dragIcon);

            dragBtn_delete.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Container parent = dragBtn_delete.getParent();
                    if (parent == null || targetLabels.isEmpty()) {
                        return;
                    }
                    else{
                        // 获取拖动按钮的边界矩形
                        Rectangle btnRect = dragBtn_delete.getBounds();
                        // 计算按钮中心点（用于方法2）
                        int btnCenterX = btnRect.x + btnRect.width / 2;
                        int btnCenterY = btnRect.y + btnRect.height / 2;

                        // 遍历所有目标 Label，判定重合
                        for (JLabel label : targetLabels) {

                            // 校验 Label 有效且与按钮同父容器
                            if (label.getParent() != parent || !label.isVisible()) {
                                continue;
                            }
                            Rectangle labelRect = label.getBounds();
                            //中心包含（精准）
                            if (labelRect.contains(btnCenterX, btnCenterY)) {
                                int index = targetLabels.indexOf(label);//可以获取标记框和gc
                                if(gs.gc[index + 1].whyDie != whyDie.NONE) break;//死人不能选
                                resources.playSound("click.wav");

                                if(huChosen.contains(index+1)){
                                    int h = huChosen.indexOf(index+1);
                                    huChosen.remove(h);
                                }
                                gs.hiddenHunterScheduledSkillTargets[index+1][gs.gameDay] = false;//潜伏占false
                                int a = 0;
                                while(a < trueNum.size()){
                                    gs.gc[trueNum.get(a)].claimedRoleScheduledSkillTargets[index+1][gs.gameDay] = false;//其他占也是true
                                    a++;
                                }
                                for(int b = 0;b < trueNum.size();++b) {
                                    for(int j = 1;j < gs.gc.length;++j) {
                                        if (gs.gc[trueNum.get(b)].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                                            isHu[0] = true;
                                            if (isTest) {
                                                System.out.println("跳出一层循环");
                                            }
                                            break;
                                        }
                                        isHu[0] = false;
                                    }
                                    if(isHu[0]){
                                        break;
                                    }
                                }
                                for (int y = 1; y < gs.gc.length; ++y) {
                                    if (gs.hiddenHunterScheduledSkillTargets[y][gs.gameDay]) {
                                        isHu[0] = true;
                                        break;
                                    }
                                }
                                if(isTest) {
                                    for (int hh = 1; hh < gs.gc.length; ++hh) {
                                        for (int t = 1; t < gs.gc.length; ++t) {
                                            if (gs.gc[hh].claimedRoleScheduledSkillTargets[t][gs.gameDay]) {
                                                System.out.println(uiComponentFactory.getJobText(gs.gc[hh].number) + "预告了" + uiComponentFactory.getJobText(gs.gc[t].number));
                                            }
                                        }
                                    }
                                }
                                //显示
                                for(int u = 0; u < zhanbuNum.size();++u){
                                    zbLabels.get(index + u * (gs.gc.length - 1)).setVisible(false);
                                }
                                resultLabels.get(index).setVisible(false);
                                frameLabels.get(index).setVisible(false);

                                frameLabels.get(index).repaint();
                                jPanel.repaint(label.getBounds()); // 只重绘目标角色区域

                                break;
                            }

                            // 恢复手型光标
                            dragBtn_delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        }
                    }
                }
            });

            infoHuPanel.add(dragBtn_delete);
            infoHuPanel.add(infoBoard);
            jPanel.add(infoHuPanel);
            jPanel.setComponentZOrder(infoHuPanel,0);

            resizeComponents();
            jPanel.revalidate();
            jPanel.repaint();

        });



        jPanel.add(scrollPane);
        jPanel.add(board);
        jPanel.add(background);

        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }//投票场景，也是玩家操作的场景

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
        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tishiText.setVisible(false); // 隐藏组件
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start(); // 启动定时器
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
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
                extraText.append("自由投票\n");//自由投票
                break;
            case 1://灰随机
                extraText.append("グレラン：\n");
                for(int i = 0;i < gs.gc.length;++i){
                    if(greyCharas[i][gameDay] != 0){
                        extraText.append(uiComponentFactory.getJobText(gs.gc[greyCharas[i][gameDay]].number));
                    }
                }
                break;
            case 2://指定
                extraText.append("指定投票：\n");
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
        if(isTest) {
            System.out.println(leftPiao);
            System.out.println(rightPiao);
        }
        piaoText.setText(leftPiao.toString());
        piaoText.setForeground(Color.black);
        piaoText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        piaoText.setLineWrap(true);       // 自动换行
        piaoText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText.setEditable(false);
        piaoText.setOpaque(false);
        piaoText.setBackground(new Color(0,0,0,0));
        piaoText.setBorder(BorderFactory.createEmptyBorder());
        piaoText.setBounds(40,228,1000,430);
        jPanel.add(piaoText);
        jPanel.setComponentZOrder(piaoText,0);

        piaoText1.setText(rightPiao.toString());
        piaoText1.setForeground(Color.black);
        piaoText1.setFont(new Font("Takao Mincho",Font.BOLD,24));
        piaoText1.setLineWrap(true);       // 自动换行
        piaoText1.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText1.setEditable(false);
        piaoText1.setOpaque(false);
        piaoText1.setBackground(new Color(0,0,0,0));
        piaoText1.setBorder(BorderFactory.createEmptyBorder());
        piaoText1.setBounds(400,228,450,430);
        jPanel.add(piaoText1);
        jPanel.setComponentZOrder(piaoText1,0);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
    }//显示历史票型，需要传入第几轮投票，第几天的，以及处刑方式
    public void createPiao(String str,int round,boolean[] isReVote) {
        //str是最上面的字符串
        if(isTest) {
            System.out.println("******************************当前gameDay等于" + gs.gameDay + "******************************************");
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
        System.out.println(leftPiao);
        System.out.println(rightPiao);

        piaoText.setText(leftPiao.toString());
        piaoText.setForeground(Color.black);
        piaoText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        piaoText.setLineWrap(true);       // 自动换行
        piaoText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText.setEditable(false);
        piaoText.setFocusable(false);
        piaoText.setOpaque(false);
        piaoText.setBackground(new Color(0,0,0,0));
        piaoText.setBorder(BorderFactory.createEmptyBorder());
        piaoText.setBounds(40,228,900,430);
        jPanel.add(piaoText);
        jPanel.setComponentZOrder(piaoText,0);

        piaoText1.setText(rightPiao.toString());
        piaoText1.setForeground(Color.black);
        piaoText1.setFont(new Font("Takao Mincho",Font.BOLD,24));
        piaoText1.setLineWrap(true);       // 自动换行
        piaoText1.setWrapStyleWord(true);// 按单词拆分换行（避免单词截断）
        piaoText1.setEditable(false);
        piaoText1.setFocusable(false);
        piaoText1.setOpaque(false);
        piaoText1.setBackground(new Color(0,0,0,0));
        piaoText1.setBorder(BorderFactory.createEmptyBorder());
        piaoText1.setBounds(530,228,450,430);
        jPanel.add(piaoText1);
        jPanel.setComponentZOrder(piaoText1,0);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
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
        System.out.println(leftPiao);
        System.out.println(rightPiao);

        piaoText.setText(leftPiao.toString());
        piaoText.setForeground(Color.black);
        piaoText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        piaoText.setLineWrap(true);       // 自动换行
        piaoText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText.setEditable(false);
        piaoText.setOpaque(false);
        piaoText.setBackground(new Color(0,0,0,0));
        piaoText.setBorder(BorderFactory.createEmptyBorder());
        piaoText.setBounds(40,228,900,430);
        jPanel.add(piaoText);
        jPanel.setComponentZOrder(piaoText,0);

        piaoText1.setText(rightPiao.toString());
        piaoText1.setForeground(Color.black);
        piaoText1.setFont(new Font("Takao Mincho",Font.BOLD,24));
        piaoText1.setLineWrap(true);       // 自动换行
        piaoText1.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        piaoText1.setEditable(false);
        piaoText1.setOpaque(false);
        piaoText1.setBackground(new Color(0,0,0,0));
        piaoText1.setBorder(BorderFactory.createEmptyBorder());
        piaoText1.setBounds(530,228,450,430);
        jPanel.add(piaoText1);
        jPanel.setComponentZOrder(piaoText1,0);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
    }//显示怀疑度


    public void end_game(Scene scene)
    {
        Event event = events.poll();
        ImageIcon bgIcon;
        switch(scene)
        {
            case END_FOX:
                while(event.eventname != EventName.yhsl)
                {
                    event = events.poll();
                }
                resources.playBgm("失败画面.wav");
                bgIcon = resources.getImage("endFox.png");
                break;
            case END_WOLF:
                while(event.eventname != EventName.krsl && event.eventname != EventName.rlsl){
                    event = events.poll();
                }
                resources.playBgm("失败画面.wav");
                bgIcon = resources.getImage("endWolf.png");
                break;
            case END_VILLAGE:
            default:
                while(event.eventname != EventName.crsl)
                {
                    event = events.poll();
                }
                resources.playBgm("胜利画面.wav");
                bgIcon = resources.getImage("endVillage.png");
                break;
        }
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        //背景图片
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,bgIcon);

        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = PanelSimpleFactory.createSimplePanel(260,450,760,230,false,true);
        diaPanel.add(dialogPanel);     // 顶层：对话框面板
        JLabel back = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,760,230,
                backIcon);
        // 角色名称标签（添加到对话框面板）
        JLabel nameLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,20,10,1000,30,uiComponentFactory.getCharacterFullName(event.ch1));
        dialogPanel.add(nameLabel);  // 添加到对话框面板

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = TextareaSimpleFactory.createBasicTextArea(Color.WHITE);
        dialogText.setBounds(20,50,710,200);
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,0,0,760,230,null,null);
        // 文本逐字打印逻辑
        String text = resources.getEventText(event);
        final String[] fullText = {text};
        final int[] index = {0};
        Timer typeTimer = new Timer(50, e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.append(String.valueOf(fullText[0].charAt(index[0])));
                index[0]++;
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        dialogPanel.setVisible(true);
        typeTimer.start();
        //人物立绘
        ImageIcon[] CharIcon = resources.getEventImage(event);
        JLabel Chara = new JLabel();
        //此时判断本次事件是不是接连事件
        if(!linkIcon.isEmpty())
        {
            //是接连发生的事件则一左一右
            //待修改
            //展示第一个，话说完了点击再展示第二个
            Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,650,
                    720 - CharIcon[0].getIconHeight() - 30,CharIcon[0].getIconWidth(),CharIcon[0].getIconHeight()
                    ,CharIcon[0]);

            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();
            
            JLabel Chara2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,300,
                    720 - linkIcon.get(0).getIconHeight() - 30,linkIcon.get(0).getIconWidth(),
                    linkIcon.get(0).getIconHeight(),linkIcon.get(0));

            diaPanel.add(Chara2);
            diaPanel.setComponentZOrder(Chara2, 1);
            linkIcon.remove(0);
        }
        //其他
        else{

            Chara = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,CharIcon[0]);
            boolean isLinked = false;
            switch(event.eventname)
            {
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case gprz11p:
                case zcrh12:
                    isLinked = true;
                    break;
            }
            if(isLinked)
            {

                Chara.setBounds(300,720 - CharIcon[0].getIconHeight() - 30,CharIcon[0].getIconWidth(),CharIcon[0].getIconHeight());
                linkIcon.add(CharIcon[0]);
            }
            else
            {
                Chara.setBounds((1280 - CharIcon[0].getIconWidth()) / 2,720 - CharIcon[0].getIconHeight() - 30
                ,CharIcon[0].getIconWidth(),CharIcon[0].getIconHeight());
            }
            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();
        }

        // 按钮点击事件
        nextBtn.addActionListener(e -> {
            if (index[0] < fullText[0].length()) {
                dialogText.setText(fullText[0]);
                index[0] = fullText[0].length();
                typeTimer.stop();
            } else {
                dialogPanel.setVisible(false);
                switch(scene)
                {
                    case END_VILLAGE:
                        resources.playSound("村人胜利音效.wav");
                        break;
                    case END_WOLF:
                        resources.playSound("人狼胜利音效.wav");
                        break;
                    case END_FOX:
                        resources.playSound("妖狐胜利音效.wav");
                        break;
                }
                Timer timer = new Timer(3000,e1 -> {
                    currentScene = Scene.END_ANIME;
                    run();
                    ((Timer) e1.getSource()).stop();
                });
                timer.start();
            }
        });
        dialogPanel.add(nextBtn);  // 添加到对话框面板
        dialogPanel.add(dialogText);  // 添加到对话框面板
        dialogPanel.add(back);  // 添加到对话框面板

        diaPanel.add(background);          // 最底层：背景
        jPanel.add(diaPanel);

        // 强制触发一次大小调整
        jPanel.setComponentZOrder(diaPanel,0);
        resizeComponents();
        diaPanel.revalidate();
        diaPanel.repaint();
        diaPanel.setVisible(true);
        jPanel.revalidate();
        jPanel.repaint();
    }
    public void end_anime()
    {
        jPanel.removeAll();

        // 背景图片
        JLabel background = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("frame #19252.png"));
        //ImageIcon btnImage = ;
        JButton nextBtn = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1130,720 - ConstNum.RETURN_HEIGHT,
                ConstNum.RETURN_WIDTH*6/10
                ,ConstNum.RETURN_HEIGHT*6/10,resources.getImage("PVBtitile.png"));
        nextBtn.addActionListener(e ->
        {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        jPanel.add(nextBtn);

        for(int i = 1;i < gs.gc.length; i++)
        {
            StringBuilder infoText = new StringBuilder();
            infoText.append("公称職業:\n").append(uiComponentFactory.getZY(gs.gc[i].claimedRole)).append("\n真の職業:\n").append(uiComponentFactory.getZY(gs.gc[i].actualRole)).append("\n");
            if(gs.gc[i].whyDie != whyDie.NONE)
            {
                switch(gs.gc[i].whyDie)
                {
                    case beiyao:
                        infoText.append(gs.gc[i].dieDay).append("日目狼噛");
                        break;
                    case chuxing:
                        infoText.append(gs.gc[i].dieDay).append("日目処刑 ");
                        break;
                    case zhousha:
                        infoText.append(gs.gc[i].dieDay).append("日目呪殺 ");
                        break;
                    case dayhouzhui:
                    case nighthouzhui:
                        infoText.append(gs.gc[i].dieDay).append("日目後追 ");
                        break;
                    case daymaozhou:
                    case nightmaozhou:
                        infoText.append(gs.gc[i].dieDay).append("日目猫呪");
                        break;
                    default:
                        break;
                }
            }
            else
            {
                if(gs.end == 1)
                {
                    infoText.append("最終存活");
                }
                else if(gs.end == 2)
                {
                    if(gs.gc[i].actualRole < 7||gs.gc[i].actualRole > 9)
                    {
                        infoText.append("最終死亡");
                    }
                    else
                    {
                        infoText.append("最終胜利");
                    }
                }
                else if(gs.end == 3)
                {
                    if(gs.gc[i].actualRole < 10)
                    {
                        infoText.append("最終死亡");
                    }
                    else
                    {
                        infoText.append("最終胜利");
                    }
                }
            }
            System.out.println(infoText);
            JTextArea infoLabel = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK,16,infoText.toString(),false);
            StringBuilder xName = new StringBuilder();
            StringBuilder imageName = new StringBuilder();
            if(gs.gc[i].number <=9)imageName.append("0");
            imageName.append(gs.gc[i].number);
            switch(gs.gc[i].whyDie)
            {
                case NONE:
                    switch(gs.gc[i].actualRole)
                    {
                        case 5:
                            imageName.append("cs");
                            break;
                        case 10:
                            imageName.append("fs");
                            break;
                        case 11:
                            imageName.append("hs");
                            break;
                        case 7:
                            imageName.append("ws");
                            break;
                        case 8:
                        case 9:
                            imageName.append("ks");
                            break;
                        default:
                            imageName.append("s");
                            break;
                    }
                    break;
                case chuxing:
                    imageName.append("gs");
                    xName.append("turi.png");
                    break;
                case daymaozhou:
                    imageName.append("gs");
                    xName.append("noroi.png");
                    break;
                case dayhouzhui:
                    imageName.append("gs");
                    xName.append("atooi.png");
                    break;
                default:
                    imageName.append("gs");
                    xName.append("kami.png");
                    break;
            }
            imageName.append(".png");
            String textName = gs.gc[i].number + "job.png";//文本
            ImageIcon characterImage = resources.getImage(imageName.toString());
            ImageIcon characterText = resources.getImage(textName);
            if(!xName.isEmpty())
            {
                //不为空说明有死亡
                ImageIcon deathImage = resources.getImage(xName.toString());
                JLabel deathLabel;
                if(i <= (gs.gc.length - 1 + 1)/2)
                {
                    //死亡叉叉
                    deathLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            22+(characterImage.getIconWidth()+40) * i,110,deathImage.getIconWidth(),
                            deathImage.getIconHeight(),deathImage);
                }
                else{
                    deathLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                            22+(characterImage.getIconWidth()+40) * (i - ((gs.gc.length - 1+1)/2)),
                            260+characterImage.getIconHeight(),deathImage.getIconWidth(),deathImage.getIconHeight()
                    ,deathImage);
                }
                jPanel.add(deathLabel);
            }

            JLabel label;
            JLabel textLabel;
            if(i <= (gs.gc.length - 1 + 1)/2)
            {
                label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        20+(characterImage.getIconWidth()+40) * i,100,characterImage.getIconWidth(),
                        characterImage.getIconHeight(),characterImage
                        );
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,
                        35+(characterImage.getIconWidth()+40) * i,
                        100+characterImage.getIconHeight()-characterText.getIconHeight()/2,
                        characterText.getIconWidth() / 2,characterText.getIconHeight()/2,characterText);
                infoLabel.setBounds(20+(characterImage.getIconWidth()+40) * i,210,100,150);
            }
            else
            {
                label = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,
                        20+(characterImage.getIconWidth()+40) * (i - ((gs.gc.length - 1+1)/2)),
                        250+characterImage.getIconHeight(),characterImage.getIconWidth(),
                        characterImage.getIconHeight(),characterImage
                );
                textLabel = LabelSimpleFactory.makeLabel(LabelConst.Text_Label,
                        35+(characterImage.getIconWidth()+40) * (i - ((gs.gc.length - 1+1)/2)),
                        250+2* characterImage.getIconHeight()-characterText.getIconHeight()/2,
                        characterText.getIconWidth() / 2,characterText.getIconHeight()/2,characterText);
                infoLabel.setBounds(20+(characterImage.getIconWidth()+40) * (i - ((gs.gc.length - 1+1)/2)),
                        460,100, 150);
            }
            jPanel.add(infoLabel);
            jPanel.add(textLabel);
            jPanel.add(label);

        }
        String winIconText = "";
        String winText = "";
        switch(gs.end)
        {
            case 1:
                winIconText = "Icon1_0.png";
                winText = "村人勝利";
                break;
            case 2:
                winIconText = "Icon2.png";
                winText = "人狼勝利";
                break;
            case 3:
                winIconText = "Icon4.png";
                winText = "妖狐勝利";
                break;
        }
        JLabel winLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,50,25,40,40,resources.getImage(winIconText));
        jPanel.add(winLabel);
        
        JTextArea infoLabel = TextareaSimpleFactory.createBoldTitleTextArea(Color.BLACK,24,winText,false);
        infoLabel.setBounds(100,29,200,100);
        jPanel.add(infoLabel);

        jPanel.add(background);

        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }//结束画面

    //信息场景
    //InfoScene_A_B 其实就是指从最初的info主界面开始 点击第A个按钮进入的场景中，点击第B个按钮后进入的场景。
    public void InfoScene()
    {
        jPanel.removeAll();//清除
        //词条按钮
        ImageIcon btnNext = resources.getImage("avg_button2.png");
        JButton btn_next[] = new JButton[6];
        for(int i=1;i<=5;i++)
        {
            final int j = i;
            btn_next[i] = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,80,10+70*i,220,50,resources.getHelpText("Info" + i + ".txt"),btnNext);
            btn_next[i].setHorizontalTextPosition(SwingConstants.CENTER);
            btn_next[i].addActionListener(e -> {
                resources.playSound("click.wav");
                currentScene = Scene.valueOf("INFO_SCENE_" + j);
                run();
            });
            jPanel.add(btn_next[i]);
        }
        //整个的背景
        JLabel backgroundLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("PVBG.png"));
        //背景上的背景
        JLabel backgroundLabel_2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,10,10,980,660,resources.getImage("avg1_resized(3).png"));
        //返回主菜单按钮
        JButton btnMenu = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1050,560,ConstNum.RETURN_WIDTH,ConstNum.RETURN_HEIGHT,
                resources.getImage("PVBtitile.png"));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        jPanel.add(btnMenu);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);
        resizeComponents();//强制重置一次
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
        InfoWork();
    }
    public void InfoWork()  //信息界面的通用背景与按钮的设置
    {

    }
    public void InfoScene_Second(Scene scene)//二级目录下的信息界面
    {
        jPanel.removeAll();
        //整个的背景
        JLabel backgroundLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,ConstNum.WINDOW_HEIGHT,
                resources.getImage("PVBG.png"));
        //背景上的背景
        JLabel backgroundLabel_2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,10,10,980,660,
                resources.getImage("avg1_resized(3).png"));

        //返回主菜单按钮
        JButton btnMenu = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1050,560,ConstNum.RETURN_WIDTH,ConstNum.RETURN_HEIGHT,
                resources.getImage("PVBtitile.png"));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        JButton btnBack = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1050,400,ConstNum.RETURN_WIDTH,ConstNum.RETURN_HEIGHT,
                resources.getImage("PVBreturn.png"));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = scene.FatherScene(scene);
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = TextareaSimpleFactory.createBoldTitleTextArea(Color.WHITE,24,resources.getHelpText(scene.toString()),true);
        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        scrollPane.setBounds(50,50,880,630);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        jPanel.add(btnMenu);
        jPanel.add(btnBack);
        jPanel.add(scrollPane);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);

        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }
    public void InfoScene_First(Scene scene) //一级目录下的信息界面
    {
        jPanel.removeAll();
        //整个的背景
        JLabel backgroundLabel = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,0,0,ConstNum.WINDOW_WIDTH,
                ConstNum.WINDOW_HEIGHT,resources.getImage("PVBG.png"));
        //背景上的背景
        JLabel backgroundLabel_2 = LabelSimpleFactory.makeLabel(LabelConst.Simple_Label,10,10,980,660,
                resources.getImage("avg1_resized(3).png"));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1050,560,ConstNum.RETURN_WIDTH,ConstNum.RETURN_HEIGHT,menu);
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,1050,400,ConstNum.RETURN_WIDTH,ConstNum.RETURN_HEIGHT,back);
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE;
            run();
        });
        //词条按钮
        int subSum = scene.SubInfoSum(scene);
        ImageIcon btnNext[] = new ImageIcon[subSum + 1];
        JButton btn_next[] = new JButton[subSum + 1];
        for(int i = 1;i <= subSum;i++)
        {
            final int currentIndex = i;
            btnNext[i] = resources.getImage("avg_button2.png");  //
            btn_next[i] = ButtonSimpleFactory.makeButton(ButtonConst.Simple_Button,
                    80 ,70 * i  + 10,222, 50,
                    resources.getHelpText("Info" + scene.FirstInfoNum(scene) + "-" + i + ".txt"),btnNext[i]);
            // 核心设置：文本在图标上方，且水平居中
            btn_next[i].setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心
            btn_next[i].addActionListener(e -> {
                resources.playSound("click.wav");
                currentScene = Scene.values()[scene.ordinal() + currentIndex];    //
                run();
            });
            jPanel.add(btn_next[i]);
        }
        jPanel.add(btnMenu);
        jPanel.add(btnBack);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }
}


