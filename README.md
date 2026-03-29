# gdx-claude-preview

LibGDX extension that lets Claude Code see and interact with a running game via live screenshots and input bridging.

## How it works

`gdx-claude-preview` wraps your game's `ApplicationListener` and embeds a lightweight HTTP server (NanoHTTPD) inside the game process. The server serves a browser page that displays the game as a continuously refreshing image stream, with click and keyboard events forwarded back to the game.

Claude Code's preview tools (`preview_screenshot`, `preview_click`, etc.) connect to `http://localhost:<port>` and interact with the game through this browser bridge -- no window manager access or native screen capture required.

```
Game renders frame --> glReadPixels --> encoder thread --> JPEG
Browser polls /screenshot --> displays image --> forwards clicks to /input
Claude preview tools --> see the browser page --> interact with the game
```

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | HTML preview page with auto-refreshing game view |
| `/screenshot` | GET | Latest game frame as JPEG |
| `/input` | POST | Mouse click events (`{x, y, button}`) |
| `/key` | POST | Keyboard events (`{type, code}`) |
| `/info` | GET | Game dimensions, FPS, ready state |

## Setup

### 1. Add the dependency

```gradle
repositories {
    mavenLocal()
    // or for published releases:
    // maven { url "https://jitpack.io" }
}

dependencies {
    implementation "com.github.pvydro:gdx-claude-preview:1.0.0"
}
```

### 2. Wrap your game (one line)

```java
// DesktopLauncher.java
new Lwjgl3Application(LivePreview.wrap(new MyGame(), 8090), config);
```

### 3. Open the preview

Run your game and open `http://localhost:8090` in a browser, or point Claude Code's preview tools at it.

## Performance

The capture pipeline is designed to have near-zero impact on game performance:

**Offloaded encoding** -- The GL thread only does `glReadPixels` and a `ByteBuffer` copy into a `byte[]`. JPEG encoding (the expensive part) runs on a dedicated background thread. The game never blocks on encoding.

**Proactive capture** -- Instead of capturing on-demand when the browser requests a screenshot, the library captures every 6th frame automatically and caches the result. `/screenshot` returns the cached JPEG instantly with no GL-thread involvement.

**Half-resolution output** -- The encoder downscales to 50% in both dimensions (e.g. 1920x1061 becomes 960x530), reducing encoding work by 4x. For a browser preview, half-res is indistinguishable.

**Row-bulk pixel copy** -- Pixel data is copied using bulk `setRGB` per row instead of per-pixel, and the vertical flip (GL's bottom-left origin) is handled during the downscale pass with no extra iteration.

**Skip-on-busy** -- If the encoder thread hasn't finished the previous frame, the next capture is skipped entirely. The GL thread is never blocked waiting for the encoder.

**Zero overhead when idle** -- When no preview server is running or no browser is connected, the only cost is the frame counter increment.

## Architecture

```
src/main/java/com/pvydro/gdxclaudepreview/
  LivePreview.java              -- Public API: static wrap() method
  LivePreviewWrapper.java       -- ApplicationListener decorator
  LivePreviewHttpServer.java    -- HTTP endpoints + embedded HTML page
  FramebufferCapture.java       -- GL capture + background JPEG encoding
  InputBridge.java              -- Thread-safe input event queue
  KeyMapping.java               -- JS KeyboardEvent.code --> LibGDX Input.Keys
  internal/
    NanoHTTPD.java              -- Vendored HTTP server (BSD license)
```

### Thread model

- **GL thread**: runs the game loop, does `glReadPixels` every 6th frame, submits raw pixels to the encoder
- **Encoder thread**: single daemon thread that flips, downscales, and JPEG-encodes frames
- **HTTP threads** (NanoHTTPD pool): serve cached screenshots, enqueue input events

Cross-thread communication uses `volatile` references, `ConcurrentLinkedQueue`, and a single `ExecutorService`. No locks.

## License

MIT
