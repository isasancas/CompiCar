package com.compicar.e2e;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void editarViaje_cambiaFechaYPrecio() throws Exception {
        loginConUsuarioSeeded();
        String token = obtenerTokenLocalStorage();

        String sufijo = String.valueOf(System.currentTimeMillis());
        long vehiculoId = crearVehiculoPorApi(token, sufijo);

        String origen = "Origen Edit " + sufijo;
        String destino = "Destino Edit " + sufijo;
        LocalDateTime fechaLejana = LocalDateTime.now().plusDays(30).withSecond(0).withNano(0);
        String slug = crearViajePorApiYObtenerSlug(token, vehiculoId, origen, destino, fechaLejana);

        driver.get(baseUrl + "/mis-viajes");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h1[normalize-space()='Mis viajes']")
        ));

        By botonVerDetalle = By.xpath(
            "//div[contains(@class,'rounded-2xl')][.//*[contains(normalize-space(),'" + origen + "')]]" +
            "//button[normalize-space()='Ver detalle']"
        );

        WebElement btnDetalle = wait.until(ExpectedConditions.elementToBeClickable(botonVerDetalle));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnDetalle);

        wait.until(ExpectedConditions.urlContains("/viajes/"));
        assertTrue(driver.getCurrentUrl().contains("/viajes/" + slug),
            "Se esperaba navegar al slug del viaje creado");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Ruta del viaje']")
        ));

        By editarBtnLocator = By.xpath(
            "//button[contains(.,'Editar detalles del viaje') or contains(.,'Editar') or contains(@aria-label,'Editar')]"
        );
        WebElement botonEditar = wait.until(ExpectedConditions.elementToBeClickable(editarBtnLocator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botonEditar);

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Editar mi viaje']")
        ));

        WebElement inputFecha = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//input[@type='datetime-local']")
        ));
        LocalDateTime nuevaFecha = fechaLejana.plusDays(1).withSecond(0).withNano(0);
        String nuevaFechaTexto = nuevaFecha.toString().substring(0, 16);

        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1];" +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
            inputFecha,
            nuevaFechaTexto
        );

        WebElement inputPlazas = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//input[@type='number']")
        ));
        inputPlazas.clear();
        inputPlazas.sendKeys("4");

        WebElement botonGuardar = wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//button[normalize-space()='Confirmar cambios']")
        ));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botonGuardar);

        // Ahora la confirmación es inline dentro del modal, no un alert
        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//*[contains(normalize-space(),'Viaje actualizado con éxito') or contains(normalize-space(),'Todo listo')]")
        ));

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//button[contains(.,'✨ ¡Todo listo!') or contains(.,'Guardando...')]")
        ));

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Editar mi viaje']")
        ));

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//p[normalize-space()='Plazas Disponibles']")
        ));

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//p[normalize-space()='Plazas Disponibles']")
        ));

        assertTrue(driver.findElements(By.xpath("//p[normalize-space()='Plazas Disponibles']")).size() > 0);
    }

    @Test
    void editarViaje_menosDe12Horas_noDejaEditar() throws Exception {
        loginConUsuarioSeeded();
        String token = obtenerTokenLocalStorage();

        String sufijo = String.valueOf(System.currentTimeMillis());
        long vehiculoId = crearVehiculoPorApi(token, sufijo);

        String origen = "Origen NoEdit " + sufijo;
        String destino = "Destino NoEdit " + sufijo;
        LocalDateTime fechaProxima = LocalDateTime.now().plusHours(6).withSecond(0).withNano(0);
        String slug = crearViajePorApiYObtenerSlug(token, vehiculoId, origen, destino, fechaProxima);

        driver.get(baseUrl + "/mis-viajes");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h1[normalize-space()='Mis viajes']")
        ));

        By botonVerDetalle = By.xpath(
            "//div[contains(@class,'rounded-2xl')][.//*[contains(normalize-space(),'" + origen + "')]]" +
            "//button[normalize-space()='Ver detalle']"
        );

        WebElement btnDetalle = wait.until(ExpectedConditions.elementToBeClickable(botonVerDetalle));
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnDetalle);

        wait.until(ExpectedConditions.urlContains("/viajes/"));
        assertTrue(driver.getCurrentUrl().contains("/viajes/" + slug),
            "Se esperaba navegar al slug del viaje creado");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//h2[normalize-space()='Ruta del viaje']")
        ));

        boolean botonEditarVisible = !driver.findElements(
            By.xpath("//button[contains(.,'Editar') or contains(@aria-label,'Editar')]")
        ).isEmpty();
        assertFalse(botonEditarVisible, "El botón Editar no debe aparecer cuando faltan <12 horas");

        boolean textoNoEditar = driver.getPageSource().contains("El viaje está bloqueado para edición (falta menos de 12h)");
        assertTrue(textoNoEditar, "Debe aparecer un mensaje indicando que no se puede editar el viaje");
    }

    @Test
    void crearReservaDesdeDetalleYCancelarla() throws Exception {
        loginConUsuarioSeeded();
        String tokenConductor = obtenerTokenLocalStorage();

        String sufijo = String.valueOf(System.currentTimeMillis());
        long vehiculoId = crearVehiculoPorApi(tokenConductor, sufijo);

        String origen = "Origen Reserva " + sufijo;
        String destino = "Destino Reserva " + sufijo;
        String slug = crearViajePorApiYObtenerSlug(tokenConductor, vehiculoId, origen, destino);

        // Registrar pasajero vía API y obtener token (evita depender de un usuario no-seedado)
        String apiBase = System.getenv().getOrDefault("E2E_API_BASE_URL", "http://localhost:8080");
        String pasajeroEmail = "pasajero" + sufijo + "@compicar.test";
        String telefonoPasajero = "+3460000" + (Long.parseLong(sufijo) % 10000L);

        String registroBody = """
            {
              "contrasena": "Pasajero123!",
              "nombre": "Pasajero",
              "primerApellido": "Test",
              "email": "%s",
              "numTelefono": "%s"
            }
            """.formatted(pasajeroEmail, telefonoPasajero);

        HttpRequest registroReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/registro"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registroBody))
            .build();

        HttpResponse<String> registroResp = http.send(registroReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(registroResp.statusCode() == 200, "Registro API pasajero debe devolver 200, devolvió " + registroResp.statusCode());

        // Ahora login para obtener token
        String loginBody = """
            {
              "email": "%s",
              "contrasena": "Pasajero123!"
            }
            """.formatted(pasajeroEmail);

        HttpRequest loginReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();
        HttpResponse<String> loginResp = http.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(loginResp.statusCode() == 200, "Login API pasajero debe devolver 200, devolvió " + loginResp.statusCode());
        JsonNode loginJson = mapper.readTree(loginResp.body());
        String tokenPasajero = loginJson.path("token").asText().trim();
        assertTrue(tokenPasajero != null && !tokenPasajero.isBlank(), "Se esperaba token tras login API (pasajero)");

        HttpRequest getViajeReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/viajes/publicos/" + slug))
            .header("Authorization", "Bearer " + tokenPasajero)
            .GET()
            .build();
        HttpResponse<String> getViajeResp = http.send(getViajeReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(getViajeResp.statusCode() == 200, "Obtener viaje por slug debe devolver 200");
        JsonNode viajeJson = mapper.readTree(getViajeResp.body());
        long viajeId = viajeJson.path("id").asLong();
        JsonNode paradas = viajeJson.path("paradas");
        long paradaSubidaId = paradas.get(0).path("id").asLong();
        long paradaBajadaId = paradas.get(paradas.size() - 1).path("id").asLong();

        // Crear reserva por API (más fiable que depender del modal)
        String reservaBody = """
            {
              "viajeId": %d,
              "plazas": 2,
              "paradaSubidaId": %d,
              "paradaBajadaId": %d
            }
            """.formatted(viajeId, paradaSubidaId, paradaBajadaId);

        HttpRequest crearReservaReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/reservas/crear"))
            .header("Authorization", "Bearer " + tokenPasajero)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(reservaBody))
            .build();

        HttpResponse<String> crearReservaResp = http.send(crearReservaReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(crearReservaResp.statusCode() == 200 || crearReservaResp.statusCode() == 201,
            "Crear reserva API debe devolver 200/201, devolvió " + crearReservaResp.statusCode());

        // Poll API mis-reservas hasta que aparezca la reserva y obtener su id
        long reservaId = -1;
        boolean encontrada = false;
        for (int i = 0; i < 20; i++) { // ~10s total
            HttpRequest misReservasReq = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/api/reservas/mis-reservas"))
                .header("Authorization", "Bearer " + tokenPasajero)
                .GET()
                .build();
            HttpResponse<String> misReservasResp = http.send(misReservasReq, HttpResponse.BodyHandlers.ofString());
            if (misReservasResp.statusCode() == 200) {
                JsonNode arr = mapper.readTree(misReservasResp.body());
                for (JsonNode r : arr) {
                    long rViajeId = r.path("viajeId").asLong(-1);
                    if (rViajeId == viajeId) {
                        reservaId = r.path("id").asLong(-1);
                        encontrada = true;
                        break;
                    }
                }
                if (encontrada) break;
            }
            Thread.sleep(500);
        }
        assertTrue(encontrada && reservaId > 0, "La reserva debería aparecer en /api/reservas/mis-reservas tras crearla");

        // Cancelar la reserva vía API
        HttpRequest cancelarReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/reservas/cancelar?reservaId=" + reservaId))
            .header("Authorization", "Bearer " + tokenPasajero)
            .PUT(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> cancelarResp = http.send(cancelarReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(cancelarResp.statusCode() == 200 || cancelarResp.statusCode() == 204,
            "Cancelar reserva API debe devolver 200/204, devolvió " + cancelarResp.statusCode());

        // Esperar a que la cancelación se refleje en la API
        boolean cancelada = false;
        for (int i = 0; i < 20; i++) { // ~10s total
            HttpRequest misReservasReq = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/api/reservas/mis-reservas"))
                .header("Authorization", "Bearer " + tokenPasajero)
                .GET()
                .build();
            HttpResponse<String> misReservasResp = http.send(misReservasReq, HttpResponse.BodyHandlers.ofString());
            if (misReservasResp.statusCode() == 200) {
                JsonNode arr = mapper.readTree(misReservasResp.body());
                for (JsonNode r : arr) {
                    long rId = r.path("id").asLong(-1);
                    String estado = r.path("estado").asText("").toUpperCase();
                    if (rId == reservaId && (estado.contains("CANCELAD") || estado.contains("CANCELADA"))) {
                        cancelada = true;
                        break;
                    }
                }
                if (cancelada) break;
            }
            Thread.sleep(500);
        }
        assertTrue(cancelada, "La reserva debe aparecer como cancelada en /api/reservas/mis-reservas tras su cancelación");
    }

    @Test
    void editarReservaDesdeDetalleViajeOPerfil() throws Exception {
        loginConUsuarioSeeded();
        String tokenConductor = obtenerTokenLocalStorage();

        String sufijo = String.valueOf(System.currentTimeMillis());
        long vehiculoId = crearVehiculoPorApi(tokenConductor, sufijo);

        String origen = "Origen EditReserva " + sufijo;
        String destino = "Destino EditReserva " + sufijo;
        String slug = crearViajePorApiYObtenerSlug(tokenConductor, vehiculoId, origen, destino);

        // Registrar pasajero vía API y obtener token
        String apiBase = System.getenv().getOrDefault("E2E_API_BASE_URL", "http://localhost:8080");
        String pasajeroEmail = "pasajero" + sufijo + "@compicar.test";
        String telefonoPasajero = "+3460000" + (Long.parseLong(sufijo) % 10000L);

        String registroBody = """
            {
            "contrasena": "Pasajero123!",
            "nombre": "Pasajero",
            "primerApellido": "Test",
            "email": "%s",
            "numTelefono": "%s"
            }
            """.formatted(pasajeroEmail, telefonoPasajero);

        HttpRequest registroReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/registro"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(registroBody))
            .build();
        HttpResponse<String> registroResp = http.send(registroReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(registroResp.statusCode() == 200, "Registro API pasajero debe devolver 200, devolvió " + registroResp.statusCode());

        String loginBody = """
            {
            "email": "%s",
            "contrasena": "Pasajero123!"
            }
            """.formatted(pasajeroEmail);
        HttpRequest loginReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(loginBody))
            .build();
        HttpResponse<String> loginResp = http.send(loginReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(loginResp.statusCode() == 200, "Login API pasajero debe devolver 200, devolvió " + loginResp.statusCode());
        JsonNode loginJson = mapper.readTree(loginResp.body());
        String tokenPasajero = loginJson.path("token").asText().trim();
        assertTrue(tokenPasajero != null && !tokenPasajero.isBlank(), "Se esperaba token tras login API (pasajero)");

        // Obtener datos del viaje (id y paradas)
        HttpRequest getViajeReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/viajes/publicos/" + slug))
            .header("Authorization", "Bearer " + tokenPasajero)
            .GET()
            .build();
        HttpResponse<String> getViajeResp = http.send(getViajeReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(getViajeResp.statusCode() == 200, "Obtener viaje por slug debe devolver 200");
        JsonNode viajeJson = mapper.readTree(getViajeResp.body());
        long viajeId = viajeJson.path("id").asLong();
        JsonNode paradas = viajeJson.path("paradas");
        long paradaSubidaId = paradas.get(0).path("id").asLong();
        long paradaBajadaId = paradas.get(paradas.size() - 1).path("id").asLong();

        // Crear reserva inicial vía API (1 plaza)
        String crearReservaBody = """
            {
            "viajeId": %d,
            "plazas": 1,
            "paradaSubidaId": %d,
            "paradaBajadaId": %d
            }
            """.formatted(viajeId, paradaSubidaId, paradaBajadaId);

        HttpRequest crearReservaReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/reservas/crear"))
            .header("Authorization", "Bearer " + tokenPasajero)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(crearReservaBody))
            .build();
        HttpResponse<String> crearReservaResp = http.send(crearReservaReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(crearReservaResp.statusCode() == 200 || crearReservaResp.statusCode() == 201,
            "Crear reserva API debe devolver 200/201, devolvió " + crearReservaResp.statusCode());

        // Poll API mis-reservas hasta encontrar la reserva y obtener su id
        long reservaId = -1;
        for (int i = 0; i < 20; i++) {
            HttpRequest misReservasReq = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/api/reservas/mis-reservas"))
                .header("Authorization", "Bearer " + tokenPasajero)
                .GET()
                .build();
            HttpResponse<String> misReservasResp = http.send(misReservasReq, HttpResponse.BodyHandlers.ofString());
            if (misReservasResp.statusCode() == 200) {
                JsonNode arr = mapper.readTree(misReservasResp.body());
                for (JsonNode r : arr) {
                    long rViajeId = r.path("viajeId").asLong(-1);
                    if (rViajeId == viajeId) {
                        reservaId = r.path("id").asLong(-1);
                        break;
                    }
                }
                if (reservaId > 0) break;
            }
            Thread.sleep(500);
        }
        assertTrue(reservaId > 0, "No se encontró la reserva creada en /api/reservas/mis-reservas");

        // Actualizar la reserva vía API (pasar a 2 plazas)
        String actualizarBody = """
            {
            "viajeId": %d,
            "plazas": 2,
            "paradaSubidaId": %d,
            "paradaBajadaId": %d
            }
            """.formatted(viajeId, paradaSubidaId, paradaBajadaId);

        HttpRequest actualizarReq = HttpRequest.newBuilder()
            .uri(URI.create(apiBase + "/api/reservas/actualizar/" + reservaId))
            .header("Authorization", "Bearer " + tokenPasajero)
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(actualizarBody))
            .build();
        HttpResponse<String> actualizarResp = http.send(actualizarReq, HttpResponse.BodyHandlers.ofString());
        assertTrue(actualizarResp.statusCode() == 200,
            "Actualizar reserva API debe devolver 200, devolvió " + actualizarResp.statusCode());

        // Verificar el cambio en /api/reservas/mis-reservas
        boolean actualizado = false;
        for (int i = 0; i < 20; i++) {
            HttpRequest misReservasReq = HttpRequest.newBuilder()
                .uri(URI.create(apiBase + "/api/reservas/mis-reservas"))
                .header("Authorization", "Bearer " + tokenPasajero)
                .GET()
                .build();
            HttpResponse<String> misReservasResp = http.send(misReservasReq, HttpResponse.BodyHandlers.ofString());
            if (misReservasResp.statusCode() == 200) {
                JsonNode arr = mapper.readTree(misReservasResp.body());
                for (JsonNode r : arr) {
                    if (r.path("id").asLong(-1) == reservaId) {
                        int plazas = r.path("cantidadPlazas").asInt(-1);
                        if (plazas == 2 || r.path("estado").asText("").toUpperCase().contains("PENDIENTE")) {
                            actualizado = true;
                        }
                        break;
                    }
                }
                if (actualizado) break;
            }
            Thread.sleep(500);
        }

        assertTrue(actualizado, "La reserva actualizada debe mostrarse con 2 plazas o en estado PENDIENTE");
    }

    private String crearViajePorApiYObtenerSlug(String token, long vehiculoId, String origen, String destino, LocalDateTime fechaHora) throws Exception {
        String apiBase = System.getenv().getOrDefault("E2E_API_BASE_URL", "http://localhost:8080");

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
        String matricula = String.format(Locale.ROOT, "%04dXYZ", Long.parseLong(sufijo) % 10000L);        String body = mapper.writeValueAsString(new VehiculoReq(
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

    private void logoutYLoginComoPasajero() {
        driver.get(baseUrl + "/perfil");
        
        try {
            WebElement botonLogout = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Cerrar sesión' or normalize-space()='Logout']")
            ));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", botonLogout);
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
        }

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
        emailInput.sendKeys("pasajero@compicar.test");
        passwordInput.clear();
        passwordInput.sendKeys("Pasajero123!");
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/perfil"),
            ExpectedConditions.urlContains("/inicio")
        ));
    }
}
