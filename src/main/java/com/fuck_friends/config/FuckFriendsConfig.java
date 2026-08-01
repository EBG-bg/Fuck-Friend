package com.fuck_friends.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FuckFriendsConfig {
    public int resetIntervalTicks = 12000; // 10 minutes * 60 seconds * 20 ticks
    public int maxTpCount = 6;
    public int maxDeathCount = 2;
    
    public String messageReset = "§a传送和死亡限制已重置!";
    public String messageTpLimitReached = "§c你到达了传送上限!";
    public String messageSpectatorMode = "§c你到达了死亡上限!";
    public String messageUnsafeTpDenied = "§c你只能将其他玩家传送到自己身边。";
    public String actionbarSpectatorTime = "§e重生时间: %02d:%02d";
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "fuck_friends.json");
    private static FuckFriendsConfig instance;

    public static FuckFriendsConfig getInstance() {
        if (instance == null) {
            loadConfig();
        }
        return instance;
    }

    public static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                FuckFriendsConfig loaded = GSON.fromJson(reader, FuckFriendsConfig.class);
                instance = loaded == null ? new FuckFriendsConfig() : loaded;
                instance.validate();
            } catch (RuntimeException | IOException e) {
                e.printStackTrace();
                instance = new FuckFriendsConfig();
                saveConfig();
            }
        } else {
            instance = new FuckFriendsConfig();
            saveConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String formatSpectatorTime(long minutes, long seconds) {
        try {
            return String.format(actionbarSpectatorTime, minutes, seconds);
        } catch (RuntimeException e) {
            return String.format("§e重生时间: %02d:%02d", minutes, seconds);
        }
    }

    private void validate() {
        resetIntervalTicks = Math.max(20, resetIntervalTicks);
        maxTpCount = Math.max(0, maxTpCount);
        maxDeathCount = Math.max(0, maxDeathCount);

        FuckFriendsConfig defaults = new FuckFriendsConfig();
        messageReset = valueOrDefault(messageReset, defaults.messageReset);
        messageTpLimitReached = valueOrDefault(messageTpLimitReached, defaults.messageTpLimitReached);
        messageSpectatorMode = valueOrDefault(messageSpectatorMode, defaults.messageSpectatorMode);
        messageUnsafeTpDenied = valueOrDefault(messageUnsafeTpDenied, defaults.messageUnsafeTpDenied);
        actionbarSpectatorTime = valueOrDefault(actionbarSpectatorTime, defaults.actionbarSpectatorTime);

        try {
            String.format(actionbarSpectatorTime, 0L, 0L);
        } catch (RuntimeException e) {
            actionbarSpectatorTime = defaults.actionbarSpectatorTime;
        }
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
