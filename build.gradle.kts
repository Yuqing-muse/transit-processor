plugins {
    java
    application
}

group = "com.littlepay"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("com.littlepay.Main")
}

repositories {
    mavenCentral()
}

configurations.all {
    resolutionStrategy.force(
        // CVE-2025-48924: transitive via opencsv
        "org.apache.commons:commons-lang3:3.20.0",
        // CVE-2025-48734: transitive via opencsv
        "commons-beanutils:commons-beanutils:1.11.0"
    )
}

dependencies {
    implementation("com.opencsv:opencsv:5.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    // CVE-2026-24400 affects isXmlEqualTo() only — not used in this project
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
