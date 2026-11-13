package com.galaxyhomes.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Optional Floodgate/Cumulus bridge for native Bedrock forms. */
public final class GeyserUtils {
    private GeyserUtils() {}

    public record Button(String text, String response, String imageUrl) {
        public Button(String text, String response) { this(text, response, null); }
    }

    public static boolean isBedrockPlayer(Player player) {
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return (boolean) api.getMethod("isFloodgatePlayer", java.util.UUID.class)
                .invoke(instance, player.getUniqueId());
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void sendForm(Player player, String id, String title, String content,
                                List<Button> buttons, BiConsumer<Player, String> callback) {
        if (player == null || !isBedrockPlayer(player)) return;
        SimpleForm.Builder builder = SimpleForm.builder().title(title).content(content);
        for (Button button : buttons) {
            if (button.imageUrl() == null || button.imageUrl().isBlank()) {
                builder.button(button.text());
            } else {
                builder.button(button.text(), FormImage.Type.URL, button.imageUrl());
            }
        }
        builder.validResultHandler(response -> {
            int index = response.clickedButtonId();
            if (index >= 0 && index < buttons.size()) callback.accept(player, buttons.get(index).response());
        });
        builder.closedResultHandler(() -> { /* Closing the form intentionally ignores it. */ });
        send(player, builder.build());
    }

    public static void sendInput(Player player, String title, String prompt, String placeholder,
                                 BiConsumer<Player, String> callback) {
        if (player == null || !isBedrockPlayer(player)) return;
        CustomForm form = CustomForm.builder()
            .title(title)
            .input(prompt, placeholder)
            .validResultHandler(response -> callback.accept(player, response.asInput(0)))
            .build();
        send(player, form);
    }

    public static void openPlayerPicker(Player player, boolean here,
                                        BiConsumer<Player, Player> callback) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.removeIf(candidate -> candidate.getUniqueId().equals(player.getUniqueId()));
        online.sort(java.util.Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        if (online.isEmpty()) {
            player.sendMessage(MessageUtils.color("&cNo other players are online."));
            return;
        }
        List<Button> buttons = online.stream()
            .map(target -> new Button(target.getName(), target.getUniqueId().toString(),
                "https://mc-heads.net/avatar/" + target.getUniqueId() + "/64"))
            .toList();
        sendForm(player, here ? "tpahere-picker" : "tpa-picker",
            "Available online players", "Select a player to send a " + (here ? "/tpahere" : "/tpa") + " request.",
            buttons, (viewer, response) -> {
                try {
                    Player target = Bukkit.getPlayer(java.util.UUID.fromString(response));
                    if (target == null) viewer.sendMessage(MessageUtils.color("&cThat player is no longer online."));
                    else callback.accept(viewer, target);
                } catch (IllegalArgumentException ignored) {
                    viewer.sendMessage(MessageUtils.color("&cInvalid player selection."));
                }
            });
    }

    private static void send(Player player, Object form) {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            for (java.lang.reflect.Method method : apiClass.getMethods()) {
                if (method.getName().equals("sendForm") && method.getParameterCount() == 2) {
                    method.invoke(api, player.getUniqueId(), form);
                    return;
                }
            }
            player.sendMessage(MessageUtils.color("&cBedrock forms are unavailable. Please use the Java menu or commands."));
        } catch (Throwable error) {
            player.sendMessage(MessageUtils.color("&cCould not open the Bedrock form. Check Floodgate/Cumulus installation."));
        }
    }
}
