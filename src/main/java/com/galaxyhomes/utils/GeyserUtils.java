package com.galaxyhomes.utils;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
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
                UUID.class
            ).invoke(instance, player.getUniqueId());

        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Best-effort Bedrock form sender.
     * If Floodgate/Cumulus is present, we try to open a real form; otherwise we fall back to a text message.
     */
    public static void sendForm(
        Player player,
        String id,
        String title,
        String content,
        List<Button> buttons,
        BiConsumer<Player, String> callback
    ) {
        if (player == null) return;

        try {
            if (sendFloodgateForm(player, title, content, buttons, callback)) {
                return;
            }
        } catch (Throwable ignored) {
            // Ignore and fall back below.
        }

        try {
            if (sendCumulusForm(player, title, content, buttons, callback)) {
                return;
            }
        } catch (Throwable ignored) {
            // Ignore and fall back below.
        }

        if (isBedrockPlayer(player)) {
            player.sendMessage(
                MessageUtils.color("&e" + title + "&7: " + content + " &8(Use /tpaccept or /tpdeny)")
            );
        }
    }

    private static boolean sendFloodgateForm(
        Player player,
        String title,
        String content,
        List<Button> buttons,
        BiConsumer<Player, String> callback
    ) throws Exception {
        Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
        Object instance = apiClass.getMethod("getInstance").invoke(null);

        Object form = createFloodgateSimpleForm(title, content, buttons, callback);
        if (form == null) {
            return false;
        }

        for (Method method : apiClass.getMethods()) {
            Class<?>[] params = method.getParameterTypes();
            if (method.getName().equals("sendForm") && params.length == 2) {
                Class<?> first = params[0];
                Class<?> second = params[1];
                if (UUID.class.isAssignableFrom(first) && second.getName().contains("Form")) {
                    method.invoke(instance, player.getUniqueId(), form);
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean sendCumulusForm(
        Player player,
        String title,
        String content,
        List<Button> buttons,
        BiConsumer<Player, String> callback
    ) throws Exception {
        Class<?> simpleFormClass = Class.forName("org.geysermc.cumulus.form.SimpleForm");
        Object form = simpleFormClass.getDeclaredConstructor().newInstance();

        invokeIfPresent(form, "setTitle", title);
        invokeIfPresent(form, "setContent", content);

        if (buttons != null) {
            for (Button button : buttons) {
                invokeIfPresent(form, "addButton", button.text());
            }
        }

        Method sendMethod = null;
        for (Method method : Class.forName("org.geysermc.cumulus.form.Form").getMethods()) {
            if (method.getName().equals("send") || method.getName().equals("sendTo")) {
                sendMethod = method;
                break;
            }
        }

        if (sendMethod == null) {
            return false;
        }

        sendMethod.invoke(form, player);
        return true;
    }

    private static Object createFloodgateSimpleForm(
        String title,
        String content,
        List<Button> buttons,
        BiConsumer<Player, String> callback
    ) throws Exception {
        try {
            Class<?> simpleFormClass = Class.forName("org.geysermc.floodgate.api.form.SimpleForm");
            Object form = simpleFormClass.getDeclaredConstructor().newInstance();

            invokeIfPresent(form, "setTitle", title);
            invokeIfPresent(form, "setContent", content);

            if (buttons != null) {
                for (Button button : buttons) {
                    invokeIfPresent(form, "addButton", button.text());
                }
            }

            if (callback != null) {
                hookFloodgateResponse(form, callback);
            }

            return form;
        } catch (Throwable ignored) {
            // Try builder path below.
        }

        try {
            Class<?> builderClass = Class.forName("org.geysermc.floodgate.api.form.SimpleForm$Builder");
            Object builder = builderClass.getMethod("builder").invoke(null);
            builderClass.getMethod("title", String.class).invoke(builder, title);
            builderClass.getMethod("content", String.class).invoke(builder, content);

            if (buttons != null) {
                for (Button button : buttons) {
                    builderClass.getMethod("button", String.class).invoke(builder, button.text());
                }
            }

            if (callback != null) {
                try {
                    builderClass.getMethod("responseHandler", java.util.function.BiConsumer.class).invoke(builder, (java.util.function.BiConsumer<Object, Object>) (response, data) -> {
                        if (response instanceof Player p) {
                            callback.accept(p, String.valueOf(data));
                        }
                    });
                } catch (Throwable ignored2) {
                    // No-op: some versions do not expose this builder hook.
                }
            }

            return builderClass.getMethod("build").invoke(builder);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void hookFloodgateResponse(Object form, BiConsumer<Player, String> callback) {
        if (callback == null || form == null) return;

        for (String methodName : List.of("setResponseHandler", "setCallback", "setOnResponse")) {
            try {
                Method method = form.getClass().getMethod(methodName, java.util.function.BiConsumer.class);
                method.invoke(form, (java.util.function.BiConsumer<Object, Object>) (response, data) -> {
                    if (response instanceof Player player) {
                        callback.accept(player, String.valueOf(data));
                    }
                });
                return;
            } catch (Throwable ignored) {
                // Try next candidate.
            }
        }
    }

    private static void invokeIfPresent(Object target, String methodName, String value) throws Exception {
        for (Method method : target.getClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1 && method.getParameterTypes()[0] == String.class) {
                method.invoke(target, value);
                return;
            }
        }
    }
}
