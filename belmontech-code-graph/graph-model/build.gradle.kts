plugins {
    id("java")
    id("java-library")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    // Use Jackson 3 everywhere
    implementation("tools.jackson.core:jackson-core:3.0.2")
    implementation("tools.jackson.core:jackson-databind:3.0.2")

    // jtoon uses Jackson 3.x natively
    implementation("dev.toonformat:jtoon:1.0.6")
    implementation("commons-io:commons-io:2.15.1")
}
