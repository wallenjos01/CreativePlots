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

    minecraft("com.mojang:minecraft:1.21.6")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.14")

    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.11") {
        isTransitive = false
    }

    listOf(
        "fabric-registry-sync-v0"
    ).forEach { mod ->
        modApi(include(fabricApi.module(mod, "0.127.0+1.21.6"))!!)
    }

    modImplementation("me.lucko:fabric-permissions-api:0.3.3")
    modImplementation("org.wallentines:midnightlib:2.1.0")
    modImplementation("org.wallentines:databridge:0.8.2")
    modImplementation("org.wallentines:inventory-menus:0.2.1")
    modImplementation("org.wallentines:pseudonym-minecraft:0.4.1")
    modImplementation("org.wallentines:midnightcfg-platform-minecraft:3.4.1")
}
