package org.wallentines.creativeplots.fabric;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.core.registries.BuiltInRegistries;
import org.wallentines.creativeplots.api.CreativePlotsAPI;
import org.wallentines.creativeplots.api.math.Region;
import org.wallentines.creativeplots.api.plot.IPlot;
import org.wallentines.creativeplots.api.plot.IPlotWorld;
import org.wallentines.creativeplots.api.plot.PlotDirection;
import org.wallentines.creativeplots.api.plot.PlotPos;
import org.wallentines.creativeplots.common.Plot;
import org.wallentines.creativeplots.fabric.generator.PlotworldGenerator;
import org.wallentines.mdcfg.Tuples;
import org.wallentines.midnightcore.api.item.InventoryGUI;
import org.wallentines.midnightcore.api.item.MItemStack;
import org.wallentines.midnightcore.api.player.Location;
import org.wallentines.midnightcore.api.text.*;
import org.wallentines.midnightcore.fabric.util.CommandUtil;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.math.Vec3d;
import org.wallentines.midnightlib.math.Vec3i;
import org.wallentines.midnightcore.api.player.MPlayer;
import org.wallentines.midnightcore.fabric.player.FabricPlayer;
import org.wallentines.midnightcore.fabric.util.ConversionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.*;

public class MainCommand {

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
            Commands.literal("plot")
                .requires(Permissions.require("creativeplots.command", 2))
                .then(Commands.literal("claim")
                    .requires(Permissions.require("creativeplots.command.claim", 2))
                    .executes(context -> executeClaim(context, context.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeClaim(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("delete")
                    .requires(Permissions.require("creativeplots.command.delete", 2))
                    .executes(this::executeDelete)
                )
                .then(Commands.literal("tp")
                    .requires(Permissions.require("creativeplots.command.tp", 2))
                    .then(Commands.argument("plot", StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            List<String> out = new ArrayList<>();
                            MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());
                            IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);

                            for(IPlot p : pw.getPlotRegistry()) {
                                if(p.getOwner().equals(pl.getUUID())) {
                                    out.add(p.getId());
                                }
                            }

                            return SharedSuggestionProvider.suggest(out, builder);
                        })
                        .executes(context -> executeTp(context, context.getArgument("plot", String.class)))
                    )
                )
                .then(Commands.literal("rename")
                    .requires(Permissions.require("creativeplots.command.rename", 2))
                    .then(Commands.argument("name", StringArgumentType.greedyString())
                        .executes(context -> executeRename(context, context.getArgument("name", String.class)))
                    )
                )
                .then(Commands.literal("trust")
                    .requires(Permissions.require("creativeplots.command.trust", 2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeTrust(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("remove")
                    .requires(Permissions.require("creativeplots.command.remove", 2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeRemove(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("deny")
                    .requires(Permissions.require("creativeplots.command.deny", 2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeDeny(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("allow")
                    .requires(Permissions.require("creativeplots.command.allow", 2))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> executeAllow(context, context.getArgument("player", EntitySelector.class).findSinglePlayer(context.getSource())))
                    )
                )
                .then(Commands.literal("merge")
                    .requires(Permissions.require("creativeplots.command.merge", 2))
                    .executes(this::executeMerge)
                )
                .then(Commands.literal("time")
                    .requires(Permissions.require("creativeplots.command.time", 2))
                    .then(Commands.literal("reset")
                        .executes(context -> executeTime(context, null))
                    )
                    .then(Commands.argument("time", IntegerArgumentType.integer(0, 24000))
                        .executes(context -> executeTime(context, context.getArgument("time", Integer.class)))
                    )
                )
                .then(Commands.literal("weather")
                    .requires(Permissions.require("creativeplots.command.weather", 2))
                    .then(Commands.literal("reset")
                            .executes(context -> executeWeather(context, null, null))
                    )
                    .then(Commands.argument("rain", BoolArgumentType.bool())
                        .then(Commands.argument("thunder", BoolArgumentType.bool())
                            .executes(context -> executeWeather(context, context.getArgument("rain", Boolean.class), context.getArgument("thunder", Boolean.class)))
                        )
                    )
                )
        );

    }

    private int executeClaim(CommandContext<CommandSourceStack> context, ServerPlayer player) {

        MPlayer pl = FabricPlayer.wrap(player);

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = new Vec3d(player.getX(), player.getY(), player.getZ()).truncate();
        PlotPos pos = PlotPos.fromCoords(location.getX(), location.getZ(), pw.getPlotSize(), pw.getRoadSize());

        if(pos == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_location");
            return 0;
        }

        IPlot plot = pw.getPlotRegistry().getPlotAt(pos);
        if(plot != null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.claimed");
            return 0;
        }

        // Register a new plot
        plot = new Plot(pw, pos.getX() + "," + pos.getZ(), pos);
        plot.setOwner(pl.getUUID());

        ((Plot) plot).register(pw.getPlotRegistry());

        CommandUtil.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.claim.success");
        plot.onEnter(pl);


        ServerLevel l = context.getSource().getLevel();
        BlockState state = BuiltInRegistries.BLOCK.get(ConversionUtil.toResourceLocation(pw.getClaimedBorderBlock())).defaultBlockState();
        int borderHeight = pw.getGenerationHeight() + 1;

        Region reg = pos.getRegion(pw);
        BlockPos.MutableBlockPos bpos = new BlockPos.MutableBlockPos();

        reg.forEachBorder(vec2i -> {
            bpos.set(vec2i.getX(), borderHeight, vec2i.getY());
            l.setBlock(bpos, state, 2);
        });

        return 1;
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().getCoordinates().truncate();
        IPlot plot = pw.getPlot(location);

        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        InventoryGUI confirm = pl.getServer().getMidnightCore().createGUI(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.title", pl));
        confirm.setItem(
                3,
                MItemStack.Builder
                        .woolWithColor(Color.fromRGBI(10))
                        .withName(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.yes", pl))
                        .build(),
                (type, user) -> {
                    doDelete(pw, plot, context.getSource().getLevel());
                    CommandUtil.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.delete.success");
                    confirm.close(user);
                });

        confirm.setItem(
                5,
                MItemStack.Builder
                        .woolWithColor(Color.fromRGBI(12))
                        .withName(CreativePlotsAPI.getInstance().getLangProvider().getMessage("command.delete.gui.no", pl))
                        .build(),
                (type, user) -> confirm.close(user));

        confirm.open(pl, 0);

        return 1;
    }

    private void doDelete(IPlotWorld world, IPlot plot, ServerLevel level) {

        ChunkGenerator gen = level.getChunkSource().getGenerator();
        if(!(gen instanceof PlotworldGenerator)) return;

        for(Region r : plot.getArea()) {
            Region newReg = r.outset(1);
            ((PlotworldGenerator) gen).regenerateRegion(newReg, level);
        }

        world.getPlotRegistry().unregisterPlot(plot);
    }

    private int executeTp(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        IPlot plot = pw.getPlotRegistry().getPlot(name);
        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        pl.teleport(new Location(pl.getLocation().getWorldId(), plot.getTeleportLocation(), 0, 0));
        return 1;
    }

    private int executeRename(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().getCoordinates().truncate();
        IPlot plot = pw.getPlot(location);

        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        MComponent outName;
        if(pl.hasPermission("creativeplots.command.rename.raw", 2)) {
            outName = MComponent.parse(name);
        } else {
            outName = new MTextComponent(name);
        }
        outName = outName.withStyle(outName.getStyle().fillFrom(new MStyle().withColor(Color.fromRGBI(6))));
        plot.setName(outName);
        CommandUtil.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.rename.success", CustomPlaceholder.create("name", outName));

        return 1;

    }

    private int executeTrust(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(player);
        MPlayer sender = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(sender);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().getCoordinates().truncate();
        IPlot plot = pw.getPlot(location);

        if(plot == null || !plot.hasOwnerPermissions(sender)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(pl.equals(sender)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_self_target");
            return 0;
        }

        if(plot.canEdit(pl)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_trust_target");
            return 0;
        }

        plot.trustPlayer(player.getUUID());
        CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.trust.success");

        return 1;
    }

    private int executeRemove(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(player);
        MPlayer sender = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(sender);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().getCoordinates().truncate();
        IPlot plot = pw.getPlot(location);
        if(plot == null || !plot.hasOwnerPermissions(sender)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(pl.equals(sender)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_self_target");
            return 0;
        }

        if(!plot.canEdit(pl)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_remove_target");
            return 0;
        }

        plot.untrustPlayer(pl.getUUID());
        CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.trust.success");

        return 1;
    }

    private int executeDeny(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(player);
        MPlayer sender = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(sender);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }
        Vec3i location = sender.getLocation().getCoordinates().truncate();
        IPlot plot = pw.getPlot(location);
        if(plot == null || !plot.hasOwnerPermissions(sender)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(pl.equals(sender)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_self_target");
            return 0;
        }

        if(plot.isDenied(pl.getUUID())) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_deny_target");
            return 0;
        }

        plot.denyPlayer(player.getUUID());
        CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.deny.success");

        return 1;
    }

    private int executeAllow(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(player);
        MPlayer sender = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(sender);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = sender.getLocation().getCoordinates().truncate();
        IPlot plot = pw.getPlot(location);
        if(plot == null || !plot.hasOwnerPermissions(sender)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        if(pl.equals(sender)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_self_target");
            return 0;
        }

        if(!plot.isDenied(pl.getUUID())) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_undeny_target");
            return 0;
        }

        plot.undenyPlayer(player.getUUID());
        CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.allow.success");

        return 1;
    }

    private int executeMerge(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {

        ServerPlayer spl = context.getSource().getPlayerOrException();
        MPlayer pl = FabricPlayer.wrap(spl);

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().getCoordinates().truncate();
        PlotPos pos = PlotPos.fromCoords(location.getX(), location.getZ(), pw.getPlotSize(), pw.getRoadSize());

        if(pos == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_location");
            return 0;
        }

        IPlot plot = pw.getPlotRegistry().getPlotAt(pos);
        if(plot == null || !pl.getUUID().equals(plot.getOwner())) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        PlotDirection dir = PlotDirection.byName(spl.getDirection().getName());
        if(dir == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_direction");
            return 0;
        }

        PlotPos otherPos = pos.getAdjacent(dir);
        IPlot other = pw.getPlotRegistry().getPlotAt(otherPos);

        if(other == null || !other.getOwner().equals(pl.getUUID())) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.claimed");
            return 0;
        }

        int max = CreativePlotsAPI.getInstance().getMaxPlotSize();
        if(plot.getPositions().size() + other.getPositions().size() > max) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.too_big", CustomPlaceholderInline.create("limit", max+""));
            return 0;
        }

        CommandUtil.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.merge.success");

        ServerLevel l = context.getSource().getLevel();

        ChunkGenerator gen = l.getChunkSource().getGenerator();
        Set<Region> roads = null;
        if (gen instanceof PlotworldGenerator) {
            roads = mergePlots(plot, other, pw);
        }

        plot.merge(other);
        ((Plot) plot).register(pw.getPlotRegistry());

        if (roads != null) {

            int height = pw.getGenerationHeight();
            BlockPos.MutableBlockPos bpos = new BlockPos.MutableBlockPos();
            BlockState border = BuiltInRegistries.BLOCK.get(ConversionUtil.toResourceLocation(pw.getClaimedBorderBlock())).defaultBlockState();
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState top = ((PlotworldGenerator) gen).settings().getBlockForLayer(pw.getGenerationHeight(), l.getMinBuildHeight());

            // Remove roads
            for(Region road : roads) {

                for (int x = road.getLowerBound().getX(); x < road.getUpperBound().getX(); x++) {
                    for (int z = road.getLowerBound().getZ(); z < road.getUpperBound().getZ(); z++) {

                        bpos.set(x, height, z);
                        l.setBlock(bpos, top, 2);
                        l.blockUpdated(bpos, top.getBlock());

                        bpos.set(x, height + 1, z);
                        l.setBlock(bpos, air, 2);
                        l.blockUpdated(bpos, air.getBlock());
                    }
                }
            }

            // Fix Borders
            for(Region reg : plot.getArea()) {

                reg.forEachBorder(vec2i -> {
                    if(plot.contains(new Vec3i(vec2i.getX(),height,vec2i.getY()))) return;
                    bpos.set(vec2i.getX(), height + 1, vec2i.getY());
                    l.setBlock(bpos, border, 2);
                });
            }
        }

        return 1;
    }

    private static Set<Region> mergePlots(IPlot plot1, IPlot plot2, IPlotWorld pw) {

        Set<Region> regions = new HashSet<>();

        boolean smaller = plot2.getPositions().size() < plot1.getPositions().size();

        IPlot p1 = smaller ? plot2 : plot1;
        IPlot p2 = smaller ? plot1 : plot2;

        for(PlotPos pos : p1.getPositions()) {

            List<PlotDirection> directions = new ArrayList<>(4);

            for(PlotDirection pd : PlotDirection.values()) {
                if(p2.getPositions().contains(pos.getAdjacent(pd))) {
                    directions.add(pd);
                }
            }
            EnumSet<PlotDirection> dirs = EnumSet.copyOf(directions);

            for(PlotDirection pd : dirs) {
                regions.add(pos.getRoad(pd, pw));
            }

            // Check for corners
            for(PlotDirection pd : PlotDirection.values()) {
                if(!dirs.contains(pd) && p1.getPositions().contains(pos.getAdjacent(pd))) {
                    directions.add(pd);
                }
            }
            if(directions.size() == 0) continue;

            dirs = EnumSet.copyOf(directions);

            for(Tuples.T2<PlotDirection, PlotDirection> diagonal : PlotDirection.DIAGONAL_DIRECTIONS) {

                PlotDirection ns = diagonal.p1;
                PlotDirection ew = diagonal.p2;

                if(dirs.contains(ns) && dirs.contains(ew)) {
                    if(p2.getPositions().contains(pos.getDiagonal(ns, ew))) {
                        regions.add(pos.getIntersection(ns, ew, pw));
                    }
                }
            }
        }

        return regions;
    }

    private int executeTime(CommandContext<CommandSourceStack> context, Integer time) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().getCoordinates().truncate();
        IPlot plot = pw.getPlot(location);

        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        plot.setTimeOfDay(time);
        plot.forceRefresh();
        CommandUtil.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.time.success");

        return 1;
    }

    private int executeWeather(CommandContext<CommandSourceStack> context, Boolean rain, Boolean thunder) throws CommandSyntaxException {

        MPlayer pl = FabricPlayer.wrap(context.getSource().getPlayerOrException());

        IPlotWorld pw = CreativePlotsAPI.getInstance().getPlotWorld(pl);
        if(pw == null) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.invalid_world");
            return 0;
        }

        Vec3i location = pl.getLocation().getCoordinates().truncate();
        IPlot plot = pw.getPlot(location);

        if(plot == null || !plot.hasOwnerPermissions(pl)) {
            CommandUtil.sendCommandFailure(context, CreativePlotsAPI.getInstance().getLangProvider(), "command.error.not_claimed");
            return 0;
        }

        plot.setRaining(rain);
        plot.setThundering(thunder);
        plot.forceRefresh();
        CommandUtil.sendCommandSuccess(context, CreativePlotsAPI.getInstance().getLangProvider(), false, "command.weather.success");

        return 1;
    }

}
