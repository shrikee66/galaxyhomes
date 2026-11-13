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

    public record Button(String text, String response, String imageType, String imageData) {
        public Button(String text, String response) {
            this(text, response, null, null);
        }
    }

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

    /** Sends a native Bedrock SimpleForm and routes selected button responses. */
    public static void sendForm(Player player, String id, String title, String content,
                                 List<Button> buttons, BiConsumer<Player, String> callback) {
        if (player == null || !isBedrockPlayer(player)) return;

        try {
            Class<?> simpleForm = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = simpleForm.getMethod("builder").invoke(null);
            builder.getClass().getMethod("title", String.class).invoke(builder, title);
            builder.getClass().getMethod("content", String.class).invoke(builder, content);

            for (Button button : buttons) {
                addButton(builder, button);
            }

            Method handlerMethod = findConsumerMethod(builder.getClass(), "validResultHandler");
            if (handlerMethod != null && callback != null) {
                Consumer<Object> handler = response -> {
                    try {
                        int index = ((Number) response.getClass().getMethod("clickedButtonId")
                            .invoke(response)).intValue();
                        if (index >= 0 && index < buttons.size()) {
                            callback.accept(player, buttons.get(index).response());
                        }
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // Invalid response: leave the operation available through normal commands.
                    }
                };
                handlerMethod.invoke(builder, handler);
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
            throw new NoSuchMethodException("FloodgateApi.sendForm(UUID, Form) not found");
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            player.sendMessage(MessageUtils.color("&e" + title + "&7: " + content
                + " &8(Use the in-game commands if forms are unavailable)"));
        }
    }

    private static void addButton(Object builder, Button button) throws ReflectiveOperationException {
        if (button.imageType() == null || button.imageData() == null) {
            builder.getClass().getMethod("button", String.class).invoke(builder, button.text());
            return;
        }

        try {
            Class<?> imageTypeClass = Class.forName("org.geysermc.cumulus.util.FormImage$Type");
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object imageType = Enum.valueOf((Class<? extends Enum>) imageTypeClass.asSubclass(Enum.class), button.imageType());
            builder.getClass().getMethod("button", String.class, imageTypeClass, String.class)
                .invoke(builder, button.text(), imageType, button.imageData());
        } catch (ReflectiveOperationException | IllegalArgumentException unsupportedImageApi) {
            // Avoid Bedrock's missing-texture black/pink icon: use a plain text button
            // when a server's Cumulus version doesn't support the requested image API.
            builder.getClass().getMethod("button", String.class).invoke(builder, button.text());
        }
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
