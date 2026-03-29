package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

import java.util.concurrent.ConcurrentLinkedQueue;

public class InputBridge {

    public static class InputEvent {
        public enum Type { TOUCH_DOWN, TOUCH_UP, KEY_DOWN, KEY_UP }

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
    }

    private final ConcurrentLinkedQueue<InputEvent> eventQueue = new ConcurrentLinkedQueue<>();

    /**
     * Called from HTTP thread.
     */
    public void enqueue(InputEvent event) {
        eventQueue.add(event);
    }

    /**
     * Called from GL thread each frame. Dispatches all queued events to the current InputProcessor.
     */
    public void drainEvents() {
        InputProcessor processor = Gdx.input.getInputProcessor();
        if (processor == null) {
            eventQueue.clear();
            return;
        }

        InputEvent event;
        while ((event = eventQueue.poll()) != null) {
            switch (event.type) {
                case TOUCH_DOWN:
                    processor.touchDown(event.x, event.y, 0, event.button);
                    break;
                case TOUCH_UP:
                    processor.touchUp(event.x, event.y, 0, event.button);
                    break;
                case KEY_DOWN:
                    processor.keyDown(event.keyCode);
                    break;
                case KEY_UP:
                    processor.keyUp(event.keyCode);
                    break;
            }
        }
    }
}
