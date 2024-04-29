/**
 * setting.gradle 里面设置了repositories所以注释这里
 */
repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}
gradlePlugin {
    plugins {
        create("ClickPlugin") {
            id = "com.nofish.plugins.click"
            implementationClass = "com.nofish.plugins.click.ClickPlugin"
        }
    }
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    val androidToolsBuild = "8.0.2"
    val kotlinVersion = "1.8.10"
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("com.android.tools.build:gradle:${androidToolsBuild}")
    implementation("com.android.tools.build:gradle-api:${androidToolsBuild}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:${kotlinVersion}")
    implementation("org.ow2.asm:asm-commons:9.5")
    implementation("commons-io:commons-io:2.13.0")
}
