import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wallentines.creativeplots.PlotMap;
import org.wallentines.midnightlib.math.CuboidRegion;
import org.wallentines.midnightlib.math.Vec2i;
import org.wallentines.midnightlib.math.Vec3d;

public class TestPlotMap {

    @Test
    public void testPlotPosition() {

        // _+++++_=_+++++_
        // _+++++_=_+++++_
        // _+++++_=_+++++_
        // _______=_______
        // =======0=======
        // _______=_______
        // _+++++_=_+++++_
        // _+++++_=_+++++_
        // _+++++_=_+++++_
        PlotMap map = new PlotMap(5, 3);

        Assertions.assertNull(map.getPlotPosition(0, 0));

        Assertions.assertEquals(new Vec2i(0,0), map.getPlotPosition(2, 2));
        Assertions.assertEquals(new Vec2i(-1,0), map.getPlotPosition(-2, 2));
        Assertions.assertEquals(new Vec2i(0,-1), map.getPlotPosition(2, -2));
        Assertions.assertEquals(new Vec2i(-1,-1), map.getPlotPosition(-2, -2));

    }

    @Test
    public void testPlotRegion() {

        PlotMap map = new PlotMap(5, 3);

        CuboidRegion reg = map.getDefaultPlotRegion(new Vec2i(0,0), 0, 5);

        Assertions.assertEquals(new Vec3d(2, 0, 2), reg.getLowerBound());
        Assertions.assertEquals(new Vec3d(7, 5, 7), reg.getUpperBound());

    }

    @Test
    public void testPlotStart() {

        PlotMap map = new PlotMap(5, 3);

        Assertions.assertEquals(new Vec2i(2, 2), map.getPlotStart(new Vec2i(0, 0)));
        Assertions.assertEquals(new Vec2i(-6, 2), map.getPlotStart(new Vec2i(-1, 0)));
        Assertions.assertEquals(new Vec2i(-6, -6), map.getPlotStart(new Vec2i(-1, -1)));
        Assertions.assertEquals(new Vec2i(2, -6), map.getPlotStart(new Vec2i(0, -1)));

    }
}
