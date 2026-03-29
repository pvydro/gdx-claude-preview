package com.pvydro.gdxclaudepreview;

import org.junit.Test;

import static org.junit.Assert.*;

public class FramebufferCaptureTest {

    @Test
    public void requestScreenshotTimesOutWhenNothingCompletes() {
        FramebufferCapture capture = new FramebufferCapture();

        long start = System.currentTimeMillis();
        byte[] result = capture.requestScreenshot(100);
        long elapsed = System.currentTimeMillis() - start;

        assertNull(result);
        assertTrue("Should have waited ~100ms, but waited " + elapsed, elapsed >= 80);
    }

    @Test
    public void cachedScreenshotInitiallyNull() {
        FramebufferCapture capture = new FramebufferCapture();
        assertNull(capture.getCachedScreenshot());
    }

    @Test
    public void captureIfRequestedWithNoPendingIsNoOp() {
        // This would NPE or throw if it incorrectly tried to capture without a pending request.
        // Without GL context, captureIfRequested should just return immediately
        // since pendingRequest is null.
        FramebufferCapture capture = new FramebufferCapture();
        capture.captureIfRequested(); // Should not throw
    }

    @Test
    public void concurrentRequestsReturnCachedOrWait() throws Exception {
        FramebufferCapture capture = new FramebufferCapture();

        // First request will set a pending future
        Thread t1 = new Thread(() -> capture.requestScreenshot(200));
        t1.start();

        // Give t1 time to set the pending request
        Thread.sleep(50);

        // Second request while first is pending — should return null (no cache yet)
        byte[] result = capture.requestScreenshot(50);
        assertNull(result);

        t1.join(500);
    }
}
