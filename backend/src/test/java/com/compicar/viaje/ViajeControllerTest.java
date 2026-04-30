package com.compicar.viaje;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.compicar.viaje.dto.CalcularPrecioTrayectoRequestDTO;
import com.compicar.viaje.dto.PrecioTrayectoResponseDTO;
import com.compicar.viaje.dto.ViajeDTO;
import com.compicar.viaje.dto.VehiculoDTO;
import com.compicar.viaje.dto.ParadaDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ViajeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ViajeService viajeService;

    @InjectMocks
    private ViajeController viajeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(viajeController).build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearViaje_ok_autenticado() throws Exception {
        autenticar("driver@compicar.com");

        Viaje retorno = new Viaje();
        ReflectionTestUtils.setField(retorno, "id", 1L);
        retorno.setEstado(EstadoViaje.PENDIENTE);

        when(viajeService.crearViaje(anyString(), any(Viaje.class))).thenReturn(retorno);

        mockMvc.perform(post("/api/viajes/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(viajeService).crearViaje(ArgumentMatchers.eq("driver@compicar.com"), any(Viaje.class));
    }

    @Test
    void crearViaje_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/viajes/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(viajeService);
    }

    @Test
    void crearViaje_errorServicio_403() throws Exception {
        autenticar("driver@compicar.com");
        when(viajeService.crearViaje(anyString(), any(Viaje.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No permitido"));

        mockMvc.perform(post("/api/viajes/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void calcularPrecioTrayecto_ok() throws Exception {
        autenticar("driver@compicar.com");

        PrecioTrayectoResponseDTO dto = new PrecioTrayectoResponseDTO();
        dto.setFuente("GEMINI");
        dto.setLitrosEstimados(new BigDecimal("5.00"));
        dto.setPrecioCombustibleLitro(new BigDecimal("1.700"));
        dto.setCosteTotalCombustible(new BigDecimal("8.50"));

        when(viajeService.calcularPrecioTrayecto(anyString(), any(CalcularPrecioTrayectoRequestDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/api/viajes/precio/calcular")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehiculoId\":10,\"distanciaKm\":100.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fuente").value("GEMINI"))
                .andExpect(jsonPath("$.litrosEstimados").value(5.00));

        verify(viajeService).calcularPrecioTrayecto(ArgumentMatchers.eq("driver@compicar.com"), any(CalcularPrecioTrayectoRequestDTO.class));
    }

    @Test
    void calcularPrecioTrayecto_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/viajes/precio/calcular")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehiculoId\":10,\"distanciaKm\":100.0}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(viajeService);
    }

    @Test
    void calcularPrecioTrayecto_validacion_limite_distanciaInvalida_400() throws Exception {
        autenticar("driver@compicar.com");

        mockMvc.perform(post("/api/viajes/precio/calcular")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehiculoId\":10,\"distanciaKm\":0.0}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(viajeService);
    }

    @Test
    void obtenerMisViajes_ok() throws Exception {
        autenticar("driver@compicar.com");

        ViajeDTO v1 = new ViajeDTO(
                1L,
                LocalDateTime.of(2026, 5, 10, 9, 0),
                "PENDIENTE",
                3,
                new BigDecimal("10.00"),
                new VehiculoDTO(10L, "Seat", "Ibiza", "1234ABC"),
                List.of(new ParadaDTO(1L, "Sevilla", "ORIGEN", 1), new ParadaDTO(2L, "Cadiz", "DESTINO", 2)),
                "sevilla-cadiz-2026-05-10",
                "",
                "",
                List.of()
        );

        when(viajeService.obtenerMisViajes("driver@compicar.com")).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/viajes/mis-viajes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("sevilla-cadiz-2026-05-10"));
    }

    @Test
    void obtenerMisViajes_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/viajes/mis-viajes"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(viajeService);
    }

    @Test
    void obtenerViajesParticipados_ok() throws Exception {
        autenticar("driver@compicar.com");

        ViajeDTO v1 = new ViajeDTO(
                2L,
                LocalDateTime.of(2026, 5, 11, 9, 0),
                "PENDIENTE",
                2,
                new BigDecimal("12.00"),
                new VehiculoDTO(11L, "Toyota", "Yaris", "9876XYZ"),
                List.of(),
                "jerez-cadiz-2026-05-11",
                "",
                "",
                List.of()
        );

        when(viajeService.obtenerViajesParticipados("driver@compicar.com")).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/viajes/participados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void obtenerViajePorSlug_ok() throws Exception {
        ViajeDTO dto = new ViajeDTO(
                7L,
                LocalDateTime.of(2026, 6, 1, 8, 30),
                "PENDIENTE",
                3,
                new BigDecimal("15.00"),
                new VehiculoDTO(20L, "Kia", "Ceed", "4567DEF"),
                List.of(),
                "sevilla-huelva-2026-06-01",
                "",
                "",
                List.of()
        );

        when(viajeService.obtenerViajePorSlug("sevilla-huelva-2026-06-01")).thenReturn(dto);

        mockMvc.perform(get("/api/viajes/sevilla-huelva-2026-06-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.slug").value("sevilla-huelva-2026-06-01"));
    }

    @Test
    void obtenerViajePorSlug_noExiste_404() throws Exception {
        when(viajeService.obtenerViajePorSlug("no-existe"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        mockMvc.perform(get("/api/viajes/no-existe"))
                .andExpect(status().isNotFound());
    }

    private void autenticar(String email) {
        SecurityContext context = new org.springframework.security.core.context.SecurityContextImpl();
        context.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(email, null));
        SecurityContextHolder.setContext(context);
        clearInvocations(viajeService);
    }
}
