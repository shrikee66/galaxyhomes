package com.galaxyhomes.data;

import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerData {
    private final UUID uuid;
    private final File file;
    private final Map<String, Home> homes = new LinkedHashMap<>();
    private final TPAData tpaData = new TPAData();
    private boolean dirty = false;

    public PlayerData(UUID uuid, File file) {
        this.uuid = uuid;
        this.file = file;
    }

    public UUID getUuid() { return uuid; }
    public Map<String, Home> getHomes() { return homes; }
    public Home getHome(String name) { return homes.get(name.toLowerCase()); }

    public void addHome(Home home) {
        homes.put(home.getName().toLowerCase(), home);
        dirty = true;
    }

    public boolean removeHome(String name) {
        Home removed = homes.remove(name.toLowerCase());
        if (removed != null) {
            dirty = true;
            return true;
        }
        return false;
    }

    public boolean renameHome(String oldName, String newName) {
        Home home = homes.remove(oldName.toLowerCase());
        if (home != null) {
            Home newHome = new Home(newName, home.getWorldUID(),
                    home.getWorldName(), home.getX(), home.getY(),
                    home.getZ(), home.getYaw(), home.getPitch());
            homes.put(newName.toLowerCase(), newHome);
            dirty = true;
            return true;
        }
        return false;
    }

    public int getHomeCount() { return homes.size(); }
    public TPAData getTPAData() { return tpaData; }
    public boolean isDirty() { return dirty; }
    public void setDirty(boolean dirty) { this.dirty = dirty; }
    public File getFile() { return file; }

    public void load() {
        homes.clear();
        tpaData.getBlocked().clear();

        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (config.isConfigurationSection("homes")) {
            for (String key : config.getConfigurationSection("homes").getKeys(false)) {
                String path = "homes." + key;
                String name = config.getString(path + ".name", key);
                UUID worldUID = UUID.fromString(config.getString(
                        path + ".world",
                        "00000000-0000-0000-0000-000000000000"));
                String worldName = config.getString(path + ".world-name", "world");

                double x = config.getDouble(path + ".x", 0);
                double y = config.getDouble(path + ".y", 64);
                double z = config.getDouble(path + ".z", 0);
                float yaw = (float) config.getDouble(path + ".yaw", 0);
                float pitch = (float) config.getDouble(path + ".pitch", 0);

                Home home = new Home(name, worldUID, worldName,
                        x, y, z, yaw, pitch);
                homes.put(name.toLowerCase(), home);
            }
        }

        if (config.isConfigurationSection("tpa")) {
            tpaData.setToggle(config.getBoolean("tpa.toggle", false));

            for (String s : config.getStringList("tpa.blocked")) {
                try {
                    tpaData.addBlocked(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() throws IOException {
        if (!dirty && file.exists()) return;

        YamlConfiguration config = new YamlConfiguration();

        for (Home home : homes.values()) {
            String path = "homes." + home.getName();
            config.set(path + ".name", home.getName());
            config.set(path + ".world", home.getWorldUID().toString());
            config.set(path + ".world-name", home.getWorldName());
            config.set(path + ".x", home.getX());
            config.set(path + ".y", home.getY());
            config.set(path + ".z", home.getZ());
            config.set(path + ".yaw", home.getYaw());
            config.set(path + ".pitch", home.getPitch());
        }

        config.set("tpa.toggle", tpaData.isToggle());

        List<String> blockedStrings = new ArrayList<>();
        for (UUID blocked : tpaData.getBlocked()) {
            blockedStrings.add(blocked.toString());
        }
        config.set("tpa.blocked", blockedStrings);

        file.getParentFile().mkdirs();
        config.save(file);
        dirty = false;
    }

    public void createBackup(File backupFolder, int maxBackups) throws IOException {
        if (!file.exists()) return;

        String timestamp = new java.text.SimpleDateFormat(
                "yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupFile = new File(backupFolder, uuid + "_" + timestamp + ".yml");

        backupFolder.mkdirs();
        java.nio.file.Files.copy(file.toPath(), backupFile.toPath());

        File[] backups = backupFolder.listFiles(
                (dir, name) -> name.startsWith(uuid + "_") && name.endsWith(".yml"));

        if (backups != null && backups.length > maxBackups) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < backups.length - maxBackups; i++) {
                backups[i].delete();
            }
        }
    }
}
