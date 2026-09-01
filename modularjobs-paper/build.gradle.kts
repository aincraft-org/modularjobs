plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.guava)
    implementation("org.aincraft:utilities-common:2026.08.27") {
        isTransitive = false
    }
    // Utilities owns the SQL/Jdbi/Hikari lifecycle; ModularJobs remains JDBC-facing at its
    // ConnectionSource boundary so existing repositories and transactions keep their contracts.
    implementation("org.aincraft:utilities-db-sql:2026.08.27")
    implementation(project(":modularjobs-api"))
    implementation(project(":modularjobs-common"))
    implementation(libs.databag.api)
    implementation(libs.databag.common)
    implementation(libs.databag.paper)
    implementation(libs.exp4j)
    implementation(libs.caffeine)
    implementation(libs.gson)
    implementation(libs.configurate.core)
    compileOnly("io.github.flog99:mapgui-api:1.0.0")
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.paper.api)
    // Mint2 ledger API (soft-depend; Mint2 ships it at runtime; included build
    // substitutes dev.mintychochip:mint-api / mint-common from ../mint2)
    compileOnly(libs.mint2.api)
    compileOnly(libs.mint2.common)
    // External Preferences API (soft-depend; Preferences plugin ships it at runtime)
    compileOnly(libs.preferences.api)
    testImplementation(libs.mint2.api)
    testImplementation(libs.mint2.common)
    testImplementation(libs.preferences.api)
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
    // Preferences API for payable preference wiring tests (compileOnly in main)
    testImplementation(libs.preferences.api)
    // MySQL only — driver ships in the plugin artifact
    implementation(libs.mysql)
    testImplementation(libs.mysql)
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
