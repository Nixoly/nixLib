package dev.nixoly.nixlib.events;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventBusTest {

    static final class HelloEvent {
        String text;
        HelloEvent(String text) { this.text = text; }
    }

    static final class CancellableSpawn implements Cancellable {
        boolean cancelled;
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean v) { cancelled = v; }
    }

    @Test
    void simpleHandlerFires() {
        EventBus bus = new EventBus();
        AtomicInteger calls = new AtomicInteger();
        bus.on(HelloEvent.class, e -> calls.incrementAndGet());

        bus.post(new HelloEvent("hi"));
        bus.post(new HelloEvent("ho"));

        assertThat(calls).hasValue(2);
    }

    @Test
    void higherPriorityRunsFirst() {
        EventBus bus = new EventBus();
        List<String> order = new ArrayList<>();
        bus.on(HelloEvent.class, 0, false, e -> order.add("low"));
        bus.on(HelloEvent.class, 100, false, e -> order.add("high"));

        bus.post(new HelloEvent("x"));
        assertThat(order).containsExactly("high", "low");
    }

    @Test
    void cancelledEventSkipsLaterHandlersUnlessOverridden() {
        EventBus bus = new EventBus();
        List<String> hits = new ArrayList<>();
        bus.on(CancellableSpawn.class, 10, false, e -> { hits.add("a"); e.setCancelled(true); });
        bus.on(CancellableSpawn.class, 5, false, e -> hits.add("b-skipped"));
        bus.on(CancellableSpawn.class, 0, true, e -> hits.add("c-forced"));

        bus.post(new CancellableSpawn());

        assertThat(hits).containsExactly("a", "c-forced");
    }

    @Test
    void subscribeAllScansAnnotatedMethods() {
        EventBus bus = new EventBus();
        AnnotatedListener listener = new AnnotatedListener();
        bus.subscribeAll(listener);

        bus.post(new HelloEvent("annotated"));
        assertThat(listener.received).isEqualTo("annotated");
    }

    @Test
    void invalidSubscribeMethodFails() {
        EventBus bus = new EventBus();
        assertThatThrownBy(() -> bus.subscribeAll(new BadListener()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listenerCountReflectsRegistrations() {
        EventBus bus = new EventBus();
        bus.on(HelloEvent.class, e -> {});
        bus.on(HelloEvent.class, e -> {});
        bus.on(CancellableSpawn.class, e -> {});

        assertThat(bus.listenerCount()).isEqualTo(3);
        bus.clear();
        assertThat(bus.listenerCount()).isZero();
    }

    static final class AnnotatedListener {
        String received;

        @Subscribe(priority = 5)
        void onHello(HelloEvent event) {
            received = event.text;
        }
    }

    static final class BadListener {
        @Subscribe
        void noArgs() {}
    }
}
