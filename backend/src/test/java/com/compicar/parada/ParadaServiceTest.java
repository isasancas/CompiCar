package com.compicar.parada;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ParadaServiceTest {

    @Mock
    private ParadaRepository paradaRepository;
    @Mock
    private ViajeRepository viajeRepository;

    @InjectMocks
    private ParadaServiceImpl paradaService;

    private Viaje viaje;

    @BeforeEach
    void setUp() {
        viaje = new Viaje();
        ReflectionTestUtils.setField(viaje, "id", 99L);
        viaje.setEstado(EstadoViaje.PENDIENTE);
        viaje.setFechaHoraSalida(LocalDateTime.of(2026, 5, 10, 9, 0));
        viaje.setParadas(new ArrayList<>());
    }

    @Test
    void crearParada_ok_asignaViajeYGuarda() {
        Parada nueva = parada("Sevilla", TipoParada.ORIGEN, 1);
        when(viajeRepository.findById(99L)).thenReturn(Optional.of(viaje));
        when(paradaRepository.save(any(Parada.class))).thenAnswer(inv -> inv.getArgument(0));

        Parada result = paradaService.crearParada(99L, nueva);

        assertNotNull(result);
        assertEquals(viaje, result.getViaje());
        verify(paradaRepository).save(nueva);
    }

    @Test
    void crearParada_viajeNoExiste_lanza404() {
        when(viajeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.crearParada(99L, parada("Sevilla", TipoParada.ORIGEN, 1)));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Viaje no encontrado", ex.getReason());
        verify(paradaRepository, never()).save(any());
    }

    @Test
    void anadirParadas_ok_conListaInicialNoNula() {
        Parada existente = parada("Sevilla", TipoParada.ORIGEN, 1);
        existente.setViaje(viaje);
        viaje.setParadas(new ArrayList<>(List.of(existente)));

        Parada p2 = parada("Jerez", TipoParada.INTERMEDIA, 2);
        Parada p3 = parada("Cadiz", TipoParada.DESTINO, 3);

        when(viajeRepository.findById(99L)).thenReturn(Optional.of(viaje));
        when(paradaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Viaje result = paradaService.anadirParadas(99L, List.of(p2, p3));

        assertNotNull(result);
        assertEquals(3, result.getParadas().size());
        assertEquals(viaje, p2.getViaje());
        assertEquals(viaje, p3.getViaje());

        verify(paradaRepository).saveAll(argThat(iterable -> {
            List<Parada> lista = StreamSupport.stream(iterable.spliterator(), false).toList();
            return lista.size() == 2 && lista.contains(p2) && lista.contains(p3);
        }));
    }

    @Test
    void anadirParadas_ok_inicializaListaSiEsNull() {
        viaje.setParadas(null);

        Parada p1 = parada("Sevilla", TipoParada.ORIGEN, 1);
        Parada p2 = parada("Cadiz", TipoParada.DESTINO, 2);

        when(viajeRepository.findById(99L)).thenReturn(Optional.of(viaje));
        when(paradaRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        Viaje result = paradaService.anadirParadas(99L, List.of(p1, p2));

        assertNotNull(result.getParadas());
        assertEquals(2, result.getParadas().size());
        assertEquals(viaje, p1.getViaje());
        assertEquals(viaje, p2.getViaje());
    }

    @Test
    void anadirParadas_viajeNoExiste_lanza404() {
        when(viajeRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.anadirParadas(99L, List.of(parada("X", TipoParada.ORIGEN, 1))));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Viaje no encontrado", ex.getReason());
        verify(paradaRepository, never()).saveAll(anyList());
    }

    @Test
    void anadirParadas_limite_listaVacia_retornaViajeSinCambios() {
        viaje.setParadas(new ArrayList<>(List.of(parada("Sevilla", TipoParada.ORIGEN, 1))));
        when(viajeRepository.findById(99L)).thenReturn(Optional.of(viaje));
        when(paradaRepository.saveAll(List.of())).thenReturn(List.of());

        Viaje result = paradaService.anadirParadas(99L, List.of());

        assertEquals(1, result.getParadas().size());
        verify(paradaRepository).saveAll(List.of());
    }

    @Test
    void obtenerParadasPorViaje_ok_devuelveResultadoRepositorio() {
        Parada p1 = parada("Sevilla", TipoParada.ORIGEN, 1);
        Parada p2 = parada("Cadiz", TipoParada.DESTINO, 2);
        when(paradaRepository.findByViaje(viaje)).thenReturn(List.of(p1, p2));

        List<Parada> result = paradaService.obtenerParadasPorViaje(viaje);

        assertEquals(2, result.size());
        assertEquals("Sevilla", result.get(0).getLocalizacion());
        verify(paradaRepository).findByViaje(viaje);
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