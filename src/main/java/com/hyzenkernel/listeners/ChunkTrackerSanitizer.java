package com.hyzenkernel.listeners;

import com.hyzenkernel.HyzenKernel;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * ChunkTrackerSanitizer - Prevents world crash when ChunkTracker has invalid PlayerRefs
 *
 * GitHub Issue: https://github.com/HyzenNet/Kernel/issues/6
 *
 * The Bug:
 * When a player disconnects, ChunkTracker.tryUnloadChunk() can crash with:
 * java.lang.NullPointerException: Cannot invoke "Ref.getStore()" because the return value of
 * "PlayerRef.getReference()" is null
 * at com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker.tryUnloadChunk(ChunkTracker.java:532)
 *
 * This happens because:
 * 1. Player disconnects
 * 2. ChunkTracker still has references to chunks that player was tracking
 * 3. When trying to unload those chunks, PlayerRef.getReference() returns null
 * 4. Calling getStore() on null crashes
 *
 * The Fix:
 * This sanitizer runs each tick on entities with ChunkTracker components.
 * It validates PlayerRef references and removes invalid ones before
 * Hytale's PlayerChunkTrackerSystems$UpdateSystem can crash on them.
 */
public class ChunkTrackerSanitizer extends EntityTickingSystem<EntityStore> {

    private final HyzenKernel plugin;

    // Discovered via reflection
    private Class<?> chunkTrackerClass = null;
    private ComponentType chunkTrackerType = null;
    private Class<?> playerRefClass = null;
    private Method getReferencesMethod = null;  // Method to get player refs from ChunkTracker
    private Method getReferenceMethod = null;   // PlayerRef.getReference()
    private Field playerRefsField = null;       // Field holding player refs if no getter

    private boolean initialized = false;
    private boolean apiDiscoveryFailed = false;

    // Statistics
    private final AtomicInteger entitiesChecked = new AtomicInteger(0);
    private final AtomicInteger invalidRefsFound = new AtomicInteger(0);
    private final AtomicInteger crashesPrevented = new AtomicInteger(0);

    public ChunkTrackerSanitizer(HyzenKernel plugin) {
        this.plugin = plugin;
    }

    @Override
    public Query<EntityStore> getQuery() {
        // ChunkTracker is on Player entities
        return Player.getComponentType();
    }

    @Override
    public void tick(
            float deltaTime,
            int index,
            ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store,
            CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!initialized && !apiDiscoveryFailed) {
            discoverApi();
        }

        if (apiDiscoveryFailed || chunkTrackerType == null) {
            return;
        }

        try {
            entitiesChecked.incrementAndGet();

            // Get ChunkTracker component from player
            Object chunkTracker = chunk.getComponent(index, chunkTrackerType);
            if (chunkTracker == null) {
                return;
            }

            // Try to validate and clean player refs
            validateAndCleanPlayerRefs(chunkTracker);

        } catch (Exception e) {
            plugin.getLogger().at(Level.FINE).log(
                    "[ChunkTrackerSanitizer] Error during tick: " + e.getMessage());
        }
    }

    /**
     * Validate and clean any invalid PlayerRefs in the ChunkTracker
     */
    private void validateAndCleanPlayerRefs(Object chunkTracker) {
        try {
            // Try multiple approaches to find and validate player refs

            // Approach 1: Look for fields that might contain PlayerRefs
            for (Field field : chunkTrackerClass.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(chunkTracker);

                if (value == null) continue;

                // Check if it's a collection that might contain PlayerRefs
                if (value instanceof Collection) {
                    cleanCollection((Collection<?>) value, field.getName());
                } else if (value instanceof Map) {
                    cleanMap((Map<?, ?>) value, field.getName());
                } else if (playerRefClass != null && playerRefClass.isInstance(value)) {
                    // Single PlayerRef field
                    if (isInvalidPlayerRef(value)) {
                        field.set(chunkTracker, null);
                        invalidRefsFound.incrementAndGet();
                        crashesPrevented.incrementAndGet();
                        plugin.getLogger().at(Level.INFO).log(
                                "[ChunkTrackerSanitizer] Cleared invalid PlayerRef in field: " + field.getName());
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().at(Level.FINE).log(
                    "[ChunkTrackerSanitizer] Error validating refs: " + e.getMessage());
        }
    }

    /**
     * Clean invalid PlayerRefs from a collection
     */
    private void cleanCollection(Collection<?> collection, String fieldName) {
        try {
            int removed = 0;
            Iterator<?> iterator = collection.iterator();
            while (iterator.hasNext()) {
                Object item = iterator.next();
                if (item != null && playerRefClass != null && playerRefClass.isInstance(item)) {
                    if (isInvalidPlayerRef(item)) {
                        iterator.remove();
                        removed++;
                    }
                }
            }
            if (removed > 0) {
                invalidRefsFound.addAndGet(removed);
                crashesPrevented.incrementAndGet();
                plugin.getLogger().at(Level.INFO).log(
                        "[ChunkTrackerSanitizer] Removed " + removed + " invalid PlayerRef(s) from collection: " + fieldName);
            }
        } catch (Exception e) {
            // Collection might not support removal, that's okay
        }
    }

    /**
     * Clean invalid PlayerRefs from a map (keys or values)
     */
    private void cleanMap(Map<?, ?> map, String fieldName) {
        try {
            int removed = 0;
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                Object key = entry.getKey();
                Object value = entry.getValue();

                boolean keyInvalid = key != null && playerRefClass != null &&
                        playerRefClass.isInstance(key) && isInvalidPlayerRef(key);
                boolean valueInvalid = value != null && playerRefClass != null &&
                        playerRefClass.isInstance(value) && isInvalidPlayerRef(value);

                if (keyInvalid || valueInvalid) {
                    iterator.remove();
                    removed++;
                }
            }
            if (removed > 0) {
                invalidRefsFound.addAndGet(removed);
                crashesPrevented.incrementAndGet();
                plugin.getLogger().at(Level.INFO).log(
                        "[ChunkTrackerSanitizer] Removed " + removed + " invalid PlayerRef entry(s) from map: " + fieldName);
            }
        } catch (Exception e) {
            // Map might not support removal, that's okay
        }
    }

    /**
     * Check if a PlayerRef has an invalid (null) internal reference
     */
    private boolean isInvalidPlayerRef(Object playerRef) {
        try {
            if (getReferenceMethod != null) {
                Object ref = getReferenceMethod.invoke(playerRef);
                return ref == null;
            }
        } catch (Exception e) {
            // If we can't check, assume it's valid
        }
        return false;
    }

    private void discoverApi() {
        try {
            plugin.getLogger().at(Level.INFO).log("[ChunkTrackerSanitizer] Discovering ChunkTracker API...");

            // Find ChunkTracker class
            chunkTrackerClass = Class.forName("com.hypixel.hytale.server.core.modules.entity.player.ChunkTracker");

            // Get ComponentType
            Method getComponentTypeMethod = chunkTrackerClass.getMethod("getComponentType");
            chunkTrackerType = (ComponentType) getComponentTypeMethod.invoke(null);

            // Find PlayerRef class
            playerRefClass = Class.forName("com.hypixel.hytale.server.core.universe.PlayerRef");

            // Find getReference() method on PlayerRef
            getReferenceMethod = playerRefClass.getMethod("getReference");

            initialized = true;
            plugin.getLogger().at(Level.INFO).log("[ChunkTrackerSanitizer] API discovery successful!");
            plugin.getLogger().at(Level.INFO).log("  - ChunkTracker type: " + chunkTrackerType);
            plugin.getLogger().at(Level.INFO).log("  - PlayerRef class: " + playerRefClass);
            plugin.getLogger().at(Level.INFO).log("  - getReference method: " + getReferenceMethod);

            // Log fields for debugging
            plugin.getLogger().at(Level.FINE).log("  - ChunkTracker fields:");
            for (Field field : chunkTrackerClass.getDeclaredFields()) {
                plugin.getLogger().at(Level.FINE).log("    - " + field.getName() + ": " + field.getType().getSimpleName());
            }

        } catch (ClassNotFoundException e) {
            plugin.getLogger().at(Level.WARNING).log(
                    "[ChunkTrackerSanitizer] API discovery failed - class not found: " + e.getMessage());
            apiDiscoveryFailed = true;
        } catch (NoSuchMethodException e) {
            plugin.getLogger().at(Level.WARNING).log(
                    "[ChunkTrackerSanitizer] API discovery failed - method not found: " + e.getMessage());
            apiDiscoveryFailed = true;
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log(
                    "[ChunkTrackerSanitizer] API discovery failed: " + e.getMessage());
            apiDiscoveryFailed = true;
        }
    }

    /**
     * Get status for the /interactionstatus command
     */
    public String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("Initialized: ").append(initialized).append("\n");
        sb.append("API Discovery Failed: ").append(apiDiscoveryFailed).append("\n");
        sb.append("Entities Checked: ").append(entitiesChecked.get()).append("\n");
        sb.append("Invalid Refs Found: ").append(invalidRefsFound.get()).append("\n");
        sb.append("Crashes Prevented: ").append(crashesPrevented.get());
        return sb.toString();
    }

    public int getCrashesPrevented() {
        return crashesPrevented.get();
    }
}
