package org.wallentines.creativeplots;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;

public class ExplosionUtil {

    public static boolean shouldBlockExplode(Explosion explosion, BlockPos blockPos) {

        Plotworld pw = (Plotworld) explosion.level();
        PlotMap map = pw.getPlotMap();

        if(map == null) {
            return true;
        }

        Entity ent = explosion.getIndirectSourceEntity();
        if(ent instanceof ServerPlayer spl) {
            return explosion.level().mayInteract(spl, blockPos);
        } else {
            return false;
        }

    }

}
