package pl.jakubmikolajczyk.monitoring;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/// The dependency graph between feature packages is a design decision (see
/// docs/implementation-plan.md): customer <- transaction <- {detection, alert},
/// detection -> alert, common depends on nothing. These rules turn that
/// convention into a build break instead of a code-review hope.
@AnalyzeClasses(
        packages = "pl.jakubmikolajczyk.monitoring",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule featurePackagesFormNoCycles =
            slices().matching("pl.jakubmikolajczyk.monitoring.(*)..")
                    .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule commonIsALeafAndKnowsNoFeatures =
            noClasses().that().resideInAPackage("..monitoring.common..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..monitoring.customer..", "..monitoring.transaction..",
                            "..monitoring.detection..", "..monitoring.alert..");

    @ArchTest
    static final ArchRule businessFeaturesNeverCallIntoDetection =
            noClasses().that().resideInAnyPackage(
                            "..monitoring.customer..", "..monitoring.transaction..", "..monitoring.alert..")
                    .should().dependOnClassesThat().resideInAPackage("..monitoring.detection..");

    @ArchTest
    static final ArchRule customerStaysFullyIndependent =
            noClasses().that().resideInAPackage("..monitoring.customer..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..monitoring.transaction..", "..monitoring.detection..", "..monitoring.alert..");
}
