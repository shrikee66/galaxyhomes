package com.galaxyhomes.utils;

import com.galaxyhomes.config.ConfigManager.SoundConfig;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtils {
    private SoundUtils() {}

    public static void playSound(Player p, SoundConfig c) {
        if (p == null || c == null) return;

        try {
            p.playSound(
                p.getLocation(),
                Sound.valueOf(c.sound.toUpperCase()),
                c.volume,
                c.pitch
            );
        } catch (IllegalArgumentException ignored) {}
    }
}
