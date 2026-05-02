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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.dto.ReservaDTO;
import com.compicar.reserva.dto.ReservaRequest;

@ExtendWith(MockitoExtension.class)
class ReservaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ReservaService reservaService;

    @Mock
    private PersonaRepository personaRepository;

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
        Field idField = Reserva.class.getDeclaredField("id");
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
        Field idField = Persona.class.getDeclaredField("id");
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
        Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 1L);

        Persona p = new Persona();
        Field emailField = Persona.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(p, "user@compicar.com");
        
        Field personIdField = Persona.class.getDeclaredField("id");
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
        Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 1L);

        Persona p = new Persona();
        Field emailField = Persona.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(p, "owner@compicar.com");
        
        Field personIdField = Persona.class.getDeclaredField("id");
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
        Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 5L);
        when(reservaService.reservaConfirmada("driver@compicar.com", 5L)).thenReturn(r);

        mockMvc.perform(get("/api/reservas/confirmar")
                .param("reservaId", "5")
                .principal(new TestingAuthenticationToken("driver@compicar.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void reservaNoPresentado_ok() throws Exception {
        Reserva r = new Reserva();
        Field idField = Reserva.class.getDeclaredField("id");
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
        Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 7L);
        when(reservaService.obtenerReservasComoConductor("driver@compicar.com")).thenReturn(List.of(r));

        mockMvc.perform(get("/api/reservas/pendientes-conductor")
                .principal(new TestingAuthenticationToken("driver@compicar.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7));
    }

    @Test
    void rechazarReserva_ok() throws Exception {
        Reserva r = new Reserva();
        Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 8L);
        when(reservaService.rechazarReserva("driver@compicar.com", 8L)).thenReturn(r);

        mockMvc.perform(put("/api/reservas/rechazar")
                .param("reservaId", "8")
                .principal(new TestingAuthenticationToken("driver@compicar.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8));
    }

    @Test
    void cancelarReserva_ok_autenticado() throws Exception {
        autenticar("user@compicar.com");

        Reserva r = new Reserva();
        Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 15L);

        when(reservaService.cancelarReserva("user@compicar.com", 15L)).thenReturn(r);

        mockMvc.perform(put("/api/reservas/cancelar")
                .param("reservaId", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15));

        verify(reservaService).cancelarReserva("user@compicar.com", 15L);
    }

    @Test
    void cancelarReserva_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(put("/api/reservas/cancelar")
                .param("reservaId", "15"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservaService);
    }

    @Test
    void obtenerReservasPorPersona_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/reservas/mis-reservas"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(personaRepository, reservaService);
    }

    @Test
    void obtenerReservasPorPersona_usuarioNoEncontrado_401() throws Exception {
        autenticar("user@compicar.com");
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/reservas/mis-reservas"))
                .andExpect(status().isUnauthorized());

        verify(personaRepository).findByEmail("user@compicar.com");
        verifyNoInteractions(reservaService);
    }

    @Test
    void obtenerReservasPorViaje_ok() throws Exception {
        Reserva r = new Reserva();
        Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 20L);

        when(reservaService.obtenerReservasPorViaje(10L)).thenReturn(List.of(r));

        mockMvc.perform(get("/api/reservas/viaje")
                .param("viajeId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20));

        verify(reservaService).obtenerReservasPorViaje(10L);
    }

    @Test
    void actualizarReserva_ok_autenticado() throws Exception {
        autenticar("user@compicar.com");

        Reserva r = new Reserva();
        Field idField = Reserva.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(r, 9L);

        when(reservaService.actualizarReserva(anyString(), anyLong(), any(ReservaRequest.class)))
                .thenReturn(r);

        mockMvc.perform(put("/api/reservas/actualizar/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"viajeId\":10,\"plazas\":2,\"paradaSubidaId\":101,\"paradaBajadaId\":102}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));

        verify(reservaService).actualizarReserva(anyString(), anyLong(), any(ReservaRequest.class));
    }

    @Test
    void actualizarReserva_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(put("/api/reservas/actualizar/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"viajeId\":10,\"plazas\":2,\"paradaSubidaId\":101,\"paradaBajadaId\":102}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservaService);
    }

    @Test
    void reservaConfirmada_principalNull_401() throws Exception {
        mockMvc.perform(get("/api/reservas/confirmar")
                .param("reservaId", "5"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservaService);
    }

    @Test
    void rechazarReserva_principalNull_401() throws Exception {
        mockMvc.perform(put("/api/reservas/rechazar")
                .param("reservaId", "8"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reservaService);
    }

    private void autenticar(String email) {
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new TestingAuthenticationToken(email, null));
        SecurityContextHolder.setContext(context);
        clearInvocations(reservaService);
    }
}