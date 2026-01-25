package com.hyzenkernel.commands;

import com.hyzenkernel.HyzenKernel;
import com.hyzenkernel.ui.HyzenKernelDashboardPage;
import com.hyzenkernel.util.ChatColorUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;

/**
 * Command: /hyzenkernel
 *
 * Opens the HyzenKernel Admin Dashboard UI, providing a visual interface
 * for monitoring statistics, viewing sanitizer status, and managing
 * configuration settings.
 *
 * This replaces the old chat-spamming statistics commands with a
 * clean CustomUIPage dashboard.
 *
 * Usage:
 *   /hyzenkernel - Opens the dashboard UI
 *
 * Permissions: Requires admin privileges
 */
public class DashboardCommand extends AbstractPlayerCommand {

    private final HyzenKernel plugin;

    public DashboardCommand(HyzenKernel plugin) {
        super("hyzenkernel", "hyzenkernel.command.dashboard.desc");
        this.plugin = plugin;
        addAliases("hf", "hyzenkerneldash", "dashboard");
    }

    @Override
    protected boolean canGeneratePermission() {
        // Only admins should access the dashboard
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
        if (player == null) {
            return;
        }

        try {
            // Create and open the dashboard page
            HyzenKernelDashboardPage dashboardPage = new HyzenKernelDashboardPage(playerRef);

            // Open the custom UI page for the player
            player.getPageManager().openCustomPage(ref, store, dashboardPage);

            plugin.getLogger().at(Level.FINE).log(
                    "[Dashboard] Opened HyzenKernel dashboard for player"
            );

        } catch (Exception e) {
            // If UI fails to open, fall back to chat message
            plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[Dashboard] Failed to open dashboard UI, showing fallback message");

            sendFallbackMessage(player);
        }
    }

    /**
     * Send a fallback message if the UI fails to open.
     */
    private void sendFallbackMessage(Player player) {
        ChatColorUtil.sendMessage(player, "&6=== HyzenKernel Dashboard ===");
        ChatColorUtil.sendMessage(player, "&cUnable to open dashboard UI.");
        ChatColorUtil.sendMessage(player, "&7The dashboard UI may not be supported in this environment.");
        ChatColorUtil.sendMessage(player, "&7");
        ChatColorUtil.sendMessage(player, "&7Alternative commands:");
        ChatColorUtil.sendMessage(player, "&f  /chunkstatus &7- View chunk information");
        ChatColorUtil.sendMessage(player, "&f  /fixcounter &7- View/modify block counters");
        ChatColorUtil.sendMessage(player, "&f  /interactionstatus &7- View interaction status");
        ChatColorUtil.sendMessage(player, "&f  /chunkprotect &7- Manage chunk protection");
    }
}
