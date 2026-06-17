import javax.swing.*;
import java.awt.*;

public class UIComponentFactory
{
    public String getJobText(int num)
    {
        if (num >= 1 && num <= 44)
            return CharacterKanjiName.values()[num].getShortName();
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