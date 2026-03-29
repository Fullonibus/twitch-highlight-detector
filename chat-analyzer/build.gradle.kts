plugins {
    `java-library`
}

dependencies {
    implementation(project(":twitch-irc"))
    testImplementation("org.mockito:mockito-core")
}
