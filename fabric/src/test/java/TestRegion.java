import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.midnightlib.math.Vec3i;

import java.util.concurrent.atomic.AtomicInteger;

public class TestRegion {

    @Test
    public void testRegion() {

        Region region = new Region(new Vec3i(1,6,3), new Vec3i(6,4,8));

        Assertions.assertEquals(new Vec3i(1,4,3), region.getLowerBound());
        Assertions.assertEquals(new Vec3i(6,6,8), region.getUpperBound());
    }

    @Test
    public void testBorder() {

        Region reg = new Region(new Vec3i(1,1,1), new Vec3i(2,2,2));

        AtomicInteger minX = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger minY = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger maxX = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger maxY = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger iterations = new AtomicInteger();

        reg.forEachBorder(vec2i -> {
            iterations.getAndIncrement();
            if(vec2i.getX() < minX.get()) minX.set(vec2i.getX());
            if(vec2i.getY() < minY.get()) minY.set(vec2i.getY());
            if(vec2i.getX() > maxX.get()) maxX.set(vec2i.getX());
            if(vec2i.getY() > maxY.get()) maxY.set(vec2i.getY());
        });

        Assertions.assertEquals(8, iterations.get());
        Assertions.assertEquals(0, minX.get());
        Assertions.assertEquals(0, minY.get());
        Assertions.assertEquals(2, maxX.get());
        Assertions.assertEquals(2, maxY.get());


        reg = new Region(new Vec3i(-3,-3,-3), new Vec3i(4,4,4));

        minX.set(Integer.MAX_VALUE);
        minY.set(Integer.MAX_VALUE);
        maxX.set(Integer.MIN_VALUE);
        maxY.set(Integer.MIN_VALUE);
        iterations.set(0);

        reg.forEachBorder(vec2i -> {
            iterations.getAndIncrement();
            if(vec2i.getX() < minX.get()) minX.set(vec2i.getX());
            if(vec2i.getY() < minY.get()) minY.set(vec2i.getY());
            if(vec2i.getX() > maxX.get()) maxX.set(vec2i.getX());
            if(vec2i.getY() > maxY.get()) maxY.set(vec2i.getY());
        });

        Assertions.assertEquals(32, iterations.get());
        Assertions.assertEquals(-4, minX.get());
        Assertions.assertEquals(-4, minY.get());
        Assertions.assertEquals(4, maxX.get());
        Assertions.assertEquals(4, maxY.get());
    }

}
