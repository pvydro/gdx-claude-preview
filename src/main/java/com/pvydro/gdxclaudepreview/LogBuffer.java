package com.pvydro.gdxclaudepreview;

/**
 * Fixed-size ring buffer for log messages. Lock-free for single-writer (GL thread)
 * with snapshot reads from HTTP threads.
 */
public class LogBuffer {

    private final String[] entries;
    private final String[] levels;
    private volatile int writeIndex = 0;
    private volatile int totalWritten = 0;

    public LogBuffer(int capacity) {
        this.entries = new String[capacity];
        this.levels = new String[capacity];
    }

    /**
     * Append a log entry. Called from the GL thread (via Gdx.app.log/error/debug).
     */
    public void append(String level, String tag, String message) {
        int idx = writeIndex % entries.length;
        entries[idx] = "[" + tag + "] " + message;
        levels[idx] = level;
        writeIndex = idx + 1 == entries.length ? 0 : idx + 1;
        totalWritten++;
    }

    /**
     * Returns all log entries added since the given sequence number as a JSON array string.
     * Also returns the new sequence number for the next poll.
     */
    public String toJsonSince(int sinceSequence) {
        int total = totalWritten;
        int available = Math.min(total - sinceSequence, entries.length);
        if (available <= 0) {
            return "{\"seq\":" + total + ",\"logs\":[]}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"seq\":").append(total).append(",\"logs\":[");

        int start = (writeIndex - available + entries.length) % entries.length;
        for (int i = 0; i < available; i++) {
            int idx = (start + i) % entries.length;
            if (i > 0) sb.append(',');
            sb.append("{\"level\":\"");
            sb.append(levels[idx] != null ? levels[idx] : "LOG");
            sb.append("\",\"msg\":");
            sb.append(escapeJson(entries[idx] != null ? entries[idx] : ""));
            sb.append('}');
        }

        sb.append("]}");
        return sb.toString();
    }

    public int getTotalWritten() {
        return totalWritten;
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        sb.append(String.format("%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
