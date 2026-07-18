plugins { id("ares.java-library-conventions") }

dependencies {
    implementation(project(":ares-annotations"))
    testImplementation(project(":ares-runtime"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}
