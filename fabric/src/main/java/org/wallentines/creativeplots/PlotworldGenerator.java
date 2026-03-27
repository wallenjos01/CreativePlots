package org.wallentines.creativeplots;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import org.jetbrains.annotations.NotNull;
import org.wallentines.creativeplots.PlotMap.BlockType;
import org.wallentines.midnightlib.math.Vec2i;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class PlotworldGenerator extends ChunkGenerator {

    public static final MapCodec<PlotworldGenerator> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
            PlotworldSettings.CODEC.fieldOf("settings").forGetter(PlotworldGenerator::plotworldSettings),
            RoadGeneratorSettings.CODEC.fieldOf("roads").forGetter(PlotworldGenerator::roadSettings),
            Codec.BOOL.fieldOf("decorate").orElse(false).forGetter(PlotworldGenerator::decorate)
    ).apply(instance, instance.stable(PlotworldGenerator::new)));

    private final PlotworldSettings plotworldSettings;
    private final RoadGeneratorSettings roadSettings;
    private final boolean decorate;

    public PlotworldGenerator(PlotworldSettings plotworldSettings, RoadGeneratorSettings roadSettings, boolean decorate) {
        super(plotworldSettings.biomeSource());
        this.plotworldSettings = plotworldSettings;
        this.roadSettings = roadSettings;
        this.decorate = decorate;
    }

    public PlotworldSettings plotworldSettings() {
        return plotworldSettings;
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

        BlockType type = roadSettings.plotMap().getAt(0,0);
        List<FlatLayerInfo> layers = switch(type) {
            case BlockType.PLOT -> plotworldSettings.plotLayers();
            case BlockType.ROAD -> plotworldSettings.roadLayers();
            case BlockType.BORDER -> plotworldSettings.borderLayers();
        };

        return levelHeightAccessor.getMinY() + Math.min(levelHeightAccessor.getHeight(), (int) stateColumn(layers).count());
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types types, LevelHeightAccessor levelHeightAccessor, RandomState randomState) {


        BlockType type = roadSettings.plotMap().getAt(x, z);

        List<FlatLayerInfo> layers = switch(type) {
            case BlockType.PLOT -> plotworldSettings.plotLayers();
            case BlockType.ROAD -> plotworldSettings.roadLayers();
            case BlockType.BORDER -> plotworldSettings.borderLayers();
        };

        int trueHeight = 0;
        int opaqueHeight = 0;
        for(FlatLayerInfo layer : layers) {
            trueHeight += layer.getHeight();
            if(types.isOpaque().test(layer.getBlockState())) {
                opaqueHeight = trueHeight;
            }
        }

        return levelHeightAccessor.getMinY() + opaqueHeight + 1;
    }

    @Override
    public @NotNull CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structureManager, ChunkAccess chunkAccess) {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(0,0,0);
        Heightmap oceanHeight = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surfaceHeight = chunkAccess.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
 
        int offX = chunkAccess.getPos().x * 16;
        int offZ = chunkAccess.getPos().z * 16;

        for(int x = 0 ; x < 16 ; x++) {
            for(int z = 0 ; z < 16 ; z++) {
                
                BlockType type = roadSettings.plotMap().getAt(x + offX, z + offZ);
                pos.setX(x);
                pos.setZ(z);

                List<FlatLayerInfo> layers = switch(type) {
                    case BlockType.PLOT -> plotworldSettings.plotLayers();
                    case BlockType.ROAD -> plotworldSettings.roadLayers();
                    case BlockType.BORDER -> plotworldSettings.borderLayers();
                };

                int height = 0;
                for(FlatLayerInfo layer : layers) {
                    BlockState state = layer.getBlockState();
                    for(int y = 0 ; y < layer.getHeight() ; y++) {

                        pos.setY(y + chunkAccess.getMinY() + height);
                        chunkAccess.setBlockState(pos, state);

                        oceanHeight.update(pos.getX(), pos.getY(), pos.getZ(), state);
                        surfaceHeight.update(pos.getX(), pos.getY(), pos.getZ(), state);
                    }
                    height += layer.getHeight();
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

    public void generateBorder(ServerLevel level, Vec2i plotPos, boolean claimed) {

        PlotMap map = roadSettings.plotMap();
        Vec2i start = map.getPlotStart(plotPos);
        int plotSize = map.plotSize();
        int borderSize = map.borderSize();

        MutableBlockPos lower = new MutableBlockPos();
        MutableBlockPos upper = new MutableBlockPos();

        List<FlatLayerInfo> layers = claimed ? plotworldSettings.claimedBorderLayers() : plotworldSettings.borderLayers();
            
        int realHeight = level.getMinY();
        for(FlatLayerInfo layer : layers) {

            BlockState state = layer.getBlockState();

            for(int y = 0 ; y < layer.getHeight() ; y++) {

                lower.setY(y + realHeight);
                upper.setY(lower.getY());

                for(int x = 0 ; x < plotSize + (2 * borderSize) ; x++) {

                    lower.setX(x + start.getX() - borderSize);
                    upper.setX(lower.getX());

                    for(int z = 0 ; z < borderSize ; z++) { 

                        lower.setZ(z + start.getY() - borderSize);
                        upper.setZ(z + start.getY() + plotSize);

                        level.setBlock(lower, state, 2);
                        level.setBlock(upper, state, 2);
                    }
                }

                for(int z = 0 ; z < plotSize ; z++) {

                    lower.setZ(z + start.getY() - borderSize + 1);
                    upper.setZ(lower.getZ());

                    for(int x = 0 ; x < borderSize ; x++) {

                        lower.setX(x + start.getX() - borderSize);
                        upper.setX(x + start.getX() + plotSize);

                        level.setBlock(lower, state, 2);
                        level.setBlock(upper, state, 2);
                    }
                }
            }

            realHeight += layer.getHeight();
        }
    }

    // TODO: Clear merged plots
    public void clearPlot(ServerLevel level, Vec2i plotPos) {

        PlotMap map = roadSettings.plotMap();
        Vec2i start = map.getPlotStart(plotPos);
        int plotSize = map.plotSize();
        List<FlatLayerInfo> layers = this.plotworldSettings.plotLayers();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int realHeight = level.getMinY();
        for(FlatLayerInfo layer : layers) {
            for(int y = 0 ; y < layer.getHeight() ; y++) {
                pos.setY(y + realHeight);
                
                for(int x = 0 ; x < plotSize ; x++) {
                    pos.setX(x + start.getX());
                    for(int z = 0 ; z < plotSize ; z++) {
                        pos.setZ(z + start.getY());

                        level.setBlock(pos, layer.getBlockState(), 2);
                    }
                }

            }

            realHeight += layer.getHeight();
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        for(int y = realHeight ; y < level.getHeight() ; y++) {
            for(int x = 0 ; x < plotSize ; x++) {
                for(int z = 0 ; z < plotSize ; z++) {
                    level.setBlock(pos.set(x + start.getX(), y, z + start.getY()), air, 2);
                }
            }
        }
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel worldGenLevel, ChunkAccess chunkAccess, StructureManager structureManager) {
        if(decorate) {
            super.applyBiomeDecoration(worldGenLevel, chunkAccess, structureManager);
        }
    }

    @Override
    public @NotNull NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor accessor, RandomState random) {

        PlotMap.BlockType type = roadSettings.plotMap().getAt(x, z);

        List<FlatLayerInfo> layers = switch(type) {
            case BlockType.PLOT -> plotworldSettings.plotLayers();
            case BlockType.ROAD -> plotworldSettings.roadLayers();
            case BlockType.BORDER -> plotworldSettings.borderLayers();
        };

        return new NoiseColumn(
                accessor.getMinY(),
                stateColumn(layers)
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


    private Stream<BlockState> stateColumn(List<FlatLayerInfo> layers) {
        return layers.stream().flatMap(layer -> Stream.generate(() -> layer.getBlockState()).limit(layer.getHeight()));

    }

}
