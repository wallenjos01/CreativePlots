package org.wallentines.creativeplots;

import org.jetbrains.annotations.Nullable;
import org.wallentines.mdcfg.ConfigObject;
import org.wallentines.mdcfg.ConfigSection;
import org.wallentines.mdcfg.codec.FileWrapper;
import org.wallentines.mdcfg.codec.JSONCodec;
import org.wallentines.mdcfg.serializer.ConfigContext;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.math.Vec2i;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class PlotStorage {

    private final Serializer<Plot> plotSerializer;
    private final FileWrapper<ConfigObject> file;
    private final Map<Vec2i, Plot> plots;

    public PlotStorage(Plotworld world, Path file) {
        this.plotSerializer = Plot.serializer(world);
        this.file = new FileWrapper<>(ConfigContext.INSTANCE, JSONCodec.fileCodec(), file, StandardCharsets.UTF_8, new ConfigSection());
        this.plots = new HashMap<>();
    }

    public void load() {

        file.load();
        Map<Vec2i, Plot> res = serializer.deserialize(ConfigContext.INSTANCE, file.getRoot()).getOrThrow();

        this.plots.clear();
        this.plots.putAll(res);
    }

    public void save() {
        SerializeResult<ConfigObject> obj = serializer.serialize(ConfigContext.INSTANCE, plots);
        file.setRoot(obj.getOrThrow());
        file.save();
    }

    @Nullable
    public Plot getPlot(Vec2i pos) {
        return plots.get(pos);
    }

    public void removePlot(Vec2i pos) {

        Plot plot = plots.get(pos);
        if(plot == null) return;

        for(Vec2i vec : plot.positions()) {
            plots.remove(vec);
        }
    }

    public void addPlot(Plot plot) {
        for(Vec2i vec : plot.positions()) {
            plots.put(vec, plot);
        }
    }

    private final Serializer<Map<Vec2i, Plot>> serializer = new Serializer<Map<Vec2i, Plot>>() {
        @Override
        public <O> SerializeResult<O> serialize(SerializeContext<O> context, Map<Vec2i, Plot> value) {
            Map<String, O> out = new HashMap<>();
            for(Map.Entry<Vec2i, Plot> e : value.entrySet()) {
                String posKey = e.getKey().toString();
                if(e.getValue().rootPosition().equals(e.getKey())) {

                    SerializeResult<O> res = plotSerializer.serialize(context, e.getValue());
                    if(!res.isComplete()) return res;

                    out.put(posKey, res.getOrNull());
                } else {
                    out.put(posKey, context.toString(posKey));
                }
            }
            return SerializeResult.success(context.toMap(out));
        }

        @Override
        public <O> SerializeResult<Map<Vec2i, Plot>> deserialize(SerializeContext<O> context, O value) {
            return context.asMap(value).map(plots -> {

                Map<Vec2i, Plot> out = new HashMap<>();

                for(Map.Entry<String, O> ent : plots.entrySet()) {

                    Vec2i pos = Vec2i.parse(ent.getKey());
                    if(pos == null) {
                        return SerializeResult.failure("Invalid plot position: " + ent.getKey());
                    }

                    if(context.isMap(ent.getValue())) {
                        SerializeResult<Plot> plot = plotSerializer.deserialize(context, ent.getValue());
                        if(!plot.isComplete()) {
                            return SerializeResult.failure("Unable to deserialize a plot at " + pos + "!", plot.getError());
                        }

                        out.put(pos, plot.getOrNull());
                    } else if(context.isString(ent.getValue())) {

                        SerializeResult<String> str = context.asString(ent.getValue());
                        if(!str.isComplete()) {
                            return SerializeResult.failure("Unable to deserialize a plot at " + pos + "!", str.getError());
                        }

                        String refStr = str.getOrNull();
                        Vec2i ref = Vec2i.parse(refStr);
                        if(ref == null || !out.containsKey(ref)) {
                            return SerializeResult.failure("Invalid plot reference: " + ent.getKey() + " -> " + refStr);
                        }

                        out.put(pos, out.get(ref));

                    } else {

                        return SerializeResult.failure("Unable to deserialize a plot at " + pos + "! Expected a map or a string!");
                    }
                }

                return SerializeResult.success(out);
            });
        }
    };

}
