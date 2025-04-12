package org.wallentines.creativeplots;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.SetBlockCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wallentines.midnightlib.math.Vec2i;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlotworldGenerator extends ChunkGenerator {

    public static final MapCodec<PlotworldGenerator> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            FlatLevelGeneratorSettings.CODEC.fieldOf("settings").forGetter(PlotworldGenerator::layerSettings),
            RoadGeneratorSettings.CODEC.fieldOf("roads").forGetter(PlotworldGenerator::roadSettings),
            Codec.BOOL.fieldOf("decorate").orElse(false).forGetter(PlotworldGenerator::decorate)
    ).apply(instance, instance.stable(PlotworldGenerator::new)));
    private static final Logger log = LoggerFactory.getLogger(PlotworldGenerator.class);

    private final FlatLevelGeneratorSettings layerSettings;
    private final RoadGeneratorSettings roadSettings;
    private final boolean decorate;

    public PlotworldGenerator(FlatLevelGeneratorSettings layerSettings, RoadGeneratorSettings roadSettings, boolean decorate) {
        super(new FixedBiomeSource(layerSettings.getBiome()));
        this.layerSettings = layerSettings;
        this.roadSettings = roadSettings;
        this.decorate = decorate;
    }

    public FlatLevelGeneratorSettings layerSettings() {
        return layerSettings;
    }

    public RoadGeneratorSettings roadSettings() {
        return roadSettings;
    }

    public boolean decorate() {
        return decorate;
    }

    @Override
    protected @NotNull MapCodec<PlotworldGenerator> codec() {
        return CODEC;
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor levelHeightAccessor) {
        return levelHeightAccessor.getMinY() + Math.min(levelHeightAccessor.getHeight(), this.layerSettings.getLayers().size());
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {
        List<BlockState> layers = this.layerSettings.getLayers();

        for(int y = Math.min(layers.size() - 1, levelHeightAccessor.getMaxY()); y >= 0; --y) {

            BlockState blockState = layers.get(y);
            if (blockState != null && types.isOpaque().test(blockState)) {
                return levelHeightAccessor.getMinY() + y + 1;
            }
        }

        return levelHeightAccessor.getMinY();
    }

    @Override
    public @NotNull CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunkAccess) {

        List<BlockState> layers = this.layerSettings.getLayers();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        Heightmap oceanHeight = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surfaceHeight = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        int maxY = Math.min(chunkAccess.getHeight(), layers.size()) - 1;
        for(int y = 0; y < maxY; ++y) {
            BlockState state = layers.get(y);
            if (state != null) {
                int realY = chunkAccess.getMinY() + y;

                for(int x = 0; x < 16; ++x) {
                    for(int z = 0; z < 16; ++z) {
                        chunkAccess.setBlockState(pos.set(x, realY, z), state, 816);
                        oceanHeight.update(x, realY, z, state);
                        surfaceHeight.update(x, realY, z, state);
                    }
                }
            }
        }

        BlockState plotState = layers.get(maxY);
        BlockState roadState = roadSettings.roadBlock().defaultBlockState();
        BlockState borderState = roadSettings.borderBlock().defaultBlockState();

        int realY = chunkAccess.getMinY() + maxY;

        int offX = chunkAccess.getPos().x * 16;
        int offZ = chunkAccess.getPos().z * 16;

        for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {

                PlotMap.BlockType type = roadSettings.plotMap().getAt(offX + x, offZ + z);

                if(type == PlotMap.BlockType.PLOT && plotState != null) {
                    chunkAccess.setBlockState(pos.set(x, realY, z), plotState, 816);
                    oceanHeight.update(x, realY, z, plotState);
                    surfaceHeight.update(x, realY, z, plotState);
                } else {

                    chunkAccess.setBlockState(pos.set(x, realY, z), roadState, 816);
                    int height = realY;
                    if(type == PlotMap.BlockType.BORDER) {
                        height++;
                        chunkAccess.setBlockState(pos.set(x, realY + 1, z), borderState, 816);
                    }

                    oceanHeight.update(x, height, z, plotState);
                    surfaceHeight.update(x, height, z, plotState);
                }

            }
        }

        return CompletableFuture.completedFuture(chunkAccess);
    }


    public int getMinY() {
        return 0;
    }

    public int getGenDepth() {
        return 384;
    }

    public int getSeaLevel() {
        return -63;
    }

    public int getPlotY(LevelHeightAccessor accessor) {
        int maxY = Math.min(accessor.getHeight(), layerSettings.getLayers().size()) - 1;
        return accessor.getMinY() + maxY;
    }

    public int getBorderY(LevelHeightAccessor accessor) {
        return getPlotY(accessor) + 1;
    }

    public void generateBorder(ServerLevel level, Vec2i plotPos, boolean claimed) {

        PlotMap map = roadSettings.plotMap();
        Vec2i start = map.getPlotStart(plotPos);
        int height = getBorderY(level);
        int plotSize = map.plotSize();

        Block borderBlock = claimed ? roadSettings().claimedBorderBlock() : roadSettings().borderBlock();
        BlockState borderState = borderBlock.defaultBlockState();

        // Set
        BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos(start.getX(), height, start.getY());
        for(int x = 0 ; x < plotSize + 2 ; x++) {
            mbp.setX(start.getX() - 1 + x);
            mbp.setZ(start.getY() - 1);
            level.setBlock(mbp, borderState, 2);

            mbp.setZ(start.getY() + plotSize);
            level.setBlock(mbp, borderState, 2);
        }

        for(int z = 0 ; z < plotSize ; z++) {
            mbp.setZ(start.getY() + z);
            mbp.setX(start.getX() - 1);
            level.setBlock(mbp, borderState, 2);

            mbp.setX(start.getX() + plotSize);
            level.setBlock(mbp, borderState, 2);
        }

        // Update
//        for(int x = 0 ; x < plotSize + 2 ; x++) {
//            level.updateNeighborsAt(new BlockPos(start.getX() - 1 + x, height, start.getY() - 1), borderBlock);
//            level.updateNeighborsAt(new BlockPos(start.getX() - 1 + x, height, start.getY() + plotSize), borderBlock);
//        }
//
//        for(int z = 0 ; z < plotSize ; z++) {
//            level.blockUpdated(new BlockPos(start.getX() - 1, height, start.getY() + z), borderBlock);
//            level.blockUpdated(new BlockPos(start.getX() + plotSize, height, start.getY() + z), borderBlock);
//        }
//
    }

    // TODO: Clear merged plots
    public void clearPlot(ServerLevel level, Vec2i plotPos) {

        PlotMap map = roadSettings.plotMap();
        Vec2i start = map.getPlotStart(plotPos);
        int plotSize = map.plotSize();
        List<BlockState> layers = this.layerSettings.getLayers();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        // Set
        int maxY = Math.min(level.getHeight(), layers.size());
        log.info("Filling layers from 0 to {}", maxY);
        for(int y = 0; y < maxY; ++y) {
            BlockState state = layers.get(y);
            if (state != null) {
                int realY = level.getMinY() + y;
                for(int x = 0 ; x < plotSize ; x++) {
                    for(int z = 0 ; z < plotSize ; z++) {
                        level.setBlock(pos.set(x + start.getX(), realY, z + start.getY()), state, 2);
                    }
                }
            }
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        for(int y = maxY ; y < level.getHeight() ; y++) {
            int realY = level.getMinY() + y;
            for(int x = 0 ; x < plotSize ; x++) {
                for(int z = 0 ; z < plotSize ; z++) {
                    level.setBlock(pos.set(x + start.getX(), realY, z + start.getY()), air, 2);
                }
            }
        }

        // Update
//        for(int y = 0; y < maxY; ++y) {
//            BlockState state = layers.get(y);
//            if (state != null) {
//                Block block = state.getBlock();
//                int realY = level.getMinY() + y;
//                for(int x = 0 ; x < plotSize ; x++) {
//                    for(int z = 0 ; z < plotSize ; z++) {
//                        level.blockUpdated(pos.set(x + start.getX(), realY, z + start.getY()), block);
//                    }
//                }
//            }
//        }
//        for(int y = maxY ; y < level.getHeight() ; y++) {
//            int realY = level.getMinY() + y;
//            for(int x = 0 ; x < plotSize ; x++) {
//                for(int z = 0 ; z < plotSize ; z++) {
//                    level.blockUpdated(pos.set(x + start.getX(), realY, z + start.getY()), Blocks.AIR);
//                }
//            }
//        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel worldGenLevel, ChunkAccess chunkAccess, StructureManager structureManager) {
        if(decorate) {
            super.applyBiomeDecoration(worldGenLevel, chunkAccess, structureManager);
        }
    }

    @Override
    public @NotNull NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor accessor, RandomState random) {
        return new NoiseColumn(
                accessor.getMinY(),
                this.layerSettings
                        .getLayers()
                        .stream()
                        .limit(accessor.getHeight())
                        .map((state) -> state == null
                                ? Blocks.AIR.defaultBlockState()
                                : state).toArray(BlockState[]::new));

    }

    @Override
    public void addDebugScreenInfo(List<String> entries, RandomState random, BlockPos pos) { }

    @Override
    public void applyCarvers(WorldGenRegion worldGenRegion, long l, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunkAccess) { }

    @Override
    public void buildSurface(WorldGenRegion worldGenRegion, StructureManager structureManager, RandomState randomState, ChunkAccess chunkAccess) { }

    @Override
    public void spawnOriginalMobs(WorldGenRegion worldGenRegion) { }



}
