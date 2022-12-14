
plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

tasks.compileJava {
    options.release.set(19)
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
    implementation(platform("software.amazon.awssdk:bom:2.19.4"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:ec2")
    implementation("software.amazon.awssdk:efs")
    implementation("software.amazon.awssdk:lambda")
    implementation("software.amazon.awssdk:pricing")
    implementation("software.amazon.awssdk:rds")
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:sts")
    runtimeOnly("software.amazon.awssdk:sso")           // Needed for SSO-based profiles

    implementation("com.sun.mail:jakarta.mail:2.0.1")

    implementation("info.picocli:picocli:4.7.0")        // CLI option parsing
    implementation("com.diogonunes:JColor:5.5.1")       // Terminal colors
    implementation("com.squareup.moshi:moshi:1.14.0")   // Json parsing

    implementation("org.jgrapht:jgrapht-core:1.5.1")    // Graph
    implementation("org.jgrapht:jgrapht-io:1.5.1")      // Graph
    constraints {
        implementation("org.apache.commons:commons-text:1.10") {
            because("version 1.8 pulled from jgraph-io has CVE-2022-42889")
        }
    }

    runtimeOnly("org.slf4j:slf4j-nop:1.7.30")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

application {
    mainClass.set("awscanner.Main")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
