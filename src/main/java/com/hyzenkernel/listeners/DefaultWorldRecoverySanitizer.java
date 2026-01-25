package com.hyzenkernel.listeners;

import com.hyzenkernel.HyzenKernel;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * FIX: Default World Recovery - GitHub Issue #23
 *
 * PROBLEM: When any exception occurs in the world thread (from any plugin),
 * Hytale removes the world via removeWorldExceptionally(). The default world
 * is never recreated, leaving all players unable to join until server restart.
 *
 * SOLUTION: Listen for RemoveWorldEvent with EXCEPTIONAL reason. When the default
 * world is removed, schedule a reload from disk after a short delay. This gives
 * Hytale time to clean up the crashed world state before we reload.
 *
 * SAFETY FEATURES:
 * - Cooldown period to prevent rapid recovery loops
 * - Maximum retry attempts to avoid infinite loops
 * - Only recovers the configured default world
 * - Logs all recovery attempts for debugging
 */
public class DefaultWorldRecoverySanitizer {

    private final HyzenKernel plugin;
    private EventRegistration<?, ?> eventRegistration;

    // Recovery state
    private final AtomicBoolean recoveryInProgress = new AtomicBoolean(false);
    private final AtomicInteger recoveryCount = new AtomicInteger(0);
    private volatile long lastRecoveryAttempt = 0;

    // Configuration
    private static final long RECOVERY_DELAY_MS = 2000;      // Wait 2s before reload
    private static final long RECOVERY_COOLDOWN_MS = 30000;  // 30s between recovery attempts
    private static final int MAX_RECOVERY_ATTEMPTS = 5;      // Max attempts before giving up

    public DefaultWorldRecoverySanitizer(HyzenKernel plugin) {
        this.plugin = plugin;
    }

    /**
     * Register the event listener with HyzenKernel event registry.
     */
    public void register() {
        eventRegistration = plugin.getEventRegistry().registerGlobal(
            RemoveWorldEvent.class,
            this::onWorldRemoved
        );

        plugin.getLogger().at(Level.INFO).log(
            "[DefaultWorldRecovery] Event handler registered - monitoring for exceptional world removal"
        );
    }

    /**
     * Handle world removal events.
     */
    private void onWorldRemoved(RemoveWorldEvent event) {
        World world = event.getWorld();
        if (world == null) {
            return;
        }

        String worldName = world.getName();
        RemoveWorldEvent.RemovalReason reason = event.getRemovalReason();

        // Only handle EXCEPTIONAL removals
        if (reason != RemoveWorldEvent.RemovalReason.EXCEPTIONAL) {
            plugin.getLogger().at(Level.FINE).log(
                "[DefaultWorldRecovery] World '%s' removed with reason %s - not recovering",
                worldName, reason
            );
            return;
        }

        // Check if this is the default world
        String defaultWorldName = getDefaultWorldName();
        if (defaultWorldName == null || !defaultWorldName.equalsIgnoreCase(worldName)) {
            plugin.getLogger().at(Level.INFO).log(
                "[DefaultWorldRecovery] Non-default world '%s' removed exceptionally - not recovering",
                worldName
            );
            return;
        }

        // Log the exceptional removal
        plugin.getLogger().at(Level.WARNING).log(
            "[DefaultWorldRecovery] DEFAULT WORLD '%s' removed exceptionally! Initiating recovery...",
            worldName
        );

        // Attempt recovery
        scheduleRecovery(worldName);
    }

    /**
     * Schedule world recovery after a delay.
     */
    private void scheduleRecovery(String worldName) {
        // Check if recovery is already in progress
        if (!recoveryInProgress.compareAndSet(false, true)) {
            plugin.getLogger().at(Level.INFO).log(
                "[DefaultWorldRecovery] Recovery already in progress, skipping duplicate"
            );
            return;
        }

        // Check cooldown
        long now = System.currentTimeMillis();
        if (now - lastRecoveryAttempt < RECOVERY_COOLDOWN_MS) {
            long waitTime = RECOVERY_COOLDOWN_MS - (now - lastRecoveryAttempt);
            plugin.getLogger().at(Level.WARNING).log(
                "[DefaultWorldRecovery] Cooldown active, waiting %dms before retry",
                waitTime
            );
            recoveryInProgress.set(false);

            // Schedule after cooldown
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> scheduleRecovery(worldName),
                waitTime,
                TimeUnit.MILLISECONDS
            );
            return;
        }

        // Check max attempts
        int attempts = recoveryCount.incrementAndGet();
        if (attempts > MAX_RECOVERY_ATTEMPTS) {
            plugin.getLogger().at(Level.SEVERE).log(
                "[DefaultWorldRecovery] CRITICAL: Max recovery attempts (%d) exceeded! " +
                "Server requires manual restart. Check logs for root cause.",
                MAX_RECOVERY_ATTEMPTS
            );
            recoveryInProgress.set(false);
            return;
        }

        lastRecoveryAttempt = now;

        plugin.getLogger().at(Level.INFO).log(
            "[DefaultWorldRecovery] Scheduling world reload in %dms (attempt %d/%d)",
            RECOVERY_DELAY_MS, attempts, MAX_RECOVERY_ATTEMPTS
        );

        // Schedule the actual recovery
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            () -> performRecovery(worldName),
            RECOVERY_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * Perform the actual world reload.
     */
    private void performRecovery(String worldName) {
        try {
            Universe universe = Universe.get();

            // Double-check the world is still missing
            World existingWorld = universe.getWorld(worldName);
            if (existingWorld != null) {
                plugin.getLogger().at(Level.INFO).log(
                    "[DefaultWorldRecovery] World '%s' already exists, recovery not needed",
                    worldName
                );
                recoveryInProgress.set(false);
                return;
            }

            // Check if world is loadable from disk
            if (!universe.isWorldLoadable(worldName)) {
                plugin.getLogger().at(Level.SEVERE).log(
                    "[DefaultWorldRecovery] CRITICAL: World '%s' is not loadable from disk! " +
                    "World data may be corrupted or missing.",
                    worldName
                );
                recoveryInProgress.set(false);
                return;
            }

            plugin.getLogger().at(Level.INFO).log(
                "[DefaultWorldRecovery] Loading world '%s' from disk...",
                worldName
            );

            // Reload the world
            CompletableFuture<World> loadFuture = universe.loadWorld(worldName);

            loadFuture.whenComplete((world, throwable) -> {
                recoveryInProgress.set(false);

                if (throwable != null) {
                    plugin.getLogger().at(Level.SEVERE).log(
                        "[DefaultWorldRecovery] FAILED to reload world '%s': %s",
                        worldName, throwable.getMessage()
                    );

                    // Log full stack trace at FINE level
                    plugin.getLogger().at(Level.FINE).log(
                        "[DefaultWorldRecovery] Stack trace: " + throwable
                    );
                } else if (world != null) {
                    plugin.getLogger().at(Level.INFO).log(
                        "[DefaultWorldRecovery] SUCCESS! World '%s' has been recovered. " +
                        "Players can now join. (Recovery #%d)",
                        worldName, recoveryCount.get()
                    );

                    // Reset recovery count on success
                    recoveryCount.set(0);
                } else {
                    plugin.getLogger().at(Level.WARNING).log(
                        "[DefaultWorldRecovery] World load returned null for '%s'",
                        worldName
                    );
                }
            });

        } catch (Exception e) {
            plugin.getLogger().at(Level.SEVERE).log(
                "[DefaultWorldRecovery] Exception during recovery: %s",
                e.getMessage()
            );
            recoveryInProgress.set(false);
        }
    }

    /**
     * Get the configured default world name from server config.
     */
    private String getDefaultWorldName() {
        try {
            var config = HytaleServer.get().getConfig();
            if (config != null && config.getDefaults() != null) {
                return config.getDefaults().getWorld();
            }
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log(
                "[DefaultWorldRecovery] Could not get default world name: %s",
                e.getMessage()
            );
        }
        return "default"; // Fallback
    }

    /**
     * Get the number of recovery attempts made this session.
     */
    public int getRecoveryCount() {
        return recoveryCount.get();
    }

    /**
     * Check if a recovery is currently in progress.
     */
    public boolean isRecoveryInProgress() {
        return recoveryInProgress.get();
    }

    /**
     * Reset the recovery counter (useful after manual intervention).
     */
    public void resetRecoveryCounter() {
        recoveryCount.set(0);
        lastRecoveryAttempt = 0;
        plugin.getLogger().at(Level.INFO).log(
            "[DefaultWorldRecovery] Recovery counter reset"
        );
    }
}
