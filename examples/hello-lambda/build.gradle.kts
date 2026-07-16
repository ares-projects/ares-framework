plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly(project(":ares-annotations"))
    annotationProcessor(project(":ares-processor"))
    implementation(project(":ares-runtime"))
}
