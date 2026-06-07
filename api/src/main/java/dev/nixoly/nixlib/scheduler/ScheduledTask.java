package dev.nixoly.nixlib.scheduler;

public interface ScheduledTask {

    void cancel();

    boolean isCancelled();

    boolean isRunning();
}
