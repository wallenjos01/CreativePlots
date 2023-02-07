import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.creativeplots.api.plot.PlotPos;
import org.wallentines.creativeplots.common.Plot;
import org.wallentines.creativeplots.common.PlotWorld;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightlib.registry.Identifier;

import java.util.List;

public class TestPlot {

    private static final PlotWorld PLOT_WORLD = new PlotWorld(
            100,
            5,
            64,
            -64,
            256,
            new Identifier("minecraft", "smooth_stone_slab"),
            new Identifier("minecraft", "smooth_quartz_slab"));


    @Test
    public void testArea() {

        Plot plot = new Plot(PLOT_WORLD, "test", new PlotPos(0,0));
        int floor = PLOT_WORLD.getWorldFloor();
        int ceil = PLOT_WORLD.getWorldHeight() - PLOT_WORLD.getWorldFloor();

        List<Region> area = plot.getArea();

        Assertions.assertEquals(1, area.size());
        Assertions.assertEquals(new Region(new Vec3i(3, floor, 3), new Vec3i(103, ceil, 103)), area.get(0));


        plot.merge(new Plot(PLOT_WORLD, "test2", new PlotPos(0, 1)));
        area = plot.getArea();

        Assertions.assertEquals(1, area.size());
        Assertions.assertEquals(new Region(new Vec3i(3, floor, 3), new Vec3i(103, ceil, 208)), area.get(0));


        plot.merge(new Plot(PLOT_WORLD, "test3", new PlotPos(1,1)));
        area = plot.getArea();

        Assertions.assertEquals(2, area.size());
        Assertions.assertEquals(new Region(new Vec3i(3, floor, 3), new Vec3i(103, ceil, 208)), area.get(0));
        Assertions.assertEquals(new Region(new Vec3i(3, floor, 108), new Vec3i(208, ceil, 208)), area.get(1));


        plot.merge(new Plot(PLOT_WORLD, "test4", new PlotPos(1,0)));
        area = plot.getArea();
        Assertions.assertEquals(1, area.size());
        Assertions.assertEquals(new Region(new Vec3i(3, floor, 3), new Vec3i(208, ceil, 208)), area.get(0));
    }

}
