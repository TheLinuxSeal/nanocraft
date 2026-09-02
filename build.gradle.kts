plugins {
    java
    // Or kotlin("jvm") if you are using Kotlin
}

repositories {
    mavenCentral()
}

// 1. Defin


group = "org.sutormin.nanocraft"
version = "snap"

val lwjglVersion = "3.4.0"
val lwjglNatives = "natives-linux" // Change to "natives-linux" or "natives-macos" depending on your OS



dependencies {
    // 2. Core LWJGL
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")

    // 3. GLFW Bindings
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")

    // 4. OpenGL Bindings
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")

    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")

    // STB Native Library
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")

    // 5. JOML (Java OpenGL Math Library - recommended for 3D matrices/vectors)
    implementation("org.joml:joml:1.10.8")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.sutormin.nanocraft.Main" // Change to your actual main class path
    }

    // Include dependencies inside the JAR so it runs standalone
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}