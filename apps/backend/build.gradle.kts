import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("org.springframework.boot")           version "3.3.1"
    id("io.spring.dependency-management")    version "1.1.5"
    id("com.github.johnrengelman.shadow")    version "8.1.1"   // fat-jar for Lambda
}

group   = "app.rallyhub"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:2.26.12")
    }
}

dependencies {
    // Spring
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.cloud:spring-cloud-function-adapter-aws:4.1.2")
    implementation("org.springframework.cloud:spring-cloud-function-context:4.1.2")

    // AWS SDK v2
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:dynamodb-enhanced")
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:ses")
    implementation("software.amazon.awssdk:cognitoidentityprovider")

    // AWS Lambda
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.14.0")

    // JWT verification
    implementation("com.nimbusds:nimbus-jose-jwt:9.40")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.assertj:assertj-core")
}

// Spring Cloud Function version alignment
extra["spring-cloud.version"] = "2023.0.2"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.2")
        mavenBom("software.amazon.awssdk:bom:2.26.12")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Shadow jar — Lambda deployment artifact
tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes["Start-Class"] = "app.rallyhub.RallyhubApplication"
    }
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
