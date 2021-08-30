import java.io.ByteArrayOutputStream

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
    maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
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
    //    create("berlin").doFirst {
    //        val typed = Scanner(System.`in`).nextLine()
    //        val parsed = typed.split(' ')
    //        println ("Arguments received: " + parsed.joinToString())
    //    }
    //    register<JavaExec>("berlin") {
    //        classpath = sourceSets.main.get().runtimeClasspath
    //        mainClass.set("org.matsim.run.RunEpisim")
    //        args = listOf("--modules", "OpenBerlinScenario", "--iterations", "365")
    //    }
    register<Berlin>("berlin") {
        //        iterations = "1"
    }
    //    addRule("run") {
    //        println(this)
    ////        maybeCreate("berlin").doLast {
    ////            println("running")
    ////        }
    //    }

    register<Dresden>("dresden") {

    }
}

open class Berlin : JavaExec() {

    // JVM Args

    @Optional
    @get:Input
    @set:Option(option = "jvm parallelism", description = "-Djava.util.concurrent.ForkJoinPool.common.parallelism")
    var jvmParallelism: String? = null


    // Matsim args

    @get:Input
    @set:Option(option = "iterations", description = "number of days the simulation should run for")
    var iterations = "10"

    @get:Input
    @set:Option(option = "output", description = "the output folder where the results will be written")
    var output = "output-berlin"

    @Optional
    @get:Input
    @set:Option(option = "random seed", description = "the global random seed")
    var randomSeed: String? = null

    init {
        //        allJvmArgs = allJvmArgs + "-Xms160G" + "-Xmx160G" + "-XX:+UnlockExperimentalVMOptions" + "-XX:+UseParallelGC"
        allJvmArgs = allJvmArgs + "-Xms10G" + "-Xmx10G" + "-XX:+UnlockExperimentalVMOptions" + "-XX:+UseParallelGC"
        jvmParallelism?.let { allJvmArgs = allJvmArgs + "-Djava.util.concurrent.ForkJoinPool.common.parallelism=$it" }
        println(allJvmArgs)
        classpath = project.extensions.getByName<SourceSetContainer>("sourceSets").main.get().runtimeClasspath
        mainClass.set("org.matsim.run.RunEpisim")
    }

    override fun exec() {
        args = mutableListOf("--modules", "OpenBerlinScenario",
                             "--iterations", iterations,
                             "--config:controler.outputDirectory", output)
        randomSeed?.let { args = args!! + "--config:global.randomSeed" + it }
        super.exec()
    }
}

abstract class Hemera : DefaultTask() {

    @Optional
    @get:Input
    @set:Option(option = "nodes", description = "number of nodes to run on")
    var nodes: String = "1"

    @Optional
    @get:Input
    @set:Option(option = "time", description = "max time allowed")
    var time: String = "01:00:00"

    @Optional
    @get:Input
    @set:Option(option = "jobName", description = "the job name")
    var jobName: String = "single"

//    @Optional
    @get:Input
    @set:Option(option = "perfStat", description = "performance statistics")
    var perfStat: Boolean = true

    @TaskAction
    fun exec() {
        //store the output instead of printing to the console:
        val output = ByteArrayOutputStream()

        project.exec {

            workingDir = File("/bigdata/casus/matsim/matsim-episim-libs")

            val args = arrayListOf("srun", "-n", nodes, "-t", time, "--job-name=$jobName")
            if (perfStat) {
                args += "perf"
                args += "stat"
            }
            commandLine(args + "./gradlew" + "berlin")
//            commandLine("ls", "-la")

            standardOutput = output
        }
        println(output)
    }
}

open class Dresden : Hemera()