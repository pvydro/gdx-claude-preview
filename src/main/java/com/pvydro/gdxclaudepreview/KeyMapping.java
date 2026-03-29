package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Input;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps JavaScript KeyboardEvent.code strings to LibGDX Input.Keys constants.
 */
public class KeyMapping {

    private static final Map<String, Integer> MAP = new HashMap<>();

    static {
        // Letters
        MAP.put("KeyA", Input.Keys.A);
        MAP.put("KeyB", Input.Keys.B);
        MAP.put("KeyC", Input.Keys.C);
        MAP.put("KeyD", Input.Keys.D);
        MAP.put("KeyE", Input.Keys.E);
        MAP.put("KeyF", Input.Keys.F);
        MAP.put("KeyG", Input.Keys.G);
        MAP.put("KeyH", Input.Keys.H);
        MAP.put("KeyI", Input.Keys.I);
        MAP.put("KeyJ", Input.Keys.J);
        MAP.put("KeyK", Input.Keys.K);
        MAP.put("KeyL", Input.Keys.L);
        MAP.put("KeyM", Input.Keys.M);
        MAP.put("KeyN", Input.Keys.N);
        MAP.put("KeyO", Input.Keys.O);
        MAP.put("KeyP", Input.Keys.P);
        MAP.put("KeyQ", Input.Keys.Q);
        MAP.put("KeyR", Input.Keys.R);
        MAP.put("KeyS", Input.Keys.S);
        MAP.put("KeyT", Input.Keys.T);
        MAP.put("KeyU", Input.Keys.U);
        MAP.put("KeyV", Input.Keys.V);
        MAP.put("KeyW", Input.Keys.W);
        MAP.put("KeyX", Input.Keys.X);
        MAP.put("KeyY", Input.Keys.Y);
        MAP.put("KeyZ", Input.Keys.Z);

        // Digits
        MAP.put("Digit0", Input.Keys.NUM_0);
        MAP.put("Digit1", Input.Keys.NUM_1);
        MAP.put("Digit2", Input.Keys.NUM_2);
        MAP.put("Digit3", Input.Keys.NUM_3);
        MAP.put("Digit4", Input.Keys.NUM_4);
        MAP.put("Digit5", Input.Keys.NUM_5);
        MAP.put("Digit6", Input.Keys.NUM_6);
        MAP.put("Digit7", Input.Keys.NUM_7);
        MAP.put("Digit8", Input.Keys.NUM_8);
        MAP.put("Digit9", Input.Keys.NUM_9);

        // Function keys
        MAP.put("F1", Input.Keys.F1);
        MAP.put("F2", Input.Keys.F2);
        MAP.put("F3", Input.Keys.F3);
        MAP.put("F4", Input.Keys.F4);
        MAP.put("F5", Input.Keys.F5);
        MAP.put("F6", Input.Keys.F6);
        MAP.put("F7", Input.Keys.F7);
        MAP.put("F8", Input.Keys.F8);
        MAP.put("F9", Input.Keys.F9);
        MAP.put("F10", Input.Keys.F10);
        MAP.put("F11", Input.Keys.F11);
        MAP.put("F12", Input.Keys.F12);

        // Arrows
        MAP.put("ArrowUp", Input.Keys.UP);
        MAP.put("ArrowDown", Input.Keys.DOWN);
        MAP.put("ArrowLeft", Input.Keys.LEFT);
        MAP.put("ArrowRight", Input.Keys.RIGHT);

        // Modifiers
        MAP.put("ShiftLeft", Input.Keys.SHIFT_LEFT);
        MAP.put("ShiftRight", Input.Keys.SHIFT_RIGHT);
        MAP.put("ControlLeft", Input.Keys.CONTROL_LEFT);
        MAP.put("ControlRight", Input.Keys.CONTROL_RIGHT);
        MAP.put("AltLeft", Input.Keys.ALT_LEFT);
        MAP.put("AltRight", Input.Keys.ALT_RIGHT);

        // Common keys
        MAP.put("Space", Input.Keys.SPACE);
        MAP.put("Enter", Input.Keys.ENTER);
        MAP.put("Escape", Input.Keys.ESCAPE);
        MAP.put("Backspace", Input.Keys.BACKSPACE);
        MAP.put("Tab", Input.Keys.TAB);
        MAP.put("Delete", Input.Keys.FORWARD_DEL);
        MAP.put("Insert", Input.Keys.INSERT);
        MAP.put("Home", Input.Keys.HOME);
        MAP.put("End", Input.Keys.END);
        MAP.put("PageUp", Input.Keys.PAGE_UP);
        MAP.put("PageDown", Input.Keys.PAGE_DOWN);

        // Punctuation
        MAP.put("Comma", Input.Keys.COMMA);
        MAP.put("Period", Input.Keys.PERIOD);
        MAP.put("Slash", Input.Keys.SLASH);
        MAP.put("Semicolon", Input.Keys.SEMICOLON);
        MAP.put("Quote", Input.Keys.APOSTROPHE);
        MAP.put("BracketLeft", Input.Keys.LEFT_BRACKET);
        MAP.put("BracketRight", Input.Keys.RIGHT_BRACKET);
        MAP.put("Backslash", Input.Keys.BACKSLASH);
        MAP.put("Minus", Input.Keys.MINUS);
        MAP.put("Equal", Input.Keys.EQUALS);
        MAP.put("Backquote", Input.Keys.GRAVE);
    }

    /**
     * @return LibGDX keycode for the given JS KeyboardEvent.code, or -1 if unmapped.
     */
    public static int toLibGdx(String jsCode) {
        Integer key = MAP.get(jsCode);
        return key != null ? key : -1;
    }
}
