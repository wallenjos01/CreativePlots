package org.wallentines.creativeplots;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.wallentines.mdcfg.serializer.ObjectSerializer;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.math.*;
import org.wallentines.pseudonym.PipelineContext;
import org.wallentines.pseudonym.lang.LangManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Plot {

    private final Plotworld world;
    private final Vec2i rootPosition;
    private final Set<Vec2i> positions;
    private final List<Region> regions;
    private final Set<UUID> editors;
    private final String ownerName;

    private UUID owner;
    private Color color;
    private String name;


    public Plot(Plotworld world, Vec2i rootPosition, Set<Vec2i> positions, List<Region> regions, Color color, String name, UUID owner, String ownerName, Set<UUID> editors) {
        this.world = world;
        this.rootPosition = rootPosition;
        this.positions = positions;
        this.regions = regions;
        this.color = color == null ? Color.fromRGBI(6) : color;
        this.name = name == null ? rootPosition.toString() : name;
        this.owner = owner;
        this.ownerName = ownerName == null ? owner.toString() : ownerName;
        this.editors = new HashSet<>(editors);
    }

    public Plotworld world() {
        return world;
    }

    public Vec2i rootPosition() {
        return rootPosition;
    }

    public Set<Vec2i> positions() {
        return positions;
    }

    public List<Region> regions() {
        return regions;
    }

    public UUID owner() {
        return owner;
    }

    public Color color() {
        return color;
    }

    public String name() {
        return name;
    }

    public String ownerName() {
        return ownerName;
    }

    public Set<UUID> editors() {
        return editors;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void sendTitle(ServerPlayer player, LangManager<?, Component> langManager) {

        PipelineContext ctx = PipelineContext.builder(player)
                .withContextPlaceholder("name", name)
                .withContextPlaceholder("owner", ownerName)
                .withContextPlaceholder("color", color().toHex())
                .build();

        player.connection.send(new ClientboundSetTitleTextPacket(langManager.getMessageFor("title.name", ctx)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(langManager.getMessageFor("title.owner", ctx)));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 30, 5));

    }


    public boolean contains(Vec3d pos) {
        for(Region r : regions) {
            if(r.isWithin(pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(Vec3i pos) {
        for(Region r : regions) {
            if(r.isWithin(pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean mayModify(UUID uuid) {
        return owner.equals(uuid) || editors.contains(uuid);
    }

    public boolean hasOwnerPermissions(ServerPlayer player) {
        return owner.equals(player.getUUID()) || Permissions.check(player, "creativeplots.owner_everywhere", 2);
    }

    public static Serializer<Plot> serializer(Plotworld parent) {
        return ObjectSerializer.create(
                Vec2i.SERIALIZER.entry("root_position", Plot::rootPosition),
                Vec2i.SERIALIZER.listOf().mapToSet().entry("positions", Plot::positions),
                Region.SERIALIZER.listOf().mapToList().entry("regions", Plot::regions),
                Color.SERIALIZER.entry("color", Plot::color).optional(),
                Serializer.STRING.entry("name", Plot::name).optional(),
                Serializer.UUID.entry("owner", Plot::owner),
                Serializer.STRING.entry("owner_name", Plot::ownerName).optional(),
                Serializer.UUID.listOf().mapToSet().entry("editors", Plot::editors).optional(),
                (root, pos, reg, color, name, owner, ownerName, editors) ->
                        new Plot(parent, root, pos, reg, color, name, owner, ownerName, editors));
    }

}
