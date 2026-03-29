package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class InputBridgeTest {

    private InputBridge bridge;
    private Input mockInput;
    private InputProcessor mockProcessor;

    @Before
    public void setUp() {
        bridge = new InputBridge();
        mockInput = mock(Input.class);
        mockProcessor = mock(InputProcessor.class);
        Gdx.input = mockInput;
    }

    // --- Event dispatch tests ---

    @Test
    public void clickEventDispatchesTouchDownAndUp() {
        when(mockInput.getInputProcessor()).thenReturn(mockProcessor);

        bridge.enqueue(InputBridge.InputEvent.click(100, 200, Input.Buttons.LEFT));
        bridge.enqueue(InputBridge.InputEvent.clickUp(100, 200, Input.Buttons.LEFT));
        bridge.drainEvents();

        verify(mockProcessor).touchDown(100, 200, 0, Input.Buttons.LEFT);
        verify(mockProcessor).touchUp(100, 200, 0, Input.Buttons.LEFT);
    }

    @Test
    public void keyEventDispatchesKeyDownAndUp() {
        when(mockInput.getInputProcessor()).thenReturn(mockProcessor);

        bridge.enqueue(InputBridge.InputEvent.keyDown(Input.Keys.SPACE));
        bridge.enqueue(InputBridge.InputEvent.keyUp(Input.Keys.SPACE));
        bridge.drainEvents();

        verify(mockProcessor).keyDown(Input.Keys.SPACE);
        verify(mockProcessor).keyUp(Input.Keys.SPACE);
    }

    @Test
    public void mouseMoveDispatchesToProcessorMouseMoved() {
        when(mockInput.getInputProcessor()).thenReturn(mockProcessor);

        bridge.enqueue(InputBridge.InputEvent.mouseMove(300, 400));
        bridge.drainEvents();

        verify(mockProcessor).mouseMoved(300, 400);
    }

    @Test
    public void noProcessorStillUpdatesPollingState() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(50, 50, Input.Buttons.LEFT));
        bridge.drainEvents(); // Should not throw

        // Polling state still updated even without processor
        assertTrue(bridge.isPreviewButtonPressed(Input.Buttons.LEFT));
        assertEquals(50, bridge.getPreviewX());
        assertEquals(50, bridge.getPreviewY());
    }

    @Test
    public void multipleEventsDrainInOrder() {
        when(mockInput.getInputProcessor()).thenReturn(mockProcessor);

        bridge.enqueue(InputBridge.InputEvent.keyDown(Input.Keys.A));
        bridge.enqueue(InputBridge.InputEvent.keyDown(Input.Keys.B));
        bridge.enqueue(InputBridge.InputEvent.keyUp(Input.Keys.A));
        bridge.enqueue(InputBridge.InputEvent.keyUp(Input.Keys.B));
        bridge.drainEvents();

        org.mockito.InOrder inOrder = inOrder(mockProcessor);
        inOrder.verify(mockProcessor).keyDown(Input.Keys.A);
        inOrder.verify(mockProcessor).keyDown(Input.Keys.B);
        inOrder.verify(mockProcessor).keyUp(Input.Keys.A);
        inOrder.verify(mockProcessor).keyUp(Input.Keys.B);
    }

    @Test
    public void drainWithEmptyQueueIsNoOp() {
        when(mockInput.getInputProcessor()).thenReturn(mockProcessor);
        bridge.drainEvents();
        verifyNoInteractions(mockProcessor);
    }

    // --- Polling state tests ---

    @Test
    public void mouseMoveUpdatesPreviewPosition() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.mouseMove(150, 250));
        bridge.drainEvents();

        assertEquals(150, bridge.getPreviewX());
        assertEquals(250, bridge.getPreviewY());
        assertTrue(bridge.isPreviewActive());
    }

    @Test
    public void touchDownSetsButtonPressedAndTouchedState() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(100, 200, Input.Buttons.LEFT));
        bridge.drainEvents();

        assertTrue(bridge.isPreviewButtonPressed(Input.Buttons.LEFT));
        assertTrue(bridge.isPreviewButtonJustPressed(Input.Buttons.LEFT));
        assertTrue(bridge.isPreviewTouched());
        assertTrue(bridge.isPreviewJustTouched());
        assertEquals(100, bridge.getPreviewX());
        assertEquals(200, bridge.getPreviewY());
    }

    @Test
    public void touchUpClearsButtonPressedState() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(100, 200, Input.Buttons.LEFT));
        bridge.enqueue(InputBridge.InputEvent.clickUp(100, 200, Input.Buttons.LEFT));
        bridge.drainEvents();

        assertFalse(bridge.isPreviewButtonPressed(Input.Buttons.LEFT));
        assertFalse(bridge.isPreviewTouched());
        // justPressed is still true because it happened this frame
        assertTrue(bridge.isPreviewButtonJustPressed(Input.Buttons.LEFT));
        assertTrue(bridge.isPreviewJustTouched());
    }

    @Test
    public void keyDownSetsKeyPressedState() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.keyDown(Input.Keys.W));
        bridge.drainEvents();

        assertTrue(bridge.isPreviewKeyPressed(Input.Keys.W));
        assertTrue(bridge.isPreviewKeyJustPressed(Input.Keys.W));
    }

    @Test
    public void keyUpClearsKeyPressedState() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.keyDown(Input.Keys.W));
        bridge.enqueue(InputBridge.InputEvent.keyUp(Input.Keys.W));
        bridge.drainEvents();

        assertFalse(bridge.isPreviewKeyPressed(Input.Keys.W));
    }

    @Test
    public void justPressedFlagsClearedOnNextDrain() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.keyDown(Input.Keys.A));
        bridge.enqueue(InputBridge.InputEvent.click(10, 20, Input.Buttons.LEFT));
        bridge.drainEvents();

        assertTrue(bridge.isPreviewKeyJustPressed(Input.Keys.A));
        assertTrue(bridge.isPreviewButtonJustPressed(Input.Buttons.LEFT));
        assertTrue(bridge.isPreviewJustTouched());

        // Next frame — just-pressed flags reset
        bridge.drainEvents();

        assertFalse(bridge.isPreviewKeyJustPressed(Input.Keys.A));
        assertFalse(bridge.isPreviewButtonJustPressed(Input.Buttons.LEFT));
        assertFalse(bridge.isPreviewJustTouched());

        // But pressed state persists (key/button still held)
        assertTrue(bridge.isPreviewKeyPressed(Input.Keys.A));
        assertTrue(bridge.isPreviewButtonPressed(Input.Buttons.LEFT));
    }

    @Test
    public void deltaComputedFromPreviousFrame() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.mouseMove(100, 200));
        bridge.drainEvents();

        // First frame: delta from 0,0 to 100,200
        assertEquals(100, bridge.getPreviewDeltaX());
        assertEquals(200, bridge.getPreviewDeltaY());

        bridge.enqueue(InputBridge.InputEvent.mouseMove(130, 210));
        bridge.drainEvents();

        // Second frame: delta from 100,200 to 130,210
        assertEquals(30, bridge.getPreviewDeltaX());
        assertEquals(10, bridge.getPreviewDeltaY());
    }

    @Test
    public void multipleButtonsTrackedIndependently() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(10, 10, Input.Buttons.LEFT));
        bridge.enqueue(InputBridge.InputEvent.click(10, 10, Input.Buttons.RIGHT));
        bridge.drainEvents();

        assertTrue(bridge.isPreviewButtonPressed(Input.Buttons.LEFT));
        assertTrue(bridge.isPreviewButtonPressed(Input.Buttons.RIGHT));
        assertTrue(bridge.isPreviewTouched());

        // Release only left
        bridge.drainEvents(); // clear just-pressed
        bridge.enqueue(InputBridge.InputEvent.clickUp(10, 10, Input.Buttons.LEFT));
        bridge.drainEvents();

        assertFalse(bridge.isPreviewButtonPressed(Input.Buttons.LEFT));
        assertTrue(bridge.isPreviewButtonPressed(Input.Buttons.RIGHT));
        assertTrue(bridge.isPreviewTouched()); // still touched because right is held
    }
}
