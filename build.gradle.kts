plugins {
    id("java")
    id("idea")
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "net.andylizi.haproxydetector"
version = "4.0.0"

val bStatsVersion = "3.0.2"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.dmulloy2.net/repository/public")
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly("io.netty:netty-codec-haproxy:4.1.108.Final")
    implementation("commons-validator:commons-validator:1.8.0")
    implementation("org.bstats:bstats-bukkit:$bStatsVersion")
    implementation("org.bstats:bstats-bungeecord:$bStatsVersion")
    implementation("org.bstats:bstats-velocity:$bStatsVersion")
    implementation("org.bstats:bstats-velocity:$bStatsVersion")
    compileOnly("io.github.waterfallmc:waterfall-api:1.20-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.shadowJar {
    archiveClassifier = ""
    relocate("org.bstats", "net.andylizi.haproxydetector.bstats")
}
