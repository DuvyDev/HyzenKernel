package com.hyzenkernel.config;

/**
 * HyzenKernel Configuration - Contains all runtime and early plugin settings.
 * 
 * This class is deserialized from config.json using Gson.
 * All fields have default values that match the original hardcoded behavior.
 */
public class HyzenKernelConfig {

    // Sanitizer toggles
    public SanitizersConfig sanitizers = new SanitizersConfig();
    
    // Interaction manager settings
    public InteractionManagerConfig interactionManager = new InteractionManagerConfig();
    
    // Instance tracker settings
    public InstanceTrackerConfig instanceTracker = new InstanceTrackerConfig();
    
    // Monitor settings
    public MonitorConfig monitor = new MonitorConfig();
    
    // Empty archetype settings
    public EmptyArchetypeConfig emptyArchetype = new EmptyArchetypeConfig();
    
    // Logging settings
    public LoggingConfig logging = new LoggingConfig();
    
    // Transformer toggles (for early plugin)
    public TransformersConfig transformers = new TransformersConfig();
    
    // World transformer settings
    public WorldConfig world = new WorldConfig();
    
    // Early plugin logging
    public EarlyConfig early = new EarlyConfig();

    // Interaction timeout settings (for early plugin)
    public InteractionTimeoutConfig interactionTimeout = new InteractionTimeoutConfig();

    /**
     * Sanitizer toggle configuration
     */
    public static class SanitizersConfig {
        public boolean respawnBlock = true;
        public boolean processingBench = true;
        public boolean craftingManager = true;
        public boolean interactionManager = true;
        public boolean spawnBeacon = true;
        public boolean chunkTracker = true;
        public boolean gatherObjective = true;
        public boolean emptyArchetype = true;
        public boolean instancePositionTracker = true;
        public boolean sharedInstancePersistence = true;
        public boolean defaultWorldRecovery = true;  // Auto-reload default world after crash
    }

    /**
     * Interaction manager configuration
     */
    public static class InteractionManagerConfig {
        public long clientTimeoutMs = 2000;
    }

    /**
     * Instance tracker configuration
     */
    public static class InstanceTrackerConfig {
        public int positionTtlHours = 24;
    }

    /**
     * Monitor configuration
     */
    public static class MonitorConfig {
        public int logIntervalTicks = 6000; // 5 minutes at 20 TPS
    }

    /**
     * Empty archetype configuration
     */
    public static class EmptyArchetypeConfig {
        public int skipFirstN = 1000;
        public int logEveryN = 10000;
    }

    /**
     * Logging configuration
     */
    public static class LoggingConfig {
        public boolean verbose = false;
        public boolean sanitizerActions = true;
    }

    /**
     * Transformer toggle configuration (for early plugin)
     */
    public static class TransformersConfig {
        public boolean interactionChain = true;
        public boolean interactionManager = true;
        public boolean world = true;
        public boolean spawnReferenceSystems = true;
        public boolean beaconSpawnController = true;
        public boolean blockComponentChunk = true;
        public boolean spawnMarkerSystems = true;
        public boolean spawnMarkerEntity = true;
        public boolean trackedPlacement = true;
        public boolean commandBuffer = true;
        public boolean worldMapTracker = true;
        public boolean archetypeChunk = true;
        public boolean interactionTimeout = true;
        public boolean uuidSystem = true;
        public boolean tickingThread = true;
        public boolean universeRemovePlayer = true;
        public boolean worldSpawningSystem = true;
        public boolean staticSharedInstances = true;
    }

    /**
     * World transformer configuration
     */
    public static class WorldConfig {
        public int retryCount = 5;
        public long retryDelayMs = 20;
    }

    /**
     * Early plugin configuration
     */
    public static class EarlyConfig {
        public EarlyLoggingConfig logging = new EarlyLoggingConfig();
    }

    /**
     * Early plugin logging configuration
     */
    public static class EarlyLoggingConfig {
        public boolean verbose = false;
    }

    /**
     * Interaction timeout configuration
     * Controls how long the server waits for client responses during interactions
     */
    public static class InteractionTimeoutConfig {
        /** Base timeout in milliseconds (added to ping-based calculation) */
        public long baseTimeoutMs = 6000;

        /** Multiplier applied to average ping */
        public double pingMultiplier = 3.0;
    }


    // ============================================
    // Convenience setter methods for runtime config updates
    // ============================================

    /**
     * Set verbose logging.
     */
    public void setVerbose(boolean verbose) {
        this.logging.verbose = verbose;
    }

    /**
     * Check if verbose logging is enabled.
     */
    public boolean isVerbose() {
        return this.logging.verbose;
    }

    /**
     * Set sanitizer action logging.
     */
    public void setLogSanitizerActions(boolean enabled) {
        this.logging.sanitizerActions = enabled;
    }

    /**
     * Check if sanitizer action logging is enabled.
     */
    public boolean logSanitizerActions() {
        return this.logging.sanitizerActions;
    }

}
