plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("ru.homeless.MainKt")
}

val exposedVersion: String by project

dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.telegram:telegrambots:6.1.0")
    implementation("org.telegram:telegrambotsextensions:6.1.0")

    implementation("mysql:mysql-connector-java:8.0.30")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
    implementation("ch.qos.logback:logback-core:1.4.0")
    implementation("ch.qos.logback:logback-classic:1.4.0")

    implementation ("com.google.api-client:google-api-client:1.33.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20220620-1.32.1")


    implementation("com.google.apis:google-api-services-script:v1-rev20220323-1.32.1")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

sourceSets {
    main {
        resources {
            srcDirs("src/main/resources")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    withType<Copy> {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn.addAll(listOf("compileKotlin", "processResources"))
        archiveClassifier.set("fat") // Naming the jar
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }
}