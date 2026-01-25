package com.hyzenkernel.commands;

import com.hyzenkernel.HyzenKernel;
import com.hyzenkernel.listeners.CraftingManagerSanitizer;
import com.hyzenkernel.listeners.GatherObjectiveTaskSanitizer;
import com.hyzenkernel.listeners.InteractionManagerSanitizer;
import com.hyzenkernel.listeners.SpawnBeaconSanitizer;
import com.hyzenkernel.listeners.ChunkTrackerSanitizer;
import com.hyzenkernel.systems.InteractionChainMonitor;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hyzenkernel.util.ChatColorUtil;

/**
 * Command: /interactionstatus (alias: /hyfixstatus, /hfs)
 *
 * Shows comprehensive HyzenKernel statistics including:
 * - Crashes prevented by each sanitizer
 * - Memory management statistics
 * - Information about unfixable Hytale core bugs
 *
 * This helps server admins:
 * 1. Understand how HyzenKernel is protecting their server
 * 2. Gather evidence for bug reports to Hytale
 * 3. Monitor plugin health
 */
public class InteractionStatusCommand extends AbstractPlayerCommand {

    private final HyzenKernel plugin;

    public InteractionStatusCommand(HyzenKernel plugin) {
        super("interactionstatus", "hyzenkernel.command.interactionstatus.desc");
        this.plugin = plugin;
        addAliases("hyfixstatus", "hfs");
    }

    @Override
    protected boolean canGeneratePermission() {
        // Only admins should use this
        return true;
    }

    @Override
    protected void execute(
            CommandContext context,
            Store<EntityStore> store,
            Ref<EntityStore> ref,
            PlayerRef playerRef,
            World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        // Header
        sendMessage(player, "&6========================================");
        sendMessage(player, "&6         HyzenKernel v1.3.10 Status");
        sendMessage(player, "&6========================================");
        sendMessage(player, "");

        // InteractionChainMonitor statistics
        InteractionChainMonitor monitor = plugin.getInteractionChainMonitor();
        if (monitor != null) {
            String status = monitor.getFullStatus();
            for (String line : status.split("\n")) {
                if (line.startsWith("===")) {
                    sendMessage(player, "&6" + line);
                } else if (line.startsWith("---")) {
                    sendMessage(player, "&e" + line);
                } else if (line.contains(": 0")) {
                    sendMessage(player, "&7" + line);
                } else if (line.contains(":") && !line.trim().isEmpty()) {
                    sendMessage(player, "&a" + line);
                } else {
                    sendMessage(player, "&7" + line);
                }
            }
        } else {
            sendMessage(player, "&cInteractionChainMonitor not initialized");
        }

        sendMessage(player, "");

        // GatherObjectiveTaskSanitizer status
        GatherObjectiveTaskSanitizer objectiveSanitizer = plugin.getGatherObjectiveTaskSanitizer();
        if (objectiveSanitizer != null) {
            sendMessage(player, "&6--- Objective Task Sanitizer ---");
            String status = objectiveSanitizer.getStatus();
            for (String line : status.split("\n")) {
                sendMessage(player, "&7" + line);
            }
        }

        sendMessage(player, "");

        // CraftingManagerSanitizer status
        CraftingManagerSanitizer craftingSanitizer = plugin.getCraftingManagerSanitizer();
        if (craftingSanitizer != null) {
            sendMessage(player, "&6--- Crafting Manager Sanitizer ---");
            String status = craftingSanitizer.getStatus();
            for (String line : status.split("\n")) {
                sendMessage(player, "&7" + line);
            }
        }

        sendMessage(player, "");

        // InteractionManagerSanitizer status (Issue #1)
        InteractionManagerSanitizer interactionSanitizer = plugin.getInteractionManagerSanitizer();
        if (interactionSanitizer != null) {
            sendMessage(player, "&6--- Interaction Manager Sanitizer ---");
            String status = interactionSanitizer.getStatus();
            for (String line : status.split("\n")) {
                sendMessage(player, "&7" + line);
            }
        }

        sendMessage(player, "");

        // SpawnBeaconSanitizer status (Issue #4)
        SpawnBeaconSanitizer spawnSanitizer = plugin.getSpawnBeaconSanitizer();
        if (spawnSanitizer != null) {
            sendMessage(player, "&6--- Spawn Beacon Sanitizer ---");
            String status = spawnSanitizer.getStatus();
            for (String line : status.split("\n")) {
                sendMessage(player, "&7" + line);
            }
        }

        sendMessage(player, "");

        // SpawnMarkerReferenceSanitizer status (Issue #5)
        // Fixed upstream - no longer patched by HyzenKernel
        sendMessage(player, "&6--- Spawn Marker Reference Sanitizer ---");
        sendMessage(player, "&aFIXED upstream");
        sendMessage(player, "&7No longer patched by HyzenKernel");

        sendMessage(player, "");

        // ChunkTrackerSanitizer status (Issue #6)
        ChunkTrackerSanitizer chunkTrackerSanitizer = plugin.getChunkTrackerSanitizer();
        if (chunkTrackerSanitizer != null) {
            sendMessage(player, "&6--- Chunk Tracker Sanitizer ---");
            String status = chunkTrackerSanitizer.getStatus();
            for (String line : status.split("\n")) {
                sendMessage(player, "&7" + line);
            }
        }

        sendMessage(player, "");

        // Known unfixable bugs section
        sendMessage(player, "&6--- Known Unfixable Hytale Bugs ---");
        sendMessage(player, "&7These require fixes from Hytale developers:");
        sendMessage(player, "&c  - InteractionChain Sync Buffer Overflow");
        sendMessage(player, "&c  - Missing Replacement Interactions");
        sendMessage(player, "&c  - Client/Server Interaction Desync");
        sendMessage(player, "&7See HYTALE_CORE_BUGS.md for technical details");

        sendMessage(player, "");
        sendMessage(player, "&6========================================");
        sendMessage(player, "&7Docs: github.com/HyzenNet/kernel");
        sendMessage(player, "&6========================================");
    }

    private void sendMessage(Player player, String message) {
        ChatColorUtil.sendMessage(player, message);
    }
}
