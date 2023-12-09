import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "1.9.20"
}

group = "com.rocketraman"
version = "1.0-SNAPSHOT"

val kotlinVersion = "1.9.20"
val ktorVersion = "2.3.7"

repositories {
  mavenCentral()
}

configurations.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-")) {
      useVersion(kotlinVersion)
      because("Use consistent Kotlin stdlib and reflect artifacts")
    }
  }
}

dependencies {
  implementation("io.ktor:ktor-network:$ktorVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug")
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "17"
  }
}
