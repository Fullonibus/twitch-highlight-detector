plugins {
    `java-library`
}

dependencies {
    implementation(project(":highlight-service"))
    implementation("org.springframework.boot:spring-boot-starter")
}
