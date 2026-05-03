package com.compicar.viaje;

import static org.mockito.ArgumentMatchers.any;
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
    void crearViaje_errorServicio_400() throws Exception {
        autenticar("driver@compicar.com");
        when(viajeService.crearViaje(anyString(), any(Viaje.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paradas inválidas"));

        mockMvc.perform(post("/api/viajes/crear")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
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
    void calcularPrecioTrayecto_errorServicio_403() throws Exception {
        autenticar("driver@compicar.com");
        when(viajeService.calcularPrecioTrayecto(anyString(), any(CalcularPrecioTrayectoRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Vehículo no pertenece al usuario"));

        mockMvc.perform(post("/api/viajes/precio/calcular")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"vehiculoId\":11,\"distanciaKm\":100.0}"))
                .andExpect(status().isForbidden());
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
    void obtenerMisViajes_viajeParticipado_ok() throws Exception {
        autenticar("passenger@compicar.com");

        ViajeDTO v1 = new ViajeDTO(
                3L,
                LocalDateTime.of(2026, 5, 12, 14, 0),
                "PENDIENTE",
                1,
                new BigDecimal("8.00"),
                new VehiculoDTO(12L, "Hyundai", "i20", "3333MNO"),
                List.of(),
                "malaga-granada-2026-05-12",
                "",
                "",
                List.of()
        );

        when(viajeService.obtenerMisViajes("passenger@compicar.com")).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/viajes/mis-viajes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(3));
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
    void obtenerViajesParticipados_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/viajes/participados"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(viajeService);
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

    @Test
    void buscarViajesPublicos_conTodosFiltros_ok() throws Exception {
        ViajeDTO viaje = new ViajeDTO(
                5L,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                "PENDIENTE",
                4,
                new BigDecimal("14.50"),
                new VehiculoDTO(15L, "Ford", "Focus", "5555ABC"),
                List.of(),
                "madrid-barcelona-2026-05-15",
                "",
                "",
                List.of()
        );

        when(viajeService.buscarViajesPublicos("Madrid", "Barcelona", java.time.LocalDate.of(2026, 5, 15)))
                .thenReturn(List.of(viaje));

        mockMvc.perform(get("/api/viajes/publicos")
                .param("origen", "Madrid")
                .param("destino", "Barcelona")
                .param("fecha", "2026-05-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].slug").value("madrid-barcelona-2026-05-15"));
    }

    @Test
    void buscarViajesPublicos_sinFecha_ok() throws Exception {
        ViajeDTO viaje = new ViajeDTO(
                6L,
                LocalDateTime.of(2026, 5, 20, 14, 30),
                "PENDIENTE",
                2,
                new BigDecimal("11.00"),
                new VehiculoDTO(16L, "Peugeot", "308", "6666XYZ"),
                List.of(),
                "valencia-murcia-2026-05-20",
                "",
                "",
                List.of()
        );

        when(viajeService.buscarViajesPublicos("Valencia", "Murcia", null))
                .thenReturn(List.of(viaje));

        mockMvc.perform(get("/api/viajes/publicos")
                .param("origen", "Valencia")
                .param("destino", "Murcia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(6));
    }

    @Test
    void buscarViajesPublicos_listaVacia_ok() throws Exception {
        when(viajeService.buscarViajesPublicos("NoExiste", "Inexistente", null))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/viajes/publicos")
                .param("origen", "NoExiste")
                .param("destino", "Inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void obtenerViajesPublicosPorConductor_ok() throws Exception {
        ViajeDTO v1 = new ViajeDTO(
                8L,
                LocalDateTime.of(2026, 5, 25, 11, 0),
                "PENDIENTE",
                3,
                new BigDecimal("16.00"),
                new VehiculoDTO(17L, "Renault", "Scenic", "7777DEF"),
                List.of(),
                "bilbao-vitoria-2026-05-25",
                "",
                "",
                List.of()
        );

        when(viajeService.obtenerViajesPublicosPorConductor("conductor-slug")).thenReturn(List.of(v1));

        mockMvc.perform(get("/api/viajes/publicos/conductor/conductor-slug"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(8));
    }

    @Test
    void obtenerViajesPublicosPorConductor_sinViajes_ok() throws Exception {
        when(viajeService.obtenerViajesPublicosPorConductor("sin-viajes")).thenReturn(List.of());

        mockMvc.perform(get("/api/viajes/publicos/conductor/sin-viajes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void obtenerViajePublicoPorSlug_ok() throws Exception {
        ViajeDTO dto = new ViajeDTO(
                9L,
                LocalDateTime.of(2026, 6, 10, 15, 0),
                "PENDIENTE",
                5,
                new BigDecimal("18.50"),
                new VehiculoDTO(18L, "Nissan", "Qashqai", "8888GHI"),
                List.of(),
                "alicante-ibiza-2026-06-10",
                "",
                "",
                List.of()
        );

        when(viajeService.obtenerViajePorSlug("alicante-ibiza-2026-06-10")).thenReturn(dto);

        mockMvc.perform(get("/api/viajes/publicos/alicante-ibiza-2026-06-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void cancelarViaje_ok_autenticado() throws Exception {
        autenticar("driver@compicar.com");

        ViajeDTO viajeActualizado = new ViajeDTO(
                10L,
                LocalDateTime.of(2026, 5, 30, 10, 0),
                "CANCELADO",
                3,
                new BigDecimal("12.00"),
                new VehiculoDTO(19L, "Opel", "Astra", "9999JKL"),
                List.of(),
                "sevilla-jaen-2026-05-30",
                "",
                "",
                List.of()
        );

        when(viajeService.cancelarViaje("driver@compicar.com", "sevilla-jaen-2026-05-30"))
                .thenReturn(viajeActualizado);

        mockMvc.perform(put("/api/viajes/sevilla-jaen-2026-05-30/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADO"));

        verify(viajeService).cancelarViaje("driver@compicar.com", "sevilla-jaen-2026-05-30");
    }

    @Test
    void cancelarViaje_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(put("/api/viajes/slug-test/cancelar"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(viajeService);
    }

    @Test
    void cancelarViaje_errorServicio_403() throws Exception {
        autenticar("otro@compicar.com");
        when(viajeService.cancelarViaje("otro@compicar.com", "slug-no-suyo"))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el conductor"));

        mockMvc.perform(put("/api/viajes/slug-no-suyo/cancelar"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelarViaje_errorServicio_404() throws Exception {
        autenticar("driver@compicar.com");
        when(viajeService.cancelarViaje("driver@compicar.com", "no-existe"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        mockMvc.perform(put("/api/viajes/no-existe/cancelar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarViaje_ok_autenticado() throws Exception {
        autenticar("driver@compicar.com");

        ViajeDTO viajeActualizado = new ViajeDTO(
                11L,
                LocalDateTime.of(2026, 6, 5, 12, 0),
                "PENDIENTE",
                5,
                new BigDecimal("15.00"),
                new VehiculoDTO(10L, "Seat", "Ibiza", "1234ABC"),
                List.of(),
                "sevilla-cadiz-2026-06-05",
                "",
                "",
                List.of()
        );

        when(viajeService.actualizarViaje(
                ArgumentMatchers.eq("driver@compicar.com"),
                ArgumentMatchers.eq("sevilla-cadiz-2026-06-05"),
                any(Viaje.class)))
                .thenReturn(viajeActualizado);

        mockMvc.perform(put("/api/viajes/sevilla-cadiz-2026-06-05")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"precio\":\"15.00\",\"plazasDisponibles\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.precio").value(15.00));

        verify(viajeService).actualizarViaje(
                ArgumentMatchers.eq("driver@compicar.com"),
                ArgumentMatchers.eq("sevilla-cadiz-2026-06-05"),
                any(Viaje.class));
    }

    @Test
    void actualizarViaje_noAutenticado_401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(put("/api/viajes/slug-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(viajeService);
    }

    @Test
    void actualizarViaje_errorServicio_403() throws Exception {
        autenticar("otro@compicar.com");
        when(viajeService.actualizarViaje(
                ArgumentMatchers.eq("otro@compicar.com"),
                ArgumentMatchers.eq("slug-otro"),
                any(Viaje.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres el conductor"));

        mockMvc.perform(put("/api/viajes/slug-otro")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarViaje_errorServicio_400_menosDe12h() throws Exception {
        autenticar("driver@compicar.com");
        when(viajeService.actualizarViaje(
                ArgumentMatchers.eq("driver@compicar.com"),
                ArgumentMatchers.eq("viaje-urgente"),
                any(Viaje.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menos de 12 horas para salida"));

        mockMvc.perform(put("/api/viajes/viaje-urgente")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarViaje_errorServicio_404() throws Exception {
        autenticar("driver@compicar.com");
        when(viajeService.actualizarViaje(
                ArgumentMatchers.eq("driver@compicar.com"),
                ArgumentMatchers.eq("no-existe"),
                any(Viaje.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        mockMvc.perform(put("/api/viajes/no-existe")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isNotFound());
    }

    private void autenticar(String email) {
        SecurityContext context = new org.springframework.security.core.context.SecurityContextImpl();
        context.setAuthentication(new org.springframework.security.authentication.TestingAuthenticationToken(email, null));
        SecurityContextHolder.setContext(context);
        clearInvocations(viajeService);
    }
}
