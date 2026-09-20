package com.compicar.integracion;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContaminacionAireIntegrationTest extends BaseIntegrationTest {

    @Test
    void obtenerContaminacionActual_autenticado_ok() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/contaminacion/actual")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.datos").exists())
            .andExpect(jsonPath("$.fechaConsulta").exists());
    }

    @Test
    void obtenerContaminacionActual_sinToken_ok() throws Exception {
        mockMvc.perform(get("/api/contaminacion/actual"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.datos").exists())
            .andExpect(jsonPath("$.fechaConsulta").exists());
    }
}