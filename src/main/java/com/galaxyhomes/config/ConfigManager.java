
package com.galaxyhomes.config;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final GalaxyHomes plugin;
    private FileConfiguration config;
    private File configFile;

    public ConfigManager(GalaxyHomes plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reload() {
        loadConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not save config.yml", e);
        }
    }

    public int getWarmup(String type) {
        return config.getInt("teleport.warmups." + type, 3);
    }

    public int getCooldownSeconds() {
        return config.getInt("teleport.cooldown-seconds", 5);
    }

    public boolean isCountdownEnabled() {
        return config.getBoolean("teleport.countdown.enabled", true);
    }

    public String getCountdownFormat() {
        return config.getString("teleport.countdown.format", "&aTeleporting in &e{time}&as...");
    }

    public boolean isCancelOnMove() {
        return config.getBoolean("teleport.countdown.cancel-on-move", true);
    }

    public boolean isCancelOnDamage() {
        return config.getBoolean("teleport.countdown.cancel-on-damage", true);
    }

    public double getMoveThreshold() {
        return config.getDouble("teleport.countdown.move-threshold", 0.5);
    }

    public int getTPAExpireSeconds() {
        return config.getInt("tpa.expire-seconds", 60);
    }

    public boolean isCrossPlatformEnabled() {
        return config.getBoolean("tpa.cross-platform", true);
    }

    public int getMaxOutgoingRequests() {
        return config.getInt("tpa.max-outgoing", 1);
    }

    public int getMaxIncomingRequests() {
        return config.getInt("tpa.max-incoming", 5);
    }

    public int getMaxHomes() {
        return config.getInt("homes.max-homes", -1);
    }

    public String getDefaultHomeName() {
        return config.getString("homes.default-name", "home");
    }

    public boolean isAllowSpacesInName() {
        return config.getBoolean("homes.allow-spaces", true);
    }

    public int getMaxNameLength() {
        return config.getInt("homes.max-name-length", 32);
    }

    public org.bukkit.Location getSpawnLocation() {
        String worldName = config.getString("spawn.world", "");
        if (worldName.isEmpty()) return null;

        org.bukkit.World world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;

        return new org.bukkit.Location(world,
                config.getDouble("spawn.x", 0),
                config.getDouble("spawn.y", 64),
                config.getDouble("spawn.z", 0),
                (float) config.getDouble("spawn.yaw", 0),
                (float) config.getDouble("spawn.pitch", 0));
    }

    public void setSpawnLocation(org.bukkit.Location location) {
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        saveConfig();
    }

    public String getGUITitle() {
        return config.getString("gui.title", "&8&lGalaxy &b&lHomes");
    }

    public int getGUIRowsPerPage() {
        return Math.min(Math.max(config.getInt("gui.rows-per-page", 4), 1), 6);
    }

    public String getHomeItem() {
        return config.getString("gui.home-item", "WRITTEN_BOOK");
    }

    public String getManageItem() {
        return config.getString("gui.manage-item", "OAK_SIGN");
    }

    public String getPrevPageItem() {
        return config.getString("gui.prev-page-item", "ARROW");
    }

    public String getNextPageItem() {
        return config.getString("gui.next-page-item", "ARROW");
    }

    public String getCloseItem() {
        return config.getString("gui.close-item", "BARRIER");
    }

    public boolean isFillEmpty() {
        return config.getBoolean("gui.fill-empty", true);
    }

    public String getFillItem() {
        return config.getString("gui.fill-item", "GRAY_STAINED_GLASS_PANE");
    }

    public boolean isAnimationEnabled() {
        return config.getBoolean("gui.animation.enabled", true);
    }

    public String getAnimationType() {
        return config.getString("gui.animation.type", "SLIDE");
    }

    public int getAnimationDuration() {
        return config.getInt("gui.animation.duration-ticks", 10);
    }

    public boolean isBedrockEnabled() {
        return config.getBoolean("bedrock.enabled", true);
    }

    public String getBedrockFormTitle() {
        return config.getString("bedrock.form-title", "§8§lGalaxy §b§lHomes");
    }

    public boolean isUsePlayerHeads() {
        return config.getBoolean("bedrock.use-player-heads", true);
    }

    public int getPickerPageSize() {
        return config.getInt("bedrock.picker-page-size", 10);
    }

    public String getConfirmButton() {
        return config.getString("bedrock.confirm-button", "GREEN_WOOL");
    }

    public String getCancelButton() {
        return config.getString("bedrock.cancel-button", "RED_WOOL");
    }

    public boolean isAutoShowRequests() {
        return config.getBoolean("bedrock.auto-show-requests", true);
    }

    public boolean isSoundsEnabled() {
        return config.getBoolean("sounds.enabled", true);
    }

    public SoundConfig getSoundConfig(String path) {
        if (!config.isConfigurationSection("sounds." + path)) {
            return new SoundConfig("UI_BUTTON_CLICK", 0.5f, 1.0f);
        }

        return new SoundConfig(
                config.getString("sounds." + path + ".sound", "UI_BUTTON_CLICK"),
                (float) config.getDouble("sounds." + path + ".volume", 0.5),
                (float) config.getDouble("sounds." + path + ".pitch", 1.0)
        );
    }

    public boolean isParticlesEnabled() {
        return config.getBoolean("particles.enabled", true);
    }

    public ParticleConfig getParticleConfig(String path) {
        if (!config.isConfigurationSection("particles." + path)) {
            return new ParticleConfig("END_ROD", 20, 0.5, 1.0, 0.5, 0.1);
        }

        return new ParticleConfig(
                config.getString("particles." + path + ".type", "END_ROD"),
                config.getInt("particles." + path + ".count", 20),
                config.getDouble("particles." + path + ".offset-x", 0.5),
                config.getDouble("particles." + path + ".offset-y", 1.0),
                config.getDouble("particles." + path + ".offset-z", 0.5),
                config.getDouble("particles." + path + ".speed", 0.1)
        );
    }

    public ParticleConfig getWarmupParticleConfig() {
        if (!config.isConfigurationSection("particles.warmup")) {
            return new ParticleConfig("VILLAGER_HAPPY", 5, 0.3, 0.5, 0.3, 0.05, 20);
        }

        return new ParticleConfig(
                config.getString("particles.warmup.type", "VILLAGER_HAPPY"),
                config.getInt("particles.warmup.count", 5),
                config.getDouble("particles.warmup.offset-x", 0.3),
                config.getDouble("particles.warmup.offset-y", 0.5),
                config.getDouble("particles.warmup.offset-z", 0.3),
                config.getDouble("particles.warmup.speed", 0.05),
                config.getInt("particles.warmup.interval-ticks", 20)
        );
    }

    public String getMessage(String key) {
        return config.getString("messages." + key, "");
    }

    public String getPrefix() {
        return config.getString("messages.prefix", "&8[&bGalaxyHomes&8] ");
    }

    public boolean isBackupOnSave() {
        return config.getBoolean("database.backup-on-save", true);
    }

    public int getMaxBackups() {
        return config.getInt("database.max-backups", 10);
    }

    public static class SoundConfig {
        public final String sound;
        public final float volume;
        public final float pitch;

        public SoundConfig(String sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    public static class ParticleConfig {
        public final String type;
        public final int count;
        public final double offsetX;
        public final double offsetY;
        public final double offsetZ;
        public final double speed;
        public final int intervalTicks;

        public ParticleConfig(String type, int count, double offsetX,
                              double offsetY, double offsetZ, double speed) {
            this(type, count, offsetX, offsetY, offsetZ, speed, 0);
        }

        public ParticleConfig(String type, int count, double offsetX,
                              double offsetY, double offsetZ, double speed,
                              int intervalTicks) {
            this.type = type;
            this.count = count;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.speed = speed;
            this.intervalTicks = intervalTicks;
        }
    }
}
