import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    kotlin("plugin.spring") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.springframework.boot") version "2.6.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "com.wib.ti.cookiecutter"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    maven {
        url = uri("https://wib.jfrog.io/artifactory/default-maven-virtual/")
        credentials {
            username = findProperty("artifactoryUser").toString()
            password = findProperty("artifactoryPassword").toString()
        }
    }
    mavenCentral()
}
val wibProtobufVersion = "0.2023157.2"

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // WIB
    implementation("com.wib.infrastructure:wib_infrastructure:2023.222.2")
    implementation("com.wib.traffic_inspection:traffic-inspection-pre-processor-messaging:$wibProtobufVersion")
    implementation("com.wib.traffic_inspection:traffic-inspection-usage-discovery-messaging:$wibProtobufVersion")
    implementation("com.wib.platform:messaging:2023.115.1")

    // Spring boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Jackson
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.kafka:kafka-clients")
    implementation("org.apache.kafka:kafka-streams")
    implementation("commons-codec:commons-codec:1.15")
    implementation("io.confluent:kafka-protobuf-serializer:5.5.1")

    // Kafka missing M1 packages
    implementation("org.xerial.snappy:snappy-java:1.1.8.4")
    implementation("org.rocksdb:rocksdbjni:7.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")

    // protobuf
    runtimeOnly("com.google.protobuf:protobuf-java-util:3.21.9")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")


    // Tests
    testImplementation("org.assertj:assertj-core:3.23.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    testImplementation("org.apache.kafka:kafka-streams-test-utils")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.bootJar {
    archiveFileName.set("${project.name}.jar")
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
    exclude("application.yml")
}
