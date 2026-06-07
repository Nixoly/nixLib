package dev.nixoly.nixlib.serializers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public final class LocationSerializer {

    private LocationSerializer() {}

    public static String toString(Location location) {
        if (location == null) return null;
        World world = location.getWorld();
        String worldName = world == null ? "" : world.getName();
        return String.format(java.util.Locale.ROOT,
                "%s;%.4f;%.4f;%.4f;%.2f;%.2f",
                worldName,
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public static Optional<Location> fromString(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        String[] parts = input.split(";");
        if (parts.length < 4) return Optional.empty();
        try {
            World world = Bukkit.getWorld(parts[0]);
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return Optional.of(new Location(world, x, y, z, yaw, pitch));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
