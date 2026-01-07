plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
}

group = "org.burufi.pdp"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xjvm-default=all-compatibility")
    }
}

dependencies {
    testImplementation(platform(libs.bom.junit5))

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.assertj)

}

tasks.test {
    useJUnitPlatform()
}