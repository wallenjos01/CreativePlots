package org.wallentines.creativeplots.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.Plotworld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.context.UseOnContext;

@Mixin(FlintAndSteelItem.class)
public class MixinFlintAndSteel {

    @Inject(method="useOn", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), cancellable=true)
    private void injectUse(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {

        BlockPos where = ctx.getClickedPos().relative(ctx.getClickedFace());
        Plotworld pw = (Plotworld) ctx.getLevel();

        if(!pw.canEntityModify(ctx.getPlayer(), where)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }


}
