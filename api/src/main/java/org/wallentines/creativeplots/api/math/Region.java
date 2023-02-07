package org.wallentines.creativeplots.api.math;

import org.wallentines.midnightlib.math.Vec2i;
import org.wallentines.midnightlib.math.Vec3i;

import java.util.Objects;
import java.util.function.Consumer;

public class Region {

    Vec3i lower;
    Vec3i upper;

    public Region(Vec3i p1, Vec3i p2) {

        this.lower = new Vec3i(
                Math.min(p1.getX(), p2.getX()),
                Math.min(p1.getY(), p2.getY()),
                Math.min(p1.getZ(), p2.getZ()));
        this.upper = new Vec3i(
                Math.max(p1.getX(), p2.getX()),
                Math.max(p1.getY(), p2.getY()),
                Math.max(p1.getZ(), p2.getZ())
        );
    }

    public Vec3i getLowerBound() {
        return lower;
    }

    public Vec3i getUpperBound() {
        return upper;
    }

    public boolean contains(Vec3i point) {

        return  point.getX() >= lower.getX() && point.getX() < upper.getX() &&
                point.getY() >= lower.getY() && point.getY() < upper.getY() &&
                point.getZ() >= lower.getZ() && point.getZ() < upper.getZ();
    }

    public Region outset(int i) {
        return new Region(lower.subtract(i), upper.add(i));
    }

    public void forEachBorder(Consumer<Vec2i> column) {

        for(int x = lower.getX() - 1 ; x < upper.getX() + 1 ; x++) {
            column.accept(new Vec2i(x, lower.getZ() - 1));
            column.accept(new Vec2i(x, upper.getZ()));
        }

        for(int z = lower.getZ() ; z < upper.getZ() ; z++) {
            column.accept(new Vec2i(lower.getX() - 1, z));
            column.accept(new Vec2i(upper.getX(), z));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Region region = (Region) o;
        return Objects.equals(lower, region.lower) && Objects.equals(upper, region.upper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper);
    }

    @Override
    public String toString() {
        return "[" + lower.toString() + "]-[" + upper.toString() + "]";
    }
}
