plugins {
    java
    id("com.github.johnrengelman.shadow")  version "8.1.1"
    id("io.micronaut.application")         version "4.4.2"
    id("org.graalvm.buildtools.native")    version "0.10.2"
}

version = "1.0.0"
group   = "app.rallyhub"

repositories {
    mavenCentral()
}

// ── Dependency versions ───────────────────────────────────────────
val micronautVersion    = "4.5.0"
val awsSdkVersion       = "2.26.12"
val lombokVersion       = "1.18.34"

dependencies {
    // Lombok MUST be declared before Micronaut processors
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Micronaut annotation processors
    annotationProcessor("io.micronaut:micronaut-inject-java")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")
    annotationProcessor("io.micronaut:micronaut-graal")

    // Micronaut core
    implementation("io.micronaut:micronaut-inject")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.validation:micronaut-validation")

    // Micronaut AWS — Lambda API Gateway proxy adapter
    implementation("io.micronaut.aws:micronaut-function-aws-api-proxy")
    implementation("io.micronaut.aws:micronaut-function-aws")

    // AWS SDK v2 (BOM manages versions)
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    implementation("software.amazon.awssdk:dynamodb")
    implementation("software.amazon.awssdk:dynamodb-enhanced")
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:ses")
    implementation("software.amazon.awssdk:url-connection-client") // lightweight HTTP client for native

    // AWS Lambda Java SDK
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation("com.amazonaws:aws-lambda-java-events:3.14.0")

    // JWT — Cognito JWKS verification
    implementation("com.nimbusds:nimbus-jose-jwt:9.40")

    // Logging
    runtimeOnly("ch.qos.logback:logback-classic")

    // Testing — Lombok processors must come first here too
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("io.micronaut:micronaut-inject-java")
    testImplementation("io.micronaut.test:micronaut-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.yaml:snakeyaml")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

micronaut {
    version(micronautVersion)
    runtime("lambda_provided")   // Custom runtime — GraalVM native binary
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("app.rallyhub.*")
    }
}

application {
    mainClass.set("app.rallyhub.RallyhubApplication")
}

// ── GraalVM native image ──────────────────────────────────────────
graalvmNative {
    binaries {
        named("main") {
            imageName.set("bootstrap")   // Lambda custom runtime expects 'bootstrap'
            buildArgs.addAll(
                "--no-fallback",
                "--initialize-at-build-time=org.slf4j,ch.qos.logback",
                "-H:+ReportExceptionStackTraces",
                "--enable-url-protocols=https",
                "-march=compatibility"   // Broadest CPU compatibility for Lambda
            )
        }
    }
    toolchainDetection.set(false)   // Use GraalVM CE from PATH / GitHub Action
}

// ── Package native binary as Lambda deployment zip ───────────────
tasks.register<Zip>("packageNative") {
    group = "build"
    description = "Zips the GraalVM native binary for Lambda deployment"
    dependsOn("nativeCompile")
    archiveFileName.set("function.zip")
    from(layout.buildDirectory.file("native/nativeCompile/bootstrap"))
}

// ── Shadow jar (JVM fallback / local testing) ────────────────────
tasks.shadowJar {
    archiveClassifier.set("all")
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    manifest { attributes["Main-Class"] = "app.rallyhub.RallyhubApplication" }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
