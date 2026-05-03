package com.compicar.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservaIntegrationTest extends BaseIntegrationTest {

    @Test
    void crearReservaYObtenerPorId_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);

        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        MvcResult createResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/reservas/" + reservaId)
            .header("Authorization", "Bearer " + passengerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(reservaId));
    }

    @Test
    void crearReserva_sinToken_403() throws Exception {
        Map<String, Object> payload = Map.of(
            "viajeId", 1,
            "plazas", 1,
            "paradaSubidaId", 1,
            "paradaBajadaId", 2
        );

        mockMvc.perform(post("/api/reservas/crear")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
    }

    @Test
    void obtenerReservaPorId_forbiddenParaOtroUsuario() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        MvcResult createResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.id")).longValue();

        String otherToken = registerAndLogin();

        mockMvc.perform(get("/api/reservas/" + reservaId)
            .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void confirmarYPendientesYNoPresentado_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        MvcResult createResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/reservas/pendientes-conductor")
            .header("Authorization", "Bearer " + driverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(reservaId));

        mockMvc.perform(get("/api/reservas/confirmar")
            .param("reservaId", String.valueOf(reservaId))
            .header("Authorization", "Bearer " + driverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CONFIRMADA"));

        mockMvc.perform(get("/api/reservas/noPresentado")
            .param("reservaId", String.valueOf(reservaId))
            .header("Authorization", "Bearer " + driverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("NO_PRESENTADO"));
    }

    @Test
    void cancelarReserva_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        MvcResult createResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(put("/api/reservas/cancelar")
            .param("reservaId", String.valueOf(reservaId))
            .header("Authorization", "Bearer " + passengerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void cancelarReserva_sinToken_401() throws Exception {
        mockMvc.perform(put("/api/reservas/cancelar")
            .param("reservaId", "1"))
            .andExpect(status().isForbidden());
    }

    @Test
    void obtenerMisReservas_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/reservas/mis-reservas")
            .header("Authorization", "Bearer " + passengerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void obtenerMisReservas_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/reservas/mis-reservas"))
            .andExpect(status().isForbidden());
    }

    @Test
    void obtenerReservasPorViaje_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/reservas/viaje")
            .param("viajeId", String.valueOf(viajeId))
            .header("Authorization", "Bearer " + driverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void obtenerReservasPorViaje_sinReservas_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();

        mockMvc.perform(get("/api/reservas/viaje")
            .param("viajeId", String.valueOf(viajeId))
            .header("Authorization", "Bearer " + driverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void actualizarReserva_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        MvcResult createResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.id")).longValue();

        Map<String, Object> updatePayload = Map.of(
            "viajeId", viajeId,
            "plazas", 2,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        mockMvc.perform(put("/api/reservas/actualizar/" + reservaId)
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatePayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(reservaId));
    }

    @Test
    void actualizarReserva_sinToken_401() throws Exception {
        Map<String, Object> payload = Map.of(
            "viajeId", 1,
            "plazas", 1,
            "paradaSubidaId", 1,
            "paradaBajadaId", 2
        );

        mockMvc.perform(put("/api/reservas/actualizar/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
    }

    @Test
    void actualizarReserva_usuarioDistinto_403() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        MvcResult createResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.id")).longValue();

        String otherToken = registerAndLogin();

        Map<String, Object> updatePayload = Map.of(
            "viajeId", viajeId,
            "plazas", 2,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        mockMvc.perform(put("/api/reservas/actualizar/" + reservaId)
            .header("Authorization", "Bearer " + otherToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatePayload)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rechazarReserva_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "viajeId", viajeId,
            "plazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        MvcResult createResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createResult.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/reservas/rechazar")
            .param("reservaId", String.valueOf(reservaId))
            .header("Authorization", "Bearer " + driverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADA"));
    }

    @Test
    void rechazarReserva_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/reservas/rechazar")
            .param("reservaId", "1"))
            .andExpect(status().isForbidden());
    }
}