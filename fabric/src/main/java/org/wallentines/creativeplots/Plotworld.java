package org.wallentines.creativeplots;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.wallentines.mdcfg.Tuples;
import org.wallentines.midnightlib.math.Vec2i;

import java.util.UUID;

public interface Plotworld {

    void playerMoved(ServerPlayer player, BlockPos oldPos, BlockPos newPos);

    PlotMap getPlotMap();

    Tuples.T2<PlotMap.BlockType, Plot> getBlockInfo(int x, int z);

    Plot getPlotAt(Vec2i position);

    Plot claimPlot(ServerPlayer player, Vec2i plotPos);

    void clearPlot(Vec2i plotPos);

    void deletePlot(Vec2i plotPos);

    boolean isAdmin(UUID uuid);

    void setAdmin(UUID uuid, boolean admin);



}
