package com.lx862.summitbot.command;

import com.lx862.summitbot.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand extends DiscordCommand {
    public HelpCommand() {
        super("help", "Show all command descriptions and usage");
    }

    @Override
    public void invoke(MessageReceivedEvent messageCreateEvent, String[] args, DiscordBot bot, JDA client) throws Exception {
        List<String> commands = new ArrayList<>();
        for(DiscordCommand command : DiscordBot.commands) {
            commands.add(String.format("`%s` - %s", command.name, command.description));
        }

        MessageEmbed embed = bot.createEmbed()
                .setTitle("Help")
                .setDescription(String.join("\n", commands))
                .build();

        messageCreateEvent.getMessage().replyEmbeds(embed).queue();
    }
}
