package dev.nixoly.nixlib.items;

import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SkullBuilderTest {

    private static final String TEXTURE_BASE64 =
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1"
                    + "cmUvMzdlZWNhZTU4ZmVjYzY3NDI4MjM1MmM5Zjg2ZGMxOGE3NGI0NjYwOWY4N2I2ZDY4MTg2MDA2ZGJmMjU1MDZmIn19fQ==";

    private static final String EXPECTED_URL =
            "http://textures.minecraft.net/texture/37eecae58fecc674282352c9f86dc18a74b46609f87b6d68186006dbf25506f";

    @Test
    void extractUrlReadsSkinUrlFromJson() {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + EXPECTED_URL + "\"}}}";
        assertThat(SkullBuilder.extractUrl(json)).isEqualTo(EXPECTED_URL);
    }

    @Test
    void extractUrlReturnsNullWhenNoUrlPresent() {
        assertThat(SkullBuilder.extractUrl("{\"textures\":{}}")).isNull();
        assertThat(SkullBuilder.extractUrl("not json at all")).isNull();
        assertThat(SkullBuilder.extractUrl(null)).isNull();
    }

    @Test
    void resolveTextureUrlDecodesBase64Blob() {
        assertThat(SkullBuilder.resolveTextureUrl(TEXTURE_BASE64)).isEqualTo(EXPECTED_URL);
    }

    @Test
    void resolveTextureUrlAcceptsRawUrl() {
        assertThat(SkullBuilder.resolveTextureUrl(EXPECTED_URL)).isEqualTo(EXPECTED_URL);
        assertThat(SkullBuilder.resolveTextureUrl("https://example.com/skin.png"))
                .isEqualTo("https://example.com/skin.png");
    }

    @Test
    void resolveTextureUrlExpandsBareHash() {
        String hash = "37eecae58fecc674282352c9f86dc18a74b46609f87b6d68186006dbf25506f";
        assertThat(SkullBuilder.resolveTextureUrl(hash))
                .isEqualTo("http://textures.minecraft.net/texture/" + hash);
    }

    @Test
    void resolveTextureUrlTrimsSurroundingWhitespace() {
        assertThat(SkullBuilder.resolveTextureUrl("  " + TEXTURE_BASE64 + "  ")).isEqualTo(EXPECTED_URL);
    }

    @Test
    void resolveTextureUrlReturnsNullForNullBlankOrGarbage() {
        assertThat(SkullBuilder.resolveTextureUrl(null)).isNull();
        assertThat(SkullBuilder.resolveTextureUrl("")).isNull();
        assertThat(SkullBuilder.resolveTextureUrl("   ")).isNull();
        assertThat(SkullBuilder.resolveTextureUrl("hello world")).isNull();
    }

    @Test
    void resolveTextureUrlHandlesBase64WithoutUrlAsNull() {
        String json = "{\"textures\":{\"SKIN\":{}}}";
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        assertThat(SkullBuilder.resolveTextureUrl(encoded)).isNull();
    }
}
