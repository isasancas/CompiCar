package com.compicar.viaje;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class CalculoPrecioIATest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void pedirEstimacionJson_apiKeyVacia_lanzaBadGateway() {
        CalculoPrecioIA sut = new CalculoPrecioIA();
        ReflectionTestUtils.setField(sut, "apiKey", "");
        ReflectionTestUtils.setField(sut, "model", "test-model");
        ReflectionTestUtils.setField(sut, "endpoint", "http://localhost:9999");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> sut.pedirEstimacionJson("prompt"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        assertEquals("Gemini API key no configurada", ex.getReason());
    }

    @Test
    void pedirEstimacionJson_respuestaOk_devuelveText() throws Exception {
        server = levantarServidor(200,
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"precio_combustible_litro\\\":1.65,\\\"detalle\\\":\\\"ok\\\"}\"}]}}]}");

        CalculoPrecioIA sut = new CalculoPrecioIA();
        ReflectionTestUtils.setField(sut, "apiKey", "abc");
        ReflectionTestUtils.setField(sut, "model", "test-model");
        ReflectionTestUtils.setField(sut, "endpoint", "http://localhost:" + server.getAddress().getPort());

        String result = sut.pedirEstimacionJson("prompt");

        assertEquals("{\"precio_combustible_litro\":1.65,\"detalle\":\"ok\"}", result);
    }

    @Test
    void pedirEstimacionJson_httpError_lanzaBadGateway() throws Exception {
        server = levantarServidor(500, "{\"error\":\"boom\"}");

        CalculoPrecioIA sut = new CalculoPrecioIA();
        ReflectionTestUtils.setField(sut, "apiKey", "abc");
        ReflectionTestUtils.setField(sut, "model", "test-model");
        ReflectionTestUtils.setField(sut, "endpoint", "http://localhost:" + server.getAddress().getPort());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> sut.pedirEstimacionJson("prompt"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        assertEquals("Gemini devolvio error HTTP", ex.getReason());
    }

    @Test
    void pedirEstimacionJson_textVacio_lanzaBadGateway() throws Exception {
        server = levantarServidor(200,
            "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"\"}]}}]}");

        CalculoPrecioIA sut = new CalculoPrecioIA();
        ReflectionTestUtils.setField(sut, "apiKey", "abc");
        ReflectionTestUtils.setField(sut, "model", "test-model");
        ReflectionTestUtils.setField(sut, "endpoint", "http://localhost:" + server.getAddress().getPort());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> sut.pedirEstimacionJson("prompt"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        assertEquals("Gemini devolvio respuesta vacia", ex.getReason());
    }

    @Test
    void pedirEstimacionJson_jsonInvalido_lanzaBadGatewayParseo() throws Exception {
        server = levantarServidor(200, "{not-json");

        CalculoPrecioIA sut = new CalculoPrecioIA();
        ReflectionTestUtils.setField(sut, "apiKey", "abc");
        ReflectionTestUtils.setField(sut, "model", "test-model");
        ReflectionTestUtils.setField(sut, "endpoint", "http://localhost:" + server.getAddress().getPort());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
            () -> sut.pedirEstimacionJson("prompt"));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
        assertEquals("Error parseando respuesta de Gemini", ex.getReason());
    }

    private HttpServer levantarServidor(int status, String responseBody) throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress(0), 0);
        s.createContext("/test-model:generateContent", exchange -> responder(exchange, status, responseBody));
        s.start();
        return s;
    }

    private void responder(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
