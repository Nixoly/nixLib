package dev.nixoly.nixlib.collection;

public final class DoubleSampleQueue {

    private final double[] buffer;
    private int head;
    private int size;

    public DoubleSampleQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.buffer = new double[capacity];
    }

    public void add(double value) {
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

    public double[] copyInto(double[] target) {
        int capacity = buffer.length;
        if (size <= capacity - head) {
            System.arraycopy(buffer, head, target, 0, size);
        } else {
            int tail = capacity - head;
            System.arraycopy(buffer, head, target, 0, tail);
            System.arraycopy(buffer, 0, target, tail, size - tail);
        }
        return target;
    }
}
