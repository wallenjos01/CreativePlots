package org.wallentines.creativeplots.api.plot;

import org.wallentines.mdcfg.Tuples;
import org.wallentines.midnightlib.math.Vec3i;

import java.util.List;

public enum PlotDirection {

    NORTH("north", 0,-1),
    SOUTH("south", 0, 1),
    EAST ("east", 1, 0),
    WEST ("west",-1, 0);


    final String name;
    final int xs;
    final int zs;

    PlotDirection(String name, int x, int z) {

        this.name = name;
        this.xs = x;
        this.zs = z;
    }

    public String getName() {
        return name;
    }

    public static PlotDirection byName(String name) {
        for(PlotDirection dir : values()) {
            if(dir.name.equals(name)) {
                return dir;
            }
        }
        return null;
    }

    public boolean isPerpendicular(PlotDirection other) {
        return (Math.abs(xs) != Math.abs(other.xs));
    }

    public PlotDirection[] getPerpendicular() {
        PlotDirection[] out = new PlotDirection[2];
        int i = 0;
        for(PlotDirection d : values()) {
            if(d.isPerpendicular(this)) {
                out[i] = d;
                i++;
            }
        }
        return out;
    }

    public int getXShift() {
        return xs;
    }

    public int getZShift() {
        return zs;
    }

    public int magnitude() {
        return xs + zs;
    }

    public Vec3i vector() {
        return new Vec3i(xs,0,zs);
    }

    public Vec3i vectorInverted() {
        return new Vec3i(-1 * zs, 0, -1 * xs);
    }

    public static final List<Tuples.T2<PlotDirection, PlotDirection>> DIAGONAL_DIRECTIONS = List.of(
            new Tuples.T2<>(NORTH, WEST),
            new Tuples.T2<>(NORTH, EAST),
            new Tuples.T2<>(SOUTH, WEST),
            new Tuples.T2<>(SOUTH, EAST)
    );

}
