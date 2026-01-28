plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "com.hyzenkernel"
// Version is set in manifest.json - don't let gradle override it
val projectVersion = "1.0.9"
version = projectVersion

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

repositories {
    mavenCentral()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    // Hytale Server API - place HytaleServer.jar in libs/ folder
    compileOnly(files("libs/HytaleServer.jar"))

    // Annotations
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // (no runtime deps currently)
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
            println("Updated manifest.json to version ${project.version}")
        }
    }
}

tasks.jar {
    dependsOn("updateManifestVersion")
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Plugin-Class" to "com.hyzenkernel.HyzenKernel"
        )
    }
}

// Shadow JAR configuration
tasks.shadowJar {
    dependsOn("updateManifestVersion")
    archiveClassifier.set("")  // No classifier, replaces main jar
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Plugin-Class" to "com.hyzenkernel.HyzenKernel"
        )
    }

    minimize()
}

// Make build use shadowJar
tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}
