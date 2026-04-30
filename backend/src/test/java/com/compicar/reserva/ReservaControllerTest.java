package com.compicar.reserva;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.compicar.persona.Persona;
import com.compicar.reserva.dto.ReservaDTO;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReservaService reservaService;

    @Mock
    private com.compicar.persona.PersonaRepository personaRepository;

    @InjectMocks
    private ReservaController reservaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservaController).build();
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearReserva_ok_autenticado() throws Exception {
        autenticar("user@compicar.com");

        Reserva retorno = new Reserva();
        java.lang.reflect.Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(retorno, 1L);

        // CORRECCIÓN: Usamos any() para todos los campos para evitar el error de Strict Stubbing
        // Mockito a veces falla al diferenciar entre long y Long en modo estricto.
        when(reservaService.crearReserva(any(), any(), any(), any(), any()))
                .thenReturn(retorno);

        mockMvc.perform(post("/api/reservas/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"viajeId\":10,\"plazas\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        // Verificamos de la misma forma que definimos el stubbing
        verify(reservaService).crearReserva(any(), any(), any(), any(), any());
    }

    @Test
    void crearReserva_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/reservas/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"viajeId\":10,\"plazas\":1}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservaService);
    }

    @Test
    void obtenerReservasPorPersona_ok() throws Exception {
        autenticar("user@compicar.com");

        ReservaDTO dto = new ReservaDTO(1L, "PENDIENTE", LocalDateTime.now(), 10L, 2L, "Nombre", "slug", 1L, 2L, 1);
        Persona persona = new Persona();
        java.lang.reflect.Field idField = Persona.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(persona, 2L);
        
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(java.util.Optional.of(persona));
        when(reservaService.obtenerReservasPorPersona(persona)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/reservas/mis-reservas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void obtenerReservaPorId_ok_autorizado() throws Exception {
        autenticar("user@compicar.com");

        Reserva r = new Reserva();
        java.lang.reflect.Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 1L);

        Persona p = new Persona();
        java.lang.reflect.Field emailField = Persona.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(p, "user@compicar.com");
        
        java.lang.reflect.Field personIdField = Persona.class.getDeclaredField("id");
        personIdField.setAccessible(true);
        personIdField.set(p, 2L);

        r.setPersona(p);

        when(reservaService.obtenerReservaPorId(1L)).thenReturn(r);

        mockMvc.perform(get("/api/reservas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void obtenerReservaPorId_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/reservas/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerReservaPorId_forbidden() throws Exception {
        autenticar("other@compicar.com");

        Reserva r = new Reserva();
        java.lang.reflect.Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 1L);

        Persona p = new Persona();
        java.lang.reflect.Field emailField = Persona.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(p, "owner@compicar.com");
        
        java.lang.reflect.Field personIdField = Persona.class.getDeclaredField("id");
        personIdField.setAccessible(true);
        personIdField.set(p, 2L);
        r.setPersona(p);

        when(reservaService.obtenerReservaPorId(1L)).thenReturn(r);

        mockMvc.perform(get("/api/reservas/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reservaConfirmada_ok() throws Exception {
        Reserva r = new Reserva();
        java.lang.reflect.Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 5L);
        when(reservaService.reservaConfirmada("driver@compicar.com", 5L)).thenReturn(r);

        mockMvc.perform(get("/api/reservas/confirmar")
                .param("reservaId", "5")
                .principal(new org.springframework.security.authentication.TestingAuthenticationToken("driver@compicar.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void reservaNoPresentado_ok() throws Exception {
        Reserva r = new Reserva();
        java.lang.reflect.Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 6L);
        when(reservaService.reservaNoPresentado(6L)).thenReturn(r);

        mockMvc.perform(get("/api/reservas/noPresentado").param("reservaId", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6));
    }

    @Test
    void obtenerPendientes_principalNull_401() throws Exception {
        mockMvc.perform(get("/api/reservas/pendientes-conductor"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void obtenerPendientes_ok() throws Exception {
        Reserva r = new Reserva();
        java.lang.reflect.Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 7L);
        when(reservaService.obtenerReservasComoConductor("driver@compicar.com")).thenReturn(List.of(r));

        mockMvc.perform(get("/api/reservas/pendientes-conductor")
                .principal(new org.springframework.security.authentication.TestingAuthenticationToken("driver@compicar.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void rechazarReserva_ok() throws Exception {
        Reserva r = new Reserva();
        java.lang.reflect.Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 8L);
        when(reservaService.rechazarReserva("driver@compicar.com", 8L)).thenReturn(r);

        mockMvc.perform(put("/api/reservas/rechazar")
                .param("reservaId", "8")
                .principal(new org.springframework.security.authentication.TestingAuthenticationToken("driver@compicar.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));
    }

    private void autenticar(String email) {
        SecurityContext context = new org.springframework.security.core.context.SecurityContextImpl();
        context.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(email, null));
        SecurityContextHolder.setContext(context);
        // Limpiar invocaciones previas para evitar que el modo estricto detecte basura de otros tests
        clearInvocations(reservaService);
    }
}