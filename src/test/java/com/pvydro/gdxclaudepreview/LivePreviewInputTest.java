package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LivePreviewInputTest {

    private Input mockDelegate;
    private InputBridge bridge;
    private LivePreviewInput liveInput;

    @Before
    public void setUp() {
        mockDelegate = mock(Input.class);
        bridge = new InputBridge();
        liveInput = new LivePreviewInput(mockDelegate, bridge);
        // InputBridge.drainEvents() reads Gdx.input.getInputProcessor()
        Gdx.input = liveInput;
    }

    private void activatePreviewWithMouseMove(int x, int y) {
        when(mockDelegate.getInputProcessor()).thenReturn(null);
        bridge.enqueue(InputBridge.InputEvent.mouseMove(x, y));
        bridge.drainEvents();
    }

    // --- Position polling ---

    @Test
    public void getXReturnsPreviewPositionWhenActive() {
        activatePreviewWithMouseMove(300, 400);

        assertEquals(300, liveInput.getX());
        assertEquals(300, liveInput.getX(0));
    }

    @Test
    public void getYReturnsPreviewPositionWhenActive() {
        activatePreviewWithMouseMove(300, 400);

        assertEquals(400, liveInput.getY());
        assertEquals(400, liveInput.getY(0));
    }

    @Test
    public void getXDelegatesToRealInputWhenInactive() {
        // No preview events sent, so preview is inactive
        when(mockDelegate.getX()).thenReturn(500);
        when(mockDelegate.getX(0)).thenReturn(500);

        assertEquals(500, liveInput.getX());
        assertEquals(500, liveInput.getX(0));
    }

    @Test
    public void nonZeroPointerAlwaysDelegates() {
        activatePreviewWithMouseMove(300, 400);

        when(mockDelegate.getX(1)).thenReturn(999);
        when(mockDelegate.getY(1)).thenReturn(888);

        assertEquals(999, liveInput.getX(1));
        assertEquals(888, liveInput.getY(1));
    }

    @Test
    public void deltaXYReturnsPreviewDeltaWhenActive() {
        when(mockDelegate.getInputProcessor()).thenReturn(null);
        bridge.enqueue(InputBridge.InputEvent.mouseMove(100, 200));
        bridge.drainEvents();

        bridge.enqueue(InputBridge.InputEvent.mouseMove(130, 215));
        bridge.drainEvents();

        assertEquals(30, liveInput.getDeltaX());
        assertEquals(15, liveInput.getDeltaY());
    }

    // --- Pressed state (OR logic) ---

    @Test
    public void isKeyPressedORsPreviewAndReal() {
        // Real says pressed
        when(mockDelegate.isKeyPressed(Input.Keys.W)).thenReturn(true);
        assertTrue(liveInput.isKeyPressed(Input.Keys.W));

        // Real says not pressed, but preview has it pressed
        when(mockDelegate.isKeyPressed(Input.Keys.A)).thenReturn(false);
        when(mockDelegate.getInputProcessor()).thenReturn(null);
        bridge.enqueue(InputBridge.InputEvent.keyDown(Input.Keys.A));
        bridge.drainEvents();
        assertTrue(liveInput.isKeyPressed(Input.Keys.A));

        // Neither has it pressed
        when(mockDelegate.isKeyPressed(Input.Keys.D)).thenReturn(false);
        assertFalse(liveInput.isKeyPressed(Input.Keys.D));
    }

    @Test
    public void isButtonPressedORsPreviewAndReal() {
        when(mockDelegate.isButtonPressed(Input.Buttons.LEFT)).thenReturn(false);
        when(mockDelegate.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(50, 50, Input.Buttons.LEFT));
        bridge.drainEvents();

        assertTrue(liveInput.isButtonPressed(Input.Buttons.LEFT));
    }

    @Test
    public void isButtonJustPressedORsPreviewAndReal() {
        when(mockDelegate.isButtonJustPressed(Input.Buttons.RIGHT)).thenReturn(false);
        when(mockDelegate.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(50, 50, Input.Buttons.RIGHT));
        bridge.drainEvents();

        assertTrue(liveInput.isButtonJustPressed(Input.Buttons.RIGHT));
    }

    @Test
    public void isTouchedORsPreviewAndReal() {
        when(mockDelegate.isTouched()).thenReturn(false);
        when(mockDelegate.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(50, 50, Input.Buttons.LEFT));
        bridge.drainEvents();

        assertTrue(liveInput.isTouched());
    }

    @Test
    public void justTouchedORsPreviewAndReal() {
        when(mockDelegate.justTouched()).thenReturn(false);
        when(mockDelegate.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(50, 50, Input.Buttons.LEFT));
        bridge.drainEvents();

        assertTrue(liveInput.justTouched());
    }

    @Test
    public void getPressureReturnsOneWhenPreviewTouched() {
        when(mockDelegate.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(50, 50, Input.Buttons.LEFT));
        bridge.drainEvents();

        assertEquals(1.0f, liveInput.getPressure(), 0.001f);
        assertEquals(1.0f, liveInput.getPressure(0), 0.001f);
    }

    // --- Delegation tests ---

    @Test
    public void nonPollingMethodsDelegateDirectly() {
        InputProcessor proc = mock(InputProcessor.class);
        liveInput.setInputProcessor(proc);
        verify(mockDelegate).setInputProcessor(proc);

        when(mockDelegate.getInputProcessor()).thenReturn(proc);
        assertSame(proc, liveInput.getInputProcessor());

        when(mockDelegate.getAzimuth()).thenReturn(1.5f);
        assertEquals(1.5f, liveInput.getAzimuth(), 0.001f);

        when(mockDelegate.isPeripheralAvailable(Input.Peripheral.Gyroscope)).thenReturn(true);
        assertTrue(liveInput.isPeripheralAvailable(Input.Peripheral.Gyroscope));
    }
}
