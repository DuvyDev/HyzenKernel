package com.hyzenkernel;

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
import com.hyzenkernel.systems.InteractionChainMonitor;
import com.hyzenkernel.systems.SharedInstancePersistenceSystem;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

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
 * - SharedInstancePersistenceSystem: Keeps shared portal instance terrain persistent
 * - GatherObjectiveTaskSanitizer: Prevents crash from null refs in quest objectives (v1.3.0)
 * - InteractionChainMonitor: Tracks unfixable Hytale bugs for reporting (v1.3.0)
 * - CraftingManagerSanitizer: Prevents crash from stale bench references (v1.3.1)
 * - InteractionManagerSanitizer: Prevents NPE crash when opening crafttables (v1.3.1, Issue #1)
 * - SpawnBeaconSanitizer: Prevents crash from null spawn parameters in BeaconSpawnController (v1.3.7, Issue #4)
 * - [MOVED TO EARLY PLUGIN] SpawnMarkerReferenceSanitizer: Now fixed via bytecode transformation (v1.4.0)
 * - ChunkTrackerSanitizer: Prevents crash from invalid PlayerRefs after player disconnect (v1.3.9, Issue #6)
 */
public class HyzenKernel extends JavaPlugin {

    private static HyzenKernel instance;
    private InstancePositionTracker instancePositionTracker;
    private GatherObjectiveTaskSanitizer gatherObjectiveTaskSanitizer;
    private InteractionChainMonitor interactionChainMonitor;
    private CraftingManagerSanitizer craftingManagerSanitizer;
    private InteractionManagerSanitizer interactionManagerSanitizer;
    private SpawnBeaconSanitizer spawnBeaconSanitizer;
    private ChunkTrackerSanitizer chunkTrackerSanitizer;
    private DefaultWorldRecoverySanitizer defaultWorldRecoverySanitizer;

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

        // Fix 5: Shared portal instance persistence (static terrain)
        // Prevents shared instance worlds from being deleted on unload
        if (config.isSanitizerEnabled("sharedInstancePersistence")) {
            getChunkStoreRegistry().registerSystem(new SharedInstancePersistenceSystem(this));
            getLogger().at(Level.INFO).log("[FIX] SharedInstancePersistenceSystem registered - keeps shared portal terrain on disk");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] SharedInstancePersistenceSystem - disabled via config");
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
            getLogger().at(Level.INFO).log("[FIX] ChunkTrackerSanitizer registered - prevents crash from invalid PlayerRefs after player disconnect");
        } else {
            getLogger().at(Level.INFO).log("[DISABLED] ChunkTrackerSanitizer - disabled via config");
        }

        // Register admin commands
        registerCommands();
    }

    private void registerCommands() {
        getCommandRegistry().registerCommand(new CleanInteractionsCommand(this));
        getCommandRegistry().registerCommand(new CleanWarpsCommand(this));
        getCommandRegistry().registerCommand(new FixCounterCommand(this));
        getCommandRegistry().registerCommand(new InteractionStatusCommand(this));
        getCommandRegistry().registerCommand(new WhoCommand());
        getLogger().at(Level.INFO).log("[CMD] Registered /cleaninteractions, /cleanwarps, /fixcounter, /interactionstatus, and /who commands");
    }

    @Override
    protected void start() {
        getLogger().at(Level.INFO).log("HyzenKernel has started! " + getFixCount() + " bug fix(es) active.");
    }

    @Override
    protected void shutdown() {
        getLogger().at(Level.INFO).log("HyzenKernel has been disabled.");
    }

    private int getFixCount() {
        // Base fixes: RespawnBlockSanitizer, ProcessingBenchSanitizer, EmptyArchetypeSanitizer,
        // InstancePositionTracker, GatherObjectiveTaskSanitizer, InteractionChainMonitor,
        // CraftingManagerSanitizer, InteractionManagerSanitizer, SpawnBeaconSanitizer, ChunkTrackerSanitizer
        // (SpawnMarkerReferenceSanitizer moved to early plugin)
        int count = 10;
        return count;
    }

    public static HyzenKernel getInstance() {
        return instance;
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
     * Get the DefaultWorldRecoverySanitizer for commands and status.
     */
    public DefaultWorldRecoverySanitizer getDefaultWorldRecoverySanitizer() {
        return defaultWorldRecoverySanitizer;
    }
}
