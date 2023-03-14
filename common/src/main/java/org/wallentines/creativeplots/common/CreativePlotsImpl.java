package org.wallentines.creativeplots.common;

import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.api.plot.IPlotWorld;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.midnightcore.api.FileConfig;
import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.api.text.LangProvider;
import org.wallentines.midnightcore.api.text.LangRegistry;
import org.wallentines.midnightlib.registry.Identifier;
import org.wallentines.midnightlib.registry.Registry;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CreativePlotsImpl extends CreativePlotsAPI {

    private final Registry<IPlotWorld> plotWorlds;
    private final LangProvider langProvider;
    private final FileConfig configFile;
    private final ConfigSection configDefaults;

    private final List<Identifier> allowedItems;

    private int maxSize = 4;

    public CreativePlotsImpl(Path dataFolder, ConfigSection langDefaults, ConfigSection configDefaults) {

        File langFolder = dataFolder.resolve("lang").toFile();
        if(!langFolder.isDirectory() && !langFolder.mkdirs()) {
            throw new IllegalArgumentException("Unable to create lang folder at " + langFolder.getPath() + "!");
        }

        this.plotWorlds = new Registry<>("minecraft");
        this.langProvider = new LangProvider(langFolder, LangRegistry.fromConfigSection(langDefaults));
        this.configFile = FileConfig.findOrCreate("config", dataFolder.toFile());
        this.configDefaults = configDefaults;
        this.allowedItems = new ArrayList<>();

        loadConfig();

        MidnightCoreAPI.onServerStartup(server ->
            server.tickEvent().register(this, ev -> {
                for(IPlotWorld world : CreativePlotsAPI.getInstance().getWorlds()) {
                    world.onTick();
                }
            }));
    }

    public long reload() {

        long time = System.currentTimeMillis();

        // Reset content
        plotWorlds.clear();
        allowedItems.clear();

        // Load new content
        loadConfig();

        time = System.currentTimeMillis() - time;
        return time;

    }

    public LangProvider getLangProvider() {
        return langProvider;
    }

    public void saveWorlds() {

        if(plotWorlds.getSize() == 0) return;

        ConfigSection worlds = configFile.getRoot().getOrCreateSection("worlds");
        for(IPlotWorld world : plotWorlds) {

            Identifier id = plotWorlds.getId(world);
            worlds.set(id.toString(), (PlotWorld) world, PlotWorld.SERIALIZER);
        }

        configFile.save();
    }

    private void loadConfig() {

        ConfigSection root = configFile.getRoot();

        root.fill(configDefaults);
        configFile.save();

        if(root.hasSection("worlds")) {

            ConfigSection sec = root.getSection("worlds");
            for(String key : sec.getKeys()) {

                if(!sec.hasSection(key)) continue;
                Identifier id = Identifier.parse(key);

                SerializeResult<PlotWorld> pw = PlotWorld.SERIALIZER.deserialize(ConfigContext.INSTANCE, sec.get(key));
                if(pw.isComplete()) {
                    PlotWorld world = pw.getOrThrow();
                    plotWorlds.register(id, world);
                    getLogger().info("Registered Plot World " + key + " with " + world.plotRegistry.getSize() + " registered plots");
                    getLogger().info(world.toString());
                } else {
                    getLogger().warn("An error occurred while parsing a plot world!");
                    getLogger().warn(pw.getError());
                }
            }
        }

        allowedItems.addAll(root.getListFiltered("allowed_items", Identifier.serializer("minecraft")));
        maxSize = root.getInt("max_plot_size");

    }

    public int getMaxPlotSize() {
        return maxSize;
    }

    public IPlotWorld getPlotWorld(MPlayer player) {

        return plotWorlds.get(player.getLocation().getWorldId());
    }

    public IPlotWorld getPlotWorld(Identifier id) {

        return plotWorlds.get(id);

    }

    public Identifier getPlotWorldId(IPlotWorld world) {

        return plotWorlds.getId(world);
    }

    public Iterable<IPlotWorld> getWorlds() {
        return plotWorlds;
    }

    public boolean isAllowedItem(Identifier id) {
        for(Identifier item : allowedItems) {
            if(item.equals(id)) return true;
        }
        return false;
    }

}
