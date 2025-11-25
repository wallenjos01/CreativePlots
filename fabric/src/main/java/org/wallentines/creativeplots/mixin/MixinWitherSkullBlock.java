package org.wallentines.creativeplots.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WitherSkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.wallentines.creativeplots.PlotMap;
import org.wallentines.creativeplots.Plotworld;

@Mixin(WitherSkullBlock.class)
public class MixinWitherSkullBlock {

    @WrapOperation(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;)Lnet/minecraft/world/entity/Entity;"))
    private static <T extends Entity> T onWitherSummon(EntityType<T> instance, Level level, EntitySpawnReason reason, Operation<T> original) {

        if(level.isClientSide()) return original.call(instance, level, reason);

        ServerLevel lvl = (ServerLevel) level;
        PlotMap map = ((Plotworld) lvl).getPlotMap();
        if(map == null) return original.call(instance, level, reason);

        return null;
    }

}
