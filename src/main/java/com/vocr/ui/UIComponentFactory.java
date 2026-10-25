package com.vocr.ui;

import com.vocr.domain.CharacterEnglishName;
import com.vocr.domain.CharacterKanjiName;
import com.vocr.domain.CharacterKatakanaName;
import com.vocr.domain.whyDie;

public class UIComponentFactory
{
    private static final String[] ZY_NAMES = {"", "占い師", "霊能者", "狩人", "共有者", "猫又",
            "村人", "人狼", "狂人", "狂信者", "妖狐", "背徳者"};
    private static final String[] WHY_DIE_NAMES = {"没死", "处刑", "白天猫咒", "白天后追",
            "被咬", "夜间猫咒", "夜间后追", "咒杀"};

    public String getJobText(int num)
    {
        if (num >= 1 && num <= 44)
            return CharacterKanjiName.values()[num].getShortName();
        return "";
    }
    public String getZY(int i)
    {
        if (i >= 1 && i < ZY_NAMES.length) return ZY_NAMES[i];
        return "無し";
    }
    public String getWhyDie(whyDie i)
    {
        if (i == null || i.ordinal() >= WHY_DIE_NAMES.length) return "";
        return WHY_DIE_NAMES[i.ordinal()];
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

    /**
     * 获取人物的日文显示名(日文汉字或平假名 + 空格 + 片假名)，不含英文名。
     * 用于不应显示英文名的界面正文。
     */
    public String getCharacterDisplayName(CharacterEnglishName englishName)
    {
        if (englishName == null || englishName == CharacterEnglishName.NONE)
            return "NONE / NONE";

        int idx = englishName.ordinal();
        // 三种枚举按相同顺序定义，ordinal一一对应
        String kanjiName = CharacterKanjiName.values()[idx].name();
        String katakanaName = CharacterKatakanaName.values()[idx].name();
        return String.format("%s %s", kanjiName, katakanaName);
    }

}
