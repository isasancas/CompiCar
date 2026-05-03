package com.compicar.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ViajeIntegrationTest extends BaseIntegrationTest {

    @Test
    void crearViajeYObtenerPorSlug_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);

        crearViaje(token, vehiculoId);
        String slug = obtenerPrimerViajeSlug(token);

        mockMvc.perform(get("/api/viajes/" + slug)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(slug))
            .andExpect(jsonPath("$.vehiculo.id").value(vehiculoId));
    }

    @Test
    void obtenerMisViajes_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);
        crearViaje(token, vehiculoId);

        mockMvc.perform(get("/api/viajes/mis-viajes")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].vehiculo.id").value(vehiculoId));
    }

    @Test
    void calcularPrecioTrayecto_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);

        Map<String, Object> payload = Map.of(
            "vehiculoId", vehiculoId,
            "distanciaKm", 120.0
        );

        MvcResult result = mockMvc.perform(post("/api/viajes/precio/calcular")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.litrosEstimados").exists())
            .andExpect(jsonPath("$.costeTotalCombustible").exists())
            .andReturn();

        String fuente = JsonPath.read(result.getResponse().getContentAsString(), "$.fuente");
        assertTrue("GEMINI".equals(fuente) || "FALLBACK".equals(fuente));
    }

    @Test
    void calcularPrecioTrayecto_distanciaInvalida_400() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);

        Map<String, Object> payload = Map.of(
            "vehiculoId", vehiculoId,
            "distanciaKm", 0.0
        );

        mockMvc.perform(post("/api/viajes/precio/calcular")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void crearViaje_sinToken_401() throws Exception {
        Map<String, Object> payload = Map.of(
            "fechaHoraSalida", "2026-12-01T10:00:00",
            "estado", "PENDIENTE",
            "plazasDisponibles", 3,
            "precio", 10.50
        );

        mockMvc.perform(post("/api/viajes/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
    }

    @Test
    void obtenerViajesParticipados_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);
        crearViaje(token, vehiculoId);
        String slug = obtenerPrimerViajeSlug(token);

        mockMvc.perform(get("/api/viajes/participados")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void obtenerViajesParticipados_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/viajes/participados"))
            .andExpect(status().isForbidden());
    }

    @Test
    void buscarViajesPublicos_sinParametros_ok() throws Exception {
        mockMvc.perform(get("/api/viajes/publicos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void buscarViajesPublicos_conParametros_ok() throws Exception {
        mockMvc.perform(get("/api/viajes/publicos")
            .param("origen", "Sevilla")
            .param("destino", "Cadiz")
            .param("fecha", "2026-12-01"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void obtenerViajesPublicosPorConductor_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);
        crearViaje(token, vehiculoId);

        MvcResult personaResult = mockMvc.perform(get("/api/personas/perfil")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        String slug = JsonPath.read(personaResult.getResponse().getContentAsString(), "$.slug");

        mockMvc.perform(get("/api/viajes/publicos/conductor/" + slug))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void obtenerViajePublicoPorSlug_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);
        crearViaje(token, vehiculoId);
        String slug = obtenerPrimerViajeSlug(token);

        mockMvc.perform(get("/api/viajes/publicos/" + slug))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(slug));
    }

    @Test
    void obtenerViajePublicoPorSlug_noExiste_404() throws Exception {
        mockMvc.perform(get("/api/viajes/publicos/slug-inexistente"))
            .andExpect(status().isNotFound());
    }

    @Test
    void cancelarViaje_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);
        crearViaje(token, vehiculoId);
        String slug = obtenerPrimerViajeSlug(token);

        mockMvc.perform(put("/api/viajes/" + slug + "/cancelar")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(slug));
    }

    @Test
    void cancelarViaje_sinToken_401() throws Exception {
        mockMvc.perform(put("/api/viajes/cualquier-slug/cancelar"))
            .andExpect(status().isForbidden());
    }

    @Test
    void cancelarViaje_viajeMismoUsuario_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);
        crearViaje(token, vehiculoId);
        String slug = obtenerPrimerViajeSlug(token);

        mockMvc.perform(put("/api/viajes/" + slug + "/cancelar")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void actualizarViaje_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);
        crearViaje(token, vehiculoId);
        String slug = obtenerPrimerViajeSlug(token);

        Map<String, Object> updatePayload = Map.of(
            "fechaHoraSalida", LocalDateTime.of(2026, 12, 2, 14, 0).toString(),
            "estado", "PENDIENTE",
            "plazasDisponibles", 2,
            "precio", 15.50,
            "vehiculo", Map.of("id", vehiculoId)
        );

        mockMvc.perform(put("/api/viajes/" + slug)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatePayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(slug));
    }

    @Test
    void actualizarViaje_sinToken_401() throws Exception {
        Map<String, Object> payload = Map.of(
            "fechaHoraSalida", "2026-12-01T10:00:00",
            "estado", "PENDIENTE",
            "plazasDisponibles", 3,
            "precio", 10.50
        );

        mockMvc.perform(put("/api/viajes/cualquier-slug")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
    }

    @Test
    void actualizarViaje_usuarioDistinto_403() throws Exception {
        String token1 = registerAndLogin();
        Long vehiculoId = crearVehiculo(token1);
        crearViaje(token1, vehiculoId);
        String slug = obtenerPrimerViajeSlug(token1);

        String token2 = registerAndLogin();

        Map<String, Object> updatePayload = Map.of(
            "fechaHoraSalida", LocalDateTime.of(2026, 12, 2, 14, 0).toString(),
            "estado", "PENDIENTE",
            "plazasDisponibles", 2,
            "precio", 15.50,
            "vehiculo", Map.of("id", vehiculoId)
        );

        mockMvc.perform(put("/api/viajes/" + slug)
            .header("Authorization", "Bearer " + token2)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatePayload)))
            .andExpect(status().isForbidden());
    }
}
