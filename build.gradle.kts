plugins {
    java
    `maven-publish`
}

group = "com.astralab"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("net.dmulloy2:ProtocolLib:5.3.0")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    processResources {
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand("version" to project.version) }
    }
    test { useJUnitPlatform() }
    jar {
        archiveBaseName.set("AstraEmojis")
        manifest.attributes["paperweight-mappings-namespace"] = "mojang"
    }
}
