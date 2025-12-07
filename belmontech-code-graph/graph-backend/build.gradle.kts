plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    implementation(project(":graph-model"))
    implementation("org.springframework.boot:spring-boot-starter-web")
}
