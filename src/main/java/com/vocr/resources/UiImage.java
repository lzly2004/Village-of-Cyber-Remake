package com.vocr.resources;

/** 直接由界面按固定用途选择的图片资源。 */
public enum UiImage {
    FALLBACK("frame.png"),
    WINDOW_ICON("Icon2.png"),

    TITLE_LOGO("titleLogo.png"),
    START_BUTTON("startButton.png"),
    CONTINUE_BUTTON("continueButton.png"),
    REPLAY_BUTTON("replayButton.png"),
    RECORD_BUTTON("recordButton.png"),
    INFO_BUTTON("infoButton.png"),
    COLLECTIONS_BUTTON("collectionsButton.png"),
    TITLE_BACKGROUND("title_base_resized.png"),

    TITLE_RETURN("PVBtitile.png"),
    NAVIGATION_RETURN("PVBreturn.png"),
    INFO_NEXT("avg_button2.png"),
    INFO_PANEL("avg1_resized(3).png"),
    REPLAY_BACKGROUND("PVBG.png"),

    MESSAGE_FRAME("messageframe.png"),
    DIALOGUE_AFTERNOON_BACKGROUND("haikei.png"),
    DIALOGUE_DAY_BACKGROUND("haikei3.png"),
    GAME_SETUP_DIALOGUE_BACKGROUND("komorebi002.png"),
    DAY_BACKGROUND("komorebi002yuu.png"),
    NIGHT_BACKGROUND("komorebi000night01.png"),
    MUSIC_BUTTON("musicBtn.png"),
    CALENDAR("hiduke.png"),
    REVIEW_BOARD("frame #19252.png"),

    END_FOX("endFox.png"),
    END_WOLF("endWolf.png"),
    END_VILLAGE("endVillage.png"),

    VOTE_RESULT_ALL("result2_all.png"),
    DIVINATION_DRAG("uranaiAll.png"),
    DELETE("delete.png"),
    HISTORY_BACK("rirekiBack.png"),
    HISTORY_NEXT("rirekiNext.png");

    private static final String IMAGE_DIRECTORY = "/images/";

    private final String fileName;

    UiImage(String fileName) {
        this.fileName = fileName;
    }

    public String resourcePath() {
        return IMAGE_DIRECTORY + fileName;
    }
}
