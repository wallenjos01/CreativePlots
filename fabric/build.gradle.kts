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

    minecraft("com.mojang:minecraft:1.21.10")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.18.1")

    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.11") {
        isTransitive = false
    }

    listOf(
        "fabric-registry-sync-v0"
    ).forEach { mod ->
        modApi(include(fabricApi.module(mod, "0.138.3+1.21.10"))!!)
    }

    modImplementation("me.lucko:fabric-permissions-api:0.5.0")
    modImplementation("org.wallentines:midnightlib:2.2.0")
    modImplementation("org.wallentines:databridge:0.9.0")
    modImplementation("org.wallentines:inventory-menus:0.2.2")
    modImplementation("org.wallentines:pseudonym-minecraft:0.4.3")
    modImplementation("org.wallentines:midnightcfg-platform-minecraft:3.5.1")
}
