class Event
{//事件类型
    public CharacterEnglishName ch1,ch2,ch3;
    public EventName eventname;
    public Event(EventName eventname, CharacterEnglishName ch1, CharacterEnglishName ch2, CharacterEnglishName ch3)
    {
        // 统一的业务逻辑实现
        this.eventname = eventname;
        this.ch1 = ch1;
        this.ch2 = ch2;
        this.ch3 = ch3;
    }

    // 2. 重载：仅传ename+ch1+ch2，ch3默认null
    public Event(EventName ename, CharacterEnglishName ch1, CharacterEnglishName ch2) {
        this(ename, ch1, ch2, null); // 调用全参构造函数，复用逻辑
    }

    // 3. 重载：仅传ename+ch1，ch2、ch3默认null
    public Event(EventName ename, CharacterEnglishName ch1) {
        this(ename, ch1, null, null); // 调用全参构造函数
    }

    //

    @Override
    public String toString()
    {
        if(eventname == null) return null;
        String str = eventname.toString();
        if(ch1 == null) str += " null";
        else str += " " + CharacterKanjiName.values()[ch1.ordinal()].toString();
        if(ch2 == null) str += " null";
        else str += " " + CharacterKanjiName.values()[ch2.ordinal()].toString();
        return str;
    }
}
