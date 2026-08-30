package com.compicar.viajeBase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.viaje.Viaje;
import com.compicar.viaje.ViajeRepository;
import com.compicar.viaje.ViajeService;
import com.compicar.viajeRecurrente.ViajeRecurrenteRepository;
import com.compicar.viajeRecurrente.ViajeRecurrenteService;

@ExtendWith(MockitoExtension.class)
class ViajeRouterServiceTest {

    @Mock
    private ViajeService viajeService;
    @Mock
    private ViajeRecurrenteService viajeRecurrenteService;
    @Mock
    private ViajeRepository viajeRepository;
    @Mock
    private ViajeRecurrenteRepository viajeRecurrenteRepository;

    @InjectMocks
    private ViajeRouterService viajeRouterService;

    // --- OBTENER POR SLUG ---

    @Test
    void obtenerPorSlug_viajeNormal_delegaEnViajeService() {
        when(viajeRepository.existsBySlug("slug-normal")).thenReturn(true);
        when(viajeService.obtenerViajePorSlug("slug-normal")).thenReturn(null);

        viajeRouterService.obtenerPorSlug("slug-normal");

        verify(viajeService).obtenerViajePorSlug("slug-normal");
        verifyNoInteractions(viajeRecurrenteService);
    }

    @Test
    void obtenerPorSlug_viajeRecurrente_delegaEnViajeRecurrenteService() {
        when(viajeRepository.existsBySlug("slug-rec")).thenReturn(false);
        when(viajeRecurrenteRepository.existsBySlug("slug-rec")).thenReturn(true);
        when(viajeRecurrenteService.obtenerViajeRecurrentePorSlug("slug-rec")).thenReturn(null);

        viajeRouterService.obtenerPorSlug("slug-rec");

        verify(viajeRecurrenteService).obtenerViajeRecurrentePorSlug("slug-rec");
    }

    @Test
    void obtenerPorSlug_noExiste_lanza404() {
        when(viajeRepository.existsBySlug("slug-inexistente")).thenReturn(false);
        when(viajeRecurrenteRepository.existsBySlug("slug-inexistente")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRouterService.obtenerPorSlug("slug-inexistente"));

        assertEquals(404, ex.getStatusCode().value());
        assertEquals("Viaje no encontrado", ex.getReason());
    }

    // --- INICIAR VIAJE ---

    @Test
    void iniciarViaje_viajeNormal_delegaEnViajeService() {
        when(viajeRepository.existsBySlug("slug-normal")).thenReturn(true);
        when(viajeService.iniciarViaje(anyString(), anyString())).thenReturn(null);

        viajeRouterService.iniciarViaje("test@test.com", "slug-normal");

        verify(viajeService).iniciarViaje("test@test.com", "slug-normal");
    }

    @Test
    void iniciarViaje_viajeRecurrente_delegaEnViajeRecurrenteService() {
        when(viajeRepository.existsBySlug("slug-rec")).thenReturn(false);
        when(viajeRecurrenteRepository.existsBySlug("slug-rec")).thenReturn(true);
        when(viajeRecurrenteService.iniciarViajeRecurrente(anyString(), anyString())).thenReturn(null);

        viajeRouterService.iniciarViaje("test@test.com", "slug-rec");

        verify(viajeRecurrenteService).iniciarViajeRecurrente("test@test.com", "slug-rec");
    }

    @Test
    void iniciarViaje_noExiste_lanza404() {
        when(viajeRepository.existsBySlug("slug-inexistente")).thenReturn(false);
        when(viajeRecurrenteRepository.existsBySlug("slug-inexistente")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> viajeRouterService.iniciarViaje("test@test.com", "slug-inexistente"));

        assertEquals(404, ex.getStatusCode().value());
    }

    // --- CONFIRMAR CHECKIN ---

    @Test
    void confirmarCheckin_viajeNormal_delegaEnViajeService() {
        when(viajeRepository.existsBySlug("slug-normal")).thenReturn(true);
        when(viajeService.confirmarCheckin(anyString(), anyString(), anyString())).thenReturn(null);

        viajeRouterService.confirmarCheckin("test@test.com", "slug-normal", "1234");

        verify(viajeService).confirmarCheckin("test@test.com", "slug-normal", "1234");
    }

    @Test
    void confirmarCheckin_viajeRecurrente_delegaEnViajeRecurrenteService() {
        when(viajeRepository.existsBySlug("slug-rec")).thenReturn(false);
        when(viajeRecurrenteRepository.existsBySlug("slug-rec")).thenReturn(true);
        when(viajeRecurrenteService.confirmarCheckinRecurrente(anyString(), anyString(), anyString())).thenReturn(null);

        viajeRouterService.confirmarCheckin("test@test.com", "slug-rec", "1234");

        verify(viajeRecurrenteService).confirmarCheckinRecurrente("test@test.com", "slug-rec", "1234");
    }

    // --- FINALIZAR VIAJE ---

    @Test
    void finalizarViaje_viajeRecurrente_delegaCorrectamente() {
        when(viajeRepository.existsBySlug("slug-rec")).thenReturn(false);
        when(viajeRecurrenteRepository.existsBySlug("slug-rec")).thenReturn(true);
        when(viajeRecurrenteService.finalizarViajeRecurrente(anyString(), anyString())).thenReturn(null);

        viajeRouterService.finalizarViaje("test@test.com", "slug-rec");

        verify(viajeRecurrenteService).finalizarViajeRecurrente("test@test.com", "slug-rec");
    }

    // --- CANCELAR VIAJE ---

    @Test
    void cancelarViaje_viajeNormal_delegaCorrectamente() {
        when(viajeRepository.existsBySlug("slug-normal")).thenReturn(true);
        when(viajeService.cancelarViaje(anyString(), anyString())).thenReturn(null);

        viajeRouterService.cancelarViaje("test@test.com", "slug-normal");

        verify(viajeService).cancelarViaje("test@test.com", "slug-normal");
    }

    // --- CANCELAR VIAJE INCOMPARECE CONDUCTOR ---

    @Test
    void cancelarViajeIncompareceConductor_viajeRecurrente_delegaCorrectamente() {
        when(viajeRepository.existsBySlug("slug-rec")).thenReturn(false);
        when(viajeRecurrenteRepository.existsBySlug("slug-rec")).thenReturn(true);
        when(viajeRecurrenteService.cancelarViajeRecurrenteIncompareceConductor(anyString(), anyString())).thenReturn(null);

        viajeRouterService.cancelarViajeIncompareceConductor("test@test.com", "slug-rec");

        verify(viajeRecurrenteService).cancelarViajeRecurrenteIncompareceConductor("test@test.com", "slug-rec");
    }

    // --- ACTUALIZAR VIAJE ---

    @Test
    void actualizarViaje_viajeNormal_delegaCorrectamente() {
        Viaje viajeEditado = new Viaje();
        when(viajeRepository.existsBySlug("slug-normal")).thenReturn(true);
        when(viajeService.actualizarViaje(anyString(), anyString(), any())).thenReturn(null);

        viajeRouterService.actualizarViaje("test@test.com", "slug-normal", viajeEditado);

        verify(viajeService).actualizarViaje("test@test.com", "slug-normal", viajeEditado);
    }
}