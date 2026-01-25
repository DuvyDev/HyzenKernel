# HyzenKernel

Essential bug fixes for Hytale Early Access servers. Prevents crashes and player kicks caused by known issues in Hytale's core systems.

## Support

**Need help?** Join our Discord for community support!

**Discord:** https://discord.gg/r6KzU4n7V8

**Found a bug?** Report it on GitHub:

**GitHub Issues:** https://github.com/HyzenNet/Kernel/issues

---

## Installation

HyzenKernel consists of two plugins that work together:

### Runtime Plugin (Required)

The main plugin that fixes most crashes.

1. Download `hyzenkernel.jar`
2. Place in your server's `mods/` folder
3. Restart the server

### Early Plugin (Recommended)

Fixes deep networking bugs that cause combat/interaction desync.

1. Download `hyzenkernel-early.jar`
2. Place in your server's `earlyplugins/` folder
3. Start the server with one of these options:
   - Set environment variable: `ACCEPT_EARLY_PLUGINS=1`
   - OR press Enter when you see the early plugins warning at startup

---

## What Gets Fixed

### Runtime Plugin

- **Pickup Item Crash** - World thread crash when player disconnects while picking up item
- **RespawnBlock Crash** - Player kicked when breaking bed/sleeping bag
- **ProcessingBench Crash** - Player kicked when bench is destroyed while open
- **Instance Exit Crash** - Player kicked when exiting dungeon with corrupted data
- **Chunk Memory Bloat** - Server runs out of memory from unloaded chunks
- **CraftingManager Crash** - Player kicked when opening crafting bench
- **InteractionManager Crash** - Player kicked during certain interactions
- **Quest Objective Crash** - Quest system crashes when target despawns
- **SpawnMarker Crash** - World crash during entity spawning

### Early Plugin (Bytecode Fixes)

- **Sync Buffer Overflow** - Fixes combat/food/tool desync (400-2500 errors per session)
- **Sync Position Gap** - Fixes "out of order" exception that kicks players
- **Instance Portal Race** - Fixes "player already in world" crash when entering portals
- **Null SpawnController** - Fixes world crashes when spawn beacons load
- **Null Spawn Parameters** - Fixes world crashes in volcanic/cave biomes
- **Duplicate Block Components** - Fixes player kicks when using teleporters
- **Null npcReferences (Removal)** - Fixes crash on spawn marker removal
- **BlockCounter Not Decrementing** - Fixes teleporter limit stuck at 5
- **WorldMapTracker Iterator Crash** - Fixes server crashes every ~30 min on high-pop servers

---

## Verifying Installation

### Runtime Plugin

Look for these messages in your server log at startup:

```
[HyzenKernel|P] Plugin enabled - HyzenKernel vX.X.X
[HyzenKernel|P] [ChunkCleanupSystem] Active on MAIN THREAD
```

### Early Plugin

Look for these messages in your server log at startup (10 transformers):

```
[HyzenKernel-Early] Transforming InteractionChain class...
[HyzenKernel-Early] InteractionChain transformation COMPLETE!

[HyzenKernel-Early] Transforming World class...
[HyzenKernel-Early] World transformation COMPLETE!

[HyzenKernel-Early] Transforming SpawnReferenceSystems$BeaconAddRemoveSystem...
[HyzenKernel-Early] SpawnReferenceSystems transformation COMPLETE!


[HyzenKernel-Early] Transforming BlockComponentChunk...
[HyzenKernel-Early] BlockComponentChunk transformation COMPLETE!

[HyzenKernel-Early] Transforming SpawnReferenceSystems$MarkerAddRemoveSystem...
[HyzenKernel-Early] MarkerAddRemoveSystem transformation COMPLETE!


[HyzenKernel-Early] Transforming TrackedPlacement$OnAddRemove...
[HyzenKernel-Early] TrackedPlacement transformation COMPLETE!

[HyzenKernel-Early] Transforming WorldMapTracker...
[HyzenKernel-Early] WorldMapTracker transformation COMPLETE!
```

---

## Admin Commands

| Command | Description |
|---------|-------------|
| `/interactionstatus` | Show HyzenKernel statistics and crash prevention counts |
| `/cleaninteractions` | Scan/remove orphaned interaction zones |
| `/cleanwarps` | Scan/remove orphaned warp entries |
| `/fixcounter` | Fix/view teleporter BlockCounter values |
| `/who` | List online players |

Aliases: `/hfs`, `/hyfixstatus`, `/ci`, `/cleanint`, `/fixinteractions`, `/cw`, `/fixwarps`, `/warpclean`, `/fc`, `/blockcounter`, `/teleporterlimit`

---

## Troubleshooting

### Plugin not loading

1. Check that the JAR is in the correct folder:
   - Runtime plugin: `mods/hyzenkernel.jar`
   - Early plugin: `earlyplugins/hyzenkernel-early.jar`

2. Check server logs for errors during startup

3. Verify Java 21+ is installed: `java -version`

### Early plugin warning at startup

This is normal! Hytale shows a security warning for early plugins because they can modify game code. HyzenKernel only modifies the specific buggy methods to fix them.

To bypass the warning:
- Set `ACCEPT_EARLY_PLUGINS=1` environment variable
- OR press Enter when prompted

### Still seeing crashes

1. Check which version of HyzenKernel you have: `/hyzenkernel`
2. Update to the latest version
3. Report the crash on GitHub with:
   - Full server log showing the error
   - Steps to reproduce (if known)
   - HyzenKernel version

---

## Compatibility

- **Hytale:** Early Access (2025+)
- **Java:** 21 or higher
- **Side:** Server-side only (no client install needed)

---

## More Information

For detailed technical documentation, source code, and contribution guidelines:

**GitHub Repository:** https://github.com/HyzenNet/Kernel

**Full Bug Documentation:** https://github.com/HyzenNet/Kernel/blob/main/BUGS_FIXED.md

---

## Support the Project

- Star the repo on GitHub
- Report bugs you encounter
- Join our Discord community
- Share HyzenKernel with other server admins!

**Discord:** https://discord.gg/r6KzU4n7V8
