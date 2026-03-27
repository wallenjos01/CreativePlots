package org.wallentines.creativeplots.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.Plotworld;

@Mixin(SpawnEggItem.class)
public abstract class MixinSpawnEgg {

    @Shadow public abstract EntityType<?> getType(ItemStack itemStack);

    @Inject(method = "useOn", at= @At(value = "INVOKE", target = "Lnet/minecraft/world/item/context/UseOnContext;getItemInHand()Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    private void onUse(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir, @Local Level level) {

        Plotworld pw = (Plotworld) level;
        if(pw.getPlotMap() == null) return;

        EntityType<?> entityType = getType(context.getItemInHand());

        if(context.getPlayer() == null || (entityType == EntityType.ENDER_DRAGON || entityType == EntityType.WITHER) && !pw.isAdmin(context.getPlayer().getUUID())) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }

    // @WrapOperation(method="spawnMob", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"))
    // private <T extends Entity> T onSpawn(EntityType<T> instance, ServerLevel level, ItemStack item, LivingEntity entity, BlockPos pos, EntitySpawnReason reason, boolean b1, boolean b2, Operation<T> operation) {
    //
    //     T out = operation.call(instance, level, item, entity, pos, reason, b1, b2);
    //     if(level.isClientSide()) return out;
    //
    //     Plotworld pw = (Plotworld) (ServerLevel) level;
    //     PlotMap map = pw.getPlotMap();
    //     if(map == null || pw.isAdmin(entity.getUUID())) return out;
    //
    //     Vec2i plotPos = map.getPlotPosition(pos.getX(), pos.getZ());
    //     Plot plot = pw.getPlotAt(plotPos);
    //
    //     if(plot == null || plot.mobCount > plot.mobCap) {
    //         System.out.println("Removed from spawnMob()");
    //         out.remove(Entity.RemovalReason.DISCARDED);
    //     } else {
    //         ((EntityExtension) out).setHomePlot(plotPos);
    //     }
    //
    //     return out;
    // }

}
