package com.compicar.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUpMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    protected String registerAndLogin() throws Exception {
        String email = "it+" + System.nanoTime() + "@compicar.test";
        String phone = "+346" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
        String password = "Password123!";

        Map<String, Object> registro = Map.of(
            "contrasena", password,
            "nombre", "UsuarioIT",
            "primerApellido", "Prueba",
            "segundoApellido", "Auto",
            "email", email,
            "numTelefono", phone
        );

        mockMvc.perform(post("/api/registro")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registro)))
            .andExpect(status().isOk());

        Map<String, Object> login = Map.of(
            "email", email,
            "contrasena", password
        );

        MvcResult loginResult = mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(login)))
            .andExpect(status().isOk())
            .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");
        Assertions.assertNotNull(token);
        return token;
    }

    protected Long crearVehiculo(String token) throws Exception {
        String matricula = String.format("%04dABC", ThreadLocalRandom.current().nextInt(1000, 9999));

        Map<String, Object> payload = Map.of(
            "matricula", matricula,
            "marca", "Seat",
            "modelo", "Ibiza",
            "plazas", 4,
            "consumo", 5.2,
            "anio", 2020,
            "tipo", "COCHE"
        );

        MvcResult result = mockMvc.perform(post("/api/vehiculos")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isCreated())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected MvcResult crearViaje(String token, Long vehiculoId) throws Exception {
        Map<String, Object> payload = Map.of(
            "fechaHoraSalida", LocalDateTime.of(2026, 12, 1, 10, 0).toString(),
            "estado", "PENDIENTE",
            "plazasDisponibles", 3,
            "precio", 10.50,
            "vehiculo", Map.of("id", vehiculoId),
            "paradas", List.of(
                Map.of(
                    "fechaHora", LocalDateTime.of(2026, 12, 1, 10, 0).toString(),
                    "localizacion", "Sevilla",
                    "tipo", "ORIGEN",
                    "orden", 1
                ),
                Map.of(
                    "fechaHora", LocalDateTime.of(2026, 12, 1, 11, 30).toString(),
                    "localizacion", "Cadiz",
                    "tipo", "DESTINO",
                    "orden", 2
                )
            )
        );

        return mockMvc.perform(post("/api/viajes/crear")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk())
            .andReturn();
    }

    protected Long obtenerPrimerViajeId(String token) throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/viajes/mis-viajes")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    protected String obtenerPrimerViajeSlug(String token) throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/api/viajes/mis-viajes")
            .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].slug");
    }
}