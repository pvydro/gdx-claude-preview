package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ScreenUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FramebufferCapture {

    private final AtomicReference<CompletableFuture<byte[]>> pendingRequest = new AtomicReference<>();
    private volatile byte[] cachedScreenshot;

    /**
     * Called from HTTP thread. Requests a screenshot and blocks until the GL thread delivers it.
     */
    public byte[] requestScreenshot(long timeoutMs) {
        byte[] cached = cachedScreenshot;

        CompletableFuture<byte[]> future = new CompletableFuture<>();
        if (!pendingRequest.compareAndSet(null, future)) {
            // Another request already pending — wait on cached or that one
            if (cached != null) return cached;
            CompletableFuture<byte[]> existing = pendingRequest.get();
            if (existing != null) {
                try {
                    return existing.get(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    return cached;
                }
            }
            return cached;
        }

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingRequest.compareAndSet(future, null);
            return cached;
        }
    }

    /**
     * Called from GL thread every frame. Only captures when a request is pending.
     */
    public void captureIfRequested() {
        CompletableFuture<byte[]> future = pendingRequest.get();
        if (future == null) return;

        try {
            int w = Gdx.graphics.getBackBufferWidth();
            int h = Gdx.graphics.getBackBufferHeight();

            Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);

            byte[] jpeg = pixmapToJpeg(pixmap, w, h);
            pixmap.dispose();

            cachedScreenshot = jpeg;
            future.complete(jpeg);
        } catch (Exception e) {
            future.completeExceptionally(e);
        } finally {
            pendingRequest.set(null);
        }
    }

    private byte[] pixmapToJpeg(Pixmap pixmap, int w, int h) throws IOException {
        // GL framebuffer is bottom-up, need to flip vertically
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = pixmap.getPixel(x, y);
                // Pixmap pixel is RGBA8888
                int r = (pixel >>> 24) & 0xFF;
                int g = (pixel >>> 16) & 0xFF;
                int b = (pixel >>> 8) & 0xFF;
                // Flip: row y in pixmap → row (h-1-y) in image
                image.setRGB(x, h - 1 - y, (r << 16) | (g << 8) | b);
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "JPEG", baos);
        return baos.toByteArray();
    }

    public byte[] getCachedScreenshot() {
        return cachedScreenshot;
    }
}
