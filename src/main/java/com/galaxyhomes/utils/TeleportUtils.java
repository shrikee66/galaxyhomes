package com.galaxyhomes.utils;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.entity.Player;
import java.util.Map;

public final class TeleportUtils {
    private TeleportUtils() {}

    public static boolean teleport(Player player,
                                   org.bukkit.Location location,
                                   String messageKey,
                                   Map<String, String> placeholders) {
        if (player == null || location == null || location.getWorld() == null) {
            return false;
        }

        MessageUtils.sendMessage(player, messageKey, placeholders);

        return GalaxyHomes.getInstance()
                .getTeleportManager()
                .startTeleport(player, location, "tpa", null);
    }
}
