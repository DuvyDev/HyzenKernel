package com.hyzenkernel.util;

import com.hyzenkernel.HyzenKernel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * ReflectionHelper - Discovers hidden Hytale APIs for chunk management
 *
 * This class uses reflection to scan the World class and find chunk-related
 * methods that may not be part of the public API but can be used to force
 * chunk unloading.
 */
public class ReflectionHelper {

    private final HyzenKernel plugin;

    // Discovered APIs - cached for reuse
    private Method unloadChunkMethod = null;
    private Method getLoadedChunksMethod = null;
    private Field chunkManagerField = null;

    // Discovery results
    private final List<String> discoveredMethods = new ArrayList<>();
    private final List<String> discoveredFields = new ArrayList<>();
    private final List<String> chunkMethods = new ArrayList<>();

    public ReflectionHelper(HyzenKernel plugin) {
        this.plugin = plugin;
    }

    /**
     * Scans an object's class for all methods and fields.
     * Focuses on finding chunk-related APIs.
     */
    public void discoverAPIs(Object target, String targetName) {
        if (target == null) {
            plugin.getLogger().at(Level.WARNING).log(
                "[ReflectionHelper] Cannot scan null target: " + targetName
            );
            return;
        }

        Class<?> clazz = target.getClass();
        plugin.getLogger().at(Level.INFO).log(
            "[ReflectionHelper] Scanning " + targetName + " (" + clazz.getName() + ")"
        );

        // Scan public methods
        scanMethods(clazz.getMethods(), "PUBLIC");

        // Scan declared methods (including private)
        try {
            scanMethods(clazz.getDeclaredMethods(), "DECLARED");
        } catch (SecurityException e) {
            plugin.getLogger().at(Level.WARNING).log(
                "[ReflectionHelper] Security exception scanning declared methods: " + e.getMessage()
            );
        }

        // Scan fields
        try {
            scanFields(clazz.getDeclaredFields());
        } catch (SecurityException e) {
            plugin.getLogger().at(Level.WARNING).log(
                "[ReflectionHelper] Security exception scanning fields: " + e.getMessage()
            );
        }

        // Log summary
        logSummary();
    }

    private void scanMethods(Method[] methods, String scope) {
        for (Method m : methods) {
            String methodSig = formatMethod(m);
            discoveredMethods.add(scope + ": " + methodSig);

            // Check for chunk-related methods
            String nameLower = m.getName().toLowerCase();
            if (nameLower.contains("chunk") ||
                nameLower.contains("unload") ||
                nameLower.contains("load") && nameLower.contains("region")) {

                chunkMethods.add(methodSig);

                // Try to make accessible for later use
                try {
                    m.setAccessible(true);

                    // Look for specific methods we need
                    if (nameLower.contains("unload") && nameLower.contains("chunk")) {
                        unloadChunkMethod = m;
                        plugin.getLogger().at(Level.INFO).log(
                            "[ReflectionHelper] FOUND unloadChunk method: " + methodSig
                        );
                    }

                    if ((nameLower.contains("get") || nameLower.contains("loaded")) &&
                        nameLower.contains("chunk")) {
                        getLoadedChunksMethod = m;
                        plugin.getLogger().at(Level.INFO).log(
                            "[ReflectionHelper] FOUND getLoadedChunks method: " + methodSig
                        );
                    }

                } catch (Exception e) {
                    plugin.getLogger().at(Level.FINE).log(
                        "[ReflectionHelper] Cannot access method: " + methodSig
                    );
                }
            }
        }
    }

    private void scanFields(Field[] fields) {
        for (Field f : fields) {
            String fieldSig = formatField(f);
            discoveredFields.add(fieldSig);

            // Check for chunk-related fields
            String nameLower = f.getName().toLowerCase();
            String typeLower = f.getType().getSimpleName().toLowerCase();

            if (nameLower.contains("chunk") || typeLower.contains("chunk") ||
                nameLower.contains("region") || typeLower.contains("region")) {

                try {
                    f.setAccessible(true);
                    chunkManagerField = f;
                    plugin.getLogger().at(Level.INFO).log(
                        "[ReflectionHelper] FOUND chunk field: " + fieldSig
                    );
                } catch (Exception e) {
                    plugin.getLogger().at(Level.FINE).log(
                        "[ReflectionHelper] Cannot access field: " + fieldSig
                    );
                }
            }
        }
    }

    private String formatMethod(Method m) {
        String params = Arrays.stream(m.getParameterTypes())
            .map(Class::getSimpleName)
            .collect(Collectors.joining(", "));
        String modifiers = Modifier.toString(m.getModifiers());
        return String.format("%s %s %s(%s)",
            modifiers,
            m.getReturnType().getSimpleName(),
            m.getName(),
            params
        );
    }

    private String formatField(Field f) {
        String modifiers = Modifier.toString(f.getModifiers());
        return String.format("%s %s %s",
            modifiers,
            f.getType().getSimpleName(),
            f.getName()
        );
    }

    private void logSummary() {
        plugin.getLogger().at(Level.INFO).log(
            "[ReflectionHelper] Discovery complete: " +
            discoveredMethods.size() + " methods, " +
            discoveredFields.size() + " fields, " +
            chunkMethods.size() + " chunk-related APIs"
        );

        // Log all chunk methods found
        if (!chunkMethods.isEmpty()) {
            plugin.getLogger().at(Level.INFO).log("[ReflectionHelper] Chunk-related methods:");
            for (String method : chunkMethods) {
                plugin.getLogger().at(Level.INFO).log("  - " + method);
            }
        } else {
            plugin.getLogger().at(Level.WARNING).log(
                "[ReflectionHelper] No chunk-related methods found on World class!"
            );
        }
    }

    /**
     * Returns true if we found a method to unload chunks.
     */
    public boolean hasUnloadCapability() {
        return unloadChunkMethod != null || chunkManagerField != null;
    }

    /**
     * Returns true if we found a method to get loaded chunks.
     */
    public boolean hasGetLoadedChunksCapability() {
        return getLoadedChunksMethod != null;
    }

    /**
     * Get the discovered unload method.
     */
    public Method getUnloadChunkMethod() {
        return unloadChunkMethod;
    }

    /**
     * Get the discovered get loaded chunks method.
     */
    public Method getGetLoadedChunksMethod() {
        return getLoadedChunksMethod;
    }

    /**
     * Get the discovered chunk manager field.
     */
    public Field getChunkManagerField() {
        return chunkManagerField;
    }

    /**
     * Get all discovered chunk-related method signatures.
     */
    public List<String> getChunkMethods() {
        return new ArrayList<>(chunkMethods);
    }

    /**
     * Get all discovered method signatures.
     */
    public List<String> getAllMethods() {
        return new ArrayList<>(discoveredMethods);
    }

    /**
     * Get all discovered field signatures.
     */
    public List<String> getAllFields() {
        return new ArrayList<>(discoveredFields);
    }

    /**
     * Try to invoke the unload chunk method on a target.
     * Returns true if successful.
     */
    public boolean tryUnloadChunk(Object target, Object... args) {
        if (unloadChunkMethod == null) {
            return false;
        }

        try {
            unloadChunkMethod.invoke(target, args);
            return true;
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log(
                "[ReflectionHelper] Failed to invoke unloadChunk: " + e.getMessage()
            );
            return false;
        }
    }

    /**
     * Try to get loaded chunks from a target.
     * Returns null if not available.
     */
    @SuppressWarnings("unchecked")
    public Object tryGetLoadedChunks(Object target) {
        if (getLoadedChunksMethod == null) {
            return null;
        }

        try {
            return getLoadedChunksMethod.invoke(target);
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log(
                "[ReflectionHelper] Failed to invoke getLoadedChunks: " + e.getMessage()
            );
            return null;
        }
    }

    /**
     * Try to get the chunk manager from a target.
     * Returns null if not available.
     */
    public Object tryGetChunkManager(Object target) {
        if (chunkManagerField == null) {
            return null;
        }

        try {
            return chunkManagerField.get(target);
        } catch (Exception e) {
            plugin.getLogger().at(Level.WARNING).log(
                "[ReflectionHelper] Failed to get chunkManager field: " + e.getMessage()
            );
            return null;
        }
    }
}
