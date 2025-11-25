package org.wallentines.creativeplots.mixin;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.wallentines.creativeplots.*;
import org.wallentines.databridge.api.ServerStateObjects;
import org.wallentines.mdcfg.Tuples;
import org.wallentines.midnightlib.math.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
@Implements(@Interface(iface=Plotworld.class, prefix="creativeplots_pw$"))
public abstract class MixinServerLevel {

    @Nullable
    @Unique
    private PlotStorage creativeplots$storage;

    @Nullable
    @Unique
    private PlotworldGenerator creativeplots$generator;

    @Nullable
    @Unique
    private CreativePlots creativeplots$instance;

    @Nullable
    @Unique
    private Set<UUID> creativeplots$admins;

    @Inject(method="<init>", at=@At("TAIL"))
    private void onInit(MinecraftServer minecraftServer, Executor executor, LevelStorageSource.LevelStorageAccess access,
                        ServerLevelData levelData, ResourceKey<Level> resourceKey, LevelStem levelStem,
                        boolean debug, long seed, List<CustomSpawner> list,
                        boolean tickTime, RandomSequences randomSequences, CallbackInfo ci) {

        if(!(levelStem.generator() instanceof PlotworldGenerator gen)) return;

        creativeplots$instance = ServerStateObjects.getStateObject(minecraftServer, CreativePlots.class, ResourceLocation.tryBuild("creativeplots", "instance")).orElseThrow();
        creativeplots$admins = new HashSet<>();

        ServerLevel lvl = (ServerLevel) (Object) this;

        creativeplots$storage = new PlotStorage((Plotworld) lvl, access.getDimensionPath(resourceKey).resolve("plots.json"));
        creativeplots$storage.load();

        creativeplots$generator = gen;
    }

    @Inject(method="save", at=@At("TAIL"))
    private void onSave(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
        if(creativeplots$storage == null) return;
        creativeplots$storage.save();
    }

    @Inject(method="addPlayer", at=@At("TAIL"))
    private void onJoin(ServerPlayer serverPlayer, CallbackInfo ci) {
        if(Permissions.check(serverPlayer, "creativeplots.interact_anywhere", 2)) {
            creativeplots$admins.add(serverPlayer.getUUID());
        }
    }

    @Inject(method="mayInteract", at=@At("RETURN"), cancellable = true)
    private void onMayInteract(Entity entity, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {

        if(!cir.getReturnValue()
                || creativeplots$generator == null
                || creativeplots$storage == null
                || creativeplots$admins.contains(entity.getUUID()))
            return;

        PlotMap map = creativeplots$generator.roadSettings().plotMap();
        Vec2i plotPos = map.getPlotPosition(blockPos.getX(), blockPos.getZ());
        if(plotPos == null) {
            cir.setReturnValue(false);
            return;
        }

        Plot plot = creativeplots$storage.getPlot(plotPos);
        if(plot == null || !plot.contains(new Vec3i(blockPos.getX(), blockPos.getY(), blockPos.getZ()))) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(plot.mayModify(entity.getUUID()));
    }



    public void creativeplots_pw$playerMoved(ServerPlayer player, BlockPos oldPos, BlockPos newPos) {
        if(creativeplots$generator == null || creativeplots$storage == null) return;

        PlotMap map = creativeplots$generator.roadSettings().plotMap();
        Vec2i plotPos = map.getPlotPosition(newPos.getX(), newPos.getZ());
        if(plotPos == null) return;

        // Check if they crossed the border
        Plot plot = creativeplots$storage.getPlot(plotPos);
        if(plot == null) {
            if(map.getAt(newPos.getX(), newPos.getZ()) == PlotMap.BlockType.PLOT
                && map.getAt(oldPos.getX(), oldPos.getZ()) != PlotMap.BlockType.PLOT) {

                player.connection.send(new ClientboundSetTitleTextPacket(creativeplots$instance.getLangManager().getMessage("title.unclaimed", player)));
                player.connection.send(new ClientboundSetSubtitleTextPacket(creativeplots$instance.getLangManager().getMessage("title.claim", player)));
                player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 5));
            }
        } else {
            if(!plot.contains(new Vec3i(oldPos.getX(), oldPos.getY(), oldPos.getZ())) &&
                plot.contains(new Vec3i(newPos.getX(), newPos.getY(), newPos.getZ()))) {

                plot.sendTitle(player, creativeplots$instance.getLangManager());
            }

        }
    }

    public PlotMap creativeplots_pw$getPlotMap() {
        if(creativeplots$generator == null || creativeplots$storage == null) return null;
        return creativeplots$generator.roadSettings().plotMap();
    }

    // TODO: Make this work with merged plots
    @NotNull
    public Tuples.T2<PlotMap.BlockType, Plot> creativeplots_pw$getBlockInfo(int x, int z) {
        if(creativeplots$generator == null || creativeplots$storage == null) return new Tuples.T2<>(null, null);

        PlotMap.BlockType bt = creativeplots$generator.roadSettings().plotMap().getAt(x, z);
        Vec2i pos = creativeplots$generator.roadSettings().plotMap().getPlotPosition(x, z);
        if(pos == null) {
            return new Tuples.T2<>(bt, null);
        };
        Plot plot = creativeplots$storage.getPlot(pos);
        return new Tuples.T2<>(bt, plot);
    }

    public Plot creativeplots_pw$getPlotAt(Vec2i plotPos) {
        if(creativeplots$generator == null || creativeplots$storage == null) return null;
        return creativeplots$storage.getPlot(plotPos);
    }

    public Plot creativeplots_pw$claimPlot(ServerPlayer player, Vec2i plotPos) {

        if(creativeplots$generator == null || creativeplots$storage == null) return null;
        if(creativeplots$storage.getPlot(plotPos) != null) {
            return null;
        }

        ServerLevel self = (ServerLevel) (Object) this;

        PlotMap map = creativeplots$generator.roadSettings().plotMap();
        CuboidRegion reg = map.getDefaultPlotRegion(plotPos, self.getMinY(), self.getHeight());
        if(!reg.isWithin(new Vec3d(player.getX(), player.getY(), player.getZ()))) {
            return null;
        }

        Plot plot = new Plot(
                (Plotworld) self,
                plotPos,
                Set.of(plotPos),
                List.of(reg),
                Color.fromRGBI(6),
                plotPos.toString(),
                player.getUUID(),
                player.getGameProfile().name(),
                Set.of());

        creativeplots$generator.generateBorder(self, plotPos, true);
        creativeplots$storage.addPlot(plot);
        plot.sendTitle(player, creativeplots$instance.getLangManager());

        return plot;
    }


    public void creativeplots_pw$clearPlot(Vec2i plotPos) {
        if(creativeplots$generator == null || creativeplots$storage == null) return;

        ServerLevel self = (ServerLevel) (Object) this;
        creativeplots$generator.clearPlot(self, plotPos);
    }


    public void creativeplots_pw$deletePlot(Vec2i plotPos) {
        if(creativeplots$generator == null || creativeplots$storage == null) return;

        ServerLevel self = (ServerLevel) (Object) this;

        creativeplots$generator.clearPlot(self, plotPos);
        creativeplots$generator.generateBorder(self, plotPos, false);
        creativeplots$storage.removePlot(plotPos);
    }


    public boolean creativeplots_pw$isAdmin(UUID uuid) {
        if(creativeplots$generator == null || creativeplots$storage == null) return false;
        return creativeplots$admins.contains(uuid);
    }


    public void creativeplots_pw$setAdmin(UUID uuid, boolean admin) {
        if(creativeplots$generator == null || creativeplots$storage == null) return;
        if(admin) {
            creativeplots$admins.add(uuid);
        } else {
            creativeplots$admins.remove(uuid);
        }
    }
}
