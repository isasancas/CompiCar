package com.compicar.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
