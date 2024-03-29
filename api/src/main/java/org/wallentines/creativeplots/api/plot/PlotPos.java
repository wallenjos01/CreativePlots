package org.wallentines.creativeplots.api.plot;

import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.math.Vec3i;

public class PlotPos implements Cloneable {

    // The coordinates of the plot
    private final int x, z;
    private final int hashCode;


    /**
     * Constructs a new PlotPos object at given coordinates
     *
     * @param x The X coordinate
     * @param z The Y coordinate
     */
    public PlotPos(int x, int z) {
        this.x = x;
        this.z = z;

        hashCode = toString().hashCode();
    }

    /**
     * Returns the X coordinate of a plot
     *
     * @return X coordinate as an int
     */
    public int getX() {
        return x;
    }

    /**
     * Returns the Z coordinate of a plot
     *
     * @return Z coordinate as an int
     */
    public int getZ() {
        return z;
    }


    /**
     * Converts the PlotPos into a Region object
     *
     * @param world The PlotWorld object to query
     * @return A Region derived from the plot's location and world
     */
    public Region getRegion(IPlotWorld world) {

        int size = world.getPlotSize();

        Vec3i loc = world.toLocation(this);

        Vec3i p1 = new Vec3i(loc.getX(), world.getWorldFloor(), loc.getZ());
        Vec3i p2 = new Vec3i(loc.getX() + size, world.getWorldHeight() - world.getWorldFloor(), loc.getZ() + size);

        return new Region(p1, p2);
    }


    /**
     * Returns the position of an adjacent plot
     *
     * @param direction The direction of the adjacent plot relative to this position
     * @return The adjacent plot's position as a PlotPos
     */
    public PlotPos getAdjacent(PlotDirection direction) {
        return new PlotPos(x + direction.getXShift(), z + direction.getZShift());
    }

    public PlotPos getDiagonal(PlotDirection ns, PlotDirection ew) {
        if(!ns.isPerpendicular(ew)) throw new IllegalArgumentException("Directions must be perpendicular!");
        return new PlotPos(x + ns.getXShift() + ew.getXShift(), z + ns.getZShift() + ew.getZShift());
    }


    /**
     * Checks whether another PlotPos is adjacent to this one
     *
     * @param pos The position of the other plot
     * @return Whether the other plot is adjacent, as a boolean
     */
    public boolean isAdjacent(PlotPos pos) {
        return Math.abs(pos.x - x) == 1 || Math.abs(pos.z -z) == 1;
    }


    // Override Methods

    @Override
    public boolean equals(Object obj) {

        if(!(obj instanceof PlotPos)) return false;

        PlotPos pos = (PlotPos) obj;
        return pos.x == x && pos.z == z;
    }

    @Override
    public String toString() {
        return "[" + this.x + ", " + this.z + "]";
    }

    private static int integerDivide(int a, int b)
    {
        if (a < 0)
            if (b < 0)
                return -a / -b;
            else
                return -(-a / b) - (-a % b != 0 ? 1 : 0);
        else if (b < 0)
            return -(a / -b) - (a % -b != 0 ? 1 : 0);
        else
            return a / b;
    }


    /**
     * Generates a PlotPos from world coordinates
     *
     * @param x        The X coordinate in the world
     * @param z        The Y coordinate in the world
     * @param plotSize The size of each plot in the world
     * @param roadSize The size of the roads in the world
     * @return         A new PlotPos object
     */
    public static PlotPos fromCoords(int x, int z, int plotSize, int roadSize) {

        int offset = roadSize / 2;
        if(roadSize % 2 == 1) offset += 1;

        x -= offset;
        z -= offset;

        int size = plotSize + roadSize;

        int ox = x % size;
        int oz = z % size;

        if(ox < 0) ox += size;
        if(oz < 0) oz += size;

        if(ox < plotSize && oz < plotSize) {

            int px = integerDivide(x, size);
            int pz = integerDivide(z, size);

            return new PlotPos(px, pz);
        }

        return null;
    }

    /**
     * Generates a list of surrounding PlotPos objects from world coordinates
     *
     * @param x        The X coordinate in the world
     * @param z        The Y coordinate in the world
     * @param plotSize The size of each plot in the world
     * @param roadSize The size of the roads in the world
     * @return         An array of PlotPos objects
     */
    public static PlotPos[] surroundingFromCoords(int x, int z, int plotSize, int roadSize) {

        int offset = roadSize / 2;
        if(roadSize % 2 == 1) offset += 1;

        x = x - offset;
        z = z - offset;

        int size = plotSize + roadSize;

        int hx = integerDivide(x, size);
        int hz = integerDivide(z, size);
        int lx = hx - 1;
        int lz = hz - 1;

        PlotPos[] poss = new PlotPos[4];
        poss[0] = new PlotPos(lx, lz);
        poss[1] = new PlotPos(hx, lz);
        poss[2] = new PlotPos(lx, hz);
        poss[3] = new PlotPos(hx, hz);

        return poss;
    }

    public static boolean isPlotBorder(int x, int z, int plotSize, int roadSize) {

        int offset = roadSize / 2;
        if(roadSize % 2 == 1) offset += 1;

        x -= offset;
        z -= offset;

        int size = plotSize + roadSize;

        int ox = x % size;
        int oz = z % size;

        if(ox < 0) ox += size;
        if(oz < 0) oz += size;

        return (ox == size - 1 || ox == plotSize || oz == size - 1 || oz == plotSize) && ((ox <= plotSize || ox == size - 1) && (oz <= plotSize || oz == size - 1));

    }

    public Region getRoad(PlotDirection direction, IPlotWorld world) {

        int roadSize = world.getRoadSize();
        int plotSize = world.getPlotSize();

        int lowOffset = direction.magnitude() == 1 ? plotSize : roadSize;
        int highOffset = direction.magnitude() == 1 ? roadSize : plotSize;

        Vec3i offset = new Vec3i(direction.xs, 0, direction.zs);
        Region plot = getRegion(world);

        return new Region(
                plot.getLowerBound().add(offset.multiply(lowOffset)),
                plot.getUpperBound().add(offset.multiply(highOffset))
        );
    }

    public Region getIntersection(PlotDirection ns, PlotDirection ew, IPlotWorld world) {

        if(!ns.isPerpendicular(ew)) throw new IllegalArgumentException("Directions must be perpendicular!");

        int roadSize = world.getRoadSize();
        int plotSize = world.getPlotSize();

        int shiftXLower = Math.max(0, ns.getXShift() + ew.getXShift());
        int shiftZLower = Math.max(0, ns.getZShift() + ew.getZShift());
        int shiftXUpper = ns.getXShift() + ew.getXShift();
        int shiftZUpper = ns.getZShift() + ew.getZShift();

        Region plot = getRegion(world);
        Vec3i origin = plot.getLowerBound().add(new Vec3i(shiftXLower, 0, shiftZLower).multiply(plotSize));


        return new Region(
                origin,
                new Vec3i(origin.getX(), plot.getUpperBound().getY(), origin.getZ()).add(new Vec3i(shiftXUpper, 0, shiftZUpper).multiply(roadSize))
        );
    }

    public static final Serializer<PlotPos> SERIALIZER = ObjectSerializer.create(
            Serializer.INT.entry("x", PlotPos::getX),
            Serializer.INT.entry("z", PlotPos::getZ),
            PlotPos::new
    );

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public PlotPos clone() {
        try {
            return (PlotPos) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
