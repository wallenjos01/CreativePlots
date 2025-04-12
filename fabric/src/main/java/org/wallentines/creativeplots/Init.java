package org.wallentines.creativeplots;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Init implements ModInitializer {

    private static final Logger log = LoggerFactory.getLogger(Init.class);

    @Override
    public void onInitialize() {

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, "creativeplots:plotworld", PlotworldGenerator.CODEC);

        try {
            Class.forName("com.sk89q.worldedit.fabric.FabricPlayer");
            WorldeditIntegration.register();
        } catch (ReflectiveOperationException e) {
            log.error("Unable to initialize CreativePlots WorldEdit integration!", e);
        }

    }
}
