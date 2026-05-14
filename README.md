# Watcher

Watcher is a Fabric client mod concept for Minecraft 1.21.11 by Blueful. It is structured as an in-game, OBS-inspired recording suite with recorder lifecycle management, framebuffer capture, session browsing, playback state, settings, keybinds, and asynchronous export orchestration.

## Current implementation

- Fabric client entrypoint: `com.astreonix.watcher.WatcherClient`
- Recorder state machine: `STOPPED`, `RECORDING`, `PAUSED`
- Session model with queued video frames, audio chunks, metadata, and immutable settings snapshots
- Framebuffer capture from `GameRenderer` render tail
- Audio capture API for game and voice streams without system-wide audio capture
- Modular voice DSP chain with pitch, distortion, and robotic effects
- Pause-menu Watcher button, dashboard, recordings screen, settings screen, and timer HUD overlay
- Keybinds: `F9` toggles recording, `F10` toggles pause/resume
- Asynchronous export/session persistence that writes captured PNG frames and metadata into `.watcher` session folders

## Integration work still required

The architecture is ready for the hard platform-specific pieces, but the following need implementation against resolved Minecraft/Yarn/Fabric APIs:

- Real framebuffer readback into `VideoFrame.rgbaPixels`
- Minecraft sound engine mixed-output interception
- Simple Voice Chat or equivalent mod integration for decoded voice packets
- Audio/video mux staging files for FFmpeg
- Full in-game playback rendering of captured frames and synchronized audio
- Timeline trimming and frame stepping UI

The default output directory is `.minecraft/recordings/watcher/`, configurable through `watcher.json`.
