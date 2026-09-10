package com.igarciamen.notifications;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.igarciamen.notifications");

    @Test
    void controllers_end_in_Controller() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().haveSimpleNameEndingWith("Controller");
        rule.check(CLASSES);

        System.out.println("[Architecture OK] Every @RestController ends with 'Controller'.");
    }

    @Test
    void controllers_are_in_package_controller() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(RestController.class)
                .should().resideInAPackage("..controller..");
        rule.check(CLASSES);

        System.out.println("[Architecture OK] Every @RestController lives in the ..controller.. package.");
    }

    // No "controllers do not depend on repositories" rule here: this
    // microservice has no repository package at all (no database).
}
