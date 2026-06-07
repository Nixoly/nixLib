package dev.nixoly.nixlib.collection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoubleSampleQueueTest {

    @Test
    void newQueueIsEmpty() {
        DoubleSampleQueue queue = new DoubleSampleQueue(5);
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.isFull()).isFalse();
        assertThat(queue.capacity()).isEqualTo(5);
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new DoubleSampleQueue(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DoubleSampleQueue(-3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copyMatchesInsertionOrderBeforeWrap() {
        DoubleSampleQueue queue = new DoubleSampleQueue(3);
        queue.add(1.5);
        queue.add(2.5);
        double[] dst = new double[2];
        queue.copyInto(dst);
        assertThat(dst).containsExactly(1.5, 2.5);
        assertThat(queue.size()).isEqualTo(2);
        assertThat(queue.isFull()).isFalse();
    }

    @Test
    void wrapPreservesOldestSurvivingValues() {
        DoubleSampleQueue queue = new DoubleSampleQueue(4);
        for (double v : new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0}) {
            queue.add(v);
        }
        double[] dst = new double[4];
        queue.copyInto(dst);
        assertThat(dst).containsExactly(4.0, 5.0, 6.0, 7.0);
    }

    @Test
    void clearResetsBuffer() {
        DoubleSampleQueue queue = new DoubleSampleQueue(2);
        queue.add(9.0);
        queue.add(9.0);
        queue.clear();
        assertThat(queue.isEmpty()).isTrue();
        assertThat(queue.size()).isZero();
    }
}
