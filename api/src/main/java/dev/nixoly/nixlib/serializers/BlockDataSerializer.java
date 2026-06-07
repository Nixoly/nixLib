package dev.nixoly.nixlib.serializers;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.util.Optional;

public final class BlockDataSerializer {

    private BlockDataSerializer() {}

    public static String toString(BlockData data) {
        return data == null ? null : data.getAsString();
    }

    public static Optional<BlockData> fromString(String input) {
        if (input == null || input.isBlank()) return Optional.empty();
        try {
            return Optional.of(Bukkit.createBlockData(input));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
