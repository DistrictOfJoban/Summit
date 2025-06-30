package com.lx862.summitbot.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.summitbot.DiscordBot;
import com.lx862.summitbot.servermanager.PteroClient;
import com.lx862.summitbot.util.JsonUtil;
import com.lx862.summitbot.Summit;
import com.lx862.summitbot.util.Util;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.util.stream.Collectors;

public class StatsCommand extends DiscordCommand {
    public StatsCommand() {
        super("stats", "View current statistics of the server");
    }

    @Override
    public void invoke(MessageReceivedEvent messageCreateEvent, String[] args, DiscordBot bot, JDA client) throws Exception {
        PteroClient pteroClient = Summit.getPteroClient();
        if(!pteroClient.isUsable()) {
            messageCreateEvent.getMessage().reply("Pterodactyl Integration is not setup!").queue();
            return;
        }

        try {
            JsonObject resource = Summit.getPteroClient().getResources().getAsJsonObject();
            JsonObject server = Summit.getPteroClient().getServer().getAsJsonObject();

            String allocationString = "";
            for(JsonElement allocElement : JsonUtil.getArray("attributes.relationships.allocations.data", server)) {
                JsonObject alloc = allocElement.getAsJsonObject();
                allocationString +=
                        String.format("`%s:%s` - %s\n\n",
                                JsonUtil.getString("attributes.ip", alloc),
                                JsonUtil.getString("attributes.port", alloc),
                                JsonUtil.getString("attributes.notes", alloc, ""));
            }

            EmbedBuilder embedBuilder = bot.createEmbed()
                    .setTitle(String.format("%s (%s)", JsonUtil.getString("attributes.name", server), JsonUtil.getString("attributes.status", server)))
                    .setDescription(JsonUtil.getString("attributes.description", server))
                    .addField("Node", JsonUtil.getString("attributes.node.name", server), false)
                    .addField("Allocation", allocationString, false)
                    .setFooter(String.format("Server ID: %s", JsonUtil.getString("attributes.identifier", server)), null);

            embedBuilder.setTitle(String.format("%s (%s)", JsonUtil.getString("attributes.name", server), JsonUtil.getString("attributes.current_state", resource)))
            .addField("CPU", Math.round((JsonUtil.getLong("attributes.resources.cpu_absolute", resource) / (double)(JsonUtil.getLong("attributes.limits.cpu", server)) * 100)) + "%", false)
            .addField("RAM", Util.toGB(JsonUtil.getLong("attributes.resources.memory_bytes", resource), 3, "GB") + "/" + Util.toGB(JsonUtil.getLong("attributes.limits.memory", server), 1, "GB"), false)
            .addField("Disk Space", Util.toGB( JsonUtil.getLong("attributes.resources.disk_bytes", resource), 3, "GB") + "/" + Util.toGB(JsonUtil.getLong("attributes.limits.disk", server), 1, "GB"), false);

            messageCreateEvent.getMessage().replyEmbeds(embedBuilder.build()).queue();
        } catch (IOException e) {
            MessageEmbed embed = bot.createEmbed()
                    .setTitle(":x: Stats")
                    .setDescription(String.format("Cannot retrieve information from panel.\n`%s`", e.getMessage()))
                    .build();

            messageCreateEvent.getMessage().replyEmbeds(embed).queue();
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
