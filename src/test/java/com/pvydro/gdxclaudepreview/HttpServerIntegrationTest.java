package com.pvydro.gdxclaudepreview;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class HttpServerIntegrationTest {

    private LivePreviewHttpServer server;
    private int port;
    private FramebufferCapture capture;
    private InputBridge inputBridge;

    @Before
    public void setUp() throws Exception {
        port = findFreePort();
        capture = new FramebufferCapture();
        inputBridge = new InputBridge();
        server = new LivePreviewHttpServer(port, capture, inputBridge);
        server.start();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void rootReturnsHtml() throws Exception {
        HttpURLConnection conn = get("/");
        assertEquals(200, conn.getResponseCode());
        String body = readBody(conn);
        assertTrue(body.contains("<!DOCTYPE html>"));
        assertTrue(body.contains("LibGDX Live Preview"));
        assertTrue(body.contains("role=\"application\""));
    }

    @Test
    public void infoBeforeGameReady() throws Exception {
        HttpURLConnection conn = get("/info");
        assertEquals(200, conn.getResponseCode());
        String body = readBody(conn);
        assertTrue(body.contains("\"ready\":false"));
    }

    @Test
    public void screenshotBeforeGameReady() throws Exception {
        HttpURLConnection conn = get("/screenshot");
        assertEquals(503, conn.getResponseCode());
    }

    @Test
    public void screenshotAfterGameReadyButNoCapture() throws Exception {
        server.setGameReady(true);
        HttpURLConnection conn = get("/screenshot");
        // Will timeout waiting for GL thread capture and return 503
        assertEquals(503, conn.getResponseCode());
    }

    @Test
    public void postInput() throws Exception {
        HttpURLConnection conn = post("/input", "{\"type\":\"click\",\"x\":100,\"y\":200,\"button\":0}");
        assertEquals(200, conn.getResponseCode());
        String body = readBody(conn);
        assertTrue(body.contains("\"ok\":true"));
    }

    @Test
    public void postKey() throws Exception {
        HttpURLConnection conn = post("/key", "{\"type\":\"keydown\",\"key\":\"a\",\"code\":\"KeyA\"}");
        assertEquals(200, conn.getResponseCode());
        String body = readBody(conn);
        assertTrue(body.contains("\"ok\":true"));
    }

    @Test
    public void postKeyUnmapped() throws Exception {
        HttpURLConnection conn = post("/key", "{\"type\":\"keydown\",\"key\":\"?\",\"code\":\"UnknownKey\"}");
        assertEquals(200, conn.getResponseCode());
        String body = readBody(conn);
        assertTrue(body.contains("\"mapped\":false"));
    }

    @Test
    public void notFoundForUnknownPath() throws Exception {
        HttpURLConnection conn = get("/nonexistent");
        assertEquals(404, conn.getResponseCode());
    }

    @Test
    public void corsHeadersPresent() throws Exception {
        HttpURLConnection conn = get("/info");
        assertEquals("*", conn.getHeaderField("Access-Control-Allow-Origin"));
    }

    private HttpURLConnection get(String path) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(3000);
        return conn;
    }

    private HttpURLConnection post(String path, String body) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(2000);
        conn.setReadTimeout(3000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private String readBody(HttpURLConnection conn) throws Exception {
        InputStream is = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }

    private static int findFreePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
