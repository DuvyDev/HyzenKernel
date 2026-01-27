# HyzenKernel

Essential bug fixes for Hytale Early Access servers. Prevents crashes, player kicks, and desync issues caused by known bugs in Hytale's core systems.

HyzenKernel is a fork of HyFixes for Hyzen.net (A Hytale server), improved, cleaner, more organized, and optimized. Credits: https://github.com/John-Willikers/hyfixes

---

## Two Plugins, One Solution

HyzenKernel consists of **two complementary plugins** that work together to fix different types of bugs:

| Plugin | File | Purpose |
|--------|------|---------|
| **Runtime Plugin** | `hyzenkernel-X.X.X.jar` | Fixes bugs at runtime using sanitizers and event hooks |
| **Early Plugin** | `hyzenkernel-early-X.X.X.jar` | Fixes deep core bugs via bytecode transformation at class load |

### Why Two Plugins?

Some Hytale bugs occur in code paths that cannot be intercepted at runtime. The **Early Plugin** uses Java bytecode transformation (ASM) to rewrite buggy methods *before* they're loaded, allowing us to fix issues deep in Hytale's networking and interaction systems.

---

## Quick Start

### Runtime Plugin (Required)

1. Download `hyzenkernel-X.X.X.jar` from [Releases](https://github.com/HyzenNet/Kernel/releases)
2. Place in your server's `mods/` directory
3. Restart the server

### Early Plugin (Recommended)

1. Download `hyzenkernel-early-X.X.X.jar` from [Releases](https://github.com/HyzenNet/Kernel/releases)
2. Place in your server's `earlyplugins/` directory
3. Start the server with early plugins enabled:
   - Set `ACCEPT_EARLY_PLUGINS=1` environment variable, OR
   - Press Enter when prompted at startup

---

## What Gets Fixed

### Runtime Plugin Fixes

| Bug | Severity | What Happens |
|-----|----------|--------------|
| Pickup Item Crash | Critical | World thread crashes, ALL players kicked |
| RespawnBlock Crash | Critical | Player kicked when breaking bed |
| ProcessingBench Crash | Critical | Player kicked when bench is destroyed |
| Instance Exit Crash | Critical | Player kicked when exiting dungeon |
| CraftingManager Crash | Critical | Player kicked when opening bench |
| InteractionManager Crash | Critical | Player kicked during interactions |
| Quest Objective Crash | Critical | Quest system crashes |
| SpawnMarker Crash | Critical | World thread crashes during spawning |

### Early Plugin Fixes (Bytecode)

| Bug | Severity | What Happens |
|-----|----------|--------------|
| Sync Buffer Overflow | Critical | Combat/food/tool desync, 400-2500 errors/session |
| Sync Position Gap | Critical | Player kicked with "out of order" exception |
| Instance Portal Race | Critical | Player kicked when entering instance portals (retry loop fix) |
| Null SpawnController | Critical | World crashes when spawn beacons load |
| Null Spawn Parameters | Critical | World crashes in volcanic/cave biomes |
| WorldSpawningSystem Invalid Ref | Critical | World thread crash during spawn job creation (invalid chunk ref) |
| Duplicate Block Components | Critical | Player kicked when using teleporters |
| Null npcReferences (Removal) | Critical | World crashes when spawn markers are removed |
| Null npcReferences (Constructor) | Critical | ROOT CAUSE: SpawnMarkerEntity never initializes array |
| SetMemoriesCapacity Interaction | Critical | Interaction tick crashes when PlayerMemories component is unavailable |
| World.execute Shutdown Guard | Medium | Task submissions during world shutdown throw exceptions/spam logs |
| Prefab Missing Asset Guard | Medium | Prevents exceptions/spam when a prefab file is missing |
| BlockCounter Not Decrementing | Medium | Teleporter limit stuck at 5, can't place new ones |
| WorldMapTracker Iterator Crash | Critical | Server crashes every ~30 min on high-pop servers |
| ArchetypeChunk Stale Entity | Critical | IndexOutOfBoundsException when NPC systems access removed entities |
| Operation Timeout | Critical | Player kicked from network packet timeouts |
| Null UUID on Entity Remove | Critical | Crash when removing entities with null UUIDs |
| Universe Player Remove | Critical | Crash when removing players from universe |
| TickingThread Stop | Medium | Server shutdown issues causing hangs |
| CommandBuffer Component Access | Critical | Crash when accessing components through command buffers |

---

## How It Works

### Runtime Plugin

The runtime plugin registers **sanitizers** that run each server tick:

```
Server Tick
    |
    v
[PickupItemSanitizer] --> Check for null targetRef --> Mark as finished
[CraftingManagerSanitizer] --> Check for stale bench refs --> Clear them
[InteractionManagerSanitizer] --> Check for null contexts --> Remove chain
    |
    v
Hytale's Systems Run (safely, with corrupted data already cleaned up)
```

It also uses **RefSystems** that hook into entity lifecycle events to catch crashes during removal/unload operations.

### Early Plugin

The early plugin uses ASM bytecode transformation to rewrite methods at class load time:

```
Server Startup
    |
    v
JVM loads InteractionChain.class
    |
    v
[InteractionChainTransformer] intercepts class bytes
    |
    v
[PutSyncDataMethodVisitor] rewrites putInteractionSyncData()
[UpdateSyncPositionMethodVisitor] rewrites updateSyncPosition()
    |
    v
Fixed class is loaded into JVM
```

**Original buggy code:**
```java
if (adjustedIndex < 0) {
    LOGGER.severe("Attempted to store sync data...");
    return;  // DATA DROPPED!
}
```

**Transformed fixed code:**
```java
if (adjustedIndex < 0) {
    // Expand buffer backwards instead of dropping
    int expansion = -adjustedIndex;
    for (int i = 0; i < expansion; i++) {
        tempSyncData.add(0, null);
    }
    tempSyncDataOffset += adjustedIndex;
    adjustedIndex = 0;
}
// Continue processing...
```

---

## Configuration

## Admin Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/interactionstatus` | `/hyfixstatus`, `/hfs` | Show HyzenKernel statistics and status |
| `/cleaninteractions` | `/ci`, `/cleanint`, `/fixinteractions` | Scan/remove orphaned interaction zones |
| `/cleanwarps` | `/cw`, `/fixwarps`, `/warpclean` | Scan/remove orphaned warp entries |
| `/fixcounter` | `/fc`, `/blockcounter`, `/teleporterlimit` | Fix/view teleporter BlockCounter values |
| `/who` | | List online players |

---

## Verification

### Runtime Plugin Loaded

Look for these log messages at startup:
```
[HyzenKernel] Plugin enabled - HyzenKernel vX.X.X
[HyzenKernel] [PickupItemSanitizer] Active - monitoring for corrupted pickup items
```

### Early Plugin Loaded

Look for these log messages at startup (18 transformers):
```
[HyzenKernel-Early] InteractionChain transformation COMPLETE!
[HyzenKernel-Early] ArchetypeChunk transformation COMPLETE!
[HyzenKernel-Early] BlockComponentChunk transformation COMPLETE!
[HyzenKernel-Early] CommandBuffer transformation COMPLETE!
[HyzenKernel-Early] InteractionManager transformation COMPLETE!
[HyzenKernel-Early] PacketHandler transformation COMPLETE!
[HyzenKernel-Early] PrefabLoader transformation COMPLETE!
[HyzenKernel-Early] SetMemoriesCapacityInteraction transformation COMPLETE!
[HyzenKernel-Early] SpawnMarkerSystems transformation COMPLETE!
[HyzenKernel-Early] SpawnReferenceSystems transformation COMPLETE!
[HyzenKernel-Early] TickingThread transformation COMPLETE!
[HyzenKernel-Early] Universe transformation COMPLETE!
[HyzenKernel-Early] World transformation COMPLETE!
[HyzenKernel-Early] WorldMapTracker transformation COMPLETE!
[HyzenKernel-Early] WorldSpawningSystem transformation COMPLETE!
[HyzenKernel-Early] LivingEntity transformation COMPLETE!
[HyzenKernel-Early] Successfully transformed UUIDSystem.onEntityRemove()
```

---

## Documentation

| Document | Description |
|----------|-------------|
| [BUGS_FIXED.md](BUGS_FIXED.md) | Detailed technical info on every bug we fix |
| [HYTALE_CORE_BUGS.md](HYTALE_CORE_BUGS.md) | Bugs that require Hytale developers to fix |
| [CHANGELOG.md](CHANGELOG.md) | Version history and release notes |

---

## Support

**Found a bug?** Please report it on [GitHub Issues](https://github.com/HyzenNet/Kernel/issues) with:
- Server logs showing the error
- Steps to reproduce (if known)
- HyzenKernel version

**Need help?** Join our [Discord](https://discord.gg/6g7McTd27z) for community support!

---

## Building from Source

Requires Java 25 and access to `HytaleServer.jar`.

```bash
# Clone the repo
git clone https://github.com/HyzenNet/Kernel.git
cd Kernel

# Place HytaleServer.jar in libs/ directory
mkdir -p libs
cp /path/to/HytaleServer.jar libs/

# Build runtime plugin
./gradlew build
# Output: build/libs/hyzenkernel.jar

# Build early plugin
cd hyzenkernel-early
./gradlew build
# Output: build/libs/hyzenkernel-early-1.0.0.jar
```

---

## CI/CD

This repository uses GitHub Actions to automatically:
- Build on every push to `main`
- Create releases when you push a version tag (`v1.0.0`, `v1.0.1`, etc.)

---

## License

This project is provided as-is for the Hytale community. Use at your own risk.

---

## Contributing

Found another Hytale bug that needs patching? We'd love your help!

- Open an issue or pull request.
