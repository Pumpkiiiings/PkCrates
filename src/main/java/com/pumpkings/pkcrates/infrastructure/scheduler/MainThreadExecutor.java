package com.pumpkings.pkcrates.infrastructure.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * {@link Executor} that guarantees a task runs on the Bukkit main thread.
 *
 * <p>Intended for use with {@code CompletableFuture.thenApplyAsync(fn, executor)} and
 * friends, so that continuations chained onto database futures never touch the Bukkit
 * API — or dispatch Bukkit events — from a {@code ForkJoinPool} worker.</p>
 *
 * <p>If the caller is already on the main thread the task runs inline, avoiding a
 * one-tick delay for the common synchronous path.</p>
 */
public final class MainThreadExecutor implements Executor {

    private final Plugin plugin;

    public MainThreadExecutor(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * @throws RejectedExecutionException If the plugin is disabled and the task cannot
     *                                    be scheduled. Chained futures complete
     *                                    exceptionally instead of hanging forever.
     */
    @Override
    public void execute(Runnable command) {
        if (Bukkit.isPrimaryThread()) {
            command.run();
            return;
        }
        if (!plugin.isEnabled()) {
            throw new RejectedExecutionException("Plugin is disabled; cannot schedule main-thread task.");
        }
        Bukkit.getScheduler().runTask(plugin, command);
    }
}
