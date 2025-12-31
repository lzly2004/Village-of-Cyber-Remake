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

    private enum Scene  //界面枚举类型
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
    }//定义枚举类型当前处于什么界面
    Scene currentScene;//当前是什么场景

    JFrame jFrame;//窗口
    JPanel jPanel;//容器
    JPanel diaPanel;//对话容器

    class ScalableComponent
    {
        Component component;
        double relX, relY; // 相对位置比例
        double relWidth, relHeight; // 相对大小比例
        Image originalImage; // 原始图片(用于按钮和标签的图标)

        ScalableComponent(Component comp, double x, double y, double w, double h, Image img)
        {
            component = comp;
            relX = x;
            relY = y;
            relWidth = w;
            relHeight = h;
            originalImage = img;
        }
        public Component getComponent(){
            return component;
        }
        public double getXRatio(){
            return relX;
        }
        public double getYRatio(){
            return relY;
        }
        public double getWidthRatio(){
            return relWidth;
        }
        public double getHeightRatio(){
            return relHeight;
        }
    }//保存所有需要缩放的组件及其原始属性，但是后面为了稳定禁止了窗口缩放，也就没用了，单纯作为设置组件位置和大小的工具
    List<ScalableComponent> scalableComponents = new ArrayList<>();//用于添加需要缩放的组件，现在没有用了

    //定义当前待处理的事件
    UI() {}
    static GameStatus gs;//从主逻辑类拿到的游戏状态
    ResourcesInterface resources;//资源接口
    MainLogicInterface mainLogic;//主逻辑接口

    boolean isTest = true;//测试，false时不显示测试内容
    public void init()
    {
        currentScene = Scene.START_SCENE;//初始为开始界面
        jFrame = new JFrame("Village of Cyber:Remake v1.0.3");
        jFrame.setResizable(false);
        jFrame.setSize(1280,720);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setLocationRelativeTo(null);
        ImageIcon frameIcon = new ImageIcon("vocr/resources/images/Icon2.png");
        jFrame.setIconImage(frameIcon.getImage());
        jPanel = new JPanel();
        jPanel.setSize(1280,720);
        jPanel.setLayout(null);
        diaPanel = new JPanel();
        diaPanel.setSize(1280,720);
        diaPanel.setLayout(null);
        diaPanel.setOpaque(false);
        jFrame.add(jPanel);


        // 添加窗口大小变化监听器
        jFrame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeComponents();
            }
        });

    }//初始化
    private void resizeComponents() {
        int currentWidth = jFrame.getWidth();
        int currentHeight = jFrame.getHeight();

        // 确保面板大小与窗口一致
        jPanel.setSize(currentWidth, currentHeight);

        for (ScalableComponent sc : scalableComponents) {
            // 计算新位置和大小
            int x = (int)(currentWidth * sc.relX);
            int y = (int)(currentHeight * sc.relY);
            int width = (int)(currentWidth * sc.relWidth);
            int height = (int)(currentHeight * sc.relHeight);

            // 设置组件位置和大小
            sc.component.setBounds(x, y, width, height);

            // 处理带图标的组件
            if (sc.originalImage != null) {
                Image scaledImage = sc.originalImage.getScaledInstance(
                        width, height, Image.SCALE_SMOOTH);

                if (sc.component instanceof JLabel) {
                    ((JLabel)sc.component).setIcon(new ImageIcon(scaledImage));
                } else if (sc.component instanceof JButton) {
                    ((JButton)sc.component).setIcon(new ImageIcon(scaledImage));
                }
            }
        }

        jPanel.revalidate();
        jPanel.repaint();
    } //调整所有组件大小和位置
    public void run()
    {
        resources = Game.getInstance().getResources();
        mainLogic = Game.getInstance().getMainLogic();
        switch (currentScene){
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
                end_village();
                break;
            case END_WOLF:
                end_wolf();
                break;
            case END_FOX:
                end_fox();
                break;
            case END_ANIME:
                end_anime();
                break;
            case INFO_SCENE:
                InfoScene();
                break;
            case INFO_SCENE_1:
                InfoScene_1();
                break;
            case INFO_SCENE_1_1:
                InfoScene_1_1();
                break;
            case INFO_SCENE_1_2:
                InfoScene_1_2();
                break;
            case INFO_SCENE_1_3:
                InfoScene_1_3();
                break;
            case INFO_SCENE_1_4:
                InfoScene_1_4();
                break;
            case INFO_SCENE_1_5:
                InfoScene_1_5();
                break;
            case INFO_SCENE_1_6:
                InfoScene_1_6();
                break;
            case INFO_SCENE_1_7:
                InfoScene_1_7();
                break;
            case INFO_SCENE_2:
                InfoScene_2();
                break;
            case INFO_SCENE_2_1:
                InfoScene_2_1();
                break;
            case INFO_SCENE_2_2:
                InfoScene_2_2();
                break;
            case INFO_SCENE_2_3:
                InfoScene_2_3();
                break;
            case INFO_SCENE_2_4:
                InfoScene_2_4();
                break;
            case INFO_SCENE_2_5:
                InfoScene_2_5();
                break;
            case INFO_SCENE_2_6:
                InfoScene_2_6();
                break;
            case INFO_SCENE_3:
                InfoScene_3();
                break;
            case INFO_SCENE_3_1:
                InfoScene_3_1();
                break;
            case INFO_SCENE_3_2:
                InfoScene_3_2();
                break;
            case INFO_SCENE_3_3:
                InfoScene_3_3();
                break;
            case INFO_SCENE_3_4:
                InfoScene_3_4();
                break;
            case INFO_SCENE_3_5:
                InfoScene_3_5();
                break;
            case INFO_SCENE_3_6:
                InfoScene_3_6();
                break;
            case INFO_SCENE_4:
                InfoScene_4();
                break;
            case INFO_SCENE_4_1:
                InfoScene_4_1();
                break;
            case INFO_SCENE_4_2:
                InfoScene_4_2();
                break;
            case INFO_SCENE_4_3:
                InfoScene_4_3();
                break;
            case INFO_SCENE_4_4:
                InfoScene_4_4();
                break;
            case INFO_SCENE_4_5:
                InfoScene_4_5();
                break;
            case INFO_SCENE_4_6:
                InfoScene_4_6();
                break;
            case INFO_SCENE_4_7:
                InfoScene_4_7();
                break;
            case INFO_SCENE_4_8:
                InfoScene_4_8();
                break;
            case INFO_SCENE_5:
                InfoScene_5();
                break;
            case INFO_SCENE_5_1:
                InfoScene_5_1();
                break;
            case INFO_SCENE_5_2:
                InfoScene_5_2();
                break;
            case INFO_SCENE_5_3:
                InfoScene_5_3();
                break;
            case INFO_SCENE_5_4:
                InfoScene_5_4();
                break;
            case INFO_SCENE_5_5:
                InfoScene_5_5();
                break;
            case INFO_SCENE_5_6:
                InfoScene_5_6();
                break;
            case INFO_SCENE_5_7:
                InfoScene_5_7();
                break;
            case INFO_SCENE_5_8:
                InfoScene_5_8();
                break;
            case INFO_SCENE_5_9:
                InfoScene_5_9();
                break;
        }
    }//运行，每次run都会到一个场景

    public static LinkedList<Event> events = new LinkedList<>();//作为事件队列

    public void addEvent(Event event) {
        events.add(event);
        if(isTest && event != null && !events.isEmpty())System.out.println("事件添加成功且不为空");

    }//添加event
    public void btnSet (JButton btn){
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setForeground(Color.white);
        btn.setFont(new Font("Takao Mincho",Font.BOLD,20));
        btn.setFocusPainted(false);
    }//按钮快捷设置
    public void labelSet (JLabel label){
        label.setOpaque(false);
        label.setForeground(Color.BLACK);
        label.setFont(new Font("Takao Mincho",Font.BOLD,50));
    }//标签快捷设置
    public void testBtn(){
        //测试用按钮，显示信息
        JButton test = new JButton("点我进入");

        scalableComponents.add(new ScalableComponent(test,0,0,60.0/1280,30.0/720,null));
        test.addActionListener(e -> {
            for(int i = 1;i < gs.gc.length;++i){
                System.out.println("编号"+i+" "+getJobText(gs.gc[i].number) + " 真实职业："+getZY(gs.gc[i].actualRole)+" 声称职业："+getZY(gs.gc[i].claimedRole)
                +" 死亡日期"+gs.gc[i].dieDay + " 死亡原因" + getwhyDie(gs.gc[i].whyDie) + " 怀疑度：");
                for(int j = 1;j < gs.gc.length;++j){
                    System.out.print(getJobText(gs.gc[j].number)+" 为"+gs.gc[i].suspicionValue[j]+" ");
                }
                System.out.println();

                System.out.print(getJobText(gs.gc[i].number) + " 怀疑前三为" + gs.gc[i].top3SuspectedPlayers[1][gs.gameDay] + " "+ gs.gc[i].top3SuspectedPlayers[2][gs.gameDay] + " "+ gs.gc[i].top3SuspectedPlayers[3][gs.gameDay] + " ");
                System.out.println();
            }
        });
        jPanel.add(test);
        jPanel.setComponentZOrder(test,0);
    }//设置测试按钮，显示信息

    public String getCharacterFullName(CharacterEnglishName englishName) {
        // 初始化默认值
        String kanjiName = CharacterKanjiName.NONE.name();
        String katakanaName = CharacterKatakanaName.NONE.name();

        // 通过switch一一匹配所有枚举的对应关系
        switch (englishName) {
            case NONE:
                kanjiName = CharacterKanjiName.NONE.name();
                katakanaName = CharacterKatakanaName.NONE.name();
                break;
            case Abel:
                kanjiName = CharacterKanjiName.青年.name();
                katakanaName = CharacterKatakanaName.アーベル.name();
                break;
            case Erich:
                kanjiName = CharacterKanjiName.研究生.name();
                katakanaName = CharacterKatakanaName.エーリッヒ.name();
                break;
            case Matthäus:
                kanjiName = CharacterKanjiName.傭兵.name();
                katakanaName = CharacterKatakanaName.マテウス.name();
                break;
            case Otfried:
                kanjiName = CharacterKanjiName.教師.name();
                katakanaName = CharacterKatakanaName.オトフリート.name();
                break;
            case Karl:
                kanjiName = CharacterKanjiName.情報通.name();
                katakanaName = CharacterKatakanaName.カルル.name();
                break;
            case Till:
                kanjiName = CharacterKanjiName.少年.name();
                katakanaName = CharacterKatakanaName.ティル.name();
                break;
            case Samuel:
                kanjiName = CharacterKanjiName.宝石商.name();
                katakanaName = CharacterKatakanaName.ザムエル.name();
                break;
            case Hans:
                kanjiName = CharacterKanjiName.旅人.name();
                katakanaName = CharacterKatakanaName.ハンス.name();
                break;
            case Beatrice:
                kanjiName = CharacterKanjiName.少女.name();
                katakanaName = CharacterKatakanaName.ベアトリーチェ.name();
                break;
            case Amanda:
                kanjiName = CharacterKanjiName.陶芸家.name();
                katakanaName = CharacterKatakanaName.アマンダ.name();
                break;
            case Irene:
                kanjiName = CharacterKanjiName.ランプ屋.name();
                katakanaName = CharacterKatakanaName.イレーネ.name();
                break;
            case Elsa:
                kanjiName = CharacterKanjiName.歌姫.name();
                katakanaName = CharacterKatakanaName.エルザ.name();
                break;
            case Nora:
                kanjiName = CharacterKanjiName.未亡人.name();
                katakanaName = CharacterKatakanaName.ノーラ.name();
                break;
            case Johanna:
                kanjiName = CharacterKanjiName.物識り.name();
                katakanaName = CharacterKatakanaName.ヨハナ.name();
                break;
            case Milli:
                kanjiName = CharacterKanjiName.読書家.name();
                katakanaName = CharacterKatakanaName.ミリィ.name();
                break;
            case Judith:
                kanjiName = CharacterKanjiName.召使い.name();
                katakanaName = CharacterKatakanaName.ユーディット.name();
                break;
            case Michael:
                kanjiName = CharacterKanjiName.貴族.name();
                katakanaName = CharacterKatakanaName.ミハエル.name();
                break;
            case Günther:
                kanjiName = CharacterKanjiName.自衛団長.name();
                katakanaName = CharacterKatakanaName.ギュンター.name();
                break;
            case David:
                kanjiName = CharacterKanjiName.騎士.name();
                katakanaName = CharacterKatakanaName.ダーヴィッド.name();
                break;
            case Julian:
                kanjiName = CharacterKanjiName.職人見習い.name();
                katakanaName = CharacterKatakanaName.ユリアン.name();
                break;
            case Klemens:
                kanjiName = CharacterKanjiName.神父.name();
                katakanaName = CharacterKatakanaName.クレメンス.name();
                break;
            case Heinrich:
                kanjiName = CharacterKanjiName.探偵.name();
                katakanaName = CharacterKatakanaName.ハインリヒ.name();
                break;
            case Liddi:
                kanjiName = CharacterKanjiName.学生.name();
                katakanaName = CharacterKatakanaName.リディ.name();
                break;
            case Brigitte:
                kanjiName = CharacterKanjiName.小説家.name();
                katakanaName = CharacterKatakanaName.ブリジット.name();
                break;
            case Helga:
                kanjiName = CharacterKanjiName.酒場のママ.name();
                katakanaName = CharacterKatakanaName.ヘルガ.name();
                break;
            case Natalie:
                kanjiName = CharacterKanjiName.シスター.name();
                katakanaName = CharacterKatakanaName.ナターリエ.name();
                break;
            case Volker: // 双生儿弟弟
                kanjiName = CharacterKanjiName.双生児弟.name();
                katakanaName = CharacterKatakanaName.フォルカー.name();
                break;
            case Eva: // 双生儿姐姐
                kanjiName = CharacterKanjiName.双生児姉.name();
                katakanaName = CharacterKatakanaName.エーファ.name();
                break;
            case Willy:
                kanjiName = CharacterKanjiName.流れ者.name();
                katakanaName = CharacterKatakanaName.ヴィリー.name();
                break;
            case Reichard:
                kanjiName = CharacterKanjiName.修道士.name();
                katakanaName = CharacterKatakanaName.ライヒアルト.name();
                break;
            case Hugo:
                kanjiName = CharacterKanjiName.宿屋主人.name();
                katakanaName = CharacterKatakanaName.フーゴー.name();
                break;
            case Rosa:
                kanjiName = CharacterKanjiName.踊り子.name();
                katakanaName = CharacterKatakanaName.ローザ.name();
                break;
            case Wendel:
                kanjiName = CharacterKanjiName.神学生.name();
                katakanaName = CharacterKatakanaName.ウェンデル.name();
                break;
            case Sergius:
                kanjiName = CharacterKanjiName.薬師.name();
                katakanaName = CharacterKatakanaName.ゼルギウス.name();
                break;
            case Kaja:
                kanjiName = CharacterKanjiName.煙突掃除人.name();
                katakanaName = CharacterKatakanaName.カヤ.name();
                break;
            case Betti:
                kanjiName = CharacterKanjiName.店員.name();
                katakanaName = CharacterKatakanaName.ベッティ.name();
                break;
            case Chloe:
                kanjiName = CharacterKanjiName.洗濯女.name();
                katakanaName = CharacterKatakanaName.クロエ.name();
                break;
            case Carmen:
                kanjiName = CharacterKanjiName.娼妓.name();
                katakanaName = CharacterKatakanaName.カルメン.name();
                break;
            case Renate:
                kanjiName = CharacterKanjiName.剣士.name();
                katakanaName = CharacterKatakanaName.レナーテ.name();
                break;
            case Romi:
                kanjiName = CharacterKanjiName.村娘.name();
                katakanaName = CharacterKatakanaName.ロミ.name();
                break;
            case Gerda:
                kanjiName = CharacterKanjiName.刺繍工.name();
                katakanaName = CharacterKatakanaName.ゲルダ.name();
                break;
            case Iwan:
                kanjiName = CharacterKanjiName.大工.name();
                katakanaName = CharacterKatakanaName.イヴァン.name();
                break;
            case Oktavia:
                kanjiName = CharacterKanjiName.貴婦人.name();
                katakanaName = CharacterKatakanaName.オクタヴィア.name();
                break;
            case Helmut:
                kanjiName = CharacterKanjiName.音楽家.name();
                katakanaName = CharacterKatakanaName.ヘルムート.name();
                break;
            default:
                kanjiName = "未知角色";
                katakanaName = "未知片假名";
                break;
        }

        // 核心修改：拼接为「日文汉字 片假名/英文」格式
        return String.format("%s %s/%s", kanjiName, katakanaName, englishName.name());
    }//获取人物的完整名字，输入的是event的ch1
    public String getJobText(int num) {
        switch (num) {
            case 1:
                return "青年";
            case 2:
                return "研究";
            case 3:
                return "傭兵";
            case 4:
                return "教師";
            case 5:
                return "情報";
            case 6:
                return "少年";
            case 7:
                return "宝石";
            case 8:
                return "旅人";
            case 9:
                return "少女";
            case 10:
                return "陶芸";
            case 11:
                return "洋灯";
            case 12:
                return "歌姫";
            case 13:
                return "未亡";
            case 14:
                return "物識";
            case 15:
                return "読書";
            case 16:
                return "召使";
            case 17:
                return "貴族";
            case 18:
                return "団長";
            case 19:
                return "騎士";
            case 20:
                return "職人";
            case 21:
                return "神父";
            case 22:
                return "探偵";
            case 23:
                return "学生";
            case 24:
                return "小説";
            case 25:
                return "女将";
            case 26:
                return "尼僧";
            case 27:
                return "双弟";
            case 28:
                return "双姉";
            case 29:
                return "流者";
            case 30:
                return "修道";
            case 31:
                return "宿主";
            case 32:
                return "踊子";
            case 33:
                return "神学";
            case 34:
                return "薬師";
            case 35:
                return "煙突";
            case 36:
                return "店員";
            case 37:
                return "洗濯";
            case 38:
                return "娼妓";
            case 39:
                return "剣士";
            case 40:
                return "村娘";
            case 41:
                return "刺繡";
            case 42:
                return "大工";
            case 43:
                return "婦人";
            case 44:
                return "音楽";
            default:
                return "";
        }
    }//获取名字，需要传入人物编号
    public String getZY(int i){
        String str = "";
        switch(i){
            case 1:
                str = "占い師";
                break;
            case 2:
                str = "霊能者";
                break;
            case 3:
                str = "狩人";
                break;
            case 5:
                str = "猫又";
                break;
            case 4:
                str = "共有者";
                break;
            case 10:
                str = "妖狐";
                break;
            case 11:
                str = "背徳者";
                break;
            case 7:
                str = "人狼";
                break;
            case 8:
                str = "狂人";
                break;
            case 9:
                str = "狂信者";
                break;
            case 6:
                str = "村人";
                break;
            default:
                str = "無し";
                break;
        }
        return str;
    }//获取职业文本，需要传入角色的number
    public String getwhyDie(whyDie i){
        String str = "";
        switch(i){
            case NONE :
                str = "没死";
                break;
            case nightmaozhou:
                str = "夜间猫咒";
                break;
            case daymaozhou:
                str = "白天猫咒";
                break;
            case dayhouzhui:
                str = "白天后追";
                break;
            case zhousha:
                str = "咒杀";
                break;
            case nighthouzhui:
                str = "夜间后追";
                break;
            case chuxing:
                str = "处刑";
                break;
            case beiyao:
                str = "被咬";
                break;
            default:
                break;
        }
        return str;
    }//获取角色死因，用于测试信息的显示

    String levelName = "";//当前关卡职业配置图片，用于投票履历中显示

    public void StartScene(){
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
        if(isTest){
            if(events.isEmpty()){
                System.out.println("事件成功清空");
            }
        }
        jPanel.removeAll();
        scalableComponents.clear(); // 清空之前的组件列表
        resources.playBgm("start_menu.wav");

        // 游戏标题
        ImageIcon titleIcon = resources.getImage("titleLogo.png");
        JLabel titleLabel = new JLabel(titleIcon);
        labelSet(titleLabel);
        jPanel.add(titleLabel);
        // 添加到可缩放列表(相对位置和大小)
        scalableComponents.add(new ScalableComponent(
                titleLabel, 600.0/1280, 200.0/720,
                554.0/1280, 138.0/720,
                titleIcon.getImage()
        ));

        // 按钮属性
        int x = 660;
        int y = 400;
        int width = 309;
        int height = 68;
        int x_div = 300;
        int y_div = 80;

        // 新游戏按钮
        ImageIcon startIcon = resources.getImage("startButton.png");
        JButton btnStart = new JButton(startIcon);
        btnSet(btnStart);
        btnStart.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.GAME_SCENE_SELECT;
            run();
        });
        jPanel.add(btnStart);
        scalableComponents.add(new ScalableComponent(
                btnStart, x/(double)1280, y/(double)720,
                width/(double)1280, height/(double)720,
                startIcon.getImage()
        ));

        // 继续游戏按钮
        ImageIcon continueIcon = resources.getImage("continueButton.png");
        JButton btnContinue = new JButton(continueIcon);
        btnSet(btnContinue);
        btnContinue.addActionListener(e -> resources.playSound("click.wav"));
        jPanel.add(btnContinue);
        scalableComponents.add(new ScalableComponent(
                btnContinue, (x+x_div)/(double)1280, y/(double)720,
                width/(double)1280, height/(double)720,
                continueIcon.getImage()
        ));

        // 选择存档按钮
        ImageIcon saveIcon = resources.getImage("replayButton.png");
        JButton btnSave = new JButton(saveIcon);
        btnSet(btnSave);
        btnSave.addActionListener(e -> resources.playSound("click.wav"));
        jPanel.add(btnSave);
        scalableComponents.add(new ScalableComponent(
                btnSave, x/(double)1280, (y+y_div)/(double)720,
                width/(double)1280, height/(double)720,
                saveIcon.getImage()
        ));

        // 数据统计按钮
        ImageIcon recordIcon = resources.getImage("recordButton.png");
        JButton btnRecord = new JButton(recordIcon);
        btnSet(btnRecord);
        btnRecord.addActionListener(e -> resources.playSound("click.wav"));
        jPanel.add(btnRecord);
        scalableComponents.add(new ScalableComponent(
                btnRecord, (x+x_div)/(double)1280, (y+y_div)/(double)720,
                width/(double)1280, height/(double)720,
                recordIcon.getImage()
        ));

        // 信息查看按钮
        ImageIcon infoIcon = resources.getImage("infoButton.png");
        JButton btnInfo = new JButton(infoIcon);
        btnSet(btnInfo);
        btnInfo.addActionListener(e ->{
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE;
            resources.playBgm("Info.wav");
            run();
        });
        jPanel.add(btnInfo);
        scalableComponents.add(new ScalableComponent(
                btnInfo, x/(double)1280, (y+y_div*2)/(double)720,
                width/(double)1280, height/(double)720,
                infoIcon.getImage()
        ));


        // 角色收集按钮
        ImageIcon collectionsIcon = resources.getImage("collectionsButton.png");
        JButton btnCollections = new JButton(collectionsIcon);
        btnSet(btnCollections);
        btnCollections.addActionListener(e -> resources.playSound("click.wav"));
        jPanel.add(btnCollections);
        scalableComponents.add(new ScalableComponent(
                btnCollections, (x+x_div)/(double)1280, (y+y_div*2)/(double)720,
                width/(double)1280, height/(double)720,
                collectionsIcon.getImage()
        ));

        // 背景图片（放在最后添加，确保在最底层）
        ImageIcon bgIcon = resources.getImage("title_base_resized.png");
        JLabel background = new JLabel(bgIcon);
        jPanel.add(background);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));

        resizeComponents();//强制重置一次
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }//开始界面
    public void GameScene_select(){
        jPanel.removeAll();
        scalableComponents.clear();


        //简易村
        ImageIcon village1 = resources.getImage("game1.png");
        JButton btn1 = new JButton(village1);
        btnSet(btn1);
        scalableComponents.add(new ScalableComponent(btn1,30.0/1280,20.0/720,
                village1.getIconWidth()/1280.0,village1.getIconHeight()/720.0,
                village1.getImage()));
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
        village1 = resources.getImage("game2.png");
        btn1 = new JButton(village1);
        btnSet(btn1);
        scalableComponents.add(new ScalableComponent(btn1,30.0/1280.0,(20.0+ village1.getIconHeight())/720.0,
                village1.getIconWidth()/1280.0,village1.getIconHeight()/720.0,
                village1.getImage()));
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
        village1 = resources.getImage("game3.png");
        btn1 = new JButton(village1);
        btnSet(btn1);
        scalableComponents.add(new ScalableComponent(btn1,30.0/1280.0,(20.0+ village1.getIconHeight()*2)/720.0,
                village1.getIconWidth()/1280.0,village1.getIconHeight()/720.0,
                village1.getImage()));
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
        village1 = resources.getImage("game4 #19067.png");
        btn1 = new JButton(village1);
        btnSet(btn1);
        scalableComponents.add(new ScalableComponent(btn1,30.0/1280.0,(20 + village1.getIconHeight()*3)/720.0,
                village1.getIconWidth()/1280.0,village1.getIconHeight()/720.0,
                village1.getImage()));
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
        village1 = resources.getImage("game5.png");
        btn1 = new JButton(village1);
        btnSet(btn1);
        scalableComponents.add(new ScalableComponent(btn1,(30.0+ village1.getIconWidth())/1280.0,20.0/720.0,
                village1.getIconWidth()/1280.0,village1.getIconHeight()/720.0,
                village1.getImage()));
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
        village1 = resources.getImage("game6.png");
        btn1 = new JButton(village1);
        btnSet(btn1);
        scalableComponents.add(new ScalableComponent(btn1,(30.0+ village1.getIconWidth())/1280.0,(20.0+ village1.getIconHeight())/720.0,
                village1.getIconWidth()/1280.0,village1.getIconHeight()/720.0,
                village1.getImage()));
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
        village1 = resources.getImage("game7.png");
        btn1 = new JButton(village1);
        btnSet(btn1);
        scalableComponents.add(new ScalableComponent(btn1,(30.0+ village1.getIconWidth())/1280.0,(20.0+ 2*village1.getIconHeight())/720.0,
                village1.getIconWidth()/1280.0,village1.getIconHeight()/720.0,
                village1.getImage()));
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
        ImageIcon back = resources.getImage("return.png");
        btn1 = new JButton(back);
        btnSet(btn1);
        scalableComponents.add(new ScalableComponent(btn1,(1280 - back.getIconWidth() - 30)/1280.0,(720 - 40- back.getIconHeight())/720.0,
                back.getIconWidth()/1280.0,back.getIconHeight()/720.0,
                back.getImage()));
        btn1.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        jPanel.add(btn1);

        // 背景图片（放在最后添加，确保在最底层）
        ImageIcon bgIcon = resources.getImage("title_base_resized.png");
        JLabel background = new JLabel(bgIcon);
        jPanel.add(background);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));
        // 强制触发一次大小调整
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);

    }//关卡选择界面
    public void GameScene_night(){
//        //测试event11111
//        Event event1 = new Event();
//        event1.eventname = EventName.yjsw;
//        event1.ch1 = CharacterEnglishName.Beatrice;
//        addEvent(event1);
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
        scalableComponents.clear();

        //背景
        ImageIcon back = resources.getImage("komorebi000night01.png");
        JLabel label = new JLabel(back);
        scalableComponents.add(new ScalableComponent(label,0,0,1,1, back.getImage()));
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
            public void actionPerformed(ActionEvent e) {
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
    public void GameScene_day(){

        gs = mainLogic.getGameStatus();//新一天获取新gs
        switch(gs.end){
            case 1:
                //村胜
                jPanel.removeAll();
                scalableComponents.clear();;
                jPanel.revalidate();
                jPanel.repaint();
                currentScene = Scene.END_VILLAGE;
                run();
                break;
            case 2:
                //狼胜
                jPanel.removeAll();
                scalableComponents.clear();;
                jPanel.revalidate();
                jPanel.repaint();
                currentScene = Scene.END_WOLF;
                run();
                break;
            case 3:
                //狐胜
                jPanel.removeAll();
                scalableComponents.clear();;
                jPanel.revalidate();
                jPanel.repaint();
                currentScene = Scene.END_FOX;
                run();
                break;
        }
        jPanel.removeAll();
        scalableComponents.clear();;

        // 背景图片
        ImageIcon bgIcon = resources.getImage("komorebi002.png");
        JLabel background = new JLabel(bgIcon);
        background.setOpaque(false);
        background.setFocusable(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));

        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(null);
        dialogPanel.setOpaque(false);
        dialogPanel.setBackground(new Color(0, 0, 0, 0));
        scalableComponents.add(new ScalableComponent(
                dialogPanel, 260.0 / 1280, 450.0 / 720,
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                null
        ));
        jPanel.add(dialogPanel);     // 顶层：对话框面板
        //对话框背景
        JLabel back = new JLabel(backIcon);
        scalableComponents.add(new ScalableComponent(
                back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                backIcon.getImage()
        ));

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = new JTextArea();
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        dialogText.setLineWrap(true);
        dialogText.setWrapStyleWord(true);
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        scalableComponents.add(new ScalableComponent(
                dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                null
        ));

        JButton nextBtn = new JButton();
        btnSet(nextBtn);

        scalableComponents.add(new ScalableComponent(
                nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                null
        ));

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
    public void dialogue_day_death(){
        if(events.getFirst().eventname == EventName.wsw){
            //如果不是夜间死亡事件，则和平
            resources.playSound("平和音效.wav");
            //无死亡就播放平和

            diaPanel.removeAll();
            diaPanel.setVisible(true);
            //背景图片
            ImageIcon bgIcon = resources.getImage("haikei3.png");
            JLabel background = new JLabel(bgIcon);
            background.setOpaque(false);
            background.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    background, 0, 0, 1.0, 1.0,
                    bgIcon.getImage()
            ));


            // 对话框背景（添加到对话框面板）
            ImageIcon backIcon = resources.getImage("messageframe.png");
            // 对话框面板
            JPanel dialogPanel = new JPanel();
            dialogPanel.setLayout(null);
            dialogPanel.setOpaque(false);
            dialogPanel.setBackground(new Color(0, 0, 0, 0));
            scalableComponents.add(new ScalableComponent(
                    dialogPanel, 260.0 / 1280, 450.0 / 720,
                    backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                    null
            ));
            diaPanel.add(dialogPanel);     // 顶层：对话框面板

            JLabel back = new JLabel(backIcon);
            scalableComponents.add(new ScalableComponent(
                    back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                    backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                    backIcon.getImage()
            ));


            // 文本显示区域（添加到对话框面板）
            JTextArea dialogText = new JTextArea();
            dialogText.setForeground(Color.WHITE);
            dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
            dialogText.setLineWrap(true);
            dialogText.setWrapStyleWord(true);
            dialogText.setEditable(false);
            dialogText.setOpaque(false);
            dialogText.setBackground(new Color(0, 0, 0, 0));
            dialogText.setBorder(BorderFactory.createEmptyBorder());

            scalableComponents.add(new ScalableComponent(
                    dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                    (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                    null
            ));

            JButton nextBtn = new JButton();
            btnSet(nextBtn);

            scalableComponents.add(new ScalableComponent(
                    nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                    backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                    null
            ));

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
        else {
            Event event = events.poll();
            if (event == null) {
                currentScene = Scene.GAME_SCENE_VOTE;
                run();
            }
            jPanel.removeAll();
            diaPanel.removeAll();
            diaPanel.setVisible(true);
            //背景图片
            ImageIcon bgIcon = resources.getImage("haikei3.png");
            JLabel background = new JLabel(bgIcon);
            background.setOpaque(false);
            background.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    background, 0, 0, 1.0, 1.0,
                    bgIcon.getImage()
            ));


            // 对话框背景（添加到对话框面板）
            ImageIcon backIcon = resources.getImage("messageframe.png");
            // 对话框面板
            JPanel dialogPanel = new JPanel();
            dialogPanel.setLayout(null);
            dialogPanel.setOpaque(false);
            dialogPanel.setBackground(new Color(0, 0, 0, 0));
            scalableComponents.add(new ScalableComponent(
                    dialogPanel, 260.0 / 1280, 450.0 / 720,
                    backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                    null
            ));
            diaPanel.add(dialogPanel);     // 顶层：对话框面板

            //人物立绘
            ImageIcon[] CharIcon = resources.getEventImage(event);
            JLabel Chara = new JLabel();
            JLabel back = new JLabel(backIcon);
            scalableComponents.add(new ScalableComponent(
                    back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                    backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                    backIcon.getImage()
            ));

            // 角色名称标签（添加到对话框面板）
            JLabel nameLabel = new JLabel();
            if (event.ch1 != null) {
                nameLabel.setText(getCharacterFullName(event.ch1));
            }
            nameLabel.setForeground(Color.WHITE);
            nameLabel.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
            nameLabel.setOpaque(false);
            dialogPanel.add(nameLabel);  // 添加到对话框面板
            scalableComponents.add(new ScalableComponent(
                    nameLabel, (40) / 1280.0, (10) / 720.0,  // 基于窗口的绝对位置
                    1000.0 / 1280, 30.0 / 720,
                    null
            ));

            // 文本显示区域（添加到对话框面板）
            JTextArea dialogText = new JTextArea();
            dialogText.setForeground(Color.WHITE);
            dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
            dialogText.setLineWrap(true);
            dialogText.setWrapStyleWord(true);
            dialogText.setEditable(false);
            dialogText.setOpaque(false);
            dialogText.setBackground(new Color(0, 0, 0, 0));
            dialogText.setBorder(BorderFactory.createEmptyBorder());

            scalableComponents.add(new ScalableComponent(
                    dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                    (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                    null
            ));

            JButton nextBtn = new JButton();
            btnSet(nextBtn);

            scalableComponents.add(new ScalableComponent(
                    nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                    backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                    null
            ));
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

                Chara.setIcon(CharIcon[0]);
                Chara.setOpaque(false);
                Chara.setFocusable(false);
                scalableComponents.add(new ScalableComponent(
                        Chara, (1280 - CharIcon[0].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
                diaPanel.add(Chara);
                Timer t1 = new Timer(1000, e -> {
                    //过一会显示死亡
                    resources.playSound("夜间死亡音效.wav");

                    Chara.setVisible(false);
                    JLabel Chara2 = new JLabel(CharIcon[1]);
                    Chara2.setOpaque(false);
                    Chara2.setFocusable(false);
                    scalableComponents.add(new ScalableComponent(
                            Chara2, (1280 - CharIcon[1].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[1].getIconHeight() - 30) / 720.0,
                            CharIcon[1].getIconWidth() / 1280.0, CharIcon[1].getIconHeight() / 720.0,
                            CharIcon[1].getImage()
                    ));
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
    public void dialogue_chuxing(){
        resources.playBgm("");
        Event event = events.poll();
        scalableComponents.clear();
        jPanel.removeAll();
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        // 背景图片
        ImageIcon bgIcon = resources.getImage("haikei.png");
        JLabel background = new JLabel(bgIcon);
        background.setOpaque(false);
        background.setFocusable(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));


        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(null);
        dialogPanel.setOpaque(false);
        dialogPanel.setBackground(new Color(0, 0, 0, 0));
        scalableComponents.add(new ScalableComponent(
                dialogPanel, 260.0 / 1280, 450.0 / 720,
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                null
        ));
        diaPanel.add(dialogPanel);         // 顶层：对话框面板

        //人物立绘
        ImageIcon[] CharIcon = resources.getEventImage(event);
            JLabel Chara = new JLabel(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara, (1280 - CharIcon[0].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                    CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                    CharIcon[0].getImage()
            ));
            diaPanel.add(Chara);
//            Timer timer = new Timer(1000,e -> {
//                Chara.setVisible(false);
//                JLabel Chara2 = new JLabel(CharIcon[1]);
//                Chara2.setOpaque(false);
//                Chara2.setFocusable(false);
//                scalableComponents.add(new ScalableComponent(
//                        Chara2, (1280 - CharIcon[1].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[1].getIconHeight() - 30) / 720.0,
//                        CharIcon[1].getIconWidth() / 1280.0, CharIcon[1].getIconHeight() / 720.0,
//                        CharIcon[1].getImage()
//                ));
//                diaPanel.add(Chara2);
//
//                resizeComponents();
//                diaPanel.revalidate();
//                diaPanel.repaint();
//                diaPanel.setComponentZOrder(Chara2,1);
//
//                ((Timer) e.getSource()).stop();
//            });
//            timer.start(); //启动定时器

        //对话框背景
        JLabel back = new JLabel(backIcon);
        scalableComponents.add(new ScalableComponent(
                back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                backIcon.getImage()
        ));

        // 角色名称标签（添加到对话框面板）

        JLabel nameLabel = new JLabel();
        if(event.ch1!=null) {
            nameLabel.setText(getCharacterFullName(event.ch1));
        }
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        nameLabel.setOpaque(false);
        dialogPanel.add(nameLabel);  // 添加到对话框面板
        scalableComponents.add(new ScalableComponent(
                nameLabel, (40) / 1280.0, (10) / 720.0,  // 基于窗口的绝对位置
                1000.0 / 1280, 30.0 / 720,
                null
        ));

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = new JTextArea();
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        dialogText.setLineWrap(true);
        dialogText.setWrapStyleWord(true);
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setFocusable(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        scalableComponents.add(new ScalableComponent(
                dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                null
        ));

        JButton nextBtn = new JButton();
        btnSet(nextBtn);

        scalableComponents.add(new ScalableComponent(
                nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                null
        ));

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
                    if(event.eventname == EventName.cxs) {
                        fullText[0] = "投票の結果、" + values[gs.gc[chuxingWho].number].name() + "は処刑されました。";
                    }
                    else if(event.eventname == EventName.hzsw){
                        int num = 0;
                        for(int r = 1;r < gs.gc.length;++r){
                            if(gs.gc[r].whyDie == whyDie.dayhouzhui){
                                num = gs.gc[r].number;
                            }
                        }
                        fullText[0] = "" + values[num].name() + "後追いで死亡した。";
                    }
                    else if(event.eventname == EventName.mzsw){
                        int num = 0;
                        for(int r = 1;r < gs.gc.length;++r){
                            if(gs.gc[r].whyDie == whyDie.daymaozhou){
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
                JLabel Chara2 = new JLabel(CharIcon[1]);
                Chara2.setOpaque(false);
                Chara2.setFocusable(false);
                scalableComponents.add(new ScalableComponent(
                        Chara2, (1280 - CharIcon[1].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[1].getIconHeight() - 30) / 720.0,
                        CharIcon[1].getIconWidth() / 1280.0, CharIcon[1].getIconHeight() / 720.0,
                        CharIcon[1].getImage()
                ));
                if(isNext[0]){
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

    public void dialogue_day(){

        Event event = events.poll();

        if(event == null){
            //如果显示死亡后没有其他事件直接到vote
            currentScene = Scene.GAME_SCENE_VOTE;
            run();
        }
        boolean isConnect = true;//是否为接连发生事件的下一个
        switch(event.eventname){
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
        if(!linkIcon.isEmpty()&&!isConnect){
            //前一个是，而后一个不是，直接移除不管正常中间显示
            if(isTest) {
                System.out.println("进入了isConnect");
            }
            linkIcon.remove(0);
        }
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        //背景图片
        ImageIcon bgIcon = resources.getImage("haikei3.png");
        JLabel background = new JLabel(bgIcon);
        background.setOpaque(false);
        background.setFocusable(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));


        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(null);
        dialogPanel.setOpaque(false);
        dialogPanel.setBackground(new Color(0, 0, 0, 0));
        scalableComponents.add(new ScalableComponent(
                dialogPanel, 260.0 / 1280, 450.0 / 720,
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                null
        ));
        diaPanel.add(dialogPanel);     // 顶层：对话框面板



        JLabel back = new JLabel(backIcon);
        scalableComponents.add(new ScalableComponent(
                back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                backIcon.getImage()
        ));

        // 角色名称标签（添加到对话框面板）
        JLabel nameLabel = new JLabel();
        if(event.ch1!=null) {
            nameLabel.setText(getCharacterFullName(event.ch1));
        }
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        nameLabel.setOpaque(false);
        dialogPanel.add(nameLabel);  // 添加到对话框面板
        scalableComponents.add(new ScalableComponent(
                nameLabel, (40) / 1280.0, (10) / 720.0,  // 基于窗口的绝对位置
                1000.0 / 1280, 30.0 / 720,
                null
        ));

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = new JTextArea();
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        dialogText.setLineWrap(true);
        dialogText.setWrapStyleWord(true);
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        scalableComponents.add(new ScalableComponent(
                dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                null
        ));

        JButton nextBtn = new JButton();
        btnSet(nextBtn);

        scalableComponents.add(new ScalableComponent(
                nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                null
        ));

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
        if(!linkIcon.isEmpty()) {
            //是接连发生的事件则一左一右
            //待修改
            //展示第一个，话说完了点击再展示第二个
            if(!specialEvent[0]) {
                if(isTest) {
                    System.out.println("**********不为空且不是特殊事件");
                }
                Chara.setIcon(CharIcon[0]);
                Chara.setOpaque(false);
                Chara.setFocusable(false);
                scalableComponents.add(new ScalableComponent(
                        Chara, 650 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
                diaPanel.add(Chara);
                resizeComponents();
                diaPanel.revalidate();
                diaPanel.repaint();

                JLabel Chara2 = new JLabel(linkIcon.get(0));

                Chara2.setOpaque(false);
                Chara2.setFocusable(false);
                scalableComponents.add(new ScalableComponent(
                        Chara2, 300 / 1280.0, (720 - linkIcon.get(0).getIconHeight() - 30) / 720.0,
                        linkIcon.get(0).getIconWidth() / 1280.0, linkIcon.get(0).getIconHeight() / 720.0,
                        linkIcon.get(0).getImage()
                ));
                diaPanel.add(Chara2);
                diaPanel.setComponentZOrder(Chara2, 1);

                linkIcon.remove(0);

                if(event.eventname == EventName.gprz11r) {
                    //共有认证
                    System.out.println("共有认证rrrr");
                    if (events.getFirst().eventname == EventName.gprz11p) {
                        if (isTest) {
                            System.out.println("共有认证pppp");
                        }
                        linkIcon.add(CharIcon[0]);
                        specialEvent[0] = true;
                    }
                    else {
                        if (isTest) {
                            System.out.println("共有认证失败");
                        }
                    }
                }
            }
            else{
                Chara.setIcon(linkIcon.get(0));
                Chara.setOpaque(false);
                Chara.setFocusable(false);
                scalableComponents.add(new ScalableComponent(
                        Chara, 650 / 1280.0, (720 - linkIcon.get(0).getIconHeight() - 30) / 720.0,
                        linkIcon.get(0).getIconWidth() / 1280.0, linkIcon.get(0).getIconHeight() / 720.0,
                        linkIcon.get(0).getImage()
                ));
                diaPanel.add(Chara);
                resizeComponents();
                diaPanel.revalidate();
                diaPanel.repaint();

                JLabel Chara2 = new JLabel(CharIcon[0]);

                Chara2.setOpaque(false);
                Chara2.setFocusable(false);
                scalableComponents.add(new ScalableComponent(
                        Chara2, 300 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
                diaPanel.add(Chara2);
                diaPanel.setComponentZOrder(Chara2, 1);

                linkIcon.remove(0);

                specialEvent[0] = false;
            }
        }
        else{
            if(isTest){
                System.out.println("linkIcon是空");
            }
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            boolean isLinked = false;
            switch(event.eventname){
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case zcrh12:
                    isLinked = true;
                    break;
            }
            if(isLinked){
                scalableComponents.add(new ScalableComponent(
                        Chara, 300 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
                linkIcon.add(CharIcon[0]);
            }
            else {
                scalableComponents.add(new ScalableComponent(
                        Chara, (1280 - CharIcon[0].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
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
    public void dialogue_afternoon(){
        Event event = events.poll();

        diaPanel.removeAll();
        diaPanel.setVisible(true);
        //背景图片
        ImageIcon bgIcon = resources.getImage("haikei.png");
        JLabel background = new JLabel(bgIcon);
        background.setOpaque(false);
        background.setFocusable(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));


        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(null);
        dialogPanel.setOpaque(false);
        dialogPanel.setBackground(new Color(0, 0, 0, 0));
        scalableComponents.add(new ScalableComponent(
                dialogPanel, 260.0 / 1280, 450.0 / 720,
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                null
        ));
        diaPanel.add(dialogPanel);     // 顶层：对话框面板



        JLabel back = new JLabel(backIcon);
        scalableComponents.add(new ScalableComponent(
                back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                backIcon.getImage()
        ));

        // 角色名称标签（添加到对话框面板）
        JLabel nameLabel = new JLabel();
        if(event.ch1!=null) {
            nameLabel.setText(getCharacterFullName(event.ch1));
        }
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        nameLabel.setOpaque(false);
        dialogPanel.add(nameLabel);  // 添加到对话框面板
        scalableComponents.add(new ScalableComponent(
                nameLabel, (40) / 1280.0, (10) / 720.0,  // 基于窗口的绝对位置
                1000.0 / 1280, 30.0 / 720,
                null
        ));

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = new JTextArea();
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        dialogText.setLineWrap(true);
        dialogText.setWrapStyleWord(true);
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        scalableComponents.add(new ScalableComponent(
                dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                null
        ));

        JButton nextBtn = new JButton();
        btnSet(nextBtn);

        scalableComponents.add(new ScalableComponent(
                nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                null
        ));

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
        if(!linkIcon.isEmpty()) {
            //是接连发生的事件则一左一右
            //待修改
            //展示第一个，话说完了点击再展示第二个
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara, 650/ 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                    CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                    CharIcon[0].getImage()
            ));
            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();

            JLabel Chara2 = new JLabel(linkIcon.get(0));

            Chara2.setOpaque(false);
            Chara2.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara2, 300 / 1280.0, (720 - linkIcon.get(0).getIconHeight() - 30) / 720.0,
                    linkIcon.get(0).getIconWidth() / 1280.0, linkIcon.get(0).getIconHeight() / 720.0,
                    linkIcon.get(0).getImage()
            ));
            diaPanel.add(Chara2);
            diaPanel.setComponentZOrder(Chara2, 1);

            linkIcon.remove(0);
        }
        //其他则中间
        else{
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            boolean isLinked = false;
            switch(event.eventname){
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case gprz11p:
                case zcrh12:
                    isLinked = true;
                    break;
            }
            if(isLinked){
                scalableComponents.add(new ScalableComponent(
                        Chara, 300 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
                linkIcon.add(CharIcon[0]);
            }
            else {
                scalableComponents.add(new ScalableComponent(
                        Chara, (1280 - CharIcon[0].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
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

    public JButton createDraggableButton() {
        // 1. 创建基础空按钮
        JButton draggableBtn = new JButton();
        draggableBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // 默认大小（方便拖拽，可按需删除或调整）
        draggableBtn.setPreferredSize(new Dimension(80, 30));

        // 记录核心数据：初始位置（拖拽前的位置，用于松开后回位）
        final Point initPos = new Point();
        // 记录鼠标相对于按钮的偏移量
        final int[] mouseOffset = new int[2];

        // 2. 鼠标按下：记录初始位置+偏移量+切换光标+按钮置顶
        draggableBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 记录按钮当前位置（拖拽前的初始位置）
                initPos.setLocation(draggableBtn.getX(), draggableBtn.getY());
                // 记录鼠标在按钮内的偏移量（防止拖动瞬移）
                mouseOffset[0] = e.getX();
                mouseOffset[1] = e.getY();
                // 切换为移动光标
                draggableBtn.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                // 按钮置顶，避免被其他组件遮挡
                Container parent = draggableBtn.getParent();
                if (parent != null) {
                    parent.setComponentZOrder(draggableBtn, 0);
                    parent.repaint();
                }
            }


        });

        // 3. 鼠标拖动：跟随移动+边界限制（不超出父容器）
        draggableBtn.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Container parent = draggableBtn.getParent();
                // 校验父容器尺寸有效（避免缩放时计算错乱）
                if (parent == null || parent.getWidth() <= 0 || parent.getHeight() <= 0
                        || draggableBtn.getWidth() <= 0 || draggableBtn.getHeight() <= 0) {
                    return;
                }

                Point screenPos = e.getLocationOnScreen();
                SwingUtilities.convertPointFromScreen(screenPos, parent);

                int newX = screenPos.x - mouseOffset[0];
                int newY = screenPos.y - mouseOffset[1];

                // 边界限制（同上）
                newX = Math.max(0, Math.min(newX, parent.getWidth() - draggableBtn.getWidth()));
                newY = Math.max(0, Math.min(newY, parent.getHeight() - draggableBtn.getHeight()));

                draggableBtn.setBounds(newX, newY, draggableBtn.getWidth(), draggableBtn.getHeight());
            }
        });

        return draggableBtn;
    }//创建一个拖动button

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

    public void GameScene_vote(){

        jPanel.removeAll();
        scalableComponents.clear();
        if(isTest) {
            testBtn();//测试按钮
        }
        // 背景图片
        ImageIcon bgIcon = resources.getImage("komorebi002yuu.png");
        JLabel background = new JLabel(bgIcon);
        background.setOpaque(false);
        background.setFocusable(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));



        //按钮
        if((gs.aliveCounter - 1)/2 == 1){
            ImageIcon musicBtnIcon = resources.getImage("musicBtn.png");
            JButton btnMusic = new JButton(musicBtnIcon);
            btnSet(btnMusic);
            scalableComponents.add(new ScalableComponent(btnMusic,15.0/1280,35.0/720,musicBtnIcon.getIconWidth()/1280.0,musicBtnIcon.getIconHeight()/720.0,musicBtnIcon.getImage()));
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
            JButton btnMusic = new JButton(musicBtnIcon);
            btnSet(btnMusic);
            scalableComponents.add(new ScalableComponent(btnMusic,15.0/1280,35.0/720,musicBtnIcon.getIconWidth()/1280.0,musicBtnIcon.getIconHeight()/720.0,musicBtnIcon.getImage()));
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

        //jPanel.add(musicText);
        // 遍历游戏中的所有角色（假设角色存储在gs.gc数组中）
        //测试11111111111111111
//        gs.gc[5].claimedRole = 3;
//
//        gs.gc[5].claimedRoleorder = 2;
//
//        gs.gc[8].whyDie = whyDie.chuxing;
//        gs.gc[6].whyDie = whyDie.beiyao;
//        gs.gc[2].whyDie = whyDie.chuxing;
//
//
//        gs.gc[8].dieDay = 1;
//        gs.gc[2].dieDay = 1;
//        gs.gc[6].dieDay = 1;
//
//        //1, 8, 10, 11, 12, 13, 41, 20, 22, 23, 24, 26, 29, 30, 33, 34, 35, 36, 37, 41
//        gs.gc[1].skillTarget[1] = 11;
//        gs.gc[2].skillTarget[1] = 12;
//        gs.gc[3].skillTarget[1] = 13;
//        gs.gc[4].skillTarget[1] = 14;
//        gs.gc[5].skillTarget[1] = 15;
//        gs.gc[6].skillTarget[1] = 16;
//        gs.gc[7].skillTarget[1] = 17;
//        gs.gc[8].skillTarget[1] = 18;
//        gs.gc[9].skillTarget[1] = 19;
//        gs.gc[10].skillTarget[1] = 20;
//        gs.gc[11].skillTarget[1] = 21;
//        gs.gc[12].skillTarget[1] = 22;
//        gs.gc[13].skillTarget[1] = 23;
//        gs.gc[14].skillTarget[1] = 24;
//        gs.gc[15].skillTarget[1] = 25;
//        gs.gc[16].skillTarget[1] = 26;
//        gs.gc[17].skillTarget[1] = 27;
//        gs.gc[18].skillTarget[1] = 28;
//        gs.gc[19].skillTarget[1] = 29;
//        gs.gc[20].skillTarget[1] = 30;
//
//        gs.gc[1].skillTarget[2] = 2;
//        gs.gc[2].skillTarget[2] = 4;
//        gs.gc[3].skillTarget[2] = 6;
//        gs.gc[4].skillTarget[2] = 8;
//        gs.gc[5].skillTarget[2] = 10;
//        gs.gc[6].skillTarget[2] = 12;
//        gs.gc[7].skillTarget[2] = 14;
//        gs.gc[8].skillTarget[2] = 16;
//        gs.gc[9].skillTarget[2] = 18;
//        gs.gc[10].skillTarget[2] = 20;
//        gs.gc[11].skillTarget[2] = 22;
//        gs.gc[12].skillTarget[2] = 24;
//        gs.gc[13].skillTarget[2] = 26;
//        gs.gc[14].skillTarget[2] = 28;
//        gs.gc[15].skillTarget[2] = 20;
//        gs.gc[16].skillTarget[2] = 32;
//        gs.gc[17].skillTarget[2] = 34;
//        gs.gc[18].skillTarget[2] = 36;
//        gs.gc[19].skillTarget[2] = 38;
//        gs.gc[20].skillTarget[2] = 40;

//        gs.gc[10].claimedRole = 1;
//        gs.gc[10].claimedRoleorder = 2;
//        gs.gc[3].claimedRole = 1;
//        gs.gc[3].claimedRoleorder = 3;
//        gs.gc[15].claimedRole = 2;
//        gs.gc[15].claimedRoleorder = 2;
//        gs.gc[1].claimedRole = 1;
//        gs.gc[1].claimedRoleorder = 1;
//        gs.gc[2].claimedRole = 1;
//        gs.gc[2].claimedRoleorder = 2;
//        gs.gc[3].claimedRole = 2;
//        gs.gc[3].claimedRoleorder = 1;
//        gs.gc[4].claimedRole = 2;
//        gs.gc[4].claimedRoleorder = 2;

//        gs.gc[12].claimedRole = 4;
//        gs.gc[11].claimedRole = 4;

//        gs.gc[6].whyDie = whyDie.dayhouzhui;
//        gs.gc[7].whyDie = whyDie.daymaozhou;

//        gs.gc[8].claimedRole = 5;
//        gs.gc[9].claimedRole = 11;


        for (int i = 1; i <= gs.gc.length - 1; i++) {
            // 获取第i个角色对象
            //GameCharacter character = gs.gc[i];
            //if (character == null) continue;

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
            if(gs.gc[i].claimedRole > 0 && gs.gc[i].claimedRole < 6){
                //有职业则进入
                if(gs.gc[i].claimedRole <= 3){
                    //职业图标
                    claimedRoleName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder).append(".png");
                    //技能结果
                    skillTargetName.append(gs.gc[i].claimedRole).append("_").append(gs.gc[i].claimedRoleorder);
                    int a = 0;
                    for(int day = 1;day < gs.gameDay;day++) {
                        a = gs.gc[i].skillTarget[day];
                    }
                    if(a != 0){
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
                else{
                    claimedRoleName.append(gs.gc[i].claimedRole).append(".png");
                }
                ImageIcon claimedRoleIcon = resources.getImage(claimedRoleName.toString());
                JLabel claimedRoleLabel = new JLabel(claimedRoleIcon);
                if(i <= (gs.gc.length - 1 + 1)/2){
                    //职业
                    scalableComponents.add(new ScalableComponent(claimedRoleLabel,((160+64 * i)/1280.0),0.0/720,
                            claimedRoleIcon.getIconWidth()/1280.0
                            ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(claimedRoleLabel,(160+64 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,98.0/720.0
                            ,claimedRoleIcon.getIconWidth()/1280.0
                            ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                }
                jPanel.add(claimedRoleLabel);



            }

            if(!xName.isEmpty()){
                //不为空说明有死亡
                ImageIcon deathImage = resources.getImage(xName.toString());
                JLabel deathLabel = new JLabel(deathImage);
                if(i <= (gs.gc.length - 1 + 1)/2){
                    //死亡叉叉
                    scalableComponents.add(new ScalableComponent(deathLabel,((165+64 * i)/1280.0),10.0/720,
                            deathImage.getIconWidth()/1280.0
                            ,deathImage.getIconHeight()/720.0,deathImage.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(deathLabel,(165+64 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,108.0/720.0
                            ,deathImage.getIconWidth()/1280.0
                            ,deathImage.getIconHeight()/720.0,deathImage.getImage()));
                }
                jPanel.add(deathLabel);
            }

            ImageIcon characterImage = resources.getImage(imageName.toString());
            ImageIcon characterText = resources.getImage(textName);
            JLabel label = new JLabel(characterImage);
            JLabel textLabel = new JLabel(characterText);
            if(i <= (gs.gc.length - 1 + 1)/2){
                //头像
                scalableComponents.add(new ScalableComponent(label,(160+characterImage.getIconWidth() * i)/1280.0,0.0/720,characterImage.getIconWidth()/1280.0
                        ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                //文字
                scalableComponents.add(new ScalableComponent(textLabel,(175+characterImage.getIconWidth() * i)/1280.0,(characterImage.getIconHeight()-characterText.getIconHeight()/2.0)/720.0,characterText.getIconWidth() / 2.0 /1280.0
                        ,characterText.getIconHeight()/2.0/720.0,characterText.getImage()));
            }
            else{
                scalableComponents.add(new ScalableComponent(label,(160+characterImage.getIconWidth() * (i - ((gs.gc.length - 1+1)/2)))/1280.0,(0.0+characterImage.getIconHeight())/720.0
                        ,characterImage.getIconWidth()/1280.0
                        ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                scalableComponents.add(new ScalableComponent(textLabel,(175+characterImage.getIconWidth() * (i - ((gs.gc.length - 1+1)/2)))/1280.0,(2* characterImage.getIconHeight()-characterText.getIconHeight()/2.0)/720.0,characterText.getIconWidth() / 2.0 /1280.0
                        ,characterText.getIconHeight()/2.0/720.0,characterText.getImage()));
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
                if (i <= (gs.gc.length - 1 + 1) / 2) {
                    //技能
                    scalableComponents.add(new ScalableComponent(skillTargetLabel, ((160 + 64 * (i + 1) - skillTargetIcon.getIconWidth() * zynum) / 1280.0), (0.0 + (order - 1) * skillTargetIcon.getIconHeight()) / 720,
                            skillTargetIcon.getIconWidth() / 1280.0
                            , skillTargetIcon.getIconHeight() / 720.0, skillTargetIcon.getImage()));
                } else {
                    scalableComponents.add(new ScalableComponent(skillTargetLabel, (160 + 64 * (i + 1 - ((gs.gc.length - 1 + 1) / 2)) - skillTargetIcon.getIconWidth() * zynum) / 1280.0, (98.0 + (order - 1) * skillTargetIcon.getIconHeight()) / 720.0
                            , skillTargetIcon.getIconWidth() / 1280.0
                            , skillTargetIcon.getIconHeight() / 720.0, skillTargetIcon.getImage()));
                }
                jPanel.add(skillTargetLabel);
                jPanel.setComponentZOrder(skillTargetLabel, 0);
            }
        }


        //显示数据
        ImageIcon dataImage = resources.getImage("hiduke.png");
        JLabel data = new JLabel(dataImage);
        scalableComponents.add(new ScalableComponent(data,880.0/1280,10.0/720,
                dataImage.getIconWidth()/1280.0,dataImage.getIconHeight()/720.0,
                dataImage.getImage()));
        //数据文本
        JTextArea dataText = new JTextArea("    "+gs.gameDay+"日目\n 生存者:" +gs.aliveCounter+"\n 死亡者:"+gs.deathCounter+"\n 吊り縄:"+(gs.aliveCounter - 1)/2);
        dataText.setForeground(Color.WHITE);
        dataText.setFont(new Font("Takao Mincho",Font.BOLD,20));
        dataText.setLineWrap(true);       // 自动换行
        dataText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dataText.setEditable(false);
        dataText.setOpaque(false);
        dataText.setBackground(new Color(0, 0, 0, 0));
        dataText.setBorder(BorderFactory.createEmptyBorder());
        scalableComponents.add(new ScalableComponent(dataText,890.0/1280,25.0/720,100.0/1280,130.0/720,null));

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
        for(int i = 1; i <= gs.gc.length - 1;i++){
            for(int j = 1;j < gs.gameDay;++j) {
                if (gs.gc[i].dieDay == j) {
                    isPeace.add(false);//只要有一个的死亡日期是昨天，就不平和
                }
            }
        }

        List<Integer> peacePos = new ArrayList<>();
        for(int j = 1;j < gs.gameDay;++j) {
            int pos = 0;
            if(!isPeace.isEmpty()&&isPeace.get(j-1)){
                //第j天平和，则找出位置
                for(int i = 1; i <= gs.gc.length - 1;i++){
                    if(gs.gc[i].dieDay!= 0 && gs.gc[i].dieDay < j){
                        pos++;
                    }
                }
                peacePos.add(pos);
            }
        }
        for(int k = 1; k < gs.gameDay;++k){
            int shitiCnt = 0;
            ArrayList<Integer> shitiNum = new ArrayList<>();
            for(int i = 1; i <= gs.gc.length - 1;i++){
                //注意是宣称有这个职业才要显示
                //死亡了显示到死亡前
                if(k == 1) switch(gs.gc[i].claimedRole){
                    case 1://占卜
                        zhanbu.append(getJobText(gs.gc[i].number)).append(" : ");
                        for(int j = 1;j < gs.gameDay;++j){
                            //第一天占卜，在第二天才能看，此时只有第一天有，第二天没有
                            if(gs.gc[i].dieDay != 0&&j >= gs.gc[i].dieDay){
                                //如果超出范围
                                break;
                            }
                            else{

                                if(gs.gc[i].skillTarget[j] > (gs.gc.length - 1)){
                                    //黑球
                                    zhanbu.append(getJobText(gs.gc[gs.gc[i].skillTarget[j] - (gs.gc.length-1)].number)).append("●");
                                    zhanbu.append("→");
                                }
                                else if((gs.gc[i].skillTarget[j] > 0)){
                                    zhanbu.append(getJobText(gs.gc[gs.gc[i].skillTarget[j]].number)).append("○");
                                    zhanbu.append("→");
                                }
                            }

                        }
                        if(gs.gc[i].nonHumanMarker) {
                            zhanbu.append("破绽");
                            zhanbu.append("→");
                        }
                        zhanbu.setLength(zhanbu.length() - 1);
                        zhanbu.append("\n");//换行

                        break;
                    case 2://灵能
                        lingneng.append(getJobText(gs.gc[i].number)).append(" : ");
                        for(int j = 2;j < gs.gameDay;++j){
                            if(gs.gc[i].dieDay != 0&&j >= gs.gc[i].dieDay){
                                //如果超出范围
                                break;
                            }
                            else{
                                if(gs.gc[i].skillTarget[j] > (gs.gc.length - 1)){
                                    //黑球
                                    lingneng.append(getJobText(gs.gc[gs.gc[i].skillTarget[j] - (gs.gc.length-1)].number)).append("●");
                                    lingneng.append("→");
                                }
                                else if((gs.gc[i].skillTarget[j] > 0)){
                                    lingneng.append(getJobText(gs.gc[gs.gc[i].skillTarget[j]].number)).append("○");
                                    lingneng.append("→");
                                }
                            }
                        }
                        if(gs.gc[i].nonHumanMarker) {
                            lingneng.append("破绽");
                            lingneng.append("→");
                        }
                        lingneng.setLength(lingneng.length()-1);
                        lingneng.append("\n");//换行

                        break;
                    case 3://猎人
                        lieren.append(getJobText(gs.gc[i].number)).append(" : ");
                        for(int j = 2;j < gs.gameDay;++j){
                            if(gs.gc[i].dieDay != 0&&j >= gs.gc[i].dieDay) {
                                //如果超出范围
                                break;
                            }
                            if(gs.gc[i].skillTarget[j] != 0){
                                //没有对象
                                lieren.append(getJobText(gs.gc[gs.gc[i].skillTarget[j]].number));
                                lieren.append("→");
                            }

                        }
                        if(gs.gc[i].nonHumanMarker) {
                            lieren.append("破绽");
                            lieren.append("→");

                        }
                        lieren.setLength(lieren.length()-1);
                        lieren.append("\n");//换行
                        break;
                }
                switch(gs.gc[i].whyDie){
                    case whyDie.chuxing:
                        if(gs.gc[i].actualRole == 10){
                            for (int j = 1; j <= gs.gc.length - 1; j++) {
                                //循环找一下谁是背德
                                if (gs.gc[j].actualRole == 11 && gs.gc[j].whyDie !=whyDie.NONE && gs.gc[i].dieDay == k && gs.gc[j].dieDay < gs.gc[i].dieDay) {
                                    //如果背德已经死了且死亡在妖狐前，就正常显示
                                    chuxing.append(getJobText(gs.gc[i].number)).append("→");
                                    break;
                                }
                                //没死的话就不做处理，等后面有dayhouzhui的时候一起处理
                            }
                        }
                        else if(gs.gc[i].actualRole == 5){

                        }
                        else{
                            if(gs.gc[i].dieDay == k){
                            chuxing.append(getJobText(gs.gc[i].number)).append("→");
                            }
                        }
                        break;
                    case whyDie.daymaozhou:
                        if(gs.gc[i].dieDay == k) {
                            for (int j = 1; j <= gs.gc.length - 1; j++) {
                                //循环找一下谁是猫
                                if (gs.gc[j].actualRole == 5) {
                                    chuxing.append(getJobText(gs.gc[j].number)).append("+");
                                    break;
                                }

                            }

                            //实现 猫+受猫害者
                            chuxing.append(getJobText(gs.gc[i].number)).append("(猫呪)").append("→");
                        }
                        break;
                    case whyDie.dayhouzhui:
                        if(gs.gc[i].dieDay == k) {
                            for (int j = 1; j <= gs.gc.length - 1; j++) {
                                //循环找一下谁是妖狐
                                if (gs.gc[j].actualRole == 10) {
                                    chuxing.append(getJobText(gs.gc[j].number)).append("+");
                                    break;
                                }
                            }
                            //实现 狐+背德
                            chuxing.append(getJobText(gs.gc[i].number)).append("(後追)").append("→");
                        }
                        break;

                    default:
                        //其他情况，即夜晚被杀
                        if(gs.gc[i].dieDay == k) {
                            shitiCnt++;
                            shitiNum.add(gs.gc[i].number);
                        }
                        break;
                }



                if(i == gs.gc.length - 1&& shitiCnt == 0){
                    //当第k日循环到最后一个角色shitiCnt都为0说明没死
                    shiti.append("平和→");
                }
            }
            if(shitiCnt == 1) {
                shiti.append(getJobText(shitiNum.get(0))).append("→");
            }
            else {
                for (int l = 0; l < shitiNum.size(); ++l) {
                    shiti.append(getJobText(shitiNum.get(l))).append("+");
                }
                shiti.setLength(shiti.length()-1);
                shiti.append("→");
            }
        }
        if(shiti.length()>2) {
            shiti.setLength(shiti.length() - 1);
        }
        if(chuxing.length()>2) {
            chuxing.setLength(chuxing.length() - 1);
        }
        StringBuilder result = new StringBuilder();
        result.append("[占い師]\n").append(zhanbu).append("[霊能者]\n").append(lingneng).append("[処刑]\n").append(chuxing).append("\n[死体]\n").append
                (shiti).append("\n[護衛先]\n").append(lieren);


        //背景
        ImageIcon boardImage = resources.getImage("frame #19252.png");
        JLabel board = new JLabel(boardImage);
        scalableComponents.add(new ScalableComponent(board,0.0/1280,198.0/720,
                (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                boardImage.getImage()));
        JTextArea infoText = new JTextArea();
        infoText.setText(result.toString());
        infoText.setForeground(Color.BLACK);
        infoText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        infoText.setLineWrap(true);       // 自动换行
        infoText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        infoText.setEditable(false);
        infoText.setFocusable(false);
        infoText.setOpaque(false);
        infoText.setBackground(new Color(0, 0, 0, 0));
        infoText.setBorder(BorderFactory.createEmptyBorder());
        // 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(infoText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 不显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,40.0/1280,228.0/720,
                (200+boardImage.getIconWidth() - 80)/1280.0,(50+boardImage.getIconHeight()- 60)/720.0,
                null));

        //按钮
        //投票
        ImageIcon btnImage = resources.getImage("goTohyo.png");
        JButton voteBtn = new JButton(btnImage);
        btnSet(voteBtn);

        scalableComponents.add(new ScalableComponent(voteBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight())/720.0,
                        btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                        btnImage.getImage()));

        jPanel.add(voteBtn);
        //记录确认
        btnImage = resources.getImage("check.png");
        JButton recordBtn = new JButton(btnImage);
        btnSet(recordBtn);
        scalableComponents.add(new ScalableComponent(recordBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 2 - 10)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        jPanel.add(recordBtn);
        //指示按钮
        btnImage = resources.getImage("shiji.png");
        JButton pointBtn = new JButton(btnImage);
        btnSet(pointBtn);
        scalableComponents.add(new ScalableComponent(pointBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 3 - 20)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));

        jPanel.add(pointBtn);
        //回避按钮(关)
        btnImage = resources.getImage("关闭回避.png");
        JButton avoidBtn = new JButton(btnImage);
        btnSet(avoidBtn);
        scalableComponents.add(new ScalableComponent(avoidBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 4 - 30)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));

        jPanel.add(avoidBtn);
        //回避按钮(开)
        btnImage = resources.getImage("开启回避.png");
        JButton avoidBtn1 = new JButton(btnImage);
        btnSet(avoidBtn1);
        scalableComponents.add(new ScalableComponent(avoidBtn1,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 4 - 30)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        avoidBtn1.setVisible(false);
        jPanel.add(avoidBtn1);
        //退出按钮
        btnImage = resources.getImage("IntroTitle.png");
        JButton menuBtn = new JButton(btnImage);
        btnSet(menuBtn);
        scalableComponents.add(new ScalableComponent(menuBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 5 - 40)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        menuBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        jPanel.add(menuBtn);
        //投票第二层
        //灰随机
        btnImage = resources.getImage("tohyoGrey.png");
        JButton greyBtn = new JButton(btnImage);
        btnSet(greyBtn);

        scalableComponents.add(new ScalableComponent(greyBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 3 - 20)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        greyBtn.setVisible(false);
        jPanel.add(greyBtn);
        //自由投票
        btnImage = resources.getImage("tohyoFree.png");
        JButton freeBtn = new JButton(btnImage);
        btnSet(freeBtn);
        scalableComponents.add(new ScalableComponent(freeBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 2 - 10)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        freeBtn.setVisible(false);
        jPanel.add(freeBtn);

        //记录确认第二层
        //怀疑度
        btnImage = resources.getImage("checkUtagai.png");
        JButton doubtBtn = new JButton(btnImage);
        btnSet(doubtBtn);

        scalableComponents.add(new ScalableComponent(doubtBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 3 - 20)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        doubtBtn.setVisible(false);
        jPanel.add(doubtBtn);
        //投票履历
        btnImage = resources.getImage("checkTohyo.png");
        JButton votehisBtn = new JButton(btnImage);
        btnSet(votehisBtn);
        scalableComponents.add(new ScalableComponent(votehisBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 2 - 10)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        votehisBtn.setVisible(false);
        jPanel.add(votehisBtn);

        //指示按钮第二层
        //co指示
        btnImage = resources.getImage("doCO.png");
        JButton coBtn = new JButton(btnImage);
        btnSet(coBtn);

        scalableComponents.add(new ScalableComponent(coBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 3 - 20)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        coBtn.setVisible(false);
        jPanel.add(coBtn);
        //指定指示
        btnImage = resources.getImage("doShitei.png");
        JButton ppBtn = new JButton(btnImage);
        btnSet(ppBtn);
        scalableComponents.add(new ScalableComponent(ppBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 2 - 10)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        ppBtn.setVisible(false);
        jPanel.add(ppBtn);
        //返回按钮
        btnImage = resources.getImage("return.png");
        JButton returnBtn = new JButton(btnImage);
        btnSet(returnBtn);
        scalableComponents.add(new ScalableComponent(returnBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight())/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        returnBtn.setVisible(false);
        jPanel.add(returnBtn);
        //下一天按钮
        btnImage = resources.getImage("nextDay.png");
        JButton nextBtn = new JButton(btnImage);
        btnSet(nextBtn);
        scalableComponents.add(new ScalableComponent(nextBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight())/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        nextBtn.setVisible(false);
        jPanel.add(nextBtn);
        //同票再次投票按钮
        btnImage = resources.getImage("goTohyo.png");
        JButton againBtn = new JButton(btnImage);
        btnSet(againBtn);
        scalableComponents.add(new ScalableComponent(againBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight())/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        againBtn.setVisible(false);
        jPanel.add(againBtn);
        //指定投票按钮
        //指定投票
        btnImage = resources.getImage("tohyoShitei.png");
        JButton readyVoteBtn = new JButton(btnImage);
        btnSet(readyVoteBtn);

        scalableComponents.add(new ScalableComponent(readyVoteBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 3 - 20)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        readyVoteBtn.setVisible(false);
        jPanel.add(readyVoteBtn);
        //co按钮第三层
        //询问co
        btnImage = resources.getImage("询问CO.png");
        JButton askCoBtn = new JButton(btnImage);
        btnSet(askCoBtn);
        scalableComponents.add(new ScalableComponent(askCoBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 4 - 30)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        askCoBtn.setVisible(false);
        jPanel.add(askCoBtn);
        //灵能指示
        btnImage = resources.getImage("reiCO.png");
        JButton reiBtn = new JButton(btnImage);
        btnSet(reiBtn);

        scalableComponents.add(new ScalableComponent(reiBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 3 - 20)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        reiBtn.setVisible(false);
        jPanel.add(reiBtn);
        //猎人指示
        btnImage = resources.getImage("kariCO.png");
        JButton kariBtn = new JButton(btnImage);
        btnSet(kariBtn);
        scalableComponents.add(new ScalableComponent(kariBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 2 - 10)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        kariBtn.setVisible(false);
        jPanel.add(kariBtn);

        //占卜指示
        btnImage = resources.getImage("uranaiCO.png");
        JButton uranaiBtn = new JButton(btnImage);
        btnSet(uranaiBtn);

        scalableComponents.add(new ScalableComponent(uranaiBtn,(1060.0 - btnImage.getIconWidth() - 30)/1280,(720 - 40 - btnImage.getIconHeight()* 3 - 20)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        uranaiBtn.setVisible(false);
        jPanel.add(uranaiBtn);
        //共有指示
        btnImage = resources.getImage("kyouyuCO.png");
        JButton kyouyuBtn = new JButton(btnImage);
        btnSet(kyouyuBtn);
        scalableComponents.add(new ScalableComponent(kyouyuBtn,(1060.0 - btnImage.getIconWidth() - 30)/1280,(720 - 40 - btnImage.getIconHeight()* 2 - 10)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        kyouyuBtn.setVisible(false);
        jPanel.add(kyouyuBtn);
        //猫又指示
        btnImage = resources.getImage("catCO.png");
        JButton catBtn = new JButton(btnImage);
        btnSet(catBtn);
        scalableComponents.add(new ScalableComponent(catBtn,(1060.0 - btnImage.getIconWidth() - 30)/1280,(720 - 40 - btnImage.getIconHeight())/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        catBtn.setVisible(false);
        jPanel.add(catBtn);
        //指定指示按钮第三层
        //指定投票
        btnImage = resources.getImage("tohyoShitei.png");
        JButton fixedVoteBtn = new JButton(btnImage);
        btnSet(fixedVoteBtn);

        scalableComponents.add(new ScalableComponent(fixedVoteBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 3 - 20)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        fixedVoteBtn.setVisible(false);
        jPanel.add(fixedVoteBtn);
        //指定占卜
        btnImage = resources.getImage("shiteiUranai.png");
        JButton fixedUranaiBtn = new JButton(btnImage);
        btnSet(fixedUranaiBtn);
        scalableComponents.add(new ScalableComponent(fixedUranaiBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight()* 2 - 10)/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
        fixedUranaiBtn.setVisible(false);
        jPanel.add(fixedUranaiBtn);

        //指定护卫
        btnImage = resources.getImage("shiteiGoei.png");
        JButton protectBtn = new JButton(btnImage);
        btnSet(protectBtn);
        scalableComponents.add(new ScalableComponent(protectBtn,1060.0/1280,(720 - 40 - btnImage.getIconHeight())/720.0,
                btnImage.getIconWidth()/1280.0,btnImage.getIconHeight()/720.0,
                btnImage.getImage()));
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
        for(int j = 1;j<gs.gameDay;++j){
            //第j天
            for(int k = 1;k<gs.gc.length;++k){
                if(gs.gc[k].claimedRole == 1) {
                    //如果有占卜师
                    int num = gs.gc[k].skillTarget[j];

                    if(num > gs.gc.length - 1){
                        num -= gs.gc.length- 1;
                    }

                    if(!beiZhan1.contains(num)) {
                        beiZhan1.add(num);
                        System.out.println(num);
                    }
                }
            }
        }

        for(int i = 1;i<gs.gc.length;++i){
            //灰投票是获取无球无职活着的人
            if(isTest){
                System.out.println("已进入灰循环" + getJobText(gs.gc[i].number) + " " +gs.gc[i].whyDie+" "+ gs.gc[i].claimedRole);
            }
            if(gs.gc[i].whyDie == whyDie.NONE&&(gs.gc[i].claimedRole == 0 || gs.gc[i].claimedRole == 6)) {
                //活着
                //不能有声称的职业
                //村人和未声明的
                if(beiZhan1.contains(i)){
                    if(isTest){
                        System.out.println(getJobText(gs.gc[i].number) + "被占卜过了，不是灰");
                    }
                    continue;
                }
                if(isTest){
                System.out.println(getJobText(gs.gc[i].number) + "是灰");
                }
                cxList.add(i);

            }
        }
        //怀疑度
        doubtBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            doubtBtn.setVisible(false);
            votehisBtn.setVisible(false);
            infoText.setVisible(false);

            createDoubt();
        });
//        //投票履历
//        //测试111111111111
//        voteRounds.add(2);
//        voteRounds.add(3);
        JPanel hisPanel = new JPanel();
        hisPanel.setOpaque(false);
        hisPanel.setLayout(null);
        scalableComponents.add(new ScalableComponent(hisPanel,0.0/1280,198.0/720,
                (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                null));

        jPanel.add(hisPanel);
        hisPanel.setVisible(false);
        ImageIcon levelIcon = resources.getImage(levelName);
        JLabel levellb = new JLabel(levelIcon);
        jPanel.add(levellb);
        scalableComponents.add(new ScalableComponent(levellb,700.0/1280,600.0/720,
                levelIcon.getIconWidth()/1280.0,levelIcon.getIconHeight()/720.0,
                levelIcon.getImage()));
        levellb.setVisible(false);

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

            for(int i = 0;i < voteRounds.size();++i){
                int gameday = i+2;
                int roundMax = voteRounds.get(gameday-2);
                //前结果
                ImageIcon btnImage1;
                btnImage1 = resources.getImage("rirekiBack.png");
                JButton backResult = new JButton(btnImage1);
                btnSet(backResult);

                scalableComponents.add(new ScalableComponent(backResult,1060.0/1280,(720 - 40 - btnImage1.getIconHeight()* 3 - 20)/720.0,
                        btnImage1.getIconWidth()/1280.0,btnImage1.getIconHeight()/720.0,
                        btnImage1.getImage()));
                backResult.setVisible(false);
                jPanel.add(backResult);
                jPanel.setComponentZOrder(backResult,0);
                //次结果
                btnImage1 = resources.getImage("rirekiNext.png");
                JButton nextResult = new JButton(btnImage1);
                btnSet(nextResult);
                scalableComponents.add(new ScalableComponent(nextResult,1060.0/1280,(720 - 40 - btnImage1.getIconHeight()* 2 - 10)/720.0,
                        btnImage1.getIconWidth()/1280.0,btnImage1.getIconHeight()/720.0,
                        btnImage1.getImage()));
                nextResult.setVisible(false);
                jPanel.add(nextResult);
                jPanel.setComponentZOrder(nextResult,0);
                //前结果
                btnImage1 = resources.getImage("rirekiBack.png");
                JButton backResult1 = new JButton(btnImage1);
                btnSet(backResult1);

                scalableComponents.add(new ScalableComponent(backResult1,1060.0/1280,(720 - 40 - btnImage1.getIconHeight()* 3 - 20)/720.0,
                        btnImage1.getIconWidth()/1280.0,btnImage1.getIconHeight()/720.0,
                        btnImage1.getImage()));
                backResult1.setVisible(false);
                jPanel.add(backResult1);
                jPanel.setComponentZOrder(backResult1,0);
                //次结果
                btnImage1 = resources.getImage("rirekiNext.png");
                JButton nextResult1 = new JButton(btnImage1);
                btnSet(nextResult1);
                scalableComponents.add(new ScalableComponent(nextResult1,1060.0/1280,(720 - 40 - btnImage1.getIconHeight()* 2 - 10)/720.0,
                        btnImage1.getIconWidth()/1280.0,btnImage1.getIconHeight()/720.0,
                        btnImage1.getImage()));
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
                JButton dayBtn = new JButton(dayIcon);
                btnSet(dayBtn);
                dayBtn.addActionListener(e1 -> {

                    createDayPiao(1,gameday,voteMethods.get(gameday-2));
                    hisPanel.setVisible(false);
                    if(roundMax != 1){
                       nextResult.setVisible(true);
                    }
                });
                if(i < 5){
                scalableComponents.add(new ScalableComponent(dayBtn,((10+dayIcon.getIconWidth())*i)/1280.0,10.0/720,
                        dayIcon.getIconWidth()/1280.0,dayIcon.getIconHeight()/720.0,
                        dayIcon.getImage()));
                }
                else if(i < 10){
                    scalableComponents.add(new ScalableComponent(dayBtn,((10+dayIcon.getIconWidth())*(i -5))/1280.0,(dayIcon.getIconHeight()+20)/720.0,
                            dayIcon.getIconWidth()/1280.0,dayIcon.getIconHeight()/720.0,
                            dayIcon.getImage()));

                }
                else{
                    scalableComponents.add(new ScalableComponent(dayBtn,((10+dayIcon.getIconWidth())*(i -10))/1280.0,(dayIcon.getIconHeight()*2+30)/720.0,
                            dayIcon.getIconWidth()/1280.0,dayIcon.getIconHeight()/720.0,
                            dayIcon.getImage()));
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
                        isSelectedVoteTargetResult.append(getJobText(gs.gc[i].number)+" ");
                    }

                }
                isSelectedVoteTargetResult.append("\n");
            }


            if(isZhan[0]) {
                isSelectedVoteTargetResult.append("[指定占い]\n");
                for (int i = 1; i < gs.gc.length; ++i) {
                    if (gs.gc[i].claimedRole == 1&& gs.gc[i].whyDie == whyDie.NONE) {
                        isSelectedVoteTargetResult.append(getJobText(gs.gc[i].number) + "→");
                        for (int j = 1; j < gs.gc.length; ++j) {
                            if (gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                                isSelectedVoteTargetResult.append(getJobText(gs.gc[j].number) + ",");
                            }

                        }
                        isSelectedVoteTargetResult.append("\n");
                    }
                }
                int cc = 0;
                for (int j = 1; j < gs.gc.length; ++j) {
                    if (gs.hiddenSeerScheduledSkillTargets[j][gs.gameDay]) {
                        if(cc++ == 0)isSelectedVoteTargetResult.append("潜伏→");
                        isSelectedVoteTargetResult.append(getJobText(gs.gc[j].number) + ",");
                    }

                }
                isSelectedVoteTargetResult.append("\n");

            }

            if(isTest) {
                System.out.println("是否护卫"+isHu[0]);
                for (int i = 1; i < gs.gc.length; ++i) {
                    for (int j = 1; j < gs.gc.length; ++j) {
                        if (gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                            System.out.println(getJobText(gs.gc[i].number)+"护卫了"+gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]);
                        }
                    }
                }
            }
            if(isHu[0]) {
                isSelectedVoteTargetResult.append("[指定護衛]\n");

                for (int i = 1; i < gs.gc.length; ++i) {
                    if (gs.gc[i].claimedRole == 3 && gs.gc[i].whyDie == whyDie.NONE) {
                        isSelectedVoteTargetResult.append(getJobText(gs.gc[i].number) + "→");
                        for (int j = 1; j < gs.gc.length; ++j) {
                            if (gs.gc[i].claimedRoleScheduledSkillTargets[j][gs.gameDay]) {
                                isSelectedVoteTargetResult.append(getJobText(gs.gc[j].number) + ",");
                            }
                        }
                        isSelectedVoteTargetResult.append("\n");
                    }
                }
                int vv = 0;
                for (int j = 1; j < gs.gc.length; ++j) {
                    if (gs.hiddenHunterScheduledSkillTargets[j][gs.gameDay]) {
                        if(vv++ == 0) isSelectedVoteTargetResult.append("潜伏→");
                        isSelectedVoteTargetResult.append(getJobText(gs.gc[j].number) + ",");
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
            scalableComponents.add(new ScalableComponent(scrollPane1,40.0/1280,228.0/720,
                    (200+boardImage.getIconWidth() - 80)/1280.0,(50+boardImage.getIconHeight()- 60)/720.0,
                    null));

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
                greyText.append(getJobText(gs.gc[cxList.get(i)].number));
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
//            //测试Event!!!!
//            Event event = new Event();
//            event.eventname = EventName.yjsw;
//            event.ch1 = CharacterEnglishName.Abel;
//            Event event1 = new Event();
//            event1.eventname = EventName.yjsw;
//            event1.ch1 = CharacterEnglishName.Carmen;
//            addEvent(event);
//            addEvent(event1);
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
                    isSelectedVoteTargetText.append(getJobText(gs.gc[chuxingList.get(i)].number)).append(",");
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

        JPanel infoPanel = new JPanel();//用于存头像按钮
        infoPanel.setLayout(null); // 关键！确保手动设置的坐标生效
        JPanel infoZhanPanel = new JPanel();//用于存头像按钮
        infoZhanPanel.setLayout(null); // 关键！确保手动设置的坐标生效
        JPanel infoHuPanel = new JPanel();//用于存头像按钮
        infoHuPanel.setLayout(null); // 关键！确保手动设置的坐标生效
        JPanel infoCoPanel = new JPanel();//用于存头像按钮
        infoCoPanel.setLayout(null); // 关键！确保手动设置的坐标生效
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
                    if(i <= (gs.gc.length - 1 + 1)/2){
                        //职业
                        scalableComponents.add(new ScalableComponent(claimedRoleLabel,((60+74 * i)/1280.0),20.0/720,
                                claimedRoleIcon.getIconWidth()/1280.0
                                ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(claimedRoleLabel,(60+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                                ,claimedRoleIcon.getIconWidth()/1280.0
                                ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                    }
                    infoCoPanel.add(claimedRoleLabel);
                }//职业图标

                //标记
                ImageIcon chooseIcon = resources.getImage("frameSRed.png");
                JLabel chooseLabel = new JLabel(chooseIcon);
                frameLabels.add(chooseLabel);
                if(i <= (gs.gc.length - 1 + 1)/2){

                    scalableComponents.add(new ScalableComponent(chooseLabel,((60+74 * i)/1280.0),20.0/720,
                            chooseIcon.getIconWidth()/1280.0
                            ,chooseIcon.getIconHeight()/720.0,chooseIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(chooseLabel,(60+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                            ,chooseIcon.getIconWidth()/1280.0
                            ,chooseIcon.getIconHeight()/720.0,chooseIcon.getImage()));
                }
                infoCoPanel.add(chooseLabel);
                chooseLabel.setVisible(false);
                //投票标记
                ImageIcon voteIcon = resources.getImage("result2_all.png");
                JLabel voteLabel = new JLabel(voteIcon);

                if(i <= (gs.gc.length - 1 + 1)/2){
                    //all标记
                    scalableComponents.add(new ScalableComponent(voteLabel,((60+5+74 * i)/1280.0),20.0/720,
                            voteIcon.getIconWidth()/1280.0
                            ,voteIcon.getIconHeight()/720.0,voteIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(voteLabel,(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                            ,voteIcon.getIconWidth()/1280.0
                            ,voteIcon.getIconHeight()/720.0,voteIcon.getImage()));
                }
                infoCoPanel.add(voteLabel);
                infoCoPanel.setComponentZOrder(voteLabel,0);
                voteLabel.setVisible(false);
                if(gs.gc[i].isSelectedVoteTarget[gs.gameDay]) voteLabel.setVisible(true);

                //all标记
                ImageIcon voteAllIcon = resources.getImage("result1_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);

                if(i <= (gs.gc.length - 1 + 1)/2){
                    //all标记
                    scalableComponents.add(new ScalableComponent(voteAllLabel,((60+5+74 * i)/1280.0),40.0/720,
                            voteAllIcon.getIconWidth()/1280.0
                            ,voteAllIcon.getIconHeight()/720.0,voteAllIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(voteAllLabel,(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,148.0/720.0
                            ,voteAllIcon.getIconWidth()/1280.0
                            ,voteAllIcon.getIconHeight()/720.0,voteAllIcon.getImage()));
                }
                infoCoPanel.add(voteAllLabel);
                resultLabels.add(voteAllLabel);
                voteAllLabel.setVisible(false);

                //占卜标记
                if(i < zhanbuNum.size()+1) {
                    for (int i2 = 1; i2 < gs.gc.length; ++i2) {
                        ImageIcon zbIcon = resources.getImage("result1_" + zhanbuOrder.get(i-1) + "white.png");
                        JLabel zbLabel = new JLabel(zbIcon);
                        zbLabels.add(zbLabel);
                        if (i2 <= (gs.gc.length - 1 + 1) / 2) {
                            //all标记
                            scalableComponents.add(new ScalableComponent(zbLabel, ((60 + 5 + zbIcon.getIconWidth() + 74 * i2) / 1280.0), (20.0 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1)) / 720.0,
                                    zbIcon.getIconWidth() / 1280.0
                                    , zbIcon.getIconHeight() / 720.0, zbIcon.getImage()));
                        } else {
                            scalableComponents.add(new ScalableComponent(zbLabel, (60 + 5 + zbIcon.getIconWidth() + 74 * (i2 - ((gs.gc.length - 1 + 1) / 2))) / 1280.0, (128.0 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1)) / 720.0
                                    , zbIcon.getIconWidth() / 1280.0
                                    , zbIcon.getIconHeight() / 720.0, zbIcon.getImage()));
                        }
                        infoCoPanel.add(zbLabel);
                        zbLabel.setVisible(false);
                    }
                }
                //头像
                ImageIcon characterImage = resources.getImage(imageName.toString());
                JLabel label = new JLabel(characterImage);
                targetLabels.add(label);
                if(i <= (gs.gc.length - 1 + 1)/2){

                    scalableComponents.add(new ScalableComponent(label,(60+(characterImage.getIconWidth()+10) * i)/1280.0,20.0/720,characterImage.getIconWidth()/1280.0
                            ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(label,(60+(characterImage.getIconWidth()+10) * (i - ((gs.gc.length - 1+1)/2)))/1280.0,(30.0+characterImage.getIconHeight())/720.0
                            ,characterImage.getIconWidth()/1280.0
                            ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                }
                infoCoPanel.add(label);


            }
            for(int k = 2;k <= gs.gameDay;++k){
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
                    if(i1 <= (gs.gc.length - 1 + 1)/2){
                        //职业
                        scalableComponents.add(new ScalableComponent(skillTargetLabel,((50+74 * (i1+1))- skillTargetIcon.getIconWidth()*zynum)/1280.0,(20.0 + (order - 1)*skillTargetIcon.getIconHeight())/720,
                                skillTargetIcon.getIconWidth()/1280.0
                                ,skillTargetIcon.getIconHeight()/720.0,skillTargetIcon.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(skillTargetLabel,((50+74 * (i1 +1 - ((gs.gc.length - 1+1)/2))) - skillTargetIcon.getIconWidth()*zynum)/1280.0,(128.0 + (order-1)*skillTargetIcon.getIconHeight())/720.0
                                ,skillTargetIcon.getIconWidth()/1280.0
                                ,skillTargetIcon.getIconHeight()/720.0,skillTargetIcon.getImage()));
                    }
                    infoCoPanel.add(skillTargetLabel);
                    infoCoPanel.setComponentZOrder(skillTargetLabel,0);
                }
            }
            scalableComponents.add(new ScalableComponent(infoCoPanel,0.0/1280,198.0/720,
                    (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                    null));

            JLabel infoBoard = new JLabel(boardImage);
            scalableComponents.add(new ScalableComponent(infoBoard,0.0/1280,0.0/720,
                    (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                    boardImage.getImage()));

            ImageIcon dragIcon = resources.getImage("uranaiAll.png");
            JButton dragBtn = createDraggableButton();
            dragBtn.setIcon(dragIcon);
            btnSet(dragBtn);
            scalableComponents.add(new ScalableComponent(dragBtn,250.0/1280,350/720.0,dragIcon.getIconWidth()/2.0/1280,dragIcon.getIconHeight()/2.0/720,
                    dragIcon.getImage()));
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
            JButton dragBtn_delete = createDraggableButton();
            dragBtn_delete.setIcon(dragIcon);
            btnSet(dragBtn_delete);
            scalableComponents.add(new ScalableComponent(dragBtn_delete,800.0/1280,350/720.0,dragIcon.getIconWidth()/2.0/1280,dragIcon.getIconHeight()/2.0/720,
                    dragIcon.getImage()));
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
                    if(i <= (gs.gc.length - 1 + 1)/2){
                        //职业
                        scalableComponents.add(new ScalableComponent(claimedRoleLabel,((60+74 * i)/1280.0),20.0/720,
                                claimedRoleIcon.getIconWidth()/1280.0
                                ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(claimedRoleLabel,(60+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                                ,claimedRoleIcon.getIconWidth()/1280.0
                                ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                    }
                    infoPanel.add(claimedRoleLabel);
                }//职业图标

                //标记 需要记录下来，然后下次进入的时候如果i是标记过的直接设置成true
                ImageIcon chooseIcon = resources.getImage("frameSBlue.png");
                JLabel chooseLabel = new JLabel(chooseIcon);
                frameLabels.add(chooseLabel);
                if(i <= (gs.gc.length - 1 + 1)/2){
                    //职业
                    scalableComponents.add(new ScalableComponent(chooseLabel,((60+74 * i)/1280.0),20.0/720,
                            chooseIcon.getIconWidth()/1280.0
                            ,chooseIcon.getIconHeight()/720.0,chooseIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(chooseLabel,(60+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                            ,chooseIcon.getIconWidth()/1280.0
                            ,chooseIcon.getIconHeight()/720.0,chooseIcon.getImage()));
                }
                infoPanel.add(chooseLabel);
                chooseLabel.setVisible(false);
                if(voteChosen.contains(i)){
                    //如果有i，则显示
                    chooseLabel.setVisible(true);
                }
                //all标记
                ImageIcon voteAllIcon = resources.getImage("result2_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);

                if(i <= (gs.gc.length - 1 + 1)/2){
                    //all标记
                    scalableComponents.add(new ScalableComponent(voteAllLabel,((60+5+74 * i)/1280.0),30.0/720,
                            voteAllIcon.getIconWidth()/1280.0
                            ,voteAllIcon.getIconHeight()/720.0,voteAllIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(voteAllLabel,(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,138.0/720.0
                            ,voteAllIcon.getIconWidth()/1280.0
                            ,voteAllIcon.getIconHeight()/720.0,voteAllIcon.getImage()));
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
                if(i <= (gs.gc.length - 1 + 1)/2){

                    scalableComponents.add(new ScalableComponent(label,(60+(characterImage.getIconWidth()+10) * i)/1280.0,20.0/720,characterImage.getIconWidth()/1280.0
                            ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(label,(60+(characterImage.getIconWidth()+10) * (i - ((gs.gc.length - 1+1)/2)))/1280.0,(30.0+characterImage.getIconHeight())/720.0
                            ,characterImage.getIconWidth()/1280.0
                            ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                }
                infoPanel.add(label);
            }
            for(int k = 2;k <= gs.gameDay;++k){
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
                    if(i1 <= (gs.gc.length - 1 + 1)/2){
                        //职业
                        scalableComponents.add(new ScalableComponent(skillTargetLabel,((50+74 * (i1+1))- skillTargetIcon.getIconWidth()*zynum)/1280.0,(20.0 + (order - 1)*skillTargetIcon.getIconHeight())/720,
                                skillTargetIcon.getIconWidth()/1280.0
                                ,skillTargetIcon.getIconHeight()/720.0,skillTargetIcon.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(skillTargetLabel,((50+74 * (i1 +1 - ((gs.gc.length - 1+1)/2))) - skillTargetIcon.getIconWidth()*zynum)/1280.0,(128.0 + (order-1)*skillTargetIcon.getIconHeight())/720.0
                                ,skillTargetIcon.getIconWidth()/1280.0
                                ,skillTargetIcon.getIconHeight()/720.0,skillTargetIcon.getImage()));
                    }
                    infoPanel.add(skillTargetLabel);
                    infoPanel.setComponentZOrder(skillTargetLabel,0);
                }
            }

            scalableComponents.add(new ScalableComponent(infoPanel,0.0/1280,198.0/720,
                    (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                    null));

            JLabel infoBoard = new JLabel(boardImage);
            scalableComponents.add(new ScalableComponent(infoBoard,0.0/1280,0.0/720,
                    (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                    boardImage.getImage()));

            ImageIcon dragIcon = resources.getImage("touhyou.png");
            JButton dragBtn = createDraggableButton();
            dragBtn.setIcon(dragIcon);
            btnSet(dragBtn);
            scalableComponents.add(new ScalableComponent(dragBtn,250.0/1280,350/720.0,dragIcon.getIconWidth()/2.0/1280,dragIcon.getIconHeight()/2.0/720,
                    dragIcon.getImage()));
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
                                if (gs.gc[a].isSelectedVoteTarget[gs.gameDay]) System.out.println(getJobText(gs.gc[a].number));
                            }
                        }
                    }
                }
            });
            infoPanel.add(dragBtn);

            dragIcon = resources.getImage("delete.png");
            JButton dragBtn_delete = createDraggableButton();
            dragBtn_delete.setIcon(dragIcon);
            btnSet(dragBtn_delete);
            scalableComponents.add(new ScalableComponent(dragBtn_delete,500.0/1280,350/720.0,dragIcon.getIconWidth()/2.0/1280,dragIcon.getIconHeight()/2.0/720,
                    dragIcon.getImage()));
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
                                if (gs.gc[a].isSelectedVoteTarget[gs.gameDay]) System.out.println(getJobText(gs.gc[a].number));
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
                    if(i <= (gs.gc.length - 1 + 1)/2){
                        //职业
                        scalableComponents.add(new ScalableComponent(claimedRoleLabel,((60+74 * i)/1280.0),20.0/720,
                                claimedRoleIcon.getIconWidth()/1280.0
                                ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(claimedRoleLabel,(60+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                                ,claimedRoleIcon.getIconWidth()/1280.0
                                ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                    }
                    infoZhanPanel.add(claimedRoleLabel);
                }//职业图标

                //标记
                ImageIcon chooseIcon = resources.getImage("frameSRed.png");
                JLabel chooseLabel = new JLabel(chooseIcon);
                frameLabels.add(chooseLabel);
                if(i <= (gs.gc.length - 1 + 1)/2){
                    //职业
                    scalableComponents.add(new ScalableComponent(chooseLabel,((60+74 * i)/1280.0),20.0/720,
                            chooseIcon.getIconWidth()/1280.0
                            ,chooseIcon.getIconHeight()/720.0,chooseIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(chooseLabel,(60+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                            ,chooseIcon.getIconWidth()/1280.0
                            ,chooseIcon.getIconHeight()/720.0,chooseIcon.getImage()));
                }
                infoZhanPanel.add(chooseLabel);
                chooseLabel.setVisible(false);
                if(zhanChosen.contains(i)){
                    //如果有i，则显示
                    chooseLabel.setVisible(true);
                }
                //投票标记
                ImageIcon voteIcon = resources.getImage("result2_all.png");
                JLabel voteLabel = new JLabel(voteIcon);

                if(i <= (gs.gc.length - 1 + 1)/2){
                    //all标记
                    scalableComponents.add(new ScalableComponent(voteLabel,((60+5+74 * i)/1280.0),20.0/720,
                            voteIcon.getIconWidth()/1280.0
                            ,voteIcon.getIconHeight()/720.0,voteIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(voteLabel,(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                            ,voteIcon.getIconWidth()/1280.0
                            ,voteIcon.getIconHeight()/720.0,voteIcon.getImage()));
                }
                infoZhanPanel.add(voteLabel);
                infoZhanPanel.setComponentZOrder(voteLabel,0);
                voteLabel.setVisible(false);
                if(gs.gc[i].isSelectedVoteTarget[gs.gameDay]) voteLabel.setVisible(true);
                //all标记
                ImageIcon voteAllIcon = resources.getImage("result1_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);
                resultLabels.add(voteAllLabel);
                if(i <= (gs.gc.length - 1 + 1)/2){
                    //all标记
                    scalableComponents.add(new ScalableComponent(voteAllLabel,((60+5+74 * i)/1280.0),40.0/720,
                            voteAllIcon.getIconWidth()/1280.0
                            ,voteAllIcon.getIconHeight()/720.0,voteAllIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(voteAllLabel,(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,148.0/720.0
                            ,voteAllIcon.getIconWidth()/1280.0
                            ,voteAllIcon.getIconHeight()/720.0,voteAllIcon.getImage()));
                }
                infoZhanPanel.add(voteAllLabel);
                voteAllLabel.setVisible(false);
                if(zhanChosen.contains(i)){
                    //如果有i，则显示
                    voteAllLabel.setVisible(true);
                }
                //占卜标记
                if(i < zhanbuNum.size()+1) {
                    for (int i2 = 1; i2 < gs.gc.length; ++i2) {
                        ImageIcon zbIcon = resources.getImage("result1_" + zhanbuOrder.get(i-1) + "white.png");
                        JLabel zbLabel = new JLabel(zbIcon);
                        zbLabels.add(zbLabel);
                        if (i2 <= (gs.gc.length - 1 + 1) / 2) {
                            //all标记
                            scalableComponents.add(new ScalableComponent(zbLabel, ((60 + 5 + zbIcon.getIconWidth() + 74 * i2) / 1280.0), (20.0 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1)) / 720.0,
                                    zbIcon.getIconWidth() / 1280.0
                                    , zbIcon.getIconHeight() / 720.0, zbIcon.getImage()));
                        } else {
                            scalableComponents.add(new ScalableComponent(zbLabel, (60 + 5 + zbIcon.getIconWidth() + 74 * (i2 - ((gs.gc.length - 1 + 1) / 2))) / 1280.0, (128.0 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1)) / 720.0
                                    , zbIcon.getIconWidth() / 1280.0
                                    , zbIcon.getIconHeight() / 720.0, zbIcon.getImage()));
                        }
                        infoZhanPanel.add(zbLabel);
                        zbLabel.setVisible(false);
                        if(zhanChosen.contains(i)){
                            //如果有i，则显示
                            zbLabel.setVisible(true);
                        }
                    }
                }
                    //头像
                    ImageIcon characterImage = resources.getImage(imageName.toString());
                    JLabel label = new JLabel(characterImage);
                    targetLabels.add(label);
                    if(i <= (gs.gc.length - 1 + 1)/2){

                        scalableComponents.add(new ScalableComponent(label,(60+(characterImage.getIconWidth()+10) * i)/1280.0,20.0/720,characterImage.getIconWidth()/1280.0
                                ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(label,(60+(characterImage.getIconWidth()+10) * (i - ((gs.gc.length - 1+1)/2)))/1280.0,(30.0+characterImage.getIconHeight())/720.0
                                ,characterImage.getIconWidth()/1280.0
                                ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                    }
                    infoZhanPanel.add(label);


            }
            for(int k = 2;k <= gs.gameDay;++k){
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
                    if(i1 <= (gs.gc.length - 1 + 1)/2){
                        //职业
                        scalableComponents.add(new ScalableComponent(skillTargetLabel,((50+74 * (i1+1))- skillTargetIcon.getIconWidth()*zynum)/1280.0,(20.0 + (order - 1)*skillTargetIcon.getIconHeight())/720,
                                skillTargetIcon.getIconWidth()/1280.0
                                ,skillTargetIcon.getIconHeight()/720.0,skillTargetIcon.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(skillTargetLabel,((50+74 * (i1 +1 - ((gs.gc.length - 1+1)/2))) - skillTargetIcon.getIconWidth()*zynum)/1280.0,(128.0 + (order-1)*skillTargetIcon.getIconHeight())/720.0
                                ,skillTargetIcon.getIconWidth()/1280.0
                                ,skillTargetIcon.getIconHeight()/720.0,skillTargetIcon.getImage()));
                    }
                    infoZhanPanel.add(skillTargetLabel);
                    infoZhanPanel.setComponentZOrder(skillTargetLabel,0);
                }
            }

            scalableComponents.add(new ScalableComponent(infoZhanPanel,0.0/1280,198.0/720,
                    (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                    null));

            JLabel infoBoard = new JLabel(boardImage);
            scalableComponents.add(new ScalableComponent(infoBoard,0.0/1280,0.0/720,
                    (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                    boardImage.getImage()));

            ImageIcon dragIcon = resources.getImage("uranaiAll.png");
            JButton dragBtn = createDraggableButton();
            dragBtn.setIcon(dragIcon);
            btnSet(dragBtn);
            scalableComponents.add(new ScalableComponent(dragBtn,150.0/1280,350/720.0,dragIcon.getIconWidth()/2.0/1280,dragIcon.getIconHeight()/2.0/720,
                    dragIcon.getImage()));
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
                                                System.out.println(getJobText(gs.gc[hh].number) + "预告了" + getJobText(gs.gc[t].number));
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
                JButton Btn = createDraggableButton();
                Btn.setIcon(Icon1);
                btnSet(Btn);
                scalableComponents.add(new ScalableComponent(Btn,(zhanbuOrder.get(cur)*100+150)/1280.0,350/720.0,Icon1.getIconWidth()/2.0/1280,Icon1.getIconHeight()/2.0/720,
                        Icon1.getImage()));
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
                                                    System.out.println(getJobText(gs.gc[hh].number) + "预告了" + getJobText(gs.gc[t].number));
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
            JButton dragBtn_delete = createDraggableButton();
            dragBtn_delete.setIcon(dragIcon);
            btnSet(dragBtn_delete);
            scalableComponents.add(new ScalableComponent(dragBtn_delete,800.0/1280,350/720.0,dragIcon.getIconWidth()/2.0/1280,dragIcon.getIconHeight()/2.0/720,
                    dragIcon.getImage()));
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
                                                System.out.println(getJobText(gs.gc[hh].number) + "预告了" + getJobText(gs.gc[t].number));
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
            for (int i = 1; i <= gs.gc.length - 1; i++) {
                if(gs.gc[i].claimedRole == 3&&gs.gc[i].whyDie == whyDie.NONE){
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
                    if(i <= (gs.gc.length - 1 + 1)/2){
                        //职业
                        scalableComponents.add(new ScalableComponent(claimedRoleLabel,((60+74 * i)/1280.0),20.0/720,
                                claimedRoleIcon.getIconWidth()/1280.0
                                ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(claimedRoleLabel,(60+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                                ,claimedRoleIcon.getIconWidth()/1280.0
                                ,claimedRoleIcon.getIconHeight()/720.0,claimedRoleIcon.getImage()));
                    }
                    infoHuPanel.add(claimedRoleLabel);
                }//职业图标

                //标记
                ImageIcon chooseIcon = resources.getImage("frameOrange.png");
                JLabel chooseLabel = new JLabel(chooseIcon);
                frameLabels.add(chooseLabel);
                if(i <= (gs.gc.length - 1 + 1)/2){
                    //职业
                    scalableComponents.add(new ScalableComponent(chooseLabel,((60+74 * i)/1280.0),20.0/720,
                            64/1280.0
                            ,98/720.0,chooseIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(chooseLabel,(60+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                            ,64/1280.0
                            ,98/720.0,chooseIcon.getImage()));
                }
                infoHuPanel.add(chooseLabel);
                chooseLabel.setVisible(false);

                if(huChosen.contains(i)){
                    //如果有i，则显示
                    chooseLabel.setVisible(true);
                }
                //投票标记
                ImageIcon voteIcon = resources.getImage("result2_all.png");
                JLabel voteLabel = new JLabel(voteIcon);

                if(i <= (gs.gc.length - 1 + 1)/2){
                    //all标记
                    scalableComponents.add(new ScalableComponent(voteLabel,((60+5+74 * i)/1280.0),20.0/720,
                            voteIcon.getIconWidth()/1280.0
                            ,voteIcon.getIconHeight()/720.0,voteIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(voteLabel,(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,128.0/720.0
                            ,voteIcon.getIconWidth()/1280.0
                            ,voteIcon.getIconHeight()/720.0,voteIcon.getImage()));
                }
                infoHuPanel.add(voteLabel);
                infoHuPanel.setComponentZOrder(voteLabel,0);
                voteLabel.setVisible(false);
                if(gs.gc[i].isSelectedVoteTarget[gs.gameDay]) voteLabel.setVisible(true);

                //all标记
                ImageIcon voteAllIcon = resources.getImage("result3_all.png");
                JLabel voteAllLabel = new JLabel(voteAllIcon);
                resultLabels.add(voteAllLabel);
                if(i <= (gs.gc.length - 1 + 1)/2){
                    //all标记
                    scalableComponents.add(new ScalableComponent(voteAllLabel,((60+5+74 * i)/1280.0),40.0/720,
                            voteAllIcon.getIconWidth()/1280.0
                            ,voteAllIcon.getIconHeight()/720.0,voteAllIcon.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(voteAllLabel,(60+5+74 * (i  - ((gs.gc.length - 1+1)/2)))/1280.0,148.0/720.0
                            ,voteAllIcon.getIconWidth()/1280.0
                            ,voteAllIcon.getIconHeight()/720.0,voteAllIcon.getImage()));
                }
                infoHuPanel.add(voteAllLabel);
                voteAllLabel.setVisible(false);
                if(huChosen.contains(i)){
                    //如果有i，则显示
                    voteAllLabel.setVisible(true);
                }
                //占卜标记
                if(i < zhanbuNum.size()+1) {
                    for (int i2 = 1; i2 < gs.gc.length; ++i2) {
                        ImageIcon zbIcon = resources.getImage("result3_" + zhanbuOrder.get(i-1) + ".png");
                        JLabel zbLabel = new JLabel(zbIcon);
                        zbLabels.add(zbLabel);
                        if (i2 <= (gs.gc.length - 1 + 1) / 2) {
                            //all标记
                            scalableComponents.add(new ScalableComponent(zbLabel, ((60 + 5 + zbIcon.getIconWidth() + 74 * i2) / 1280.0), (20.0 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1)) / 720.0,
                                    zbIcon.getIconWidth() / 1280.0
                                    , zbIcon.getIconHeight() / 720.0, zbIcon.getImage()));
                        } else {
                            scalableComponents.add(new ScalableComponent(zbLabel, (60 + 5 + zbIcon.getIconWidth() + 74 * (i2 - ((gs.gc.length - 1 + 1) / 2))) / 1280.0, (128.0 + zbIcon.getIconHeight() * zhanbuOrder.get(i-1)) / 720.0
                                    , zbIcon.getIconWidth() / 1280.0
                                    , zbIcon.getIconHeight() / 720.0, zbIcon.getImage()));
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
                if(i <= (gs.gc.length - 1 + 1)/2){

                    scalableComponents.add(new ScalableComponent(label,(60+(characterImage.getIconWidth()+10) * i)/1280.0,20.0/720,characterImage.getIconWidth()/1280.0
                            ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(label,(60+(characterImage.getIconWidth()+10) * (i - ((gs.gc.length - 1+1)/2)))/1280.0,(30.0+characterImage.getIconHeight())/720.0
                            ,characterImage.getIconWidth()/1280.0
                            ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                }
                infoHuPanel.add(label);


            }
            for(int k = 2;k <= gs.gameDay;++k){
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
                    if(i1 <= (gs.gc.length - 1 + 1)/2){
                        //职业
                        scalableComponents.add(new ScalableComponent(skillTargetLabel,((50+74 * (i1+1))- skillTargetIcon.getIconWidth()*zynum)/1280.0,(20.0 + (order - 1)*skillTargetIcon.getIconHeight())/720,
                                skillTargetIcon.getIconWidth()/1280.0
                                ,skillTargetIcon.getIconHeight()/720.0,skillTargetIcon.getImage()));
                    }
                    else{
                        scalableComponents.add(new ScalableComponent(skillTargetLabel,((50+74 * (i1 +1 - ((gs.gc.length - 1+1)/2))) - skillTargetIcon.getIconWidth()*zynum)/1280.0,(128.0 + (order-1)*skillTargetIcon.getIconHeight())/720.0
                                ,skillTargetIcon.getIconWidth()/1280.0
                                ,skillTargetIcon.getIconHeight()/720.0,skillTargetIcon.getImage()));
                    }
                    infoHuPanel.add(skillTargetLabel);
                    infoHuPanel.setComponentZOrder(skillTargetLabel,0);
                }
            }

            scalableComponents.add(new ScalableComponent(infoHuPanel,0.0/1280,198.0/720,
                    (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                    null));

            JLabel infoBoard = new JLabel(boardImage);
            scalableComponents.add(new ScalableComponent(infoBoard,0.0/1280,0.0/720,
                    (200 + boardImage.getIconWidth())/1280.0,(50+boardImage.getIconHeight())/720.0,
                    boardImage.getImage()));

            ImageIcon dragIcon = resources.getImage("goeiAll.png");
            JButton dragBtn = createDraggableButton();
            dragBtn.setIcon(dragIcon);
            btnSet(dragBtn);
            scalableComponents.add(new ScalableComponent(dragBtn,250.0/1280,350/720.0,dragIcon.getIconWidth()/2.0/1280,dragIcon.getIconHeight()/2.0/720,
                    dragIcon.getImage()));
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
                                                System.out.println(getJobText(gs.gc[hh].number) + "预告了" + getJobText(gs.gc[t].number));
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

            while(arr[0] < zhanbuNum.size()){
                int cur = arr[0];
                System.out.println(cur);
                System.out.println(zhanbuNum.size());
                ImageIcon Icon1 = resources.getImage("goei" + zhanbuOrder.get(cur)+".png");
                JButton Btn = createDraggableButton();
                Btn.setIcon(Icon1);
                btnSet(Btn);
                scalableComponents.add(new ScalableComponent(Btn,(zhanbuOrder.get(cur)*150+250)/1280.0,350/720.0,Icon1.getIconWidth()/2.0/1280,Icon1.getIconHeight()/2.0/720,
                        Icon1.getImage()));
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
                                                    System.out.println(getJobText(gs.gc[hh].number) + "预告了" + getJobText(gs.gc[t].number));
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
            JButton dragBtn_delete = createDraggableButton();
            dragBtn_delete.setIcon(dragIcon);
            btnSet(dragBtn_delete);
            scalableComponents.add(new ScalableComponent(dragBtn_delete,800.0/1280,350/720.0,dragIcon.getIconWidth()/2.0/1280,dragIcon.getIconHeight()/2.0/720,
                    dragIcon.getImage()));
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
                                                System.out.println(getJobText(gs.gc[hh].number) + "预告了" + getJobText(gs.gc[t].number));
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

    public void createTishi(String str){
        JTextArea tishiText = new JTextArea();
        tishiText.setText(str);
        tishiText.setForeground(Color.WHITE);
        tishiText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        tishiText.setLineWrap(true);       // 自动换行
        tishiText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        tishiText.setEditable(false);
        tishiText.setBackground(new Color(0,0,0,180));
        tishiText.setBorder(BorderFactory.createEmptyBorder());
        scalableComponents.add(new ScalableComponent(tishiText,300.0/1280,300.0/720,500.0/1280,200/720.0,null));
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
        for (int i = 1; i < gs.gc.length; ++i) {
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
                        extraText.append(getJobText(gs.gc[greyCharas[i][gameDay]].number));
                    }
                }
                break;
            case 2://指定
                extraText.append("指定投票：\n");
                for(int i = 0;i < gs.gc.length;++i){
                    if(isSelectedVoteTargetCharas[i][gameDay] != 0){
                        extraText.append(getJobText(gs.gc[isSelectedVoteTargetCharas[i][gameDay]].number)).append(",");
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
                    rightPiao.append(getJobText(gs.gc[i].number)).append("：").append( voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(getJobText(gs.gc[gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    //有10个了就去右边
                }
                else {
                    leftPiao.append(getJobText(gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(getJobText(gs.gc[gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
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
        scalableComponents.add(new ScalableComponent(piaoText,40.0/1280,228.0/720,
                (1000)/1280.0,(430)/720.0,
                null));
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
        scalableComponents.add(new ScalableComponent(piaoText1,400.0/1280,228.0/720,
                (450)/1280.0,(430)/720.0,
                null));
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
                    rightPiao.append(getJobText(gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(getJobText(gs.gc[gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    //有10个了就去右边
                } else {
                    leftPiao.append(getJobText(gs.gc[i].number)).append("：").append(voteTotal[i][round]).append("票  ")
                            .append("投票先→").append(getJobText(gs.gc[gs.gc[i].voteTarget[gameDay][round]].number)).append("\n");
                    leftCnt++;
                }
            }


            if(i == gs.gc.length - 1) {
                for (int i1 = 0; i1 < 10 - leftCnt; ++i1) {
                    leftPiao.append("\n");
                }
                //4 票で、剣士さんが処刑されました。
                if (maxCnt == 1) {
                    leftPiao.append(max).append("票で").append(getJobText(gs.gc[maxPos.get(0)].number)).append("さんが処刑されました。");
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
        scalableComponents.add(new ScalableComponent(piaoText,40.0/1280,228.0/720,
                (900)/1280.0,(430)/720.0,
                null));
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
        scalableComponents.add(new ScalableComponent(piaoText1,530.0/1280,228.0/720,
                (450)/1280.0,(430)/720.0,
                null));
        jPanel.add(piaoText1);
        jPanel.setComponentZOrder(piaoText1,0);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
    }//显示票型，需要传入首行内容，是第几轮投票，是否重投
    public void createDoubt(){
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
                            if (u == 1) rightPiao.append(getJobText(gs.gc[i].number)).append("：");

                            rightPiao.append(getJobText(gs.gc[gs.gc[i].top3SuspectedPlayers[u][gs.gameDay]].number));
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
                            if(u == 1)leftPiao.append(getJobText(gs.gc[i].number)).append("：");
                            leftPiao.append(getJobText(gs.gc[gs.gc[i].top3SuspectedPlayers[u][gs.gameDay]].number));
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
        scalableComponents.add(new ScalableComponent(piaoText,40.0/1280,228.0/720,
                (900)/1280.0,(430)/720.0,
                null));
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
        scalableComponents.add(new ScalableComponent(piaoText1,530.0/1280,228.0/720,
                (450)/1280.0,(430)/720.0,
                null));
        jPanel.add(piaoText1);
        jPanel.setComponentZOrder(piaoText1,0);
        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
    }//显示怀疑度

    public void end_village(){
        Event event = events.poll();
        while(event.eventname != EventName.crsl){
            event = events.poll();
        }
        resources.playBgm("胜利画面.wav");
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        //背景图片
        ImageIcon bgIcon = resources.getImage("endVillage.png");
        JLabel background = new JLabel(bgIcon);
        background.setOpaque(false);
        background.setFocusable(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));


        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(null);
        dialogPanel.setOpaque(false);
        dialogPanel.setBackground(new Color(0, 0, 0, 0));
        scalableComponents.add(new ScalableComponent(
                dialogPanel, 260.0 / 1280, 450.0 / 720,
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                null
        ));
        diaPanel.add(dialogPanel);     // 顶层：对话框面板



        JLabel back = new JLabel(backIcon);
        scalableComponents.add(new ScalableComponent(
                back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                backIcon.getImage()
        ));

        // 角色名称标签（添加到对话框面板）
        JLabel nameLabel = new JLabel();
        if(event.ch1!=null) {
            nameLabel.setText(getCharacterFullName(event.ch1));
        }
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        nameLabel.setOpaque(false);
        dialogPanel.add(nameLabel);  // 添加到对话框面板
        scalableComponents.add(new ScalableComponent(
                nameLabel, (20) / 1280.0, (10) / 720.0,  // 基于窗口的绝对位置
                1000.0 / 1280, 30.0 / 720,
                null
        ));

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = new JTextArea();
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        dialogText.setLineWrap(true);
        dialogText.setWrapStyleWord(true);
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setFocusable(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        scalableComponents.add(new ScalableComponent(
                dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                null
        ));

        JButton nextBtn = new JButton();
        btnSet(nextBtn);

        scalableComponents.add(new ScalableComponent(
                nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                null
        ));

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
        if(!linkIcon.isEmpty()) {
            //是接连发生的事件则一左一右
            //待修改
            //展示第一个，话说完了点击再展示第二个
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara, 650/ 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                    CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                    CharIcon[0].getImage()
            ));
            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();

            JLabel Chara2 = new JLabel(linkIcon.get(0));

            Chara2.setOpaque(false);
            Chara2.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara2, 300 / 1280.0, (720 - linkIcon.get(0).getIconHeight() - 30) / 720.0,
                    linkIcon.get(0).getIconWidth() / 1280.0, linkIcon.get(0).getIconHeight() / 720.0,
                    linkIcon.get(0).getImage()
            ));
            diaPanel.add(Chara2);
            diaPanel.setComponentZOrder(Chara2, 1);

            linkIcon.remove(0);
        }
        //其他
        else{
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            boolean isLinked = false;
            switch(event.eventname){
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case gprz11p:
                case zcrh12:
                    isLinked = true;
                    break;
            }
            if(isLinked){
                scalableComponents.add(new ScalableComponent(
                        Chara, 300 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
                linkIcon.add(CharIcon[0]);
            }
            else {
                scalableComponents.add(new ScalableComponent(
                        Chara, (1280 - CharIcon[0].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
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
                resources.playSound("村人胜利音效.wav");
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


    }//村胜
    public void end_wolf(){
        Event event = events.poll();
        while(event.eventname != EventName.krsl && event.eventname != EventName.rlsl){
            event = events.poll();
        }
        resources.playBgm("失败画面.wav");
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        //背景图片
        ImageIcon bgIcon = resources.getImage("endWolf.png");
        JLabel background = new JLabel(bgIcon);
        background.setOpaque(false);
        background.setFocusable(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));


        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(null);
        dialogPanel.setOpaque(false);
        dialogPanel.setBackground(new Color(0, 0, 0, 0));
        scalableComponents.add(new ScalableComponent(
                dialogPanel, 260.0 / 1280, 450.0 / 720,
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                null
        ));
        diaPanel.add(dialogPanel);     // 顶层：对话框面板



        JLabel back = new JLabel(backIcon);
        scalableComponents.add(new ScalableComponent(
                back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                backIcon.getImage()
        ));

        // 角色名称标签（添加到对话框面板）
        JLabel nameLabel = new JLabel();
        if(event.ch1!=null) {
            nameLabel.setText(getCharacterFullName(event.ch1));
        }
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        nameLabel.setOpaque(false);
        dialogPanel.add(nameLabel);  // 添加到对话框面板
        scalableComponents.add(new ScalableComponent(
                nameLabel, (20) / 1280.0, (10) / 720.0,  // 基于窗口的绝对位置
                1000.0 / 1280, 30.0 / 720,
                null
        ));

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = new JTextArea();
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        dialogText.setLineWrap(true);
        dialogText.setWrapStyleWord(true);
        dialogText.setFocusable(false);
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        scalableComponents.add(new ScalableComponent(
                dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                null
        ));

        JButton nextBtn = new JButton();
        btnSet(nextBtn);

        scalableComponents.add(new ScalableComponent(
                nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                null
        ));

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
        if(!linkIcon.isEmpty()) {
            //是接连发生的事件则一左一右
            //待修改
            //展示第一个，话说完了点击再展示第二个
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara, 650/ 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                    CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                    CharIcon[0].getImage()
            ));
            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();

            JLabel Chara2 = new JLabel(linkIcon.get(0));

            Chara2.setOpaque(false);
            Chara2.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara2, 300 / 1280.0, (720 - linkIcon.get(0).getIconHeight() - 30) / 720.0,
                    linkIcon.get(0).getIconWidth() / 1280.0, linkIcon.get(0).getIconHeight() / 720.0,
                    linkIcon.get(0).getImage()
            ));
            diaPanel.add(Chara2);
            diaPanel.setComponentZOrder(Chara2, 1);

            linkIcon.remove(0);
        }
        //其他
        else{
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            boolean isLinked = false;
            switch(event.eventname){
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case gprz11p:
                case zcrh12:
                    isLinked = true;
                    break;
            }
            if(isLinked){
                scalableComponents.add(new ScalableComponent(
                        Chara, 300 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
                linkIcon.add(CharIcon[0]);
            }
            else {
                scalableComponents.add(new ScalableComponent(
                        Chara, (1280 - CharIcon[0].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
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
                resources.playSound("人狼胜利音效.wav");
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


    }//狼胜
    public void end_fox(){
        Event event = events.poll();
        while(event.eventname != EventName.yhsl){
            event = events.poll();
        }
        resources.playBgm("失败画面.wav");
        diaPanel.removeAll();
        diaPanel.setVisible(true);
        //背景图片
        ImageIcon bgIcon = resources.getImage("endFox.png");
        JLabel background = new JLabel(bgIcon);
        background.setOpaque(false);
        background.setFocusable(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                bgIcon.getImage()
        ));


        // 对话框背景（添加到对话框面板）
        ImageIcon backIcon = resources.getImage("messageframe.png");
        // 对话框面板
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(null);
        dialogPanel.setOpaque(false);
        dialogPanel.setBackground(new Color(0, 0, 0, 0));
        scalableComponents.add(new ScalableComponent(
                dialogPanel, 260.0 / 1280, 450.0 / 720,
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                null
        ));
        diaPanel.add(dialogPanel);     // 顶层：对话框面板



        JLabel back = new JLabel(backIcon);
        scalableComponents.add(new ScalableComponent(
                back, 0.0 / 1280, 0.0 / 720,  // 基于对话框的绝对比例
                backIcon.getIconWidth() / 1280.0, backIcon.getIconHeight() / 720.0,
                backIcon.getImage()
        ));

        // 角色名称标签（添加到对话框面板）
        JLabel nameLabel = new JLabel();
        if(event.ch1!=null) {
            nameLabel.setText(getCharacterFullName(event.ch1));
        }
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        nameLabel.setOpaque(false);
        dialogPanel.add(nameLabel);  // 添加到对话框面板
        scalableComponents.add(new ScalableComponent(
                nameLabel, (20) / 1280.0, (10) / 720.0,  // 基于窗口的绝对位置
                1000.0 / 1280, 30.0 / 720,
                null
        ));

        // 文本显示区域（添加到对话框面板）
        JTextArea dialogText = new JTextArea();
        dialogText.setFocusable(false);
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho", Font.PLAIN, 26));
        dialogText.setLineWrap(true);
        dialogText.setWrapStyleWord(true);
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        scalableComponents.add(new ScalableComponent(
                dialogText, (20) / 1280.0, (50) / 720.0,  // 基于窗口的绝对位置
                (backIcon.getIconWidth() - 50) / 1280.0, (backIcon.getIconHeight() - 30) / 720.0,
                null
        ));

        JButton nextBtn = new JButton();
        btnSet(nextBtn);

        scalableComponents.add(new ScalableComponent(
                nextBtn, 0 / 1280.0, 0 / 720.0,  // 基于窗口的绝对位置
                backIcon.getIconWidth() / 1280.0, backIcon.getIconWidth() / 720.0,
                null
        ));

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
        if(!linkIcon.isEmpty()) {
            //是接连发生的事件则一左一右
            //待修改
            //展示第一个，话说完了点击再展示第二个
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara, 650/ 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                    CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                    CharIcon[0].getImage()
            ));
            diaPanel.add(Chara);
            resizeComponents();
            diaPanel.revalidate();
            diaPanel.repaint();

            JLabel Chara2 = new JLabel(linkIcon.get(0));

            Chara2.setOpaque(false);
            Chara2.setFocusable(false);
            scalableComponents.add(new ScalableComponent(
                    Chara2, 300 / 1280.0, (720 - linkIcon.get(0).getIconHeight() - 30) / 720.0,
                    linkIcon.get(0).getIconWidth() / 1280.0, linkIcon.get(0).getIconHeight() / 720.0,
                    linkIcon.get(0).getImage()
            ));
            diaPanel.add(Chara2);
            diaPanel.setComponentZOrder(Chara2, 1);

            linkIcon.remove(0);
        }
        //其他
        else{
            Chara.setIcon(CharIcon[0]);
            Chara.setOpaque(false);
            Chara.setFocusable(false);
            boolean isLinked = false;
            switch(event.eventname){
                case gyfo1:
                case qfjc5:
                case zjgh8b:
                case zjgb8:
                case gprz11p:
                case zcrh12:
                    isLinked = true;
                    break;
            }
            if(isLinked){
                scalableComponents.add(new ScalableComponent(
                        Chara, 300 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
                linkIcon.add(CharIcon[0]);
            }
            else {
                scalableComponents.add(new ScalableComponent(
                        Chara, (1280 - CharIcon[0].getIconWidth()) / 2.0 / 1280.0, (720 - CharIcon[0].getIconHeight() - 30) / 720.0,
                        CharIcon[0].getIconWidth() / 1280.0, CharIcon[0].getIconHeight() / 720.0,
                        CharIcon[0].getImage()
                ));
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
                resources.playSound("妖狐胜利音效.wav");
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


    }//狐胜
    public void end_anime(){
        //测试测试测试111
//        gs.gc[1].dieDay = 3;
//        gs.gc[1].whyDie = whyDie.dayhouzhui;
//
//        gs.gc[14].dieDay = 5;
//        gs.gc[14].whyDie = whyDie.beiyao;
//        gs.gc[4].dieDay = 1;
//        gs.gc[4].whyDie = whyDie.zhousha;
//        gs.gc[17].dieDay = 8;
//        gs.gc[17].whyDie = whyDie.daymaozhou;

        jPanel.removeAll();
        scalableComponents.clear();

        // 背景图片
        ImageIcon backIcon = resources.getImage("frame #19252.png");
        JLabel background = new JLabel(backIcon);
        background.setFocusable(false);
        background.setOpaque(false);
        scalableComponents.add(new ScalableComponent(
                background, 0, 0, 1.0, 1.0,
                backIcon.getImage()
        ));
        ImageIcon btnImage = resources.getImage("PVBtitile.png");
        JButton nextBtn = new JButton(btnImage);
        btnSet(nextBtn);
        scalableComponents.add(new ScalableComponent(nextBtn,1130.0/1280,(720  - btnImage.getIconHeight())/720.0,
                btnImage.getIconWidth()*0.6/1280.0,btnImage.getIconHeight()*0.6/720.0,
                btnImage.getImage()));
        nextBtn.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        jPanel.add(nextBtn);

        for(int i = 1;i < gs.gc.length; i++) {

            StringBuilder infoText = new StringBuilder();
            infoText.append("公称職業:\n").append(getZY(gs.gc[i].claimedRole)).append("\n真の職業:\n").append(getZY(gs.gc[i].actualRole)).append("\n");
            if(gs.gc[i].whyDie != whyDie.NONE){
                switch(gs.gc[i].whyDie){
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
            else{
                if(gs.end == 1) {
                    infoText.append("最終存活");
                }
                else if(gs.end == 2) {
                    if(gs.gc[i].actualRole < 7||gs.gc[i].actualRole > 9) {
                        infoText.append("最終死亡");
                    }
                    else {
                        infoText.append("最終胜利");
                    }
                }
                else if(gs.end == 3) {
                    if(gs.gc[i].actualRole < 10) {
                        infoText.append("最終死亡");
                    }
                    else {
                        infoText.append("最終胜利");
                    }
                }
            }
            System.out.println(infoText);
            JTextArea infoLabel = new JTextArea(infoText.toString());
            infoLabel.setEditable(false);
            infoLabel.setLineWrap(true);
            infoLabel.setWrapStyleWord(true);
            infoLabel.setBorder(null);
            infoLabel.setOpaque(false);
            infoLabel.setFocusable(false);
            infoLabel.setFont(new Font("Takao Mincho", Font.BOLD, 16));

            StringBuilder xName = new StringBuilder();
            StringBuilder imageName = new StringBuilder();
            if(gs.gc[i].number <=9)imageName.append("0");
            imageName.append(gs.gc[i].number);
            switch(gs.gc[i].whyDie){
                case NONE:
                    switch(gs.gc[i].actualRole){
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
            if(!xName.isEmpty()){
                //不为空说明有死亡
                ImageIcon deathImage = resources.getImage(xName.toString());
                JLabel deathLabel = new JLabel(deathImage);
                if(i <= (gs.gc.length - 1 + 1)/2){
                    //死亡叉叉
                    scalableComponents.add(new ScalableComponent(deathLabel,(22+(characterImage.getIconWidth()+40) * i)/1280.0,110.0/720,
                            deathImage.getIconWidth()/1280.0,deathImage.getIconHeight()/720.0,deathImage.getImage()));
                }
                else{
                    scalableComponents.add(new ScalableComponent(deathLabel,(22+(characterImage.getIconWidth()+40) * (i - ((gs.gc.length - 1+1)/2)))/1280.0,
                            (260.0+characterImage.getIconHeight())/720.0
                            ,deathImage.getIconWidth()/1280.0
                            ,deathImage.getIconHeight()/720.0,deathImage.getImage()));
                }
                jPanel.add(deathLabel);
            }

            JLabel label = new JLabel(characterImage);
            JLabel textLabel = new JLabel(characterText);
            if(i <= (gs.gc.length - 1 + 1)/2){
                //头像
                scalableComponents.add(new ScalableComponent(label,(20+(characterImage.getIconWidth()+40) * i)/1280.0,100.0/720,characterImage.getIconWidth()/1280.0
                        ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                //文字
                scalableComponents.add(new ScalableComponent(textLabel,(35+(characterImage.getIconWidth()+40) * i)/1280.0,(100.0+characterImage.getIconHeight()-characterText.getIconHeight()/2.0)/720.0,characterText.getIconWidth() / 2.0 /1280.0
                        ,characterText.getIconHeight()/2.0/720.0,characterText.getImage()));
                //文本
                scalableComponents.add(new ScalableComponent(infoLabel,(20+(characterImage.getIconWidth()+40) * i)/1280.0,210.0/720,100/1280.0
                        ,150/720.0,null));
            }
            else{
                scalableComponents.add(new ScalableComponent(label,(20+(characterImage.getIconWidth()+40) * (i - ((gs.gc.length - 1+1)/2)))/1280.0,(250.0+characterImage.getIconHeight())/720.0
                        ,characterImage.getIconWidth()/1280.0
                        ,characterImage.getIconHeight()/720.0,characterImage.getImage()));
                scalableComponents.add(new ScalableComponent(textLabel,(35+(characterImage.getIconWidth()+40) * (i - ((gs.gc.length - 1+1)/2)))/1280.0,(250.0+2* characterImage.getIconHeight()-characterText.getIconHeight()/2.0)/720.0,characterText.getIconWidth() / 2.0 /1280.0
                        ,characterText.getIconHeight()/2.0/720.0,characterText.getImage()));
                //文本
                scalableComponents.add(new ScalableComponent(infoLabel,(20+(characterImage.getIconWidth()+40) * (i - ((gs.gc.length - 1+1)/2)))/1280.0,460.0/720,100/1280.0
                        ,150/720.0,null));
            }
            jPanel.add(infoLabel);
            jPanel.add(textLabel);
            jPanel.add(label);

        }
        String winIconText = "";
        String winText = "";
        switch(gs.end){
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
        ImageIcon winIcon = resources.getImage(winIconText);
        JLabel winLabel = new JLabel(winIcon);
        scalableComponents.add(new ScalableComponent(winLabel,50/1280.0,25/720.0,
                winIcon.getIconWidth()/1280.0,winIcon.getIconHeight()/720.0,
                    winIcon.getImage()));
        jPanel.add(winLabel);

        JTextArea infoLabel = new JTextArea(winText);
        infoLabel.setEditable(false);
        infoLabel.setFocusable(false);
        infoLabel.setLineWrap(true);
        infoLabel.setWrapStyleWord(true);
        infoLabel.setBorder(null);
        infoLabel.setOpaque(false);
        infoLabel.setFont(new Font("Takao Mincho", Font.BOLD, 24));
        scalableComponents.add(new ScalableComponent(infoLabel,100/1280.0,29/720.0,200/1280.0,100/1280.0,null));
        jPanel.add(infoLabel);

        jPanel.add(background);

        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }//结束画面

    //信息场景
    //InfoScene_A_B 其实就是指从最初的info主界面开始 点击第A个按钮进入的场景中，点击第B个按钮后进入的场景。
    public void InfoScene(){
        jPanel.removeAll();//清除
        scalableComponents.clear();


        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });

        //词条按钮
        //1
        ImageIcon btnNext = resources.getImage("avg_button2.png");
        JButton btn_next = new JButton(resources.getHelpText("Info1.txt"),btnNext);
        btnSet(btn_next);
        // 核心设置：文本在图标上方，且水平居中
        btn_next.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next,80.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //2
        JButton btn_next2 = new JButton(resources.getHelpText("Info2.txt"),btnNext);
        btnSet(btn_next2);
        // 核心设置：文本在图标上方，且水平居中
        btn_next2.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next2.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2;
            run();
        });
        //3
        scalableComponents.add(new ScalableComponent(btn_next2,80.0/1280,150.0/720,222.0/1280,50.0/720,btnNext.getImage()));

        JButton btn_next3 = new JButton(resources.getHelpText("Info3.txt"),btnNext);
        btnSet(btn_next3);
        // 核心设置：文本在图标上方，且水平居中
        btn_next3.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next3.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next3,80.0/1280,220.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //4
        JButton btn_next4 = new JButton(resources.getHelpText("Info4.txt"),btnNext);
        btnSet(btn_next4);
        // 核心设置：文本在图标上方，且水平居中
        btn_next4.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next4.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next4,80.0/1280,290.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //5
        JButton btn_next5 = new JButton(resources.getHelpText("Info5.txt"),btnNext);
        btnSet(btn_next5);
        // 核心设置：文本在图标上方，且水平居中
        btn_next5.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next5.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next5,80.0/1280,360.0/720,222.0/1280,50.0/720,btnNext.getImage()));

        jPanel.add(btn_next2);
        jPanel.add(btn_next3);
        jPanel.add(btn_next4);
        jPanel.add(btn_next5);
        jPanel.add(btnMenu);
        jPanel.add(btn_next);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);

        resizeComponents();//强制重置一次
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }
    public void InfoScene_1(){
        jPanel.removeAll();
        scalableComponents.clear();
        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE;
            run();
        });
        //词条按钮
        //1
        ImageIcon btnNext = resources.getImage("avg_button2.png");
        JButton btn_next = new JButton(resources.getHelpText("Info1-1.txt"),btnNext);
        btnSet(btn_next);
        // 核心设置：文本在图标上方，且水平居中
        btn_next.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1_1;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next,80.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //2
        JButton btn_next2 = new JButton(resources.getHelpText("Info1-2.txt"),btnNext);
        btnSet(btn_next2);
        // 核心设置：文本在图标上方，且水平居中
        btn_next2.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next2.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1_2;
            run();
        });
        //3
        scalableComponents.add(new ScalableComponent(btn_next2,80.0/1280,150.0/720,222.0/1280,50.0/720,btnNext.getImage()));

        JButton btn_next3 = new JButton(resources.getHelpText("Info1-3.txt"),btnNext);
        btnSet(btn_next3);
        // 核心设置：文本在图标上方，且水平居中
        btn_next3.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next3.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1_3;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next3,80.0/1280,220.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //4
        JButton btn_next4 = new JButton(resources.getHelpText("Info1-4.txt"),btnNext);
        btnSet(btn_next4);
        // 核心设置：文本在图标上方，且水平居中
        btn_next4.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next4.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1_4;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next4,80.0/1280,290.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //5
        JButton btn_next5 = new JButton(resources.getHelpText("Info1-5.txt"),btnNext);
        btnSet(btn_next5);
        // 核心设置：文本在图标上方，且水平居中
        btn_next5.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next5.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1_5;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next5,80.0/1280,360.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //6
        JButton btn_next6 = new JButton(resources.getHelpText("Info1-6.txt"),btnNext);
        btnSet(btn_next6);
        // 核心设置：文本在图标上方，且水平居中
        btn_next6.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next6.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1_6;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next6,80.0/1280,430.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //7
        JButton btn_next7 = new JButton(resources.getHelpText("Info1-7.txt"),btnNext);
        btnSet(btn_next7);
        // 核心设置：文本在图标上方，且水平居中
        btn_next7.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next7.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1_7;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next7,400.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));


        jPanel.add(btn_next);
        jPanel.add(btn_next2);
        jPanel.add(btn_next3);
        jPanel.add(btn_next4);
        jPanel.add(btn_next5);
        jPanel.add(btn_next6);
        jPanel.add(btn_next7);
        jPanel.add(btnMenu);
        jPanel.add(btnBack);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);


        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }
    public void InfoScene_1_1(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info1-1-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,900.0/1280,630.0/720,null));

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
    public void InfoScene_1_2(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info1-2-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,950.0/1280,630.0/720,null));

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
    public void InfoScene_1_3(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info1-3-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,950.0/1280,630.0/720,null));

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
    public void InfoScene_1_4(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info1-4-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_1_5(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info1-5-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_1_6(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info1-6-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_1_7(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_1;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info1-7-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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

    public void InfoScene_2(){
        jPanel.removeAll();
        scalableComponents.clear();
        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE;
            run();
        });
        //词条按钮
        //1
        ImageIcon btnNext = resources.getImage("avg_button2.png");
        JButton btn_next = new JButton(resources.getHelpText("Info2-1.txt"),btnNext);
        btnSet(btn_next);
        // 核心设置：文本在图标上方，且水平居中
        btn_next.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2_1;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next,80.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //2
        JButton btn_next2 = new JButton(resources.getHelpText("Info2-2.txt"),btnNext);
        btnSet(btn_next2);
        // 核心设置：文本在图标上方，且水平居中
        btn_next2.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next2.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2_2;
            run();
        });
        //3
        scalableComponents.add(new ScalableComponent(btn_next2,80.0/1280,150.0/720,222.0/1280,50.0/720,btnNext.getImage()));

        JButton btn_next3 = new JButton(resources.getHelpText("Info2-3.txt"),btnNext);
        btnSet(btn_next3);
        // 核心设置：文本在图标上方，且水平居中
        btn_next3.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next3.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2_3;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next3,80.0/1280,220.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //4
        JButton btn_next4 = new JButton(resources.getHelpText("Info2-4.txt"),btnNext);
        btnSet(btn_next4);
        // 核心设置：文本在图标上方，且水平居中
        btn_next4.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next4.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2_4;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next4,80.0/1280,290.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //5
        JButton btn_next5 = new JButton(resources.getHelpText("Info2-5.txt"),btnNext);
        btnSet(btn_next5);
        // 核心设置：文本在图标上方，且水平居中
        btn_next5.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next5.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2_5;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next5,80.0/1280,360.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //6
        JButton btn_next6 = new JButton(resources.getHelpText("Info2-6.txt"),btnNext);
        btnSet(btn_next6);
        // 核心设置：文本在图标上方，且水平居中
        btn_next6.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next6.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2_6;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next6,80.0/1280,430.0/720,222.0/1280,50.0/720,btnNext.getImage()));


        jPanel.add(btn_next);
        jPanel.add(btn_next2);
        jPanel.add(btn_next3);
        jPanel.add(btn_next4);
        jPanel.add(btn_next5);
        jPanel.add(btn_next6);
        jPanel.add(btnMenu);
        jPanel.add(btnBack);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);


        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }
    public void InfoScene_2_1(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //职业图标
        ImageIcon vocation = resources.getImage("Icon1_0.png");
        JLabel vocationLabel = new JLabel(vocation);
        vocationLabel.setOpaque(false);
        vocationLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(vocationLabel,50.0/1280,60.0/720,60.0/1280,90.0/1280,vocation.getImage()));

        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info2-1-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,120.0/720,900.0/1280,630.0/720,null));

        jPanel.add(vocationLabel);
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
    public void InfoScene_2_2(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //职业图标
        ImageIcon vocation = resources.getImage("Icon1_1.png");
        JLabel vocationLabel = new JLabel(vocation);
        vocationLabel.setOpaque(false);
        vocationLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(vocationLabel,50.0/1280,60.0/720,60.0/1280,90.0/1280,vocation.getImage()));

        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info2-2-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,120.0/720,950.0/1280,630.0/720,null));

        jPanel.add(vocationLabel);
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
    public void InfoScene_2_3(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //职业图标
        ImageIcon vocation = resources.getImage("Icon1_2.png");
        JLabel vocationLabel = new JLabel(vocation);
        vocationLabel.setOpaque(false);
        vocationLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(vocationLabel,50.0/1280,60.0/720,60.0/1280,90.0/1280,vocation.getImage()));

        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info2-3-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,120.0/720,950.0/1280,630.0/720,null));

        jPanel.add(vocationLabel);
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
    public void InfoScene_2_4(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //职业图标
        ImageIcon vocation = resources.getImage("Icon1_3.png");
        JLabel vocationLabel = new JLabel(vocation);
        vocationLabel.setOpaque(false);
        vocationLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(vocationLabel,50.0/1280,60.0/720,60.0/1280,90.0/1280,vocation.getImage()));

        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info2-4-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,120.0/720,880.0/1280,630.0/720,null));

        jPanel.add(vocationLabel);
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
    public void InfoScene_2_5(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //职业图标
        ImageIcon vocation = resources.getImage("Icon3.png");
        JLabel vocationLabel = new JLabel(vocation);
        vocationLabel.setOpaque(false);
        vocationLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(vocationLabel,50.0/1280,60.0/720,60.0/1280,90.0/1280,vocation.getImage()));

        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info2-5-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,120.0/720,880.0/1280,630.0/720,null));

        jPanel.add(vocationLabel);
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
    public void InfoScene_2_6(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //职业图标
        ImageIcon vocation = resources.getImage("Icon2.png");
        JLabel vocationLabel = new JLabel(vocation);
        vocationLabel.setOpaque(false);
        vocationLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(vocationLabel,50.0/1280,60.0/720,60.0/1280,90.0/1280,vocation.getImage()));

        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_2;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info2-6-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,110.0/720,880.0/1280,630.0/720,null));

        jPanel.add(vocationLabel);
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

    public void InfoScene_3(){
        jPanel.removeAll();
        scalableComponents.clear();
        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE;
            run();
        });
        //词条按钮
        //1
        ImageIcon btnNext = resources.getImage("avg_button2.png");
        JButton btn_next = new JButton(resources.getHelpText("Info3-1.txt"),btnNext);
        btnSet(btn_next);
        // 核心设置：文本在图标上方，且水平居中
        btn_next.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3_1;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next,80.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //2
        JButton btn_next2 = new JButton(resources.getHelpText("Info3-2.txt"),btnNext);
        btnSet(btn_next2);
        // 核心设置：文本在图标上方，且水平居中
        btn_next2.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next2.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3_2;
            run();
        });
        //3
        scalableComponents.add(new ScalableComponent(btn_next2,80.0/1280,150.0/720,222.0/1280,50.0/720,btnNext.getImage()));

        JButton btn_next3 = new JButton(resources.getHelpText("Info3-3.txt"),btnNext);
        btnSet(btn_next3);
        // 核心设置：文本在图标上方，且水平居中
        btn_next3.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next3.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3_3;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next3,80.0/1280,220.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //4
        JButton btn_next4 = new JButton(resources.getHelpText("Info3-4.txt"),btnNext);
        btnSet(btn_next4);
        // 核心设置：文本在图标上方，且水平居中
        btn_next4.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next4.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3_4;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next4,80.0/1280,290.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //5
        JButton btn_next5 = new JButton(resources.getHelpText("Info3-5.txt"),btnNext);
        btnSet(btn_next5);
        // 核心设置：文本在图标上方，且水平居中
        btn_next5.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next5.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3_5;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next5,80.0/1280,360.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //6
        JButton btn_next6 = new JButton(resources.getHelpText("Info3-6.txt"),btnNext);
        btnSet(btn_next6);
        // 核心设置：文本在图标上方，且水平居中
        btn_next6.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next6.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3_6;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next6,80.0/1280,430.0/720,222.0/1280,50.0/720,btnNext.getImage()));


        jPanel.add(btn_next);
        jPanel.add(btn_next2);
        jPanel.add(btn_next3);
        jPanel.add(btn_next4);
        jPanel.add(btn_next5);
        jPanel.add(btn_next6);

        jPanel.add(btnMenu);
        jPanel.add(btnBack);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);


        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }
    public void InfoScene_3_1(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info3-1-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_3_2(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info3-2-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_3_3(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info3-3-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_3_4(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info3-4-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_3_5(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info3-5-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_3_6(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_3;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info3-6-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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

    public void InfoScene_4(){
        jPanel.removeAll();
        scalableComponents.clear();
        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE;
            run();
        });
        //词条按钮
        //1
        ImageIcon btnNext = resources.getImage("avg_button2.png");
        JButton btn_next = new JButton(resources.getHelpText("Info4-1.txt"),btnNext);
        btnSet(btn_next);
        // 核心设置：文本在图标上方，且水平居中
        btn_next.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4_1;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next,80.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //2
        JButton btn_next2 = new JButton(resources.getHelpText("Info4-2.txt"),btnNext);
        btnSet(btn_next2);
        // 核心设置：文本在图标上方，且水平居中
        btn_next2.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next2.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4_2;
            run();
        });
        //3
        scalableComponents.add(new ScalableComponent(btn_next2,80.0/1280,150.0/720,222.0/1280,50.0/720,btnNext.getImage()));

        JButton btn_next3 = new JButton(resources.getHelpText("Info4-3.txt"),btnNext);
        btnSet(btn_next3);
        // 核心设置：文本在图标上方，且水平居中
        btn_next3.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next3.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4_3;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next3,80.0/1280,220.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //4
        JButton btn_next4 = new JButton(resources.getHelpText("Info4-4.txt"),btnNext);
        btnSet(btn_next4);
        // 核心设置：文本在图标上方，且水平居中
        btn_next4.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next4.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4_4;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next4,80.0/1280,290.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //5
        JButton btn_next5 = new JButton(resources.getHelpText("Info4-5.txt"),btnNext);
        btnSet(btn_next5);
        // 核心设置：文本在图标上方，且水平居中
        btn_next5.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next5.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4_5;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next5,80.0/1280,360.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //6
        JButton btn_next6 = new JButton(resources.getHelpText("Info4-6.txt"),btnNext);
        btnSet(btn_next6);
        // 核心设置：文本在图标上方，且水平居中
        btn_next6.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next6.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4_6;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next6,80.0/1280,430.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //7
        JButton btn_next7 = new JButton(resources.getHelpText("Info4-7.txt"),btnNext);
        btnSet(btn_next7);
        // 核心设置：文本在图标上方，且水平居中
        btn_next7.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next7.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4_7;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next7,400.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //8
        JButton btn_next8 = new JButton(resources.getHelpText("Info4-8.txt"),btnNext);
        btnSet(btn_next8);
        // 核心设置：文本在图标上方，且水平居中
        btn_next8.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next8.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4_8;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next8,400.0/1280,150.0/720,222.0/1280,50.0/720,btnNext.getImage()));


        jPanel.add(btn_next);
        jPanel.add(btn_next2);
        jPanel.add(btn_next3);
        jPanel.add(btn_next4);
        jPanel.add(btn_next5);
        jPanel.add(btn_next6);
        jPanel.add(btn_next7);
        jPanel.add(btn_next8);
        jPanel.add(btnMenu);
        jPanel.add(btnBack);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);


        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }
    public void InfoScene_4_1(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info4-1-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_4_2(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info4-2-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_4_3(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info4-3-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_4_4(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info4-4-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_4_5(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info4-5-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_4_6(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info4-6-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_4_7(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info4-7-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_4_8(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_4;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info4-8-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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

    public void InfoScene_5(){
        jPanel.removeAll();
        scalableComponents.clear();
        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE;
            run();
        });
        //词条按钮
        //1
        ImageIcon btnNext = resources.getImage("avg_button2.png");
        JButton btn_next = new JButton(resources.getHelpText("Info5-1.txt"),btnNext);
        btnSet(btn_next);
        // 核心设置：文本在图标上方，且水平居中
        btn_next.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_1;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next,80.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //2
        JButton btn_next2 = new JButton(resources.getHelpText("Info5-2.txt"),btnNext);
        btnSet(btn_next2);
        // 核心设置：文本在图标上方，且水平居中
        btn_next2.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next2.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_2;
            run();
        });
        //3
        scalableComponents.add(new ScalableComponent(btn_next2,80.0/1280,150.0/720,222.0/1280,50.0/720,btnNext.getImage()));

        JButton btn_next3 = new JButton(resources.getHelpText("Info5-3.txt"),btnNext);
        btnSet(btn_next3);
        // 核心设置：文本在图标上方，且水平居中
        btn_next3.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next3.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_3;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next3,80.0/1280,220.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //4
        JButton btn_next4 = new JButton(resources.getHelpText("Info5-4.txt"),btnNext);
        btnSet(btn_next4);
        // 核心设置：文本在图标上方，且水平居中
        btn_next4.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next4.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_4;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next4,80.0/1280,290.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //5
        JButton btn_next5 = new JButton(resources.getHelpText("Info5-5.txt"),btnNext);
        btnSet(btn_next5);
        // 核心设置：文本在图标上方，且水平居中
        btn_next5.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next5.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_5;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next5,80.0/1280,360.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //6
        JButton btn_next6 = new JButton(resources.getHelpText("Info5-6.txt"),btnNext);
        btnSet(btn_next6);
        // 核心设置：文本在图标上方，且水平居中
        btn_next6.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next6.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_6;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next6,80.0/1280,430.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //7
        JButton btn_next7 = new JButton(resources.getHelpText("Info5-7.txt"),btnNext);
        btnSet(btn_next7);
        // 核心设置：文本在图标上方，且水平居中
        btn_next7.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next7.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_7;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next7,400.0/1280,80.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //8
        JButton btn_next8 = new JButton(resources.getHelpText("Info5-8.txt"),btnNext);
        btnSet(btn_next8);
        // 核心设置：文本在图标上方，且水平居中
        btn_next8.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next8.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_8;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next8,400.0/1280,150.0/720,222.0/1280,50.0/720,btnNext.getImage()));
        //9
        JButton btn_next9 = new JButton(resources.getHelpText("Info5-9.txt"),btnNext);
        btnSet(btn_next9);
        // 核心设置：文本在图标上方，且水平居中
        btn_next9.setHorizontalTextPosition(SwingConstants.CENTER); // 文本在图标的水平中心

        btn_next9.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5_9;
            run();
        });
        scalableComponents.add(new ScalableComponent(btn_next9,400.0/1280,220.0/720,222.0/1280,50.0/720,btnNext.getImage()));

        jPanel.add(btn_next);
        jPanel.add(btn_next2);
        jPanel.add(btn_next3);
        jPanel.add(btn_next4);
        jPanel.add(btn_next5);
        jPanel.add(btn_next6);
        jPanel.add(btn_next7);
        jPanel.add(btn_next8);
        jPanel.add(btn_next9);

        jPanel.add(btnMenu);
        jPanel.add(btnBack);
        jPanel.add(backgroundLabel_2);
        jPanel.add(backgroundLabel);


        resizeComponents();
        jPanel.revalidate();
        jPanel.repaint();
        jFrame.setVisible(true);
    }
    public void InfoScene_5_1(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-1-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_5_2(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-2-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_5_3(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-3-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_5_4(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-4-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_5_5(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-5-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_5_6(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-6-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_5_7(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-7-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_5_8(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-8-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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
    public void InfoScene_5_9(){
        jPanel.removeAll();
        scalableComponents.clear();

        //整个的背景
        ImageIcon background = resources.getImage("PVBG.png");
        JLabel backgroundLabel = new JLabel(background);
        backgroundLabel.setOpaque(false);
        backgroundLabel.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel,0,0,1.0,1.0,background.getImage()));
        //背景上的背景
        ImageIcon background_2 = resources.getImage("avg1_resized(3).png");
        JLabel backgroundLabel_2 = new JLabel(background_2);
        backgroundLabel_2.setOpaque(false);
        backgroundLabel_2.setFocusable(false);
        scalableComponents.add(new ScalableComponent(backgroundLabel_2,10.0/1280,10.0/720,980.0/1280,660.0/720,background_2.getImage()));
        //返回主菜单按钮
        ImageIcon menu = resources.getImage("PVBtitile.png");
        JButton btnMenu = new JButton();
        btnSet(btnMenu);
        scalableComponents.add(new ScalableComponent(btnMenu,1050.0/1280,560.0/720,194.0/1280,127.0/720,menu.getImage()));
        btnMenu.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.START_SCENE;
            run();
        });
        //返回上一界面
        ImageIcon back = resources.getImage("PVBreturn.png");
        JButton btnBack = new JButton();
        btnSet(btnBack);
        scalableComponents.add(new ScalableComponent(btnBack, 1050.0 / 1280, 400.0 / 720, 194.0 / 1280, 127.0 / 720, back.getImage()));
        btnBack.addActionListener(e -> {
            resources.playSound("click.wav");
            currentScene = Scene.INFO_SCENE_5;
            run();
        });

        // 文本显示区域（核心修改：添加滚动和自动换行）
        JTextArea dialogText = new JTextArea(resources.getHelpText("Info5-9-1.txt"));
        dialogText.setForeground(Color.WHITE);
        dialogText.setFont(new Font("Takao Mincho",Font.BOLD,24));
        // 1. 开启自动换行
        dialogText.setLineWrap(true);       // 自动换行
        dialogText.setWrapStyleWord(true);  // 按单词拆分换行（避免单词截断）
        dialogText.setEditable(false);
        dialogText.setOpaque(false);
        dialogText.setBackground(new Color(0, 0, 0, 0));
        dialogText.setBorder(BorderFactory.createEmptyBorder());

        // 2. 创建滚动面板，包裹文本区域
        JScrollPane scrollPane = new JScrollPane(dialogText);
        // 隐藏不必要的边框
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        // 仅在需要时显示滚动条
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 关闭水平滚动条
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false); // 确保滚动面板背景透明
        scalableComponents.add(new ScalableComponent(scrollPane,50.0/1280,50.0/720,880.0/1280,630.0/720,null));

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

}


