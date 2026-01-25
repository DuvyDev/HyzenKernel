package com.hyzenkernel;

import com.hyzenkernel.commands.ChunkProtectionCommand;
import com.hyzenkernel.commands.ChunkStatusCommand;
import com.hyzenkernel.commands.ChunkUnloadCommand;
import com.hyzenkernel.commands.CleanInteractionsCommand;
import com.hyzenkernel.commands.CleanWarpsCommand;
import com.hyzenkernel.commands.FixCounterCommand;
import com.hyzenkernel.commands.InteractionStatusCommand;
import com.hyzenkernel.commands.WhoCommand;
import com.hyzenkernel.config.ConfigManager;
import com.hyzenkernel.listeners.CraftingManagerSanitizer;
import com.hyzenkernel.listeners.EmptyArchetypeSanitizer;
import com.hyzenkernel.listeners.InteractionManagerSanitizer;
import com.hyzenkernel.listeners.GatherObjectiveTaskSanitizer;
import com.hyzenkernel.listeners.InstancePositionTracker;
import com.hyzenkernel.listeners.ProcessingBenchSanitizer;
import com.hyzenkernel.listeners.RespawnBlockSanitizer;
import com.hyzenkernel.listeners.DefaultWorldRecoverySanitizer;
import com.hyzenkernel.listeners.SpawnBeaconSanitizer;
import com.hyzenkernel.listeners.ChunkTrackerSanitizer;
import com.hyzenkernel.listeners.TeleporterProtectionListener;
import com.hyzenkernel.systems.ChunkCleanupSystem;
import com.hyzenkernel.systems.ChunkProtectionRegistry;
import com.hyzenkernel.systems.ChunkProtectionScanner;
import com.hyzenkernel.systems.ChunkUnloadManager;
import com.hyzenkernel.systems.InteractionChainMonitor;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * HyzenKernel - Bug fixes for Hytale Early Access
 *
 * This plugin contains workarounds for known Hytale server bugs
 * that may cause crashes or unexpected behavior.
 *
 * Current fixes:
 * - RespawnBlockSanitizer: Prevents crash when breaking respawn blocks with null respawnPoints
 * - ProcessingBenchSanitizer: Prevents crash when breaking processing benches with open windows
 * - EmptyArchetypeSanitizer: Monitors for entities with invalid state (empty archetypes)
 * - InstancePositionTracker: Prevents kick when exiting instances with missing return world
 * - ChunkUnloadManager: Aggressively unloads chunks to prevent memory bloat (v1.2.0)
 * - GatherObjectiveTaskSanitizer: Prevents crash from null refs in quest objectives (v1.3.0)
 * - InteractionChainMonitor: Tracks unfixable Hytale bugs for reporting (v1.3.0)
 * - CraftingManagerSanitizer: Prevents crash from stale bench references (v1.3.1)
 * - InteractionManagerSanitizer: Prevents NPE crash when opening crafttables (v1.3.1, Issue #1)
 * - SpawnBeaconSanitizer: Prevents crash from null spawn parameters in BeaconSpawnController (v1.3.7, Issue #4)
 * - [MOVED TO EARLY PLUGIN] SpawnMarkerReferenceSanitizer: Now fixed via bytecode transformation (v1.4.0)
 * - ChunkTrackerSanitizer: Prevents crash from invalid PlayerRefs during chunk unload (v1.3.9, Issue #6)
 */
public class HyzenKernel extends JavaPlugin {

    private static HyzenKernel instance;
    private InstancePositionTracker instancePositionTracker;
    private ChunkUnloadManager chunkUnloadManager;
    private ChunkCleanupSystem chunkCleanupSystem;
    private ChunkProtectionRegistry chunkProtectionRegistry;
    private ChunkProtectionScanner chunkProtectionScanner;
    private GatherObjectiveTaskSanitizer gatherObjectiveTaskSanitizer;
    private InteractionChainMonitor interactionChainMonitor;
    private CraftingManagerSanitizer craftingManagerSanitizer;
    private InteractionManagerSanitizer interactionManagerSanitizer;
    private SpawnBeaconSanitizer spawnBeaconSanitizer;
    private ChunkTrackerSanitizer chunkTrackerSanitizer;
    private DefaultWorldRecoverySanitizer defaultWorldRecoverySanitizer;
    private TeleporterProtectionListener teleporterProtectionListener;

    public HyzenKernel(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        getLogger().at(Level.INFO).log("HyzenKernel is loading...");
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("Setting up HyzenKernel...");

        // Initialize configuration first
        ConfigManager config = ConfigManager.getInstance();
        if (config.isLoadedFromFile()) {
            getLogger().at(Level.INFO).log("[CONFIG] Loaded configuration from mods/hyzenkernel/config.json");
        } else {
            getLogger().at(Level.INFO).log("[CONFIG] Using default configuration (config.json generated)");
        }
        if (config.hasEnvironmentOverrides()) {
            getLogger().at(Level.INFO).log("[CONFIG] Environment variable overrides applied");
        }

        // Register bug fix systems
        registerBugFixes();

        getLogger().at(Level.INFO).log("HyzenKernel setup complete!");
    }

    private void registerBugFixes() {
        ConfigManager config = ConfigManager.getInstance();

        // Fix 1: RespawnBlock null respawnPoints crash
        // Hytale's RespawnBlock$OnRemove.onEntityRemove() crashes if respawnPoints is null
        if (config.isSanitizerEnabled("respawnBlock")) {
            getChunkStoreRegistry().registerSystem(new RespawnBlockSanitizer(this));
            getLogger().at(Level.INFO).log("[FIX] RespawnBlockSanitizer registered - prevents crash when breaking respawn blocks");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] RespawnBlockSanitizer - disabled via config");
        }

        // Fix 2: ProcessingBench window NPE crash
        // Hytale's ProcessingBenchState.onDestroy() crashes when windows have null refs
        if (config.isSanitizerEnabled("processingBench")) {
            getChunkStoreRegistry().registerSystem(new ProcessingBenchSanitizer(this));
            getLogger().at(Level.INFO).log("[FIX] ProcessingBenchSanitizer registered - prevents crash when breaking benches with open windows");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] ProcessingBenchSanitizer - disabled via config");
        }

        // Fix 3: Empty archetype entity monitoring
        // Monitors for entities with invalid state (empty archetypes)
        if (config.isSanitizerEnabled("emptyArchetype")) {
            getEntityStoreRegistry().registerSystem(new EmptyArchetypeSanitizer(this));
            getLogger().at(Level.INFO).log("[FIX] EmptyArchetypeSanitizer registered - monitors for invalid entity states");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] EmptyArchetypeSanitizer - disabled via config");
        }

        // Fix 4: Instance exit missing return world crash
        // Tracks player positions before entering instances and restores them if exit fails
        if (config.isSanitizerEnabled("instancePositionTracker")) {
            instancePositionTracker = new InstancePositionTracker(this);
            instancePositionTracker.register();
            getLogger().at(Level.INFO).log("[FIX] InstancePositionTracker registered - prevents crash when exiting instances with missing return world");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] InstancePositionTracker - disabled via config");
        }

        // Fix 5: Chunk memory bloat - chunks not unloading (v1.2.0)
        // Uses reflection to discover and call chunk unload APIs
        // NOTE: Can be disabled via config or HYFIXES_DISABLE_CHUNK_UNLOAD=true for BetterMap compatibility (Issue #21)
        if (!config.isChunkUnloadEnabled()) {
            getLogger().at(Level.INFO).log("[DISABLED] ChunkUnloadManager - disabled via config");
            getLogger().at(Level.INFO).log("[DISABLED] This improves compatibility with BetterMap and other map plugins");
            chunkUnloadManager = null;
            chunkCleanupSystem = null;
        } else {
            chunkUnloadManager = new ChunkUnloadManager(this);

            // Fix 5b: Main-thread chunk cleanup system (v1.2.2)
            // Runs cleanup methods on the main server thread to avoid InvocationTargetException
            chunkCleanupSystem = new ChunkCleanupSystem(this);
            getEntityStoreRegistry().registerSystem(chunkCleanupSystem);
            getLogger().at(Level.INFO).log("[FIX] ChunkCleanupSystem registered - runs cleanup on main thread");
            
            // Fix 5c: Chunk protection system (v1.4.2)
            // Prevents cleanup of chunks containing teleporters, portals, and other important content
            if (config.isChunkProtectionEnabled()) {
                chunkProtectionRegistry = new ChunkProtectionRegistry(this);
                chunkProtectionScanner = new ChunkProtectionScanner(this, chunkProtectionRegistry);
                
                // Wire up protection to cleanup system
                chunkCleanupSystem.setChunkProtection(chunkProtectionRegistry, chunkProtectionScanner);
                
                // Try to get world reference for scanning
                try {
                    World world = Universe.get().getWorld("default");
                    if (world != null) {
                        chunkCleanupSystem.setWorld(world);
                    }
                } catch (Exception e) {
                    getLogger().at(Level.INFO).log("[PROT] World not available yet - will be set later");
                }
                
                getLogger().at(Level.INFO).log("[PROT] ChunkProtectionSystem registered - protects teleporters and portals from cleanup");

                // Register teleporter listener for real-time protection updates
                teleporterProtectionListener = new TeleporterProtectionListener(this, chunkProtectionRegistry);
                if (teleporterProtectionListener.isInitialized()) {
                    getChunkStoreRegistry().registerSystem(teleporterProtectionListener);
                    chunkCleanupSystem.setTeleporterListener(teleporterProtectionListener);
                    getLogger().at(Level.INFO).log("[PROT] TeleporterProtectionListener registered - auto-protects new teleporters");
                } else {
                    getLogger().at(Level.WARNING).log("[PROT] TeleporterProtectionListener not initialized - teleporter events won't be monitored");
                }

            } else {
                getLogger().at(Level.INFO).log("[DISABLED] ChunkProtectionSystem - disabled via config");
            }

            // Wire up the systems
            chunkUnloadManager.setChunkCleanupSystem(chunkCleanupSystem);
            if (chunkProtectionRegistry != null) {
                chunkUnloadManager.setProtectionRegistry(chunkProtectionRegistry);
            }
            chunkUnloadManager.start();
            getLogger().at(Level.INFO).log("[FIX] ChunkUnloadManager registered - aggressively unloads unused chunks");

            // Enable Map-Aware Mode if configured (v1.8.0) - BetterMaps compatibility
            if (config.isMapAwareModeEnabled()) {
                try {
                    World defaultWorld = Universe.get().getWorld("default");
                    if (defaultWorld != null) {
                        chunkUnloadManager.enableMapAwareMode(defaultWorld);
                    } else {
                        getLogger().at(Level.WARNING).log("[MapAware] Could not enable - default world not available yet");
                        chunkUnloadManager.setMapAwareModeRequested(true);
                        getLogger().at(Level.INFO).log("[MapAware] Map-Aware mode will try to initialize on first cleanup cycle");
                    }
                } catch (Exception e) {
                    getLogger().at(Level.WARNING).log("[MapAware] Could not enable map-aware mode: " + e.getMessage());
                }
            } else {
                getLogger().at(Level.INFO).log("[TIP] Enable mapAwareMode in config.json for BetterMap compatibility");
            }
        }

        // Fix 6: GatherObjectiveTask null ref crash (v1.3.0)
        // Validates refs in quest objectives before they can crash
        if (config.isSanitizerEnabled("gatherObjective")) {
            gatherObjectiveTaskSanitizer = new GatherObjectiveTaskSanitizer(this);
            getEntityStoreRegistry().registerSystem(gatherObjectiveTaskSanitizer);
            getLogger().at(Level.INFO).log("[FIX] GatherObjectiveTaskSanitizer registered - prevents crash from null refs in quest objectives");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] GatherObjectiveTaskSanitizer - disabled via config");
        }

        // Fix 7: InteractionChain monitoring (v1.3.0)
        // Tracks unfixable Hytale bugs for reporting to developers
        interactionChainMonitor = new InteractionChainMonitor(this);
        getEntityStoreRegistry().registerSystem(interactionChainMonitor);
        getLogger().at(Level.INFO).log("[MON] InteractionChainMonitor registered - tracks HyzenKernel statistics");

        // Fix 8: CraftingManager bench already set crash (v1.3.1)
        // Clears stale bench references before they cause IllegalArgumentException
        if (config.isSanitizerEnabled("craftingManager")) {
            craftingManagerSanitizer = new CraftingManagerSanitizer(this);
            getEntityStoreRegistry().registerSystem(craftingManagerSanitizer);
            getLogger().at(Level.INFO).log("[FIX] CraftingManagerSanitizer registered - prevents bench already set crash");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] CraftingManagerSanitizer - disabled via config");
        }

        // Fix 9: InteractionManager NPE crash when opening crafttables (v1.3.1)
        // GitHub Issue: https://github.com/HyzenNet/Kernel/issues/1
        // Validates interaction chains and removes ones with null context before they cause NPE
        if (config.isSanitizerEnabled("interactionManager")) {
            interactionManagerSanitizer = new InteractionManagerSanitizer(this);
            getEntityStoreRegistry().registerSystem(interactionManagerSanitizer);
            getLogger().at(Level.INFO).log("[FIX] InteractionManagerSanitizer registered - prevents crafttable interaction crash");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] InteractionManagerSanitizer - disabled via config");
        }

        // Fix 10: SpawnBeacon null RoleSpawnParameters crash (v1.3.7)
        // GitHub Issue: https://github.com/HyzenNet/Kernel/issues/4
        // Validates spawn parameters before BeaconSpawnController.createRandomSpawnJob() can crash
        if (config.isSanitizerEnabled("spawnBeacon")) {
            spawnBeaconSanitizer = new SpawnBeaconSanitizer(this);
            getEntityStoreRegistry().registerSystem(spawnBeaconSanitizer);
            getLogger().at(Level.INFO).log("[FIX] SpawnBeaconSanitizer registered - prevents spawn beacon null parameter crash");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] SpawnBeaconSanitizer - disabled via config");
        }

        // Fix 11: SpawnMarkerReference null npcReferences crash (v1.3.8)
        // GitHub Issue: https://github.com/HyzenNet/Kernel/issues/5
        // MOVED TO EARLY PLUGIN in v1.4.0 - Now fixed via bytecode transformation
        // The early plugin transforms SpawnMarkerEntity constructor to initialize npcReferences
        getLogger().at(Level.INFO).log("[MOVED] SpawnMarkerReferenceSanitizer - now fixed via early plugin bytecode transformation");

        // Fix 14: Default World Recovery (v1.4.3)
        // GitHub Issue: https://github.com/HyzenNet/Kernel/issues/23
        // Automatically reloads the default world when it crashes exceptionally
        if (config.isSanitizerEnabled("defaultWorldRecovery")) {
            defaultWorldRecoverySanitizer = new DefaultWorldRecoverySanitizer(this);
            defaultWorldRecoverySanitizer.register();
            getLogger().at(Level.INFO).log("[FIX] DefaultWorldRecoverySanitizer registered - auto-recovers default world after crash");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] DefaultWorldRecoverySanitizer - disabled via config");
        }

        // Fix 12: ChunkTracker null PlayerRef crash (v1.3.9)
        // GitHub Issue: https://github.com/HyzenNet/Kernel/issues/6
        // Prevents world crash when ChunkTracker has invalid PlayerRefs after player disconnect
        if (config.isSanitizerEnabled("chunkTracker")) {
            chunkTrackerSanitizer = new ChunkTrackerSanitizer(this);
            getEntityStoreRegistry().registerSystem(chunkTrackerSanitizer);
            getLogger().at(Level.INFO).log("[FIX] ChunkTrackerSanitizer registered - prevents chunk unload crash after player disconnect");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] ChunkTrackerSanitizer - disabled via config");
        }

        // Register admin commands
        registerCommands();
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new ChunkProtectionCommand(this));
        getCommandRegistry().registerCommand(new ChunkStatusCommand(this));
        getCommandRegistry().registerCommand(new ChunkUnloadCommand(this));
        getCommandRegistry().registerCommand(new CleanInteractionsCommand(this));
        getCommandRegistry().registerCommand(new CleanWarpsCommand(this));
        getCommandRegistry().registerCommand(new FixCounterCommand(this));
        getCommandRegistry().registerCommand(new InteractionStatusCommand(this));
        getCommandRegistry().registerCommand(new WhoCommand());
        getLogger().at(Level.INFO).log("[CMD] Registered /chunkprotect, /chunkstatus, /chunkunload, /fixcounter, /interactionstatus, and /who commands");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("HyzenKernel has started! " + getFixCount() + " bug fix(es) active.");
    }

    @Override
    protected void shutdown() {
        // Stop the chunk unload manager
        if (chunkUnloadManager != null) {
            chunkUnloadManager.stop();
        }

        getLogger().at(Level.INFO).log("HyzenKernel has been disabled.");
    }

    private int getFixCount() {
        // Base fixes: RespawnBlockSanitizer, ProcessingBenchSanitizer, EmptyArchetypeSanitizer,
        // InstancePositionTracker, GatherObjectiveTaskSanitizer, InteractionChainMonitor,
        // CraftingManagerSanitizer, InteractionManagerSanitizer, SpawnBeaconSanitizer, ChunkTrackerSanitizer
        // (SpawnMarkerReferenceSanitizer moved to early plugin)
        int count = 10;
        // ChunkUnloadManager + ChunkCleanupSystem (optional, can be disabled for BetterMap compatibility)
        if (chunkUnloadManager != null) {
            count += 2;
        }
        return count;
    }

    public static HyzenKernel getInstance() {
        return instance;
    }

    /**
     * Get the ChunkUnloadManager for commands and status.
     */
    public ChunkUnloadManager getChunkUnloadManager() {
        return chunkUnloadManager;
    }

    /**
     * Get the ChunkCleanupSystem for commands and status.
     */
    public ChunkCleanupSystem getChunkCleanupSystem() {
        return chunkCleanupSystem;
    }

    /**
     * Get the GatherObjectiveTaskSanitizer for commands and status.
     */
    public GatherObjectiveTaskSanitizer getGatherObjectiveTaskSanitizer() {
        return gatherObjectiveTaskSanitizer;
    }

    /**
     * Get the InteractionChainMonitor for commands and status.
     */
    public InteractionChainMonitor getInteractionChainMonitor() {
        return interactionChainMonitor;
    }

    /**
     * Get the CraftingManagerSanitizer for commands and status.
     */
    public CraftingManagerSanitizer getCraftingManagerSanitizer() {
        return craftingManagerSanitizer;
    }

    /**
     * Get the InteractionManagerSanitizer for commands and status.
     */
    public InteractionManagerSanitizer getInteractionManagerSanitizer() {
        return interactionManagerSanitizer;
    }

    /**
     * Get the SpawnBeaconSanitizer for commands and status.
     */
    public SpawnBeaconSanitizer getSpawnBeaconSanitizer() {
        return spawnBeaconSanitizer;
    }

    /**
     * Get the ChunkTrackerSanitizer for commands and status.
     */
    public ChunkTrackerSanitizer getChunkTrackerSanitizer() {
        return chunkTrackerSanitizer;
    }

    /**
     * Get the ChunkProtectionRegistry for commands and status.
     */
    public ChunkProtectionRegistry getChunkProtectionRegistry() {
        return chunkProtectionRegistry;
    }
    
    /**
     * Get the ChunkProtectionScanner for commands and status.
     */
    public ChunkProtectionScanner getChunkProtectionScanner() {
        return chunkProtectionScanner;
    }

    /**
     * Get the DefaultWorldRecoverySanitizer for commands and status.
     */
    public DefaultWorldRecoverySanitizer getDefaultWorldRecoverySanitizer() {
        return defaultWorldRecoverySanitizer;
    }
}
