plugins {
    id("ares.java-conventions") apply false
    id("ares.java-library-conventions") apply false
}

group = "io.github.ares-projects"
version = "0.1.0-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version
}
