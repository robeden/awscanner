
plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

tasks.compileJava {
    options.release.set(17)
}
//java {
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(17))
//    }
//}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("software.amazon.awssdk:bom:2.17.271"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:ec2")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:rds")
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:sts")

    implementation("info.picocli:picocli:4.6.3")        // CLI option parsing
    implementation("com.diogonunes:JColor:5.5.1")       // Terminal colors


    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
}

application {
    mainClass.set("awscanner.Main")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
