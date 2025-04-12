package org.wallentines.creativeplots;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record RoadGeneratorSettings(PlotMap plotMap, Block roadBlock, Block borderBlock, Block claimedBorderBlock) {

    public RoadGeneratorSettings(int plotSize, int roadSize, Block roadBlock, Block borderBlock, Block claimedBorderBlock) {
        this(new PlotMap(plotSize, roadSize), roadBlock, borderBlock, claimedBorderBlock);
    }

    public int plotSize() {
        return plotMap.plotSize();
    }

    public int roadSize() {
        return plotMap.roadSize();
    }

    public static final Codec<RoadGeneratorSettings> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Codec.intRange(1, 1024).fieldOf("plot_size").forGetter(RoadGeneratorSettings::plotSize),
            Codec.intRange(1, 1024).fieldOf("road_size").forGetter(RoadGeneratorSettings::roadSize),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("road_block").orElse(Blocks.AIR).forGetter(RoadGeneratorSettings::roadBlock),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("border_block").orElse(Blocks.AIR).forGetter(RoadGeneratorSettings::borderBlock),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("claimed_border_block").orElse(Blocks.AIR).forGetter(RoadGeneratorSettings::claimedBorderBlock)
    ).apply(instance, RoadGeneratorSettings::new));

}
