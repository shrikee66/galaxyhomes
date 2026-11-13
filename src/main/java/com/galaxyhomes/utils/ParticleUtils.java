package com.galaxyhomes.utils;

import com.galaxyhomes.config.ConfigManager.ParticleConfig;
import org.bukkit.Location;
import org.bukkit.Particle;

public final class ParticleUtils {
    private ParticleUtils() {}

    public static void spawnParticles(Location loc, ParticleConfig c) {
        if (loc == null || loc.getWorld() == null || c == null) return;

        try {
            loc.getWorld().spawnParticle(
                Particle.valueOf(c.type.toUpperCase()),
                loc,
                Math.max(0, c.count),
                c.offsetX,
                c.offsetY,
                c.offsetZ,
                c.speed
            );
        } catch (IllegalArgumentException ignored) {}
    }
}
