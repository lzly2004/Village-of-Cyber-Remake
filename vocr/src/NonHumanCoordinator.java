import java.util.ArrayList;
import java.util.List;

public class NonHumanCoordinator
{
    private final GameModule module;
    private final GameContext ctx;
    private final SuspicionSystem suspicion;
    private final COManager coManager;
    private final ResultEventGenerator eventGenerator;
    private final Runnable gylogic;
    private final Runnable deliverEvents;

    public NonHumanCoordinator(GameModule module)
    {
        this.module = module;
        this.ctx = module.getCtx();
        this.suspicion = module.getSuspicion();
        this.coManager = module.getCoManager();
        this.eventGenerator = new ResultEventGenerator(ctx);
        this.gylogic = module.getGylogic();
        this.deliverEvents = module.getDeliverEvents();
    }

    public void coordinate(ArrayList<Integer> diebody)
    {
        int n = ctx.getPlayerSum();
        int gd = ctx.getGameDay();

        generatePreliminaryDivinations(n);
        initNonHumanPlans();
        int rlz = assignWolfSeerTargets(n, diebody);
        assignWolfMediumTargets(rlz);
        assignMadmanFanaticStrategy(n, diebody);
        assignFoxStrategy(n, diebody);
        assignDeviantStrategy(n, diebody);

        handleDaytimeCO(diebody, gd);
    }

    private void generatePreliminaryDivinations(int n)
    {
        DebugLogger.log("非人的初始工作：所有非人都生成一份预备占文");
        for(int i=1;i<=n;i++)
        {
            if(GameLogicUtils.feiren(ctx.getActualRole(i)) == 0) continue;
            int[] weight = new int[n+1];
            for(int j=1;j<=n;j++)
            {
                if(i == j)
                {
                    weight[j] = -GameConstants.INF;
                    continue;
                }
                if(ctx.getActualRole(i) != 7 && ctx.getActualRole(i) != 9)
                {
                    weight[j] = 1;
                    continue;
                }
                if(ctx.getActualRole(j) != 7) weight[j] = 1;
                else weight[j] = -GameConstants.INF;
            }
            ctx.ybzw[i] = suspicion.getOne(weight);
            DebugLogger.log("非人的预备占文，非人:"+i+",预备占文:"+ctx.getYBZW(i));
        }
    }

    private void initNonHumanPlans()
    {
        DebugLogger.log("非人的初始工作：非人决定自己的计划倾向");
        for(int i=1;i<=ctx.initialWolfCount;i++)
        {
            DebugLogger.log("rlindex[i]:" + ctx.rlindex[i]);
            if(ctx.isDead(ctx.rlindex[i])) continue;
            ctx.nonHumanPlan[ctx.rlindex[i]] = 4;
        }
    }

    private int assignWolfSeerTargets(int n, ArrayList<Integer> diebody)
    {
        DebugLogger.log("非人的初始工作：人狼上占");
        int rlzhan = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(10-ctx.kz, (11-ctx.initialWolfCount)*15, ctx.kz + (ctx.initialWolfCount-2)*3)));
        int rlz = rlzhan;
        while(rlzhan != 0)
        {
            int rl = ConstNum.randomInt(1,ctx.initialWolfCount);
            if(ctx.isDead(ctx.rlindex[rl]) || ctx.zw[ctx.rlindex[rl]] != 0) continue;
            rlzhan--;
            ctx.nonHumanPlan[ctx.rlindex[rl]] = 1;
            int op = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(80-2*ctx.kz, 15, 5+ctx.kz, ctx.kz)));
            int[] weight = new int[n+1];
            for(int i=1;i<=n;i++)
            {
                if((op == 1 || op == 2) && ctx.getActualRole(i) == 7 && ctx.rlindex[rl] != i)
                {
                    weight[i] = 1;
                }
                else if((op == 0 || op == 3) && ctx.getActualRole(i) != 7 && ctx.getYBZW(ctx.rlindex[rl]) != i)
                {
                    weight[i] = 1;
                }
                else weight[i] = -GameConstants.INF;
            }
            int target = suspicion.getOne(weight);
            if(op == 0 || op == 2 || diebody.contains(target))
                ctx.zw[ctx.rlindex[rl]] = target;
            else
                ctx.zw[ctx.rlindex[rl]] = target + n;
            DebugLogger.log("人狼上占的占文，人狼："+rl+",占文："+ ctx.zw[ctx.rlindex[rl]]);
        }
        return rlz;
    }

    private void assignWolfMediumTargets(int rlz)
    {
        DebugLogger.log("非人的初始工作：人狼上灵");
        int p;
        if(ctx.initialWolfCount == 2)
        {
            if(rlz == 0)  p = ctx.kz + 30;
            else if(rlz == 1) p = 0;
            else p = 0;
        }
        else if(ctx.initialWolfCount == 3)
        {
            if(rlz == 0) p = ctx.kz + 50;
            else if(rlz == 1) p = ctx.kz + 30;
            else p = 0;
        }
        else
        {
            if(rlz == 0) p = ctx.kz + 60;
            else if(rlz == 1) p = ctx.kz + 30;
            else p = ctx.kz + 10;
        }
        if(GameLogicUtils.probabilityJudge(p))
        {
            int rl;
            while(true)
            {
                rl = ConstNum.randomInt(1,ctx.initialWolfCount);
                if(ctx.isDead(ctx.rlindex[rl]) || ctx.zw[ctx.rlindex[rl]] != 0) continue;
                ctx.nonHumanPlan[ctx.rlindex[rl]] = 2;
                break;
            }
        }
    }

    private void assignMadmanFanaticStrategy(int n, ArrayList<Integer> diebody)
    {
        DebugLogger.log("非人的初始工作：狂人狂信策略");
        if(ctx.getPeiyi() == peiyi.kuangxin || ctx.getPeiyi() == peiyi.daxing) ctx.kyojin = 0;
        int kop = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(55-20*ctx.getKyojin(),35+25*ctx.getKyojin(),10-5*ctx.getKyojin())));
        int leader = ctx.getNonHumanLeader();
        if(kop == 1)
            ctx.nonHumanPlan[leader] = 2;
        else if(kop == 2)
            ctx.nonHumanPlan[leader] = 4;
        else
        {
            ctx.nonHumanPlan[leader] = 1;
            if(ctx.getKyojin() == 1)
            {
                int target,option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(75-ctx.kz,25+ctx.kz)));
                while(true)
                {
                    target = ConstNum.randomInt(1,n);
                    DebugLogger.log("狂人占文对象：" + target);
                    if(target == ctx.getYBZW(leader) || target == leader) continue;
                    if(option == 0 || diebody.contains(target))
                        ctx.zw[leader] = target;
                    else
                        ctx.zw[leader] = target + n;
                    break;
                }
            }
            else
            {
                int target,option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(70-2*ctx.kz,20,ctx.kz+10,ctx.kz)));
                while(true)
                {
                    target = ConstNum.randomInt(1,n);
                    if(target == ctx.getYBZW(leader) || target == leader) continue;
                    if(ctx.getActualRole(target) == 7 && option < 2) continue;
                    if(ctx.getActualRole(target) != 7 && option > 1) continue;
                    DebugLogger.log("狂信占文对象：" + target);
                    if(option == 0 || option == 2 || diebody.contains(target))
                        ctx.zw[leader] = target;
                    else
                        ctx.zw[leader] = target + n;
                    DebugLogger.log("狂信占文：" + ctx.zw[leader]);
                    break;
                }
            }
        }
    }

    private void assignFoxStrategy(int n, ArrayList<Integer> diebody)
    {
        DebugLogger.log("非人的初始工作：妖狐策略");
        if(ctx.getFox() > 0 && ctx.isAlive(ctx.getFox()))
        {
            int op = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(5+ctx.kz,ctx.kz,95-2*ctx.kz)));
            if(op == 1)
                ctx.nonHumanPlan[ctx.getFox()] = 2;
            else if(op == 2)
                ctx.nonHumanPlan[ctx.getFox()] = 4;
            else
            {
                ctx.nonHumanPlan[ctx.getFox()] = 1;
                int target, option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(90 - ctx.kz, 10 + ctx.kz)));
                while (true)
                {
                    target = ConstNum.randomInt(1, n);
                    if (target == ctx.getYBZW(ctx.getFox()) || target == ctx.getFox()) continue;
                    DebugLogger.log("妖狐占文对象：" + target);
                    if (option == 0 || diebody.contains(target))
                        ctx.zw[ctx.getFox()] = target;
                    else
                        ctx.zw[ctx.getFox()] = target + n;
                    DebugLogger.log("妖狐占文：" + ctx.zw[ctx.getFox()]);
                    break;
                }
            }
        }
    }

    private void assignDeviantStrategy(int n, ArrayList<Integer> diebody)
    {
        DebugLogger.log("非人的初始工作：背德策略");
        if(ctx.getDeviant() > 0 && ctx.isAlive(ctx.getDeviant()))
        {
            int bop = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(50+ctx.kz,5,45-ctx.kz)));
            if(bop == 1)
                ctx.nonHumanPlan[ctx.getDeviant()] = 2;
            else if(bop == 2)
                ctx.nonHumanPlan[ctx.getDeviant()] = 4;
            else
            {
                DebugLogger.log("背德开始选择占文");
                ctx.nonHumanPlan[ctx.getDeviant()] = 1;
                int target, option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(60 - ctx.kz, 20, 20+ctx.kz, 0)));
                if(option == 2)
                {
                    ctx.zw[ctx.getDeviant()] = ctx.getFox();
                    DebugLogger.log("狐狸白球");
                }
                else
                    while (true)
                    {
                        target = ConstNum.randomInt(1, n);
                        DebugLogger.log("背德选择占文:"+target);
                        if (target == ctx.getYBZW(ctx.getDeviant()) || target == ctx.getDeviant())
                            continue;
                        if (ctx.getActualRole(target) == 10) continue;
                        if (option == 0 || diebody.contains(target))
                            ctx.zw[ctx.getDeviant()] = target;
                        else
                            ctx.zw[ctx.getDeviant()] = target + n;
                        DebugLogger.log("背德占文:" + ctx.zw[ctx.getDeviant()]);
                        break;
                    }
            }
        }
    }

    private void handleDaytimeCO(ArrayList<Integer> diebody, int gd)
    {
        DebugLogger.log("白天临时改变策略,以及占灵共的行动");
        if(diebody.size() > 1)
        {
            handleCurseKillScenario(diebody, gd);
        }
        else
        {
            handleNormalScenario(diebody, gd);
        }
    }

    // ============================================================
    // 普通场景（非咒杀日）协调者方法
    // ============================================================

    /**
     * 普通场景上下文 — 封装 handleNormalScenario 各阶段的中间状态
     */
    private static class NormalScenarioContext {
        int n;
        int gd;
        boolean gqian;                  // 是否有共欠CO时机
        ArrayList<IntPair> response;     // 接黒回复列表
        boolean havecatco;              // 是否有猫又CO
        int jhzhan;                    // 计划上占的人数
        int jhling;                    // 计划上灵的人数
    }

    private void handleNormalScenario(ArrayList<Integer> diebody, int gd)
    {
        NormalScenarioContext c = new NormalScenarioContext();
        c.n = ctx.getPlayerSum();
        c.gd = gd;
        c.gqian = ctx.getPeiyi() != peiyi.jianyi && ctx.getActualRole(diebody.get(0)) == 4;
        c.response = new ArrayList<>();
        c.havecatco = false;
        c.jhzhan = 0;
        c.jhling = 0;

        // Phase 1: 初始CO设置（占/灵列表构建 + 共逻辑）
        executeInitialCOSetup();

        // Phase 2: 裁剪占CO（最多3人）
        trimSeerCOToMax3();

        // Phase 3: 黑球占/灵CO时机
        processBlackBallCOSecondary(diebody);

        // Phase 4: 处理接黒回复
        if(ctx.zhans.size() == 0) {
            DebugLogger.log("本次无占，共逻辑");
            gylogic.run();
        } else {
            handleBlackResponses(c.n, c.response);
        }

        // Phase 5: 处理猫又CO响应 + 裁剪响应 + 处理剩余响应
        processCatCOResponses(c, diebody);

        // Phase 6: 询问猫又CO
        if(c.havecatco)
            coManager.askCoByRole(Role.mao);

        // Phase 7: 处理未CO玩家的剩余占/灵计划
        handleRemainingCOForUnclaimed(c);

        // Phase 8: 白球占/灵CO时机（条件触发）
        executeWhiteBallCOConditional(c, diebody);
    }

    private void executeInitialCOSetup()
    {
        DebugLogger.log("非人的初始工作：占卜co");
        buildSeerCOList();
        DebugLogger.log("非人的初始工作：灵能co");
        buildMediumCOList();
        DebugLogger.log("非人的初始工作：共逻辑");
        gylogic.run();
        DebugLogger.log("非人的初始工作：真占（已在占卜co阶段加入，跳过重复添加）");
    }

    private void trimSeerCOToMax3()
    {
        ctx.shuffleZhans();
        while(ctx.zhans.size() > 3)
        {
            ctx.shuffleZhans();
            if(ctx.zhans.get(0) == ctx.getActualRoleIndex(1)) continue;
            ctx.setSkillTarget(ctx.zhans.get(0), 1, 0);
            ctx.nonHumanPlan[ctx.zhans.get(0)] = 0;
            ctx.zhans.remove(0);
        }
    }

    private void processBlackBallCOSecondary(ArrayList<Integer> diebody)
    {
        DebugLogger.log("非人的初始工作：黑球占co时机");
        for(int i=0;i<ctx.zhans.size();i++)
        {
            coManager.processActualCo(ctx.zhans.get(i),1,diebody);
        }
        for(int i=0;i<ctx.lings.size();i++)
        {
            coManager.processActualCo(ctx.lings.get(i),2,diebody);
        }
    }

    /**
     * Phase 5: 处理猫又CO响应、裁剪响应、处理剩余响应
     */
    private void processCatCOResponses(NormalScenarioContext c, ArrayList<Integer> diebody)
    {
        DebugLogger.log("本次有无猫又co：");
        c.response = GameLogicUtils.shuffleList(c.response);

        // 统计已CO的狼队占/灵数量
        int lzsum = 0, llsum = 0;
        for(int i=1;i<=ctx.initialWolfCount;i++)
        {
            if(ctx.getClaimedRole(ctx.rlindex[i]) == 1)
                lzsum++;
            else if(ctx.getClaimedRole(ctx.rlindex[i]) == 2)
                llsum++;
        }

        // 过滤猫又CO响应
        for(int i=0;i<c.response.size();i++)
        {
            if(c.response.get(i).first() != 7) continue;
            if(c.response.get(i).second() == 2)
            {
                if(llsum > 0)
                {
                    int removedPlayer = c.response.get(i).first();
                    c.response.remove(i);
                    ctx.nonHumanPlan[removedPlayer] = 0;
                    i--;
                    continue;
                }
                llsum++;
            }
            if(c.response.get(i).second() == 1)
            {
                if(lzsum > 1)
                {
                    int removedPlayer = c.response.get(i).first();
                    c.response.remove(i);
                    ctx.nonHumanPlan[removedPlayer] = 0;
                    i--;
                    continue;
                }
                lzsum++;
            }
        }

        // 统计实际要上占/灵的数量
        int rezhan = 0, reling = 0;
        for(int i=0;i<c.response.size();i++)
        {
            if(c.response.get(i).second() == 1)
                rezhan++;
            else if(c.response.get(i).second() == 2)
                reling++;
        }

        // 裁剪占CO（最多4人）
        while(ctx.zhans.size() + rezhan > 4 && !c.response.isEmpty())
        {
            c.response = GameLogicUtils.shuffleList(c.response);
            if(c.response.get(0).second() == 1 && c.response.get(0).first() != ctx.getActualRoleIndex(1))
            {
                ctx.nonHumanPlan[c.response.get(0).first()] = 0;
                ctx.setSkillTarget(c.response.get(0).first(), 1, 0);
                c.response.remove(0);
            }
        }

        // 裁剪灵CO（最多3人）
        while(ctx.lings.size() + reling > 3 && !c.response.isEmpty())
        {
            c.response = GameLogicUtils.shuffleList(c.response);
            if(c.response.get(0).second() == 2 && c.response.get(0).first() != ctx.getActualRoleIndex(2))
            {
                ctx.nonHumanPlan[c.response.get(0).first()] = 0;
                c.response.remove(0);
            }
        }

        // 处理剩余响应
        for(int i=0;i<c.response.size();i++)
        {
            int respPlayer = c.response.get(i).first();
            int role = c.response.get(i).second();
            boolean alreadyProcessed = false;
            if(role == 1 && ctx.zhans.contains(respPlayer)) alreadyProcessed = true;
            if(role == 2 && ctx.lings.contains(respPlayer)) alreadyProcessed = true;

            ctx.addLazySuspicionValue(respPlayer, GameConstants.SUSPICION_INCREASE_BLACK_RESPONSE);
            if(!alreadyProcessed)
                coManager.processActualCo(respPlayer, role, diebody);
            if(role == 5)
            {
                c.havecatco = true;
            }
        }
    }

    private void handleRemainingCOForUnclaimed(NormalScenarioContext c)
    {
        int n = c.n;
        for(int i=1;i<=n;i++)
        {
            if(ctx.isAlive(i) && ctx.getClaimedRole(i) == 0)
            {
                if(ctx.getActualRole(i) == 1 || ctx.nonHumanPlan[i] == 1) c.jhzhan++;
                else if(ctx.getActualRole(i) == 2 || ctx.nonHumanPlan[i] == 2) c.jhling++;
            }
        }
        while(ctx.zhans.size() + c.jhzhan > 5 || ctx.lings.size() + c.jhling > 4)
        {
            int target = ConstNum.randomInt(1, n);
            if(ctx.getClaimedRole(target) == 0 && ctx.nonHumanPlan[target] > 0 && ctx.nonHumanPlan[target] < 3)
            {
                if(ctx.zhans.size() + c.jhzhan > 5 && ctx.nonHumanPlan[target] == 1)
                {
                    c.jhzhan--;
                    ctx.nonHumanPlan[target] = 0;
                    ctx.setSkillTarget(target, 1, 0);
                }
                else if(ctx.lings.size() + c.jhling > 4 && ctx.nonHumanPlan[target] == 2)
                {
                    c.jhling--;
                    ctx.nonHumanPlan[target] = 0;
                }
            }
        }
    }

    private void executeWhiteBallCOConditional(NormalScenarioContext c, ArrayList<Integer> diebody)
    {
        if(ctx.zhans.size() == 0 || c.response.size() > 0 || c.gqian || c.havecatco)
        {
            DebugLogger.log("非人的初始工作：白球占、灵能co");
            for(int i=1;i<=c.n;i++)
            {
                if(ctx.getClaimedRole(i) != 0 || ctx.isDead(i)) continue;
                if(ctx.getActualRole(i) < 3)
                {
                    coManager.processActualCo(i, ctx.getActualRole(i), diebody);
                }
                else if(ctx.nonHumanPlan[i] == 1 || ctx.nonHumanPlan[i] == 2)
                {
                    if(ctx.nonHumanPlan[i] == 1)
                    {
                        ctx.setSkillTarget(i, 1, ctx.zw[i]);
                        DebugLogger.log("添加占文：角色" + i + " 占文" + ctx.getSkillTarget(i, 1));
                    }
                    coManager.processActualCo(i, ctx.nonHumanPlan[i], diebody);
                }
            }
            if((ctx.gyindex[1] <= 0 || ctx.getClaimedRole(ctx.gyindex[1]) != 4) &&
               (ctx.gyindex[2] <= 0 || ctx.getClaimedRole(ctx.gyindex[2]) != 4))
                gylogic.run();
        }
        c.response.clear();
    }

    private void handleCurseKillScenario(ArrayList<Integer> diebody, int gd)
    {
        DebugLogger.log("非人的初始工作：明确咒杀");
        DebugLogger.log("非人的初始工作：人狼上占");
        boolean haverl = false;
        for(int i=1;i<=ctx.initialWolfCount;i++)
        {
            if(ctx.nonHumanPlan[ctx.rlindex[i]] != 1) continue;
            if(diebody.contains(ctx.zw[ctx.rlindex[i]]))
            {
                haverl = true;
                ctx.zhans.add(ctx.rlindex[i]);
                ctx.setSkillTarget(ctx.rlindex[i], 1, ctx.zw[ctx.rlindex[i]]);
                continue;
            }
            if(diebody.contains(ctx.getYBZW(ctx.rlindex[i])))
            {
                haverl = true;
                ctx.zhans.add(ctx.rlindex[i]);
                ctx.setSkillTarget(ctx.rlindex[i], 1, ctx.getYBZW(ctx.rlindex[i]));
                continue;
            }
            ctx.nonHumanPlan[ctx.rlindex[i]] = 0;
        }
        if(!haverl)
        {
            for(int i=1;i<=ctx.initialWolfCount;i++)
            {
                if(ctx.nonHumanPlan[ctx.rlindex[i]] == 1) continue;
                if(diebody.contains(ctx.getYBZW(ctx.rlindex[i])))
                {
                    haverl = true;
                    ctx.zhans.add(ctx.rlindex[i]);
                    ctx.setSkillTarget(ctx.rlindex[i], 1, ctx.getYBZW(ctx.rlindex[i]));
                    ctx.nonHumanPlan[ctx.rlindex[i]] = 1;
                }
            }
        }

        DebugLogger.log("非人的初始工作：狂人狂信上占");
        int leader = ctx.getNonHumanLeader();
        if(ctx.isAlive(leader) && diebody.contains(ctx.getYBZW(leader)))
        {
            ctx.nonHumanPlan[leader] = 1;
            ctx.setSkillTarget(leader, 1, ctx.getYBZW(leader));
            ctx.zhans.add(leader);
        }
        else if(ctx.nonHumanPlan[leader] == 1)
            ctx.nonHumanPlan[leader] = 0;
        DebugLogger.log("非人的初始工作：真占co");
        if(ctx.isAlive(ctx.getActualRoleIndex(1)))
            ctx.zhans.add(ctx.getActualRoleIndex(1));
        DebugLogger.log("非人的初始工作：占文随机排序");
        ctx.shuffleZhans();
        for(int i=0;i<ctx.zhans.size();i++)
        {
            ctx.eventarray.add(new Event(EventName.zs14,ctx.getCharacterName(ctx.zhans.get(i)),
                    ctx.getCharacterName(ctx.getSkillTarget(ctx.zhans.get(i), 1))));
            ctx.setClaimedRole(ctx.zhans.get(i), 1);
            if(ctx.getClaimedRoleOrder(ctx.zhans.get(i)) == 0)
                ctx.setClaimedRoleOrder(ctx.zhans.get(i), ctx.incrementClaimedRoleOrder(1));
            ctx.setComingOutDay(ctx.zhans.get(i), gd);
            ctx.addLazySuspicionValue(ctx.zhans.get(i), GameConstants.SUSPICION_DECREASE_SEER_CO);
            for(int j=0;j<i;j++)
                suspicion.updateTop3Aux2(ctx.zhans.get(i),ctx.zhans.get(j),GameConstants.INF,GameConstants.INF);
        }
        DebugLogger.log("非人的初始工作：灵能co");
        DebugLogger.log("非人的初始工作：遍历每个人狼，判断是否有计划上灵的人狼");
        for(int i=1;i<=ctx.initialWolfCount;i++)
        {
            if(ctx.nonHumanPlan[ctx.rlindex[i]] != 2) continue;
            ctx.lings.add(ctx.rlindex[i]);
            break;
        }
        DebugLogger.log("非人的初始工作：狂灵信灵");
        if(ctx.getNonHumanLeader() > 0 && ctx.isAlive(ctx.getNonHumanLeader()) && ctx.nonHumanPlan[ctx.getNonHumanLeader()] == 2)
            ctx.lings.add(ctx.getNonHumanLeader());
        DebugLogger.log("非人的初始工作：真灵");
        if(ctx.isAlive(ctx.getActualRoleIndex(2)))
            ctx.lings.add(ctx.getActualRoleIndex(2));
        DebugLogger.log("非人的初始工作：灵能随机排序");
        ctx.shuffleLings();
        for(int i=0;i<ctx.lings.size();i++)
        {
            eventGenerator.addEvent(EventName.lnco18, ctx.lings.get(i));
            ctx.setClaimedRole(ctx.lings.get(i), 2);
            if(ctx.getClaimedRoleOrder(ctx.lings.get(i)) == 0)
                ctx.setClaimedRoleOrder(ctx.lings.get(i), ctx.incrementClaimedRoleOrder(2));
            ctx.setComingOutDay(ctx.lings.get(i), gd);
            ctx.addLazySuspicionValue(ctx.lings.get(i), GameConstants.SUSPICION_DECREASE_MEDIUM_CO);
            for(int j=0;j<i;j++)
                suspicion.updateTop3Aux2(ctx.lings.get(i),ctx.lings.get(j),GameConstants.INF,GameConstants.INF);
        }
        DebugLogger.log("非人的初始工作：共逻辑");
        gylogic.run();
    }

    private void buildSeerCOList()
    {
        DebugLogger.log("非人的初始工作：狼占");
        for (int i=1;i<=ctx.initialWolfCount;i++)
        {
            if(ctx.isDead(ctx.rlindex[i])) continue;
            if(ctx.nonHumanPlan[ctx.rlindex[i]] == 1)
            {
                ctx.zhans.add(ctx.rlindex[i]);
                ctx.setSkillTarget(ctx.rlindex[i], 1, ctx.zw[ctx.rlindex[i]]);
            }
        }
        DebugLogger.log("非人的初始工作：狂占信占");
        if(ctx.getNonHumanLeader() > 0 && ctx.nonHumanPlan[ctx.getNonHumanLeader()] == 1)
        {
            ctx.zhans.add(ctx.getNonHumanLeader());
            ctx.setSkillTarget(ctx.getNonHumanLeader(), 1, ctx.zw[ctx.getNonHumanLeader()]);
        }
        DebugLogger.log("非人的初始工作：狐占");
        if(ctx.getFox() > 0 && ctx.nonHumanPlan[ctx.getFox()] == 1)
        {
            ctx.zhans.add(ctx.getFox());
            ctx.setSkillTarget(ctx.getFox(), 1, ctx.zw[ctx.getFox()]);
        }
        DebugLogger.log("非人的初始工作：背占");
        if(ctx.getDeviant() > 0 && ctx.nonHumanPlan[ctx.getDeviant()] == 1)
        {
            ctx.zhans.add(ctx.getDeviant());
            ctx.setSkillTarget(ctx.getDeviant(), 1, ctx.zw[ctx.getDeviant()]);
        }
        DebugLogger.log("非人的初始工作：真占");
        if(ctx.isAlive(ctx.getActualRoleIndex(1)))
        {
            ctx.zhans.add(ctx.getActualRoleIndex(1));
        }
    }

    private void buildMediumCOList()
    {
        DebugLogger.log("非人的初始工作：狼灵");
        for(int i=1;i<=ctx.initialWolfCount;i++)
        {
            if(ctx.isDead(ctx.rlindex[i])) continue;
            if(ctx.nonHumanPlan[ctx.rlindex[i]] == 2)
            {
                ctx.lings.add(ctx.rlindex[i]);
                break;
            }
        }
        DebugLogger.log("非人的初始工作：狂灵信灵");
        if(ctx.getNonHumanLeader() > 0 && ctx.isAlive(ctx.getNonHumanLeader()) && ctx.nonHumanPlan[ctx.getNonHumanLeader()] == 2)
        {
            ctx.lings.add(ctx.getNonHumanLeader());
        }
        DebugLogger.log("非人的初始工作：狐灵");
        if(ctx.getFox() > 0 && ctx.nonHumanPlan[ctx.getFox()] == 2)
        {
            ctx.lings.add(ctx.getFox());
        }
        DebugLogger.log("非人的初始工作：背灵");
        if(ctx.getDeviant() > 0 && ctx.nonHumanPlan[ctx.getDeviant()] == 2)
        {
            ctx.lings.add(ctx.getDeviant());
        }
        DebugLogger.log("非人的初始工作：真灵");
        if(ctx.isAlive(ctx.getActualRoleIndex(2)))
        {
            ctx.lings.add(ctx.getActualRoleIndex(2));
        }
    }

    private void handleBlackResponses(int n, ArrayList<IntPair> response)
    {
        boolean morelzll = false,morelmll = false;
        ArrayList<Integer> bplayers = new ArrayList<>();
        for(int i=0;i<ctx.zhans.size();i++)
        {
            int zhan = ctx.zhans.get(i);
            if(ctx.getSkillTarget(zhan,1) <= n) continue;
            int bplayer = ctx.getSkillTarget(zhan,1) - n;
            if(!bplayers.contains(bplayer))
                bplayers.add(bplayer);
        }
        for(int j=0;j<bplayers.size();j++)
        {
            int bplayer = bplayers.get(j);
            if(ctx.isDead(bplayer)) continue;
            if(ctx.getActualRole(bplayer) < 7)
            {
                response.add(new IntPair(bplayer,ctx.getActualRole(bplayer)));
            }
            else if(ctx.getActualRole(bplayer) == 7)
            {
                if(ctx.nonHumanPlan[bplayer] != 0 && ctx.nonHumanPlan[bplayer] != 4)
                {
                    response.add(new IntPair(bplayer,ctx.nonHumanPlan[bplayer]));
                }
                else
                {
                    int lz = 0,ll = 0;
                    int lzweight = 10,llweight = 10,lmlweight = 10,lwcoweight = 10;
                    for(int i=1;i<=ctx.initialWolfCount;i++)
                        if(ctx.getClaimedRole(ctx.rlindex[i]) == 1)
                            lz++;
                        else if(ctx.getClaimedRole(ctx.rlindex[i]) == 2)
                            ll++;
                    if(lz > 1)  lzweight = 0;
                    if(ll > 0)  llweight = 0;
                    if(morelzll)
                    {
                        lzweight = 0;
                        llweight = 0;
                    }
                    if(morelmll)
                    {
                        lmlweight = 0;
                    }
                    if(ctx.getPeiyi() == peiyi.jianyi && (ctx.zhans.contains(ctx.rlindex[1]) || ctx.zhans.contains(ctx.rlindex[2])))
                    {
                        lzweight = 0;
                        llweight = 0;
                        lmlweight = 0;
                    }
                    int option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(lzweight,llweight,lmlweight,lwcoweight)));
                    switch(option)
                    {
                        case 0:
                            ctx.setSkillTarget(bplayer, 1, ctx.getYBZW(bplayer));
                            response.add(new IntPair(bplayer,1));
                            morelzll = true;
                            break;
                        case 1:
                            response.add(new IntPair(bplayer,2));
                            morelzll = true;
                            for(int k=1;k<=ctx.initialWolfCount;k++)
                            {
                                if(ctx.nonHumanPlan[ctx.rlindex[k]] == 2 && !bplayers.contains(ctx.nonHumanPlan[ctx.rlindex[k]]))
                                    ctx.nonHumanPlan[ctx.rlindex[k]] = 4;
                            }
                            break;
                        case 2:
                            ctx.nonHumanPlan[bplayer] = 5 - GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(50 + 50 * ctx.lies.size(),50+50 * ctx.maos.size()))) * 2;
                        if(ctx.getCat() == 0 || ctx.isCatDefinitivelyOut())
                            ctx.nonHumanPlan[bplayer] = 3;
                        if(ctx.nonHumanPlan[bplayer] == 5)
                            response.add(new IntPair(bplayer,5));
                        morelmll = true;
                        break;
                        case 3:
                            ctx.nonHumanPlan[bplayer] = 0;
                            break;
                    }
                }
            }
            else
            {
                DebugLogger.log("白非接黒并且没有之前的计划->狂狐背");
                int option = 0;
                switch(bplayer)
                {
                    case 8: case 9:
                    option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(95,5,0,0)));break;
                    case 10:option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(80,5,10,5)));break;
                    case 11:option = GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(45+ctx.kz,30,20,5-ctx.kz)));break;
                }
                switch(option)
                {
                    case 0:
                        ctx.setSkillTarget(bplayer, 1, ctx.getYBZW(bplayer));
                        response.add(new IntPair(bplayer,1));
                        break;
                    case 1:
                        response.add(new IntPair(bplayer,2));
                        break;
                    case 2:
                        ctx.nonHumanPlan[bplayer] = 5 - GameLogicUtils.getEventIndexByProbability(new ArrayList<>(List.of(50 + 50 * ctx.lies.size(),50+50 * ctx.maos.size()))) * 2;
                        if(ctx.getCat() == 0 || ctx.isCatDefinitivelyOut())
                            ctx.nonHumanPlan[bplayer] = 3;
                        if(ctx.nonHumanPlan[bplayer] == 5)
                            response.add(new IntPair(bplayer,5));
                        break;
                    case 3:
                        ctx.nonHumanPlan[bplayer] = 0;
                        break;
                }
            }
        }
    }
}