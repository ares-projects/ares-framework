plugins { id("ares.java-library-conventions") }

dependencies {
    api("com.amazonaws:aws-lambda-java-core:1.2.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
}
