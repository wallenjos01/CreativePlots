package org.wallentines.creativeplots.mixin;

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

}
