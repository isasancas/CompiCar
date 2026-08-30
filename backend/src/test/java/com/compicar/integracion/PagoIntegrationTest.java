package com.compicar.integracion;

import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.parada.Parada;
import com.compicar.parada.ParadaRepository;
import com.compicar.parada.TipoParada;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.vehiculo.TipoVehiculo;
import com.compicar.vehiculo.Vehiculo;
import com.compicar.vehiculo.VehiculoRepository;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PagoIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private PersonaRepository personaRepository;
    @Autowired
    private ViajeRepository viajeRepository;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private VehiculoRepository vehiculoRepository;
    @Autowired
    private ParadaRepository paradaRepository;

    private String tokenPasajero;
    private Persona pasajero;
    private Viaje viajeTest;
    private Parada paradaSubida;
    private Parada paradaBajada;

    @BeforeEach
    void setupDatosGenerales() throws Exception {
        long nano = System.nanoTime();

        // 1. Registro y login del pasajero mediante API
        String email = "pasajero+" + nano + "@compicar.test";
        Map<String, Object> registro = Map.of(
            "contrasena", "Password123!",
            "nombre", "Pasajero",
            "primerApellido", "Test",
            "segundoApellido", "Uno",
            "email", email,
            "numTelefono", "+34600000001"
        );
        mockMvc.perform(post("/api/registro")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(registro)))
            .andExpect(status().isOk());

        String res = mockMvc.perform(post("/api/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(Map.of("email", email, "contrasena", "Password123!"))))
            .andReturn().getResponse().getContentAsString();

        tokenPasajero = JsonPath.read(res, "$.token");
        pasajero = personaRepository.findByEmail(email).orElseThrow();

        // 2. Creación manual del Conductor con todos sus campos obligatorios
        Persona conductor = new Persona();
        conductor.setEmail("conductor+" + nano + "@compicar.test");
        conductor.setContrasena("Password123!");
        conductor.setNombre("Conductor");
        conductor.setPrimerApellido("Test");
        conductor.setSegundoApellido("Dos");
        conductor.setSlug("conductor-" + nano);
        conductor.setTelefono("+34600000002");
        conductor.setFondosActuales(BigDecimal.ZERO);
        conductor.setFondosTotales(BigDecimal.ZERO);
        conductor.setNumeroCancelaciones(0);
        personaRepository.save(conductor);

        // 3. Creación del Vehículo asociado al Conductor
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPersona(conductor);
        vehiculo.setMarca("Toyota");
        vehiculo.setModelo("Corolla");
        vehiculo.setMatricula("1234ABC");
        vehiculo.setAnio(2020);
        vehiculo.setPlazas(4);
        vehiculo.setConsumo(5.5);
        vehiculo.setTipo(TipoVehiculo.COCHE);
        vehiculo.setSlug("vehiculo-" + nano);
        vehiculoRepository.save(vehiculo);

        // 4. Creación del Viaje de prueba
        viajeTest = new Viaje();
        viajeTest.setPersona(conductor);
        viajeTest.setVehiculo(vehiculo);
        viajeTest.setPrecio(new BigDecimal("10.00"));
        viajeTest.setPlazasDisponibles(3);
        viajeTest.setCheckin("123456");
        viajeTest.setEstado(EstadoViaje.PENDIENTE);
        viajeTest.setFechaHoraSalida(LocalDateTime.now().plusDays(1));
        viajeTest.setSlug("viaje-test-" + nano);
        viajeRepository.save(viajeTest);

        // 5. Creación de las Paradas requeridas para la Reserva
        paradaSubida = new Parada();
        paradaSubida.setViaje(viajeTest);
        paradaSubida.setLocalizacion("Origen Test");
        paradaSubida.setFechaHora(LocalDateTime.now().plusDays(1));
        paradaSubida.setTipo(TipoParada.ORIGEN);
        paradaSubida.setOrden(1);
        paradaRepository.save(paradaSubida);

        paradaBajada = new Parada();
        paradaBajada.setViaje(viajeTest);
        paradaBajada.setLocalizacion("Destino Test");
        paradaBajada.setFechaHora(LocalDateTime.now().plusDays(1).plusHours(2));
        paradaBajada.setTipo(TipoParada.DESTINO);
        paradaBajada.setOrden(2);
        paradaRepository.save(paradaBajada);
    }

    private Pago crearReservaYPagoAsociado(String stripeId, EstadoPago estadoPago) {
        Reserva reserva = new Reserva();
        reserva.setPersona(pasajero);
        reserva.setViaje(viajeTest);
        reserva.setCantidadPlazas(1);
        reserva.setEstado(EstadoReserva.PENDIENTE);
        reserva.setFechaHoraReserva(LocalDateTime.now());
        reserva.setParadaSubida(paradaSubida);
        reserva.setParadaBajada(paradaBajada);
        reserva.setSlug("reserva-" + UUID.randomUUID());
        reservaRepository.save(reserva);

        Pago pago = new Pago();
        pago.setReserva(reserva);
        pago.setImporteTotal(new BigDecimal("10.00"));
        pago.setEstado(estadoPago);
        pago.setStripePaymentIntentId(stripeId);
        pago.setComision(new BigDecimal("5.00"));
        pago.setImporteConductor(new BigDecimal("10.00"));
        pago.setFechaCreacion(LocalDateTime.now());
        return pagoRepository.save(pago);
    }

    @Test
    void endpointProtegido_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/pagos/mis-pagos"))
            .andExpect(status().isForbidden());
    }

    @Test
    void obtenerMisPagos_conTokenValido_devuelveListaPagos() throws Exception {
        crearReservaYPagoAsociado("pi_test_mis_pagos", EstadoPago.PENDIENTE);

        mockMvc.perform(get("/api/pagos/mis-pagos")
            .header("Authorization", "Bearer " + tokenPasajero))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].stripePaymentIntentId").value("pi_test_mis_pagos"));
    }

    @Test
    void obtenerPagoPorId_pagoExistente_devuelve200() throws Exception {
        Pago pago = crearReservaYPagoAsociado("pi_test_get_id", EstadoPago.PENDIENTE);

        mockMvc.perform(get("/api/pagos/" + pago.getId())
            .header("Authorization", "Bearer " + tokenPasajero))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(pago.getId())); 
    }

    @Test
    void completarPago_pagoExistente_actualizaEstadoACapturado() throws Exception {
        Pago pago = crearReservaYPagoAsociado("pi_test_completar", EstadoPago.PENDIENTE);

        mockMvc.perform(put("/api/pagos/completar")
            .header("Authorization", "Bearer " + tokenPasajero)
            .param("pagoId", pago.getId().toString()))
            .andExpect(status().isOk());

        Pago pagoActualizado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertEquals(EstadoPago.CAPTURADO, pagoActualizado.getEstado());
    }

    @Test
    void fallarPago_pagoExistente_actualizaEstadoAFallido() throws Exception {
        Pago pago = crearReservaYPagoAsociado("pi_test_fallar", EstadoPago.PENDIENTE);

        mockMvc.perform(put("/api/pagos/fallar")
            .header("Authorization", "Bearer " + tokenPasajero)
            .param("pagoId", pago.getId().toString()))
            .andExpect(status().isOk());

        Pago pagoActualizado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertEquals(EstadoPago.FALLIDO, pagoActualizado.getEstado());
    }

    @Test
    void reembolsarPago_pagoExistente_actualizaEstadoAReembolsado() throws Exception {
        Pago pago = crearReservaYPagoAsociado("pi_test_reembolsar", EstadoPago.CAPTURADO);

        mockMvc.perform(put("/api/pagos/reembolsar")
            .header("Authorization", "Bearer " + tokenPasajero)
            .param("pagoId", pago.getId().toString()))
            .andExpect(status().isOk());

        Pago pagoActualizado = pagoRepository.findById(pago.getId()).orElseThrow();
        assertEquals(EstadoPago.REEMBOLSADO, pagoActualizado.getEstado());
    }

    @Test
    void webhookStripe_paymentIntentSucceeded_actualizaPagoACapturado() throws Exception {
        String stripeId = "pi_webhook_succeeded";
        crearReservaYPagoAsociado(stripeId, EstadoPago.PENDIENTE);

        Map<String, Object> payload = Map.of(
            "type", "payment_intent.succeeded",
            "data", Map.of("object", Map.of("id", stripeId))
        );

        mockMvc.perform(post("/api/v1/webhooks/stripe")
            .header("Stripe-Signature", "fake")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk());

        Pago pagoActualizado = pagoRepository.findByStripePaymentIntentId(stripeId).orElseThrow();
        assertEquals(EstadoPago.CAPTURADO, pagoActualizado.getEstado());
    }

    @Test
    void webhookStripe_paymentIntentFailed_actualizaPagoYCancelaReserva() throws Exception {
        String stripeId = "pi_webhook_failed";
        Pago pago = crearReservaYPagoAsociado(stripeId, EstadoPago.PENDIENTE);

        Map<String, Object> payload = Map.of(
            "type", "payment_intent.payment_failed",
            "data", Map.of("object", Map.of("id", stripeId))
        );

        mockMvc.perform(post("/api/v1/webhooks/stripe")
            .header("Stripe-Signature", "fake")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isOk());

        Pago pagoActualizado = pagoRepository.findByStripePaymentIntentId(stripeId).orElseThrow();
        assertEquals(EstadoPago.FALLIDO, pagoActualizado.getEstado());

        Reserva reservaActualizada = reservaRepository.findById(pago.getReserva().getId()).orElseThrow();
        assertEquals(EstadoReserva.CANCELADA, reservaActualizada.getEstado());
    }

    @Test
    void webhookStripe_firmaInvalida_devuelve400() throws Exception {
        Map<String, Object> payload = Map.of(
            "type", "payment_intent.succeeded",
            "data", Map.of("object", Map.of("id", "pi_123"))
        );

        mockMvc.perform(post("/api/v1/webhooks/stripe")
            .header("Stripe-Signature", "firma_falsa_invalida")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(payload)))
            .andExpect(status().isBadRequest());
    }
}