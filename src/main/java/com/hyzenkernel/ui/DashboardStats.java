package com.hyzenkernel.ui;

import com.hyzenkernel.HyzenKernel;
import com.hyzenkernel.config.ConfigManager;
import com.hyzenkernel.config.HyzenKernelConfig;
import com.hyzenkernel.listeners.*;
import com.hyzenkernel.systems.*;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class that collects statistics from all HyzenKernel systems
 * for display in the dashboard UI.
 */
public class DashboardStats {

    // Collected statistics
    private int totalCrashesPrevented = 0;
    private int activeSanitizers = 0;
    private int totalSanitizers = 0;
    private int protectedChunks = 0;
    private int chunkUnloadAttempts = 0;
    private int chunkUnloadSuccesses = 0;
    private String healthStatus = "Unknown";
    private String healthColor = "#aaaaaa";
    private String memoryUsage = "Unknown";
    private String memoryColor = "#ffffff";
    private String uptime = "Unknown";
    private String pluginVersion = "Unknown";
    private String lastCleanupTime = "Never";
    private int cleanupInterval = 0;
    private boolean chunkProtectionEnabled = false;
    private boolean mapAwareModeEnabled = false;

    private final List<SanitizerInfo> sanitizerInfos = new ArrayList<>();

    /**
     * Collect statistics from all HyzenKernel systems.
     *
     * @return A populated DashboardStats instance
     */
    public static DashboardStats collect() {
        DashboardStats stats = new DashboardStats();
        HyzenKernel plugin = HyzenKernel.getInstance();
        ConfigManager configManager = ConfigManager.getInstance();
        HyzenKernelConfig config = configManager.getConfig();

        // Plugin version
        stats.pluginVersion = retrievePluginVersion();

        // Memory statistics
        stats.collectMemoryStats();

        // Server uptime
        stats.collectUptime();

        // Config status
        stats.chunkProtectionEnabled = config.isChunkProtectionEnabled();
        stats.mapAwareModeEnabled = config.isMapAwareModeEnabled();
        stats.cleanupInterval = configManager.getChunkCleanupIntervalTicks();

        // Collect from sanitizers
        stats.collectSanitizerStats(plugin, config);

        // Collect chunk statistics
        stats.collectChunkStats(plugin);

        // Calculate overall health
        stats.calculateHealth();

        return stats;
    }

    /**
     * Retrieve plugin version from build info.
     */
    private static String retrievePluginVersion() {
        try {
            // Try to get version from plugin manifest or class
            Package pkg = HyzenKernel.class.getPackage();
            if (pkg != null && pkg.getImplementationVersion() != null) {
                return pkg.getImplementationVersion();
            }
        } catch (Exception ignored) {
        }
        return "1.8.x"; // Fallback
    }

    /**
     * Collect memory usage statistics.
     */
    private void collectMemoryStats() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

            long usedMB = heapUsage.getUsed() / (1024 * 1024);
            long maxMB = heapUsage.getMax() / (1024 * 1024);
            double percentUsed = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;

            memoryUsage = String.format("%d MB / %d MB (%.1f%%)", usedMB, maxMB, percentUsed);

            if (percentUsed > 90) {
                memoryColor = "#962f2f"; // Red
            } else if (percentUsed > 75) {
                memoryColor = "#c9a227"; // Yellow
            } else {
                memoryColor = "#3d913f"; // Green
            }
        } catch (Exception e) {
            memoryUsage = "Unable to retrieve";
            memoryColor = "#aaaaaa";
        }
    }

    /**
     * Collect server uptime.
     */
    private void collectUptime() {
        try {
            long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            Duration duration = Duration.ofMillis(uptimeMillis);

            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            long seconds = duration.toSeconds() % 60;

            if (days > 0) {
                uptime = String.format("%dd %dh %dm", days, hours, minutes);
            } else if (hours > 0) {
                uptime = String.format("%dh %dm %ds", hours, minutes, seconds);
            } else {
                uptime = String.format("%dm %ds", minutes, seconds);
            }
        } catch (Exception e) {
            uptime = "Unknown";
        }
    }

    /**
     * Collect statistics from all sanitizers.
     */
    private void collectSanitizerStats(HyzenKernel plugin, HyzenKernelConfig config) {
        // CraftingManagerSanitizer
        CraftingManagerSanitizer craftingSanitizer = plugin.getCraftingManagerSanitizer();
        if (craftingSanitizer != null) {
            int fixes = craftingSanitizer.getFixedCount();
            totalCrashesPrevented += fixes;
            sanitizerInfos.add(new SanitizerInfo("Crafting Manager", true, fixes));
            totalSanitizers++;
            activeSanitizers++;
        }

        // InteractionManagerSanitizer
        InteractionManagerSanitizer interactionSanitizer = plugin.getInteractionManagerSanitizer();
        if (interactionSanitizer != null) {
            int fixes = interactionSanitizer.getCrashesPrevented() + interactionSanitizer.getTimeoutsPrevented();
            totalCrashesPrevented += fixes;
            sanitizerInfos.add(new SanitizerInfo("Interaction Manager", true, fixes));
            totalSanitizers++;
            activeSanitizers++;
        }

        // GatherObjectiveTaskSanitizer
        GatherObjectiveTaskSanitizer gatherSanitizer = plugin.getGatherObjectiveTaskSanitizer();
        if (gatherSanitizer != null) {
            int fixes = gatherSanitizer.getFixedCount();
            totalCrashesPrevented += fixes;
            sanitizerInfos.add(new SanitizerInfo("Gather Objective Task", true, fixes));
            totalSanitizers++;
            activeSanitizers++;
        }

        // SpawnBeaconSanitizer
        SpawnBeaconSanitizer spawnBeaconSanitizer = plugin.getSpawnBeaconSanitizer();
        if (spawnBeaconSanitizer != null) {
            int fixes = spawnBeaconSanitizer.getFixedCount();
            totalCrashesPrevented += fixes;
            sanitizerInfos.add(new SanitizerInfo("Spawn Beacon", true, fixes));
            totalSanitizers++;
            activeSanitizers++;
        }

        // ChunkTrackerSanitizer
        ChunkTrackerSanitizer chunkTrackerSanitizer = plugin.getChunkTrackerSanitizer();
        if (chunkTrackerSanitizer != null) {
            int fixes = chunkTrackerSanitizer.getCrashesPrevented();
            totalCrashesPrevented += fixes;
            sanitizerInfos.add(new SanitizerInfo("Chunk Tracker", true, fixes));
            totalSanitizers++;
            activeSanitizers++;
        }

        // InteractionChainMonitor
        InteractionChainMonitor chainMonitor = plugin.getInteractionChainMonitor();
        if (chainMonitor != null) {
            // This monitor doesn't have a fix count, but it's still active
            sanitizerInfos.add(new SanitizerInfo("Interaction Chain Monitor", true, 0));
            totalSanitizers++;
            activeSanitizers++;
        }

        // Chunk Protection System (can be disabled via config)
        ChunkProtectionRegistry protectionRegistry = plugin.getChunkProtectionRegistry();
        if (protectionRegistry != null && config.isChunkProtectionEnabled()) {
            sanitizerInfos.add(new SanitizerInfo("Chunk Protection", true, 0));
            totalSanitizers++;
            activeSanitizers++;
        } else {
            sanitizerInfos.add(new SanitizerInfo("Chunk Protection", false, 0));
            totalSanitizers++;
        }

        // Chunk Unload Manager (can be disabled via config)
        ChunkUnloadManager chunkUnloadManager = plugin.getChunkUnloadManager();
        if (chunkUnloadManager != null && config.isChunkUnloadEnabled()) {
            sanitizerInfos.add(new SanitizerInfo("Chunk Unload Manager", true, 0));
            totalSanitizers++;
            activeSanitizers++;
        } else {
            sanitizerInfos.add(new SanitizerInfo("Chunk Unload Manager", false, 0));
            totalSanitizers++;
        }

        // Add additional sanitizers that may not have getters
        // These are registered via transformers/early loading
        sanitizerInfos.add(new SanitizerInfo("Respawn Block Sanitizer", true, 0));
        sanitizerInfos.add(new SanitizerInfo("Processing Bench Sanitizer", true, 0));
        sanitizerInfos.add(new SanitizerInfo("Empty Archetype Sanitizer", true, 0));
        totalSanitizers += 3;
        activeSanitizers += 3;
    }

    /**
     * Collect chunk management statistics.
     */
    private void collectChunkStats(HyzenKernel plugin) {
        // Protected chunks count
        ChunkProtectionRegistry protectionRegistry = plugin.getChunkProtectionRegistry();
        if (protectionRegistry != null) {
            protectedChunks = protectionRegistry.getProtectedChunkCount();
        }

        // Chunk unload statistics
        ChunkUnloadManager unloadManager = plugin.getChunkUnloadManager();
        if (unloadManager != null) {
            // Parse status to get attempts and successes
            String status = unloadManager.getStatus();
            chunkUnloadAttempts = parseStatFromStatus(status, "Attempts:");
            chunkUnloadSuccesses = parseStatFromStatus(status, "Successful:");

            // Get last run info
            if (status.contains("Last run:")) {
                int startIdx = status.indexOf("Last run:") + 10;
                int endIdx = status.indexOf("\n", startIdx);
                if (endIdx > startIdx) {
                    lastCleanupTime = status.substring(startIdx, endIdx).trim();
                }
            }
        }

        // Check map-aware mode
        if (unloadManager != null) {
            mapAwareModeEnabled = unloadManager.isMapAwareModeEnabled();
        }
    }

    /**
     * Parse a numeric stat from a status string.
     */
    private int parseStatFromStatus(String status, String key) {
        try {
            int keyIdx = status.indexOf(key);
            if (keyIdx >= 0) {
                int startIdx = keyIdx + key.length();
                int endIdx = startIdx;
                while (endIdx < status.length() && (Character.isDigit(status.charAt(endIdx)) || status.charAt(endIdx) == ' ')) {
                    endIdx++;
                }
                String numStr = status.substring(startIdx, endIdx).trim();
                return Integer.parseInt(numStr);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * Calculate overall system health based on collected stats.
     */
    private void calculateHealth() {
        // Health is good if:
        // - Memory usage is < 75%
        // - Most sanitizers are active
        // - No critical errors

        double sanitizerRatio = totalSanitizers > 0 ? (double) activeSanitizers / totalSanitizers : 1.0;

        if (sanitizerRatio >= 0.9 && !memoryColor.equals("#962f2f")) {
            healthStatus = "HEALTHY";
            healthColor = "#3d913f"; // Green
        } else if (sanitizerRatio >= 0.7 || memoryColor.equals("#c9a227")) {
            healthStatus = "WARNING";
            healthColor = "#c9a227"; // Yellow
        } else {
            healthStatus = "CRITICAL";
            healthColor = "#962f2f"; // Red
        }
    }

    // Getters

    public int getTotalCrashesPrevented() {
        return totalCrashesPrevented;
    }

    public int getActiveSanitizers() {
        return activeSanitizers;
    }

    public int getTotalSanitizers() {
        return totalSanitizers;
    }

    public int getProtectedChunks() {
        return protectedChunks;
    }

    public int getChunkUnloadAttempts() {
        return chunkUnloadAttempts;
    }

    public int getChunkUnloadSuccesses() {
        return chunkUnloadSuccesses;
    }

    public String getChunkUnloadRate() {
        if (chunkUnloadAttempts == 0) {
            return "N/A";
        }
        double rate = (double) chunkUnloadSuccesses / chunkUnloadAttempts * 100;
        return String.format("%.1f%%", rate);
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public String getHealthColor() {
        return healthColor;
    }

    public String getMemoryUsage() {
        return memoryUsage;
    }

    public String getMemoryColor() {
        return memoryColor;
    }

    public String getUptime() {
        return uptime;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public String getLastCleanupTime() {
        return lastCleanupTime;
    }

    public int getCleanupInterval() {
        return cleanupInterval;
    }

    public boolean isChunkProtectionEnabled() {
        return chunkProtectionEnabled;
    }

    public boolean isMapAwareModeEnabled() {
        return mapAwareModeEnabled;
    }

    public List<SanitizerInfo> getSanitizerInfos() {
        return sanitizerInfos;
    }

    /**
     * Information about a single sanitizer.
     */
    public static class SanitizerInfo {
        private final String name;
        private final boolean enabled;
        private final int fixCount;

        public SanitizerInfo(String name, boolean enabled, int fixCount) {
            this.name = name;
            this.enabled = enabled;
            this.fixCount = fixCount;
        }

        public String getName() {
            return name;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public int getFixCount() {
            return fixCount;
        }
    }
}
