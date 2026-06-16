package dev.nixoly.nixlib.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoundUtilsTest {

    @Test
    void legacyEnumKeyMapsEnderChestOpen() {
        assertThat(SoundUtils.legacyEnumKey("BLOCK_ENDER_CHEST_OPEN")).isEqualTo("block.ender_chest.open");
    }

    @Test
    void legacyEnumKeyMapsUiButtonClick() {
        assertThat(SoundUtils.legacyEnumKey("UI_BUTTON_CLICK")).isEqualTo("ui.button.click");
    }

    @Test
    void legacyEnumKeyMapsNoteBlockBass() {
        assertThat(SoundUtils.legacyEnumKey("BLOCK_NOTE_BLOCK_BASS")).isEqualTo("block.note_block.bass");
    }
}
