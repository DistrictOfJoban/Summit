package com.lx862.summitbot.command;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.lx862.summitbot.DiscordBot;
import com.lx862.summitbot.util.JsonUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class PlayersCommand extends DiscordCommand {
    private static final String[] WORLD_NAME = new String[]{"Overworld", "The Nether", "The End", "Unknown dimension"};

    public PlayersCommand() {
        super("players", "Check online players in-game.");
    }

    @Override
    public void invoke(MessageReceivedEvent messageCreateEvent, String[] args, DiscordBot bot, JDA client) {
        try {
            URLConnection con = new URL(bot.getConfig().mtrSysmapUrl + "/info").openConnection();

            try(JsonReader reader = new JsonReader(new BufferedReader(new InputStreamReader(con.getInputStream())))) {
                JsonArray playerInDim = new Gson().fromJson(reader, JsonArray.class);
                int playerCount = 0;
                EmbedBuilder embed = bot.createEmbed();

                for(int i = 0; i < playerInDim.size(); i++) {
                    String dimName = WORLD_NAME[i];
                    String playerStr = "";
                    JsonArray players = playerInDim.get(i).getAsJsonArray();
                    for(JsonElement playerElement : players) {
                        JsonObject playerObject = playerElement.getAsJsonObject();
                        if(!JsonUtil.getString("name", playerObject).isEmpty()) {
                            playerStr +=
                                    String.format("**%s**: Riding **%s** towards %s\n", playerObject.get("player").getAsString(), formatName(playerObject.get("name").getAsString()), formatName(playerObject.get("destination").getAsString()));
                        } else {
                            playerStr += "**" + JsonUtil.getString("player", playerObject) + "**\n";
                        }
                        playerCount++;
                    }

                    if(!players.isEmpty()) {
                        embed.addField(dimName, playerStr, false);
                    }
                }
                embed.setTitle(String.format("Online Player (%s)", playerCount));
                if(playerCount == 0) {
                    embed.setDescription("No player is online right now.");
                }
                messageCreateEvent.getMessage().replyEmbeds(embed.build()).queue();
            }
        } catch (IOException e) {
            messageCreateEvent.getMessage().reply("Cannot connect to Joban Minecraft Server.").queue();
        }
    }

    private static String formatName(String str) {
        return str.split("\\|\\|")[0].replace("|", " ");
    }
}
