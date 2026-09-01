package com.compicar.integracion;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.vehiculo.TipoVehiculo;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.vehiculo.VehiculoRepository;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.compicar.viajeRecurrente.ViajeRecurrente;
import com.compicar.viajeRecurrente.ViajeRecurrenteRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ViajeRecurrenteIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ViajeRecurrenteRepository viajeRecurrenteRepository;
    @Autowired
    private PersonaRepository personaRepository;
    @Autowired
    private ViajeRepository viajeRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;

    private String tokenConductor;
    private Persona conductor;
    private ViajeRecurrente viajeRecurrenteTest;

    @BeforeEach
    void setupDatosGenerales() throws Exception {
        long nano = System.nanoTime();

        // 1. Registro y login del conductor mediante API
        String email = "conductor.rec+" + nano + "@compicar.test";
        Map<String, Object> registro = Map.of(
            "contrasena", "Password123!",
            "nombre", "ConductorRec",
            "primerApellido", "Test",
            "segundoApellido", "Uno",
            "email", email,
            "numTelefono", "+34600000003"
        );
        mockMvc.perform(post("/api/registro")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registro)))
            .andExpect(status().isOk());

        String res = mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("email", email, "contrasena", "Password123!"))))
            .andReturn().getResponse().getContentAsString();

        tokenConductor = JsonPath.read(res, "$.token");
        conductor = personaRepository.findByEmail(email).orElseThrow();

        // 2. Creación del Vehículo asociado al Conductor
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPersona(conductor);
        vehiculo.setMarca("Renault");
        vehiculo.setModelo("Megane");
        vehiculo.setMatricula("5678DEF");
        vehiculo.setAnio(2021);
        vehiculo.setPlazas(4);
        vehiculo.setConsumo(5.0);
        vehiculo.setTipo(TipoVehiculo.COCHE);
        vehiculo.setSlug("vehiculo-rec-" + nano);
        vehiculoRepository.save(vehiculo);

        // 3. Creación del Viaje Padre
        Viaje viajePadre = new Viaje();
        viajePadre.setPersona(conductor);
        viajePadre.setVehiculo(vehiculo);
        viajePadre.setPrecio(new BigDecimal("12.00"));
        viajePadre.setPlazasDisponibles(4);
        viajePadre.setCheckin("654321");
        viajePadre.setEstado(EstadoViaje.PENDIENTE);
        viajePadre.setFechaHoraSalida(LocalDateTime.now().plusDays(1));
        viajePadre.setSlug("viaje-padre-" + nano);
        viajeRepository.save(viajePadre);

        // 4. Creación del Viaje Recurrente de prueba
        viajeRecurrenteTest = new ViajeRecurrente();
        viajeRecurrenteTest.setPersona(conductor);
        viajeRecurrenteTest.setVehiculo(vehiculo);
        viajeRecurrenteTest.setViajePadre(viajePadre);
        viajeRecurrenteTest.setPrecio(new BigDecimal("12.00"));
        viajeRecurrenteTest.setPlazasDisponibles(4);
        viajeRecurrenteTest.setCheckin("ABC123");
        viajeRecurrenteTest.setEstado(EstadoViaje.PENDIENTE);
        viajeRecurrenteTest.setFechaHoraSalida(LocalDateTime.now().plusHours(2));
        viajeRecurrenteTest.setSlug("viaje-recurrente-test-" + nano);
        viajeRecurrenteRepository.save(viajeRecurrenteTest);
    }

    @Test
    void obtenerViajeRecurrentePorSlug_existente_devuelve200() throws Exception {
        mockMvc.perform(get("/api/viajes-recurrentes/" + viajeRecurrenteTest.getSlug())
                .header("Authorization", "Bearer " + tokenConductor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value(viajeRecurrenteTest.getSlug()));
    }

    @Test
    void iniciarViajeRecurrente_conductorValido_actualizaAIniciado() throws Exception {
        viajeRecurrenteTest.setFechaHoraSalida(LocalDateTime.now().minusHours(1));
        viajeRecurrenteRepository.save(viajeRecurrenteTest);

        mockMvc.perform(put("/api/viajes-recurrentes/" + viajeRecurrenteTest.getSlug() + "/iniciar")
            .header("Authorization", "Bearer " + tokenConductor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("INICIADO"));

        ViajeRecurrente actualizado = viajeRecurrenteRepository.findById(viajeRecurrenteTest.getId()).orElseThrow();
        assertEquals(EstadoViaje.INICIADO, actualizado.getEstado());
    }

    @Test
    void confirmarCheckinRecurrente_datosCorrectos_actualizaAEnCurso() throws Exception {
        viajeRecurrenteTest.setEstado(EstadoViaje.INICIADO);
        viajeRecurrenteRepository.save(viajeRecurrenteTest);

        mockMvc.perform(put("/api/viajes-recurrentes/" + viajeRecurrenteTest.getSlug() + "/checkin")
            .header("Authorization", "Bearer " + tokenConductor)
            .param("checkin", "ABC123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("EN_CURSO"));

        ViajeRecurrente actualizado = viajeRecurrenteRepository.findById(viajeRecurrenteTest.getId()).orElseThrow();
        assertEquals(EstadoViaje.EN_CURSO, actualizado.getEstado());
    }

    @Test
    void cancelarViajeRecurrente_conductorValido_actualizaACancelado() throws Exception {
        mockMvc.perform(put("/api/viajes-recurrentes/" + viajeRecurrenteTest.getSlug() + "/cancelar")
            .header("Authorization", "Bearer " + tokenConductor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CANCELADO"));

        ViajeRecurrente actualizado = viajeRecurrenteRepository.findById(viajeRecurrenteTest.getId()).orElseThrow();
        assertEquals(EstadoViaje.CANCELADO, actualizado.getEstado());
    }

    @Test
    void obtenerViajeRecurrentePorSlug_noExistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/viajes-recurrentes/slug-que-no-existe")
                .header("Authorization", "Bearer " + tokenConductor))
            .andExpect(status().isNotFound());
    }

    @Test
    void iniciarViajeRecurrente_sinToken_devuelve401O403() throws Exception {
        mockMvc.perform(put("/api/viajes-recurrentes/" + viajeRecurrenteTest.getSlug() + "/iniciar"))
            .andExpect(status().isForbidden()); // O isUnauthorized() según configuración de Spring Security
    }

    @Test
    void confirmarCheckinRecurrente_checkinIncorrecto_devuelve400() throws Exception {
        viajeRecurrenteTest.setEstado(EstadoViaje.INICIADO);
        viajeRecurrenteRepository.save(viajeRecurrenteTest);

        mockMvc.perform(put("/api/viajes-recurrentes/" + viajeRecurrenteTest.getSlug() + "/checkin")
            .header("Authorization", "Bearer " + tokenConductor)
            .param("checkin", "VALOR_INCORRECTO"))
            .andExpect(status().isBadRequest());
    }
}