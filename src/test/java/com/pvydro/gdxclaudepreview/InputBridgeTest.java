package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import org.junit.Before;
import org.junit.Test;

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
    public void noProcessorClearsQueueWithoutError() {
        when(mockInput.getInputProcessor()).thenReturn(null);

        bridge.enqueue(InputBridge.InputEvent.click(50, 50, Input.Buttons.LEFT));
        bridge.drainEvents(); // Should not throw
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
}
