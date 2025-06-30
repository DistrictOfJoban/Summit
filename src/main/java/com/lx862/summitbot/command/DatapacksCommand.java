package com.lx862.summitbot.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lx862.summitbot.*;
import com.lx862.summitbot.servermanager.PteroClient;
import com.lx862.summitbot.util.JsonUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatapacksCommand extends DiscordCommand {
    public DatapacksCommand() {
        super("datapacks", "List datapacks");
    }

    @Override
    public void invoke(MessageReceivedEvent messageCreateEvent, String[] args, DiscordBot bot, JDA client) throws Exception {
        PteroClient pteroClient = Summit.getPteroClient();
        if(!pteroClient.isUsable()) {
            messageCreateEvent.getMessage().reply("Pterodactyl Integration is not setup!").queue();
            return;
        }

        JsonElement jsonElement = pteroClient.listFiles("world/datapacks");
        JsonArray dataList = jsonElement.getAsJsonObject().get("data").getAsJsonArray();
        List<String> files = new ArrayList<>();
        for(JsonElement fileElement : dataList) {
            JsonObject fileObject = fileElement.getAsJsonObject();
            files.add(JsonUtil.getString("attributes.name", fileObject));
        }

        MessageEmbed embed = bot.createEmbed()
                .setTitle(String.format("Datapacks (%d)", files.size()))
                .setDescription(String.format("```%s```", String.join("\n", files)))
                .build();

        messageCreateEvent.getMessage().replyEmbeds(embed).queue();
    }

    @Override
    public boolean hasPermission(DiscordBot bot, net.dv8tion.jda.api.entities.Member member) {
        for(String staffId : bot.getConfig().permissionConfig.staffRoles()) {
            if(member.getRoles().stream().map(ISnowflake::getId).collect(Collectors.toSet()).contains(staffId)) return true;
        }
        return false;
    }
}
