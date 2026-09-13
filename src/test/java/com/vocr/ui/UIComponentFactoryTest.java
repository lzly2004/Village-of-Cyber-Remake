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
}
