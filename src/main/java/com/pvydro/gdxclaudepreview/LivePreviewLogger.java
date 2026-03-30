package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.ApplicationLogger;

/**
 * Wraps the existing ApplicationLogger to tee log messages into a {@link LogBuffer}
 * while preserving the original logging behavior.
 */
public class LivePreviewLogger implements ApplicationLogger {

    private final ApplicationLogger delegate;
    private final LogBuffer logBuffer;

    public LivePreviewLogger(ApplicationLogger delegate, LogBuffer logBuffer) {
        this.delegate = delegate;
        this.logBuffer = logBuffer;
    }

    @Override
    public void log(String tag, String message) {
        logBuffer.append("LOG", tag, message);
        delegate.log(tag, message);
    }

    @Override
    public void log(String tag, String message, Throwable exception) {
        logBuffer.append("LOG", tag, message + "\n" + stackTraceToString(exception));
        delegate.log(tag, message, exception);
    }

    @Override
    public void error(String tag, String message) {
        logBuffer.append("ERROR", tag, message);
        delegate.error(tag, message);
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        logBuffer.append("ERROR", tag, message + "\n" + stackTraceToString(exception));
        delegate.error(tag, message, exception);
    }

    @Override
    public void debug(String tag, String message) {
        logBuffer.append("DEBUG", tag, message);
        delegate.debug(tag, message);
    }

    @Override
    public void debug(String tag, String message, Throwable exception) {
        logBuffer.append("DEBUG", tag, message + "\n" + stackTraceToString(exception));
        delegate.debug(tag, message, exception);
    }

    private static String stackTraceToString(Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString());
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append("\n  at ").append(e.toString());
        }
        return sb.toString();
    }
}
