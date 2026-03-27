package org.wallentines.creativeplots.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

@Mixin(ServerGamePacketListenerImpl.class)
public class MixinServerPacketListener {

    @WrapOperation(method="handleUseItemOn", at=@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerLevel;mayInteract(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean wrapUseItem(ServerLevel instance, Entity interactor, BlockPos blockPos, Operation<Boolean> original, @Local ItemStack usedItem) {

        if(usedItem.getItem() instanceof BlockItem) return true;
        return original.call(instance, interactor, blockPos);
    }


}
