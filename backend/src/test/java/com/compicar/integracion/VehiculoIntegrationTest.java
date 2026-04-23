package com.compicar.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehiculoIntegrationTest extends BaseIntegrationTest {

    @Test
    void crearVehiculoYListarPropios_ok() throws Exception {
        String token = registerAndLogin();

        Long vehiculoId = crearVehiculo(token);

        mockMvc.perform(get("/api/vehiculos/propios")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(vehiculoId));
    }

    @Test
    void crearVehiculo_matriculaInvalida_400() throws Exception {
        String token = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "matricula", "ABC",
            "marca", "Seat",
            "modelo", "Ibiza",
            "plazas", 4,
            "consumo", 5.2,
            "anio", 2020,
            "tipo", "COCHE"
        );

        mockMvc.perform(post("/api/vehiculos")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void editarVehiculo_ok() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);

        Map<String, Object> payload = Map.of(
            "matricula", "9999XYZ",
            "marca", "Toyota",
            "modelo", "Yaris",
            "plazas", 4,
            "consumo", 4.9,
            "anio", 2021,
            "tipo", "COCHE"
        );

        mockMvc.perform(put("/api/vehiculos/" + vehiculoId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matricula").value("9999XYZ"))
            .andExpect(jsonPath("$.marca").value("Toyota"));
    }

    @Test
    void borrarVehiculo_ok_204() throws Exception {
        String token = registerAndLogin();
        Long vehiculoId = crearVehiculo(token);

        mockMvc.perform(delete("/api/vehiculos/" + vehiculoId)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
    }

    @Test
    void obtenerVehiculoPorSlug_ok() throws Exception {
        String token = registerAndLogin();

        Map<String, Object> payload = Map.of(
            "matricula", "1234BCD",
            "marca", "Seat",
            "modelo", "Leon",
            "plazas", 5,
            "consumo", 5.8,
            "anio", 2019,
            "tipo", "COCHE"
        );

        MvcResult createResult = mockMvc.perform(post("/api/vehiculos")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andReturn();

        String slug = JsonPath.read(createResult.getResponse().getContentAsString(), "$.slug");

        mockMvc.perform(get("/api/vehiculos/" + slug)
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(slug));
    }

    @Test
    void crearVehiculo_sinToken_401() throws Exception {
        Map<String, Object> payload = Map.of(
            "matricula", "1234ABC",
            "marca", "Seat",
            "modelo", "Ibiza",
            "plazas", 4,
            "consumo", 5.2,
            "anio", 2020,
            "tipo", "COCHE"
        );

        mockMvc.perform(post("/api/vehiculos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isForbidden());
    }
}
