package com.compicar.integracion;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PersonaIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PersonaRepository personaRepository;

    @Test
    void obtenerPerfil_ok() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/personas/perfil")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void retirarFondos_ok() throws Exception {
        String token = registerAndLogin();

        Persona persona = personaRepository.findAll().stream().findFirst().orElseThrow();
        persona.setFondosActuales(BigDecimal.valueOf(50.00));
        personaRepository.save(persona);

        Map<String, Object> payload = Map.of(
                "cantidad", 10.00
        );

        mockMvc.perform(post("/api/personas/retirar-fondos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    void retirarFondos_sinSaldo_400() throws Exception {
        String token = registerAndLogin();
        
        Map<String, Object> payload = Map.of(
                "cantidad", 9999.00
        );

        mockMvc.perform(post("/api/personas/retirar-fondos")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retirarFondos_sinToken_401() throws Exception {
        Map<String, Object> payload = Map.of(
                "cantidad", 10.00
        );

        mockMvc.perform(post("/api/personas/retirar-fondos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isForbidden());
    }
}