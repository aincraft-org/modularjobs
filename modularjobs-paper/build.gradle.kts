plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.guava)
    implementation(libs.hikaricp)
    implementation(project(":modularjobs-api"))
    implementation(project(":modularjobs-common"))
    implementation(project(":databag-api"))
    implementation(project(":databag-common"))
    implementation(project(":databag-paper"))
    implementation(libs.exp4j)
    implementation(libs.caffeine)
    implementation(libs.gson)
    implementation(libs.configurate.core)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.paper.api)
    testImplementation(libs.vault.api)

    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }
    compileOnly(libs.lwc)
    compileOnly(libs.bolt)

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // MockBukkit for Paper 26.2 — mock server for OfflinePlayer / Material runtime
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api)
    // PlaceholderAPI for expansion unit tests (compileOnly in main)
    testImplementation(libs.placeholderapi)
    // MySQL driver ships in the plugin artifact; Paper provides SQLite at runtime.
    implementation(libs.mysql)
    testImplementation(libs.mysql)
    testRuntimeOnly(libs.sqlite)
}

tasks.processResources {
    // Compute the version inside the task block so the configuration cache does not
    // capture a reference to the build script object.
    val descriptorVersion = project.version.toString()
    filesMatching("plugin.yml") {
        expand("version" to descriptorVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    shadowJar {
        mergeServiceFiles()
        // Paper provides Adventure and SLF4J; bundled copies (pulled in by the api module's
        // Adventure dependency) shadow the server's newer ones on any shared classpath.
        exclude("net/kyori/**", "org/slf4j/**")
    }

    build {
        dependsOn(shadowJar)
    }

    named<xyz.jpenilla.runpaper.task.RunServer>("runServer") {
        val toolchains = project.extensions.getByType<JavaToolchainService>()
        javaLauncher.set(
            toolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        )
        // Paper version line is 26.2 (post-1.21.11 renumbering); requires Java 25
        minecraftVersion("26.2")
        downloadPlugins {
            // Bolt 1.2.x lists Paper 26.2 compatibility on Hangar
            hangar("Bolt", "1.2.22")
            // 2.12.3+ required for Paper 26.2 (2.11.x crashes parsing version "26.2")
            hangar("PlaceholderAPI", "2.12.3")
        }
    }
}
