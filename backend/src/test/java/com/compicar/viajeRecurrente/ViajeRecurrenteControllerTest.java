package com.compicar.viajeRecurrente;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.compicar.viajeRecurrente.dto.ViajeRecurrenteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ViajeRecurrenteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ViajeRecurrenteService viajeRecurrenteService;

    @InjectMocks
    private ViajeRecurrenteController viajeRecurrenteController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(viajeRecurrenteController).build();
    }

    private TestingAuthenticationToken crearAuth(String email) {
        return new TestingAuthenticationToken(email, null);
    }

    // --- GET /api/viajes-recurrentes/{slug} ---

    @Test
    void obtenerViajeRecurrentePorSlug_ok() throws Exception {
        ViajeRecurrenteDTO dto = new ViajeRecurrenteDTO();
        ReflectionTestUtils.setField(dto, "id", 1L);
        ReflectionTestUtils.setField(dto, "slug", "viaje-rec-1");

        when(viajeRecurrenteService.obtenerViajeRecurrentePorSlug("viaje-rec-1")).thenReturn(dto);

        mockMvc.perform(get("/api/viajes-recurrentes/viaje-rec-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.slug").value("viaje-rec-1"));
    }

    @Test
    void obtenerViajeRecurrentePorSlug_noExiste_404() throws Exception {
        when(viajeRecurrenteService.obtenerViajeRecurrentePorSlug("no-existe"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje recurrente no encontrado"));

        mockMvc.perform(get("/api/viajes-recurrentes/no-existe"))
                .andExpect(status().isNotFound());
    }

    // --- PUT /api/viajes-recurrentes/{slug}/iniciar ---

    @Test
    void iniciarViajeRecurrente_ok_autenticado() throws Exception {
        ViajeRecurrenteDTO dto = new ViajeRecurrenteDTO();
        ReflectionTestUtils.setField(dto, "estado", "INICIADO");

        when(viajeRecurrenteService.iniciarViajeRecurrente("driver@compicar.com", "viaje-rec-1")).thenReturn(dto);

        mockMvc.perform(put("/api/viajes-recurrentes/viaje-rec-1/iniciar")
                .principal(crearAuth("driver@compicar.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INICIADO"));

        verify(viajeRecurrenteService).iniciarViajeRecurrente("driver@compicar.com", "viaje-rec-1");
    }

    @Test
    void iniciarViajeRecurrente_errorServicio_403() throws Exception {
        when(viajeRecurrenteService.iniciarViajeRecurrente("otro@compicar.com", "viaje-rec-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado"));

        mockMvc.perform(put("/api/viajes-recurrentes/viaje-rec-1/iniciar")
                .principal(crearAuth("otro@compicar.com")))
                .andExpect(status().isForbidden());
    }

    // --- PUT /api/viajes-recurrentes/{slug}/checkin ---

    @Test
    void confirmarCheckinRecurrente_ok_autenticado() throws Exception {
        ViajeRecurrenteDTO dto = new ViajeRecurrenteDTO();
        ReflectionTestUtils.setField(dto, "estado", "EN_CURSO");

        when(viajeRecurrenteService.confirmarCheckinRecurrente("driver@compicar.com", "viaje-rec-1", "CHK123")).thenReturn(dto);

        mockMvc.perform(put("/api/viajes-recurrentes/viaje-rec-1/checkin")
                .param("checkin", "CHK123")
                .principal(crearAuth("driver@compicar.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));

        verify(viajeRecurrenteService).confirmarCheckinRecurrente("driver@compicar.com", "viaje-rec-1", "CHK123");
    }

    @Test
    void confirmarCheckinRecurrente_codigoInvalido_400() throws Exception {
        when(viajeRecurrenteService.confirmarCheckinRecurrente("driver@compicar.com", "viaje-rec-1", "BAD123"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkin inválido"));

        mockMvc.perform(put("/api/viajes-recurrentes/viaje-rec-1/checkin")
                .param("checkin", "BAD123")
                .principal(crearAuth("driver@compicar.com")))
                .andExpect(status().isBadRequest());
    }

    // --- PUT /api/viajes-recurrentes/{slug}/finalizar ---

    @Test
    void finalizarViajeRecurrente_ok_autenticado() throws Exception {
        ViajeRecurrenteDTO dto = new ViajeRecurrenteDTO();
        ReflectionTestUtils.setField(dto, "estado", "FINALIZADO");

        when(viajeRecurrenteService.finalizarViajeRecurrente("driver@compicar.com", "viaje-rec-1")).thenReturn(dto);

        mockMvc.perform(put("/api/viajes-recurrentes/viaje-rec-1/finalizar")
                .principal(crearAuth("driver@compicar.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FINALIZADO"));

        verify(viajeRecurrenteService).finalizarViajeRecurrente("driver@compicar.com", "viaje-rec-1");
    }

    // --- PUT /api/viajes-recurrentes/{slug}/cancelar ---

    @Test
    void cancelarViajeRecurrente_ok_autenticado() throws Exception {
        ViajeRecurrenteDTO dto = new ViajeRecurrenteDTO();
        ReflectionTestUtils.setField(dto, "estado", "CANCELADO");

        when(viajeRecurrenteService.cancelarViajeRecurrente("driver@compicar.com", "viaje-rec-1")).thenReturn(dto);

        mockMvc.perform(put("/api/viajes-recurrentes/viaje-rec-1/cancelar")
                .principal(crearAuth("driver@compicar.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        verify(viajeRecurrenteService).cancelarViajeRecurrente("driver@compicar.com", "viaje-rec-1");
    }

    @Test
    void cancelarViajeRecurrente_errorServicio_400() throws Exception {
        when(viajeRecurrenteService.cancelarViajeRecurrente("driver@compicar.com", "viaje-cancelado"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya está cancelado"));

        mockMvc.perform(put("/api/viajes-recurrentes/viaje-cancelado/cancelar")
                .principal(crearAuth("driver@compicar.com")))
                .andExpect(status().isBadRequest());
    }
}