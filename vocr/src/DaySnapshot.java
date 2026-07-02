// C:\Users\Lenovo\Desktop\电脑村\电脑村重制相关文件\Village of Cyber Remake\vocr\src\DaySnapshot.java
import java.util.ArrayList;
import java.util.List;

public class DaySnapshot {
    public int dayNumber;
    public int aliveCount;
    public PlayerStatus[] players;
    public List<VoteRound> voteRounds = new ArrayList<>();
    public int dailyVotingRule = -1;
    public List<Integer> greyTargetCharNums = new ArrayList<>();
    public List<Integer> designatedTargetCharNums = new ArrayList<>();

    public DaySnapshot() {}

    public static DaySnapshot parseFromRecord(String headerLine, List<String> allRecords) {
        try {
            DaySnapshot snap = new DaySnapshot();
            String[] parts = headerLine.split("\\|");
            for (String part : parts) {
                if (part.startsWith("day=")) snap.dayNumber = Integer.parseInt(part.substring(4));
                if (part.startsWith("alive=")) snap.aliveCount = Integer.parseInt(part.substring(6));
            }

            for (String line : allRecords) {
                if (line.startsWith("STATE|day=" + snap.dayNumber + "|")) {
                    snap.players = parsePlayerStatuses(line);
                }
                if (line.startsWith("VOTE_DETAIL|day=" + snap.dayNumber + "|")) {
                    VoteRound round = VoteRound.parseFromRecord(line);
                    if (round != null) snap.voteRounds.add(round);
                }
                if (line.startsWith("VOTE_METHOD|day=" + snap.dayNumber + "|")) {
                    String[] mp = line.split("\\|");
                    for (String p : mp) {
                        if (p.startsWith("rule=")) snap.dailyVotingRule = Integer.parseInt(p.substring(5));
                        if (p.startsWith("grey=") && p.length() > 5) {
                            for (String gs : p.substring(5).split(",")) {
                                try { snap.greyTargetCharNums.add(Integer.parseInt(gs.trim())); } catch (Exception ignored) {}
                            }
                        }
                        if (p.startsWith("design=") && p.length() > 7) {
                            for (String ds : p.substring(7).split(",")) {
                                try { snap.designatedTargetCharNums.add(Integer.parseInt(ds.trim())); } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            }
            return snap;
        } catch (Exception e) {
            DebugLogger.error("[DaySnapshot] 解析失败: " + e.getMessage());
            return null;
        }
    }

    private static PlayerStatus[] parsePlayerStatuses(String stateLine) {
        String content = stateLine.substring(stateLine.indexOf('|') + 1);
        content = content.substring(content.indexOf('|') + 1);

        List<PlayerStatus> list = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;

        for (char c : content.toCharArray()) {
            current.append(c);
            if (c == '{') braceCount++;
            if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    PlayerStatus ps = PlayerStatus.parseFromString(current.toString());
                    if (ps != null) list.add(ps);
                    current.setLength(0);
                }
            }
        }

        return list.toArray(new PlayerStatus[0]);
    }

    public static class PlayerStatus {
        public int playerIndex;
        public int characterNumber;
        public int actualRole;
        public int claimedRole;
        public int claimedRoleOrder;
        public int comingOutDay;  // CO日期，-1表示未CO
        public int skillTarget;  // 技能目标（占卜/灵能/猎人），0表示无
        public int deathDay;
        public int deathReason;
        public boolean nonHumanMarked;

        public PlayerStatus() {}

        public static PlayerStatus parseFromString(String str) {
            try {
                PlayerStatus ps = new PlayerStatus();

                str = str.trim().substring(1, str.length() - 1);
                
                // 提取玩家位置: {1:n=42,...} → playerIndex = 1
                int colonPos = str.indexOf(':');
                if (colonPos > 0) {
                    try {
                        ps.playerIndex = Integer.parseInt(str.substring(0, colonPos).trim());
                    } catch (Exception e) {
                        DebugLogger.warn("[DaySnapshot] 无法解析playerIndex: " + str.substring(0, colonPos));
                    }
                }
                
                String dataPart = str.substring(colonPos + 1); // "n=42,r=6,..."
                String[] pairs = dataPart.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=");
                    if (kv.length < 2) continue;
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    switch (key) {
                        case "n": 
                            ps.characterNumber = Integer.parseInt(value);
                            if (ps.characterNumber == 0) {
                                DebugLogger.warn("[DaySnapshot] ⚠️ characterNumber=0! 完整数据=" + str);
                            }
                            break;
                        case "r": ps.actualRole = Integer.parseInt(value); break;
                        case "cr": ps.claimedRole = Integer.parseInt(value); break;
                        case "cro": ps.claimedRoleOrder = Integer.parseInt(value); break;
                        case "dd": ps.deathDay = Integer.parseInt(value); break;
                        case "wd": ps.deathReason = Integer.parseInt(value); break;
                        case "nm": ps.nonHumanMarked = Integer.parseInt(value) == 1; break;
                        case "cod": ps.comingOutDay = Integer.parseInt(value); break;
                        case "st": ps.skillTarget = Integer.parseInt(value); break;
                    }
                }
                
                if (ps.playerIndex == 0 || ps.characterNumber == 0) {
                    DebugLogger.warn("[DaySnapshot] ⚠️ 解析结果: playerIndex=" + ps.playerIndex + 
                            ", characterNumber=" + ps.characterNumber + ", 原始=" + str);
                }
                
                return ps;
            } catch (Exception e) {
                DebugLogger.error("[DaySnapshot] 解析PlayerStatus失败: " + e.getMessage() + ", 原始数据=" + str);
                return null;
            }
        }

        public boolean isAlive(int currentDay) {
            return deathDay == 0 || deathDay > currentDay;
        }
    }

    public static class VoteRound {
        public int round;
        public int[][] votes;

        public VoteRound() {}

        public static VoteRound parseFromRecord(String line) {
            try {
                VoteRound vr = new VoteRound();
                String[] parts = line.split("\\|");
                for (String part : parts) {
                    if (part.startsWith("round=")) vr.round = Integer.parseInt(part.substring(6));
                }

                String voteData = parts.length > 3 ? parts[3] : "";
                if (!voteData.isEmpty()) {
                    String[] votePairs = voteData.split(",");
                    vr.votes = new int[votePairs.length][2];
                    for (int i = 0; i < votePairs.length; i++) {
                        String[] kv = votePairs[i].split("->");
                        vr.votes[i][0] = Integer.parseInt(kv[0]);
                        vr.votes[i][1] = Integer.parseInt(kv[1]);
                    }
                }
                return vr;
            } catch (Exception e) {
                return null;
            }
        }
    }
}