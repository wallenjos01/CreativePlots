package org.wallentines.creativeplots.fabric;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.common.CreativePlotsImpl;
import org.wallentines.creativeplots.common.Plot;
import org.wallentines.creativeplots.common.PlotWorld;
import org.wallentines.creativeplots.fabric.generator.PlotworldGenerator;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.midnightcore.api.text.PlaceholderManager;
import org.wallentines.midnightcore.common.util.FileUtil;
import org.wallentines.midnightcore.fabric.event.server.CommandLoadEvent;
import org.wallentines.midnightcore.fabric.event.server.ServerStopEvent;
import org.wallentines.mdcfg.ConfigSection;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.wallentines.midnightlib.event.Event;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CreativePlots implements ModInitializer {

    @Override
    public void onInitialize() {

        Path dataFolder = Paths.get("config/CreativePlots");
        if (FileUtil.tryCreateDirectory(dataFolder) == null) {
            throw new IllegalStateException("Unable to create data directory " + dataFolder);
        }

        new PlotListener().register();

        ConfigSection langDefaults = JSONCodec.loadConfig(getClass().getResourceAsStream("/creativeplots/lang/en_us.json")).asSection();
        ConfigSection configDefaults = JSONCodec.loadConfig(getClass().getResourceAsStream("/creativeplots/config.json")).asSection();

        Registry.register(BuiltInRegistries.CHUNK_GENERATOR, new ResourceLocation("creativeplots", "plotworld"), PlotworldGenerator.CODEC);

        new CreativePlotsImpl(dataFolder, langDefaults, configDefaults);

        PlotWorld.registerPlaceholders(PlaceholderManager.INSTANCE);
        Plot.registerPlaceholders(PlaceholderManager.INSTANCE);


        Event.register(CommandLoadEvent.class, this, event -> new MainCommand().register(event.getDispatcher()));
        Event.register(ServerStopEvent.class, this, event -> CreativePlotsAPI.getInstance().saveWorlds());

    }
}
