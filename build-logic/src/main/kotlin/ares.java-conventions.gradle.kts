plugins {
    java
    checkstyle
    id("com.diffplug.spotless")
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

checkstyle { configFile = rootProject.file("config/checkstyle/checkstyle.xml") }

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
