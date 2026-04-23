package com.compicar.integracion;

import com.compicar.parada.Parada;
import com.compicar.parada.ParadaRepository;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParadaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ParadaRepository paradaRepository;

    @Autowired
    private ViajeRepository viajeRepository;

    @Test
    void anadirParadasYObtenerPorViaje_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);

        crearViaje(token, vehiculoId);
        Long viajeId = obtenerPrimerViajeId(token);

        List<Map<String, Object>> nuevasParadas = List.of(
            Map.of(
                "fechaHora", LocalDateTime.of(2026, 12, 1, 10, 45).toString(),
                "localizacion", "Dos Hermanas",
                "tipo", "INTERMEDIA",
                "orden", 2
            )
        );

        mockMvc.perform(post("/api/paradas/api/viajes/" + viajeId + "/paradas")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(nuevasParadas)))
            .andExpect(status().isOk());

        Viaje viaje = viajeRepository.findById(viajeId).orElseThrow();
        List<Parada> paradas = paradaRepository.findByViaje(viaje);

        assertTrue(paradas.stream().anyMatch(p -> "Sevilla".equals(p.getLocalizacion())));
        assertTrue(paradas.stream().anyMatch(p -> "Dos Hermanas".equals(p.getLocalizacion())));
        assertTrue(paradas.stream().anyMatch(p -> "Cadiz".equals(p.getLocalizacion())));

        mockMvc.perform(get("/api/paradas/viaje/" + viajeId)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void anadirParadas_viajeNoExiste_404() throws Exception {
        String token = registerAndLogin();

        List<Map<String, Object>> payload = List.of(
            Map.of(
                "fechaHora", "2026-12-01T10:45:00",
                "localizacion", "X",
                "tipo", "INTERMEDIA",
                "orden", 1
            )
        );

        mockMvc.perform(post("/api/paradas/api/viajes/999999/paradas")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isNotFound());
    }

    @Test
    void obtenerParadasPorViaje_viajeNoExiste_404() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/paradas/viaje/999999")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isNotFound());
    }

    @Test
    void obtenerParadasPorViaje_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/paradas/viaje/1"))
            .andExpect(status().isForbidden());
    }
}