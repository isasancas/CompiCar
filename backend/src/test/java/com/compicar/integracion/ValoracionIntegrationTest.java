package com.compicar.integracion;

import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ValoracionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void crearValoracionYObtenerPorId_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();

        Map<String, Object> reservaPayload = Map.of(
            "viajeId", viajeId,
            "cantidadPlazas", 1,
            "paradaSubidaId", pSubida,
            "paradaBajadaId", pBajada
        );

        MvcResult createReservaResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reservaPayload)))
            .andExpect(status().isOk())
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createReservaResult.getResponse().getContentAsString(), "$.reservaId")).longValue();

        Reserva reserva = reservaRepository.findById(reservaId).orElseThrow();
        Long passengerId = reserva.getPersona().getId();
        Long driverId = reserva.getViaje().getPersona().getId();

        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.saveAndFlush(reserva);
        entityManager.clear();

        Map<String, Object> valoracionPayload = Map.of(
            "autorId", passengerId,
            "valoradoId", driverId,
            "viajeId", viajeId,
            "puntuacion", 5,
            "comentario", "Excelente conductor, muy puntual."
        );

        MvcResult createValResult = mockMvc.perform(post("/api/valoraciones")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(valoracionPayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.puntuacion").value(5))
            .andReturn();

        Long valoracionId = ((Number) JsonPath.read(createValResult.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(get("/api/valoraciones/" + valoracionId)
            .header("Authorization", "Bearer " + passengerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(valoracionId))
            .andExpect(jsonPath("$.comentario").value("Excelente conductor, muy puntual."));
    }

    @Test
    void crearValoracion_sinToken_403() throws Exception {
        Map<String, Object> payload = Map.of(
            "autorId", 1,
            "valoradoId", 2,
            "viajeId", 10,
            "puntuacion", 4,
            "comentario", "Buen viaje"
        );

        mockMvc.perform(post("/api/valoraciones")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
    }

    @Test
    void actualizarValoracion_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();
        Map<String, Object> reservaPayload = Map.of(
            "viajeId", viajeId, "cantidadPlazas", 1, "paradaSubidaId", pSubida, "paradaBajadaId", pBajada
        );
        MvcResult createReservaResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reservaPayload)))
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createReservaResult.getResponse().getContentAsString(), "$.reservaId")).longValue();
        Reserva reserva = reservaRepository.findById(reservaId).orElseThrow();
        Long passengerId = reserva.getPersona().getId();
        Long driverId = reserva.getViaje().getPersona().getId();
        
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.saveAndFlush(reserva);
        entityManager.clear();

        Map<String, Object> valoracionPayload = Map.of(
            "autorId", passengerId,
            "valoradoId", driverId,
            "viajeId", viajeId,
            "puntuacion", 3,
            "comentario", "Normal"
        );

        MvcResult createValResult = mockMvc.perform(post("/api/valoraciones")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(valoracionPayload)))
            .andExpect(status().isOk())
            .andReturn();

        Long valoracionId = ((Number) JsonPath.read(createValResult.getResponse().getContentAsString(), "$.id")).longValue();

        Map<String, Object> updatePayload = Map.of(
            "autorId", passengerId,
            "valoradoId", driverId,
            "viajeId", viajeId,
            "puntuacion", 4,
            "comentario", "Mejor de lo que pensaba"
        );

        mockMvc.perform(put("/api/valoraciones/" + valoracionId)
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(updatePayload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.puntuacion").value(4))
            .andExpect(jsonPath("$.comentario").value("Mejor de lo que pensaba"));
    }

    @Test
    void eliminarValoracion_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        String passengerToken = registerAndLogin();
        Map<String, Object> reservaPayload = Map.of(
            "viajeId", viajeId, "cantidadPlazas", 1, "paradaSubidaId", pSubida, "paradaBajadaId", pBajada
        );
        
        MvcResult createReservaResult = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reservaPayload)))
            .andReturn();

        Long reservaId = ((Number) JsonPath.read(createReservaResult.getResponse().getContentAsString(), "$.reservaId")).longValue();
        Reserva reserva = reservaRepository.findById(reservaId).orElseThrow();
        Long passengerId = reserva.getPersona().getId();
        Long driverId = reserva.getViaje().getPersona().getId();
        
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.saveAndFlush(reserva);
        entityManager.clear();

        Map<String, Object> valoracionPayload = Map.of(
            "autorId", passengerId, "valoradoId", driverId, "viajeId", viajeId, "puntuacion", 5, "comentario", "Test"
        );

        MvcResult createValResult = mockMvc.perform(post("/api/valoraciones")
            .header("Authorization", "Bearer " + passengerToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(valoracionPayload)))
            .andExpect(status().isOk())
            .andReturn();

        Long valoracionId = ((Number) JsonPath.read(createValResult.getResponse().getContentAsString(), "$.id")).longValue();

        mockMvc.perform(delete("/api/valoraciones/" + valoracionId)
            .header("Authorization", "Bearer " + passengerToken))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/valoraciones/" + valoracionId)
            .header("Authorization", "Bearer " + passengerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void calcularReputacion_ok() throws Exception {
        String driverToken = registerAndLogin();
        Long vehiculoId = crearVehiculo(driverToken);
        MvcResult viajeResult = crearViaje(driverToken, vehiculoId);
        String viajeJson = viajeResult.getResponse().getContentAsString();
        Long viajeId = ((Number) JsonPath.read(viajeJson, "$.id")).longValue();
        Long pSubida = ((Number) JsonPath.read(viajeJson, "$.paradas[0].id")).longValue();
        Long pBajada = ((Number) JsonPath.read(viajeJson, "$.paradas[1].id")).longValue();

        Map<String, Object> reservaPayload = Map.of(
            "viajeId", viajeId, "cantidadPlazas", 1, "paradaSubidaId", pSubida, "paradaBajadaId", pBajada
        );

        // Pasajero 1
        String passenger1Token = registerAndLogin();
        MvcResult res1Result = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passenger1Token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reservaPayload))).andReturn();
        
        Long res1Id = ((Number) JsonPath.read(res1Result.getResponse().getContentAsString(), "$.reservaId")).longValue();
        Reserva reserva1 = reservaRepository.findById(res1Id).orElseThrow();
        Long driverId = reserva1.getViaje().getPersona().getId();
        Long pass1Id = reserva1.getPersona().getId();
        reserva1.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.saveAndFlush(reserva1);
        entityManager.clear();

        mockMvc.perform(post("/api/valoraciones")
            .header("Authorization", "Bearer " + passenger1Token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "autorId", pass1Id, "valoradoId", driverId, "viajeId", viajeId, "puntuacion", 5, "comentario", "Genial"
            )))).andExpect(status().isOk());

        // Pasajero 2
        String passenger2Token = registerAndLogin();
        MvcResult res2Result = mockMvc.perform(post("/api/reservas/crear")
            .header("Authorization", "Bearer " + passenger2Token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(reservaPayload))).andReturn();
            
        Long res2Id = ((Number) JsonPath.read(res2Result.getResponse().getContentAsString(), "$.reservaId")).longValue();
        Reserva reserva2 = reservaRepository.findById(res2Id).orElseThrow();
        Long pass2Id = reserva2.getPersona().getId();
        reserva2.setEstado(EstadoReserva.CONFIRMADA);
        reservaRepository.saveAndFlush(reserva2);
        entityManager.clear();

        mockMvc.perform(post("/api/valoraciones")
            .header("Authorization", "Bearer " + passenger2Token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of(
                "autorId", pass2Id, "valoradoId", driverId, "viajeId", viajeId, "puntuacion", 4, "comentario", "Bien"
            )))).andExpect(status().isOk());

        // Calcular reputación (Promedio de 5 y 4 = 4.5)
        mockMvc.perform(get("/api/valoraciones/reputacion/" + driverId)
            .header("Authorization", "Bearer " + driverToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(4.5));
    }
}