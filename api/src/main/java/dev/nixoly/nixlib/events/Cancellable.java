package dev.nixoly.nixlib.events;

public interface Cancellable {

    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
