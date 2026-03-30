package com.pvydro.gdxclaudepreview;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.pvydro.gdxclaudepreview.internal.NanoHTTPD;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class LivePreviewHttpServer extends NanoHTTPD {

    private final FramebufferCapture capture;
    private final InputBridge inputBridge;
    private final LogBuffer logBuffer;
    private volatile boolean gameReady = false;

    public LivePreviewHttpServer(int port, FramebufferCapture capture, InputBridge inputBridge, LogBuffer logBuffer) {
        super(port);
        this.capture = capture;
        this.inputBridge = inputBridge;
        this.logBuffer = logBuffer;
    }

    public void setGameReady(boolean ready) {
        this.gameReady = ready;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        // CORS headers for all responses
        Response response;

        try {
            if (Method.GET.equals(method) && "/".equals(uri)) {
                response = serveHtml();
            } else if (Method.GET.equals(method) && "/screenshot".equals(uri)) {
                response = serveScreenshot();
            } else if (Method.POST.equals(method) && "/input".equals(uri)) {
                response = handleInput(session);
            } else if (Method.POST.equals(method) && "/key".equals(uri)) {
                response = handleKey(session);
            } else if (Method.GET.equals(method) && "/info".equals(uri)) {
                response = serveInfo();
            } else if (Method.GET.equals(method) && "/logs".equals(uri)) {
                response = serveLogs(session);
            } else if (Method.OPTIONS.equals(method)) {
                response = newFixedLengthResponse(Response.Status.OK, "text/plain", "");
            } else {
                response = newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
            }
        } catch (Exception e) {
            response = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        }

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
        return response;
    }

    private Response serveScreenshot() {
        if (!gameReady) {
            return newFixedLengthResponse(Response.Status.lookup(503), "text/plain", "Game not ready");
        }

        byte[] jpeg = capture.getLatestScreenshot();
        if (jpeg == null) {
            return newFixedLengthResponse(Response.Status.lookup(503), "text/plain", "No screenshot available");
        }

        Response resp = newFixedLengthResponse(Response.Status.OK, "image/jpeg",
                new ByteArrayInputStream(jpeg), jpeg.length);
        resp.addHeader("Cache-Control", "no-store");
        return resp;
    }

    private Response handleInput(IHTTPSession session) {
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String json = body.get("postData");
            if (json == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No body");
            }

            String type = extractString(json, "type");
            int x = extractInt(json, "x");
            int y = extractInt(json, "y");
            int button = extractInt(json, "button");
            int gdxButton = button == 2 ? Input.Buttons.RIGHT : Input.Buttons.LEFT;

            if ("mousemove".equals(type)) {
                inputBridge.enqueue(InputBridge.InputEvent.mouseMove(x, y));
            } else if ("mousedown".equals(type)) {
                inputBridge.enqueue(InputBridge.InputEvent.click(x, y, gdxButton));
            } else if ("mouseup".equals(type)) {
                inputBridge.enqueue(InputBridge.InputEvent.clickUp(x, y, gdxButton));
            } else {
                // Legacy click: send touchDown + touchUp pair
                inputBridge.enqueue(InputBridge.InputEvent.click(x, y, gdxButton));
                inputBridge.enqueue(InputBridge.InputEvent.clickUp(x, y, gdxButton));
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"ok\":true}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", e.getMessage());
        }
    }

    private Response handleKey(IHTTPSession session) {
        try {
            Map<String, String> body = new HashMap<>();
            session.parseBody(body);
            String json = body.get("postData");
            if (json == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "No body");
            }

            String type = extractString(json, "type");
            String code = extractString(json, "code");

            int keyCode = KeyMapping.toLibGdx(code);
            if (keyCode == -1) {
                return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"ok\":true,\"mapped\":false}");
            }

            if ("keydown".equals(type)) {
                inputBridge.enqueue(InputBridge.InputEvent.keyDown(keyCode));
            } else if ("keyup".equals(type)) {
                inputBridge.enqueue(InputBridge.InputEvent.keyUp(keyCode));
            }

            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"ok\":true}");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", e.getMessage());
        }
    }

    private Response serveInfo() {
        int w = gameReady ? Gdx.graphics.getWidth() : 0;
        int h = gameReady ? Gdx.graphics.getHeight() : 0;
        int fps = gameReady ? Gdx.graphics.getFramesPerSecond() : 0;
        String json = "{\"width\":" + w + ",\"height\":" + h + ",\"fps\":" + fps + ",\"ready\":" + gameReady + "}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response serveLogs(IHTTPSession session) {
        String sinceParam = session.getParms().get("since");
        int since = 0;
        if (sinceParam != null) {
            try { since = Integer.parseInt(sinceParam); } catch (NumberFormatException ignored) {}
        }
        String json = logBuffer.toJsonSince(since);
        Response resp = newFixedLengthResponse(Response.Status.OK, "application/json", json);
        resp.addHeader("Cache-Control", "no-store");
        return resp;
    }

    private Response serveHtml() {
        return newFixedLengthResponse(Response.Status.OK, "text/html", HTML_PAGE);
    }

    // Minimal JSON parsing — avoids adding a JSON library dependency
    static int extractInt(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return 0;
        idx = json.indexOf(':', idx) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = idx; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '-' || (c >= '0' && c <= '9')) sb.append(c);
            else if (sb.length() > 0) break;
        }
        return sb.length() > 0 ? Integer.parseInt(sb.toString()) : 0;
    }

    static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return "";
        idx = json.indexOf(':', idx) + 1;
        int start = json.indexOf('"', idx) + 1;
        int end = json.indexOf('"', start);
        return (start > 0 && end > start) ? json.substring(start, end) : "";
    }

    private static final String HTML_PAGE = "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "<meta charset=\"UTF-8\">\n"
            + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
            + "<title>LibGDX Live Preview</title>\n"
            + "<style>\n"
            + "* { margin: 0; padding: 0; box-sizing: border-box; }\n"
            + "body { background: #1a1a2e; overflow: hidden; display: flex; align-items: center; justify-content: center; height: 100vh; }\n"
            + "#game { width: 100vw; height: 100vh; display: block; cursor: crosshair; object-fit: contain; }\n"
            + "#status { position: fixed; top: 8px; right: 8px; color: #0f0; font: 12px monospace; background: rgba(0,0,0,0.7); padding: 4px 8px; border-radius: 4px; z-index: 10; }\n"
            + "#status.error { color: #f44; }\n"
            + "</style>\n"
            + "</head>\n"
            + "<body role=\"application\">\n"
            + "<img id=\"game\" alt=\"LibGDX Game Preview\" />\n"
            + "<div id=\"status\">Connecting...</div>\n"
            + "<script>\n"
            + "(function() {\n"
            + "  var img = document.getElementById('game');\n"
            + "  var status = document.getElementById('status');\n"
            + "  var gameW = 0, gameH = 0;\n"
            + "  var connected = false;\n"
            + "  var pollInterval = 100;\n"
            + "\n"
            + "  function updateInfo() {\n"
            + "    fetch('/info').then(function(r) { return r.json(); }).then(function(d) {\n"
            + "      gameW = d.width; gameH = d.height;\n"
            + "      if (d.ready) {\n"
            + "        status.className = '';\n"
            + "        status.textContent = gameW + 'x' + gameH + ' ' + d.fps + 'fps';\n"
            + "      } else {\n"
            + "        status.textContent = 'Waiting for game...';\n"
            + "      }\n"
            + "      connected = true;\n"
            + "    }).catch(function() {\n"
            + "      status.className = 'error';\n"
            + "      status.textContent = 'Disconnected';\n"
            + "      connected = false;\n"
            + "    });\n"
            + "  }\n"
            + "\n"
            + "  function poll() {\n"
            + "    var t = Date.now();\n"
            + "    img.onload = function() {\n"
            + "      connected = true;\n"
            + "      setTimeout(poll, pollInterval);\n"
            + "    };\n"
            + "    img.onerror = function() {\n"
            + "      setTimeout(poll, 1000);\n"
            + "    };\n"
            + "    img.src = '/screenshot?t=' + t;\n"
            + "  }\n"
            + "\n"
            + "  function gameCoords(e) {\n"
            + "    var rect = img.getBoundingClientRect();\n"
            + "    return {\n"
            + "      x: Math.round((e.clientX - rect.left) * (gameW / rect.width)),\n"
            + "      y: Math.round((e.clientY - rect.top) * (gameH / rect.height))\n"
            + "    };\n"
            + "  }\n"
            + "\n"
            + "  img.addEventListener('mousedown', function(e) {\n"
            + "    e.preventDefault();\n"
            + "    if (!gameW || !gameH) return;\n"
            + "    var c = gameCoords(e);\n"
            + "    fetch('/input', {\n"
            + "      method: 'POST',\n"
            + "      headers: {'Content-Type': 'application/json'},\n"
            + "      body: JSON.stringify({type:'mousedown', x:c.x, y:c.y, button:e.button})\n"
            + "    });\n"
            + "  });\n"
            + "\n"
            + "  document.addEventListener('mouseup', function(e) {\n"
            + "    if (!gameW || !gameH) return;\n"
            + "    var c = gameCoords(e);\n"
            + "    fetch('/input', {\n"
            + "      method: 'POST',\n"
            + "      headers: {'Content-Type': 'application/json'},\n"
            + "      body: JSON.stringify({type:'mouseup', x:c.x, y:c.y, button:e.button})\n"
            + "    });\n"
            + "  });\n"
            + "\n"
            + "  var lastMoveTime = 0;\n"
            + "  img.addEventListener('mousemove', function(e) {\n"
            + "    var now = Date.now();\n"
            + "    if (now - lastMoveTime < 16) return;\n"
            + "    lastMoveTime = now;\n"
            + "    if (!gameW || !gameH) return;\n"
            + "    var c = gameCoords(e);\n"
            + "    fetch('/input', {\n"
            + "      method: 'POST',\n"
            + "      headers: {'Content-Type': 'application/json'},\n"
            + "      body: JSON.stringify({type:'mousemove', x:c.x, y:c.y})\n"
            + "    });\n"
            + "  });\n"
            + "\n"
            + "  img.addEventListener('contextmenu', function(e) { e.preventDefault(); });\n"
            + "\n"
            + "  document.addEventListener('keydown', function(e) {\n"
            + "    e.preventDefault();\n"
            + "    fetch('/key', {\n"
            + "      method: 'POST',\n"
            + "      headers: {'Content-Type': 'application/json'},\n"
            + "      body: JSON.stringify({type:'keydown', key:e.key, code:e.code})\n"
            + "    });\n"
            + "  });\n"
            + "\n"
            + "  document.addEventListener('keyup', function(e) {\n"
            + "    e.preventDefault();\n"
            + "    fetch('/key', {\n"
            + "      method: 'POST',\n"
            + "      headers: {'Content-Type': 'application/json'},\n"
            + "      body: JSON.stringify({type:'keyup', key:e.key, code:e.code})\n"
            + "    });\n"
            + "  });\n"
            + "\n"
            + "  var logSeq = 0;\n"
            + "  function pollLogs() {\n"
            + "    fetch('/logs?since=' + logSeq).then(function(r) { return r.json(); }).then(function(d) {\n"
            + "      logSeq = d.seq;\n"
            + "      d.logs.forEach(function(entry) {\n"
            + "        if (entry.level === 'ERROR') console.error('[GDX] ' + entry.msg);\n"
            + "        else if (entry.level === 'DEBUG') console.debug('[GDX] ' + entry.msg);\n"
            + "        else console.log('[GDX] ' + entry.msg);\n"
            + "      });\n"
            + "      setTimeout(pollLogs, 500);\n"
            + "    }).catch(function() { setTimeout(pollLogs, 1000); });\n"
            + "  }\n"
            + "\n"
            + "  setInterval(updateInfo, 2000);\n"
            + "  updateInfo();\n"
            + "  poll();\n"
            + "})();\n"
            + "</script>\n"
            + "</body>\n"
            + "</html>";
}
