package com.compicar.persona;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Long> {
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Persona p WHERE p.email = ?1")
    boolean existsByEmail(String email);
    
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Persona p WHERE p.telefono = ?1")
    boolean existsByTelefono(String telefono);
    
    @Query("SELECT p FROM Persona p WHERE p.email = ?1")
    Optional<Persona> findByEmail(String email);

    @Query("SELECT p FROM Persona p WHERE p.telefono = ?1")
    Optional<Persona> findByTelefono(String telefono);

    @Query("SELECT p FROM Persona p WHERE p.nombre = ?1")
    Persona findByNombre(String nombre);

    @Query("SELECT p FROM Persona p WHERE p.email = ?1")
    Persona findByEmailOrTelefono(String email);

    Optional<Persona> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
