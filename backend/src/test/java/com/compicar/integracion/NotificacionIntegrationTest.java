package com.compicar.integracion;

import com.compicar.notificacion.Notificacion;
import com.compicar.notificacion.NotificacionRepository;
import com.compicar.notificacion.TipoNotificacion;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificacionIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Test
    void obtenerMisNotificaciones_ok() throws Exception {
        String token = registerAndLogin();

        mockMvc.perform(get("/api/notificaciones/mis-notificaciones")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void obtenerMisNotificaciones_sinToken_401() throws Exception {
        mockMvc.perform(get("/api/notificaciones/mis-notificaciones"))
                .andExpect(status().isForbidden());
    }

    @Test
    void marcarComoLeida_ok() throws Exception {
        String token = registerAndLogin();
        
        // Obtenemos la persona autenticada para asociarla como receptor
        Persona persona = personaRepository.findAll().get(0);

        // Creamos una notificación válida rellenando los campos obligatorios
        Notificacion notificacion = new Notificacion(
            "Mensaje de prueba", 
            persona, 
            TipoNotificacion.NUEVA_RESERVA // O el tipo que corresponda en tu enum
        );
        notificacion = notificacionRepository.save(notificacion);

        mockMvc.perform(put("/api/notificaciones/" + notificacion.getId() + "/leer")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void marcarComoLeida_sinToken_401() throws Exception {
        mockMvc.perform(put("/api/notificaciones/1/leer"))
                .andExpect(status().isForbidden());
    }
}