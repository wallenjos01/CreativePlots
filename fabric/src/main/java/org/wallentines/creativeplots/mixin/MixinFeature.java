package org.wallentines.creativeplots.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wallentines.creativeplots.Plot;
import org.wallentines.creativeplots.PlotMap;
import org.wallentines.creativeplots.Plotworld;
import org.wallentines.mdcfg.Tuples;


@Mixin(Feature.class)
public class MixinFeature {

    @Inject(method="setBlock", at=@At("HEAD"), cancellable = true)
    private void onSetBlock(LevelWriter levelWriter, BlockPos blockPos, BlockState blockState, CallbackInfo ci) {
        if(levelWriter instanceof Plotworld pw) {
            Tuples.T2<PlotMap.BlockType, Plot> info = pw.getBlockInfo(blockPos.getX(), blockPos.getZ());
            if(info.p1 != null && info.p2 == null) {
                ci.cancel();
            }
        }
    }

}
