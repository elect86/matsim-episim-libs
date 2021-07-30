import java.util.Scanner

plugins {
    java
    `maven-publish`
}

repositories {
    mavenCentral().content {
        excludeModule("javax.media", "jai_core")
    }
    maven("https://repo.matsim.org/repository/matsim")
    maven("https://jitpack.io")
    maven("https://repo.osgeo.org/repository/release/")
    maven("https://oss.jfrog.org/libs-snapshot")
    maven("https://mvn.topobyte.de")
    maven("https://mvn.slimjars.com")
    maven("https://mvnrepository.com/artifact/tech.tablesaw/tablesaw-core")
    maven("https://dl.bintray.com/matsim/matsim")
}

val matsimVersion = "13.0"

dependencies {

    implementation("org.matsim:matsim:$matsimVersion")
    implementation("com.typesafe:config:1.4.0")
    implementation("info.picocli:picocli:4.3.2")
    implementation("com.lmax:disruptor:3.4.2")
    implementation("com.github.ClickerMonkey:TrieHard:1.0")
    implementation("commons-io:commons-io:2.7")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("it.unimi.dsi:fastutil:8.3.1")
    implementation("org.jgrapht:jgrapht-core:1.5.0")
    implementation("org.jgrapht:jgrapht-io:1.5.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.10.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.10.2")
    implementation("org.matsim.contrib:vsp:$matsimVersion") {
        exclude("org.jogamp.gluegen", "gluegen-rt")
        exclude("org.jogamp.jogl", "jogl-all")
    }
    implementation("tech.tablesaw:tablesaw-core:0.38.1")
    implementation("tech.tablesaw:tablesaw-jsplot:0.38.1")
    implementation("org.apache.poi:poi:4.1.2")
    implementation("org.apache.poi:poi-ooxml:4.1.2")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("com.sun.xml.bind:jaxb-core:2.3.0.1")
    implementation("com.sun.xml.bind:jaxb-impl:2.3.0.1")

    implementation("com.opencsv:opencsv:4.1")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.matsim:matsim:$matsimVersion")
    testImplementation("org.assertj:assertj-core:3.15.0")
    testImplementation("org.mockito:mockito-core:3.3.3")
    testImplementation("org.openjdk.jmh:jmh-core:1.23")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.23")
}

group = "com.github.matsim-org"
version = "21.5"
description = "MATSim Episim"
//java.sourceCompatibility = JavaVersion.VERSION_11

java {
    withSourcesJar()
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks {
    create("berlin").doFirst {
        val typed = Scanner(System.`in`).nextLine()
        val parsed = typed.split(' ')
        println ("Arguments received: " + parsed.joinToString())
    }
    //    register<JavaExec>("berlin") {
    //        classpath = sourceSets.main.get().runtimeClasspath
    //        mainClass.set("org.matsim.run.RunEpisim")
    //        args = listOf("--modules OpenBerlinScenario", "--iterations", "1")
    //    }
    addRule("run") {
        println(this)
//        maybeCreate("berlin").doLast {
//            println("running")
//        }
    }
}