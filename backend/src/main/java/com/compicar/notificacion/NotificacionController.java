package com.compicar.notificacion;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @GetMapping("/mis-notificaciones")
    public List<Notificacion> obtenerMisNotificaciones(Principal principal) {
        return notificacionRepository.findByReceptorEmailOrderByFechaCreacionDesc(principal.getName());
    }

    @PutMapping("/{id}/leer")
    public void marcarComoLeida(@PathVariable Long id) {
        Notificacion n = notificacionRepository.findById(id).orElseThrow();
        n.setLeida(true);
        notificacionRepository.save(n);
    }
}
