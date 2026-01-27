# HyzenKernel (Hytale Early Access)

HyzenKernel is a set of essential fixes for Hytale Early Access servers. It was originally created to stabilize our server **Hyzen.net**, and we decided to share it with the community. It is an improved fork of **HyFixes**.

## Two plugins, one goal
- **Runtime plugin** (`hyzenkernel.jar`): runtime fixes using sanitizers and hooks.
- **Early plugin** (`hyzenkernel-early.jar`): deep fixes via ASM bytecode transforms at class load.

## Key improvements (summary)
- Prevents world thread crashes from spawns, world map issues, and invalid refs.
- Prevents kicks from instance race conditions and interaction timeouts.
- Fixes null/invalid component errors that break interactions.
- Reduces shutdown errors and task submissions to dead worlds.

## Quick install
1) Runtime (required): place `hyzenkernel-X.X.X.jar` in `mods/`
2) Early (recommended): place `hyzenkernel-early-X.X.X.jar` in `earlyplugins/` and enable early plugins

## Configuration
Generated at `mods/hyzenkernel/config.json` with toggles for fixes and logging.

## Notes
- Recommended for servers with instances, portals, and custom content.
- Compatible with vanilla Hytale Early Access servers.

## Credits / Info
Repository: `https://github.com/HyzenNet/Kernel`
