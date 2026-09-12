package com.vocr.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
