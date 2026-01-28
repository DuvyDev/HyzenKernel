package com.hyzenkernel.early;

import com.hypixel.hytale.server.core.universe.Universe;

import java.util.concurrent.CompletableFuture;

/**
 * Helper for RemovalSystem transformer.
 * Keeps vanilla async removal to avoid deleting worlds on the world thread.
 */
public final class RemovalSystemHelper {
    private RemovalSystemHelper() {
    }

    public static void removeWorldAsync(String worldName) {
        if (worldName == null) {
            return;
        }
        CompletableFuture.runAsync(() -> Universe.get().removeWorld(worldName));
    }
}

