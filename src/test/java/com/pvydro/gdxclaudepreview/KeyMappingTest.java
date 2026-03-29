package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Input;
import org.junit.Test;

import static org.junit.Assert.*;

public class KeyMappingTest {

    @Test
    public void mapsLetters() {
        assertEquals(Input.Keys.A, KeyMapping.toLibGdx("KeyA"));
        assertEquals(Input.Keys.Z, KeyMapping.toLibGdx("KeyZ"));
        assertEquals(Input.Keys.M, KeyMapping.toLibGdx("KeyM"));
    }

    @Test
    public void mapsDigits() {
        assertEquals(Input.Keys.NUM_0, KeyMapping.toLibGdx("Digit0"));
        assertEquals(Input.Keys.NUM_9, KeyMapping.toLibGdx("Digit9"));
        assertEquals(Input.Keys.NUM_5, KeyMapping.toLibGdx("Digit5"));
    }

    @Test
    public void mapsArrowKeys() {
        assertEquals(Input.Keys.UP, KeyMapping.toLibGdx("ArrowUp"));
        assertEquals(Input.Keys.DOWN, KeyMapping.toLibGdx("ArrowDown"));
        assertEquals(Input.Keys.LEFT, KeyMapping.toLibGdx("ArrowLeft"));
        assertEquals(Input.Keys.RIGHT, KeyMapping.toLibGdx("ArrowRight"));
    }

    @Test
    public void mapsModifiers() {
        assertEquals(Input.Keys.SHIFT_LEFT, KeyMapping.toLibGdx("ShiftLeft"));
        assertEquals(Input.Keys.SHIFT_RIGHT, KeyMapping.toLibGdx("ShiftRight"));
        assertEquals(Input.Keys.CONTROL_LEFT, KeyMapping.toLibGdx("ControlLeft"));
        assertEquals(Input.Keys.ALT_LEFT, KeyMapping.toLibGdx("AltLeft"));
    }

    @Test
    public void mapsCommonKeys() {
        assertEquals(Input.Keys.SPACE, KeyMapping.toLibGdx("Space"));
        assertEquals(Input.Keys.ENTER, KeyMapping.toLibGdx("Enter"));
        assertEquals(Input.Keys.ESCAPE, KeyMapping.toLibGdx("Escape"));
        assertEquals(Input.Keys.BACKSPACE, KeyMapping.toLibGdx("Backspace"));
        assertEquals(Input.Keys.TAB, KeyMapping.toLibGdx("Tab"));
    }

    @Test
    public void mapsFunctionKeys() {
        assertEquals(Input.Keys.F1, KeyMapping.toLibGdx("F1"));
        assertEquals(Input.Keys.F12, KeyMapping.toLibGdx("F12"));
    }

    @Test
    public void mapsPunctuation() {
        assertEquals(Input.Keys.COMMA, KeyMapping.toLibGdx("Comma"));
        assertEquals(Input.Keys.PERIOD, KeyMapping.toLibGdx("Period"));
        assertEquals(Input.Keys.MINUS, KeyMapping.toLibGdx("Minus"));
        assertEquals(Input.Keys.EQUALS, KeyMapping.toLibGdx("Equal"));
    }

    @Test
    public void unmappedReturnsNegativeOne() {
        assertEquals(-1, KeyMapping.toLibGdx("NonExistentKey"));
        assertEquals(-1, KeyMapping.toLibGdx(""));
    }

    @Test
    public void nullReturnsNegativeOne() {
        assertEquals(-1, KeyMapping.toLibGdx(null));
    }
}
