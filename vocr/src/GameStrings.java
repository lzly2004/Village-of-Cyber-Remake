/**
 * 游戏UI字符串常量 — 第1步重构核心。
 * 集中管理 UI.java 中散布的日语提示文本、格式模板、面板标题等。
 * 所有字段均为 public static final，可直接静态导入使用。
 */
public final class GameStrings
{
    private GameStrings() {} // 工具类不可实例化

    // ==================== 白天/入夜 ====================
    /** {0}日目になりました。 */
    public static final String DAY_START_FORMAT    = "%d日目になりました。";
    /** 犠牲者はいませんでした。 */
    public static final String NO_SACRIFICE        = "犠牲者はいませんでした。\n";

    // ==================== 死亡/处刑 ====================
    /** 投票の結果、{0}は処刑されました。 */
    public static final String EXECUTED_FORMAT     = "投票の結果、%sは処刑されました。";
    /** {0}後追いで死亡した。 */
    public static final String FOLLOW_DEATH_FORMAT = "%s後追いで死亡した。";
    /** {0}猫の呪いによって死亡した。 */
    public static final String CAT_CURSE_FORMAT    = "%s猫の呪いによって死亡した。";

    // ==================== 投票结果 ====================
    /** {0}票で{1}さんが処刑されました。 */
    public static final String VOTE_RESULT_FORMAT  = "%d票で%sさんが処刑されました。";
    /** 投票が同点となりました。再投票を行います。 */
    public static final String VOTE_TIE            = "投票が同点となりました。再投票を行います。";

    // ==================== 投票方式标签 ====================
    /** 自由投票 */
    public static final String VOTE_FREE           = "自由投票\n";
    /** グレラン */
    public static final String VOTE_GREY           = "グレラン：\n";
    /** 指定投票 */
    public static final String VOTE_DESIGNATED     = "指定投票：\n";

    // ==================== 票型标题模板 ====================
    /** -投票結果/{0}日目-自由投票 */
    public static final String VOTE_TITLE_FREE     = "-投票結果/%d日目-自由投票\n\n";
    /** -投票結果/{0}日目-グレラン */
    public static final String VOTE_TITLE_GREY     = "-投票結果/%d日目-グレラン：\n";
    /** -投票結果/{0}日目-指定投票 */
    public static final String VOTE_TITLE_DESIGN   = "-投票結果/%d日目-指定投票\n%s\n";
    /** -投票結果/{0}日目-重新投票 */
    public static final String VOTE_TITLE_REDO     = "-投票結果/%d日目-重新投票\n\n";
    /** -投票結果/{0}日目-第{1}轮 */
    public static final String VOTE_HISTORY_TITLE  = "-投票結果/%d日目-第%d轮%s\n";

    // ==================== 数据面板 ====================
    /** "    {0}日目\n 生存者:{1}\n 死亡者:{2}\n 吊り縄:{3}" */
    public static final String DATA_PANEL_FORMAT   = "    %d日目\n 生存者:%d\n 死亡者:%d\n 吊り縄:%d";

    // ==================== 信息面板区标题 ====================
    /** [占い師] */
    public static final String SECTION_SEER        = "[占い師]\n";
    /** [霊能者] */
    public static final String SECTION_MEDIUM      = "[霊能者]\n";
    /** [処刑] */
    public static final String SECTION_EXECUTION   = "[処刑]\n";
    /** [死体] */
    public static final String SECTION_BODY        = "[死体]\n";
    /** [護衛先] */
    public static final String SECTION_GUARD       = "[護衛先]\n";

    // ==================== 指定信息面板 ====================
    /** [指定投票] */
    public static final String SPECIFY_VOTE        = "[指定投票]\n";
    /** [指定占い] */
    public static final String SPECIFY_DIVINATION  = "[指定占い]\n";
    /** [指定護衛] */
    public static final String SPECIFY_GUARD       = "[指定護衛]\n";
    /** 潜伏→ */
    public static final String HIDDEN_ARROW        = "潜伏→";

    // ==================== 怀疑度面板 ====================
    /** - 疑い先 - 全体表示 */
    public static final String DOUBT_TITLE         = "- 疑い先 - 全体表示\n\n";
    /** -指定内容- */
    public static final String SPECIFY_CONTENT     = "-指定内容-\n";

    // ==================== 破绽/平和 ====================
    /** 破绽 */
    public static final String MARKER_EXPOSED      = "破绽";
    /** 平和→ */
    public static final String PEACE_ARROW         = "平和→";

    // ==================== CO提示 ====================
    /** 占い師はいないようだ */
    public static final String MSG_NO_SEER         = "占い師はいないようだ";
    /** 霊能者はいないようだ */
    public static final String MSG_NO_MEDIUM       = "霊能者はいないようだ";
    /** 狩人はいないようだ */
    public static final String MSG_NO_HUNTER       = "狩人はいないようだ";
    /** 共有はいないようだ */
    public static final String MSG_NO_SHARED       = "共有はいないようだ";
    /** 猫又はいないようだ */
    public static final String MSG_NO_CAT          = "猫又はいないようだ";
    /** 無人CO */
    public static final String MSG_NO_CO           = "無人CO";

    // ==================== 其他提示 ====================
    /** まだ特に疑い先もなく、\n投票の履歴もないようだ。 */
    public static final String MSG_NO_DOUBT_YET    = "まだ特に疑い先もなく、\n投票の履歴もないようだ。";
}
