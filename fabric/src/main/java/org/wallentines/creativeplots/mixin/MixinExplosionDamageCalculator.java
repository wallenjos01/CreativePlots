package org.wallentines.creativeplots.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.ExplosionUtil;

@Mixin(ExplosionDamageCalculator.class)
public class MixinExplosionDamageCalculator {


    @Inject(method="shouldBlockExplode", at=@At(value="RETURN"), cancellable = true)
    private void onExplode(Explosion explosion, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, float f, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            cir.setReturnValue(ExplosionUtil.shouldBlockExplode(explosion, blockPos));
        }
    }

}
