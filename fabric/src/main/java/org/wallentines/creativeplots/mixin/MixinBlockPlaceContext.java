package org.wallentines.creativeplots.mixin;

import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPlaceContext.class)
public class MixinBlockPlaceContext {

    @Inject(method="canPlace", at=@At("HEAD"), cancellable = true)
    private void onPlace(CallbackInfoReturnable<Boolean> cir) {

        BlockPlaceContext ctx = (BlockPlaceContext) (Object) this;
        if(!ctx.getLevel().mayInteract(ctx.getPlayer(), ctx.getClickedPos())) {
            cir.setReturnValue(false);
        }
    }

}
