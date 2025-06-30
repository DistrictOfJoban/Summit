package com.lx862.summitbot.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.summitbot.*;
import com.lx862.summitbot.servermanager.PteroClient;
import com.lx862.summitbot.util.JsonUtil;
import com.lx862.summitbot.util.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class BackupsCommand extends DiscordCommand {
    public BackupsCommand() {
        super("backups", "View historic backup");
    }

    @Override
    public void invoke(MessageReceivedEvent messageCreateEvent, String[] args, DiscordBot bot, JDA client) throws Exception {
        PteroClient pteroClient = Summit.getPteroClient();
        if(!pteroClient.isUsable()) {
            messageCreateEvent.getMessage().reply("Pterodactyl Integration is not setup!").queue();
            return;
        }

        MessageEmbed initialEmbed = bot.createEmbed()
                .setTitle("Backups")
                .setDescription("Looking for backups...")
                .build();

        boolean shouldShowDownload = args.length > 0 && args[0].equals("/s");

        messageCreateEvent.getMessage().replyEmbeds(initialEmbed).queue(msg -> {
            try {
                JsonElement data = pteroClient.getBackups();

                JsonArray backups = data.getAsJsonObject().get("data").getAsJsonArray();
                List<MessageEmbed> embeds = new ArrayList<>();

                for(JsonElement backupElement : backups) {
                    JsonObject backup = backupElement.getAsJsonObject().get("attributes").getAsJsonObject();
                    String uuid = JsonUtil.getString("uuid", backup);
                    String createdAt = JsonUtil.getString("created_at", backup);
                    String completedAt = JsonUtil.getString("completed_at", backup);
                    boolean creating = completedAt == null;

                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String downloadLink = shouldShowDownload ? Util.getJson("attributes.url", pteroClient.getBackup(uuid).getAsJsonObject()).getAsString() : null;

                    EmbedBuilder embed = bot.createEmbed()
                            .setTitle(backup.get("name").getAsString() + (creating ? " (Creating)" : ""))
                            .addField("File Size", Util.toGB(backup.get("bytes").getAsLong(), 3, "GB"), false)
                            .addField("Created At", dateFormat.format(Date.from(OffsetDateTime.parse(createdAt).toInstant())), false);
                    if(downloadLink != null) embed.setUrl(downloadLink);

                    if(!creating) {
                        OffsetDateTime startTime = OffsetDateTime.parse(createdAt);
                        OffsetDateTime endTime = OffsetDateTime.parse(completedAt);
                        long diffMs = (endTime.toEpochSecond() - startTime.toEpochSecond()) * 1000;
                        embed.addField("Time taken", DurationFormatUtils.formatDuration(diffMs, "HH:mm:ss", true), false);
                    }

                    embed.setFooter("ID: " + uuid, null);
                    embeds.add(embed.build());
                }

                msg.editMessageEmbeds(embeds).queue();
            } catch (IOException e) {
                Summit.LOGGER.error("[Backups Command] Error while looking for backup!", e);
                messageCreateEvent.getMessage().reply("Error while looking for backup!").queue();
            }
        });
    }

    @Override
    public boolean hasPermission(DiscordBot bot, net.dv8tion.jda.api.entities.Member member) {
        for(String staffIds : bot.getConfig().permissionConfig.adminRoles()) {
            if(member.getRoles().stream().map(ISnowflake::getId).collect(Collectors.toSet()).contains(staffIds)) return true;
        }
        return false;
    }
}
