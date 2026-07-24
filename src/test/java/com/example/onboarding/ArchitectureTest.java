package com.example.onboarding;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(
        packages = "com.example.onboarding",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    @ArchTest
    ArchRule use_cases_should_only_expose_execute =
            methods().that().areDeclaredInClassesThat()
                    .resideInAPackage("..application.usecase..")
                    .and().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
                    .and().arePublic()
                    .should().haveName("execute");

    @ArchTest
    ArchRule domain_should_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..");

    @ArchTest
    ArchRule application_should_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..");
}
