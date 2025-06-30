package com.lx862.summitbot.command;

import com.lx862.summitbot.DiscordBot;
import com.lx862.summitbot.servermanager.PteroClient;
import com.lx862.summitbot.Summit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SchematicUploadCommand extends DiscordCommand {
    private static final String[] supportedFileType = new String[]{"schem"};

    public SchematicUploadCommand() {
        super("schemup", "Upload a worldedit schematic file");
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
                    .setTitle(":x: Upload Schematic")
                    .setDescription("You should upload the schematic as an attachment to your message!")
                    .build();

            messageCreateEvent.getMessage().replyEmbeds(embed).queue();
            return;
        }

        for(Message.Attachment attachment : messageCreateEvent.getMessage().getAttachments()) {
            String[] split = attachment.getFileName().split("\\.");
            String extension = split[split.length-1];
            if(!List.of(supportedFileType).contains(extension.toLowerCase(Locale.ROOT))) {
                MessageEmbed embed = bot.createEmbed()
                        .setTitle(":x: Upload Schematic")
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

                MessageEmbed progressEmbed = bot.createEmbed()
                        .setTitle(":hourglass: Upload Schematic")
                        .setDescription("Uploading schematic to the server...")
                        .build();

                messageCreateEvent.getMessage().replyEmbeds(progressEmbed).queue(msg -> {
                    try {
                        pteroClient.uploadFile("config/worldedit/schematics", cleanFileName, bytes);
                    } catch (Exception e) {
                        Summit.LOGGER.error("Failed to upload schematic via schemup!", e);
                        MessageEmbed failedEmbed = bot.createEmbed()
                                .setTitle(":x: Upload Schematic")
                                .setDescription(String.format("Failed to upload schematic `%s` :(\n```%s```", cleanFileName, e.getMessage()))
                                .setFooter("This likely isn't your fault, please contact administrator for more info.", null)
                                .build();
                        msg.editMessageEmbeds(failedEmbed).queue();
                    }

                    String schematicName = cleanFileName.substring(0, cleanFileName.lastIndexOf("."));

                    MessageEmbed successEmbed = bot.createEmbed()
                            .setTitle(":white_check_mark: Upload Schematic")
                            .setDescription(String.format("Schematic uploaded as `%s`, load with `/schem load %s` in-game.", cleanFileName, schematicName))
                            .build();
                    msg.editMessageEmbeds(successEmbed).queue();
                });
            }
        }
    }

    @Override
    public boolean hasPermission(DiscordBot bot, net.dv8tion.jda.api.entities.Member member) {
        for(String staffIds : bot.getConfig().permissionConfig.staffRoles()) {
            if(member.getRoles().stream().map(ISnowflake::getId).collect(Collectors.toSet()).contains(staffIds)) return true;
        }
        return false;
    }
}
