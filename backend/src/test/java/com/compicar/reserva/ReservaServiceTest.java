package com.compicar.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.pago.EstadoPago;
import com.compicar.pago.Pago;
import com.compicar.pago.PagoRepository;
import com.compicar.parada.Parada;
import com.compicar.parada.ParadaRepository;
import com.compicar.parada.TipoParada;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import com.compicar.reserva.dto.ReservaRequest;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private ViajeRepository viajeRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private ParadaRepository paradaRepository;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Persona pasajero;
    private Persona conductor;
    private Viaje viaje;

    @BeforeEach
    void setUp() throws Exception {
        pasajero = new Persona("Nombre","Ape","B","pass","user@compicar.com","600000000");
        conductor = new Persona("Cond","A","B","p","driver@compicar.com","611111111");

        setId(pasajero, 2L);
        setId(conductor, 3L);

        viaje = new Viaje();
        setId(viaje, 10L);
        
        viaje.setEstado(EstadoViaje.PENDIENTE);
        viaje.setPlazasDisponibles(3);
        viaje.setPersona(conductor);
        viaje.setFechaHoraSalida(LocalDateTime.now().plusDays(1)); 
    }

    private void setId(Object entity, Long id) throws Exception {
        java.lang.reflect.Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }

    private Reserva crearReserva(Long reservaId, Persona pasajeroReserva, Viaje viajeReserva,
        EstadoReserva estado, int cantidadPlazas, Pago pago) throws Exception {
        Reserva reserva = new Reserva();
        setId(reserva, reservaId);
        reserva.setPersona(pasajeroReserva);
        reserva.setViaje(viajeReserva);
        reserva.setEstado(estado);
        reserva.setCantidadPlazas(cantidadPlazas);
        reserva.setFechaHoraReserva(LocalDateTime.now());

        if (pago != null) {
            reserva.setPago(pago);
            pago.setReserva(reserva);
        }

        return reserva;
    }

    @Test
    void crearReserva_ok() throws Exception {
        Long origenId = 101L;
        Long destinoId = 102L;
        Parada pOrigen = new Parada();
        Parada pDestino = new Parada();
        
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(paradaRepository.findById(origenId)).thenReturn(Optional.of(pOrigen));
        when(paradaRepository.findById(destinoId)).thenReturn(Optional.of(pDestino));
        
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            setId(r, 1L);
            return r;
        });

        Reserva res = reservaService.crearReserva("user@compicar.com", 10L, 1, origenId, destinoId);

        assertEquals(1L, res.getId());
        assertEquals(2, viaje.getPlazasDisponibles());
    }

    @Test
    void crearReserva_plazasInvalidas_lanza() {
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        assertThrows(IllegalArgumentException.class, () -> 
            reservaService.crearReserva("user@compicar.com", 10L, 0, null, null));
    }

    @Test
    void obtenerReservasPorPersona_mapeaADTO() {
        try {
            Persona p = pasajero;
            Reserva r = new Reserva();
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 11L);

            Parada subida = new Parada(LocalDateTime.now(), "S", TipoParada.ORIGEN, 1, viaje);
            Parada bajada = new Parada(LocalDateTime.now(), "B", TipoParada.DESTINO, 2, viaje);
            
            r.setPersona(p);
            r.setViaje(viaje);
            r.setParadaSubida(subida);
            r.setParadaBajada(bajada);
            r.setEstado(EstadoReserva.PENDIENTE);
            r.setFechaHoraReserva(LocalDateTime.now());
            r.setCantidadPlazas(1);

            when(reservaRepository.findByPersona(p)).thenReturn(List.of(r));

            var dtos = reservaService.obtenerReservasPorPersona(p);
            assertEquals(1, dtos.size());
            assertEquals(11L, dtos.get(0).getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void obtenerReservasPorViaje_ok() {
        Reserva r = new Reserva();
        try {
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 12L);
        } catch (Exception e) {}

        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(r));

        var lista = reservaService.obtenerReservasPorViaje(10L);
        assertEquals(1, lista.size());
        assertEquals(12L, lista.get(0).getId());
    }

    @Test
    void actualizarReserva_ok_y_validaPropietario() throws Exception {
        Persona p = pasajero;
        Reserva existente = new Reserva();
        setId(existente, 20L);

        Long idSubida = 101L;
        Long idBajada = 102L;
        
        Parada subida = new Parada();
        setId(subida, idSubida);
        
        Parada bajada = new Parada();
        setId(bajada, idBajada);

        existente.setPersona(p);
        existente.setViaje(viaje); 
        existente.setCantidadPlazas(1);
        existente.setParadaSubida(subida);
        existente.setParadaBajada(bajada);

        ReservaRequest datosNuevos = new ReservaRequest(
            viaje.getId(),
            2, 
            idSubida, 
            idBajada
        );

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(p));
        when(reservaRepository.findById(20L)).thenReturn(Optional.of(existente));

        when(paradaRepository.findById(idSubida)).thenReturn(Optional.of(subida));
        when(paradaRepository.findById(idBajada)).thenReturn(Optional.of(bajada));
        
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva result = reservaService.actualizarReserva("user@compicar.com", 20L, datosNuevos);
        assertEquals(20L, result.getId());
        assertEquals(2, result.getCantidadPlazas());
        assertEquals(idSubida, result.getParadaSubida().getId());
    }

    @Test
    void obtenerReservaPorId_noExiste_lanza() {
        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> reservaService.obtenerReservaPorId(99L));
    }

    @Test
    void reservaConfirmada_ok_y_permiso() {
        try {
            Reserva r = new Reserva();
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 30L);

            Viaje v = new Viaje();
            Persona owner = new Persona();
            owner.setEmail("driver@compicar.com");
            v.setPersona(owner);

            r.setViaje(v);
            when(reservaRepository.findById(30L)).thenReturn(Optional.of(r));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            Reserva res = reservaService.reservaConfirmada("driver@compicar.com", 30L);
            assertEquals(EstadoReserva.CONFIRMADA, res.getEstado());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void reservaNoPresentado_ok() {
        try {
            Reserva r = new Reserva();
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 31L);
            when(reservaRepository.findById(31L)).thenReturn(Optional.of(r));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            Reserva res = reservaService.reservaNoPresentado(31L);
            assertEquals(EstadoReserva.NO_PRESENTADO, res.getEstado());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void marcarNoPresentadoPorConductor_valida_condiciones_y_actualiza() throws Exception {
        Persona conductorLocal = new Persona();
        conductorLocal.setEmail("driver@compicar.com");
        setId(conductorLocal, 70L);

        Persona pasaj = new Persona();
        setId(pasaj, 71L);

        Viaje v = new Viaje();
        setId(v, 8L);
        v.setPersona(conductorLocal);
        v.setEstado(EstadoViaje.INICIADO);

        Reserva r = new Reserva();
        setId(r, 40L);
        r.setViaje(v);
        r.setPersona(pasaj);
        r.setEstado(EstadoReserva.PENDIENTE);

        when(personaRepository.findByEmail("driver@compicar.com")).thenReturn(Optional.of(conductorLocal));
        when(reservaRepository.findById(40L)).thenReturn(Optional.of(r));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva res = reservaService.marcarNoPresentadoPorConductor("driver@compicar.com", 40L);
        assertEquals(EstadoReserva.NO_PRESENTADO, res.getEstado());
    }

    @Test
    void obtenerReservasComoConductor_devuelveLista() {
        Reserva r = new Reserva();
        try {
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 60L);
        } catch (Exception e) {}
        
        when(reservaRepository.findPendientesParaConductor("driver@compicar.com")).thenReturn(List.of(r));
        var lista = reservaService.obtenerReservasComoConductor("driver@compicar.com");
        assertEquals(1, lista.size());
    }

    @Test
    void rechazarReserva_ok_actualizaPlazas() {
        try {
            Persona owner = new Persona();
            owner.setEmail("driver@compicar.com");

            Viaje v = new Viaje();
            v.setPersona(owner);
            v.setPlazasDisponibles(2);

            Reserva r = new Reserva();
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 70L);
            r.setViaje(v);
            r.setEstado(EstadoReserva.PENDIENTE);
            r.setCantidadPlazas(1);

            when(reservaRepository.findById(70L)).thenReturn(Optional.of(r));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
            when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));

            Reserva res = reservaService.rechazarReserva("driver@compicar.com", 70L);
            assertEquals(EstadoReserva.CANCELADA, res.getEstado());
            assertEquals(3, v.getPlazasDisponibles());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void cancelarReserva_ok_reembolsa_y_devuelvePlazas_siHanPasadoMasDe12Horas() throws Exception {
        viaje.setFechaHoraSalida(LocalDateTime.now().plusHours(13));

        Pago pago = new Pago();
        pago.setEstado(EstadoPago.PENDIENTE);

        Reserva reserva = crearReserva(
                100L, pasajero, viaje, EstadoReserva.PENDIENTE, 2, pago);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva res = reservaService.cancelarReserva("user@compicar.com", 100L);

        assertEquals(EstadoReserva.CANCELADA, res.getEstado());
        assertEquals(5, viaje.getPlazasDisponibles());
        assertEquals(EstadoPago.REEMBOLSADO, pago.getEstado());
        assertEquals(1, pasajero.getNumeroCancelaciones());

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(pagoRepository).save(pago);
        verify(personaRepository).save(pasajero);
        verify(viajeRepository).save(viaje);
        verify(reservaRepository).save(reserva);
    }

    @Test
    void cancelarReserva_ok_cobra_siFaltanMenosDe12Horas() throws Exception {
        viaje.setFechaHoraSalida(LocalDateTime.now().plusHours(11));

        Pago pago = new Pago();
        pago.setEstado(EstadoPago.PENDIENTE);

        Reserva reserva = crearReserva(
                101L, pasajero, viaje, EstadoReserva.PENDIENTE, 1, pago);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(101L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva res = reservaService.cancelarReserva("user@compicar.com", 101L);

        assertEquals(EstadoReserva.CANCELADA, res.getEstado());
        assertEquals(EstadoPago.COMPLETADO, pago.getEstado());
        assertEquals(4, viaje.getPlazasDisponibles());
        assertEquals(1, pasajero.getNumeroCancelaciones());

        verify(pagoRepository).save(pago);
        verify(personaRepository).save(pasajero);
        verify(viajeRepository).save(viaje);
        verify(notificacionRepository).save(any(Notificacion.class));
    }

    @Test
    void cancelarReserva_ok_sinPago_noLlamaAPagoRepository() throws Exception {
        viaje.setFechaHoraSalida(LocalDateTime.now().plusHours(13));

        Reserva reserva = crearReserva(
                102L, pasajero, viaje, EstadoReserva.PENDIENTE, 1, null);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(102L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(viajeRepository.save(any(Viaje.class))).thenAnswer(inv -> inv.getArgument(0));
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva res = reservaService.cancelarReserva("user@compicar.com", 102L);

        assertEquals(EstadoReserva.CANCELADA, res.getEstado());
        assertEquals(4, viaje.getPlazasDisponibles());
        assertEquals(1, pasajero.getNumeroCancelaciones());

        verify(notificacionRepository).save(any(Notificacion.class));
        verify(personaRepository).save(pasajero);
        verify(viajeRepository).save(viaje);
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void cancelarReserva_error_usuarioNoEncontrado_lanza() {
        when(personaRepository.findByEmail("missing@compicar.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservaService.cancelarReserva("missing@compicar.com", 100L));

        assertEquals("Usuario no encontrado", ex.getMessage());
    }

    @Test
    void cancelarReserva_error_reservaNoEncontrada_lanza() {
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservaService.cancelarReserva("user@compicar.com", 999L));

        assertEquals("Reserva no encontrada", ex.getMessage());
    }

    @Test
    void cancelarReserva_error_noPerteneceAlUsuario_lanza() throws Exception {
        Persona otroPasajero = new Persona("Otro", "Apellido", "B", "pass", "otro@compicar.com", "622222222");
        setId(otroPasajero, 99L);

        Reserva reserva = crearReserva(
                103L, otroPasajero, viaje, EstadoReserva.PENDIENTE, 1, null);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(103L)).thenReturn(Optional.of(reserva));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservaService.cancelarReserva("user@compicar.com", 103L));

        assertEquals("La reserva no pertenece al usuario", ex.getMessage());
    }

    @Test
    void cancelarReserva_error_yaCancelada_noHaceCambiosNiLlamaReposExtra() throws Exception {
        Reserva reserva = crearReserva(
                104L, pasajero, viaje, EstadoReserva.CANCELADA, 1, null);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(104L)).thenReturn(Optional.of(reserva));

        Reserva res = reservaService.cancelarReserva("user@compicar.com", 104L);

        assertEquals(EstadoReserva.CANCELADA, res.getEstado());
        assertEquals(3, viaje.getPlazasDisponibles());
        assertEquals(0, pasajero.getNumeroCancelaciones());

        verify(personaRepository, never()).save(any(Persona.class));
        verifyNoInteractions(viajeRepository, pagoRepository, notificacionRepository);
    }

    @Test
    void crearReserva_plazasNulas_lanza() {
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            reservaService.crearReserva("user@compicar.com", 10L, null, 101L, 102L));

        assertEquals("Debes reservar al menos 1 plaza.", ex.getMessage());
    }

    @Test
    void crearReserva_viajeNoPendiente_lanza() {
        viaje.setEstado(EstadoViaje.INICIADO);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            reservaService.crearReserva("user@compicar.com", 10L, 1, 101L, 102L));

        assertEquals("El viaje no está disponible para reservas (estado: INICIADO)", ex.getMessage());
    }

    @Test
    void crearReserva_sinPlazasDisponibles_lanza() {
        viaje.setPlazasDisponibles(1);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            reservaService.crearReserva("user@compicar.com", 10L, 2, 101L, 102L));

        assertEquals("Solo quedan 1 plazas disponibles.", ex.getMessage());
    }

    @Test
    void crearReserva_noPuedeReservarSuPropioViaje_lanza() {
        viaje.setPersona(pasajero);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            reservaService.crearReserva("user@compicar.com", 10L, 1, 101L, 102L));

        assertEquals("No puedes reservar tu propio viaje", ex.getMessage());
    }

    @Test
    void actualizarReserva_error_noPerteneceAlUsuario_lanza() throws Exception {
        Persona otro = new Persona("Otro", "Ape", "B", "pass", "otro@compicar.com", "622222222");
        setId(otro, 99L);

        Reserva existente = crearReserva(20L, otro, viaje, EstadoReserva.PENDIENTE, 1, null);

        ReservaRequest datosNuevos = new ReservaRequest(viaje.getId(), 1, 101L, 102L);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(20L)).thenReturn(Optional.of(existente));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            reservaService.actualizarReserva("user@compicar.com", 20L, datosNuevos));

        assertEquals("La reserva no pertenece al usuario", ex.getMessage());
    }

    @Test
    void actualizarReserva_error_menosDe12Horas_lanza() throws Exception {
        viaje.setFechaHoraSalida(LocalDateTime.now().plusHours(11));

        Reserva existente = crearReserva(21L, pasajero, viaje, EstadoReserva.PENDIENTE, 1, null);
        ReservaRequest datosNuevos = new ReservaRequest(viaje.getId(), 1, 101L, 102L);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(21L)).thenReturn(Optional.of(existente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            reservaService.actualizarReserva("user@compicar.com", 21L, datosNuevos));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("No se puede modificar la reserva a falta de menos de 12 horas para el viaje", ex.getReason());
    }

    @Test
    void actualizarReserva_error_plazasNulas_lanza() throws Exception {
        Reserva existente = crearReserva(22L, pasajero, viaje, EstadoReserva.PENDIENTE, 1, null);
        ReservaRequest datosNuevos = new ReservaRequest(viaje.getId(), null, 101L, 102L);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(22L)).thenReturn(Optional.of(existente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            reservaService.actualizarReserva("user@compicar.com", 22L, datosNuevos));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("El número de plazas no puede estar vacío", ex.getReason());
    }

    @Test
    void actualizarReserva_error_noHayPlazasSuficientesParaAmpliar_lanza() throws Exception {
        viaje.setPlazasDisponibles(0);

        Reserva existente = crearReserva(23L, pasajero, viaje, EstadoReserva.PENDIENTE, 1, null);
        ReservaRequest datosNuevos = new ReservaRequest(viaje.getId(), 3, 101L, 102L);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(23L)).thenReturn(Optional.of(existente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            reservaService.actualizarReserva("user@compicar.com", 23L, datosNuevos));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("No hay suficientes plazas disponibles para ampliar la reserva", ex.getReason());
    }

    @Test
    void actualizarReserva_error_paradasNulas_lanza() throws Exception {
        Reserva existente = crearReserva(24L, pasajero, viaje, EstadoReserva.PENDIENTE, 1, null);
        ReservaRequest datosNuevos = new ReservaRequest(viaje.getId(), 1, null, null);

        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(reservaRepository.findById(24L)).thenReturn(Optional.of(existente));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            reservaService.actualizarReserva("user@compicar.com", 24L, datosNuevos));

        assertEquals(HttpStatus.BAD_REQUEST.value(), ex.getStatusCode().value());
        assertEquals("Las paradas no pueden ser nulas", ex.getReason());
    }

    @Test
    void reservaConfirmada_error_sinPermiso_lanza() throws Exception {
        Reserva r = new Reserva();
        setId(r, 30L);

        Viaje v = new Viaje();
        Persona owner = new Persona();
        owner.setEmail("driver@compicar.com");
        v.setPersona(owner);
        r.setViaje(v);

        when(reservaRepository.findById(30L)).thenReturn(Optional.of(r));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            reservaService.reservaConfirmada("otro@compicar.com", 30L));

        assertEquals("No tienes permiso para confirmar esta reserva", ex.getMessage());
    }

    @Test
    void rechazarReserva_error_sinPermiso_lanza() throws Exception {
        Persona owner = new Persona();
        owner.setEmail("driver@compicar.com");

        Viaje v = new Viaje();
        v.setPersona(owner);

        Reserva r = new Reserva();
        setId(r, 40L);
        r.setViaje(v);
        r.setEstado(EstadoReserva.PENDIENTE);
        r.setCantidadPlazas(1);

        when(reservaRepository.findById(40L)).thenReturn(Optional.of(r));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            reservaService.rechazarReserva("otro@compicar.com", 40L));

        assertEquals("No tienes permiso para rechazar esta reserva", ex.getMessage());
    }

    @Test
    void rechazarReserva_estadoNoPendiente_noCambiaPlazas() throws Exception {
        Persona owner = new Persona();
        owner.setEmail("driver@compicar.com");

        Viaje v = new Viaje();
        setId(v, 50L);
        v.setPersona(owner);
        v.setPlazasDisponibles(2);

        Reserva r = new Reserva();
        setId(r, 51L);
        r.setViaje(v);
        r.setEstado(EstadoReserva.CONFIRMADA);
        r.setCantidadPlazas(1);

        when(reservaRepository.findById(51L)).thenReturn(Optional.of(r));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva res = reservaService.rechazarReserva("driver@compicar.com", 51L);

        assertEquals(EstadoReserva.CONFIRMADA, res.getEstado());
        assertEquals(2, v.getPlazasDisponibles());
        verify(viajeRepository, never()).save(any(Viaje.class));
    }

    @Test
    void marcarNoPresentadoPorConductor_error_siNoEsElConductorDelViaje() throws Exception {
        Persona conductorAutenticado = new Persona();
        conductorAutenticado.setEmail("driver@compicar.com");
        setId(conductorAutenticado, 70L);

        Persona otroConductor = new Persona();
        setId(otroConductor, 80L);

        Persona pasaj = new Persona();
        setId(pasaj, 71L);

        Viaje v = new Viaje();
        setId(v, 8L);
        v.setPersona(otroConductor); // No coincide con conductor autenticado
        v.setEstado(EstadoViaje.INICIADO);

        Reserva r = crearReserva(
                40L, pasaj, v, EstadoReserva.PENDIENTE, 1, null);

        when(personaRepository.findByEmail("driver@compicar.com")).thenReturn(Optional.of(conductorAutenticado));
        when(reservaRepository.findById(40L)).thenReturn(Optional.of(r));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservaService.marcarNoPresentadoPorConductor("driver@compicar.com", 40L));

        assertEquals("Solo el conductor del viaje puede marcar no presentado", ex.getMessage());
        verify(personaRepository, never()).save(any(Persona.class));
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void marcarNoPresentadoPorConductor_error_siViajeNoEstaIniciado() throws Exception {
        Persona conductorLocal = new Persona();
        conductorLocal.setEmail("driver@compicar.com");
        setId(conductorLocal, 70L);

        Persona pasaj = new Persona();
        setId(pasaj, 71L);

        Viaje v = new Viaje();
        setId(v, 8L);
        v.setPersona(conductorLocal);
        v.setEstado(EstadoViaje.PENDIENTE); // Rama de error

        Reserva r = crearReserva(
                41L, pasaj, v, EstadoReserva.PENDIENTE, 1, null);

        when(personaRepository.findByEmail("driver@compicar.com")).thenReturn(Optional.of(conductorLocal));
        when(reservaRepository.findById(41L)).thenReturn(Optional.of(r));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservaService.marcarNoPresentadoPorConductor("driver@compicar.com", 41L));

        assertEquals("Solo se puede marcar no presentado cuando el viaje está INICIADO", ex.getMessage());
        verify(personaRepository, never()).save(any(Persona.class));
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void marcarNoPresentadoPorConductor_error_siReservaYaCancelada() throws Exception {
        Persona conductorLocal = new Persona();
        conductorLocal.setEmail("driver@compicar.com");
        setId(conductorLocal, 70L);

        Persona pasaj = new Persona();
        setId(pasaj, 71L);

        Viaje v = new Viaje();
        setId(v, 8L);
        v.setPersona(conductorLocal);
        v.setEstado(EstadoViaje.INICIADO);

        Reserva r = crearReserva(
                42L, pasaj, v, EstadoReserva.CANCELADA, 1, null);

        when(personaRepository.findByEmail("driver@compicar.com")).thenReturn(Optional.of(conductorLocal));
        when(reservaRepository.findById(42L)).thenReturn(Optional.of(r));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservaService.marcarNoPresentadoPorConductor("driver@compicar.com", 42L));

        assertEquals("La reserva ya está cancelada o marcada como no presentado", ex.getMessage());
        verify(personaRepository, never()).save(any(Persona.class));
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }

    @Test
    void marcarNoPresentadoPorConductor_error_siReservaYaNoPresentado() throws Exception {
        Persona conductorLocal = new Persona();
        conductorLocal.setEmail("driver@compicar.com");
        setId(conductorLocal, 70L);

        Persona pasaj = new Persona();
        setId(pasaj, 71L);

        Viaje v = new Viaje();
        setId(v, 8L);
        v.setPersona(conductorLocal);
        v.setEstado(EstadoViaje.INICIADO);

        Reserva r = crearReserva(
                43L, pasaj, v, EstadoReserva.NO_PRESENTADO, 1, null);

        when(personaRepository.findByEmail("driver@compicar.com")).thenReturn(Optional.of(conductorLocal));
        when(reservaRepository.findById(43L)).thenReturn(Optional.of(r));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                reservaService.marcarNoPresentadoPorConductor("driver@compicar.com", 43L));

        assertEquals("La reserva ya está cancelada o marcada como no presentado", ex.getMessage());
        verify(personaRepository, never()).save(any(Persona.class));
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(reservaRepository, never()).save(any(Reserva.class));
    }
}