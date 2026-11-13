package com.galaxyhomes.utils;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Optional Floodgate/Cumulus bridge; Java-only servers do not require these APIs. */
public final class GeyserUtils {
    private GeyserUtils() {}

    public record Button(String text, String response) {}

    public static boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        try {
            Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            return (boolean) api.getMethod("isFloodgatePlayer", UUID.class)
                .invoke(instance, player.getUniqueId());
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    /** Sends a native Bedrock SimpleForm when Floodgate and Cumulus are available. */
    public static void sendForm(
        Player player,
        String id,
        String title,
        String content,
        List<Button> buttons,
        BiConsumer<Player, String> callback
    ) {
        if (player == null || !isBedrockPlayer(player)) return;

        try {
            Class<?> simpleForm = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = simpleForm.getMethod("builder").invoke(null);
            invokeString(builder, "title", title);
            invokeString(builder, "content", content);
            for (Button button : buttons) invokeString(builder, "button", button.text());

            // Cumulus has both Consumer and BiConsumer overloads. Select the Consumer
            // overload explicitly; choosing the first overload silently omitted our handler.
            Method resultHandler = findConsumerMethod(builder.getClass(), "validResultHandler");
            if (resultHandler != null && callback != null) {
                Consumer<Object> handler = response -> {
                    try {
                        int index = ((Number) response.getClass().getMethod("clickedButtonId")
                            .invoke(response)).intValue();
                        if (index >= 0 && index < buttons.size()) {
                            callback.accept(player, buttons.get(index).response());
                        }
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // Invalid or unsupported response; leave the request available to commands.
                    }
                };
                resultHandler.invoke(builder, handler);
            }

            Object form = builder.getClass().getMethod("build").invoke(builder);
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            for (Method method : apiClass.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (method.getName().equals("sendForm") && params.length == 2
                    && params[0] == UUID.class && params[1].isInstance(form)) {
                    method.invoke(api, player.getUniqueId(), form);
                    return;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // Fall through to a usable command-based fallback if form APIs aren't available.
        }

        player.sendMessage(MessageUtils.color("&e" + title + "&7: " + content
            + " &8(Use /tpaccept or /tpdeny)"));
    }

    private static void invokeString(Object target, String methodName, String value) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, String.class);
        method.invoke(target, value);
    }

    private static Method findConsumerMethod(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isAssignableFrom(Consumer.class)) {
                return method;
            }
        }
        return null;
    }
}
