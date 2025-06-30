package com.lx862.summitbot;

import com.lx862.summitbot.command.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscordBot extends ListenerAdapter {
    public static final List<DiscordCommand> commands = new ArrayList<>();
    private final String brand;
    private final Config config;
    private JDA client;

    static {
        commands.add(new HelpCommand());
        commands.add(new PingCommand());
        commands.add(new PlayersCommand());
        commands.add(new DatapacksCommand());
        commands.add(new DatapackUploadCommand());
        commands.add(new SchematicUploadCommand());
        commands.add(new BackupsCommand());
        commands.add(new StatsCommand());
    }

    public DiscordBot(Config config) {
        if(config.token == null) {
            Summit.LOGGER.warn("[{}] Discord Token is not set-up, will not start discord bot!", Summit.NAME);
        }
        this.brand = config.brand;
        this.config = config;
    }

    public EmbedBuilder createEmbed() {
        return new EmbedBuilder()
                .setColor((int)(0xFFFFFF * Math.random()))
                .setFooter(brand == null ? String.format("%s %s", Summit.NAME, Summit.getModVersion()) : String.format("%s (%s %s)", brand, Summit.NAME, Summit.getModVersion()), null);
    }

    public void start() {
        if(config.token != null) {
            this.client = JDABuilder.createDefault(config.token, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(this)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS) // Don't need these
                    .build();
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        final SelfUser self = event.getJDA().getSelfUser();
        Summit.LOGGER.info("[{}] Logged in as {}#{}", Summit.NAME, self.getName(), self.getDiscriminator());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot() || event.getAuthor().getId().equals(event.getJDA().getSelfUser().getId())) return;

        String messageContent = event.getMessage().getContentRaw();
        String[] args = messageContent.split(" ");
        if(args[0].startsWith(config.prefix)) {
            String commandName = args[0].substring(config.prefix.length());
            String[] pureArgs = Arrays.copyOfRange(args, 1, args.length);

            for(DiscordCommand command : commands) {
                if(command.matchesCommand(commandName)) {
                    if(!command.hasPermission(this, event.getMember())) {
                        MessageEmbed embed = createEmbed()
                                .setColor(Color.RED)
                                .setTitle(":x: No permission!")
                                .setDescription(String.format("You don't have permission to use the command `%s`!", command.name))
                                .build();

                        event.getMessage().replyEmbeds(embed).queue();
                        return;
                    }

                    try {
                        command.invoke(event, pureArgs, this, event.getJDA());
                    } catch (Exception e) {
                        Summit.LOGGER.error("[{}] Error while executing discord command {}!", Summit.NAME, command.name, e);
                        MessageEmbed embed = createEmbed()
                                .setColor(Color.RED)
                                .setTitle(":x: Command error!")
                                .setDescription(String.format("Exception occurred while running command `%s` %s\n```%s```", command.name, config.sobEmoji, e.getMessage()))
                                .build();

                        event.getMessage().replyEmbeds(embed).queue();
                    }
                }
            }
        }
    }

    public Config getConfig() {
        return this.config;
    }

    public void logout() {
        if(client != null) {
            client.shutdownNow();
            client.getHttpClient().connectionPool().evictAll();
            client.getHttpClient().dispatcher().executorService().shutdown();
        }
    }
}
