package com.hyzenkernel.listeners;

import com.hyzenkernel.HyzenKernel;
import com.hypixel.hytale.event.EventRegistration;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Unloads persistent shared instance worlds after server boot.
 *
 * Problem: Universe loads every world folder on startup. This includes
 * instance-shared-* worlds created by shared portals. Those loaded worlds
 * count against the fragment limit even when no players are inside.
 *
 * Solution: After AllWorldsLoadedEvent, unload any instance-shared-* worlds
 * with zero players. Worlds remain on disk because deleteOnRemove=false.
 */
public class SharedInstanceBootUnloader {

    private static final String SHARED_PREFIX = "instance-shared-";
    private static final long UNLOAD_DELAY_MS = 1000L;

    private final HyzenKernel plugin;
    private final AtomicBoolean ran = new AtomicBoolean(false);
    private EventRegistration<?, ?> registration;

    public SharedInstanceBootUnloader(HyzenKernel plugin) {
        this.plugin = plugin;
    }

    public void register() {
        registration = plugin.getEventRegistry().registerGlobal(
            AllWorldsLoadedEvent.class,
            event -> onAllWorldsLoaded()
        );

        plugin.getLogger().at(Level.INFO).log(
            "[SharedInstanceBootUnloader] Event handler registered - will unload shared instances after boot"
        );
    }

    private void onAllWorldsLoaded() {
        if (!ran.compareAndSet(false, true)) {
            return;
        }

        if (registration != null) {
            registration.unregister();
        }

        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            this::unloadSharedWorlds,
            UNLOAD_DELAY_MS,
            TimeUnit.MILLISECONDS
        );
    }

    private void unloadSharedWorlds() {
        Universe universe = Universe.get();
        Map<String, World> worlds = universe.getWorlds();

        if (worlds.isEmpty()) {
            plugin.getLogger().at(Level.FINE).log(
                "[SharedInstanceBootUnloader] No worlds to unload"
            );
            return;
        }

        List<String> toUnload = new ArrayList<>();
        for (World world : worlds.values()) {
            if (world == null) {
                continue;
            }
            String worldName = world.getName();
            if (worldName == null) {
                continue;
            }
            if (!worldName.toLowerCase().startsWith(SHARED_PREFIX)) {
                continue;
            }
            if (world.getPlayerCount() > 0) {
                plugin.getLogger().at(Level.FINE).log(
                    "[SharedInstanceBootUnloader] Skipping shared world '%s' (players=%d)",
                    worldName, world.getPlayerCount()
                );
                continue;
            }
            toUnload.add(worldName);
        }

        if (toUnload.isEmpty()) {
            plugin.getLogger().at(Level.INFO).log(
                "[SharedInstanceBootUnloader] No shared instance worlds to unload"
            );
            return;
        }

        int removed = 0;
        for (String worldName : toUnload) {
            try {
                if (universe.removeWorld(worldName)) {
                    removed++;
                }
            } catch (Throwable t) {
                plugin.getLogger().at(Level.SEVERE).withCause(t).log(
                    "[SharedInstanceBootUnloader] Failed to unload shared world '%s'",
                    worldName
                );
            }
        }

        plugin.getLogger().at(Level.INFO).log(
            "[SharedInstanceBootUnloader] Unloaded %d shared instance world(s) after boot",
            removed
        );
    }
}
