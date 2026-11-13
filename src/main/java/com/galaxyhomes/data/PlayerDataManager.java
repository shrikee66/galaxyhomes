package com.galaxyhomes.data;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.config.ConfigManager;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerDataManager {
    private final GalaxyHomes plugin;
    private final ConfigManager configManager;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    public PlayerDataManager(GalaxyHomes plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, u -> {
            File file = new File(plugin.getPlayersFolder(), u + ".yml");
            PlayerData data = new PlayerData(u, file);
            data.load();
            return data;
        });
    }

    public void loadAllPlayerData() {
        File folder = plugin.getPlayersFolder();
        if (!folder.exists()) {
            folder.mkdirs();
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();
            if (name.length() > 4) {
                String uuidStr = name.substring(0, name.length() - 4);
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    PlayerData data = new PlayerData(uuid, file);
                    data.load();
                    playerDataMap.put(uuid, data);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid player data file name: " + name);
                }
            }
        }
    }

    public void saveAllPlayerData() {
        for (PlayerData data : playerDataMap.values()) {
            if (data.isDirty()) {
                try {
                    if (configManager.isBackupOnSave()) {
                        data.createBackup(plugin.getBackupFolder(), configManager.getMaxBackups());
                    }
                    data.save();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Failed to save player data for " + data.getUuid(), e);
                }
            }
        }
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null && data.isDirty()) {
            try {
                if (configManager.isBackupOnSave()) {
                    data.createBackup(plugin.getBackupFolder(), configManager.getMaxBackups());
                }
                data.save();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to save player data for " + uuid, e);
            }
        }
    }

    public void unloadPlayerData(UUID uuid) {
        PlayerData data = playerDataMap.remove(uuid);
        if (data != null && data.isDirty()) {
            try {
                if (configManager.isBackupOnSave()) {
                    data.createBackup(plugin.getBackupFolder(), configManager.getMaxBackups());
                }
                data.save();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to save player data for " + uuid, e);
            }
        }
    }

    public int getLoadedPlayerCount() {
        return playerDataMap.size();
    }

    public Collection<PlayerData> getAllPlayerData() {
        return Collections.unmodifiableCollection(playerDataMap.values());
    }

    public long getTotalHomeCount() {
        return playerDataMap.values().stream()
                .mapToLong(PlayerData::getHomeCount).sum();
    }
}
