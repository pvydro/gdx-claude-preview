package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.ApplicationListener;

/**
 * Public API for gdx-claude-preview.
 *
 * Usage in DesktopLauncher:
 *   new Lwjgl3Application(LivePreview.wrap(new MyGame(), 8090), config);
 */
public class LivePreview {

    /**
     * Wraps a LibGDX ApplicationListener with live preview capability.
     * Starts an HTTP server on the given port that serves screenshots
     * and accepts input events, allowing Claude to see and interact
     * with the running game.
     *
     * @param delegate the game's ApplicationListener
     * @param port HTTP port to serve the preview on
     * @return a wrapped ApplicationListener
     */
    public static ApplicationListener wrap(ApplicationListener delegate, int port) {
        return new LivePreviewWrapper(delegate, port);
    }
}
