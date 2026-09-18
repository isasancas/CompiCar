package com.compicar.parada;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.correo.CorreoService;
import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.parada.dto.SolicitudNuevaParadaRequest;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.EstadoReserva;
import com.compicar.reserva.Reserva;
import com.compicar.reserva.ReservaRepository;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.compicar.viajeRecurrente.ViajeRecurrente;
import com.compicar.viajeRecurrente.ViajeRecurrenteRepository;

@ExtendWith(MockitoExtension.class)
class ParadaServiceTest {

    @Mock
    private ParadaRepository paradaRepository;
    @Mock
    private ViajeRepository viajeRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private ViajeRecurrenteRepository viajeRecurrenteRepository;
    @Mock
    private CorreoService correoService;

    @InjectMocks
    private ParadaServiceImpl paradaService;

    private Viaje viaje;
    private Persona pasajero;
    private Persona conductor;
    private Reserva reserva;

    @BeforeEach
    void setUp() {
        pasajero = new Persona();
        ReflectionTestUtils.setField(pasajero, "id", 10L);
        pasajero.setEmail("pasajero@compicar.com");
        pasajero.setNombre("Ana");

        conductor = new Persona();
        ReflectionTestUtils.setField(conductor, "id", 20L);
        conductor.setEmail("conductor@compicar.com");
        conductor.setNombre("Carlos");

        viaje = new Viaje();
        ReflectionTestUtils.setField(viaje, "id", 99L);
        viaje.setEstado(EstadoViaje.PENDIENTE);
        viaje.setFechaHoraSalida(LocalDateTime.of(2026, 5, 10, 9, 0));
        viaje.setPersona(conductor);
        viaje.setParadas(new ArrayList<>());

        reserva = new Reserva();
        ReflectionTestUtils.setField(reserva, "id", 50L);
        reserva.setPersona(pasajero);
        reserva.setViaje(viaje);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        
        Parada pSubida = parada("Madrid, España", TipoParada.ORIGEN, 1);
        Parada pBajada = parada("Sevilla, España", TipoParada.DESTINO, 2);
        reserva.setParadaSubida(pSubida);
        reserva.setParadaBajada(pBajada);
    }

    // --- PRUEBAS CREAR Y AÑADIR PARADAS ---

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

    // --- PRUEBAS SOLICITAR NUEVA PARADA ---

    @Test
    void solicitarNuevaParada_solicitudInvalida_lanza400() {
        assertThrows(ResponseStatusException.class, () -> paradaService.solicitarNuevaParada("email", null));
        
        SolicitudNuevaParadaRequest req1 = new SolicitudNuevaParadaRequest(null, "Cordoba", LocalDateTime.now(), null, null, false);
        assertThrows(ResponseStatusException.class, () -> paradaService.solicitarNuevaParada("email", req1));

        SolicitudNuevaParadaRequest req2 = new SolicitudNuevaParadaRequest(50L, "  ", LocalDateTime.now(), null, null, false);
        assertThrows(ResponseStatusException.class, () -> paradaService.solicitarNuevaParada("email", req2));
    }

    @Test
    void solicitarNuevaParada_pasajeroNoExiste_lanza401() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba", LocalDateTime.now(), null, null, false);
        when(personaRepository.findByEmail("noexisto@compicar.com")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.solicitarNuevaParada("noexisto@compicar.com", req));
        assertEquals(401, ex.getStatusCode().value());
    }

    @Test
    void solicitarNuevaParada_reservaNoExiste_lanza404() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba", LocalDateTime.now(), null, null, false);
        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(50L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.solicitarNuevaParada(pasajero.getEmail(), req));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void solicitarNuevaParada_reservaNoPerteneceAlPasajero_lanza403() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba", LocalDateTime.now(), null, null, false);
        Persona otro = new Persona();
        ReflectionTestUtils.setField(otro, "id", 999L);
        reserva.setPersona(otro);

        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.solicitarNuevaParada(pasajero.getEmail(), req));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void solicitarNuevaParada_reservaNoConfirmada_lanza400() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba", LocalDateTime.now(), null, null, false);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.solicitarNuevaParada(pasajero.getEmail(), req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void solicitarNuevaParada_sinViajeNiRecurrente_lanza400() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba", LocalDateTime.now(), null, null, false);
        reserva.setViaje(null);
        reserva.setViajeRecurrente(null);

        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.solicitarNuevaParada(pasajero.getEmail(), req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void solicitarNuevaParada_todaRecurrenciaSinOcurrencias_lanza400() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba", LocalDateTime.now(), null, null, true);

        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(viajeRecurrenteRepository.findByViajePadreId(99L)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.solicitarNuevaParada(pasajero.getEmail(), req));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void solicitarNuevaParada_solicitudPendienteExiste_lanza409() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba", LocalDateTime.now(), null, null, false);

        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));

        Notificacion notifExistente = new Notificacion("{\"reservaId\":50, \"localizacion\":\"Cordoba\"}", conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA);
        when(notificacionRepository.findByReceptorEmailAndTipo(conductor.getEmail(), TipoNotificacion.SOLICITUD_NUEVA_PARADA))
                .thenReturn(List.of(notifExistente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.solicitarNuevaParada(pasajero.getEmail(), req));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void solicitarNuevaParada_exito_viajeNormal() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba, España", LocalDateTime.now(), BigDecimal.valueOf(37.8), BigDecimal.valueOf(-4.7), false);

        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(notificacionRepository.findByReceptorEmailAndTipo(conductor.getEmail(), TipoNotificacion.SOLICITUD_NUEVA_PARADA))
                .thenReturn(List.of());
        when(notificacionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Notificacion> resultado = paradaService.solicitarNuevaParada(pasajero.getEmail(), req);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(TipoNotificacion.SOLICITUD_NUEVA_PARADA, resultado.get(0).getTipo());

        verify(correoService).sendSolicitudNuevaParadaConductor(
                eq(conductor.getEmail()),
                eq("Carlos"),
                eq("Ana"),
                eq("Madrid"),
                eq("Sevilla"),
                eq("Cordoba"),
                anyString()
        );
    }

    @Test
    void solicitarNuevaParada_exito_todaRecurrencia() {
        SolicitudNuevaParadaRequest req = new SolicitudNuevaParadaRequest(50L, "Cordoba", LocalDateTime.now(), null, null, true);

        ViajeRecurrente vr1 = new ViajeRecurrente();
        ReflectionTestUtils.setField(vr1, "id", 101L);
        vr1.setViajePadre(viaje);
        vr1.setPersona(conductor);

        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(viajeRecurrenteRepository.findByViajePadreId(99L)).thenReturn(List.of(vr1));

        Reserva reservaRec1 = new Reserva();
        ReflectionTestUtils.setField(reservaRec1, "id", 51L);
        reservaRec1.setPersona(pasajero);
        reservaRec1.setViajeRecurrente(vr1);
        reservaRec1.setEstado(EstadoReserva.CONFIRMADA);

        when(reservaRepository.findReservasConfirmadasDeRecurrencia(eq(99L), eq(pasajero.getId()), any()))
                .thenReturn(List.of(reservaRec1));
        when(notificacionRepository.findByReceptorEmailAndTipo(conductor.getEmail(), TipoNotificacion.SOLICITUD_NUEVA_PARADA))
                .thenReturn(List.of());
        when(notificacionRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Notificacion> resultado = paradaService.solicitarNuevaParada(pasajero.getEmail(), req);

        assertNotNull(resultado);
        assertEquals(2, resultado.size()); // Reserva padre + Reserva de la recurrencia
    }

    // --- PRUEBAS TENER SOLICITUD PENDIENTE ---

    @Test
    void tieneSolicitudNuevaParadaPendiente_reservaNoExiste_lanza404() {
        when(reservaRepository.findById(50L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.tieneSolicitudNuevaParadaPendiente(pasajero.getEmail(), 50L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void tieneSolicitudNuevaParadaPendiente_reservaPasajeroDistinto_lanza403() {
        Persona otro = new Persona();
        ReflectionTestUtils.setField(otro, "id", 888L);
        reserva.setPersona(otro);

        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.tieneSolicitudNuevaParadaPendiente(pasajero.getEmail(), 50L));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void tieneSolicitudNuevaParadaPendiente_devuelveCorrectamente() {
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(personaRepository.findByEmail(pasajero.getEmail())).thenReturn(Optional.of(pasajero));
        when(notificacionRepository.findByReceptorEmailAndTipo(conductor.getEmail(), TipoNotificacion.SOLICITUD_NUEVA_PARADA))
                .thenReturn(List.of());

        boolean pend = paradaService.tieneSolicitudNuevaParadaPendiente(pasajero.getEmail(), 50L);
        assertFalse(pend);
    }

    // --- PRUEBAS ACEPTAR SOLICITUD NUEVA PARADA ---

    @Test
    void aceptarSolicitudNuevaParada_notificacionNoExiste_lanza404() {
        when(notificacionRepository.findById(100L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.aceptarSolicitudNuevaParada(conductor.getEmail(), 100L));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void aceptarSolicitudNuevaParada_tipoIncorrecto_lanza409() {
        Notificacion notif = new Notificacion("msg", conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA_ACEPTADA);
        when(notificacionRepository.findById(100L)).thenReturn(Optional.of(notif));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.aceptarSolicitudNuevaParada(conductor.getEmail(), 100L));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void aceptarSolicitudNuevaParada_conductorDistinto_lanza403() {
        String msg = "{\"reservaId\":50, \"localizacion\":\"Cordoba\"}";
        Notificacion notif = new Notificacion(msg, conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA);

        Persona otroConductor = new Persona();
        ReflectionTestUtils.setField(otroConductor, "id", 999L);
        otroConductor.setEmail("otro@compicar.com");

        when(notificacionRepository.findById(100L)).thenReturn(Optional.of(notif));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(personaRepository.findByEmail("otro@compicar.com")).thenReturn(Optional.of(otroConductor));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.aceptarSolicitudNuevaParada("otro@compicar.com", 100L));
        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void aceptarSolicitudNuevaParada_mensajeInvalido_lanza400() {
        Notificacion notif = new Notificacion("mensaje_invalido_json", conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA);
        when(notificacionRepository.findById(100L)).thenReturn(Optional.of(notif));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.aceptarSolicitudNuevaParada(conductor.getEmail(), 100L));
        assertEquals(400, ex.getStatusCode().value());
    }

    @Test
    void aceptarSolicitudNuevaParada_sinParadaDestino_lanza400() {
        String msg = "{\"reservaId\":50, \"localizacion\":\"Cordoba\"}";
        Notificacion notif = new Notificacion(msg, conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA);

        when(notificacionRepository.findById(100L)).thenReturn(Optional.of(notif));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> paradaService.aceptarSolicitudNuevaParada(conductor.getEmail(), 100L));
        assertEquals(400, ex.getStatusCode().value());
        assertEquals("El viaje no tiene destino", ex.getReason());
    }

    @Test
    void aceptarSolicitudNuevaParada_exito_viajeNormal_conRecalculoKm() {
        String msg = "{\"reservaId\":50, \"localizacion\":\"Cordoba\", \"latitud\":37.88, \"longitud\":-4.77}";
        Notificacion notif = new Notificacion(msg, conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA);

        Parada origen = parada("Madrid", TipoParada.ORIGEN, 1);
        origen.setLatitud(BigDecimal.valueOf(40.41));
        origen.setLongitud(BigDecimal.valueOf(-3.70));

        Parada destino = parada("Sevilla", TipoParada.DESTINO, 2);
        destino.setLatitud(BigDecimal.valueOf(37.38));
        destino.setLongitud(BigDecimal.valueOf(-5.98));

        viaje.getParadas().addAll(List.of(origen, destino));

        when(notificacionRepository.findById(100L)).thenReturn(Optional.of(notif));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));

        Notificacion resultado = paradaService.aceptarSolicitudNuevaParada(conductor.getEmail(), 100L);

        assertEquals(TipoNotificacion.SOLICITUD_NUEVA_PARADA_ACEPTADA, resultado.getTipo());
        assertTrue(resultado.isLeida());
        assertEquals(3, destino.getOrden()); // Incrementa el orden del destino
        verify(paradaRepository).save(any(Parada.class));
        verify(viajeRepository).save(viaje);
        verify(correoService).sendSolicitudParadaAceptadaPasajero(
                eq(pasajero.getEmail()),
                eq("Ana"),
                eq("Carlos"),
                eq("Madrid"),
                eq("Sevilla"),
                eq("Cordoba"),
                anyString()
        );
    }

    @Test
    void aceptarSolicitudNuevaParada_exito_viajeRecurrente() {
        ViajeRecurrente vr = new ViajeRecurrente();
        ReflectionTestUtils.setField(vr, "id", 200L);
        vr.setPersona(conductor);
        vr.setFechaHoraSalida(LocalDateTime.of(2026, 6, 1, 8, 0));
        vr.setParadas(new ArrayList<>());

        Parada origen = parada("Madrid", TipoParada.ORIGEN, 1);
        Parada destino = parada("Sevilla", TipoParada.DESTINO, 2);
        vr.getParadas().addAll(List.of(origen, destino));

        reserva.setViaje(null);
        reserva.setViajeRecurrente(vr);

        String msg = "{\"reservaId\":50, \"localizacion\":\"Cordoba\"}";
        Notificacion notif = new Notificacion(msg, conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA);

        when(notificacionRepository.findById(100L)).thenReturn(Optional.of(notif));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));

        Notificacion resultado = paradaService.aceptarSolicitudNuevaParada(conductor.getEmail(), 100L);

        assertNotNull(resultado);
        verify(viajeRecurrenteRepository).save(vr);
    }

    // --- PRUEBAS RECHAZAR SOLICITUD NUEVA PARADA ---

    @Test
    void rechazarSolicitudNuevaParada_exito() {
        String msg = "{\"reservaId\":50, \"localizacion\":\"Cordoba\"}";
        Notificacion notif = new Notificacion(msg, conductor, TipoNotificacion.SOLICITUD_NUEVA_PARADA);

        when(notificacionRepository.findById(100L)).thenReturn(Optional.of(notif));
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(personaRepository.findByEmail(conductor.getEmail())).thenReturn(Optional.of(conductor));

        Notificacion resultado = paradaService.rechazarSolicitudNuevaParada(conductor.getEmail(), 100L);

        assertEquals(TipoNotificacion.SOLICITUD_NUEVA_PARADA_RECHAZADA, resultado.getTipo());
        assertTrue(resultado.isLeida());
        verify(notificacionRepository).save(notif);
        verify(correoService).sendSolicitudParadaRechazadaPasajero(
                eq(pasajero.getEmail()),
                eq("Ana"),
                eq("Carlos"),
                eq("Madrid"),
                eq("Sevilla"),
                eq("Cordoba"),
                anyString()
        );
    }

    // --- PRUEBAS CONSULTAS DE PARADAS Y TOP 5 ---

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

    @Test
    void obtenerTop5Localizaciones_ok_devuelveLista() {
        Object[] loc1 = new Object[]{"Madrid", 15L};
        Object[] loc2 = new Object[]{"Barcelona", 10L};
        when(paradaRepository.findTop5Localizaciones()).thenReturn(List.of(loc1, loc2));

        List<Object[]> result = paradaService.obtenerTop5Localizaciones();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Madrid", result.get(0)[0]);
        assertEquals(15L, result.get(0)[1]);
        verify(paradaRepository).findTop5Localizaciones();
    }

    @Test
    void obtenerTop5Localizaciones_vacio_devuelveListaVacia() {
        when(paradaRepository.findTop5Localizaciones()).thenReturn(List.of());

        List<Object[]> result = paradaService.obtenerTop5Localizaciones();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(paradaRepository).findTop5Localizaciones();
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