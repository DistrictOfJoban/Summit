package com.lx862.summitbot;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lx862.summitbot.servermanager.PteroClient;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class Summit implements ModInitializer {
    public static final String NAME = "Summit";
    public static final String ID = "summit";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);
    private static final String modVersion = FabricLoader.getInstance().getModContainer(ID).get().getMetadata().getVersion().getFriendlyString();

    private static final File configFile = FabricLoader.getInstance().getConfigDir().resolve("summitbot.json").toFile();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).serializeNulls().create();
    private static DiscordBot discordBot = null;
    private static PteroClient pteroClient = null;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register((mc) -> {
            Config config;
            try(FileReader reader = new FileReader(configFile)) {
                LOGGER.info("[{}] Reading config file...", NAME);
                config = gson.fromJson(reader, Config.class);
            } catch (FileNotFoundException e) {
                LOGGER.info("[{}] Generating config file...", NAME);
                config = new Config();

                try(FileWriter writer = new FileWriter(configFile)) {
                    gson.toJson(config, writer);
                } catch (IOException ex) {
                    LOGGER.error("Failed to generate config file!", ex);
                }
            } catch (Exception e) {
                config = new Config();
                LOGGER.error("Failed to read config file!", e);
            }
            pteroClient = new PteroClient(config.serverManagers.pterodactyl());
            discordBot = new DiscordBot(config);
            discordBot.start();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(mc -> {
            LOGGER.info("[{}] Logging out from Discord!", NAME);
            discordBot.logout();
        });
    }

    public static String getModVersion() {
        return modVersion;
    }

    public static PteroClient getPteroClient() {
        return pteroClient;
    }
}
