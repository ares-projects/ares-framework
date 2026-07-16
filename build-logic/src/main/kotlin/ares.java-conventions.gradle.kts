import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification

plugins {
    java
    checkstyle
    jacoco
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

checkstyle {
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
}

spotbugs {
    ignoreFailures = false
}

jacoco { toolVersion = "0.8.14" }

spotless {
    java {
        palantirJavaFormat("2.39.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle { ktlint() }
    format("misc") {
        target("*.md", "*.yml", "*.yaml", "*.json", ".gitignore", ".gitattributes")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.named("check") { dependsOn("spotlessCheck") }
tasks.register("formatCheck") { dependsOn("spotlessCheck") }

tasks.named("jacocoTestReport") {
    dependsOn("test")
}

tasks.withType<JacocoCoverageVerification>().configureEach {
    dependsOn("test")
    violationRules {
        rule {
            limit {
                minimum = BigDecimal("0.95")
            }
        }
    }
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
}
