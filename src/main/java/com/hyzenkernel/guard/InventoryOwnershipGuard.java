package com.hyzenkernel.guard;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Guards against inventory sharing between players (Issue #45).
 *
 * This class is called by the transformed LivingEntity.setInventory() method
 * to validate that an Inventory isn't being shared between multiple players.
 *
 * If a shared reference is detected, the inventory is deep-cloned to break
 * the shared reference before assignment.
 */
public class InventoryOwnershipGuard {

    private static final Logger LOGGER = Logger.getLogger("HyzenKernel");

    // Track inventory -> owner UUID
    // Using IdentityHashMap semantics via System.identityHashCode for exact instance matching
    private static final Map<Integer, UUID> inventoryOwners = new ConcurrentHashMap<>();

    // Track individual ItemContainers for finer detection
    private static final Map<Integer, UUID> containerOwners = new ConcurrentHashMap<>();

    // Statistics
    private static volatile int sharedReferencesDetected = 0;
    private static volatile int inventoriesCloned = 0;
    private static volatile int validationCalls = 0;

    // Cached reflection objects
    private static Method getPlayerRefMethod;
    private static Method getUuidMethod;
    private static Class<?> playerClass;
    private static boolean reflectionInitialized = false;
    private static boolean reflectionFailed = false;

    /**
     * Called by transformed LivingEntity.setInventory() BEFORE inventory assignment.
     *
     * @param livingEntity The entity receiving the inventory
     * @param inventory The inventory being assigned
     * @return The original inventory if safe, or a cloned inventory if shared reference detected
     */
    public static Object validateAndClone(Object livingEntity, Object inventory) {
        if (inventory == null || livingEntity == null) {
            return inventory;
        }

        validationCalls++;

        try {
            // Initialize reflection on first call
            if (!reflectionInitialized && !reflectionFailed) {
                initializeReflection();
            }

            if (reflectionFailed) {
                return inventory;
            }

            // Only validate for Player entities
            if (!playerClass.isInstance(livingEntity)) {
                return inventory;
            }

            UUID newOwnerUuid = getEntityUuid(livingEntity);
            if (newOwnerUuid == null) {
                return inventory;
            }

            // Use identity hash code to track exact object instances
            int inventoryId = System.identityHashCode(inventory);
            UUID currentOwner = inventoryOwners.get(inventoryId);

            if (currentOwner != null && !currentOwner.equals(newOwnerUuid)) {
                // SHARED REFERENCE DETECTED!
                sharedReferencesDetected++;

                LOGGER.log(Level.WARNING,
                    "[HyzenKernel] INVENTORY SHARING DETECTED! (Issue #45)\n" +
                    "  Inventory @{0} owned by {1} being assigned to {2}\n" +
                    "  Forcing deep clone to prevent inventory sync.\n" +
                    "  Detection #{3}",
                    new Object[]{
                        Integer.toHexString(inventoryId),
                        currentOwner.toString().substring(0, 8) + "...",
                        newOwnerUuid.toString().substring(0, 8) + "...",
                        sharedReferencesDetected
                    }
                );

                // Clone the inventory to break the shared reference
                Object clonedInventory = cloneInventory(inventory);
                if (clonedInventory != null) {
                    inventoriesCloned++;
                    int clonedId = System.identityHashCode(clonedInventory);
                    inventoryOwners.put(clonedId, newOwnerUuid);
                    registerContainers(clonedInventory, newOwnerUuid);

                    LOGGER.log(Level.INFO,
                        "[HyzenKernel] Successfully cloned inventory for player {0}\n" +
                        "  Original @{1} -> Clone @{2}\n" +
                        "  Total clones: {3}",
                        new Object[]{
                            newOwnerUuid.toString().substring(0, 8) + "...",
                            Integer.toHexString(inventoryId),
                            Integer.toHexString(clonedId),
                            inventoriesCloned
                        }
                    );

                    return clonedInventory;
                } else {
                    LOGGER.log(Level.SEVERE,
                        "[HyzenKernel] CRITICAL: Failed to clone inventory!\n" +
                        "  Players {0} and {1} may have LINKED inventories!\n" +
                        "  This is a serious bug - please report to HyzenKernel GitHub.",
                        new Object[]{
                            currentOwner.toString().substring(0, 8) + "...",
                            newOwnerUuid.toString().substring(0, 8) + "..."
                        }
                    );
                }
            } else {
                // First assignment or same owner - register ownership
                inventoryOwners.put(inventoryId, newOwnerUuid);
                registerContainers(inventory, newOwnerUuid);
            }

        } catch (Exception e) {
            // Log but don't crash - return original inventory
            if (validationCalls < 5) {
                LOGGER.log(Level.WARNING,
                    "[HyzenKernel] Error in inventory validation: " + e.getMessage());
            }
        }

        return inventory;
    }

    /**
     * Initialize reflection objects for Player class access.
     */
    private static synchronized void initializeReflection() {
        if (reflectionInitialized || reflectionFailed) {
            return;
        }

        try {
            playerClass = Class.forName("com.hypixel.hytale.server.core.entity.entities.Player");
            getPlayerRefMethod = playerClass.getMethod("getPlayerRef");

            Class<?> playerRefClass = Class.forName("com.hypixel.hytale.server.core.universe.PlayerRef");
            getUuidMethod = playerRefClass.getMethod("getUuid");

            reflectionInitialized = true;
            LOGGER.log(Level.INFO, "[HyzenKernel] InventoryOwnershipGuard initialized successfully");

        } catch (Exception e) {
            reflectionFailed = true;
            LOGGER.log(Level.WARNING,
                "[HyzenKernel] Failed to initialize InventoryOwnershipGuard reflection: " + e.getMessage());
        }
    }

    /**
     * Get UUID from a Player entity.
     */
    private static UUID getEntityUuid(Object livingEntity) {
        try {
            Object playerRef = getPlayerRefMethod.invoke(livingEntity);
            if (playerRef == null) {
                return null;
            }
            return (UUID) getUuidMethod.invoke(playerRef);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Register individual ItemContainers from an Inventory.
     */
    private static void registerContainers(Object inventory, UUID ownerUuid) {
        try {
            String[] containerGetters = {"getStorage", "getArmor", "getHotbar", "getUtility", "getTools", "getBackpack"};

            for (String getter : containerGetters) {
                try {
                    Method method = inventory.getClass().getMethod(getter);
                    Object container = method.invoke(inventory);
                    if (container != null) {
                        int containerId = System.identityHashCode(container);
                        UUID existingOwner = containerOwners.get(containerId);

                        if (existingOwner != null && !existingOwner.equals(ownerUuid)) {
                            LOGGER.log(Level.WARNING,
                                "[HyzenKernel] ItemContainer from {0} already owned by {1}, now claimed by {2}",
                                new Object[]{getter, existingOwner, ownerUuid}
                            );
                        }
                        containerOwners.put(containerId, ownerUuid);
                    }
                } catch (NoSuchMethodException ignored) {
                    // Method doesn't exist, skip
                }
            }
        } catch (Exception e) {
            // Non-critical, just skip registration
        }
    }

    /**
     * Clone an Inventory using the CODEC (serialize -> deserialize).
     */
    private static Object cloneInventory(Object inventory) {
        // Try Method 1: CODEC-based cloning (most reliable)
        Object cloned = cloneViaCodec(inventory);
        if (cloned != null) {
            return cloned;
        }

        // Try Method 2: Clone individual containers
        cloned = cloneViaContainers(inventory);
        if (cloned != null) {
            return cloned;
        }

        LOGGER.log(Level.SEVERE, "[HyzenKernel] All inventory clone methods failed!");
        return null;
    }

    /**
     * Clone via CODEC serialization (like Entity.clone() does).
     */
    private static Object cloneViaCodec(Object inventory) {
        try {
            Class<?> inventoryClass = inventory.getClass();

            // Get CODEC field
            Field codecField = inventoryClass.getField("CODEC");
            Object codec = codecField.get(null);

            // Get ExtraInfo.THREAD_LOCAL
            Class<?> extraInfoClass = Class.forName("com.hypixel.hytale.codec.ExtraInfo");
            Field threadLocalField = extraInfoClass.getField("THREAD_LOCAL");
            ThreadLocal<?> threadLocal = (ThreadLocal<?>) threadLocalField.get(null);
            Object extraInfo = threadLocal.get();

            if (extraInfo == null) {
                // Create default ExtraInfo if not available
                return null;
            }

            // Encode inventory to BSON
            Method encodeMethod = findMethod(codec.getClass(), "encode", Object.class, extraInfoClass);
            if (encodeMethod == null) {
                return null;
            }
            Object bsonValue = encodeMethod.invoke(codec, inventory, extraInfo);

            // Create new inventory instance using default constructor
            Constructor<?> defaultConstructor = inventoryClass.getDeclaredConstructor();
            defaultConstructor.setAccessible(true);
            Object newInventory = defaultConstructor.newInstance();

            // Decode BSON into new instance
            Class<?> bsonValueClass = Class.forName("org.bson.BsonValue");
            Method decodeMethod = findMethod(codec.getClass(), "decode", bsonValueClass, Object.class, extraInfoClass);
            if (decodeMethod == null) {
                return null;
            }
            decodeMethod.invoke(codec, bsonValue, newInventory, extraInfo);

            return newInventory;

        } catch (Exception e) {
            // CODEC clone failed, will try fallback
            return null;
        }
    }

    /**
     * Clone by cloning individual ItemContainers.
     */
    private static Object cloneViaContainers(Object inventory) {
        try {
            Class<?> inventoryClass = inventory.getClass();

            // Get current containers
            Object storage = invokeGetter(inventory, "getStorage");
            Object armor = invokeGetter(inventory, "getArmor");
            Object hotbar = invokeGetter(inventory, "getHotbar");
            Object utility = invokeGetter(inventory, "getUtility");
            Object tools = invokeGetter(inventory, "getTools");
            Object backpack = invokeGetter(inventory, "getBackpack");

            // Clone each container
            Object clonedStorage = cloneContainer(storage);
            Object clonedArmor = cloneContainer(armor);
            Object clonedHotbar = cloneContainer(hotbar);
            Object clonedUtility = cloneContainer(utility);
            Object clonedTools = cloneContainer(tools);
            Object clonedBackpack = cloneContainer(backpack);

            // Find constructor that takes 6 ItemContainers
            Class<?> containerClass = Class.forName("com.hypixel.hytale.server.core.inventory.container.ItemContainer");
            Constructor<?> constructor = inventoryClass.getConstructor(
                containerClass, containerClass, containerClass,
                containerClass, containerClass, containerClass
            );

            return constructor.newInstance(
                clonedStorage, clonedArmor, clonedHotbar,
                clonedUtility, clonedTools, clonedBackpack
            );

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Clone a single ItemContainer using its clone() method.
     */
    private static Object cloneContainer(Object container) {
        if (container == null) {
            return null;
        }

        try {
            Method cloneMethod = container.getClass().getMethod("clone");
            return cloneMethod.invoke(container);
        } catch (Exception e) {
            // Can't clone, return original (better than null)
            return container;
        }
    }

    /**
     * Helper to invoke a getter method.
     */
    private static Object invokeGetter(Object obj, String methodName) {
        try {
            Method method = obj.getClass().getMethod(methodName);
            return method.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Helper to find a method by name and parameter types.
     */
    private static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            // Try finding in interfaces
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == paramTypes.length) {
                    return m;
                }
            }
            return null;
        }
    }

    /**
     * Clean up tracking for a player that disconnected.
     * Called by HyzenKernel runtime plugin on player disconnect.
     */
    public static void onPlayerDisconnect(UUID playerUuid) {
        if (playerUuid == null) {
            return;
        }

        int removed = 0;

        // Remove inventory entries for this player
        removed += inventoryOwners.entrySet().removeIf(
            entry -> playerUuid.equals(entry.getValue())
        ) ? 1 : 0;

        // Remove container entries for this player
        removed += containerOwners.entrySet().removeIf(
            entry -> playerUuid.equals(entry.getValue())
        ) ? 1 : 0;

        if (removed > 0) {
            LOGGER.log(Level.FINE,
                "[HyzenKernel] Cleaned up inventory tracking for disconnected player {0}",
                playerUuid.toString().substring(0, 8) + "..."
            );
        }
    }

    /**
     * Get statistics for the /hyzenkernel status command.
     */
    public static String getStats() {
        return String.format(
            "InventoryOwnershipGuard (Issue #45):\n" +
            "  Validation calls: %d\n" +
            "  Shared references detected: %d\n" +
            "  Inventories cloned: %d\n" +
            "  Tracked inventories: %d\n" +
            "  Tracked containers: %d\n" +
            "  Status: %s",
            validationCalls,
            sharedReferencesDetected,
            inventoriesCloned,
            inventoryOwners.size(),
            containerOwners.size(),
            sharedReferencesDetected == 0 ? "No issues detected" :
                (inventoriesCloned == sharedReferencesDetected ? "All issues resolved" : "ISSUES PRESENT")
        );
    }

    /**
     * Reset statistics (for testing).
     */
    public static void resetStats() {
        sharedReferencesDetected = 0;
        inventoriesCloned = 0;
        validationCalls = 0;
        inventoryOwners.clear();
        containerOwners.clear();
    }
}
