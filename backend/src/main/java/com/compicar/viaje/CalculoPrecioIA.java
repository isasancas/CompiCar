package com.compicar.viaje;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class CalculoPrecioIA {

    private static final Logger logger = LoggerFactory.getLogger(CalculoPrecioIA.class);
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.endpoint}")
    private String endpoint;

    public String pedirEstimacionJson(String prompt) {
        return pedirEstimacionJson(prompt, false);
    }

    public String pedirEstimacionJsonConBusqueda(String prompt) {
        return pedirEstimacionJson(prompt, true);
    }

    private String pedirEstimacionJson(String prompt, boolean usarBusqueda) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini API key no configurada");
        }

        try {
            ObjectNode requestRoot = objectMapper.createObjectNode();

            ArrayNode contents = requestRoot.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            parts.addObject().put("text", prompt);

            ObjectNode generationConfig = requestRoot.putObject("generationConfig");
            generationConfig.put("temperature", 0.1);
            // Gemini puede rechazar JSON mode cuando la petición usa Google Search grounding.
            if (!usarBusqueda) {
                generationConfig.put("responseMimeType", "application/json");
            }

            if (usarBusqueda) {
                ArrayNode tools = requestRoot.putArray("tools");
                tools.addObject().putObject("google_search");
            }

            String requestBody = requestRoot.toString();

            String url = endpoint + "/" + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                String detalle = response.body() == null ? "sin cuerpo" : response.body();
                if (detalle.length() > 2000) {
                    detalle = detalle.substring(0, 2000);
                }
                logger.warn("Gemini devolvio HTTP {}: {}", response.statusCode(), detalle);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini devolvio error HTTP");
            }

            JsonNode responseRoot = objectMapper.readTree(response.body());
            JsonNode text = responseRoot.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini devolvio respuesta vacia");
            }

            return limpiarJson(text.asText());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error parseando respuesta de Gemini");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Llamada a Gemini interrumpida");
        }
    }

    private String limpiarJson(String respuesta) {
        String limpia = respuesta.trim();
        if (limpia.startsWith("```") && limpia.endsWith("```")) {
            int inicioContenido = limpia.indexOf('\n');
            if (inicioContenido >= 0) {
                limpia = limpia.substring(inicioContenido + 1, limpia.length() - 3).trim();
            }
        }
        return limpia;
    }
}
