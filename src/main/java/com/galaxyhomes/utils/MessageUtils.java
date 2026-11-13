package com.galaxyhomes.utils;

import com.galaxyhomes.GalaxyHomes;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import java.util.Map;

public final class MessageUtils {
    private static GalaxyHomes plugin;

    private MessageUtils() {}

    public static void reload(GalaxyHomes instance) {
        plugin = instance;
    }

    public static String getMessage(String key) {
        return getMessage(key, Map.of());
    }

    public static String getMessage(String key, Map<String, String> placeholders) {
        if (plugin == null) return key;

        String configKey = switch (key) {
            case "tpahere-sent" -> "tpa-here-sent";
            case "tpahere-received" -> "tpa-here-received";
            case "tpa-no-pending-requests" -> "tpa-no-request";
            case "tpa-player-offline" -> "player-offline";
            default -> key;
        };

        String value = plugin.getConfigManager().getMessage(configKey);

        if (value == null || value.isBlank()) {
            value = switch (key) {
                case "tpa-accepted-target" ->
                        "&aYour request to &e{player} &awas accepted.";
                case "tpahere-accepted" ->
                        "&aYou accepted the request from &e{player}&a.";
                case "tpahere-accepted-target" ->
                        "&e{player} &aaccepted your teleport-here request.";
                case "tpa-denied-target" ->
                        "&aYou denied the request from &e{player}&a.";
                case "tpa-expired-target" ->
                        "&cThe request from &e{player} &cexpired.";
                case "tpa-cancelled-target" ->
                        "&aYour outgoing request was cancelled.";
                case "tpa-target-offline" ->
                        "&cYour request to &e{player} &cwas cancelled because they went offline.";
                case "tpa-teleport-failed" ->
                        "&cThe teleport could not be started.";
                default -> "&cMissing message: " + key;
            };
        }

        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            value = value.replace("{" + e.getKey() + "}", e.getValue());
        }

        return color(plugin.getConfigManager().getPrefix() + value);
    }

    public static void sendMessage(CommandSender sender, String key) {
        sender.sendMessage(getMessage(key));
    }

    public static void sendMessage(CommandSender sender, String key,
                                   Map<String, String> placeholders) {
        sender.sendMessage(getMessage(key, placeholders));
    }

    public static void sendRawMessage(CommandSender sender, String message) {
        sender.sendMessage(color(message));
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&',
                text == null ? "" : text);
    }
}
