plugins {
    java
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

    // Runtime libraries are NOT shaded. PkCratesLoader resolves them through Paper's
    // MavenLibraryResolver at load time, which reuses the server's shared libraries/
    // cache and downloads only what is missing.
    //
    // Keep these coordinates identical to RUNTIME_LIBRARIES in PkCratesLoader.java —
    // a mismatch compiles fine and fails at runtime with NoClassDefFoundError.
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.46.1.3")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
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
}

