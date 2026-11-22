package com.vocr.ui;

import com.vocr.domain.CharacterKanjiName;
import com.vocr.domain.whyDie;
import com.vocr.replay.DaySnapshot;
import com.vocr.replay.ReplaySave;
import java.util.*;

/** Completed Replay presentation only; never chooses or changes a guard target. */
final class ReplayGuardHistory {
    record Line(int playerIndex, String text, boolean unannounced) {}

    static List<Line> lines(ReplaySave replay, int day) {
        var people = new TreeMap<Integer, DaySnapshot.PlayerStatus>();
        var lastDay = new HashMap<Integer, Integer>();
        var coDays = new HashMap<Integer, Integer>();
        var claimed = new HashSet<Integer>();
        for (var snapshot : replay.daySnapshots) {
            if (snapshot == null || snapshot.players == null) continue;
            for (var p : snapshot.players) {
                if (p == null || p.playerIndex < 1) continue;
                if (snapshot.dayNumber >= lastDay.getOrDefault(p.playerIndex, -1)) {
                    people.put(p.playerIndex, p); lastDay.put(p.playerIndex, snapshot.dayNumber);
                }
                if (p.claimedRole == 3) {
                    claimed.add(p.playerIndex);
                    if (p.comingOutDay > 0) coDays.merge(p.playerIndex, p.comingOutDay, Math::min);
                }
            }
        }
        var result = new ArrayList<Line>();
        for (var person : people.values()) {
            int id = person.playerIndex;
            if (person.actualRole != 3 && !claimed.contains(id)) continue;
            var current = visiblePlayer(replay, id, day + 1);
            Integer coDay = coDays.get(id);
            boolean announced = coDay != null ? coDay <= day : current != null && current.claimedRole == 3;
            // Opening night has no guard action. An unclaimed hunter who died before
            // night 2 has no diary to show, rather than an unavailable diary.
            boolean diedBeforeFirstGuard = person.deathDay == 1 || (person.deathDay == 2
                    && person.deathReason > 0 && person.deathReason < whyDie.values().length
                    && whyDie.values()[person.deathReason].isDayDeath());
            if (!announced && diedBeforeFirstGuard) continue;
            var text = new StringBuilder(name(person.characterNumber)).append(" : ");
            boolean exposed = false, any = false;
            for (int skillDay = 2; skillDay < day; skillDay++) {
                if (person.deathDay > 0 && skillDay >= person.deathDay) break;
                // getVisibleSkillTarget's legacy positional fallback must not borrow a future snapshot.
                boolean history = false;
                for (var s : replay.daySnapshots) {
                    var p = player(s, id);
                    if (s != null && s.dayNumber <= day + 1 && p != null && p.skillTargets != null
                            && p.skillTargets.length > skillDay) { history = true; break; }
                }
                var exact = exactPlayer(replay, id, skillDay + 1);
                int target = history || exact != null ? replay.getVisibleSkillTarget(id, skillDay, day) : 0;
                if (target != 0) {
                    int targetId = target > people.size() ? target - people.size() : target;
                    var targetPerson = people.get(targetId);
                    text.append(name(targetPerson != null ? targetPerson.characterNumber : targetId)).append('→');
                    any = true;
                }
                var atNight = exactPlayer(replay, id, skillDay);
                exposed |= atNight != null && atNight.nonHumanMarked;
            }
            var today = exactPlayer(replay, id, day);
            exposed |= today != null && today.nonHumanMarked;
            if (exposed) text.append(GameStrings.MARKER_EXPOSED).append('→');
            if (text.charAt(text.length() - 1) == '→') text.setLength(text.length() - 1);
            if (!announced && !any && day > 2) text.append("（記録なし）");
            result.add(new Line(id, text.append('\n').toString(), !announced));
        }
        return List.copyOf(result);
    }

    private static DaySnapshot.PlayerStatus player(DaySnapshot snapshot, int id) {
        if (snapshot == null || snapshot.players == null) return null;
        for (var p : snapshot.players) if (p != null && p.playerIndex == id) return p;
        return null;
    }
    private static DaySnapshot.PlayerStatus exactPlayer(ReplaySave replay, int id, int day) {
        for (var s : replay.daySnapshots) if (s != null && s.dayNumber == day) return player(s, id);
        return null;
    }
    private static DaySnapshot.PlayerStatus visiblePlayer(ReplaySave replay, int id, int cutoff) {
        DaySnapshot.PlayerStatus result = null; int latest = -1;
        for (var s : replay.daySnapshots) {
            if (s == null || s.dayNumber > cutoff || s.dayNumber <= latest) continue;
            var p = player(s, id);
            if (p != null) { result = p; latest = s.dayNumber; }
        }
        return result;
    }
    private static String name(int number) {
        return number > 0 && number < CharacterKanjiName.values().length
                ? CharacterKanjiName.values()[number].getShortName() : "?" + number;
    }
}
