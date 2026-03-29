plugins {
    `java-library`
}

dependencies {
    implementation(project(":twitch-irc"))
    implementation(project(":chat-analyzer"))
    implementation(project(":emote-dictionary"))
    testImplementation("org.mockito:mockito-core")
}
