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

    // ==================== 结局展示 ====================
    /** 公称職業: */
    public static final String CLAIMED_ROLE_PREFIX = "公称職業:\n";
    /** 真の職業: */
    public static final String ACTUAL_ROLE_PREFIX  = "\n真の職業:\n";
    /** {0}日目狼噛 */
    public static final String DEATH_BITE_FORMAT   = "%d日目狼噛";
    /** {0}日目処刑 */
    public static final String DEATH_EXECUTE_FORMAT = "%d日目処刑 ";
    /** {0}日目呪殺 */
    public static final String DEATH_CURSE_FORMAT  = "%d日目呪殺 ";
    /** {0}日目後追 */
    public static final String DEATH_FOLLOW_FORMAT = "%d日目後追 ";
    /** {0}日目猫呪 */
    public static final String DEATH_CAT_FORMAT    = "%d日目猫呪";
    /** 最終存活 */
    public static final String END_SURVIVE         = "最終存活";
    /** 最終死亡 */
    public static final String END_DEAD            = "最終死亡";
    /** 最終胜利 */
    public static final String END_WIN             = "最終胜利";
    /** 村人勝利 */
    public static final String WIN_VILLAGER        = "村人勝利";
    /** 人狼勝利 */
    public static final String WIN_WOLF            = "人狼勝利";
    /** 妖狐勝利 */
    public static final String WIN_FOX             = "妖狐勝利";

    // ==================== 其他提示 ====================
    /** まだ特に疑い先もなく、\n投票の履歴もないようだ。 */
    public static final String MSG_NO_DOUBT_YET    = "まだ特に疑い先もなく、\n投票の履歴もないようだ。";

    // ==================== 投票记录格式 ====================
    /** %s：%d票  投票先→%s\n */
    public static final String VOTE_LINE_FORMAT    = "%s：%d票  投票先→%s\n";

    // ==================== 格式化方法 ====================
    public static String getDayStart(int day) { return String.format(DAY_START_FORMAT, day); }
    public static String getDeathBite(int day) { return String.format(DEATH_BITE_FORMAT, day); }
    public static String getDeathExecute(int day) { return String.format(DEATH_EXECUTE_FORMAT, day); }
    public static String getDeathCurse(int day) { return String.format(DEATH_CURSE_FORMAT, day); }
    public static String getDeathFollow(int day) { return String.format(DEATH_FOLLOW_FORMAT, day); }
    public static String getDeathCat(int day) { return String.format(DEATH_CAT_FORMAT, day); }
    public static String getVoteTitleFree(int day) { return String.format(VOTE_TITLE_FREE, day); }
    public static String getVoteTitleGrey(int day) { return String.format(VOTE_TITLE_GREY, day); }
    public static String getVoteTitleRedo(int day) { return String.format(VOTE_TITLE_REDO, day); }
    public static String getVoteTitleDesign(int day, String targetText) { return String.format(VOTE_TITLE_DESIGN, day, targetText); }
    public static String getVoteHistoryTitle(int day, int round, String extra) { return String.format(VOTE_HISTORY_TITLE, day, round, extra); }

    /**
     * 获取角色头像图片后缀（复盘/结算界面用）
     * 对应 ReplayPlayerHandler 和 EndAnimeHandler 中的 switch case 逻辑
     */
    public static String getRoleImageSuffix(int actualRole) {
        return switch (actualRole) {
            case 5 -> "cs";   // 猫又
            case 10 -> "fs";  // 妖狐
            case 11 -> "hs";  // 共有者
            case 7 -> "ws";   // 狼人
            case 8, 9 -> "ks"; // 狂人、狂灵
            default -> "s";   // 村民
        };
    }

    public static String buildCharacterImageName(int characterNumber, whyDie deathReason, int actualRole, boolean useRoleSuffix) {
        StringBuilder imageName = new StringBuilder();
        if (characterNumber <= 9) imageName.append("0");
        imageName.append(characterNumber);
        if (deathReason == whyDie.NONE) {
            if (useRoleSuffix) imageName.append(getRoleImageSuffix(actualRole));
        } else {
            imageName.append("gs");
        }
        imageName.append(".png");
        return imageName.toString();
    }

    public static String buildCharacterImageNameSimple(int characterNumber, whyDie deathReason) {
        StringBuilder imageName = new StringBuilder();
        if (characterNumber <= 9) imageName.append("0");
        imageName.append(characterNumber);
        if (deathReason != whyDie.NONE) imageName.append("g");
        imageName.append("s.png");
        return imageName.toString();
    }

    public static String buildCharacterTextName(int characterNumber) {
        return characterNumber + "job.png";
    }
}