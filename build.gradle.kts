plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.5" apply false
}

allprojects {
    group = findProperty("group")?.toString() ?: "dev.nixoly.nixLib"
    version = findProperty("version")?.toString() ?: "1.0.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "java")
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    dependencies {
        val testImpl = configurations.getByName("testImplementation")
        val testRuntime = configurations.getByName("testRuntimeOnly")

        testImpl(platform("org.junit:junit-bom:5.10.2"))
        testImpl("org.junit.jupiter:junit-jupiter")
        testImpl("org.assertj:assertj-core:3.25.3")
        testRuntime("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        testLogging {
            events("passed", "failed", "skipped")
            showStandardStreams = false
        }
    }

    publishing {
        repositories {
            mavenLocal()
        }

        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }
    }
}

tasks.register("publishAllToMavenLocal") {
    group = "nixlib"
    description = "Builds and publishes api, core, and folia to ~/.m2/repository."
    dependsOn(
        ":api:publishMavenPublicationToMavenLocal",
        ":core:publishMavenPublicationToMavenLocal",
        ":folia:publishMavenPublicationToMavenLocal",
    )
}

tasks.register("installLocal") {
    group = "nixlib"
    description = "Alias for publishAllToMavenLocal."
    dependsOn(tasks.named("publishAllToMavenLocal"))
}

tasks.register("printMavenCoordinates") {
    group = "nixlib"
    description = "Prints Maven coordinates for use in consumer build.gradle.kts files."
    doLast {
        val g = project.group
        val v = project.version
        listOf("api", "core", "folia").forEach { module ->
            println("$g:$module:$v")
        }
        println("Publish: ./gradlew publishAllToMavenLocal")
        println("Consumer repositories { mavenLocal() }")
        println("Consumer dependencies { implementation(\"$g:api:$v\") ... }")
    }
}
