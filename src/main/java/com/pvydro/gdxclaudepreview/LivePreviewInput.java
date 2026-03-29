package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

/**
 * Wraps the real {@link Input} to blend preview input state into polling results.
 * <p>
 * For position queries ({@code getX}, {@code getY}), returns the preview position
 * when preview is actively sending input (within 500ms), otherwise delegates to real input.
 * <p>
 * For pressed-state queries ({@code isKeyPressed}, {@code isButtonPressed}, etc.),
 * uses OR logic: returns true if either real input or preview says pressed.
 * This allows the developer to use real keyboard/mouse alongside the preview.
 */
public class LivePreviewInput implements Input {

    private Input delegate;
    private final InputBridge bridge;

    public LivePreviewInput(Input delegate, InputBridge bridge) {
        this.delegate = delegate;
        this.bridge = bridge;
    }

    /** Update the underlying real Input (called when LWJGL3 replaces Gdx.input). */
    public void setDelegate(Input delegate) {
        this.delegate = delegate;
    }

    public Input getDelegate() {
        return delegate;
    }

    // --- Position polling (preview overrides when active) ---

    @Override
    public int getX() {
        return bridge.isPreviewActive() ? bridge.getPreviewX() : delegate.getX();
    }

    @Override
    public int getX(int pointer) {
        return (pointer == 0 && bridge.isPreviewActive()) ? bridge.getPreviewX() : delegate.getX(pointer);
    }

    @Override
    public int getY() {
        return bridge.isPreviewActive() ? bridge.getPreviewY() : delegate.getY();
    }

    @Override
    public int getY(int pointer) {
        return (pointer == 0 && bridge.isPreviewActive()) ? bridge.getPreviewY() : delegate.getY(pointer);
    }

    @Override
    public int getDeltaX() {
        return bridge.isPreviewActive() ? bridge.getPreviewDeltaX() : delegate.getDeltaX();
    }

    @Override
    public int getDeltaX(int pointer) {
        return (pointer == 0 && bridge.isPreviewActive()) ? bridge.getPreviewDeltaX() : delegate.getDeltaX(pointer);
    }

    @Override
    public int getDeltaY() {
        return bridge.isPreviewActive() ? bridge.getPreviewDeltaY() : delegate.getDeltaY();
    }

    @Override
    public int getDeltaY(int pointer) {
        return (pointer == 0 && bridge.isPreviewActive()) ? bridge.getPreviewDeltaY() : delegate.getDeltaY(pointer);
    }

    // --- Pressed state polling (OR logic) ---

    @Override
    public boolean isKeyPressed(int key) {
        return delegate.isKeyPressed(key) || bridge.isPreviewKeyPressed(key);
    }

    @Override
    public boolean isKeyJustPressed(int key) {
        return delegate.isKeyJustPressed(key) || bridge.isPreviewKeyJustPressed(key);
    }

    @Override
    public boolean isButtonPressed(int button) {
        return delegate.isButtonPressed(button) || bridge.isPreviewButtonPressed(button);
    }

    @Override
    public boolean isButtonJustPressed(int button) {
        return delegate.isButtonJustPressed(button) || bridge.isPreviewButtonJustPressed(button);
    }

    @Override
    public boolean isTouched() {
        return delegate.isTouched() || bridge.isPreviewTouched();
    }

    @Override
    public boolean isTouched(int pointer) {
        return delegate.isTouched(pointer) || (pointer == 0 && bridge.isPreviewTouched());
    }

    @Override
    public boolean justTouched() {
        return delegate.justTouched() || bridge.isPreviewJustTouched();
    }

    @Override
    public float getPressure() {
        return bridge.isPreviewTouched() ? 1.0f : delegate.getPressure();
    }

    @Override
    public float getPressure(int pointer) {
        return (pointer == 0 && bridge.isPreviewTouched()) ? 1.0f : delegate.getPressure(pointer);
    }

    // --- Everything below delegates directly ---

    @Override
    public float getAccelerometerX() { return delegate.getAccelerometerX(); }

    @Override
    public float getAccelerometerY() { return delegate.getAccelerometerY(); }

    @Override
    public float getAccelerometerZ() { return delegate.getAccelerometerZ(); }

    @Override
    public float getGyroscopeX() { return delegate.getGyroscopeX(); }

    @Override
    public float getGyroscopeY() { return delegate.getGyroscopeY(); }

    @Override
    public float getGyroscopeZ() { return delegate.getGyroscopeZ(); }

    @Override
    public int getMaxPointers() { return delegate.getMaxPointers(); }

    @Override
    public float getAzimuth() { return delegate.getAzimuth(); }

    @Override
    public float getPitch() { return delegate.getPitch(); }

    @Override
    public float getRoll() { return delegate.getRoll(); }

    @Override
    public void getRotationMatrix(float[] matrix) { delegate.getRotationMatrix(matrix); }

    @Override
    public long getCurrentEventTime() { return delegate.getCurrentEventTime(); }

    @Override
    public void setCatchBackKey(boolean catchBack) { delegate.setCatchBackKey(catchBack); }

    @Override
    public boolean isCatchBackKey() { return delegate.isCatchBackKey(); }

    @Override
    public void setCatchMenuKey(boolean catchMenu) { delegate.setCatchMenuKey(catchMenu); }

    @Override
    public boolean isCatchMenuKey() { return delegate.isCatchMenuKey(); }

    @Override
    public void setCatchKey(int keycode, boolean catchKey) { delegate.setCatchKey(keycode, catchKey); }

    @Override
    public boolean isCatchKey(int keycode) { return delegate.isCatchKey(keycode); }

    @Override
    public void setInputProcessor(InputProcessor processor) { delegate.setInputProcessor(processor); }

    @Override
    public InputProcessor getInputProcessor() { return delegate.getInputProcessor(); }

    @Override
    public boolean isPeripheralAvailable(Peripheral peripheral) { return delegate.isPeripheralAvailable(peripheral); }

    @Override
    public int getRotation() { return delegate.getRotation(); }

    @Override
    public Orientation getNativeOrientation() { return delegate.getNativeOrientation(); }

    @Override
    public void setCursorCatched(boolean catched) { delegate.setCursorCatched(catched); }

    @Override
    public boolean isCursorCatched() { return delegate.isCursorCatched(); }

    @Override
    public void setCursorPosition(int x, int y) { delegate.setCursorPosition(x, y); }

    @Override
    public void getTextInput(TextInputListener listener, String title, String text, String hint) {
        delegate.getTextInput(listener, title, text, hint);
    }

    @Override
    public void getTextInput(TextInputListener listener, String title, String text, String hint, OnscreenKeyboardType type) {
        delegate.getTextInput(listener, title, text, hint, type);
    }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible) { delegate.setOnscreenKeyboardVisible(visible); }

    @Override
    public void setOnscreenKeyboardVisible(boolean visible, OnscreenKeyboardType type) {
        delegate.setOnscreenKeyboardVisible(visible, type);
    }

    @Override
    public void vibrate(int milliseconds) { delegate.vibrate(milliseconds); }

    @Override
    public void vibrate(int milliseconds, boolean fallback) { delegate.vibrate(milliseconds, fallback); }

    @Override
    public void vibrate(int milliseconds, int amplitude, boolean fallback) {
        delegate.vibrate(milliseconds, amplitude, fallback);
    }

    @Override
    public void vibrate(VibrationType vibrationType) { delegate.vibrate(vibrationType); }
}
