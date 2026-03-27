package org.wallentines.creativeplots;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record RoadGeneratorSettings(PlotMap plotMap, int plotFloor, int plotHeight) {

    public RoadGeneratorSettings(int plotSize, int roadSize, int borderSize, int plotFloor, int plotHeight) {
        this(new PlotMap(plotSize, roadSize, borderSize), plotFloor, plotHeight);
    }

    public int plotSize() {
        return plotMap.plotSize();
    }

    public int roadSize() {
        return plotMap.roadSize();
    }

    public int borderSize() {
        return plotMap.borderSize();
    }

    public static final Codec<RoadGeneratorSettings> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            Codec.intRange(1, 1024).fieldOf("plot_size").forGetter(RoadGeneratorSettings::plotSize),
            Codec.intRange(0, 1024).fieldOf("road_size").forGetter(RoadGeneratorSettings::roadSize),
            Codec.intRange(0, 1024).fieldOf("border_size").forGetter(RoadGeneratorSettings::borderSize),
            Codec.intRange(0, 4096).optionalFieldOf("plot_floor", 0).forGetter(RoadGeneratorSettings::plotFloor),
            Codec.intRange(0, 4096).optionalFieldOf("plot_height", 4096).forGetter(RoadGeneratorSettings::plotHeight)
    ).apply(instance, RoadGeneratorSettings::new));

}
