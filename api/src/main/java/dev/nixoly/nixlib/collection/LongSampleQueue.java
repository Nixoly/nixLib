package dev.nixoly.nixlib.collection;

public final class LongSampleQueue {

    private final long[] buffer;
    private int head;
    private int size;

    public LongSampleQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.buffer = new long[capacity];
    }

    public void add(long value) {
        int capacity = buffer.length;
        if (size < capacity) {
            buffer[(head + size) % capacity] = value;
            size++;
        } else {
            buffer[head] = value;
            head = (head + 1) % capacity;
        }
    }

    public void clear() {
        head = 0;
        size = 0;
    }

    public int size() {
        return size;
    }

    public int capacity() {
        return buffer.length;
    }

    public boolean isFull() {
        return size >= buffer.length;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public double[] copyAsDoublesInto(double[] target) {
        int capacity = buffer.length;
        if (size <= capacity - head) {
            for (int i = 0; i < size; i++) {
                target[i] = buffer[head + i];
            }
        } else {
            int tail = capacity - head;
            for (int i = 0; i < tail; i++) {
                target[i] = buffer[head + i];
            }
            for (int i = 0; i < size - tail; i++) {
                target[tail + i] = buffer[i];
            }
        }
        return target;
    }
}
