package org.wallentines.creativeplots.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.EntityExtension;
import org.wallentines.creativeplots.Plotworld;
import org.wallentines.midnightlib.math.Vec2i;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

@Mixin(EntityType.class)
public class MixinEntityType {

    @Inject(method="create(Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Consumer;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/Entity;", at=@At("TAIL"))
    private <T extends Entity> void onCreate(ServerLevel level, Consumer<T> consumer, BlockPos block, EntitySpawnReason reason, boolean bl, boolean bl2, CallbackInfoReturnable<T> cir) {

        Plotworld pw = (Plotworld) level;
        if(reason == EntitySpawnReason.LOAD || reason == EntitySpawnReason.COMMAND || pw.getPlotMap() == null || cir.getReturnValue() == null) return;

        Vec2i plotPos = pw.getPlotMap().getPlotPosition(block.getX(), block.getZ());
        if(plotPos == null) {
            cir.getReturnValue().discard();
        } else {
            ((EntityExtension) cir.getReturnValue()).setHomePlot(plotPos);
        }
    }

}
