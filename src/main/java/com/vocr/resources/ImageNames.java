package com.vocr.resources;

import com.vocr.domain.SelectionType;
import com.vocr.domain.whyDie;

/** 按运行时状态生成图片文件名；数据配置中的图片名不经过这里。 */
public final class ImageNames {
    private ImageNames() {
    }

    public static String gameSetupButton(int setupNumber) {
        return "game" + setupNumber + ".png";
    }

    public static String gameSetupTitle(int setupNumber) {
        return "game" + setupNumber + "-1.png";
    }

    public static String recordTitle(int page) {
        return "gameTitle" + page + ".png";
    }

    public static String voteDay(int day) {
        return day + "day.png";
    }

    public static String characterStatusPortrait(int characterNumber, boolean alive) {
        return characterPrefix(characterNumber) + (alive ? "s.png" : "gs.png");
    }

    public static String characterResultPortrait(
            int characterNumber, whyDie deathReason, int actualRole) {
        String suffix = deathReason == whyDie.NONE ? rolePortraitSuffix(actualRole) : "gs";
        return characterPrefix(characterNumber) + suffix + ".png";
    }

    public static String characterRolePortrait(int characterNumber, int actualRole) {
        return characterPrefix(characterNumber) + rolePortraitSuffix(actualRole) + ".png";
    }

    public static String characterText(int characterNumber) {
        return characterNumber + "job.png";
    }

    public static String claimedRoleIcon(int claimedRole, int claimedRoleOrder) {
        StringBuilder name = new StringBuilder("yaku");
        if (claimedRole <= 3) {
            name.append(claimedRole).append("_").append(claimedRoleOrder);
        } else {
            name.append(claimedRole);
        }
        return name.append(".png").toString();
    }

    public static String selectionResultIcon(SelectionType type, int claimedRoleOrder) {
        return switch (type) {
            case DIVINATION -> skillResultIcon(1, claimedRoleOrder, false);
            case GUARD -> skillResultIcon(3, claimedRoleOrder, false);
            case VOTE -> throw new IllegalArgumentException("投票选择没有按CO顺序生成的结果图标");
        };
    }

    public static String claimedRoleActionIcon(SelectionType type, int claimedRoleOrder) {
        return switch (type) {
            case DIVINATION -> "uranai" + claimedRoleOrder + ".png";
            case GUARD -> "goei" + claimedRoleOrder + ".png";
            case VOTE -> throw new IllegalArgumentException("投票选择没有按CO顺序生成的行动图标");
        };
    }

    public static String skillResultIcon(int claimedRole, int claimedRoleOrder, boolean black) {
        StringBuilder name = new StringBuilder("result")
                .append(claimedRole).append("_").append(claimedRoleOrder);
        if (claimedRole != 3) {
            name.append(black ? "black" : "white");
        }
        return name.append(".png").toString();
    }

    public static String deathMarker(whyDie deathReason) {
        return switch (deathReason) {
            case chuxing -> "turi.png";
            case daymaozhou -> "noroi.png";
            case dayhouzhui -> "atooi.png";
            case beiyao, nightmaozhou, nighthouzhui, zhousha -> "kami.png";
            case NONE -> "";
        };
    }

    public static String deathMarker(int deathReason) {
        if (deathReason < 0 || deathReason >= whyDie.values().length) {
            return "";
        }
        return deathMarker(whyDie.values()[deathReason]);
    }

    private static String characterPrefix(int characterNumber) {
        return (characterNumber <= 9 ? "0" : "") + characterNumber;
    }

    private static String rolePortraitSuffix(int actualRole) {
        return switch (actualRole) {
            case 5 -> "cs";
            case 10 -> "fs";
            case 11 -> "hs";
            case 7 -> "ws";
            case 8, 9 -> "ks";
            default -> "s";
        };
    }
}
