plugins {
    `java-library`
}

dependencies {
    implementation(project(":twitch-irc"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("org.mockito:mockito-core")
}
