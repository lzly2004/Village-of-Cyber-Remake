package com.vocr.domain;

/**
 * 待展示的确定性游戏事件。
 */
public record Event(EventName eventname, CharacterEnglishName ch1,
                    CharacterEnglishName ch2, CharacterEnglishName ch3)
{
    public Event(EventName eventname, CharacterEnglishName ch1, CharacterEnglishName ch2)
    {
        this(eventname, ch1, ch2, null);
    }

    public Event(EventName eventname, CharacterEnglishName ch1)
    {
        this(eventname, ch1, null, null);
    }

    @Override
    public String toString()
    {
        if (eventname == null) return null;
        String result = eventname.toString();
        result += ch1 == null ? " null" : " " + CharacterKanjiName.values()[ch1.ordinal()];
        result += ch2 == null ? " null" : " " + CharacterKanjiName.values()[ch2.ordinal()];
        return result;
    }
}
