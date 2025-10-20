plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.ktlint)
}

group = "es.unizar.webeng"
version = "2025-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.boot.starter.data.r2dbc)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")

    runtimeOnly(libs.h2database.h2)
    runtimeOnly(libs.r2dbc.h2)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.ninjasquad.springmocck)
    testImplementation(libs.spring.boot.starter.webflux)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
}
