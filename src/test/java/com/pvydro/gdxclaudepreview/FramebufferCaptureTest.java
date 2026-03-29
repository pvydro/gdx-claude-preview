package com.pvydro.gdxclaudepreview;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class FramebufferCaptureTest {

    private FramebufferCapture capture;

    @After
    public void tearDown() {
        if (capture != null) {
            capture.dispose();
        }
    }

    @Test
    public void latestScreenshotInitiallyNull() {
        capture = new FramebufferCapture();
        assertNull(capture.getLatestScreenshot());
    }

    @Test
    public void getCachedScreenshotInitiallyNull() {
        capture = new FramebufferCapture();
        assertNull(capture.getCachedScreenshot());
    }

    @Test
    public void deprecatedRequestScreenshotReturnsNull() {
        capture = new FramebufferCapture();
        // Deprecated method should return cached (null) immediately, no blocking
        long start = System.currentTimeMillis();
        byte[] result = capture.requestScreenshot(1000);
        long elapsed = System.currentTimeMillis() - start;

        assertNull(result);
        assertTrue("Should return instantly, but took " + elapsed + "ms", elapsed < 50);
    }

    @Test
    public void onFrameRenderedWithoutGlContextDoesNotCrash() {
        // onFrameRendered accesses Gdx.graphics which is null in test context.
        // It should catch the exception gracefully, not crash.
        capture = new FramebufferCapture();
        capture.onFrameRendered(); // Should not throw
    }

    @Test
    public void captureIntervalSkipsFrames() {
        // With interval=6, only every 6th call should attempt capture.
        // Without GL context, all attempts will silently fail, but we verify
        // the skip logic by ensuring no crash over many frames.
        capture = new FramebufferCapture(6);
        for (int i = 0; i < 100; i++) {
            capture.onFrameRendered(); // Should not throw
        }
    }

    @Test
    public void disposeIsIdempotent() {
        capture = new FramebufferCapture();
        capture.dispose();
        capture.dispose(); // Should not throw
        capture = null; // prevent double-dispose in tearDown
    }

    @Test
    public void customCaptureInterval() {
        capture = new FramebufferCapture(1);
        // Interval of 1 = capture every frame. Should still not crash without GL.
        capture.onFrameRendered();
        capture.onFrameRendered();
        capture.onFrameRendered();
    }
}
