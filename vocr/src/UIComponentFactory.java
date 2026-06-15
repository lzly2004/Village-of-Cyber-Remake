import javax.swing.*;
import java.awt.*;

public class UIComponentFactory
{
    // 创建标准按钮样式（替代 btnSet 方法）
    // 角色简称查找表 (下标=角色编号)
    private static final String[] JOB_SHORT_NAMES = {
        "",           // 0: 占位
        "青年",       // 1: Abel
        "研究",       // 2: Erich
        "傭兵",       // 3: Matthäus
        "教師",       // 4: Otfried
        "情報",       // 5: Karl
        "少年",       // 6: Till
        "宝石",       // 7: Samuel
        "旅人",       // 8: Hans
        "少女",       // 9: Beatrice
        "陶芸",       // 10: Amanda
        "洋灯",       // 11: Irene
        "歌姫",       // 12: Elsa
        "未亡",       // 13: Nora
        "物識",       // 14: Johanna
        "読書",       // 15: Milli
        "召使",       // 16: Judith
        "貴族",       // 17: Michael
        "団長",       // 18: Günther
        "騎士",       // 19: David
        "職人",       // 20: Julian
        "神父",       // 21: Klemens
        "探偵",       // 22: Heinrich
        "学生",       // 23: Liddi
        "小説",       // 24: Brigitte
        "女将",       // 25: Helga
        "尼僧",       // 26: Natalie
        "双弟",       // 27: Volker
        "双姉",       // 28: Eva
        "流者",       // 29: Willy
        "修道",       // 30: Reichard
        "宿主",       // 31: Hugo
        "踊子",       // 32: Rosa
        "神学",       // 33: Wendel
        "薬師",       // 34: Sergius
        "煙突",       // 35: Kaja
        "店員",       // 36: Betti
        "洗濯",       // 37: Chloe
        "娼妓",       // 38: Carmen
        "剣士",       // 39: Renate
        "村娘",       // 40: Romi
        "刺繡",       // 41: Gerda
        "大工",       // 42: Iwan
        "婦人",       // 43: Oktavia
        "音楽",       // 44: Helmut
    };

    public String getJobText(int num)
    {
        if (num >= 1 && num < JOB_SHORT_NAMES.length)
            return JOB_SHORT_NAMES[num];
        return "";
    }
    public String getZY(int i)//获取职业文本，需要传入角色的number
    {
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
    }
    public String getwhyDie(whyDie i)//获取角色死因，用于测试信息的显示
    {
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
    }
    /**
     * 获取人物的完整名字(日文汉字 片假名/英文)。
     * 利用三个枚举共享 ordinal 的特性替代超长 switch。
     */
    public String getCharacterFullName(CharacterEnglishName englishName)
    {
        if (englishName == null || englishName == CharacterEnglishName.NONE)
            return "NONE / NONE";

        int idx = englishName.ordinal();
        // 三种枚举按相同顺序定义，ordinal一一对应
        String kanjiName = CharacterKanjiName.values()[idx].name();
        String katakanaName = CharacterKatakanaName.values()[idx].name();
        return String.format("%s %s/%s", kanjiName, katakanaName, englishName.name());
    }
}