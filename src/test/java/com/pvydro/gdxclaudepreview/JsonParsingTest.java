package com.pvydro.gdxclaudepreview;

import org.junit.Test;

import static org.junit.Assert.*;

public class JsonParsingTest {

    @Test
    public void extractIntNormalValue() {
        String json = "{\"x\":150,\"y\":200}";
        assertEquals(150, LivePreviewHttpServer.extractInt(json, "x"));
        assertEquals(200, LivePreviewHttpServer.extractInt(json, "y"));
    }

    @Test
    public void extractIntNegativeValue() {
        String json = "{\"x\":-42}";
        assertEquals(-42, LivePreviewHttpServer.extractInt(json, "x"));
    }

    @Test
    public void extractIntMissingKeyReturnsZero() {
        String json = "{\"x\":10}";
        assertEquals(0, LivePreviewHttpServer.extractInt(json, "missing"));
    }

    @Test
    public void extractIntWithSpaces() {
        String json = "{\"x\" : 99 }";
        assertEquals(99, LivePreviewHttpServer.extractInt(json, "x"));
    }

    @Test
    public void extractStringNormalValue() {
        String json = "{\"type\":\"keydown\",\"code\":\"KeyA\"}";
        assertEquals("keydown", LivePreviewHttpServer.extractString(json, "type"));
        assertEquals("KeyA", LivePreviewHttpServer.extractString(json, "code"));
    }

    @Test
    public void extractStringMissingKeyReturnsEmpty() {
        String json = "{\"type\":\"keydown\"}";
        assertEquals("", LivePreviewHttpServer.extractString(json, "missing"));
    }

    @Test
    public void extractIntZeroValue() {
        String json = "{\"button\":0}";
        assertEquals(0, LivePreviewHttpServer.extractInt(json, "button"));
    }

    @Test
    public void extractIntFromComplexJson() {
        String json = "{\"type\":\"click\",\"x\":320,\"y\":240,\"button\":2}";
        assertEquals(320, LivePreviewHttpServer.extractInt(json, "x"));
        assertEquals(240, LivePreviewHttpServer.extractInt(json, "y"));
        assertEquals(2, LivePreviewHttpServer.extractInt(json, "button"));
    }
}
