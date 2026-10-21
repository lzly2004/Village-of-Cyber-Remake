package com.vocr.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.vocr.application.EventPresentationStage;
import com.vocr.history.GameRecordManager;
import com.vocr.core.MainLogicInterface;
import com.vocr.domain.GameContextView;
import com.vocr.domain.Role;
import com.vocr.domain.peiyi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureBoundaryTest {
    private static final DescribedPredicate<JavaAccess<?>> DIRECT_PROCESS_OUTPUT =
            DescribedPredicate.describe(
                    "call Throwable.printStackTrace(...) or access System.out/System.err",
                    access -> (access.getName().equals("printStackTrace")
                            && access.getTargetOwner().isAssignableTo(Throwable.class))
                            || (access.getTargetOwner().isEquivalentTo(System.class)
                            && (access.getName().equals("out") || access.getName().equals("err"))));

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importProductionClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.vocr");
    }

    @Test
    void domainShouldRemainIndependentFromOtherProjectPackages() {
        classes()
                .that().resideInAPackage("com.vocr.domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..",
                        "com.vocr.domain..")
                .because("领域类型应当能够独立于应用编排、基础设施和界面演进")
                .check(productionClasses);
    }

    @Test
    void coreShouldNotDependOnPresentationOrReplayImplementations() {
        noClasses()
                .that().resideInAPackage("com.vocr.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.vocr.app..",
                        "com.vocr.replay..",
                        "com.vocr.resources..",
                        "com.vocr.ui..")
                .because("核心对局逻辑不应反向依赖启动层、界面或录像与资源实现")
                .check(productionClasses);
    }

    @Test
    void applicationShouldRemainIndependentFromPresentationAndInfrastructureImplementations() {
        noClasses()
                .that().resideInAPackage("com.vocr.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.vocr.app..",
                        "com.vocr.history..",
                        "com.vocr.replay..",
                        "com.vocr.resources..",
                        "com.vocr.ui..")
                .because("应用用例应能被桌面端、Web端和测试共同调用")
                .check(productionClasses);
    }

    @Test
    void coreShouldNotPersistGameResultsDirectly() {
        noClasses()
                .that().resideInAPackage("com.vocr.core..")
                .should().callMethod(
                        GameRecordManager.class, "updateRecord", int.class, int.class)
                .because("对局结果只能由应用会话通过持久化端口提交")
                .check(productionClasses);
    }

    @Test
    void uiShouldStartGamesThroughApplicationLayer() {
        noClasses()
                .that().resideInAPackage("com.vocr.ui..")
                .should().callMethod(MainLogicInterface.class, "start", peiyi.class)
                .because("开局用例已经迁移到平台无关应用层，UI不应再次绕过该入口")
                .check(productionClasses);
    }

    @Test
    void uiShouldRequestClaimsThroughApplicationLayer() {
        noClasses()
                .that().resideInAPackage("com.vocr.ui..")
                .should().callMethod(MainLogicInterface.class, "askCo", Role.class)
                .because("CO 询问用例已经迁移到平台无关应用层")
                .check(productionClasses);
        noClasses()
                .that().resideInAPackage("com.vocr.ui..")
                .should().callMethod(MainLogicInterface.class, "askCo", List.class)
                .because("按玩家询问 CO 也必须复用同一应用入口")
                .check(productionClasses);
    }

    @Test
    void uiShouldExecuteVotesThroughApplicationLayer() {
        noClasses()
                .that().resideInAPackage("com.vocr.ui..")
                .should().callMethod(
                        MainLogicInterface.class, "shokei", int.class, List.class, boolean.class)
                .because("投票与处刑用例已经迁移到平台无关应用层")
                .check(productionClasses);
    }

    @Test
    void uiShouldRenderAuthoritativeVoteResultsWithoutRecountingCoreState() {
        noClasses()
                .that().haveSimpleName("VoteResultRenderer")
                .or().haveSimpleName("VoteHistoryBrowser")
                .should().callMethod(
                        GameContextView.class, "getVoteTarget",
                        int.class, int.class, int.class)
                .orShould().callMethod(
                        GameContextView.class, "getVoteRoundCount", int.class)
                .orShould().callMethod(
                        GameContextView.class, "getVotingRule", int.class)
                .orShould().callMethod(
                        GameContextView.class, "getVotingCandidates", int.class)
                .because("Swing 投票结果界面只能渲染应用层已经计票完成的权威快照")
                .check(productionClasses);
    }

    @Test
    void dialogueHandlersShouldFollowApplicationEventPresentationStages() {
        Set<String> stageDrivenHandlers = Set.of(
                "DialogueAfternoonHandler",
                "DialogueChuxingHandler",
                "DialogueDayDeathHandler",
                "DialogueDayHandler",
                "EndGameHandler");

        for (String simpleName : stageDrivenHandlers) {
            JavaClass handler = productionClasses.get("com.vocr.ui." + simpleName);
            assertTrue(handler.getDirectDependenciesFromSelf().stream()
                            .anyMatch(dependency -> dependency.getTargetClass()
                                    .isEquivalentTo(EventPresentationStage.class)),
                    simpleName + " 必须使用应用层展示阶段推进事件流");
        }
    }

    @Test
    void uiShouldChangeTargetSelectionsThroughApplicationLayer() {
        noClasses()
                .that().resideInAPackage("com.vocr.ui..")
                .should().callMethod(
                        GameContextView.class, "setSelectedVoteTarget",
                        int.class, int.class, boolean.class)
                .because("指定投票目标必须由应用层校验并写入")
                .check(productionClasses);
        noClasses()
                .that().resideInAPackage("com.vocr.ui..")
                .should().callMethod(
                        GameContextView.class, "setClaimedRoleScheduled",
                        int.class, int.class, int.class, boolean.class)
                .because("已 CO 角色的技能预约必须由应用层统一展开")
                .check(productionClasses);
        noClasses()
                .that().resideInAPackage("com.vocr.ui..")
                .should().callMethod(
                        GameContextView.class, "setHiddenSeerScheduled",
                        int.class, int.class, boolean.class)
                .because("潜伏占卜预约必须由应用层统一展开")
                .check(productionClasses);
        noClasses()
                .that().resideInAPackage("com.vocr.ui..")
                .should().callMethod(
                        GameContextView.class, "setHiddenHunterScheduled",
                        int.class, int.class, boolean.class)
                .because("潜伏猎人预约必须由应用层统一展开")
                .check(productionClasses);
    }

    @Test
    void coreShouldExposeOnlyItsApplicationFacingApi() {
        Set<String> publicCoreTypes = productionClasses.stream()
                .filter(type -> type.getPackageName().equals("com.vocr.core"))
                .filter(type -> !type.getName().contains("$"))
                .filter(type -> type.getModifiers().contains(JavaModifier.PUBLIC))
                .map(JavaClass::getSimpleName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("MainLogic", "MainLogicInterface", "GameRecorder"), publicCoreTypes,
                "核心包只应公开应用启动、界面调用和 Replay 保存所需的三个入口类型");
    }

    @Test
    void rulesShouldRemainGeneric() {
        classes()
                .that().resideInAPackage("com.vocr.rules..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..",
                        "com.vocr.config..",
                        "com.vocr.rules..")
                .because("通用权重规则组件只应依赖自身和底层配置常量")
                .check(productionClasses);
    }

    @Test
    void loggingShouldRemainIndependentFromGamePackages() {
        classes()
                .that().resideInAPackage("com.vocr.logging..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "java..",
                        "com.vocr.logging..")
                .because("普通诊断日志不应反向读取任何游戏状态")
                .check(productionClasses);
    }

    @Test
    void directProcessOutputShouldRemainInsideLogging() {
        noClasses()
                .that().resideOutsideOfPackage("com.vocr.logging..")
                .should().accessTargetWhere(DIRECT_PROCESS_OUTPUT)
                .because("生产代码的异常和控制台输出应统一经过日志边界")
                .check(productionClasses);
    }

    @Test
    void configurationAndHistoryShouldNotDependOnCore() {
        noClasses()
                .that().resideInAnyPackage("com.vocr.config..", "com.vocr.history..")
                .should().dependOnClassesThat().resideInAPackage("com.vocr.core..")
                .because("配置与战绩组件不应仅为了日志而反向依赖核心对局实现")
                .check(productionClasses);
    }

    @Test
    void resourcesAndReplayShouldNotDependOnLiveGameOrHistoryImplementations() {
        noClasses()
                .that().resideInAnyPackage(
                        "com.vocr.resources..",
                        "com.vocr.replay..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.vocr.core..",
                        "com.vocr.history..")
                .because("资源加载与录像持久化应消费稳定数据，不应反向持有实时对局或历史战绩实现")
                .check(productionClasses);
    }

    @Test
    void nonPresentationPackagesShouldNotDependOnUi() {
        noClasses()
                .that().resideOutsideOfPackages("com.vocr.app..", "com.vocr.ui..")
                .should().dependOnClassesThat().resideInAPackage("com.vocr.ui..")
                .because("只有启动层和界面层可以直接组装具体界面")
                .check(productionClasses);
    }

    @Test
    void platformIndependentPackagesShouldNotDependOnDesktopPresentationApis() {
        noClasses()
                .that().resideInAnyPackage(
                        "com.vocr.config..",
                        "com.vocr.application..",
                        "com.vocr.core..",
                        "com.vocr.domain..",
                        "com.vocr.history..",
                        "com.vocr.logging..",
                        "com.vocr.replay..",
                        "com.vocr.rules..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "java.awt..",
                        "javax.sound..",
                        "javax.swing..")
                .because("对局、领域和基础设施代码不应通过JDK桌面类型绕过展示层边界")
                .check(productionClasses);
    }
}
