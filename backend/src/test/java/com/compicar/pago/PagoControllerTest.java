package com.compicar.pago;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.ReservaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
public class PagoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PagoService pagoService;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private PagoController pagoController;

    private ObjectMapper objectMapper;
    private Pago pagoEjemplo;
    private Persona personaEjemplo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pagoController).build();
        objectMapper = new ObjectMapper();

        personaEjemplo = new Persona();
        personaEjemplo.setId(1L);
        personaEjemplo.setEmail("usuario@test.com");

        pagoEjemplo = new Pago();
        pagoEjemplo.setId(10L);
        pagoEjemplo.setStripePaymentIntentId("pi_test_123");
        pagoEjemplo.setImporteTotal(new BigDecimal("25.00"));
        pagoEjemplo.setEstado(EstadoPago.PENDIENTE);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void simularAutenticacion(String email) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(email);

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCompletarPago_Exito() throws Exception {
        simularAutenticacion("usuario@test.com");
        pagoEjemplo.setEstado(EstadoPago.CAPTURADO);

        when(pagoService.pagoCompletado(10L)).thenReturn(pagoEjemplo);

        mockMvc.perform(put("/api/pagos/completar")
                .param("pagoId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.estado").value("CAPTURADO"));
    }

    @Test
    void testCompletarPago_SinAutenticar_Retorna401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(put("/api/pagos/completar")
                .param("pagoId", "10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testFallarPago_Exito() throws Exception {
        simularAutenticacion("usuario@test.com");
        pagoEjemplo.setEstado(EstadoPago.FALLIDO);

        when(pagoService.pagoFallido(10L)).thenReturn(pagoEjemplo);

        mockMvc.perform(put("/api/pagos/fallar")
                .param("pagoId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDO"));
    }

    @Test
    void testReembolsarPago_Exito() throws Exception {
        simularAutenticacion("usuario@test.com");
        pagoEjemplo.setEstado(EstadoPago.REEMBOLSADO);

        when(pagoService.pagoReembolsado(10L)).thenReturn(pagoEjemplo);

        mockMvc.perform(put("/api/pagos/reembolsar")
                .param("pagoId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REEMBOLSADO"));
    }

    @Test
    void testActualizarPago_Exito() throws Exception {
        simularAutenticacion("usuario@test.com");

        when(pagoService.actualizarPago(eq("usuario@test.com"), eq(5L), any(Pago.class)))
                .thenReturn(pagoEjemplo);

        mockMvc.perform(put("/api/pagos/actualizar")
                .param("usuarioEmail", "usuario@test.com")
                .param("reservaId", "5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(pagoEjemplo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void testObtenerPagoPorId_Exito() throws Exception {
        simularAutenticacion("usuario@test.com");

        when(pagoService.obtenerPagoPorId(10L)).thenReturn(pagoEjemplo);

        mockMvc.perform(get("/api/pagos/10")
                .param("pagoId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void testObtenerPagosPorPersona_Exito() throws Exception {
        simularAutenticacion("usuario@test.com");

        when(personaRepository.findByEmail("usuario@test.com")).thenReturn(Optional.of(personaEjemplo));
        when(pagoService.obtenerPagosPorPersona(personaEjemplo)).thenReturn(List.of(pagoEjemplo));

        mockMvc.perform(get("/api/pagos/mis-pagos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void testCapturarPago_Exito() throws Exception {
        mockMvc.perform(post("/api/pagos/capturar/{id}", "pi_test_123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Pago capturado con éxito"))
                .andExpect(jsonPath("$.id").value("pi_test_123"));

        verify(pagoService).capturarPago("pi_test_123");
    }

    @Test
    void testCapturarPago_NoEncontrado_Retorna404() throws Exception {
        doThrow(new EntityNotFoundException("Pago no encontrado"))
                .when(pagoService).capturarPago("pi_inexistente");

        mockMvc.perform(post("/api/pagos/capturar/{id}", "pi_inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Pago no encontrado"));
    }

    @Test
    void testCapturarPago_ErrorInterno_Retorna500() throws Exception {
        doThrow(new RuntimeException("Error en Stripe"))
                .when(pagoService).capturarPago("pi_error");

        mockMvc.perform(post("/api/pagos/capturar/{id}", "pi_error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Error al capturar el pago: Error en Stripe"));
    }
}
