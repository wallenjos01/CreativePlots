package org.wallentines.creativeplots.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.Plotworld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@Mixin(Player.class)
public abstract class MixinPlayer extends Avatar {

    protected MixinPlayer(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method="mayUseItemAt", at=@At("HEAD"), cancellable=true)
    private void injectMayUse(BlockPos blockPos, Direction direction, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {

        if(level().isClientSide()) return;

        ServerLevel lvl = (ServerLevel) level();
        Plotworld plotworld = (Plotworld) lvl;


        cir.setReturnValue(plotworld.canEntityModify((ServerPlayer) (Object) this, blockPos));

    }

}
