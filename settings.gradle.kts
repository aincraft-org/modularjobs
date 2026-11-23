
rootProject.name = "jobs2"

include("modularjobs-api", "modularjobs-common", "modularjobs-paper")

// Vendored libraries; source revisions in vendor/README.md.
for (name in listOf("databag-common", "databag-api", "databag-paper", "utilities-event")) {
    include(name)
    project(":$name").projectDir = file("vendor/$name")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // MockBukkit Paper 26.2 line (vendored until Central publishes mockbukkit-v26.2)
        maven {
            name = "mockbukkitLocal"
            url = uri(rootDir.resolve("libs/mockbukkit-maven"))
        }
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://nexus.neetgames.com/repository/maven-releases/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}