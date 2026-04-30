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

    @Mock
    private com.compicar.parada.ParadaRepository paradaRepository;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Persona pasajero;
    private Persona conductor;
    private Viaje viaje;

    @BeforeEach
    void setUp() throws Exception {
        pasajero = new Persona("Nombre","Ape","B","pass","user@compicar.com","600000000");
        conductor = new Persona("Cond","A","B","p","driver@compicar.com","611111111");

        // Usamos reflexión para setear IDs globales
        setId(pasajero, 2L);
        setId(conductor, 3L);

        viaje = new Viaje();
        setId(viaje, 10L);
        
        viaje.setEstado(EstadoViaje.PENDIENTE);
        viaje.setPlazasDisponibles(3);
        viaje.setPersona(conductor);
        // Importante para evitar NPE en lógica de fechas
        viaje.setFechaHoraSalida(LocalDateTime.now().plusDays(1)); 
    }

    // Método auxiliar para evitar repetir reflexión
    private void setId(Object entity, Long id) throws Exception {
        java.lang.reflect.Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }

    @Test
    void crearReserva_ok() throws Exception {
        // Configuración de IDs de paradas
        Long origenId = 101L;
        Long destinoId = 102L;
        Parada pOrigen = new Parada();
        Parada pDestino = new Parada();
        
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));
        
        // MOCK: Simulamos que las paradas existen en la DB
        when(paradaRepository.findById(origenId)).thenReturn(Optional.of(pOrigen));
        when(paradaRepository.findById(destinoId)).thenReturn(Optional.of(pDestino));
        
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            setId(r, 1L);
            return r;
        });

        // Llamamos con IDs que el mock reconoce
        Reserva res = reservaService.crearReserva("user@compicar.com", 10L, 1, origenId, destinoId);

        assertEquals(1L, res.getId());
        assertEquals(2, viaje.getPlazasDisponibles());
    }

    @Test
    void crearReserva_plazasInvalidas_lanza() {
        when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(pasajero));
        when(viajeRepository.findById(10L)).thenReturn(Optional.of(viaje));

        // CORRECCIÓN: Se añaden null para origenId y destinoId
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

    // 1. Configurar IDs de paradas
    Long idSubida = 101L;
    Long idBajada = 102L;
    
    Parada subida = new Parada();
    setId(subida, idSubida);
    
    Parada bajada = new Parada();
    setId(bajada, idBajada);

    // 2. Configurar la reserva existente
    existente.setPersona(p);
    existente.setViaje(viaje); 
    existente.setCantidadPlazas(1);
    existente.setParadaSubida(subida);
    existente.setParadaBajada(bajada);

    // 3. Configurar los datos de actualización
    Reserva datosNuevos = new Reserva();
    datosNuevos.setCantidadPlazas(2);
    datosNuevos.setParadaSubida(subida);
    datosNuevos.setParadaBajada(bajada);

    // 4. MOCKS
    when(personaRepository.findByEmail("user@compicar.com")).thenReturn(Optional.of(p));
    when(reservaRepository.findById(20L)).thenReturn(Optional.of(existente));
    
    // CORRECCIÓN: El servicio busca las paradas por ID en la DB, debemos responderle
    when(paradaRepository.findById(idSubida)).thenReturn(Optional.of(subida));
    when(paradaRepository.findById(idBajada)).thenReturn(Optional.of(bajada));
    
    when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

    // 5. Ejecución
    Reserva result = reservaService.actualizarReserva("user@compicar.com", 20L, datosNuevos);
    
    // 6. Verificación
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
        // CORRECCIÓN: Las personas locales necesitan ID para que el .equals(id) no falle
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
}