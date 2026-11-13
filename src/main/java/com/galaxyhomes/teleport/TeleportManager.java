package com.galaxyhomes.teleport;

import com.galaxyhomes.GalaxyHomes;
import com.galaxyhomes.config.ConfigManager;
import com.galaxyhomes.utils.MessageUtils;
import com.galaxyhomes.utils.ParticleUtils;
import com.galaxyhomes.utils.SoundUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportManager {

    private final GalaxyHomes plugin;
    private final ConfigManager configManager;
    private final Map<UUID, BukkitTask> pendingTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public GalaxyHomes getPlugin() {
        return plugin;
    }

    public void taskFinished(UUID uuid) {
        pendingTeleports.remove(uuid);
    }

    public TeleportManager(GalaxyHomes plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public boolean startTeleport(Player player, Location destination, String type, TeleportCallback callback) {
        UUID uuid = player.getUniqueId();

        if (pendingTeleports.containsKey(uuid)) {
            MessageUtils.sendMessage(player, "teleport-cancelled-command");
            return false;
        }

        long cooldownEnd = cooldowns.getOrDefault(uuid, 0L);
        long now = System.currentTimeMillis();

        if (now < cooldownEnd) {
            long remaining = (cooldownEnd - now) / 1000 + 1;
            MessageUtils.sendMessage(player, "teleport-cooldown", Map.of("time", String.valueOf(remaining)));
            return false;
        }

        int warmup = configManager.getWarmup(type);

        if (warmup <= 0) {
            performTeleport(player, destination, callback);
            setCooldown(uuid);
            return true;
        }

        Location startLocation = player.getLocation().clone();

        TeleportTask task = new TeleportTask(
                this, player, destination, startLocation, warmup, type, callback
        );

        BukkitTask bukkitTask = task.runTaskTimer(plugin, 0L, 20L);
        pendingTeleports.put(uuid, bukkitTask);
        return true;
    }

    public void cancelTeleport(UUID uuid, String reason) {
        BukkitTask task = pendingTeleports.remove(uuid);

        if (task != null) {
            task.cancel();
            Player player = Bukkit.getPlayer(uuid);

            if (player != null && player.isOnline()) {
                MessageUtils.sendMessage(player, reason);
            }
        }
    }

    public boolean isTeleporting(UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }

    public void cancelAllTeleports() {
        for (BukkitTask task : pendingTeleports.values()) {
            task.cancel();
        }
        pendingTeleports.clear();
    }

    public void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis() + configManager.getCooldownSeconds() * 1000L);
    }

    public boolean hasCooldown(UUID uuid) {
        Long cooldownEnd = cooldowns.get(uuid);
        return cooldownEnd != null && System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldown(UUID uuid) {
        Long cooldownEnd = cooldowns.get(uuid);
        if (cooldownEnd == null) return 0;
        return Math.max(0, (cooldownEnd - System.currentTimeMillis()) / 1000 + 1);
    }

    public void performTeleport(Player player, Location destination, TeleportCallback callback) {
        SoundUtils.playSound(player, configManager.getSoundConfig("success"));
        ParticleUtils.spawnParticles(destination, configManager.getParticleConfig("destination"));

        player.teleportAsync(destination).thenRun(() -> {
            if (callback != null) {
                callback.onComplete(true);
            }
        }).exceptionally(throwable -> {
            if (callback != null) {
                callback.onComplete(false);
            }
            return null;
        });
    }

    public void onPlayerMove(Player player, Location from, Location to) {
        if (!configManager.isCancelOnMove()) return;

        UUID uuid = player.getUniqueId();
        if (!pendingTeleports.containsKey(uuid)) return;

        double threshold = configManager.getMoveThreshold();

        if (from.distanceSquared(to) > threshold * threshold) {
            cancelTeleport(uuid, "teleport-cancelled-move");
        }
    }

    public void onPlayerDamage(Player player) {
        if (!configManager.isCancelOnDamage()) return;

        UUID uuid = player.getUniqueId();

        if (pendingTeleports.containsKey(uuid)) {
            cancelTeleport(uuid, "teleport-cancelled-damage");
        }
    }

    public void onPlayerCommand(Player player) {
        UUID uuid = player.getUniqueId();

        if (pendingTeleports.containsKey(uuid)) {
            cancelTeleport(uuid, "teleport-cancelled-command");
        }
    }

    public interface TeleportCallback {
        void onComplete(boolean success);
    }
}
