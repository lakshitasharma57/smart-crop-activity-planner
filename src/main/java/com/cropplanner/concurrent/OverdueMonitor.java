package com.cropplanner.concurrent;

import com.cropplanner.service.TaskService;

import java.time.LocalDate;
import java.util.function.Supplier;

/**
 * Daemon thread that re-counts overdue tasks at a fixed interval, because tasks
 * become overdue as time passes even when the user does nothing.
 * The menu reads the latest count through synchronized methods.
 */
public class OverdueMonitor implements Runnable {

    private final TaskService tasks;
    private final Supplier<LocalDate> clock;
    private final long intervalMillis;
    private int overdueCount;
    private Thread worker;

    public OverdueMonitor(TaskService tasks, Supplier<LocalDate> clock, long intervalMillis) {
        this.tasks = tasks;
        this.clock = clock;
        this.intervalMillis = intervalMillis;
    }

    public void start() {
        refresh();                                  // first value is ready before the menu appears
        worker = new Thread(this, "overdue-monitor");
        worker.setDaemon(true);                     // never keeps the program alive on its own
        worker.start();
    }

    /** Counts overdue tasks right now (also called by the menu after every change). */
    public void refresh() {
        setOverdueCount(tasks.getOverdue(clock.get()).size());
    }

    /** NEW before start(), then RUNNABLE / TIMED_WAITING (sleeping) while running, TERMINATED after stop(). */
    public Thread.State getState() {
        return worker == null ? Thread.State.NEW : worker.getState();
    }

    public String describe() {
        if (worker == null) {
            return "Monitor thread has not been started.";
        }
        return String.format("Thread '%s' | state: %s | daemon: %b | overdue tasks found: %d",
                worker.getName(), worker.getState(), worker.isDaemon(), getOverdueCount());
    }

    public synchronized int getOverdueCount() {
        return overdueCount;
    }

    private synchronized void setOverdueCount(int count) {
        this.overdueCount = count;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(intervalMillis);
                refresh();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();     // stop() was called
        }
    }

    public void stop() {
        if (worker == null) {
            return;
        }
        worker.interrupt();
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
