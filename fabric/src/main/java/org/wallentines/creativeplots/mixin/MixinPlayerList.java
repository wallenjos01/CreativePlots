package org.wallentines.creativeplots.mixin;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.wallentines.creativeplots.Plotworld;

@Mixin(PlayerList.class)
public class MixinPlayerList {

    @Inject(method="sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;I)V", at=@At("TAIL"))
    private void onPermissionUpdate(ServerPlayer serverPlayer, int i, CallbackInfo ci) {
        Plotworld pw = (Plotworld) serverPlayer.serverLevel();
        pw.setAdmin(serverPlayer.getUUID(), Permissions.check(serverPlayer, "creativeplots.plotworld", 2));
    }

}
