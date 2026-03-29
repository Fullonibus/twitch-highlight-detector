plugins {
    java
    id("org.springframework.boot") version "3.4.4"
}

dependencies {
    implementation(project(":twitch-irc"))
    implementation(project(":chat-analyzer"))
    implementation(project(":emote-dictionary"))
    implementation(project(":highlight-service"))
    implementation(project(":notification-service"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
