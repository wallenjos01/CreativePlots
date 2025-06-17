package org.wallentines.creativeplots;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.DataResult;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.wallentines.invmenu.api.InventoryMenu;
import org.wallentines.mdcfg.Tuples;
import org.wallentines.midnightlib.math.Color;
import org.wallentines.midnightlib.math.Vec2i;
import org.wallentines.pseudonym.PipelineContext;
import org.wallentines.pseudonym.lang.LangManager;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PlotCommand {

    private final Supplier<CreativePlots> data;

    public PlotCommand(Supplier<CreativePlots> data) {
        this.data = data;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build(String id,
            LiteralArgumentBuilder<CommandSourceStack> builder, CommandBuildContext buildCtx,
            Supplier<CreativePlots> data) {
        return new PlotCommand(data).build(builder);
    }

    private LiteralArgumentBuilder<CommandSourceStack> build(LiteralArgumentBuilder<CommandSourceStack> builder) {

        return builder
                .then(Commands.literal("claim")
                        .requires(Permissions.require("creativeplots.command.claim", 2))
                        .executes(ctx -> {

                            ServerPlayer spl = ctx.getSource().getPlayerOrException();
                            Plotworld pw = (Plotworld) spl.level();

                            CreativePlots inst = data.get();

                            PlotMap map = pw.getPlotMap();
                            if (map == null) {
                                ctx.getSource()
                                        .sendFailure(inst.langManager.getMessage("command.error.not_plotworld", spl));
                                return 0;
                            }

                            Vec2i pos = map.getPlotPosition(spl.getBlockX(), spl.getBlockZ());
                            if (pos == null) {
                                ctx.getSource()
                                        .sendFailure(inst.langManager.getMessage("command.error.not_in_plot", spl));
                                return 0;
                            }

                            if (pw.getPlotAt(pos) != null) {
                                ctx.getSource().sendFailure(inst.langManager.getMessage("command.error.claimed", spl));
                                return 0;
                            }

                            Plot p = pw.claimPlot(spl, pos);
                            if (p == null) {
                                ctx.getSource()
                                        .sendFailure(data.get().langManager.getMessage("command.error.claimed", spl));
                                return 0;
                            }

                            p.sendTitle(spl, data.get().langManager);

                            ctx.getSource()
                                    .sendSuccess(() -> inst.langManager.getMessageFor("command.claim",
                                            PipelineContext.builder(spl)
                                                    .withContextPlaceholder("position", pos.toString())
                                                    .build()),
                                            false);

                            return 1;
                        }))
                .then(Commands.literal("clear")
                        .requires(Permissions.require("creativeplots.command.clear", 2))
                        .executes(ctx -> {

                            Plot p = getOwnedPlot(ctx);
                            if (p == null)
                                return 0;

                            openConfirmGUI(ctx.getSource().getPlayerOrException(), data.get().langManager, () -> {
                                p.world().clearPlot(p.rootPosition());
                                ctx.getSource().sendSuccess(() -> data.get().langManager.getMessageFor("command.clear",
                                        PipelineContext.of(ctx.getSource().getEntity())), false);
                            });
                            return 1;
                        })

                )
                .then(Commands.literal("delete")
                        .requires(Permissions.require("creativeplots.command.delete", 2))
                        .executes(ctx -> {

                            Plot p = getOwnedPlot(ctx);
                            if (p == null)
                                return 0;

                            openConfirmGUI(ctx.getSource().getPlayerOrException(), data.get().langManager, () -> {
                                p.world().deletePlot(p.rootPosition());
                                ctx.getSource().sendSuccess(() -> data.get().langManager.getMessage("command.delete",
                                        ctx.getSource().getEntity()), false);
                            });
                            return 1;
                        })

                )
                .then(Commands.literal("name")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .requires(Permissions.require("creativeplots.command.name", 2))
                                .executes(ctx -> {

                                    Plot p = getOwnedPlot(ctx);
                                    if (p == null)
                                        return 0;

                                    p.setName(StringArgumentType.getString(ctx, "name"));

                                    CreativePlots inst = data.get();
                                    p.sendTitle(ctx.getSource().getPlayerOrException(), inst.langManager);

                                    ctx.getSource().sendSuccess(() -> inst.langManager.getMessage("command.name",
                                            ctx.getSource().getEntity()), false);
                                    return 1;
                                })))
                .then(Commands.literal("color")
                        .requires(Permissions.require("creativeplots.command.color", 2))
                        .then(Commands.argument("color", StringArgumentType.greedyString())
                                .suggests((ctx, sBuilder) -> SharedSuggestionProvider
                                        .suggest(ChatFormatting.getNames(true, false), sBuilder))
                                .executes(ctx -> {

                                    Plot p = getOwnedPlot(ctx);
                                    if (p == null)
                                        return 0;

                                    CreativePlots inst = data.get();
                                    DataResult<TextColor> res = TextColor
                                            .parseColor(StringArgumentType.getString(ctx, "color"));
                                    if (res.isError()) {
                                        ctx.getSource().sendFailure(inst.langManager.getMessage(
                                                "command.error.invalid_color", ctx.getSource().getEntity()));
                                        return 0;
                                    }

                                    Color clr = new Color(res.getOrThrow().getValue());

                                    p.setColor(clr);
                                    p.sendTitle(ctx.getSource().getPlayerOrException(), inst.langManager);

                                    ctx.getSource().sendSuccess(() -> inst.langManager.getMessage("command.color",
                                            ctx.getSource().getEntity()), false);
                                    return 1;
                                })))
                .then(Commands.literal("editor")
                        .requires(Permissions.require("creativeplots.command.editor", 2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests((ctx, sBuilder) -> SharedSuggestionProvider
                                                .suggest(ctx.getSource().getServer().getPlayerNames(), sBuilder))
                                        .executes(ctx -> {
                                            Plot p = getOwnedPlot(ctx);
                                            if (p == null)
                                                return 0;

                                            getUUID(ctx).thenAccept(uuid -> {
                                                if (uuid == null) {
                                                    ctx.getSource()
                                                            .sendFailure(data.get().langManager.getMessage(
                                                                    "command.error.invalid_player",
                                                                    ctx.getSource().getEntity()));
                                                    return;
                                                }
                                                p.editors().add(uuid);
                                                ctx.getSource()
                                                        .sendSuccess(() -> data.get().langManager.getMessage(
                                                                "command.editor.add", ctx.getSource().getEntity()),
                                                                false);
                                            });

                                            return 1;
                                        })))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .suggests((ctx, sBuilder) -> {
                                            Plot p = getOwnedPlot(ctx);
                                            if (p == null)
                                                return null;
                                            return SharedSuggestionProvider.suggest(p.editors().stream()
                                                    .map(uuid -> ctx.getSource().getServer().getProfileCache().get(uuid)
                                                            .map(GameProfile::getName).orElse(uuid.toString())),
                                                    sBuilder);
                                        })
                                        .executes(ctx -> {
                                            Plot p = getOwnedPlot(ctx);
                                            if (p == null)
                                                return 0;

                                            getUUID(ctx).thenAccept(uuid -> {
                                                if (uuid == null) {
                                                    ctx.getSource()
                                                            .sendFailure(data.get().langManager.getMessage(
                                                                    "command.error.invalid_player",
                                                                    ctx.getSource().getEntity()));
                                                    return;
                                                }
                                                p.editors().remove(uuid);
                                                ctx.getSource()
                                                        .sendSuccess(() -> data.get().langManager.getMessage(
                                                                "command.editor.remove", ctx.getSource().getEntity()),
                                                                false);
                                            });

                                            return 1;
                                        }))))
                .then(Commands.literal("transfer")
                        .requires(Permissions.require("creativeplots.command.transfer", 2))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((ctx, sBuilder) -> SharedSuggestionProvider
                                        .suggest(ctx.getSource().getServer().getPlayerNames(), sBuilder))
                                .executes(ctx -> {
                                    Plot p = getOwnedPlot(ctx);
                                    if (p == null)
                                        return 0;

                                    ServerPlayer spl = ctx.getSource().getPlayerOrException();

                                    getUUID(ctx).thenAccept(uuid -> {

                                        if (uuid == null) {
                                            ctx.getSource().sendFailure(data.get().langManager.getMessage(
                                                    "command.error.invalid_player", ctx.getSource().getEntity()));
                                            return;
                                        }

                                        openConfirmGUI(spl, data.get().langManager, () -> {
                                            p.setOwner(uuid);
                                            ctx.getSource().sendSuccess(() -> data.get().langManager
                                                    .getMessage("command.transfer", ctx.getSource().getEntity()),
                                                    false);
                                        });

                                    });

                                    return 1;
                                })));
    }

    private CompletableFuture<UUID> getUUID(CommandContext<CommandSourceStack> ctx) {

        String username = StringArgumentType.getString(ctx, "name");
        ServerPlayer userPlayer = ctx.getSource().getServer().getPlayerList().getPlayerByName(username);
        if (userPlayer == null) {
            return new ResolvableProfile(Optional.of(username), Optional.empty(), new PropertyMap()).resolve()
                    .thenApply(res -> res.gameProfile().getId());
        } else {
            return CompletableFuture.completedFuture(userPlayer.getUUID());
        }
    }

    private Plot getOwnedPlot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer spl = ctx.getSource().getPlayerOrException();
        Plotworld pw = (Plotworld) spl.level();

        Tuples.T2<PlotMap.BlockType, Plot> p = pw.getBlockInfo(spl.getBlockX(), spl.getBlockZ());
        if (p.p1 == null) {
            ctx.getSource().sendFailure(
                    data.get().langManager.getMessage("command.error.not_plotworld", ctx.getSource().getEntity()));
            return null;
        }

        if (p.p2 == null) {
            if (p.p1 == PlotMap.BlockType.PLOT) {
                ctx.getSource().sendFailure(
                        data.get().langManager.getMessage("command.error.not_in_plot", ctx.getSource().getEntity()));
            } else {
                ctx.getSource().sendFailure(
                        data.get().langManager.getMessage("command.error.not_claimed", ctx.getSource().getEntity()));
            }
            return null;
        }

        if (!p.p2.hasOwnerPermissions(spl)) {
            ctx.getSource().sendFailure(
                    data.get().langManager.getMessage("command.error.not_owner", ctx.getSource().getEntity()));
            return null;
        }

        return p.p2;
    }

    private static void openConfirmGUI(ServerPlayer spl, LangManager<?, Component> langManager, Runnable onClick) {

        InventoryMenu gui = InventoryMenu.create(langManager.message("gui.confirm.title"), 9);

        gui.setItem(3, ctx -> new ItemStack(Holder.direct(Items.RED_STAINED_GLASS_PANE), 1, DataComponentPatch.builder()
                .set(DataComponents.ITEM_NAME, langManager.getMessage("gui.confirm.no", ctx))
                .build()),
                (pl, ct) -> {
                    gui.close(pl);
                });

        gui.setItem(5,
                ctx -> new ItemStack(Holder.direct(Items.LIME_STAINED_GLASS_PANE), 1, DataComponentPatch.builder()
                        .set(DataComponents.ITEM_NAME, langManager.getMessage("gui.confirm.yes", ctx))
                        .build()),
                (pl, ct) -> {
                    gui.close(pl);
                    onClick.run();
                });

        gui.open(spl);
    }

}
