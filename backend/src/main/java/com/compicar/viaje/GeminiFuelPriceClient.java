package com.compicar.viaje;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class GeminiFuelPriceClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.endpoint}")
    private String endpoint;

    public String pedirEstimacionJson(String prompt) {
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
            generationConfig.put("responseMimeType", "application/json");

            String requestBody = requestRoot.toString();

            String url = endpoint + "/" + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini devolvio error HTTP");
            }

            JsonNode responseRoot = objectMapper.readTree(response.body());
            JsonNode text = responseRoot.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            
            if (text.isMissingNode() || text.asText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini devolvio respuesta vacia");
            }

            return text.asText();
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error parseando respuesta de Gemini");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Llamada a Gemini interrumpida");
        }
    }
}
