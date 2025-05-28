plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

dependencies {
    // ansi 日志颜色
    implementation("org.fusesource.jansi:jansi:2.4.0")

    // json
    implementation("com.google.code.gson:gson:2.10.1")

    // llm
    implementation("com.openai:openai-java:1.6.1")
//    implementation("com.google.genai:google-genai:0.8.0")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    // kotlin 依赖
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt" // 确保主类正确，基于 Main.kt
    }
    archiveBaseName.set("kg-kotlin") // 设置 JAR 文件名
    archiveClassifier.set("")
    archiveVersion.set("")
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // 添加此行，排除重复文件
}

kotlin {
    jvmToolchain(17)
}