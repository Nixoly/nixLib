package dev.nixoly.nixlib.scheduler;

import dev.nixoly.nixlib.version.ServerType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerProviderTest {

    @Test
    void detectIsUnknownWithoutBukkit() {
        ServerType detected = ServerType.detect();
        assertThat(detected).isIn(ServerType.UNKNOWN, ServerType.BUKKIT);
    }
}
