package com.compicar.vehiculo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.compicar.vehiculo.dto.AltaVehiculoRequestDTO;
import com.compicar.vehiculo.dto.VehiculoResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class VehiculoControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private VehiculoService vehiculoService;

    @InjectMocks
    private VehiculoController vehiculoController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vehiculoController).build();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearVehiculo_ok_201() throws Exception {
        autenticar("driver@compicar.com");

        AltaVehiculoRequestDTO req = requestValido();
        VehiculoResponseDTO resp = responseVehiculo(10L, "1234ABC", "seat-ibiza-1234abc");

        when(vehiculoService.crearVehiculo(anyString(), any(AltaVehiculoRequestDTO.class))).thenReturn(resp);

        mockMvc.perform(post("/api/vehiculos")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.matricula").value("1234ABC"));

        verify(vehiculoService).crearVehiculo(ArgumentMatchers.eq("driver@compicar.com"), any(AltaVehiculoRequestDTO.class));
    }

    @Test
    void crearVehiculo_noAutenticado_401() throws Exception {
        mockMvc.perform(post("/api/vehiculos")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestValido())))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    void crearVehiculo_validacion_matriculaInvalida_400() throws Exception {
        autenticar("driver@compicar.com");

        AltaVehiculoRequestDTO req = requestValido();
        req.setMatricula("ABC"); // no cumple regex

        mockMvc.perform(post("/api/vehiculos")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(vehiculoService);
    }

    @Test
    void obtenerMisVehiculos_ok() throws Exception {
        autenticar("driver@compicar.com");

        when(vehiculoService.obtenerMisVehiculos("driver@compicar.com"))
            .thenReturn(List.of(
                responseVehiculo(1L, "1234ABC", "seat-ibiza-1234abc"),
                responseVehiculo(2L, "5678DEF", "seat-leon-5678def")
            ));

        mockMvc.perform(get("/api/vehiculos/propios"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].matricula").value("1234ABC"))
            .andExpect(jsonPath("$[1].slug").value("seat-leon-5678def"));
    }

    @Test
    void editarVehiculo_ok() throws Exception {
        autenticar("driver@compicar.com");

        AltaVehiculoRequestDTO req = requestValido();
        VehiculoResponseDTO resp = responseVehiculo(10L, "9999XYZ", "seat-ibiza-9999xyz");
        resp.setMarca("Seat");
        resp.setModelo("Ibiza");

        when(vehiculoService.actualizarVehiculo(anyString(), anyLong(), any(AltaVehiculoRequestDTO.class))).thenReturn(resp);

        req.setMatricula("9999XYZ");
        mockMvc.perform(put("/api/vehiculos/10")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.matricula").value("9999XYZ"));

        verify(vehiculoService).actualizarVehiculo(ArgumentMatchers.eq("driver@compicar.com"), ArgumentMatchers.eq(10L), any(AltaVehiculoRequestDTO.class));
    }

    @Test
    void editarVehiculo_noEncontrado_404() throws Exception {
        autenticar("driver@compicar.com");

        when(vehiculoService.actualizarVehiculo(anyString(), anyLong(), any(AltaVehiculoRequestDTO.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehiculo no encontrado"));

        mockMvc.perform(put("/api/vehiculos/77")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestValido())))
            .andExpect(status().isNotFound());
    }

    @Test
    void borrarVehiculo_ok_204() throws Exception {
        autenticar("driver@compicar.com");

        mockMvc.perform(delete("/api/vehiculos/10"))
            .andExpect(status().isNoContent());

        verify(vehiculoService).borrarVehiculo("driver@compicar.com", 10L);
    }

    @Test
    void borrarVehiculo_conflicto_409() throws Exception {
        autenticar("driver@compicar.com");
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "No se puede borrar"))
            .when(vehiculoService).borrarVehiculo("driver@compicar.com", 10L);

        mockMvc.perform(delete("/api/vehiculos/10"))
            .andExpect(status().isConflict());
    }

    @Test
    void obtenerVehiculoPorSlug_ok() throws Exception {
        when(vehiculoService.obtenerVehiculoPorSlug("seat-ibiza-1234abc"))
            .thenReturn(responseVehiculo(10L, "1234ABC", "seat-ibiza-1234abc"));

        mockMvc.perform(get("/api/vehiculos/seat-ibiza-1234abc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(10))
            .andExpect(jsonPath("$.slug").value("seat-ibiza-1234abc"));
    }

    @Test
    void obtenerVehiculoPorSlug_noExiste_404() throws Exception {
        when(vehiculoService.obtenerVehiculoPorSlug("no-existe"))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehiculo no encontrado"));

        mockMvc.perform(get("/api/vehiculos/no-existe"))
            .andExpect(status().isNotFound());
    }

    private void autenticar(String email) {
        SecurityContext context = new SecurityContextImpl();
        context.setAuthentication(new TestingAuthenticationToken(email, null));
        SecurityContextHolder.setContext(context);
        clearInvocations(vehiculoService);
    }

    private AltaVehiculoRequestDTO requestValido() {
        AltaVehiculoRequestDTO req = new AltaVehiculoRequestDTO();
        req.setMatricula("1234ABC");
        req.setMarca("Seat");
        req.setModelo("Ibiza");
        req.setPlazas(4);
        req.setConsumo(5.2);
        req.setAnio(2020);
        req.setTipo(TipoVehiculo.COCHE);
        return req;
    }

    private VehiculoResponseDTO responseVehiculo(Long id, String matricula, String slug) {
        VehiculoResponseDTO dto = new VehiculoResponseDTO();
        dto.setId(id);
        dto.setMatricula(matricula);
        dto.setMarca("Seat");
        dto.setModelo("Ibiza");
        dto.setPlazas(4);
        dto.setConsumo(5.2);
        dto.setAnio(2020);
        dto.setTipo(TipoVehiculo.COCHE);
        dto.setSlug(slug);
        return dto;
    }
}
