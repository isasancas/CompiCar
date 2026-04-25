package com.compicar.notificacion;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByReceptorEmailOrderByFechaCreacionDesc(String email);
    long countByReceptorEmailAndLeidaFalse(String email);
}
