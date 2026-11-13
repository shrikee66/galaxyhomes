package com.galaxyhomes.utils;

import org.bukkit.entity.Player;
import java.util.List;
import java.util.function.BiConsumer;

/** Optional Floodgate bridge. Reflection keeps Java-only servers free of a hard dependency. */
public final class GeyserUtils {
    private GeyserUtils() {}

    public record Button(String text, String response) {}

    public static boolean isBedrockPlayer(Player player) {
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);

            return (boolean) api.getMethod(
                "isFloodgatePlayer",
                java.util.UUID.class
            ).invoke(instance, player.getUniqueId());

        } catch (Throwable ignored) {
            return false;
        }
    }

    /** No-op fallback when Cumulus is absent; Java commands remain fully usable. */
    public static void sendForm(
        Player player,
        String id,
        String title,
        String content,
        List<Button> buttons,
        BiConsumer<Player, String> callback
    ) {
        if (player != null && isBedrockPlayer(player)) {
            player.sendMessage(
                MessageUtils.color("&e" + title + "&7: " + content
                    + " &8(Use /tpaccept or /tpdeny)")
            );
        }
    }
}
