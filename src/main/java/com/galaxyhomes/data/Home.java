package com.galaxyhomes.data;

import org.bukkit.Location;
import org.bukkit.World;
import java.util.UUID;

public class Home {
    private final String name;
    private final UUID worldUID;
    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;

    public Home(String name, Location location) {
        this.name = name;
        this.worldUID = location.getWorld().getUID();
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public Home(String name, UUID worldUID, String worldName,
                double x, double y, double z, float yaw, float pitch) {
        this.name = name;
        this.worldUID = worldUID;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getName() { return name; }
    public UUID getWorldUID() { return worldUID; }
    public String getWorldName() { return worldName; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public Location toLocation() {
        World world = org.bukkit.Bukkit.getWorld(worldUID);
        if (world == null) world = org.bukkit.Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world, x, y, z, yaw, pitch);
    }

    @Override
    public String toString() {
        return "Home{name='" + name + "', world=" + worldName
                + ", x=" + x + ", y=" + y + ", z=" + z + "}";
    }
}
