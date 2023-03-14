package org.wallentines.creativeplots.common;

import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.api.plot.IPlot;
import org.wallentines.creativeplots.api.plot.IPlotRegistry;
import org.wallentines.creativeplots.api.plot.IPlotWorld;
import org.wallentines.creativeplots.api.plot.PlotPos;
import org.wallentines.mdcfg.serializer.*;
import org.wallentines.midnightcore.api.player.Location;
import org.wallentines.midnightcore.api.text.PlaceholderManager;
import org.wallentines.midnightcore.api.text.PlaceholderSupplier;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlotWorld implements IPlotWorld {

    protected final int worldHeight;
    protected final int worldFloor;
    protected final int generationHeight;
    protected final int plotSize;
    protected final int roadSize;

    protected final Identifier borderBlock;
    protected final Identifier borderBlockClaimed;

    protected final Vec3d spawnLocation;
    protected final PlotRegistry plotRegistry;

    protected final HashMap<MPlayer, Vec3d> locations = new HashMap<>();

    public PlotWorld(int plotSize, int roadSize, int genHeight, int worldFloor, int worldHeight, Identifier borderBlock, Identifier borderBlockClaimed) {

        this.plotSize = plotSize;
        this.roadSize = roadSize;
        this.generationHeight = genHeight;
        this.worldFloor = worldFloor;
        this.worldHeight = worldHeight;

        this.borderBlock = borderBlock;
        this.borderBlockClaimed = borderBlockClaimed;

        this.plotRegistry = new PlotRegistry();

        float pos = roadSize % 2 == 0 ? 0.5f : 0.0f;
        spawnLocation = new Vec3d(pos, genHeight, pos);
    }


    @Override
    public int getPlotSize() {
        return plotSize;
    }

    @Override
    public int getRoadSize() {
        return roadSize;
    }

    @Override
    public int getWorldFloor() {
        return worldFloor;
    }

    @Override
    public int getWorldHeight() {
        return worldHeight;
    }

    @Override
    public int getGenerationHeight() {
        return generationHeight;
    }

    @Override
    public Identifier getBorderBlock(PlotPos position) {
        return plotRegistry.getPlotAt(position) == null ? borderBlock : borderBlockClaimed;
    }

    @Override
    public Identifier getUnclaimedBorderBlock() {
        return borderBlock;
    }

    @Override
    public Identifier getClaimedBorderBlock() {
        return borderBlockClaimed;
    }

    @Override
    public Vec3d getSpawnLocation() {

        return spawnLocation;
    }

    @Override
    public Vec3i toLocation(PlotPos position) {

        int offset = roadSize / 2;
        if(roadSize % 2 == 1) offset += 1;

        int totalSize = plotSize + roadSize;

        int x = totalSize * position.getX() + offset;
        int z = totalSize * position.getZ() + offset;

        return new Vec3i(x, generationHeight + 1, z);
    }

    @Override
    public boolean canInteract(MPlayer pl, Vec3i block) {

        IPlot plot = getPlot(block);
        if(plot != null) {
            return !plot.isDenied(pl.getUUID());
        }

        return true;
    }

    @Override
    public boolean canModify(MPlayer pl, Vec3i block) {

        IPlot plot = getPlot(block);
        if(plot != null) {
            if(plot.canEdit(pl)) return true;
        }

        return pl.hasPermission("creativeplots.editanywhere", 4);
    }

    @Override
    public IPlotRegistry getPlotRegistry() {
        return plotRegistry;
    }

    @Override
    public void onEnteredWorld(MPlayer player) {
        Vec3d location = player.getLocation().getCoordinates();
        locations.put(player, location);
        playerMoved(player, new Vec3d(0,0,0), location);
    }

    @Override
    public void onLeftWorld(MPlayer player) {
        locations.remove(player);
    }

    @Override
    public void onTick() {
        for(Map.Entry<MPlayer, Vec3d> ent : locations.entrySet()) {
            Vec3d ploc = ent.getKey().getLocation().getCoordinates();
            Vec3d oloc = ent.getValue();
            if(!ploc.equals(ent.getValue())) {

                locations.put(ent.getKey(), ploc);
                playerMoved(ent.getKey(), oloc, ploc);
            }
        }
    }

    @Override
    public void forceRefresh(IPlot plot) {

        locations.forEach((player, loc) -> {
            if(getPlot(loc.truncate()) == plot) {
                plot.onLeave(player);
                plot.onEnter(player);
            }
        });
    }

    @Override
    public IPlot getPlot(Vec3i block) {

        PlotPos[] poss;

        PlotPos pos = PlotPos.fromCoords(block.getX(), block.getZ(), plotSize, roadSize);
        if(pos == null) {
            poss = PlotPos.surroundingFromCoords(block.getX(), block.getZ(), plotSize, roadSize);
        } else {
            poss = new PlotPos[] { pos };
        }

        for(PlotPos p : poss) {

            IPlot plot = plotRegistry.getPlotAt(p);
            if(plot == null) continue;

            if(plot.contains(block)) {

                return plot;
            }
        }

        return null;
    }

    private void playerMoved(MPlayer pl, Vec3d oldLoc, Vec3d newLoc) {

        IPlot oldPlot = getPlot(oldLoc.truncate());
        IPlot newPlot = getPlot(newLoc.truncate());
        if(oldPlot == newPlot) return;

        if(oldPlot != null) {
            oldPlot.onLeave(pl);
        }

        if(newPlot != null) {

            if(newPlot.isDenied(pl.getUUID())) {
                locations.put(pl, oldLoc);
                pl.teleport(new Location(pl.getLocation().getWorldId(), oldLoc, pl.getLocation().getYaw(), pl.getLocation().getPitch()));

            } else {

                newPlot.onEnter(pl);
            }
        }
    }

    public static final Serializer<PlotWorld> SERIALIZER = new Serializer<>() {
        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> context, PlotWorld value) {
            return INTERNAL_SERIALIZER.serialize(context, value).map(pw -> Plot.plotSerializer(value).listOf().serialize(context, value.plotRegistry.getUniquePlots()).flatMap(o -> context.set("plots", o, pw)));
        }

        @Override
        public <O> SerializeResult<PlotWorld> deserialize(SerializeContext<O> context, O value) {
            SerializeResult<PlotWorld> out = INTERNAL_SERIALIZER.deserialize(context, value);

            return out.map(pw -> Plot.plotSerializer(pw).filteredListOf(CreativePlotsAPI.getLogger()::warn).deserialize(context, context.get("plots", value)).mapError(() -> SerializeResult.success(new ArrayList<>())).flatMap(plots -> {
                plots.forEach(pl -> ((Plot) pl).register(pw.plotRegistry));
                return pw;
            }));
        }
    };

    private static final Serializer<PlotWorld> INTERNAL_SERIALIZER = ObjectSerializer.create(
            NumberSerializer.forInt(1, 1000000).entry("plot_size", PlotWorld::getPlotSize),
            NumberSerializer.forInt(1, 1000000).entry("road_size", PlotWorld::getRoadSize),
            Serializer.INT.entry("generation_height", PlotWorld::getGenerationHeight),
            Serializer.INT.entry("world_floor", PlotWorld::getWorldFloor),
            Serializer.INT.entry("world_height", PlotWorld::getWorldHeight),
            Identifier.serializer("minecraft").entry("border_block", PlotWorld::getUnclaimedBorderBlock),
            Identifier.serializer("minecraft").entry("border_block_claimed", PlotWorld::getClaimedBorderBlock),
            PlotWorld::new
    );

    public static void registerPlaceholders(PlaceholderManager manager) {

        manager.getInlinePlaceholders().register("creativeplots_plotworld_id",   PlaceholderSupplier.create(IPlotWorld.class, pw -> CreativePlotsAPI.getInstance().getPlotWorldId(pw).toString()));
        manager.getInlinePlaceholders().register("creativeplots_plotworld_name", PlaceholderSupplier.create(IPlotWorld.class, pw -> CreativePlotsAPI.getInstance().getPlotWorldId(pw).getPath()));
    }

    @Override
    public String toString() {
        return "PlotWorld{" +
                "worldHeight=" + worldHeight +
                ", worldFloor=" + worldFloor +
                ", generationHeight=" + generationHeight +
                ", plotSize=" + plotSize +
                ", roadSize=" + roadSize +
                ", borderBlock=" + borderBlock +
                ", borderBlockClaimed=" + borderBlockClaimed +
                '}';
    }
}
