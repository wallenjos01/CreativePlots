package org.wallentines.creativeplots.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wallentines.creativeplots.api.plot.IPlotWorld;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.api.text.LangProvider;
import org.wallentines.midnightlib.registry.Identifier;

public abstract class CreativePlotsAPI {

    protected CreativePlotsAPI() {
        INSTANCE = this;
    }

    private static final Logger LOGGER = LogManager.getLogger("CreativePlots");
    private static CreativePlotsAPI INSTANCE;

    public abstract long reload();

    public abstract LangProvider getLangProvider();

    public abstract void saveWorlds();

    public abstract int getMaxPlotSize();

    public abstract IPlotWorld getPlotWorld(MPlayer player);

    public abstract IPlotWorld getPlotWorld(Identifier id);

    public abstract Identifier getPlotWorldId(IPlotWorld world);

    public abstract Iterable<IPlotWorld> getWorlds();

    public abstract boolean isAllowedItem(Identifier id);

    public static CreativePlotsAPI getInstance() {
        return INSTANCE;
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
