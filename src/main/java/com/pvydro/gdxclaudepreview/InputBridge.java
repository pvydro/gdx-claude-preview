package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InputBridge {

    private static final long PREVIEW_ACTIVE_TIMEOUT_NS = 500_000_000L; // 500ms

    public static class InputEvent {
        public enum Type { TOUCH_DOWN, TOUCH_UP, KEY_DOWN, KEY_UP, MOUSE_MOVE }

        public final Type type;
        public final int x, y, button, keyCode;

        public InputEvent(Type type, int x, int y, int button, int keyCode) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.button = button;
            this.keyCode = keyCode;
        }

        public static InputEvent click(int x, int y, int button) {
            return new InputEvent(Type.TOUCH_DOWN, x, y, button, 0);
        }

        public static InputEvent clickUp(int x, int y, int button) {
            return new InputEvent(Type.TOUCH_UP, x, y, button, 0);
        }

        public static InputEvent keyDown(int keyCode) {
            return new InputEvent(Type.KEY_DOWN, 0, 0, 0, keyCode);
        }

        public static InputEvent keyUp(int keyCode) {
            return new InputEvent(Type.KEY_UP, 0, 0, 0, keyCode);
        }

        public static InputEvent mouseMove(int x, int y) {
            return new InputEvent(Type.MOUSE_MOVE, x, y, 0, 0);
        }
    }

    private final ConcurrentLinkedQueue<InputEvent> eventQueue = new ConcurrentLinkedQueue<>();

    // Polling state — written and read on GL thread only
    private int previewX, previewY;
    private int prevPreviewX, prevPreviewY;
    private final Set<Integer> pressedButtons = new HashSet<>();
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> justPressedButtons = new HashSet<>();
    private final Set<Integer> justPressedKeys = new HashSet<>();
    private boolean previewTouched;
    private boolean previewJustTouched;
    private long lastPreviewInputTime;
    private boolean previewActive;

    /**
     * Called from HTTP thread.
     */
    public void enqueue(InputEvent event) {
        eventQueue.add(event);
    }

    /**
     * Called from GL thread each frame. Dispatches all queued events to the current InputProcessor
     * and updates polling state.
     */
    public void drainEvents() {
        // Reset per-frame state
        justPressedButtons.clear();
        justPressedKeys.clear();
        previewJustTouched = false;
        prevPreviewX = previewX;
        prevPreviewY = previewY;
        previewActive = (System.nanoTime() - lastPreviewInputTime) < PREVIEW_ACTIVE_TIMEOUT_NS;

        InputProcessor processor = Gdx.input.getInputProcessor();

        InputEvent event;
        while ((event = eventQueue.poll()) != null) {
            lastPreviewInputTime = System.nanoTime();
            previewActive = true;

            switch (event.type) {
                case MOUSE_MOVE:
                    previewX = event.x;
                    previewY = event.y;
                    if (processor != null) {
                        processor.mouseMoved(event.x, event.y);
                    }
                    break;
                case TOUCH_DOWN:
                    previewX = event.x;
                    previewY = event.y;
                    pressedButtons.add(event.button);
                    justPressedButtons.add(event.button);
                    previewTouched = true;
                    previewJustTouched = true;
                    if (processor != null) {
                        processor.touchDown(event.x, event.y, 0, event.button);
                    }
                    break;
                case TOUCH_UP:
                    pressedButtons.remove(event.button);
                    previewTouched = !pressedButtons.isEmpty();
                    if (processor != null) {
                        processor.touchUp(event.x, event.y, 0, event.button);
                    }
                    break;
                case KEY_DOWN:
                    pressedKeys.add(event.keyCode);
                    justPressedKeys.add(event.keyCode);
                    if (processor != null) {
                        processor.keyDown(event.keyCode);
                    }
                    break;
                case KEY_UP:
                    pressedKeys.remove(event.keyCode);
                    if (processor != null) {
                        processor.keyUp(event.keyCode);
                    }
                    break;
            }
        }
    }

    // Polling state getters — called from GL thread via LivePreviewInput

    public int getPreviewX() { return previewX; }
    public int getPreviewY() { return previewY; }
    public int getPreviewDeltaX() { return previewX - prevPreviewX; }
    public int getPreviewDeltaY() { return previewY - prevPreviewY; }
    public boolean isPreviewActive() { return previewActive; }
    public boolean isPreviewTouched() { return previewTouched; }
    public boolean isPreviewJustTouched() { return previewJustTouched; }
    public boolean isPreviewButtonPressed(int button) { return pressedButtons.contains(button); }
    public boolean isPreviewButtonJustPressed(int button) { return justPressedButtons.contains(button); }
    public boolean isPreviewKeyPressed(int keyCode) { return pressedKeys.contains(keyCode); }
    public boolean isPreviewKeyJustPressed(int keyCode) { return justPressedKeys.contains(keyCode); }
}
