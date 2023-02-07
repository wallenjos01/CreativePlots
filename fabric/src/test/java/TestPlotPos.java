import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.creativeplots.api.plot.PlotDirection;
import org.wallentines.creativeplots.api.plot.PlotPos;
import org.wallentines.creativeplots.common.PlotWorld;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightlib.registry.Identifier;

public class TestPlotPos {

    private static final PlotWorld PLOT_WORLD = new PlotWorld(
            100,
            5,
            64,
            -64,
            256,
            new Identifier("minecraft", "smooth_stone_slab"),
            new Identifier("minecraft", "smooth_quartz_slab"));

    @Test
    public void testRegion() {

        PlotPos pos = new PlotPos(0,0);
        int floor = PLOT_WORLD.getWorldFloor();
        int ceil = PLOT_WORLD.getWorldHeight() - PLOT_WORLD.getWorldFloor();

        Region reg = pos.getRegion(PLOT_WORLD);

        Assertions.assertEquals(new Vec3i(3,floor,3), reg.getLowerBound());
        Assertions.assertEquals(new Vec3i(103,ceil,103), reg.getUpperBound());



        pos = new PlotPos(-1,-1);
        reg = pos.getRegion(PLOT_WORLD);

        Assertions.assertEquals(new Vec3i(-102,floor,-102), reg.getLowerBound());
        Assertions.assertEquals(new Vec3i(-2,ceil,-2), reg.getUpperBound());

    }

    @Test
    public void testRoads() {

        PlotPos pos = new PlotPos(0,0);
        int floor = PLOT_WORLD.getWorldFloor();
        int ceil = PLOT_WORLD.getWorldHeight() - PLOT_WORLD.getWorldFloor();

        Region northRoad = pos.getRoad(PlotDirection.NORTH, PLOT_WORLD);

        Assertions.assertEquals(new Vec3i(3,floor,-2), northRoad.getLowerBound());
        Assertions.assertEquals(new Vec3i(103,ceil,3), northRoad.getUpperBound());

        Region westRoad = pos.getRoad(PlotDirection.WEST, PLOT_WORLD);

        Assertions.assertEquals(new Vec3i(-2,floor,3), westRoad.getLowerBound());
        Assertions.assertEquals(new Vec3i(3,ceil,103), westRoad.getUpperBound());

        Region southRoad = pos.getRoad(PlotDirection.SOUTH, PLOT_WORLD);

        Assertions.assertEquals(new Vec3i(3,floor,103), southRoad.getLowerBound());
        Assertions.assertEquals(new Vec3i(103,ceil,108), southRoad.getUpperBound());

        Region eastRoad = pos.getRoad(PlotDirection.EAST, PLOT_WORLD);

        Assertions.assertEquals(new Vec3i(103,floor,3), eastRoad.getLowerBound());
        Assertions.assertEquals(new Vec3i(108,ceil,103), eastRoad.getUpperBound());
    }

    @Test
    public void testIntersections() {

        PlotPos pos = new PlotPos(0,0);
        int floor = PLOT_WORLD.getWorldFloor();
        int ceil = PLOT_WORLD.getWorldHeight() - PLOT_WORLD.getWorldFloor();

        Region northWestInt = pos.getIntersection(PlotDirection.NORTH, PlotDirection.WEST, PLOT_WORLD);
        Assertions.assertEquals(new Vec3i(-2, floor, -2), northWestInt.getLowerBound());
        Assertions.assertEquals(new Vec3i(3, ceil, 3), northWestInt.getUpperBound());

        Region northEastInt = pos.getIntersection(PlotDirection.NORTH, PlotDirection.EAST, PLOT_WORLD);
        Assertions.assertEquals(new Vec3i(103, floor, -2), northEastInt.getLowerBound());
        Assertions.assertEquals(new Vec3i(108, ceil, 3), northEastInt.getUpperBound());

        Region southWestInt = pos.getIntersection(PlotDirection.SOUTH, PlotDirection.WEST, PLOT_WORLD);
        Assertions.assertEquals(new Vec3i(-2, floor, 103), southWestInt.getLowerBound());
        Assertions.assertEquals(new Vec3i(3, ceil, 108), southWestInt.getUpperBound());

        Region southEastInt = pos.getIntersection(PlotDirection.SOUTH, PlotDirection.EAST, PLOT_WORLD);
        Assertions.assertEquals(new Vec3i(103, floor, 103), southEastInt.getLowerBound());
        Assertions.assertEquals(new Vec3i(108, ceil, 108), southEastInt.getUpperBound());


    }

}