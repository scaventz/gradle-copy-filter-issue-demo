import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.apache.tools.ant.filters.*;

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val nonSystemLineSeparator = when(System.lineSeparator()) {
    "\n" -> "\r\n"
    else -> "\n"
}

val createFile by tasks.registering() {
    val outputFile = layout.buildDirectory.file("foo")
    outputs.file(outputFile)
    doLast {
        outputFile.get().asFile.writeText("a${nonSystemLineSeparator}b${nonSystemLineSeparator}")
    }
}

val filterCopy by tasks.registering(Copy::class) {
    from(createFile)
    into(layout.buildDirectory.dir("bar"))
    filter {
        when (it) {
            "a" -> "a2"
            "b" -> "b2"
            else -> "x"
        }
    }
    // uncomment this line would address the issue
    filter<FixCrLfFilter>("eol" to FixCrLfFilter.CrLf.newInstance("lf"))
}

tasks.register("verifyCopy") {
    dependsOn(tasks.getByName("clean"))
    inputs.files(filterCopy)
    doLast {
        val content = files(filterCopy).asFileTree.singleFile.readText()
        when (content) {
            "a2${nonSystemLineSeparator}b2${nonSystemLineSeparator}" -> logger.quiet("filtering preserved line separator")
            "a2${System.lineSeparator()}b2${System.lineSeparator()}" -> error("filtering changed line separator to system separator")
            else -> error("filtering changed line separator to something else: ${content.toByteArray().contentToString()}")
        }
    }
}

defaultTasks("verifyCopy")