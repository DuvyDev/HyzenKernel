# HyFixes

Essential bug fixes for Hytale Early Access servers. Prevents crashes, player kicks, and desync issues caused by known bugs in Hytale's core systems.

[![Discord](https://img.shields.io/badge/Discord-Join%20for%20Support-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/6g7McTd27z)
[![GitHub Issues](https://img.shields.io/badge/GitHub-Report%20Bugs-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/John-Willikers/hyfixes/issues)

---

## Two Plugins, One Solution

HyFixes consists of **two complementary plugins** that work together to fix different types of bugs:

| Plugin | File | Purpose |
|--------|------|---------|
| **Runtime Plugin** | `hyfixes.jar` | Fixes bugs at runtime using sanitizers and event hooks |
| **Early Plugin** | `hyfixes-early.jar` | Fixes deep core bugs via bytecode transformation at class load |

### Why Two Plugins?

Some Hytale bugs occur in code paths that cannot be intercepted at runtime. The **Early Plugin** uses Java bytecode transformation (ASM) to rewrite buggy methods *before* they're loaded, allowing us to fix issues deep in Hytale's networking and interaction systems.

---

## Quick Start

### Runtime Plugin (Required)

1. Download `hyfixes.jar` from [Releases](https://github.com/John-Willikers/hyfixes/releases)
2. Place in your server's `mods/` directory
3. Restart the server

### Early Plugin (Recommended)

1. Download `hyfixes-early.jar` from [Releases](https://github.com/John-Willikers/hyfixes/releases)
2. Place in your server's `earlyplugins/` directory
3. Start the server with early plugins enabled:
   - Set `ACCEPT_EARLY_PLUGINS=1` environment variable, OR
   - Press Enter when prompted at startup

### Alternative Downloads

**ModTale:**
- [HyFixes Runtime](https://modtale.net/project/YOUR_RUNTIME_UUID) *(update link after project creation)*
- [HyFixes Early](https://modtale.net/project/YOUR_EARLY_UUID) *(update link after project creation)*

---

## What Gets Fixed

### Runtime Plugin Fixes

| Bug | Severity | What Happens |
|-----|----------|--------------|
| Pickup Item Crash | Critical | World thread crashes, ALL players kicked |
| RespawnBlock Crash | Critical | Player kicked when breaking bed |
| ProcessingBench Crash | Critical | Player kicked when bench is destroyed |
| Instance Exit Crash | Critical | Player kicked when exiting dungeon |
| Chunk Memory Bloat | High | Server runs out of memory over time |
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
| Duplicate Block Components | Critical | Player kicked when using teleporters |
| Null npcReferences (Removal) | Critical | World crashes when spawn markers are removed |
| Null npcReferences (Constructor) | Critical | ROOT CAUSE: SpawnMarkerEntity never initializes array |
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

### BetterMap Compatibility (v1.6.2+)

If you use **BetterMap** or other world map plugins, you may need to disable HyFixes' aggressive chunk unloading. The ChunkUnloadManager can clear chunk data that map plugins need for rendering, causing black/empty maps.

**To disable ChunkUnloadManager:**

**Option 1: Environment Variable**
```bash
export HYFIXES_DISABLE_CHUNK_UNLOAD=true
```

**Option 2: JVM Argument**
```bash
java -Dhyfixes.disableChunkUnload=true -jar server.jar
```

**Option 3: Pterodactyl Panel**
Add to Startup Variables:
```
HYFIXES_DISABLE_CHUNK_UNLOAD=true
```

**Trade-off:** Disabling ChunkUnloadManager means server memory may grow on servers where players explore large areas. Monitor your server's memory usage if you disable this feature.

**Verification:** When disabled, you'll see these log messages at startup:
```
[DISABLED] ChunkUnloadManager - disabled via config (HYFIXES_DISABLE_CHUNK_UNLOAD=true)
[DISABLED] This improves compatibility with BetterMap and other map plugins
```

---

## Admin Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/hyfixes` | `/hfs`, `/interactionstatus` | Show HyFixes statistics and status |
| `/chunkstatus` | | Show chunk counts and memory info |
| `/chunkunload` | | Force immediate chunk cleanup |
| `/fixcounter` | `/fc`, `/blockcounter` | Fix/view teleporter BlockCounter values |

---

## Verification

### Runtime Plugin Loaded

Look for these log messages at startup:
```
[HyFixes|P] Plugin enabled - HyFixes vX.X.X
[HyFixes|P] [PickupItemSanitizer] Active - monitoring for corrupted pickup items
[HyFixes|P] [ChunkCleanupSystem] Active on MAIN THREAD
```

### Early Plugin Loaded

Look for these log messages at startup (15 transformers):
```
[HyFixes-Early] InteractionChain transformation COMPLETE!
[HyFixes-Early] World transformation COMPLETE!
[HyFixes-Early] Universe transformation COMPLETE!
[HyFixes-Early] TickingThread transformation COMPLETE!
[HyFixes-Early] SpawnReferenceSystems transformation COMPLETE!
[HyFixes-Early] BeaconSpawnController transformation COMPLETE!
[HyFixes-Early] BlockComponentChunk transformation COMPLETE!
[HyFixes-Early] MarkerAddRemoveSystem transformation COMPLETE!
[HyFixes-Early] SpawnMarkerEntity transformation COMPLETE!
[HyFixes-Early] SpawnMarkerSystems transformation COMPLETE!
[HyFixes-Early] TrackedPlacement transformation COMPLETE!
[HyFixes-Early] WorldMapTracker transformation COMPLETE!
[HyFixes-Early] ArchetypeChunk transformation COMPLETE!
[HyFixes-Early] PacketHandler transformation COMPLETE!
[HyFixes-Early] Successfully transformed UUIDSystem.onEntityRemove()
[HyFixes-Early] CommandBuffer transformation COMPLETE!
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

**Found a bug?** Please report it on [GitHub Issues](https://github.com/John-Willikers/hyfixes/issues) with:
- Server logs showing the error
- Steps to reproduce (if known)
- HyFixes version

**Need help?** Join our [Discord](https://discord.gg/6g7McTd27z) for community support!

---

## Building from Source

Requires Java 21 and access to `HytaleServer.jar`.

```bash
# Clone the repo
git clone https://github.com/John-Willikers/hyfixes.git
cd hyfixes

# Place HytaleServer.jar in libs/ directory
mkdir -p libs
cp /path/to/HytaleServer.jar libs/

# Build runtime plugin
./gradlew build
# Output: build/libs/hyfixes.jar

# Build early plugin
cd hyfixes-early
./gradlew build
# Output: build/libs/hyfixes-early-1.0.0.jar
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

1. Open an [issue](https://github.com/John-Willikers/hyfixes/issues) describing the bug
2. Fork the repo and create a fix
3. Submit a PR with your changes

Join our [Discord](https://discord.gg/6g7McTd27z) to discuss ideas!
