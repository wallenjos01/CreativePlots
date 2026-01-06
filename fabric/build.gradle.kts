import buildlogic.Utils

plugins {
    id("build.library")
    id("build.fabric")
}

Utils.setupResources(project, rootProject, "fabric.mod.json")

repositories {
    maven("https://maven.enginehub.org/repo/")
}

dependencies {

    minecraft("com.mojang:minecraft:${project.properties["minecraft-version"]}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.properties["fabric-loader-version"]}")
    
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.18") {
        isTransitive = false
    }

    modCompileOnly("com.sk89q.worldedit:worldedit-fabric-mc1.21.11:7.3.18") {
        isTransitive = false
    }

    listOf(
        "fabric-registry-sync-v0"
    ).forEach { mod ->
        modApi(include(fabricApi.module(mod, "${project.properties["fabric-api-version"]}"))!!)
    }

    modImplementation("me.lucko:fabric-permissions-api:0.6.1")
    modImplementation("org.wallentines:midnightlib:2.2.0")
    modImplementation("org.wallentines:databridge:0.10.1")
    modImplementation("org.wallentines:inventory-menus:0.2.3")
    modImplementation("org.wallentines:pseudonym-minecraft:0.4.4")
    modImplementation("org.wallentines:midnightcfg-platform-minecraft:3.5.2")
}
