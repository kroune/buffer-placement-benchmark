plugins {
    java
    id("me.champeau.jmh") version "0.7.3"
}

group = "edu.cu"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

jmh {
    jmhVersion.set("1.37")
}
