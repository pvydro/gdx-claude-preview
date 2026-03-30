package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.ApplicationListener;

/**
 * Public API for gdx-claude-preview.
 *
 * Usage in DesktopLauncher:
 *   new Lwjgl3Application(LivePreview.wrap(new MyGame(), 8090), config);
 *
 * Custom endpoints:
 *   LivePreview.registerEndpoint("/game-state", (uri, params) -> gameStateJson);
 */
public class LivePreview {

    private static LivePreviewWrapper instance;

    /**
     * Wraps a LibGDX ApplicationListener with live preview capability.
     * Starts an HTTP server on the given port that serves screenshots
     * and accepts input events, allowing Claude to see and interact
     * with the running game.
     *
     * @param delegate the game's ApplicationListener
     * @param port HTTP port to serve the preview on
     * @return the wrapper (also an ApplicationListener)
     */
    public static LivePreviewWrapper wrap(ApplicationListener delegate, int port) {
        instance = new LivePreviewWrapper(delegate, port);
        return instance;
    }

    /**
     * Register a custom GET endpoint on the preview server.
     * Can be called before or after the server has started — early
     * registrations are buffered and flushed when create() runs.
     *
     * @param path     the URI path (e.g. "/game-state")
     * @param endpoint handler that returns a JSON response body
     */
    public static void registerEndpoint(String path, LivePreviewEndpoint endpoint) {
        if (instance != null) {
            instance.registerEndpoint(path, endpoint);
        }
    }

    /**
     * Remove a previously registered custom endpoint.
     */
    public static void removeEndpoint(String path) {
        if (instance != null) {
            instance.removeEndpoint(path);
        }
    }
}
