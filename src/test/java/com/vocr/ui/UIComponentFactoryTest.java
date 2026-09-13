package com.vocr.ui;

import com.vocr.domain.CharacterEnglishName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class UIComponentFactoryTest {
    private final UIComponentFactory factory = new UIComponentFactory();

    @Test
    void displayNameShouldUseKanjiAndKatakanaWithoutEnglishName() {
        assertEquals("宿屋主人 フーゴー", factory.getCharacterDisplayName(CharacterEnglishName.Hugo));
        assertEquals("修道士 ライヒアルト", factory.getCharacterDisplayName(CharacterEnglishName.Reichard));
    }

    @Test
    void displayNameShouldNeverContainSlashOrEnglishName() {
        for (CharacterEnglishName name : CharacterEnglishName.values()) {
            if (name == CharacterEnglishName.NONE) continue;
            String displayName = factory.getCharacterDisplayName(name);
            assertFalse(displayName.contains("/"), () -> name + " 的显示名不应包含斜杠: " + displayName);
            assertFalse(displayName.contains(name.name()),
                    () -> name + " 的显示名不应包含英文名: " + displayName);
        }
    }

    @Test
    void noneShouldKeepPlaceholderText() {
        assertEquals("NONE / NONE", factory.getCharacterDisplayName(null));
        assertEquals("NONE / NONE", factory.getCharacterDisplayName(CharacterEnglishName.NONE));
    }

    @Test
    void executionResultTextShouldUseDisplayName() {
        assertEquals("投票の結果、宿屋主人 フーゴーは処刑されました。",
                String.format(com.vocr.ui.GameStrings.EXECUTED_FORMAT,
                        factory.getCharacterDisplayName(CharacterEnglishName.Hugo)));
        assertEquals("宿屋主人 フーゴー後追いで死亡した。",
                String.format(com.vocr.ui.GameStrings.FOLLOW_DEATH_FORMAT,
                        factory.getCharacterDisplayName(CharacterEnglishName.Hugo)));
        assertEquals("宿屋主人 フーゴー猫の呪いによって死亡した。",
                String.format(com.vocr.ui.GameStrings.CAT_CURSE_FORMAT,
                        factory.getCharacterDisplayName(CharacterEnglishName.Hugo)));
    }
}
