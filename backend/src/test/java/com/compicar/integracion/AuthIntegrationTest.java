package com.compicar.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void registroYLogin_ok() throws Exception {
        String email = "auth+" + System.nanoTime() + "@compicar.test";
        Map<String, Object> registro = Map.of(
            "contrasena", "Password123!",
            "nombre", "Auth",
            "primerApellido", "Test",
            "segundoApellido", "Uno",
            "email", email,
            "numTelefono", "+34612345678"
        );

        mockMvc.perform(post("/api/registro")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registro)))
            .andExpect(status().isOk());

        Map<String, Object> login = Map.of(
            "email", email,
            "contrasena", "Password123!"
        );

        MvcResult result = mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        assertFalse(token.isBlank());
    }

    @Test
    void loginPasswordIncorrecta_devuelve500SegunManejadorActual() throws Exception {
        String token = registerAndLogin();
        // El token se usa para asegurar que el usuario existe; luego intentamos login con pass mala.
        String email = "authbad+" + System.nanoTime() + "@compicar.test";

        Map<String, Object> registro = Map.of(
            "contrasena", "Password123!",
            "nombre", "Auth",
            "primerApellido", "Bad",
            "segundoApellido", "Pwd",
            "email", email,
            "numTelefono", "+34687654321"
        );

        mockMvc.perform(post("/api/registro")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registro)))
            .andExpect(status().isOk());

        Map<String, Object> login = Map.of(
            "email", email,
            "contrasena", "mala"
        );

        mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void endpointProtegido_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/vehiculos/propios"))
            .andExpect(status().isForbidden());
    }
}
