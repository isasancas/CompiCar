package com.compicar.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class DetalleViajeOfrecidoE2ETest extends BaseE2ETest {

    private static final String SEEDED_LOGIN_EMAIL = "selenium@compicar.test";
    private static final String SEEDED_LOGIN_PASSWORD = "Selenium123!";

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void abrirDetalleDesdeMisViajes_muestraDatosDelTrayecto() throws Exception {
        loginConUsuarioSeeded();
        String token = obtenerTokenLocalStorage();

        String sufijo = String.valueOf(System.currentTimeMillis());
        long vehiculoId = crearVehiculoPorApi(token, sufijo);

        String origen = "Origen E2E " + sufijo;
        String destino = "Destino E2E " + sufijo;
        String slug = crearViajePorApiYObtenerSlug(token, vehiculoId, origen, destino);

        driver.get(baseUrl + "/mis-viajes");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h1[normalize-space()='Mis viajes']")
        ));

        By botonVerDetalle = By.xpath(
            "//div[contains(@class,'rounded-2xl')][.//*[contains(normalize-space(),'" + origen + "')]]" +
            "//button[normalize-space()='Ver detalle']"
        );

        WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(botonVerDetalle));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn);

        wait.until(ExpectedConditions.urlContains("/viajes/"));
        assertTrue(driver.getCurrentUrl().contains("/viajes/" + slug), "Se esperaba navegar al slug del viaje creado");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Ruta del viaje']")
        ));

        assertTrue(driver.getPageSource().contains(origen), "Debe mostrarse el origen en el detalle");
        assertTrue(driver.getPageSource().contains(destino), "Debe mostrarse el destino en el detalle");
    }

    @Test
    void abrirDetalleDirectamentePorSlug_muestraSeccionesPrincipales() throws Exception {
        loginConUsuarioSeeded();
        String token = obtenerTokenLocalStorage();

        String sufijo = String.valueOf(System.currentTimeMillis());
        long vehiculoId = crearVehiculoPorApi(token, sufijo);

        String origen = "Origen Directo " + sufijo;
        String destino = "Destino Directo " + sufijo;
        String slug = crearViajePorApiYObtenerSlug(token, vehiculoId, origen, destino);

        driver.get(baseUrl + "/viajes/" + slug);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Ruta del viaje']")
        ));

        assertTrue(driver.getPageSource().contains("Origen"), "Debe verse la sección Origen");
        assertTrue(driver.getPageSource().contains("Destino"), "Debe verse la sección Destino");
        assertTrue(driver.getPageSource().contains("Plazas Disponibles"), "Debe verse la tarjeta de plazas");
        assertTrue(driver.getPageSource().contains("Precio"), "Debe verse la tarjeta de precio");
        assertTrue(driver.getPageSource().contains(origen), "Debe mostrarse el origen creado");
        assertTrue(driver.getPageSource().contains(destino), "Debe mostrarse el destino creado");
    }

    private void loginConUsuarioSeeded() {
        driver.get(baseUrl + "/inicio-sesion");

        WebElement emailInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='input-email']"))
        );
        WebElement passwordInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='input-password']"))
        );
        WebElement loginButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='btn-login']"))
        );

        emailInput.clear();
        emailInput.sendKeys(SEEDED_LOGIN_EMAIL);
        passwordInput.clear();
        passwordInput.sendKeys(SEEDED_LOGIN_PASSWORD);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/perfil"),
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='perfil-page']"))
        ));
    }

    private String obtenerTokenLocalStorage() {
        String token = (String) ((JavascriptExecutor) driver)
            .executeScript("return window.localStorage.getItem('token');");
        assertTrue(token != null && !token.isBlank(), "Se esperaba token tras login");
        return token;
    }

    private long crearVehiculoPorApi(String token, String sufijo) throws Exception {
        String apiBase = System.getenv().getOrDefault("E2E_API_BASE_URL", "http://localhost:8080");
        String matricula = String.format(Locale.ROOT, "%04dABC", Math.floorMod(System.currentTimeMillis(), 10000L));

        String body = mapper.writeValueAsString(new VehiculoReq(
            matricula, "Seat", "Ibiza-" + sufijo, 4, 5.2, 2020, "COCHE"
        ));

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/vehiculos"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        assertTrue(resp.statusCode() == 201, "Crear vehículo API debe devolver 201, devolvió " + resp.statusCode());

        JsonNode json = mapper.readTree(resp.body());
        return json.path("id").asLong();
    }

    private String crearViajePorApiYObtenerSlug(String token, long vehiculoId, String origen, String destino) throws Exception {
        String apiBase = System.getenv().getOrDefault("E2E_API_BASE_URL", "http://localhost:8080");

        String fechaHora = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0).toString();

        String viajeBody = """
            {
              "fechaHoraSalida": "%s",
              "estado": "PENDIENTE",
              "plazasDisponibles": 3,
              "precio": 10.50,
              "vehiculo": { "id": %d },
              "paradas": [
                { "localizacion": "%s", "tipo": "ORIGEN", "orden": 1, "fechaHora": "%s" },
                { "localizacion": "%s", "tipo": "DESTINO", "orden": 2, "fechaHora": "%s" }
              ]
            }
            """.formatted(fechaHora, vehiculoId, escapeJson(origen), fechaHora, escapeJson(destino), fechaHora);

        HttpRequest crearReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/viajes/crear"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(viajeBody))
            .build();

        HttpResponse<String> crearResp = http.send(crearReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(crearResp.statusCode() == 200, "Crear viaje API debe devolver 200, devolvió " + crearResp.statusCode());

        HttpRequest misViajesReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/viajes/mis-viajes"))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();

        HttpResponse<String> misViajesResp = http.send(misViajesReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(misViajesResp.statusCode() == 200, "Mis viajes API debe devolver 200, devolvió " + misViajesResp.statusCode());

        JsonNode arr = mapper.readTree(misViajesResp.body());
        for (JsonNode v : arr) {
            JsonNode paradas = v.path("paradas");
            for (JsonNode p : paradas) {
                if (origen.equals(p.path("localizacion").asText())) {
                    String slug = v.path("slug").asText();
                    if (!slug.isBlank()) return slug;
                }
            }
        }

        throw new IllegalStateException("No se encontró el viaje recién creado en /api/viajes/mis-viajes");
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record VehiculoReq(
        String matricula,
        String marca,
        String modelo,
        int plazas,
        double consumo,
        int anio,
        String tipo
    ) {}
}
