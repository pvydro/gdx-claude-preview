package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.ScreenUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FramebufferCapture {

    private static final int DEFAULT_CAPTURE_INTERVAL = 6;
    private static final float JPEG_QUALITY = 0.7f;

    private final ExecutorService encoder;
    private final int captureInterval;
    private volatile byte[] cachedScreenshot;
    private int frameCount;
    private volatile boolean encoderBusy = false;

    public FramebufferCapture() {
        this(DEFAULT_CAPTURE_INTERVAL);
    }

    public FramebufferCapture(int captureInterval) {
        this.captureInterval = captureInterval;
        this.encoder = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "gdx-preview-encoder");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Called from GL thread every frame. Proactively captures every Nth frame.
     * Only does glReadPixels + byte[] copy on the GL thread — encoding happens
     * on a background thread.
     */
    public void onFrameRendered() {
        if (++frameCount % captureInterval != 0) return;
        if (encoderBusy) return; // skip if previous encode still running

        try {
            int w = Gdx.graphics.getBackBufferWidth();
            int h = Gdx.graphics.getBackBufferHeight();
            if (w <= 0 || h <= 0) return;

            Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);

            // Copy raw pixels into a byte[] so we can release the Pixmap immediately
            ByteBuffer buffer = pixmap.getPixels();
            byte[] rawPixels = new byte[buffer.remaining()];
            buffer.get(rawPixels);
            pixmap.dispose();

            // Hand off to encoder thread — GL thread is now free
            encoderBusy = true;
            encoder.submit(() -> {
                try {
                    cachedScreenshot = encodeJpeg(rawPixels, w, h);
                } catch (Exception e) {
                    System.err.println("[gdx-claude-preview] Encode error: " + e.getMessage());
                } finally {
                    encoderBusy = false;
                }
            });
        } catch (Exception e) {
            // Don't crash the game for a preview failure
        }
    }

    /**
     * Called from HTTP thread. Returns the latest cached screenshot instantly.
     */
    public byte[] getLatestScreenshot() {
        return cachedScreenshot;
    }

    /**
     * @deprecated Use {@link #getLatestScreenshot()} instead. This method exists
     * only for backward compatibility and returns the cached screenshot immediately.
     */
    @Deprecated
    public byte[] requestScreenshot(long timeoutMs) {
        return cachedScreenshot;
    }

    public byte[] getCachedScreenshot() {
        return cachedScreenshot;
    }

    /**
     * Encodes raw RGBA pixels to JPEG on the encoder thread.
     * Downscales to half resolution and flips vertically (GL origin is bottom-left).
     * Uses row-bulk pixel copy and explicit ImageWriter for speed.
     */
    private byte[] encodeJpeg(byte[] rawPixels, int fullW, int fullH) throws Exception {
        // Half-res dimensions
        int halfW = fullW / 2;
        int halfH = fullH / 2;

        BufferedImage image = new BufferedImage(halfW, halfH, BufferedImage.TYPE_INT_RGB);
        int[] rowBuffer = new int[halfW];

        // rawPixels is RGBA, 4 bytes per pixel, row-major from top-left
        // GL framebuffer is bottom-up, so row 0 in rawPixels = bottom of screen
        // We want: output row 0 = top of screen = rawPixels row (fullH-1)
        // With half-res: output row oy maps to rawPixels row (fullH - 1 - oy*2)
        int srcStride = fullW * 4;

        for (int oy = 0; oy < halfH; oy++) {
            int srcY = fullH - 1 - oy * 2; // flip + skip every other row
            int srcRowOffset = srcY * srcStride;

            for (int ox = 0; ox < halfW; ox++) {
                int srcIdx = srcRowOffset + ox * 2 * 4; // skip every other pixel
                int r = rawPixels[srcIdx] & 0xFF;
                int g = rawPixels[srcIdx + 1] & 0xFF;
                int b = rawPixels[srcIdx + 2] & 0xFF;
                rowBuffer[ox] = (r << 16) | (g << 8) | b;
            }

            image.setRGB(0, oy, halfW, 1, rowBuffer, 0, halfW);
        }

        // Encode JPEG with explicit quality settings for speed
        ByteArrayOutputStream baos = new ByteArrayOutputStream(halfW * halfH / 4);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("JPEG");
        if (!writers.hasNext()) {
            // Fallback to ImageIO.write
            ImageIO.write(image, "JPEG", baos);
            return baos.toByteArray();
        }

        ImageWriter writer = writers.next();
        try {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);

            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
            ios.flush();
            ios.close();
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    public void dispose() {
        encoder.shutdown();
        try {
            encoder.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            encoder.shutdownNow();
        }
    }
}
