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
        public Button(String text, String response) { this(text, response, null, null); }
        public Button(String text, String response, String imageType, String imageData) {
            this.text = text;
            this.response = response;
            this.imageType = imageType;
            this.imageData = imageData;
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

    /** Sends a native Bedrock SimpleForm with optional item icons and routes button responses. */
    public static void sendForm(Player player, String id, String title, String content,
                                 List<Button> buttons, BiConsumer<Player, String> callback) {
        if (player == null || !isBedrockPlayer(player)) return;
        try {
            Class<?> simpleForm = Class.forName("org.geysermc.cumulus.form.SimpleForm");
            Object builder = simpleForm.getMethod("builder").invoke(null);
            invokeString(builder, "title", title);
            invokeString(builder, "content", content);
            for (Button button : buttons) addButton(builder, button);

            Method handlerMethod = findConsumerMethod(builder.getClass(), "validResultHandler");
            if (handlerMethod != null && callback != null) {
                Consumer<Object> handler = response -> {
                    try {
                        int index = ((Number) response.getClass().getMethod("clickedButtonId").invoke(response)).intValue();
                        if (index >= 0 && index < buttons.size()) callback.accept(player, buttons.get(index).response());
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // Invalid response: leave pending requests/homes untouched.
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
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            player.sendMessage(MessageUtils.color("&e" + title + "&7: " + content + " &8(Use the in-game commands if forms are unavailable)"));
        }
    }

    private static void addButton(Object builder, Button button) throws ReflectiveOperationException {
        if (button.imageType() != null && button.imageData() != null) {
            Class<?> formImage = Class.forName("org.geysermc.cumulus.util.FormImage$Type");
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object type = Enum.valueOf((Class<? extends Enum>) formImage.asSubclass(Enum.class), button.imageType());
            Method withImage = builder.getClass().getMethod("button", String.class, formImage, String.class);
            withImage.invoke(builder, button.text(), type, button.imageData());
        } else {
            invokeString(builder, "button", button.text());
        }
    }

    private static void invokeString(Object target, String methodName, String value) throws ReflectiveOperationException {
        target.getClass().getMethod(methodName, String.class).invoke(target, value);
    }

    private static Method findConsumerMethod(Class<?> type, String methodName) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isAssignableFrom(Consumer.class)) return method;
        }
        return null;
    }
}
