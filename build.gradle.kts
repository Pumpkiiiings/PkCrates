plugins {
    java
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.pumpkings.pkcrates"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:5.2.0")
    compileOnly("net.kyori:adventure-text-minimessage:5.2.0")
    compileOnly("com.google.code.gson:gson:2.10.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    javadoc {
        options.encoding = "UTF-8"
    }
    processResources {
        filteringCharset = "UTF-8"
        val props = mapOf(
            "version" to project.version
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
    
    shadowJar {
        archiveClassifier.set("")
    }
    
    build {
        dependsOn(shadowJar)
    }
}
