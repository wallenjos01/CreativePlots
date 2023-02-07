package org.wallentines.creativeplots.common;

import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.api.event.PlotEnterEvent;
import org.wallentines.creativeplots.api.event.PlotLeaveEvent;
import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.SerializeContext;
import org.wallentines.mdcfg.serializer.SerializeResult;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightcore.api.MidnightCoreAPI;
import org.wallentines.midnightcore.api.player.Location;
import org.wallentines.midnightcore.api.server.MServer;
import org.wallentines.midnightcore.api.text.*;
import org.wallentines.midnightlib.event.Event;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.creativeplots.api.plot.*;

import java.util.*;
import java.util.regex.Pattern;

public class Plot implements IPlot {

    private static final Pattern lint = Pattern.compile("[a-z0-9_.\\-:;,]+");

    private final Set<PlotPos> positions;
    private final PlotPos rootPosition;
    private final IPlotWorld map;

    private final String id;

    private UUID owner;
    private MComponent friendlyName;

    private final Set<UUID> trusted;
    private final Set<UUID> denied;
    private final List<Region> area;

    // Create a default plot at a given position
    public Plot(IPlotWorld map, String id, PlotPos... pos) {

        this.map = map;

        if(pos.length == 0) {
            throw new IllegalStateException("Unable to create plot! No valid positions found!");
        }

        this.id = id;
        this.friendlyName = new MTextComponent(id).withStyle(new MStyle().withColor(Color.fromRGBI(6)));
        this.positions = new HashSet<>();
        this.rootPosition = pos[0];

        positions.addAll(Arrays.asList(pos));

        this.trusted = new HashSet<>();
        this.denied = new HashSet<>();
        this.area = new ArrayList<>();

        calculateArea();
    }

    public Plot(IPlotWorld map, String id, Collection<PlotPos> pos) {

        this.map = map;

        if(pos.size() == 0) {
            throw new IllegalStateException("Unable to create plot! No valid positions found!");
        }

        this.id = id;
        this.friendlyName = new MTextComponent(id).withStyle(new MStyle().withColor(Color.fromRGBI(6)));
        this.positions = new HashSet<>();
        this.rootPosition = pos.iterator().next();

        positions.addAll(pos);

        this.trusted = new HashSet<>();
        this.denied = new HashSet<>();
        this.area = new ArrayList<>();

        calculateArea();
    }

    public void register(IPlotRegistry reg) {
        for(PlotPos pos : positions) {
            reg.registerPlot(this, pos);
        }
    }


    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public MComponent getName() {
        return friendlyName;
    }

    @Override
    public boolean canEdit(MPlayer user) {
        UUID u = user.getUUID();
        return u.equals(owner) || trusted.contains(u) || user.hasPermission("creativeplots.editanywhere", 4);
    }

    @Override
    public Vec3d getTeleportLocation() {
        Vec3i blockPos = map.toLocation(rootPosition);
        return new Vec3d(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
    }

    @Override
    public boolean contains(Vec3i location) {

        for(Region reg : area) {
            if(reg.contains(location)) return true;
        }

        return false;
    }

    @Override
    public void setOwner(UUID user) {
        this.owner = user;
    }

    @Override
    public void setName(MComponent name) {
        this.friendlyName = name;
    }

    @Override
    public void merge(IPlot other) {

        positions.addAll(other.getPositions());
        calculateArea();
    }

    @Override
    public List<Region> getArea() {
        return area;
    }

    @Override
    public Set<PlotPos> getPositions() {
        return positions;
    }

    @Override
    public boolean isDenied(UUID u) {
        return denied.contains(u);
    }

    @Override
    public void trustPlayer(UUID u) {
        if(u.equals(owner) || trusted.contains(u)) return;

        trusted.add(u);
    }

    @Override
    public void untrustPlayer(UUID u) {
        trusted.remove(u);
    }

    @Override
    public void denyPlayer(UUID u) {
        if(u.equals(owner) || denied.contains(u)) return;

        denied.add(u);

        MServer srv = Objects.requireNonNull(MidnightCoreAPI.getRunningServer());
        MPlayer pl = srv.getPlayer(u);


        int offset = map.getRoadSize() / 2;
        if(map.getRoadSize() % 2 == 1) offset += 1;

        if(contains(pl.getLocation().getCoordinates().truncate())) {
            pl.teleport(new Location(pl.getLocation().getWorldId(), getTeleportLocation().add(new Vec3d(-1 * offset, 1, -1 * offset)), 0, 0));
        }
    }

    @Override
    public void undenyPlayer(UUID u) {
        denied.remove(u);
    }

    @Override
    public boolean hasOwnerPermissions(MPlayer player) {
        return player.getUUID().equals(owner) || player.hasPermission("creativeplots.ownereverywhere", 4);
    }

    @Override
    public MComponent getOwnerName() {

        return getOwnerName(null);
    }

    @Override
    public void onEnter(MPlayer player) {
        MComponent title = CreativePlotsAPI.getInstance().getLangProvider().getMessage("plot.title", player, player, this, map);
        MComponent subtitle = CreativePlotsAPI.getInstance().getLangProvider().getMessage("plot.subtitle", player, player, this, map);

        player.sendTitle(title, 20, 80, 20);
        player.sendSubtitle(subtitle, 20, 80, 20);

        Event.invoke(new PlotEnterEvent(player, this));
    }

    @Override
    public void onLeave(MPlayer player) {

        Event.invoke(new PlotLeaveEvent(player, this));
    }

    private MComponent getOwnerName(MPlayer pl) {

        if (owner == null) return CreativePlotsAPI.getInstance().getLangProvider().getMessage("plot.null_owner", pl);

        MServer server = Objects.requireNonNull(MidnightCoreAPI.getRunningServer());
        MPlayer player = server.getPlayer(owner);

        return player.getName();
    }

    private void calculateArea() {

        area.clear();

        if(positions.size() == 1) {
            area.add(rootPosition.getRegion(map));
            return;
        }

        int minX = rootPosition.getX();
        int minZ = rootPosition.getZ();
        int maxX = minX;
        int maxZ = minZ;

        for(PlotPos pos : positions) {
            if(pos.getX() < minX) minX = pos.getX();
            if(pos.getX() > maxX) maxX = pos.getX();
            if(pos.getZ() < minZ) minZ = pos.getZ();
            if(pos.getZ() > maxZ) maxZ = pos.getZ();
        }


        int xCount = maxX - minX + 1;
        int zCount = maxZ - minZ + 1;

        int[][] map = new int[xCount][zCount];

        for(int x = 0 ; x < xCount ; x++) {
            for(int z = 0 ; z < zCount ; z++) {
                PlotPos pos = new PlotPos(x + minX, z + minZ);
                map[x][z] = positions.contains(pos) ? 1 : 0;
            }
        }

        for(int x = 0 ; x < xCount ; x++) {
            for(int z = 0 ; z < zCount ; z++) {

                int state = map[x][z];

                if(state == 1) {

                    int ix = x;
                    int iz = z;

                    boolean[] canExpand = new boolean[] { true, true, true, true };
                    PlotPos lower = new PlotPos(x, z);
                    PlotPos higher = new PlotPos(x, z);

                    while(true) {

                        if(ix == 0 || map[ix - 1][higher.getZ()] == 0) canExpand[0] = false;
                        if(iz == 0 || map[higher.getX()][iz - 1] == 0) canExpand[1] = false;
                        if(ix + 1 == xCount || map[ix + 1][higher.getZ()] == 0) canExpand[2] = false;
                        if(iz + 1 == zCount || map[higher.getX()][iz + 1] == 0) canExpand[3] = false;

                        if(canExpand[0]) {
                            ix -= 1;
                            lower = lower.getAdjacent(PlotDirection.WEST);
                            continue;
                        }
                        if(canExpand[1]) {
                            iz -= 1;
                            lower = lower.getAdjacent(PlotDirection.NORTH);
                            continue;
                        }
                        if(canExpand[2]) {
                            ix += 1;
                            higher = higher.getAdjacent(PlotDirection.EAST);
                            continue;
                        }
                        if(canExpand[3]) {
                            iz += 1;
                            higher = higher.getAdjacent(PlotDirection.SOUTH);
                            continue;
                        }

                        for(int rx = lower.getX() ; rx < higher.getX() + 1; rx++) {
                            for(int rz = lower.getZ() ; rz < higher.getZ() + 1 ; rz++) {
                                map[rx][rz] = 2;
                            }
                        }

                        Region l = new PlotPos(lower.getX() + minX, lower.getZ() + minZ).getRegion(this.map);
                        Region h = new PlotPos(higher.getX() + minX, higher.getZ() + minZ).getRegion(this.map);
                        area.add(new Region(l.getLowerBound(), h.getUpperBound()));

                        break;
                    }
                }
            }
        }
    }

    @Override
    public Serializer<IPlot> serializer(IPlotWorld world) {
        return plotSerializer(world);
    }

    public static Serializer<IPlot> plotSerializer(IPlotWorld world) {
        return new Serializer<>() {
            final Serializer<Plot> internal = makeSerializer(world);
            @Override
            public <O> SerializeResult<O> serialize(SerializeContext<O> context, IPlot value) {
                return internal.serialize(context, (Plot) value);
            }

            @Override
            public <O> SerializeResult<IPlot> deserialize(SerializeContext<O> context, O value) {
                return internal.deserialize(context, value).flatMap(ip -> ip);
            }
        };
    }

    private static Serializer<Plot> makeSerializer(IPlotWorld map) {
        return ObjectSerializer.create(
                PlotPos.SERIALIZER.listOf().entry("positions", Plot::getPositions),
                Serializer.STRING.entry("id", Plot::getId),
                MComponent.SERIALIZER.entry("name", Plot::getName).optional(),
                Serializer.UUID.entry("owner", Plot::getOwner).optional(),
                Serializer.UUID.listOf().<Plot>entry("trusted", p -> p.trusted).optional(),
                Serializer.UUID.listOf().<Plot>entry("denied", p -> p.denied).optional(),
                (pos, id, name, owner, trusted, denied) -> {

                    if (!lint.matcher(id).matches()) {
                        throw new IllegalStateException("Unable to parse plot! Invalid ID!");
                    }
                    Plot out = new Plot(map, id, pos);

                    if (name != null) out.setName(name);
                    if (owner != null) out.setOwner(owner);
                    if (trusted != null) out.trusted.addAll(trusted);
                    if (denied != null) out.denied.addAll(denied);

                    return out;
                });
    }

    @Override
    public Integer getTimeOfDay() {
        return 6000;
    }

    @Override
    public boolean isRaining() {
        return false;
    }


    public static void registerPlaceholders(PlaceholderManager manager) {

        manager.getInlinePlaceholders().register("creativeplots_plot_id", PlaceholderSupplier.create(IPlot.class, IPlot::getId));
        manager.getPlaceholders().register("creativeplots_plot_name", PlaceholderSupplier.create(IPlot.class, IPlot::getName));

        manager.getPlaceholders().register("creativeplots_plot_owner", ctx -> {
            MPlayer pl = ctx.getArgument(MPlayer.class);
            Plot plot = ctx.getArgument(Plot.class);

            if(plot == null) return null;

            return plot.getOwnerName(pl);
        });
    }

}
