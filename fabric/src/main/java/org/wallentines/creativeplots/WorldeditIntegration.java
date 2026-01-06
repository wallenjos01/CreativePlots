package org.wallentines.creativeplots;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.AbstractPlayerActor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.biome.BiomeType;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.sk89q.worldedit.fabric.FabricWorld;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import org.wallentines.midnightlib.math.Vec2i;
import org.wallentines.midnightlib.math.Vec3i;

import java.lang.reflect.Field;
import java.util.Objects;

public class WorldeditIntegration {

    private static class PlotExtent extends AbstractDelegateExtent {

        static final BaseBlock AIR = Objects.requireNonNull(BlockTypes.AIR).getDefaultState().toBaseBlock();
        static final BlockState AIR_STATE = BlockTypes.AIR.getDefaultState();

        private final Plot plot;

        protected PlotExtent(Extent extent, Plot plot) {
            super(extent);
            this.plot = plot;
        }

        private boolean blockWithin(int x, int y, int z) {
            Vec3i pos = new Vec3i(x, y, z);
            return plot.contains(pos);
        }

        @Override
        public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block)
                throws WorldEditException {
            return blockWithin(location.x(), location.y(), location.z()) && super.setBlock(location, block);
        }

        @Override
        public @Nullable Entity createEntity(Location location, BaseEntity entity) {
            if (blockWithin(location.getBlockX(), location.getBlockY(), location.getBlockZ())) {
                return super.createEntity(location, entity);
            }
            return null;
        }

        @Override
        public boolean setBiome(BlockVector3 position, BiomeType biome) {
            return blockWithin(position.x(), position.y(), position.z()) && super.setBiome(position, biome);
        }

        @Override
        public BlockState getBlock(BlockVector3 position) {
            if (blockWithin(position.x(), position.y(), position.z())) {
                return super.getBlock(position);
            }
            return AIR_STATE;
        }

        @Override
        public BaseBlock getFullBlock(BlockVector3 position) {
            if (blockWithin(position.x(), position.y(), position.z())) {
                return super.getFullBlock(position);
            }
            return AIR;
        }
    }

    public static void register() {

        //Field field;
        // Field baseField;
        // try {
        //     Class<?> proxyClazz = Class.forName("com.sk89q.worldedit.extension.platform.PlayerProxy");
        //     baseField = proxyClazz.getDeclaredField("basePlayer");
        //     baseField.setAccessible(true);
        //
        //     Class<?> clazz = Class.forName("com.sk89q.worldedit.fabric.FabricPlayer");
        //     field = clazz.getDeclaredField("player");
        //     field.setAccessible(true);
        // } catch (ReflectiveOperationException ex) {
        //     throw new RuntimeException(ex);
        // }

        WorldEdit.getInstance().getEventBus().register(new Object() {

            @Subscribe
            public void sessionCallback(EditSessionEvent event) {

                if (event.getActor() == null || !event.getActor().isPlayer()) {
                    return;
                }

                AbstractPlayerActor actor = (AbstractPlayerActor) event.getActor();

                // ServerPlayer spl;
                // try {
                //     spl = (ServerPlayer) field.get(baseField.get(actor));
                // } catch (ReflectiveOperationException ex) {
                //     throw new RuntimeException(ex);
                // }

                FabricWorld world = (FabricWorld) actor.getWorld();

                Plotworld pw = (Plotworld) world.getWorld();
                if (pw.getPlotMap() == null) {
                    return;
                }
                if (pw.isAdmin(actor.getUniqueId()))
                    return;

                Location location = actor.getLocation();
                Vec2i plotPos = pw.getPlotMap().getPlotPosition(location.getBlockX(), location.getBlockZ());
                if (plotPos == null) {
                    event.setExtent(new NullExtent());
                    return;
                }

                Plot p = pw.getPlotAt(plotPos);
                if (p == null || !p.mayModify(actor.getUniqueId())) {
                    event.setExtent(new NullExtent());
                    return;
                }

                event.setExtent(new PlotExtent(event.getExtent(), p));
            }

        });

    }

}
