package com.compicar.notificacion;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class NotificacionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private NotificacionController notificacionController;

    private Notificacion notificacionEjemplo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificacionController).build();

        // 1. Instanciamos con el constructor por defecto (leida ya es false)
        notificacionEjemplo = new Notificacion();
        
        // 2. Como NO hay método setId(), usamos ReflectionTestUtils para inyectar un ID simulado
        ReflectionTestUtils.setField(notificacionEjemplo, "id", 1L);
        
        // 3. Asignamos atributos disponibles
        notificacionEjemplo.setMensaje("Tu viaje ha sido modificado.");
        
        // Nota: Para este controlador no es estrictamente necesario mockear Persona y TipoNotificacion, 
        // ya que la lógica del Controller solo actualiza "leida" y lee notificaciones.
    }

    // ==========================================
    // TESTS PARA: GET /api/notificaciones/mis-notificaciones
    // ==========================================

    @Test
    void testObtenerMisNotificaciones_Success() throws Exception {
        when(notificacionRepository.findByReceptorEmailOrderByFechaCreacionDesc("juan@example.com"))
                .thenReturn(List.of(notificacionEjemplo));

        mockMvc.perform(get("/api/notificaciones/mis-notificaciones")
                .principal(() -> "juan@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                // Verificamos explícitamente los atributos de tu entidad Notificacion
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].mensaje").value("Tu viaje ha sido modificado."))
                .andExpect(jsonPath("$[0].leida").value(false));
    }

    @Test
    void testObtenerMisNotificaciones_EmptyList() throws Exception {
        when(notificacionRepository.findByReceptorEmailOrderByFechaCreacionDesc("vacio@example.com"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/notificaciones/mis-notificaciones")
                .principal(() -> "vacio@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ==========================================
    // TESTS PARA: PUT /api/notificaciones/{id}/leer
    // ==========================================

    @Test
    void testMarcarComoLeida_Success() throws Exception {
        when(notificacionRepository.findById(1L)).thenReturn(Optional.of(notificacionEjemplo));
        when(notificacionRepository.save(any(Notificacion.class))).thenReturn(notificacionEjemplo);

        mockMvc.perform(put("/api/notificaciones/1/leer"))
                .andExpect(status().isOk());

        // Verificamos que se guardó llamando al repositorio y además
        // comprobamos que el atributo "leida" efectivamente cambió a "true"
        verify(notificacionRepository, times(1)).save(argThat(notificacion -> notificacion.isLeida()));
    }

}
