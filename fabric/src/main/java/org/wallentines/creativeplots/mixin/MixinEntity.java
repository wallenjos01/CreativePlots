package org.wallentines.creativeplots.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.EntityExtension;
import org.wallentines.creativeplots.ExplosionUtil;
import org.wallentines.creativeplots.Plot;
import org.wallentines.creativeplots.Plotworld;
import org.wallentines.midnightlib.math.Vec2i;

@Mixin(Entity.class)
@Implements(@Interface(iface=EntityExtension.class, prefix="creativeplots$"))
public abstract class MixinEntity {

    @Unique
    private Vec2i creativeplots$homePlot;

    @Shadow
    public abstract BlockPos blockPosition();

    @Shadow
    public abstract boolean equals(Object object);

    @WrapOperation(method = "setPosRaw", at = @At(value = "NEW", target = "(III)Lnet/minecraft/core/BlockPos;"))
    private BlockPos onMove(int x, int y, int z, Operation<BlockPos> original) {

        BlockPos oldPos = this.blockPosition();
        BlockPos newPos = original.call(x, y, z);

        Entity self = (Entity) (Object) this;

        Plotworld pw = (Plotworld) self.level();
        if (self instanceof ServerPlayer spl && spl.connection != null) {
            pw.playerMoved(spl, oldPos, newPos);
        } else if(creativeplots$homePlot != null 
                && !creativeplots$homePlot.equals(pw.getPlotMap().getPlotPosition(self.getBlockX(), self.getBlockZ()))) {

            self.remove(Entity.RemovalReason.DISCARDED);
        }

        return newPos;
    }

    @Inject(method="saveWithoutId", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V"))
    private void onSave(ValueOutput output, CallbackInfo ci) {
        if(creativeplots$homePlot != null) {
            output.putInt("creativeplots_home_x", creativeplots$homePlot.getX());
            output.putInt("creativeplots_home_z", creativeplots$homePlot.getY());
        }
    }

    @Inject(method="load", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private void onLoad(ValueInput input, CallbackInfo ci) {

        Integer hx = input.getInt("creativeplots_home_x").orElse(null);
        Integer hz = input.getInt("creativeplots_home_z").orElse(null);

        if(hx != null && hz != null) {
            creativeplots$homePlot = new Vec2i(hx, hz);

            Entity self = (Entity) (Object) this;
            Level level = self.level();
            if(level.isClientSide()) return;

            Plotworld pw = (Plotworld) (ServerLevel) level;

            Plot home = pw.getPlotAt(creativeplots$homePlot);
            if(home != null) {
                home.mobCount++;
            } else {
                self.remove(Entity.RemovalReason.DISCARDED);
            }
        } 
    }

    @Inject(method = "shouldBlockExplode", at = @At("RETURN"), cancellable = true)
    private void onExplode(Explosion explosion, BlockGetter blockGetter, BlockPos blockPos, BlockState blockState,
            float f, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            cir.setReturnValue(ExplosionUtil.shouldBlockExplode(explosion, blockPos));
        }
    }

    public Vec2i creativeplots$getHomePlot() {
        return creativeplots$homePlot;
    }

    public void creativeplots$setHomePlot(Vec2i pos) {

        Entity self = (Entity) (Object) this;
        Level level = self.level();
        if(level.isClientSide()) return;

        Plotworld pw = (Plotworld) (ServerLevel) level;

        if(creativeplots$homePlot != null) {
            Plot old = pw.getPlotAt(creativeplots$homePlot);
            if(old != null) old.mobCount--;
        }
        this.creativeplots$homePlot = pos;
        Plot home = pw.getPlotAt(creativeplots$homePlot);
        if(home != null) home.mobCount++;
    }

}
