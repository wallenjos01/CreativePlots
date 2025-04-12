package org.wallentines.creativeplots;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.wallentines.midnightlib.math.CuboidRegion;
import org.wallentines.midnightlib.math.Vec2i;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;

public record PlotMap(int plotSize, int roadSize) {

    public BlockType getAt(int x, int z) {

        int offset = roadSize / 2;
        int fullSize = plotSize + roadSize;

        int posX = Math.floorMod(x + offset, fullSize);
        int posZ = Math.floorMod(z + offset, fullSize);

        if(posX >= roadSize && posZ >= roadSize) {
            return BlockType.PLOT;
        }

        if(posX > 0 && posX < roadSize - 1 || posZ > 0 && posZ < roadSize - 1) {
            return BlockType.ROAD;
        }

        return BlockType.BORDER;
    }

    @Nullable
    public Vec2i getPlotPosition(int x, int z) {

        int offset = roadSize / 2;
        int fullSize = plotSize + roadSize;

        int posX = Math.floorMod(x + offset, fullSize);
        int posZ = Math.floorMod(z + offset, fullSize);

        if(posX >= roadSize && posZ >= roadSize) {
            int plotX = Math.floorDiv((x + offset), fullSize);
            int plotZ = Math.floorDiv((z + offset), fullSize);
            return new Vec2i(plotX, plotZ);
        }

        return null;
    }

    public Vec2i getAdjacent(Vec2i plot, Direction direction) {
        return new Vec2i(plot.getX() + direction.getStepX(), plot.getY() + direction.getStepZ());
    }

    public Vec2i getPlotStart(Vec2i plotPosition) {

        int offset = (roadSize / 2) + 1;
        int fullSize = plotSize + roadSize;

        return new Vec2i(offset + fullSize * plotPosition.getX(), offset + fullSize * plotPosition.getY());
    }

    public CuboidRegion getDefaultPlotRegion(Vec2i plotPosition, int floor, int height) {

        int offset = (roadSize / 2) + 1;
        int fullSize = plotSize + roadSize;

        Vec3i base = new Vec3i(offset + fullSize * plotPosition.getX(), floor, offset + fullSize * plotPosition.getY());
        Vec3i extent = new Vec3i(plotSize, height, plotSize);

        Vec3d based = new Vec3d(base.getX(), base.getY(), base.getZ());
        return new CuboidRegion(based, based.add(new Vec3d(extent.getX(), extent.getY(), extent.getZ())));

    }


    public enum BlockType {
        PLOT,
        ROAD,
        BORDER
    }

}
