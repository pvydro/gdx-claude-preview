package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.ApplicationListener;

import java.io.IOException;

public class LivePreviewWrapper implements ApplicationListener {

    private final ApplicationListener delegate;
    private final int port;
    private final FramebufferCapture capture;
    private final InputBridge inputBridge;
    private LivePreviewHttpServer server;

    public LivePreviewWrapper(ApplicationListener delegate, int port) {
        this.delegate = delegate;
        this.port = port;
        this.capture = new FramebufferCapture();
        this.inputBridge = new InputBridge();
    }

    @Override
    public void create() {
        delegate.create();

        server = new LivePreviewHttpServer(port, capture, inputBridge);
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
        inputBridge.drainEvents();
        delegate.render();
        capture.captureIfRequested();
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
        delegate.dispose();
    }
}
