package com.vocr.core;

import com.vocr.domain.peiyi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.vocr.domain.whyDie;
import com.vocr.domain.GameResult;
import com.vocr.history.GameRecordManager;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.IntUnaryOperator;
import static org.junit.jupiter.api.Assertions.*;

class FakeHunterDiaryPolicyTest {
    @Test void preparationMustNotWriteBeforeNightDeathsAreKnown() {
        GameContext ctx = fresh(peiyi.tongchang);
        Fabricator f = fabricator(ctx, new HunterGuarder(ctx, ctx.getSuspicion()));
        f.prepareFakeHunterDiaries();
        assertEquals(0, ctx.getSkillTarget(ctx.getRlIndex(1), 3));
    }

    @Test void wolfCandidateLoopMustGenerateBothCandidatesAndApplySoleDeathWeight() throws Exception {
        GameContext ctx = fresh(peiyi.tongchang);
        int actor = ctx.getRlIndex(1), dead = human(ctx,0), other = human(ctx,dead);
        FixedGuarder guarder = new FixedGuarder(ctx,dead,other);
        var ranges = new ArrayList<String>();
        int selected = select(fabricator(ctx,guarder),actor,List.of(dead),1,ranges);
        assertEquals(2,guarder.calls);
        assertEquals(List.of("1..100","1..100","0..50"),ranges);
        assertEquals(other,selected);
    }

    @Test void realSettlementMustAvoidTheSharedRoleWhoDiedThatNight() {
        var ctx=fresh(peiyi.tongchang);int wolf=ctx.getRlIndex(1),victim=ctx.getGyIndex(1);
        ctx.registerClaim(wolf,3);ctx.revealClaim(victim,4,2);
        ctx.setClaimedRoleScheduled(wolf,victim,3,true);
        var f=fabricator(ctx,new HunterGuarder(ctx,ctx.getSuspicion()));
        var actors=f.prepareFakeHunterDiaries();
        assertEquals(0,ctx.getSkillTarget(wolf,3));
        new DielogicCarrier(ctx,ctx.getSuspicion(),()->{}).execute(wolf,victim,0,human(ctx,0));
        ctx.getRandom().startRecording();
        f.completeFakeHunterDiaries(actors,ctx.getDiebody());
        assertTrue(ctx.getRandom().stopRecording().isEmpty());
        int target=ctx.getSkillTarget(wolf,3);
        assertTrue(target>0);assertNotEquals(victim,target);assertTrue(ctx.isAlive(target));
        assertEquals(List.of(victim),ctx.getDiebody());
    }

    @Test void nightKilledActorKeepsDiaryButRealDeadHunterStillCannotAct() {
        var ctx=fresh(peiyi.tongchang);int wolf=ctx.getRlIndex(1);
        var g=new HunterGuarder(ctx,ctx.getSuspicion());var f=fabricator(ctx,g);
        var actors=f.prepareFakeHunterDiaries();assertTrue(actors.contains(wolf));
        ctx.recordDeath(wolf,whyDie.beiyao);
        ctx.getRandom().startRecording();f.completeFakeHunterDiaries(actors,List.of(wolf));
        assertTrue(ctx.getRandom().stopRecording().isEmpty());
        assertTrue(ctx.getSkillTarget(wolf,3)>0);
        assertNotEquals(wolf,ctx.getSkillTarget(wolf,3));
        ctx.recordDeath(ctx.getHunter(),whyDie.chuxing);
        assertEquals(0,g.guard(ctx.getHunter()));
    }

    @Test void repeatedCompletionAndLaterNightsNeverRewriteExistingHistory() {
        var ctx=fresh(peiyi.tongchang);int wolf=ctx.getRlIndex(1);
        var f=fabricator(ctx,new HunterGuarder(ctx,ctx.getSuspicion()));
        var actors=f.prepareFakeHunterDiaries();f.completeFakeHunterDiaries(actors,List.of());
        int saved=ctx.getSkillTarget(wolf,3);assertTrue(saved>0);
        ctx.getRandom().startRecording();f.completeFakeHunterDiaries(actors,List.of(saved));
        assertEquals(saved,ctx.getSkillTarget(wolf,3));
        assertTrue(ctx.getRandom().stopRecording().isEmpty());
        ctx.incrementGameDay();ctx.recordDeath(saved,whyDie.beiyao);
        f.completeFakeHunterDiaries(f.prepareFakeHunterDiaries(),List.of(saved));
        assertEquals(saved,ctx.getSkillTarget(wolf,3));
    }

    @ParameterizedTest @ValueSource(ints={7,9})
    void wolfAndFanaticRerollStaysInsideCandidateLoop(int role) throws Exception {
        var ctx=fresh(role==9?peiyi.kuangxin:peiyi.tongchang);
        int actor=ctx.getActualRoleIndex(role),victim=human(ctx,0),other=human(ctx,victim),teammate=ctx.getRlIndex(1);
        if(teammate==actor)teammate=ctx.getRlIndex(2);
        var g=new FixedGuarder(ctx,victim,other,teammate);var ranges=new ArrayList<String>();
        Class<?> type=Class.forName("com.vocr.core.Fabricator$RandomRange");
        final int[] rolls={0};
        Object rng=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            int min=(int)a[0],max=(int)a[1];ranges.add(min+".."+max);
            return min==1?(rolls[0]++==0?1:100):0;
        });
        var m=Fabricator.class.getDeclaredMethod("selectFakeHunterTarget",int.class,List.class,type);m.setAccessible(true);
        assertEquals(other,m.invoke(fabricator(ctx,g),actor,List.of(victim),rng));
        assertEquals(3,g.calls);assertEquals(List.of("1..100","1..100","0..99"),ranges);
    }

    @ParameterizedTest @ValueSource(ints={7,9})
    void soleDeathIsSoftWeightNotHardExclusionForBothWolfRoles(int role) throws Exception {
        var ctx=fresh(role==9?peiyi.kuangxin:peiyi.tongchang);
        int actor=ctx.getActualRoleIndex(role),victim=human(ctx,0),other=human(ctx,victim);
        var ranges=new ArrayList<String>();
        assertEquals(victim,select(fabricator(ctx,new FixedGuarder(ctx,victim,other)),actor,List.of(victim),0,ranges));
        assertEquals(List.of("1..100","1..100","0..50"),ranges);
    }

    @Test void legacyRandomCompatibilitySelectorKeepsItsOldControlFlow() throws Exception {
        var ctx=fresh(peiyi.tongchang);int actor=ctx.getRlIndex(1),victim=human(ctx,0);
        var g=new FixedGuarder(ctx,victim);var ranges=new ArrayList<String>();
        Class<?> type=Class.forName("com.vocr.core.Fabricator$RandomRange");
        Object rng=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            int min=(int)a[0],max=(int)a[1];ranges.add(min+".."+max);return min==1?100:0;
        });
        var m=Fabricator.class.getDeclaredMethod("selectLegacyFakeHunterTarget",int.class,List.class,type);m.setAccessible(true);
        assertEquals(victim,m.invoke(fabricator(ctx,g),actor,List.of(victim),rng));
        assertEquals(1,g.calls);assertEquals(List.of("1..100","0..99"),ranges);
    }

    @Test void noLivingOtherTargetAndOpeningNightDoNotCreateFictitiousEntries() {
        var ctx=fresh(peiyi.jianyi);int wolf=ctx.getRlIndex(1);
        var g=new HunterGuarder(ctx,ctx.getSuspicion());var f=fabricator(ctx,g);
        var actors=f.prepareFakeHunterDiaries();
        for(int i=1;i<=ctx.getPlayerSum();i++)if(i!=wolf)ctx.recordDeath(i,whyDie.beiyao);
        f.completeFakeHunterDiaries(actors,List.of());assertEquals(0,ctx.getSkillTarget(wolf,3));
        var status=new GameStatus(new GameRandom(2));status.startGame(peiyi.jianyi);
        var first=new GameContext(status,NonHumanConfiguration.from(peiyi.jianyi,0));
        assertTrue(fabricator(first,new HunterGuarder(first,first.getSuspicion())).prepareFakeHunterDiaries().isEmpty());
    }

    @Test void terminalNightCompletesDiariesBeforeTheFinalSnapshot() throws Exception {
        var stats=GameRecordManager.inMemory();var logic=new MainLogic(event->{},stats,new GameRandom(4202L));
        logic.start(peiyi.tongchang,stats);GameContext ctx=(GameContext)logic.getGameContext();
        int actor=0;
        for(int i=1;i<=ctx.getPlayerSum();i++)
            if(ctx.getActualRole(i)==7&&ctx.getClaimedRole(i)==0){actor=i;break;}
        assertTrue(actor>0,"固定开局存在尚未CO的狼");ctx.setNonHumanPlan(actor,NonHumanPlan.NONE);
        int first=human(ctx,0),second=human(ctx,first);
        boolean executed=false;
        for(int i=1;i<=ctx.getPlayerSum();i++)
            if(i!=actor&&i!=first&&i!=second&&ctx.isAlive(i)) {
                ctx.recordDeath(i,executed?whyDie.beiyao:whyDie.chuxing);executed=true;
            }
        assertEquals(3,ctx.getAliveCounter());assertEquals(0,ctx.getSkillTarget(actor,ctx.getGameDay()));
        int day=ctx.getGameDay();
        Method night=MainLogic.class.getDeclaredMethod("nightaction");night.setAccessible(true);night.invoke(logic);
        assertEquals(GameResult.WOLF_WIN,ctx.getEndResult());
        assertTrue(ctx.getSkillTarget(actor,day)>0,"终局夜必须在提前return前保存假日记");
        var replay=com.vocr.replay.ReplaySave.fromRecords(0,logic.getRecorder().getRecords());
        final int id=actor;
        var saved=Arrays.stream(replay.daySnapshots.getLast().players).filter(p->p.playerIndex==id).findFirst().orElseThrow();
        assertEquals(ctx.getSkillTarget(actor,day),saved.skillTargets[day]);
    }

    static GameContext fresh(peiyi p) {
        var status = new GameStatus(new GameRandom(20261111L));status.startGame(p);
        var ctx = new GameContext(status,NonHumanConfiguration.from(p,0));
        while(ctx.getGameDay()<3)ctx.incrementGameDay();return ctx;
    }
    static int human(GameContext c,int excluded) {
        for(int i=1;i<=c.getPlayerSum();i++)if(i!=excluded&&c.isAlive(i)&&c.getActualRole(i)==6)return i;
        throw new AssertionError("no villager");
    }
    static Fabricator fabricator(GameContext c,HunterGuarder g) {
        return new Fabricator(c,c.getSuspicion(),new SeerDiviner(c,c.getSuspicion()),g);
    }
    static class FixedGuarder extends HunterGuarder {
        int calls;final int[] targets;
        FixedGuarder(GameContext c,int...targets){super(c,c.getSuspicion());this.targets=targets;}
        @Override int guard(int actor,IntUnaryOperator rng){return targets[(calls++)%targets.length];}
        @Override int guardForDiary(int actor,IntUnaryOperator rng){return targets[(calls++)%targets.length];}
    }
    static int select(Fabricator f,int actor,List<Integer> deaths,int index,List<String> ranges) throws Exception {
        Class<?> type=Class.forName("com.vocr.core.Fabricator$RandomRange");
        Object rng=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(p,m,a)->{
            int min=(int)a[0],max=(int)a[1];ranges.add(min+".."+max);return min==1?100:index;
        });
        Method m=Fabricator.class.getDeclaredMethod("selectFakeHunterTarget",int.class,List.class,type);
        m.setAccessible(true);return (int)m.invoke(f,actor,deaths,rng);
    }
}
