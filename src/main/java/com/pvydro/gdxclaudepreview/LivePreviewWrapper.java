package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LivePreviewWrapper implements ApplicationListener {

    private final ApplicationListener delegate;
    private final int port;
    private final FramebufferCapture capture;
    private final InputBridge inputBridge;
    private final LogBuffer logBuffer;
    private LivePreviewHttpServer server;
    private LivePreviewInput previewInput;
    /** Endpoints registered before the server starts — flushed on create(). */
    private List<Map.Entry<String, LivePreviewEndpoint>> pendingEndpoints;

    public LivePreviewWrapper(ApplicationListener delegate, int port) {
        this.delegate = delegate;
        this.port = port;
        this.capture = new FramebufferCapture();
        this.inputBridge = new InputBridge();
        this.logBuffer = new LogBuffer(256);
    }

    /**
     * Register a custom GET endpoint on the preview server.
     * Can be called before or after the server has started.
     */
    public void registerEndpoint(String path, LivePreviewEndpoint endpoint) {
        if (server != null) {
            server.registerEndpoint(path, endpoint);
        } else {
            if (pendingEndpoints == null) pendingEndpoints = new ArrayList<>();
            pendingEndpoints.add(new AbstractMap.SimpleEntry<>(path, endpoint));
        }
    }

    /**
     * Remove a previously registered custom endpoint.
     */
    public void removeEndpoint(String path) {
        if (server != null) {
            server.removeEndpoint(path);
        }
    }

    @Override
    public void create() {
        delegate.create();

        // Create input wrapper so polling-based game code sees preview input
        previewInput = new LivePreviewInput(Gdx.input, inputBridge);

        // Install logger to capture Gdx.app.log/error/debug into the log buffer
        Gdx.app.setApplicationLogger(new LivePreviewLogger(Gdx.app.getApplicationLogger(), logBuffer));

        server = new LivePreviewHttpServer(port, capture, inputBridge, logBuffer);
        // Flush any endpoints registered before the server was created
        if (pendingEndpoints != null) {
            for (Map.Entry<String, LivePreviewEndpoint> entry : pendingEndpoints) {
                server.registerEndpoint(entry.getKey(), entry.getValue());
            }
            pendingEndpoints = null;
        }
        try {
            server.start();
            server.setGameReady(true);
            System.out.println("[gdx-claude-preview] Live preview started on port " + port);
        } catch (IOException e) {
            System.err.println("[gdx-claude-preview] Failed to start HTTP server on port " + port + ": " + e.getMessage());
            System.err.println("[gdx-claude-preview] Game will continue without live preview.");
            server = null;
        }
    }

    @Override
    public void resize(int width, int height) {
        delegate.resize(width, height);
    }

    @Override
    public void render() {
        // Re-install input wrapper every frame because LWJGL3's makeCurrent()
        // overwrites Gdx.input with the real Lwjgl3Input before each render call
        if (previewInput != null) {
            if (!(Gdx.input instanceof LivePreviewInput)) {
                previewInput.setDelegate(Gdx.input);
            }
            Gdx.input = previewInput;
        }

        inputBridge.drainEvents();
        delegate.render();
        capture.onFrameRendered();
    }

    @Override
    public void pause() {
        delegate.pause();
    }

    @Override
    public void resume() {
        delegate.resume();
    }

    @Override
    public void dispose() {
        if (server != null) {
            server.setGameReady(false);
            server.stop();
            System.out.println("[gdx-claude-preview] Live preview stopped.");
        }
        if (previewInput != null) {
            Gdx.input = previewInput.getDelegate();
        }
        capture.dispose();
        delegate.dispose();
    }
}
