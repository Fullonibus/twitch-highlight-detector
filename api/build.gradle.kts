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

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
