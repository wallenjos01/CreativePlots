package org.wallentines.creativeplots.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.ExplosionUtil;
import org.wallentines.creativeplots.Plotworld;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow public abstract BlockPos blockPosition();

    @Shadow public abstract boolean equals(Object object);

    @WrapOperation(method="setPosRaw", at=@At(value="NEW", target="(III)Lnet/minecraft/core/BlockPos;"))
    private BlockPos onMove(int x, int y, int z, Operation<BlockPos> original) {

        BlockPos oldPos = this.blockPosition();
        BlockPos newPos = original.call(x, y, z);

        Entity self = (Entity) (Object) this;

        if(self instanceof ServerPlayer spl && spl.connection != null) {

            Plotworld pw = (Plotworld) spl.serverLevel();
            pw.playerMoved(spl, oldPos, newPos);
        }

        return newPos;
    }

    @Inject(method="shouldBlockExplode", at=@At("RETURN"), cancellable = true)
    private void onExplode(Explosion explosion, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState, float f, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValue()) {
            cir.setReturnValue(ExplosionUtil.shouldBlockExplode(explosion, blockPos));
        }
    }

}
