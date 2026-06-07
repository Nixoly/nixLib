package dev.nixoly.nixlib.events;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EventBus {

    private final Map<Class<?>, List<Listener<?>>> listeners = new HashMap<>();

    public <T> EventBus on(Class<T> type, Consumer<T> handler) {
        return on(type, 0, false, handler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> EventBus on(Class<T> type, int priority, boolean ignoreCancelled, Consumer<T> handler) {
        listeners.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>())
                .add(new Listener<>(handler, priority, ignoreCancelled));
        listeners.get(type).sort(Comparator.comparingInt(l -> -l.priority));
        return this;
    }

    public EventBus subscribeAll(Object holder) {
        for (Method method : holder.getClass().getDeclaredMethods()) {
            Subscribe s = method.getAnnotation(Subscribe.class);
            if (s == null) continue;
            if (method.getParameterCount() != 1) {
                throw new IllegalArgumentException("@Subscribe method must take exactly one argument: " + method);
            }
            method.setAccessible(true);
            Class<?> type = method.getParameterTypes()[0];
            attach(type, holder, method, s);
        }
        return this;
    }

    private void attach(Class<?> type, Object holder, Method method, Subscribe meta) {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Object> handler = event -> {
            try {
                method.invoke(holder, event);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("event dispatch failed for " + method.getName(), e.getCause() == null ? e : e.getCause());
            }
        };
        @SuppressWarnings({"unchecked", "rawtypes"})
        EventBus ignored = on((Class) type, meta.priority(), meta.ignoreCancelled(), (Consumer) handler);
    }

    @SuppressWarnings("unchecked")
    public <T> T post(T event) {
        List<Listener<?>> handlers = listeners.get(event.getClass());
        if (handlers == null || handlers.isEmpty()) return event;
        for (Listener<?> l : handlers) {
            if (event instanceof Cancellable c && c.isCancelled() && !l.ignoreCancelled) continue;
            ((Listener<T>) l).handler.accept(event);
        }
        return event;
    }

    public void unsubscribe(Class<?> type) {
        listeners.remove(type);
    }

    public void clear() {
        listeners.clear();
    }

    public int listenerCount() {
        int n = 0;
        for (List<Listener<?>> v : listeners.values()) n += v.size();
        return n;
    }

    private record Listener<T>(Consumer<T> handler, int priority, boolean ignoreCancelled) {}
}
