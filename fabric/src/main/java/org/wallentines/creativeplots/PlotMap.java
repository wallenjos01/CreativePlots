package org.wallentines.creativeplots;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;
import org.wallentines.midnightlib.math.CuboidRegion;
import org.wallentines.midnightlib.math.Vec2i;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;

public record PlotMap(int plotSize, int roadSize, int borderSize) {

    public int fullRoadSize() {
        return roadSize + (2 * borderSize);
    }

    public BlockType getAt(int x, int z) {

        int fullRoadSize = fullRoadSize();

        int offset = fullRoadSize / 2;
        int fullSize = plotSize + fullRoadSize;

        int posX = Math.floorMod(x + offset, fullSize);
        int posZ = Math.floorMod(z + offset, fullSize);

        if(posX >= fullRoadSize && posZ >= fullRoadSize) {
            return BlockType.PLOT;
        }

        if(posX > 0 && posX < fullRoadSize - borderSize || posZ > 0 && posZ < fullRoadSize - borderSize) {
            return BlockType.ROAD;
        }

        return BlockType.BORDER;
    }

    @Nullable
    public Vec2i getPlotPosition(int x, int z) {

        int fullRoadSize = fullRoadSize();

        int offset = fullRoadSize / 2;
        int fullSize = plotSize + fullRoadSize;

        int posX = Math.floorMod(x + offset, fullSize);
        int posZ = Math.floorMod(z + offset, fullSize);

        if(posX >= fullRoadSize && posZ >= fullRoadSize) {
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

        int fullRoadSize = fullRoadSize();

        int offset = (fullRoadSize / 2) + 1;
        int fullSize = plotSize + fullRoadSize;

        return new Vec2i(offset + fullSize * plotPosition.getX(), offset + fullSize * plotPosition.getY());
    }

    public CuboidRegion getDefaultPlotRegion(Vec2i plotPosition, int floor, int height) {

        int fullRoadSize = fullRoadSize();

        int offset = (fullRoadSize / 2) + 1;
        int fullSize = plotSize + fullRoadSize;

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
