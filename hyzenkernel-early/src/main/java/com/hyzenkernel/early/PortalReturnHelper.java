package com.hyzenkernel.early;

import com.hypixel.hytale.builtin.portals.resources.PortalWorld;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.spawn.IndividualSpawnProvider;
import com.hypixel.hytale.builtin.portals.ui.PortalSpawnFinder;
import com.hypixel.hytale.server.core.asset.type.portalworld.PortalSpawn;
import com.hypixel.hytale.logger.HytaleLogger;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Shared return-portal helper.
 *
 * Ensures shared instances use a stable spawn UUID so the return portal
 * is placed at a consistent location and does not stack.
 */
public final class PortalReturnHelper {

    private static final Transform DEFAULT_WORLDGEN_SPAWN = new Transform(0.0, 140.0, 0.0);

    private PortalReturnHelper() {
    }

    public static CompletableFuture<World> spawnReturnPortal(
            World world,
            PortalWorld portalWorld,
            UUID sampleUuid,
            String portalBlockType
    ) {
        PortalSpawn portalSpawn = portalWorld.getPortalType().getPortalSpawn();
        UUID fixedUuid = sampleUuid;
        if (isSharedInstance(world)) {
            fixedUuid = world.getWorldConfig().getUuid();
        }

        return getSpawnTransform(world, fixedUuid, portalSpawn)
            .thenCompose(spawnTransform -> {
                Vector3d spawnPoint = spawnTransform.getPosition();
                return world.getChunkAsync(ChunkUtil.indexChunkFromBlock((int) spawnPoint.x, (int) spawnPoint.z))
                    .thenAccept(chunk -> {
                        for (int dy = 0; dy < 3; dy++) {
                            for (int dx = -1; dx <= 1; dx++) {
                                for (int dz = -1; dz <= 1; dz++) {
                                    chunk.setBlock((int) spawnPoint.x + dx, (int) spawnPoint.y + dy, (int) spawnPoint.z + dz, BlockType.EMPTY);
                                }
                            }
                        }

                        chunk.setBlock((int) spawnPoint.x, (int) spawnPoint.y, (int) spawnPoint.z, portalBlockType);
                        portalWorld.setSpawnPoint(spawnTransform);
                        world.getWorldConfig().setSpawnProvider(new IndividualSpawnProvider(spawnTransform));
                        // Persist the spawn provider so shared instances reuse the same portal after reloads.
                        world.getWorldConfig().markChanged();
                        HytaleLogger.getLogger()
                            .at(java.util.logging.Level.INFO)
                            .log(
                                "Spawned return portal for " + world.getName() + " at " +
                                    (int) spawnPoint.x + ", " + (int) spawnPoint.y + ", " + (int) spawnPoint.z
                            );
                    })
                    .thenApply(nothing -> world);
            });
    }

    private static CompletableFuture<Transform> getSpawnTransform(World world, UUID sampleUuid, PortalSpawn portalSpawn) {
        ISpawnProvider spawnProvider = world.getWorldConfig().getSpawnProvider();
        if (spawnProvider == null) {
            return CompletableFuture.completedFuture(null);
        }

        Transform worldSpawnPoint = spawnProvider.getSpawnPoint(world, sampleUuid);
        if (DEFAULT_WORLDGEN_SPAWN.equals(worldSpawnPoint) && portalSpawn != null) {
            return CompletableFuture.supplyAsync(() -> {
                Transform computedSpawn = PortalSpawnFinder.computeSpawnTransform(world, portalSpawn);
                return computedSpawn == null ? worldSpawnPoint : computedSpawn;
            }, world);
        }

        Transform uppedSpawnPoint = worldSpawnPoint.clone();
        uppedSpawnPoint.getPosition().add(0.0, 0.5, 0.0);
        return CompletableFuture.completedFuture(uppedSpawnPoint);
    }

    private static boolean isSharedInstance(World world) {
        if (world == null) {
            return false;
        }
        String name = world.getName();
        return name != null && name.startsWith("instance-shared-");
    }
}
