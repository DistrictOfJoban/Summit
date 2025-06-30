package com.lx862.summitbot.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.lx862.summitbot.DiscordBot;
import com.lx862.summitbot.servermanager.PteroClient;
import com.lx862.summitbot.Summit;
import com.lx862.summitbot.util.JsonUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DatapackUploadCommand extends DiscordCommand {
    private static final String[] supportedFileType = new String[]{"zip"};

    public DatapackUploadCommand() {
        super("dpup", "Upload datapack");
    }

    @Override
    public void invoke(MessageReceivedEvent messageCreateEvent, String[] args, DiscordBot bot, JDA client) throws Exception {
        PteroClient pteroClient = Summit.getPteroClient();
        if(!pteroClient.isUsable()) {
            messageCreateEvent.getMessage().reply("Pterodactyl Integration is not setup!").queue();
            return;
        }

        if(messageCreateEvent.getMessage().getAttachments().isEmpty()) {
            MessageEmbed embed = bot.createEmbed()
                    .setTitle(":x: Upload datapack")
                    .setDescription("You should upload the datapack as an attachment to your message!")
                    .build();

            messageCreateEvent.getMessage().replyEmbeds(embed).queue();
            return;
        }

        for(Message.Attachment attachment : messageCreateEvent.getMessage().getAttachments()) {
            String[] split = attachment.getFileName().split("\\.");
            String extension = split[split.length-1];
            if(!List.of(supportedFileType).contains(extension.toLowerCase(Locale.ROOT))) {
                MessageEmbed embed = bot.createEmbed()
                        .setTitle(":x: Upload datapack")
                        .setDescription(String.format("Invalid file type for `%s`, supported file types are: `%s`", attachment.getFileName(), String.join(", ", supportedFileType)))
                        .build();
                messageCreateEvent.getMessage().replyEmbeds(embed).queue();
                return;
            }
        }

        for(Message.Attachment attachment : messageCreateEvent.getMessage().getAttachments()) {
            URL url = new URL(attachment.getUrl());
            String cleanFileName = attachment.getFileName().replace("../", "").replace("/", "_");

            try(InputStream is = url.openStream()) {
                byte[] bytes = is.readAllBytes();
                long packFormat;
                String packDescription;

                try(ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
                    ZipEntry zipEntry;
                    boolean packFileFound = false;
                    while((zipEntry = zis.getNextEntry()) != null) {
                        if(zipEntry.getName().equals("pack.mcmeta")) {
                            packFileFound = true;
                            break;
                        }
                    }
                    if(!packFileFound) {
                        MessageEmbed embed = bot.createEmbed()
                                .setTitle(":x: Upload datapack")
                                .setDescription("`pack.mcmeta` not found inside the uploaded file, this is not a valid datapack!")
                                .build();

                        messageCreateEvent.getMessage().replyEmbeds(embed).queue();
                        return;
                    }

                    JsonObject jsonObject;
                    try {
                        jsonObject = new Gson().fromJson(new InputStreamReader(zis), JsonObject.class);
                        JsonUtil.expectField("pack.pack_format", jsonObject);
                        JsonUtil.expectField("pack.description", jsonObject);
                    } catch (Exception e) {
                        Summit.LOGGER.warn("Failed to read datapack meta file during dpup command.", e);
                        MessageEmbed embed = bot.createEmbed()
                                .setTitle(":x: Upload datapack")
                                .setDescription("`pack.mcmeta` is found but failed to read, make sure the metadata file is correct!")
                                .build();

                        messageCreateEvent.getMessage().replyEmbeds(embed).queue();
                        return;
                    }
                    packFormat = JsonUtil.getLong("pack.pack_format", jsonObject);
                    packDescription = JsonUtil.getString("pack.description", jsonObject);
                }

                MessageEmbed progressEmbed = bot.createEmbed()
                        .setTitle(":hourglass: Upload datapack")
                        .setDescription("Datapack validated, uploading to the server...")
                        .addField("Datapack Info", String.format("**Pack Version**: `%d`\n**Description**: `%s`", packFormat, packDescription), false)
                        .build();

                messageCreateEvent.getMessage().replyEmbeds(progressEmbed).queue(msg -> {
                    try {
                        pteroClient.uploadFile("world/datapacks", cleanFileName, bytes);
                    } catch (Exception e) {
                        Summit.LOGGER.error("Failed to upload datapack via dpup!", e);
                        MessageEmbed failedEmbed = bot.createEmbed()
                                .setTitle(":x: Upload datapack")
                                .setDescription(String.format("Failed to upload the datapack :(\n```%s```", e.getMessage()))
                                .setFooter("This likely isn't your fault, please contact administrator for more info.", null)
                                .build();
                        msg.editMessageEmbeds(failedEmbed).queue();
                        return;
                    }

                    try {
                        pteroClient.uploadFile("world/datapacks", cleanFileName, bytes);
                        pteroClient.sendCommand("reload");

                        JsonObject playerList = pteroClient.getPlayers().getAsJsonObject();
                        long playerOnline = JsonUtil.getLong("num_players", playerList);

                        MessageEmbed successEmbed = bot.createEmbed()
                                .setTitle(":white_check_mark: Upload datapack")
                                .setDescription(String.format("Datapack uploaded as `%s`\nReloaded datapacks with **%d** player(s) online.", cleanFileName, playerOnline))
                                .build();

                        messageCreateEvent.getMessage().replyEmbeds(successEmbed).queue();
                    } catch (Exception e) {
                        Summit.LOGGER.error("[dpup command] Error on post-upload!", e);
                        MessageEmbed failedEmbed = bot.createEmbed()
                                .setTitle(":x: Error handling post-upload process")
                                .setDescription(String.format("Failed to upload the datapack :(\n```%s```", e.getMessage()))
                                .setFooter("This likely isn't your fault, please contact administrator for more info.", null)
                                .build();
                        msg.editMessageEmbeds(failedEmbed).queue();
                    }
                });
            }
        }
    }

    @Override
    public boolean hasPermission(DiscordBot bot, net.dv8tion.jda.api.entities.Member member) {
        for(String staffId : bot.getConfig().permissionConfig.adminRoles()) {
            if(member.getRoles().stream().map(ISnowflake::getId).collect(Collectors.toSet()).contains(staffId)) return true;
        }
        return false;
    }
}
