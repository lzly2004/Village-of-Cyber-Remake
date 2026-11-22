package com.vocr.core;

import com.vocr.domain.Event;
import com.vocr.domain.EventName;
import com.vocr.domain.GameResult;
import com.vocr.domain.peiyi;
import com.vocr.domain.whyDie;
import com.vocr.history.GameRecordManager;
import com.vocr.replay.ReplaySave;
import com.vocr.application.BusinessPhase;
import com.vocr.application.EventPresentationStage;
import com.vocr.application.GameSession;
import com.vocr.application.GameEventBuffer;
import com.vocr.application.GameUpdate;
import com.vocr.application.StartGameCommand;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TerminalNightPresentationTest {
    @Test void wolfWinSkipsNightDeathButKeepsTheFinalDeathRecord() throws Exception {
        Scenario s = scenario(GameResult.WOLF_WIN, false);
        s.run();
        s.assertTerminal(GameResult.WOLF_WIN, EventName.rlsl);
    }

    @Test void foxWinSkipsNightDeathButKeepsTheFinalDeathRecord() throws Exception {
        Scenario s = scenario(GameResult.FOX_WIN, false);
        s.run();
        s.assertTerminal(GameResult.FOX_WIN, EventName.yhsl);
    }

    @Test void villageWinSkipsBothCatAndWolfDeathsButKeepsBothInReplay() throws Exception {
        Scenario s = scenario(GameResult.VILLAGE_WIN, false);
        s.run();
        s.assertTerminal(GameResult.VILLAGE_WIN, EventName.crsl);
        assertEquals(Set.of(s.victim, s.wolf), new HashSet<>(s.ctx.getDiebody()));
        assertEquals(whyDie.nightmaozhou, s.ctx.getDeathReason(s.wolf));
    }

    @Test void unfinishedNightStillPublishesDeathsBeforeDayDialogue() throws Exception {
        Scenario s = scenario(GameResult.NONE, false);
        s.run();
        assertEquals(GameResult.NONE, s.ctx.getEndResult());
        assertEquals(EventName.yjsw, s.events.getFirst().eventname());
        assertEquals(1, s.events.stream().filter(e -> e.eventname() == EventName.yjsw).count());
        assertEquals(whyDie.beiyao, s.ctx.getDeathReason(s.victim));
        assertEquals(s.day + 1, s.ctx.getGameDay(), "非终局继续进入下一白天");
        assertEquals(BusinessPhase.DAY_DECISION, s.session.currentUpdate().phase());
    }

    @Test void unfinishedPeacefulNightStillPublishesItsNotice() throws Exception {
        Scenario s = scenario(GameResult.NONE, true);
        s.run();
        assertEquals(GameResult.NONE, s.ctx.getEndResult());
        assertEquals(EventName.wsw, s.events.getFirst().eventname());
        assertTrue(s.events.stream().noneMatch(e -> e.eventname() == EventName.yjsw));
        assertTrue(s.ctx.getDiebody().isEmpty());
        assertEquals(s.day + 1, s.ctx.getGameDay());
    }

    private Scenario scenario(GameResult result, boolean peaceful) throws Exception {
        List<Event> events = new ArrayList<>();
        GameEventBuffer buffer = new GameEventBuffer();
        var records = GameRecordManager.inMemory();
        MainLogic logic = new MainLogic(event -> { events.add(event); buffer.publish(event); }, records, new GameRandom(4207L));
        GameSession session = new GameSession(logic, buffer);
        session.startGame(new StartGameCommand(0, peiyi.daxing));
        GameUpdate before = session.currentUpdate(0);
        while (session.pollNextPresentedEvent() != null) { /* Desktop already read the opening. */ }
        GameContext ctx = (GameContext) logic.getGameContext();
        int wolf = livingRole(ctx, 7).getFirst();
        List<Integer> humans = livingRole(ctx, 6);
        int victim = result == GameResult.VILLAGE_WIN ? livingRole(ctx, 5).getFirst() : humans.getFirst();
        Set<Integer> keep = new HashSet<>(List.of(wolf, victim));
        if (result == GameResult.FOX_WIN) keep.add(livingRole(ctx, 10).getFirst());
        int humanCount = result == GameResult.NONE ? 3 : result == GameResult.VILLAGE_WIN ? 2 : 1;
        for (int human : humans) {
            if (humanCount == 0) break;
            if (keep.add(human)) humanCount--;
        }
        boolean executed = false;
        for (int i = 1; i <= ctx.getPlayerSum(); i++) {
            if (ctx.isAlive(i) && !keep.contains(i)) {
                ctx.recordDeath(i, executed ? whyDie.beiyao : whyDie.chuxing);
                executed = true;
            }
            ctx.setNonHumanPlan(i, NonHumanPlan.NONE);
        }
        assertTrue(executed, "夜结算前有已处刑者，保留真实灵能查询前提");
        inject(logic, "wolfBiter", new WolfBiter(ctx, ctx.getSuspicion()) {
            @Override public int[] decideBite() { return new int[]{wolf, victim}; }
        });
        inject(logic, "seerDiviner", new SeerDiviner(ctx, ctx.getSuspicion()) {
            @Override public int divine(int ignored) { return 0; }
        });
        inject(logic, "hunterGuarder", new HunterGuarder(ctx, ctx.getSuspicion()) {
            @Override public int guard(int ignored) { return peaceful ? victim : 0; }
        });
        events.clear();
        return new Scenario(logic, ctx, events, wolf, victim, ctx.getGameDay(), session, before);
    }

    private static List<Integer> livingRole(GameContext ctx, int role) {
        List<Integer> players = new ArrayList<>();
        for (int i = 1; i <= ctx.getPlayerSum(); i++)
            if (ctx.isAlive(i) && ctx.getActualRole(i) == role) players.add(i);
        assertFalse(players.isEmpty(), "固定开局必须有存活身份 " + role);
        return players;
    }

    private static void inject(MainLogic logic, String name, Object value) throws Exception {
        Field field = MainLogic.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(logic, value);
    }

    private record Scenario(MainLogic logic, GameContext ctx, List<Event> events, int wolf, int victim, int day,
                            GameSession session, GameUpdate before) {
        void run() throws Exception {
            Method method = MainLogic.class.getDeclaredMethod("nightaction");
            method.setAccessible(true);
            method.invoke(logic);
        }

        void assertTerminal(GameResult result, EventName notice) {
            assertEquals(result, ctx.getEndResult());
            assertEquals(List.of(notice), events.stream().map(Event::eventname).toList(),
                    "终局夜仅发布胜利事件，不发布死亡／平安公告");
            var update = session.currentUpdate(before.nextEventCursor());
            assertEquals(BusinessPhase.FINISHED, update.phase());
            assertEquals(before.nextEventCursor(), update.eventCursor());
            assertEquals(before.nextEventCursor() + 1, update.nextEventCursor());
            assertEquals(List.of(notice), update.events().stream().map(Event::eventname).toList());
            assertEquals(List.of(EventPresentationStage.SETTLEMENT), update.presentedEvents().stream().map(p -> p.stage()).toList());
            var pending = session.peekNextPresentedEvent();
            assertNotNull(pending);
            assertEquals(notice, pending.event().eventname());
            assertEquals(update, session.currentUpdate(before.nextEventCursor()), "重复读取内容一致");
            assertEquals(pending, session.peekNextPresentedEvent(), "续读不能消费桌面待确认事件");
            assertEquals(before.events(), session.currentUpdate(0).events().subList(0, before.events().size()),
                    "已发布的开局历史不被终局清理删除");
            assertEquals(day, ctx.getGameDay(), "终局不进入下一白天");
            assertFalse(ctx.isAlive(victim));
            var replay = ReplaySave.fromRecords(0, logic.getRecorder().getRecords());
            var last = replay.daySnapshots.getLast();
            for (int dead : ctx.getDiebody()) {
                var saved = Arrays.stream(last.players).filter(p -> p.playerIndex == dead).findFirst().orElseThrow();
                assertEquals(ctx.getDeathDay(dead), saved.deathDay);
                assertEquals(ctx.getDeathReason(dead).ordinal(), saved.deathReason);
            }
        }
    }
}
