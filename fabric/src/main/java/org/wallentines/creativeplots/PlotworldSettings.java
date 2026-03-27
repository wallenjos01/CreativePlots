package org.wallentines.creativeplots;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public record PlotworldSettings(BiomeSource biomeSource, 
                                List<FlatLayerInfo> plotLayers, 
                                List<FlatLayerInfo> roadLayers, 
                                List<FlatLayerInfo> borderLayers, 
                                List<FlatLayerInfo> claimedBorderLayers, 
                                Optional<HolderSet<StructureSet>> structureOverrides,
                                boolean lakes, 
                                boolean features) {

    public static final Codec<PlotworldSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BiomeSource.CODEC.fieldOf("biome_source").forGetter(PlotworldSettings::biomeSource),
        FlatLayerInfo.CODEC.listOf().fieldOf("plot_layers").forGetter(PlotworldSettings::plotLayers),
        FlatLayerInfo.CODEC.listOf().fieldOf("road_layers").forGetter(PlotworldSettings::roadLayers),
        FlatLayerInfo.CODEC.listOf().fieldOf("border_layers").forGetter(PlotworldSettings::borderLayers),
        FlatLayerInfo.CODEC.listOf().fieldOf("claimed_border_layers").forGetter(PlotworldSettings::claimedBorderLayers),
        RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET).lenientOptionalFieldOf("structure_overrides").forGetter(PlotworldSettings::structureOverrides),
        Codec.BOOL.optionalFieldOf("lakes", false).forGetter(PlotworldSettings::lakes),
        Codec.BOOL.optionalFieldOf("features", false).forGetter(PlotworldSettings::features)
    ).apply(instance, PlotworldSettings::new));

}
