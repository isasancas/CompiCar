package com.compicar.reserva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.compicar.parada.Parada;
import com.compicar.parada.TipoParada;
import com.compicar.persona.Persona;
import com.compicar.viaje.EstadoViaje;
import com.compicar.viaje.Viaje;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private com.compicar.persona.PersonaRepository personaRepository;

    @Mock
    private com.compicar.viaje.ViajeRepository viajeRepository;

    @Mock
    private com.compicar.pago.PagoRepository pagoRepository;

    @Mock
    private com.compicar.notificacion.NotificacionRepository notificacionRepository;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Persona pasajero;
    private Persona conductor;
    private Viaje viaje;

    @BeforeEach
    void setUp() {
        pasajero = new Persona("Nombre","Ape","B","pass","user@compicar.com","600000000");
        conductor = new Persona("Cond","A","B","p","driver@compicar.com","611111111");

        try {
            java.lang.reflect.Field pid = Persona.class.getDeclaredField("id");
            pid.setAccessible(true);
            pid.set(pasajero, 2L);
            pid.set(conductor, 3L);
        } catch (Exception e) {
            // ignore
        }

        viaje = new Viaje();
        java.lang.reflect.Field idField;
        try {
            idField = Viaje.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(viaje, 10L);
        } catch (Exception e) {
            // ignore
        }
        viaje.setEstado(EstadoViaje.PENDIENTE);
        viaje.setPlazasDisponibles(3);
        viaje.setParadas(List.of(
            new Parada(LocalDateTime.now().plusDays(1), "Origen", TipoParada.ORIGEN, 1, viaje),
            new Parada(LocalDateTime.now().plusDays(1), "Destino", TipoParada.DESTINO, 2, viaje)
        ));
        viaje.setPersona(conductor);
    }

    @Test
    void crearReserva_ok() throws Exception {
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            java.lang.reflect.Field f = Reserva.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, 1L);
            return r;
        });

        Reserva res = reservaService.crearReserva("user@compicar.com", 10L, 1);

        assertEquals(1L, res.getId());
        // plazas disponibles decreased
        assertEquals(2, viaje.getPlazasDisponibles());
    }

    @Test
    void crearReserva_plazasInvalidas_lanza() {
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        assertThrows(IllegalArgumentException.class, () -> reservaService.crearReserva("user@compicar.com", 10L, 0));
    }

    @Test
    void obtenerReservasPorPersona_mapeaADTO() {
        try {
            Persona p = pasajero;
            java.lang.reflect.Field pid = Persona.class.getDeclaredField("id");
            pid.setAccessible(true);
            pid.set(p, 2L);

            Reserva r = new Reserva();
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 11L);

            Parada subida = new Parada(LocalDateTime.now(), "S", TipoParada.ORIGEN, 1, viaje);
            Parada bajada = new Parada(LocalDateTime.now(), "B", TipoParada.DESTINO, 2, viaje);
            java.lang.reflect.Field psId = Parada.class.getDeclaredField("id");
            psId.setAccessible(true);
            psId.set(subida, 101L);
            psId.set(bajada, 102L);

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
        try {
            Reserva r = new Reserva();
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 12L);

            when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
            when(reservaRepository.findByViajeId(10L)).thenReturn(List.of(r));

            var lista = reservaService.obtenerReservasPorViaje(10L);
            assertEquals(1, lista.size());
            assertEquals(12L, lista.get(0).getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void actualizarReserva_ok_y_validaPropietario() {
        try {
            Persona p = pasajero;
            java.lang.reflect.Field pid = Persona.class.getDeclaredField("id");
            pid.setAccessible(true);
            pid.set(p, 2L);

            Reserva existente = new Reserva();
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(existente, 20L);

            existente.setPersona(p);
            Parada nuevaSubida = new Parada(LocalDateTime.now(), "NS", TipoParada.ORIGEN, 1, viaje);
            Parada nuevaBajada = new Parada(LocalDateTime.now(), "NB", TipoParada.DESTINO, 2, viaje);

            Reserva update = new Reserva();
            update.setParadaSubida(nuevaSubida);
            update.setParadaBajada(nuevaBajada);

            when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(p));
            when(reservaRepository.findById(20L)).thenReturn(Optional.of(existente));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            Reserva result = reservaService.actualizarReserva("user@compicar.com", 20L, update);
            assertEquals(20L, result.getId());
            assertEquals(nuevaSubida, result.getParadaSubida());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
            java.lang.reflect.Field vid = Viaje.class.getDeclaredField("id");
            vid.setAccessible(true);
            vid.set(v, 5L);
            Persona owner = new Persona();
            owner.setEmail("driver@compicar.com");
            java.lang.reflect.Field ownerId = Persona.class.getDeclaredField("id");
            ownerId.setAccessible(true);
            ownerId.set(owner, 50L);
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
    void marcarNoPresentadoPorConductor_valida_condiciones_y_actualiza() {
        try {
            Persona conductorLocal = new Persona();
            conductorLocal.setEmail("driver@compicar.com");
            java.lang.reflect.Field cid = Persona.class.getDeclaredField("id");
            cid.setAccessible(true);
            cid.set(conductorLocal, 70L);

            Persona pasaj = new Persona();
            java.lang.reflect.Field pid = Persona.class.getDeclaredField("id");
            pid.setAccessible(true);
            pid.set(pasaj, 71L);

            Viaje v = new Viaje();
            java.lang.reflect.Field vid = Viaje.class.getDeclaredField("id");
            vid.setAccessible(true);
            vid.set(v, 8L);
            v.setPersona(conductorLocal);
            v.setEstado(EstadoViaje.INICIADO);

            Reserva r = new Reserva();
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 40L);
            r.setViaje(v);
            r.setPersona(pasaj);
            r.setEstado(EstadoReserva.PENDIENTE);

            when(personaRepository.findByEmail("driver@compicar.com")).thenReturn(Optional.of(conductorLocal));
            when(reservaRepository.findById(40L)).thenReturn(Optional.of(r));
            when(personaRepository.save(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
            when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

            Reserva res = reservaService.marcarNoPresentadoPorConductor("driver@compicar.com", 40L);
            assertEquals(EstadoReserva.NO_PRESENTADO, res.getEstado());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void obtenerReservasComoConductor_devuelveLista() {
        Reserva r = new Reserva();
        try {
            java.lang.reflect.Field rid = Reserva.class.getDeclaredField("id");
            rid.setAccessible(true);
            rid.set(r, 60L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
}

