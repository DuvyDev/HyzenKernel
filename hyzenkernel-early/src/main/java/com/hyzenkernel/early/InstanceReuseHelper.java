package com.hyzenkernel.early;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.builtin.instances.config.InstanceWorldConfig;
import com.hypixel.hytale.builtin.instances.removal.RemovalCondition;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Shared instance helper (portal optimization).
 *
 * Reuses a single world per instanceId (e.g., Portals_Taiga) to avoid
 * re-generating chunks and copying instance assets for every portal.
 */
public final class InstanceReuseHelper {

    private InstanceReuseHelper() {
    }

    public static CompletableFuture<World> spawnSharedInstance(
            String instanceId,
            World forWorld,
            Transform returnPoint
    ) {
        if (!shouldShareInstance(instanceId)) {
            return InstancesPlugin.get().spawnInstance(instanceId, (String) null, forWorld, returnPoint);
        }

        String worldName = sharedWorldName(instanceId);
        Universe universe = Universe.get();

        World existing = universe.getWorld(worldName);
        if (existing != null && existing.isAlive()) {
            scheduleSharedConfig(existing);
            return CompletableFuture.completedFuture(existing);
        }

        if (universe.isWorldLoadable(worldName)) {
            return universe.loadWorld(worldName).thenApply(world -> {
                scheduleSharedConfig(world);
                return world;
            });
        }

        return InstancesPlugin.get().spawnInstance(instanceId, worldName, forWorld, returnPoint)
                .thenApply(world -> {
                    scheduleSharedConfig(world);
                    return world;
                });
    }

    private static boolean shouldShareInstance(String instanceId) {
        return instanceId != null && (instanceId.startsWith("Portals_") || instanceId.startsWith("Portals"));
    }

    private static String sharedWorldName(String instanceId) {
        return "instance-" + InstancesPlugin.safeName(instanceId);
    }

    private static void scheduleSharedConfig(World world) {
        CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS).execute(() -> {
            if (!world.isAlive()) {
                return;
            }
            try {
                world.execute(() -> applySharedConfig(world));
            } catch (Throwable t) {
                applySharedConfig(world);
            }
        });
    }

    private static void applySharedConfig(World world) {
        WorldConfig config = world.getWorldConfig();
        if (config.isDeleteOnUniverseStart()) {
            config.setDeleteOnUniverseStart(false);
        }
        if (config.isDeleteOnRemove()) {
            config.setDeleteOnRemove(false);
        }
        InstanceWorldConfig.ensureAndGet(config).setRemovalConditions(RemovalCondition.EMPTY);
        config.markChanged();
    }
}
