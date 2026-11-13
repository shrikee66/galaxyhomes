
package com.galaxyhomes;

import com.galaxyhomes.commands.*;
import com.galaxyhomes.config.ConfigManager;
import com.galaxyhomes.data.PlayerDataManager;
import com.galaxyhomes.listeners.InventoryListener;
import com.galaxyhomes.listeners.PlayerListener;
import com.galaxyhomes.teleport.TeleportManager;
import com.galaxyhomes.tpa.TPAManager;
import com.galaxyhomes.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.logging.Level;

public class GalaxyHomes extends JavaPlugin {

    private static GalaxyHomes instance;

    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private TeleportManager teleportManager;
    private TPAManager tpaManager;
    private BukkitTask autoSaveTask;

    @Override
    public void onEnable() {
        instance = this;
        MessageUtils.reload(this);

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        playerDataManager = new PlayerDataManager(this);
        teleportManager = new TeleportManager(this);
        tpaManager = new TPAManager(this);

        registerCommands();
        registerListeners();

        playerDataManager.loadAllPlayerData();
        startAutoSaveTask();
        registerPlaceholders();

        getLogger().info("GalaxyHomes v" + getDescription().getVersion() + " has been enabled!");
        getLogger().info("Loaded " + playerDataManager.getLoadedPlayerCount() + " player data files.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAllPlayerData();

        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        if (teleportManager != null) teleportManager.cancelAllTeleports();

        if (tpaManager != null) tpaManager.cleanup();

        getLogger().info("GalaxyHomes has been disabled!");
    }

    private void registerCommands() {
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("homes").setExecutor(new HomesCommand(this));
        getCommand("delhome").setExecutor(new DelHomeCommand(this));

        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));

        getCommand("tpa").setExecutor(new TPACommand(this));
        getCommand("tpahere").setExecutor(new TPAHereCommand(this));
        getCommand("tpaccept").setExecutor(new TPAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TPADenyCommand(this));
        getCommand("tpacancel").setExecutor(new TPACancelCommand(this));
        getCommand("tpatoggle").setExecutor(new TPAToggleCommand(this));
        getCommand("tpasettings").setExecutor(new TPASettingsCommand(this));

        getCommand("crossplayhomes").setExecutor(new AdminCommand(this));

        getCommand("sethome").setTabCompleter(new HomeTabCompleter(this));
        getCommand("home").setTabCompleter(new HomeTabCompleter(this));
        getCommand("delhome").setTabCompleter(new HomeTabCompleter(this));
        getCommand("tpa").setTabCompleter(new PlayerTabCompleter(this));
        getCommand("tpahere").setTabCompleter(new PlayerTabCompleter(this));
        getCommand("crossplayhomes").setTabCompleter(new AdminTabCompleter(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    private void startAutoSaveTask() {
        int interval = configManager.getConfig().getInt("homes.auto-save-interval", 5);

        if (interval > 0) {
            autoSaveTask = getServer().getScheduler().runTaskTimerAsynchronously(
                this,
                () -> {
                    if (playerDataManager != null) {
                        playerDataManager.saveAllPlayerData();
                    }
                },
                interval * 60 * 20L,
                interval * 60 * 20L
            );
        }
    }

    private void registerPlaceholders() {
        // Optional PlaceholderAPI integration.
    }

    public static GalaxyHomes getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public TPAManager getTPAManager() {
        return tpaManager;
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        MessageUtils.reload(this);
        getLogger().info("Configuration reloaded!");
    }

    public File getPlayersFolder() {
        File folder = new File(
            getDataFolder(),
            configManager.getConfig().getString("database.folder", "players")
        );

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    public File getBackupFolder() {
        File folder = new File(
            getDataFolder(),
            configManager.getConfig().getString("database.backup-folder", "backups")
        );

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }
}
