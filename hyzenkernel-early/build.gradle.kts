plugins {
    java
}

group = "com.hyzenkernel"
version = findProperty("version")?.toString()?.trimStart('v') ?: "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
    flatDir {
        dirs("../libs")
    }
}

dependencies {
    // Hytale Server API (for ClassTransformer interface)
    // Use parent directory's libs folder (shared with runtime plugin)
    compileOnly(files("../libs/HytaleServer.jar"))

    // ASM for bytecode manipulation (9.8+ required for Java 25 support)
    implementation("org.ow2.asm:asm:9.8")
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("org.ow2.asm:asm-util:9.8")
}

// Task to update manifest.json with current version
tasks.register("updateManifestVersion") {
    doLast {
        val manifestFile = file("src/main/resources/manifest.json")
        if (manifestFile.exists()) {
            val content = manifestFile.readText()
            val updated = content.replace(
                Regex(""""Version":\s*"[^"]*""""),
                """"Version": "${project.version}""""
            )
            manifestFile.writeText(updated)
            println("Updated early plugin manifest.json to version ${project.version}")
        }
    }
}

tasks.jar {
    dependsOn("updateManifestVersion")
    manifest {
        attributes(
            "Implementation-Title" to "HyzenKernel Early Plugin",
            "Implementation-Version" to project.version
        )
    }

    // Include ASM dependencies in the JAR (fat jar)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveBaseName.set("hyzenkernel-early")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}
