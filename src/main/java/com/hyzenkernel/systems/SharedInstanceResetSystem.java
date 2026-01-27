package com.hyzenkernel.systems;

import com.hyzenkernel.HyzenKernel;
import com.hyzenkernel.config.ConfigManager;
import com.hypixel.hytale.builtin.instances.removal.InstanceDataResource;
import com.hypixel.hytale.builtin.portals.resources.PortalWorld;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * SharedInstanceResetSystem
 *
 * For shared portal instance worlds (instance-shared-*), this system:
 * - Keeps the world persistent between restarts (clears delete flags)
 * - Resets instance timers and removes non-player entities when the world becomes empty
 * - Ensures portal timers/void events restart properly on next entry
 *
 * The terrain remains static; only dynamic state (entities + timers) is reset.
 */
public class SharedInstanceResetSystem extends TickingSystem<ChunkStore> {

    private static final String SHARED_PREFIX = "instance-shared-";
    private static final long RESET_INTERVAL_TICKS = 20; // ~1s at 20 TPS
    private static final Query<ChunkStore> CHUNK_QUERY = Query.and(
        WorldChunk.getComponentType(),
        Query.not(ChunkStore.REGISTRY.getNonSerializedComponentType())
    );

    private final HyzenKernel plugin;
    private final Set<String> dirtyWorlds = ConcurrentHashMap.newKeySet();
    private boolean loggedOnce = false;

    public SharedInstanceResetSystem(HyzenKernel plugin) {
        this.plugin = plugin;
    }

    @Override
    public void tick(float dt, int systemIndex, Store<ChunkStore> store) {
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        String worldName = world.getName();
        if (worldName == null || !worldName.startsWith(SHARED_PREFIX)) {
            return;
        }

        if (!loggedOnce) {
            plugin.getLogger().at(Level.INFO).log(
                "[SharedInstanceReset] Active - monitoring shared portal instances for resets"
            );
            loggedOnce = true;
        }

        // Ensure shared instances are persistent even if portals set delete flags.
        WorldConfig config = world.getWorldConfig();
        if (config != null && (config.isDeleteOnRemove() || config.isDeleteOnUniverseStart())) {
            config.setDeleteOnRemove(false);
            config.setDeleteOnUniverseStart(false);
            config.markChanged();
        }

        int playerCount = world.getPlayerCount();
        if (playerCount > 0) {
            dirtyWorlds.add(worldName);
            return;
        }

        if (!dirtyWorlds.contains(worldName)) {
            return;
        }

        if (world.getTick() % RESET_INTERVAL_TICKS != 0) {
            return;
        }

        dirtyWorlds.remove(worldName);
        resetSharedWorld(world);
    }

    private void resetSharedWorld(World world) {
        if (world.getPlayerCount() > 0) {
            return;
        }

        // Reset instance timers so portal countdown / void event restarts cleanly.
        InstanceDataResource instanceData = world.getChunkStore()
            .getStore()
            .getResource(InstanceDataResource.getResourceType());
        if (instanceData != null) {
            instanceData.setRemoving(false);
            instanceData.setTimeoutTimer(null);
            instanceData.setIdleTimeoutTimer(null);
            instanceData.setHadPlayer(false);
            instanceData.setWorldTimeoutTimer(null);
        }

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        ComponentType<EntityStore, Player> playerType = Player.getComponentType();

        // Clear the previous return portal block to prevent stacking.
        clearReturnPortal(world, entityStore);

        // Remove all non-player entities to force mob/encounter regeneration.
        entityStore.forEachEntityParallel((index, archetypeChunk, commandBuffer) -> {
            if (archetypeChunk.getComponent(index, playerType) != null) {
                return;
            }
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            if (ref != null && ref.isValid()) {
                commandBuffer.removeEntity(ref, RemoveReason.REMOVE);
            }
        });

        // Clear portal runtime state (event entity + spawn point).
        try {
            PortalWorld portalWorld = entityStore.getResource(PortalWorld.getResourceType());
            if (portalWorld != null) {
                portalWorld.setVoidEventRef(null);
            }
        } catch (Throwable ignored) {
            // Portals plugin not available or resource not registered; ignore.
        }

        // Unload all chunks so the next entry reloads pristine terrain from disk.
        if (ConfigManager.getInstance().isSharedInstanceUnloadChunksEnabled()) {
            scheduleChunkUnload(world);
        }

        if (ConfigManager.getInstance().logSanitizerActions()) {
            plugin.getLogger().at(Level.INFO).log(
                "[SharedInstanceReset] Reset shared instance '" + world.getName() + "' (entities cleared, timers reset)"
            );
        }
    }

    private void unloadAllChunks(World world) {
        try {
            Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
            List<Ref<ChunkStore>> refs = new ArrayList<>();
            chunkStore.forEachChunk(CHUNK_QUERY, (archetypeChunk, commandBuffer) -> {
                for (int i = 0; i < archetypeChunk.size(); i++) {
                    Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(i);
                    if (ref != null) {
                        refs.add(ref);
                    }
                }
            });
            for (Ref<ChunkStore> ref : refs) {
                if (ref != null && ref.isValid()) {
                    world.getChunkStore().remove(ref, RemoveReason.UNLOAD);
                }
            }
        } catch (Throwable ignored) {
            // Avoid hard failures during reset.
        }
    }

    private void clearReturnPortal(World world, Store<EntityStore> entityStore) {
        try {
            PortalWorld portalWorld = entityStore.getResource(PortalWorld.getResourceType());
            if (portalWorld == null || !portalWorld.exists()) {
                return;
            }
            Transform spawnPointTransform = portalWorld.getSpawnPoint();
            if (spawnPointTransform == null) {
                return;
            }
            Vector3d spawnPoint = spawnPointTransform.getPosition();
            int bx = (int) spawnPoint.x;
            int by = (int) spawnPoint.y;
            int bz = (int) spawnPoint.z;
            long chunkIndex = ChunkUtil.indexChunkFromBlock(bx, bz);
            world.getChunkAsync(chunkIndex).thenAccept(chunk -> {
                if (chunk == null) {
                    return;
                }
                for (int dy = 0; dy < 3; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            chunk.setBlock(bx + dx, by + dy, bz + dz, BlockType.EMPTY);
                        }
                    }
                }
            }).exceptionally(ignored -> null);

            // Ensure the portal chunk can be re-saved without the portal blocks.
            markPortalChunkForResave(world, chunkIndex);
        } catch (Throwable ignored) {
            // Ignore cleanup failures.
        }
    }

    private void markPortalChunkForResave(World world, long chunkIndex) {
        try {
            Ref<ChunkStore> chunkRef = world.getChunkStore().getChunkReference(chunkIndex);
            if (chunkRef == null || !chunkRef.isValid()) {
                return;
            }
            WorldChunk worldChunk = world.getChunkStore()
                .getStore()
                .getComponent(chunkRef, WorldChunk.getComponentType());
            if (worldChunk == null) {
                return;
            }
            worldChunk.setFlag(com.hypixel.hytale.server.core.universe.world.chunk.ChunkFlag.ON_DISK, false);
            worldChunk.markNeedsSaving();
        } catch (Throwable ignored) {
            // Best-effort only.
        }
    }

    private void scheduleChunkUnload(World world) {
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
            if (!world.isAlive()) {
                return;
            }
            try {
                world.execute(() -> unloadAllChunks(world));
            } catch (Throwable ignored) {
                unloadAllChunks(world);
            }
        });
    }
}
