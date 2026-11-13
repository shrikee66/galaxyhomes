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
    private final int totalSeconds;
    private final String type;
    private final TeleportManager.TeleportCallback callback;
    private int remainingSeconds;
    private int tickCounter = 0;
    private final ConfigManager configManager;

    public TeleportTask(TeleportManager manager, Player player, Location destination,
                        Location startLocation, int totalSeconds, String type,
                        TeleportManager.TeleportCallback callback) {
        this.manager = manager;
        this.configManager = manager.getPlugin().getConfigManager();
        this.player = player;
        this.destination = destination;
        this.startLocation = startLocation;
        this.totalSeconds = totalSeconds;
        this.type = type;
        this.callback = callback;
        this.remainingSeconds = totalSeconds;
    }

    @Override
    public void run() {
        if (!player.isOnline() || !player.isValid()) {
            cancel();
            manager.cancelTeleport(player.getUniqueId(), "teleport-cancelled-command");
            return;
        }

        if (configManager.isCancelOnMove()) {
            double threshold = configManager.getMoveThreshold();
            if (player.getLocation().distanceSquared(startLocation) > threshold * threshold) {
                cancel();
                manager.cancelTeleport(player.getUniqueId(), "teleport-cancelled-move");
                return;
            }
        }

        if (configManager.isCountdownEnabled() && remainingSeconds > 0) {
            String format = configManager.getCountdownFormat();
            String message = format.replace("{time}", String.valueOf(remainingSeconds));
            MessageUtils.sendRawMessage(player, message);
        }

        if (remainingSeconds > 0) {
            SoundUtils.playSound(player, configManager.getSoundConfig("countdown"));
        }

        if (tickCounter % Math.max(1, configManager.getWarmupParticleConfig().intervalTicks) == 0) {
            ParticleUtils.spawnParticles(player.getLocation(), configManager.getWarmupParticleConfig());
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
