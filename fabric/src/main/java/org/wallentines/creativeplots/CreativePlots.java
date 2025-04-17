package org.wallentines.creativeplots;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;
import org.wallentines.mdcfg.mc.api.ServerConfigFolders;
import org.wallentines.pseudonym.MessagePipeline;
import org.wallentines.pseudonym.PartialMessage;
import org.wallentines.pseudonym.lang.LangManager;
import org.wallentines.pseudonym.lang.LangProvider;
import org.wallentines.pseudonym.lang.LangRegistry;
import org.wallentines.pseudonym.mc.api.ServerPlaceholders;

import java.nio.file.Files;
import java.nio.file.Path;

public class CreativePlots {

    LangManager<?, Component> langManager;

    private CreativePlots(MinecraftServer server) {

        Path configDir = ServerConfigFolders.getConfigFolder(server).resolve("CreativePlots");
        try { Files.createDirectories(configDir); } catch (Exception e) { throw new RuntimeException(e); }

        MessagePipeline<String, PartialMessage<String>> parser = MessagePipeline.parser(ServerPlaceholders.getServerPlaceholders(server));

        this.langManager = new LangManager<>(Component.class, LangRegistry.builder(parser)
                .add("title.name","&<color>&l<name>")
                .add("title.owner","&7Owned by <owner>")
                .add("title.unclaimed","&7Unclaimed Plot")
                .add("title.claim","&fUse &a/plot claim &fto claim this plot.")
                .add("command.error.not_plotworld","You are not in a plot world!")
                .add("command.error.not_in_plot","You are not in a plot!")
                .add("command.error.claimed","This plot is already claimed!")
                .add("command.error.not_claimed","This plot is not claimed!")
                .add("command.error.not_owner","You do not own this plot!")
                .add("command.error.claim","Unable to claim plot!")
                .add("command.error.invalid_color","That is not a valid color!")
                .add("command.error.invalid_player","That is not a valid player!")
                .add("command.claim","&fPlot <position> claimed! Type &a/plot name &fto rename it.")
                .add("command.clear","&fPlot cleared")
                .add("command.delete","&fPlot deleted")
                .add("command.name","&fPlot renamed")
                .add("command.color","&fPlot color changed")
                .add("command.editor.add","&fPlot editor added")
                .add("command.editor.remove","&fPlot editor removed")
                .add("command.transfer","&fPlot ownership transferred")
                .add("gui.confirm.title","Are you sure?")
                .add("gui.confirm.yes","&aYes")
                .add("gui.confirm.no","&cNo")
                .build(),
                LangProvider.forDirectory(configDir.resolve("lang"), ServerConfigFolders.FILE_CODEC_REGISTRY, parser), ServerPlaceholders.COMPONENT_RESOLVER);


    }

    public LangManager<?, Component> getLangManager() {
        return langManager;
    }

    public static CreativePlots create(MinecraftServer server, @Nullable CreativePlots previous) {
        return new CreativePlots(server);
    }

}
