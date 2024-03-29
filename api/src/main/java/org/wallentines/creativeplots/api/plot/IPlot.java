package org.wallentines.creativeplots.api.plot;

import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.mdcfg.serializer.Serializer;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.api.text.MComponent;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IPlot {


    /**
     * Gets the Owner of the plot
     *
     * @return The Owner's UUID
     */
    UUID getOwner();


    /**
     * Gets the ID of the plot
     *
     * @return The ID as a String
     */
    String getId();


    /**
     * Gets the friendly name of the plot, as set by the user
     *
     * @return The name of the plot as a String
     */
    MComponent getName();


    /**
     * Gets the plot's position in the world
     *
     * @return The position as a PlotPos
     */
    Vec3d getTeleportLocation();


    /**
     * Gets whether or not a user can edit on a specified plot
     *
     * @param user The UUID of the user to query
     * @return     Whether or not the user can edit, as a boolean
     */
    boolean canEdit(MPlayer user);


    /**
     * Gets whether or not a particular block position is within a plot or not
     *
     * @param location The location of the block to query
     * @return         Whether or not the block is within the plot, as a boolean
     */
    boolean contains(Vec3i location);


    /**
     * Changes the owner of the plot
     *
     * @param user The UUID of the new owner
     */
    void setOwner(UUID user);


    /**
     * Changes the friendly name of the plot
     *
     * @param name The new name as a String
     */
    void setName(MComponent name);


    void merge(IPlot other);


    List<Region> getArea();


    Set<PlotPos> getPositions();


    boolean isDenied(UUID u);


    void trustPlayer(UUID u);

    void untrustPlayer(UUID u);

    void denyPlayer(UUID u);

    void undenyPlayer(UUID u);

    boolean hasOwnerPermissions(MPlayer player);

    MComponent getOwnerName();

    void onEnter(MPlayer player);

    void onLeave(MPlayer player);


    Serializer<IPlot> serializer(IPlotWorld world);

    void setTimeOfDay(Integer time);

    Integer getTimeOfDay();

    void setRaining(Boolean raining);
    Boolean isRaining();

    void setThundering(Boolean raining);
    Boolean isThundering();

    void forceRefresh();

}
