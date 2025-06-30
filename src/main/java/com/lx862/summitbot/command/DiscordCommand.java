package com.lx862.summitbot.command;

import com.lx862.summitbot.DiscordBot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class DiscordCommand {
    public final String name;
    public final String description;

    public DiscordCommand(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public boolean matchesCommand(String command) {
        return command.equals(name);
    }

    public boolean hasPermission(DiscordBot bot, net.dv8tion.jda.api.entities.Member member) {
        return true;
    }

    public abstract void invoke(MessageReceivedEvent messageCreateEvent, String[] args, DiscordBot bot, JDA client) throws Exception;
}
