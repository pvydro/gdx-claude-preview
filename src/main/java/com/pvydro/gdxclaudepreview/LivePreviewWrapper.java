package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

import java.io.IOException;

public class LivePreviewWrapper implements ApplicationListener {

    private final ApplicationListener delegate;
    private final int port;
    private final FramebufferCapture capture;
    private final InputBridge inputBridge;
    private final LogBuffer logBuffer;
    private LivePreviewHttpServer server;
    private LivePreviewInput previewInput;

    public LivePreviewWrapper(ApplicationListener delegate, int port) {
        this.delegate = delegate;
        this.port = port;
        this.capture = new FramebufferCapture();
        this.inputBridge = new InputBridge();
        this.logBuffer = new LogBuffer(256);
    }

    @Override
    public void create() {
        delegate.create();

        // Create input wrapper so polling-based game code sees preview input
        previewInput = new LivePreviewInput(Gdx.input, inputBridge);

        // Install logger to capture Gdx.app.log/error/debug into the log buffer
        Gdx.app.setApplicationLogger(new LivePreviewLogger(Gdx.app.getApplicationLogger(), logBuffer));

        server = new LivePreviewHttpServer(port, capture, inputBridge, logBuffer);
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
