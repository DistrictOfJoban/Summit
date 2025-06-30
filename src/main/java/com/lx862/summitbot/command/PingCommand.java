package com.lx862.summitbot.command;

import com.lx862.summitbot.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PingCommand extends DiscordCommand {

    public PingCommand() {
        super("ping", "Test whether the bot is online");
    }

    @Override
    public void invoke(MessageReceivedEvent messageCreateEvent, String[] args, DiscordBot bot, JDA client) {
        messageCreateEvent.getMessage().reply("Pong!").queue();
    }
}
