dependencies {
    api(libs.adventure.api)
    api(libs.jetbrains.annotations)
    api(libs.databag.api)
    api("org.aincraft:utilities-api:2026.08.27")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("ci.workflow", rootProject.file(".github/workflows/ci.yml").absolutePath)
    systemProperty("project.root", rootProject.projectDir.absolutePath)
    systemProperty(
        "ci.pom",
        layout.buildDirectory.file("publications/maven/pom-default.xml").get().asFile.absolutePath,
    )
}
