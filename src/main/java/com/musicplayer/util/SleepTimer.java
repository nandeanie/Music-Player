package com.musicplayer.util;
 
import javax.swing.*;
import java.util.concurrent.*;
 
/**
 * NEW FILE
 * Sleep timer — fades volume to 0 over 10 seconds then calls onFinish.
 * Start with startTimer(minutes). Cancel anytime with cancel().
 */
public class SleepTimer {
 
    public interface SleepCallback {
        void onTick(long secondsRemaining);
        void onFadeStart();
        void onFinish();
    }
 
    private ScheduledExecutorService scheduler;
    private final SleepCallback callback;
    private volatile boolean running = false;
 
    public SleepTimer(SleepCallback callback) {
        this.callback = callback;
    }
 
    public void startTimer(int minutes) {
        cancel(); // cancel any existing timer
        running = true;
        long totalSeconds = (long) minutes * 60;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SleepTimer");
            t.setDaemon(true);
            return t;
        });
 
        final long[] remaining = {totalSeconds};
 
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            remaining[0]--;
            SwingUtilities.invokeLater(() -> callback.onTick(remaining[0]));
 
            if (remaining[0] == 10) {
                // 10 seconds left — start fade
                SwingUtilities.invokeLater(() -> callback.onFadeStart());
            }
            if (remaining[0] <= 0) {
                running = false;
                SwingUtilities.invokeLater(() -> callback.onFinish());
                scheduler.shutdown();
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
 
    public void cancel() {
        running = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }
 
    public boolean isRunning() {
        return running;
    }
}