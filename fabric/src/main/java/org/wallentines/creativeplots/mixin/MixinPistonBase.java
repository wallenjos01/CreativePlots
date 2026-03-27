package org.wallentines.creativeplots.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.PlotMap;
import org.wallentines.creativeplots.Plotworld;
import org.wallentines.midnightlib.math.Vec2i;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(PistonBaseBlock.class)
public class MixinPistonBase {

    @Inject(method="isPushable", at=@At("HEAD"), cancellable = true)
    private static void injectIsPushable(BlockState blockState, Level level, BlockPos blockPos, Direction direction, boolean bl, Direction direction2, CallbackInfoReturnable<Boolean> cir) {

        Plotworld pw = (Plotworld) level;
        PlotMap map = pw.getPlotMap();

        if(map == null) return;

        Vec2i pos = map.getPlotPosition(blockPos.getX(), blockPos.getZ());
        if(pos == null) cir.setReturnValue(false);
    }

}
