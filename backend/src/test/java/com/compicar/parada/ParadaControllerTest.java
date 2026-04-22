package com.compicar.parada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ParadaControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Mock
    private ParadaService paradaService;
    @Mock
    private ViajeRepository viajeRepository;

    @InjectMocks
    private ParadaController paradaController;

    private Viaje viaje;
    private Parada p1;
    private Parada p2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paradaController).build();

        viaje = new Viaje();
        ReflectionTestUtils.setField(viaje, "id", 99L);
        viaje.setEstado(EstadoViaje.PENDIENTE);
        viaje.setFechaHoraSalida(LocalDateTime.of(2026, 5, 10, 9, 0));
        viaje.setParadas(new ArrayList<>());

        p1 = parada("Sevilla", TipoParada.ORIGEN, 1);
        p2 = parada("Cadiz", TipoParada.DESTINO, 2);
    }

    @Test
    void anadirParadas_ok_devuelveLista() throws Exception {
        Viaje retorno = new Viaje();
        retorno.setParadas(List.of(p1, p2));

        when(paradaService.anadirParadas(org.mockito.ArgumentMatchers.eq(99L), anyList())).thenReturn(retorno);

        mockMvc.perform(post("/api/paradas/api/viajes/99/paradas")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(p1, p2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].localizacion").value("Sevilla"))
                .andExpect(jsonPath("$[1].localizacion").value("Cadiz"));

        verify(paradaService).anadirParadas(
        org.mockito.ArgumentMatchers.eq(99L),
        argThat(lista ->
                lista.size() == 2
                        && "Sevilla".equals(lista.get(0).getLocalizacion())
                        && TipoParada.ORIGEN == lista.get(0).getTipo()
                        && Integer.valueOf(1).equals(lista.get(0).getOrden())
                        && "Cadiz".equals(lista.get(1).getLocalizacion())
                        && TipoParada.DESTINO == lista.get(1).getTipo()
                        && Integer.valueOf(2).equals(lista.get(1).getOrden())
                )
        );
    }

    @Test
    void anadirParadas_limite_listaVacia_ok() throws Exception {
        Viaje retorno = new Viaje();
        retorno.setParadas(List.of());

        when(paradaService.anadirParadas(org.mockito.ArgumentMatchers.eq(99L), anyList())).thenReturn(retorno);

        mockMvc.perform(post("/api/paradas/api/viajes/99/paradas")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void anadirParadas_errorServicio_404() throws Exception {
        when(paradaService.anadirParadas(org.mockito.ArgumentMatchers.eq(99L), anyList()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        mockMvc.perform(post("/api/paradas/api/viajes/99/paradas")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(p1, p2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void crearParada_llamadaDirecta_ok() {
        Parada creada = parada("Jerez", TipoParada.INTERMEDIA, 2);
        when(paradaService.crearParada(99L, p1)).thenReturn(creada);

        Parada result = paradaController.crearParada(99L, p1);

        assertNotNull(result);
        assertEquals("Jerez", result.getLocalizacion());
        verify(paradaService).crearParada(99L, p1);
    }

    @Test
    void crearParada_llamadaDirecta_error404() {
        when(paradaService.crearParada(99L, p1))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));

        try {
            paradaController.crearParada(99L, p1);
        } catch (ResponseStatusException ex) {
            assertEquals(404, ex.getStatusCode().value());
            assertEquals("Viaje no encontrado", ex.getReason());
        }
    }

    @Test
    void obtenerParadasPorViaje_ok() throws Exception {
        when(viajeRepository.findById(99L)).thenReturn(Optional.of(viaje));
        when(paradaService.obtenerParadasPorViaje(viaje)).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/paradas/viaje/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].localizacion").value("Sevilla"))
                .andExpect(jsonPath("$[1].tipo").value("DESTINO"));
    }

    @Test
    void obtenerParadasPorViaje_viajeNoExiste_404() throws Exception {
        when(viajeRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/paradas/viaje/99"))
                .andExpect(status().isNotFound());
    }

    private Parada parada(String localizacion, TipoParada tipo, Integer orden) {
        Parada p = new Parada();
        p.setLocalizacion(localizacion);
        p.setTipo(tipo);
        p.setOrden(orden);
        p.setFechaHora(LocalDateTime.of(2026, 5, 10, 9, 0));
        return p;
    }
}
