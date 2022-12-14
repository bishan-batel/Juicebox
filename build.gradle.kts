plugins {
	`java-library`
	id("io.papermc.paperweight.userdev") version "1.3.11"
	id("xyz.jpenilla.run-paper") version "2.0.0" // Adds runServer and runMojangMappedServer tasks for testing

	// Shades and relocates dependencies into our plugin jar. See https://imperceptiblethoughts.com/shadow/introduction/
	id("com.github.johnrengelman.shadow") version "7.1.2"

	// kotlin
	id("org.jetbrains.kotlin.jvm") version "1.5.31"
}

group = "app.bishan.juicebox"
version = "1.7.0-SNAPSHOT"
description = "Test plugin for paperweight-userdev"

java {
	// Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
	toolchain.languageVersion.set(JavaLanguageVersion.of(17))

	// Configure the source and target compatibility for the project.
//	sourceCompatibility = JavaVersion.VERSION_17
//	targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
	paperDevBundle("1.19.2-R0.1-SNAPSHOT")
//	 paperweightDevBundle("com.example.paperfork", "1.19.2-R0.1-SNAPSHOT")

	// You will need to manually specify the full dependency if using the groovy gradle dsl
	// (paperDevBundle and paperweightDevBundle functions do not work in groovy)
	// paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.19.2-R0.1-SNAPSHOT")

	// Shadow will include the runtimeClasspath by default, which implementation adds to.
	// Dependencies you don't want to include go in the compileOnly configuration.
	// Make sure to relocate shaded dependencies!
//	implementation("cloud.commandframework", "cloud-paper", "1.7.1")

	// kotlin
	implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.5.31")
	implementation("org.jetbrains.kotlin", "kotlin-reflect", "1.5.31")
}

// kotlin
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

tasks {
	// Configure reobfJar to run when invoking the build task
	assemble {
		dependsOn(reobfJar)
	}

	compileJava {
		options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

		// Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
		// See https://openjdk.java.net/jeps/247 for more information.
		options.release.set(17)
	}

	jar {
		manifest.attributes["Main-Class"] = "app.bishan.juicebox.JuiceboxPlugin"
	}

	javadoc {
		options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
	}

	processResources {
		filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything

//		val props = mapOf("plugin.version" to project.version)
//		inputs.properties(props)
//		filesMatching("plugin.yml") {
//			expand(props)
//		}
	}


	reobfJar {
		// This is an example of how you might change the output location for reobfJar. It's recommended not to do this
		// for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
		outputJar.set(layout.buildDirectory.file("libs/${project.name}-${project.version}-reobf.jar"))
	}



	shadowJar {
		// helper function to relocate a package into our package
		fun reloc(pkg: String) = relocate(pkg, "io.papermc.paperweight.testplugin.dependency.$pkg")

		// relocate cloud and it's transitive dependencies
//		reloc("cloud.commandframework")
//		reloc("io.leangen.geantyref")
	}
}

// customn task to clear all built jars, force run shadowJar, and reobfJar
tasks.register("buildPlugin") {
	// delete all jars
	// run
	dependsOn("shadowJar", "reobfJar")
}
