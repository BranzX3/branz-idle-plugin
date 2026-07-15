package com.example.plugin.database;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service managing asynchronous database save queue.
 * Batches dirty entity persistence logic and flushes every 5 seconds off the main thread.
 */
public class SaveQueueService {

    private final JavaPlugin plugin;
    private final ConcurrentLinkedQueue<Runnable> dirtyTasks = new ConcurrentLinkedQueue<>();
    private int taskId = -1;

    public SaveQueueService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Starts the asynchronous flushing timer (every 5 seconds / 100 ticks).
     */
    public void start() {
        if (taskId != -1) {
            return;
        }
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flush, 100L, 100L).getTaskId();
        plugin.getLogger().info("[SaveQueueService] Asynchronous save queue started.");
    }

    /**
     * Submits a database persistence task to be executed during the next flush cycle.
     *
     * @param task Runnable performing database update/insert
     */
    public void queueTask(Runnable task) {
        if (task != null) {
            dirtyTasks.add(task);
        }
    }

    /**
     * Flushes all queued database tasks immediately.
     * Can be invoked during plugin shutdown or by periodic timer.
     */
    public void flush() {
        if (dirtyTasks.isEmpty()) {
            return;
        }

        int processed = 0;
        Runnable task;
        while ((task = dirtyTasks.poll()) != null) {
            try {
                task.run();
                processed++;
            } catch (Exception e) {
                plugin.getLogger().severe("[SaveQueueService] Error executing queued database task: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (processed > 0) {
            plugin.getLogger().fine("[SaveQueueService] Flushed " + processed + " database tasks.");
        }
    }

    /**
     * Stops the async timer and flushes any remaining tasks before shutdown.
     */
    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        flush();
        plugin.getLogger().info("[SaveQueueService] Stopped and flushed remaining tasks.");
    }
}
