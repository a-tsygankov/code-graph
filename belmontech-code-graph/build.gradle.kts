// External plugins only:
plugins {
    id("org.springframework.boot") version "3.4.1" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

allprojects {

    group = "com.belmonttech"
    version = "1.0.0"

    repositories { mavenCentral() }

    // Align ONLY Jackson 3 modules (tools.jackson.core)
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "tools.jackson.core") {
                useVersion("3.0.2")
            }
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }
}
