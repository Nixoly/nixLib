package dev.nixoly.nixlib.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LongSampleQueueTest {

    @Test
    void newQueueIsEmpty() {
        LongSampleQueue queue = new LongSampleQueue(4);
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.isFull()).isFalse();
        assertThat(queue.size()).isZero();
        assertThat(queue.capacity()).isEqualTo(4);
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new LongSampleQueue(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LongSampleQueue(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addFillsUpToCapacity() {
        LongSampleQueue queue = new LongSampleQueue(3);
        queue.add(10L);
        queue.add(20L);
        queue.add(30L);
        assertThat(queue.size()).isEqualTo(3);
        assertThat(queue.isFull()).isTrue();

        double[] dst = new double[3];
        queue.copyAsDoublesInto(dst);
        assertThat(dst).containsExactly(10.0, 20.0, 30.0);
    }

    @Test
    void addWrapsWhenFull() {
        LongSampleQueue queue = new LongSampleQueue(3);
        for (long v : new long[]{1L, 2L, 3L, 4L, 5L}) {
            queue.add(v);
        }
        double[] dst = new double[3];
        queue.copyAsDoublesInto(dst);
        assertThat(dst).containsExactly(3.0, 4.0, 5.0);
    }

    @Test
    void clearResetsState() {
        LongSampleQueue queue = new LongSampleQueue(2);
        queue.add(100L);
        queue.add(200L);
        queue.clear();
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.isFull()).isFalse();
        assertThat(queue.size()).isZero();
    }

    @Test
    void copyAsDoublesAfterWrapPreservesInsertionOrder() {
        LongSampleQueue queue = new LongSampleQueue(4);
        for (long v : new long[]{1L, 2L, 3L, 4L, 5L, 6L}) {
            queue.add(v);
        }
        double[] dst = new double[4];
        queue.copyAsDoublesInto(dst);
        assertThat(dst).containsExactly(3.0, 4.0, 5.0, 6.0);
    }
}
