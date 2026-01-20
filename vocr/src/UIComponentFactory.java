import javax.swing.*;
import java.awt.*;

public class UIComponentFactory
{
    // 创建标准按钮样式（替代 btnSet 方法）
    public String getJobText(int num)//获取名字，需要传入人物编号
    {
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
    public String getCharacterFullName(CharacterEnglishName englishName)//获取人物的完整名字，输入的是event的ch1
    {
        // 初始化默认值
        String kanjiName = CharacterKanjiName.NONE.name();
        String katakanaName = CharacterKatakanaName.NONE.name();

        // 通过switch一一匹配所有枚举的对应关系
        switch (englishName)
        {
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
    }
}