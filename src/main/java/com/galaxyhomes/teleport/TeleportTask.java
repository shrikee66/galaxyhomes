package com.galaxyhomes.teleport;

import com.galaxyhomes.config.ConfigManager;
import com.galaxyhomes.utils.MessageUtils;
import com.galaxyhomes.utils.ParticleUtils;
import com.galaxyhomes.utils.SoundUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TeleportTask extends BukkitRunnable {

    private final TeleportManager manager;
    private final Player player;
    private final Location destination;
    private final Location startLocation;
    private final String type;
    private final TeleportManager.TeleportCallback callback;
    private int remainingSeconds;
    private int tickCounter;
    private final ConfigManager configManager;

    public TeleportTask(TeleportManager manager, Player player, Location destination,
                        Location startLocation, int totalSeconds, String type,
                        TeleportManager.TeleportCallback callback) {
        this.manager = manager;
        this.configManager = manager.getPlugin().getConfigManager();
        this.player = player;
        this.destination = destination;
        this.startLocation = startLocation;
        this.type = type;
        this.callback = callback;
        this.remainingSeconds = totalSeconds;
    }

    @Override
    public void run() {
        if (!player.isOnline() || !player.isValid()) {
            manager.cancelTeleport(player.getUniqueId(), "teleport-cancelled-command");
            return;
        }

        if (configManager.isCancelOnMove()) {
            double threshold = configManager.getMoveThreshold();
            Location current = player.getLocation();
            if (current.getWorld() != startLocation.getWorld()
                || current.distanceSquared(startLocation) > threshold * threshold) {
                // Let the manager cancel the task so it can also notify, play sound, and spawn particles.
                manager.cancelTeleport(player.getUniqueId(), "teleport-cancelled-move");
                return;
            }
        }

        if (configManager.isCountdownEnabled() && remainingSeconds > 0) {
            String message = configManager.getCountdownFormat().replace("{time}", String.valueOf(remainingSeconds));
            MessageUtils.sendRawMessage(player, message);
        }
        if (remainingSeconds > 0) SoundUtils.playSound(player, configManager.getSoundConfig("countdown"));

        ConfigManager.ParticleConfig warmupParticles = configManager.getWarmupParticleConfig();
        if (tickCounter % Math.max(1, warmupParticles.intervalTicks) == 0) {
            ParticleUtils.spawnParticles(player.getLocation(), warmupParticles);
        }

        tickCounter++;
        remainingSeconds--;
        if (remainingSeconds < 0) {
            cancel();
            manager.performTeleport(player, destination, callback);
            manager.setCooldown(player.getUniqueId());
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        manager.taskFinished(player.getUniqueId());
    }
}
