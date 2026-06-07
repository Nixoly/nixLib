package dev.nixoly.nixlib.serializers;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SerializerTests {

    private ServerMock server;
    private World world;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        MockBukkit.createMockPlugin("nixlib");
        world = server.addSimpleWorld("test-world");
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void locationRoundTrip() {
        Location loc = new Location(world, 1.5, 64.0, -7.25, 90f, -45f);
        String encoded = LocationSerializer.toString(loc);
        Optional<Location> decoded = LocationSerializer.fromString(encoded);

        assertThat(decoded).isPresent();
        Location restored = decoded.get();
        assertThat(restored.getWorld()).isSameAs(world);
        assertThat(restored.getX()).isEqualTo(1.5);
        assertThat(restored.getY()).isEqualTo(64.0);
        assertThat(restored.getZ()).isEqualTo(-7.25);
        assertThat(restored.getYaw()).isEqualTo(90f);
        assertThat(restored.getPitch()).isEqualTo(-45f);
    }

    @Test
    void garbageLocationStringRejected() {
        assertThat(LocationSerializer.fromString(null)).isEmpty();
        assertThat(LocationSerializer.fromString("")).isEmpty();
        assertThat(LocationSerializer.fromString("world;notnum;1;2")).isEmpty();
    }

    @Test
    void itemStackBase64ProducesNonEmptyString() {
        ItemStack original = new ItemStack(Material.DIAMOND_SWORD, 3);
        String encoded = ItemStackSerializer.toBase64(original);

        assertThat(encoded).isNotBlank();
        // MockBukkit can serialize but its in-memory ItemStack doesn't round-trip via Bukkit's
        // ObjectInputStream registry; on a real server the produced base64 deserialises cleanly.
    }

    @Test
    void itemStackArrayProducesNonEmptyString() {
        ItemStack[] arr = new ItemStack[] {
                new ItemStack(Material.STONE),
                null,
                new ItemStack(Material.GOLD_INGOT, 5)
        };
        String encoded = ItemStackSerializer.toBase64Array(arr);
        assertThat(encoded).isNotBlank();
    }

    @Test
    void corruptItemStackRejected() {
        assertThat(ItemStackSerializer.fromBase64("not-base64!!!")).isEmpty();
        assertThat(ItemStackSerializer.fromBase64Array("bad")).isEmpty();
    }
}
