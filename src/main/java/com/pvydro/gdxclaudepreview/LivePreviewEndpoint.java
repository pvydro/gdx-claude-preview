package com.pvydro.gdxclaudepreview;

import java.util.Map;

/**
 * Functional interface for custom HTTP endpoints registered with LivePreview.
 * Implementations are called on NanoHTTPD's server thread — keep handlers fast
 * and use volatile/atomic fields for any data shared with the GL thread.
 */
@FunctionalInterface
public interface LivePreviewEndpoint {
    /**
     * Handle an HTTP GET request to a custom endpoint.
     *
     * @param uri    the request URI (e.g. "/game-state")
     * @param params query parameters from the request
     * @return a response body string (typically JSON)
     */
    String handle(String uri, Map<String, String> params);
}
