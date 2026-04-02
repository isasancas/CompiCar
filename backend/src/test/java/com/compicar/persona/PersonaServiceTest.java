package com.compicar.persona;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.compicar.autenticacion.registro.Registro;
import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;
import com.compicar.valoracion.Valoracion;

@ExtendWith(MockitoExtension.class)
public class PersonaServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private PersonaServiceImpl personaService;

    private Persona persona;

    @BeforeEach
    void setUp() {
        persona = new Persona();
        ReflectionTestUtils.setField(persona, "id", 1L);
        persona.setNombre("Juan");
        persona.setPrimerApellido("Perez");
        persona.setSegundoApellido("Garcia");
        persona.setEmail("juan@example.com");
        persona.setTelefono("123456789");
        persona.setContrasena("encodedPassword");
    }

    @Test
    void testCrearPersonaDesdeRegistro_Success() {
        // Given
        Registro registro = new Registro();
        registro.setNombre("Ana");
        registro.setPrimerApellido("Lopez");
        registro.setEmail("ana@example.com");
        registro.setNumTelefono("987654321");
        registro.setContrasena("password123");

        when(personaRepository.existsByEmail(anyString())).thenReturn(false);
        when(personaRepository.existsByTelefono(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Persona result = personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);

        // Then
        assertNotNull(result);
        assertEquals("ana@example.com", result.getEmail());
        assertEquals("encoded_pass", result.getContrasena());
        verify(personaRepository).save(any(Persona.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void testCrearPersonaDesdeRegistro_EmailYaExiste() {
        // Given
        Registro registro = new Registro();
        registro.setEmail(persona.getEmail());

        when(personaRepository.existsByEmail(persona.getEmail())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);
        });

        assertEquals("El email ya está registrado", exception.getMessage());
        verify(personaRepository, never()).save(any());
    }

    @Test
    void testCrearPersonaDesdeRegistro_TelefonoYaExiste() {
        // Given
        Registro registro = new Registro();
        registro.setEmail("nuevo_email@example.com"); // Email libre
        registro.setNumTelefono(persona.getTelefono()); // Teléfono ocupado

        when(personaRepository.existsByEmail("nuevo_email@example.com")).thenReturn(false);
        when(personaRepository.existsByTelefono(persona.getTelefono())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);
        });

        assertEquals("El teléfono ya está registrado", exception.getMessage());
    }

    @Test
    void testCrearPersonaDesdeRegistro_TelefonoFormatoInvalido() {
        // Given
        Registro registro = new Registro();
        registro.setEmail("test@example.com");
        registro.setNumTelefono("123"); // Demasiado corto, no cumple el patrón

        when(personaRepository.existsByEmail(anyString())).thenReturn(false);

        // When & Then
        // Suponiendo que lanzas IllegalArgumentException por formato inválido
        assertThrows(IllegalArgumentException.class, () -> {
            personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);
        });
        
        verify(personaRepository, never()).save(any(Persona.class));
    }

    @Test
    void testObtenerPerfil_Success() {
        // Given
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));

        // When
        PerfilPersonaDTO result = personaService.obtenerPerfil(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Juan", result.getNombre());
        assertEquals("Perez", result.getPrimerApellido());
        assertEquals("Garcia", result.getSegundoApellido());
        assertEquals("juan@example.com", result.getEmail());
        assertEquals("123456789", result.getTelefono());
    }

    @Test
    void testObtenerPerfil_PersonaNoEncontrada() {
        // Given
        when(personaRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.obtenerPerfil(1L);
        });
        assertEquals("Persona no encontrada", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_Success() {
        // Given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.save(any(Persona.class))).thenReturn(persona);

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "juan@example.com", "123456789", "password123");

        // When
        ActualizarPerfilDTO result = personaService.actualizarPerfil(1L, dto);

        // Then
        assertNotNull(result);
        assertEquals("Juan", result.getNombre());
        assertEquals("Perez", result.getPrimerApellido());
        assertEquals("Garcia", result.getSegundoApellido());
        assertEquals("juan@example.com", result.getEmail());
        assertEquals("123456789", result.getTelefono());
        verify(personaRepository).save(persona);
    }

    @Test
    void testActualizarPerfil_UsuarioNoAutenticado() {
        // Given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.empty());

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "juan@example.com", "123456789", "password123");

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_NoPuedeModificarOtroUsuario() {
        // Given
        Persona otroUsuario = new Persona();
        ReflectionTestUtils.setField(otroUsuario, "id", 2L);
        otroUsuario.setEmail("otro@example.com");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("otro@example.com");
        when(personaRepository.findByEmail("otro@example.com")).thenReturn(Optional.of(otroUsuario));

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "juan@example.com", "123456789", "password123");

        // When & Then
        org.springframework.security.access.AccessDeniedException exception = assertThrows(
            org.springframework.security.access.AccessDeniedException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });
        assertEquals("No puedes modificar el perfil de otro usuario", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_EmailYaRegistrado() {
        // Given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.existsByEmail("nuevo@example.com")).thenReturn(true);

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "nuevo@example.com", "123456789", "password123");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });
        assertEquals("El email ya está registrado", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_ContrasenaIncorrecta() {
        // Given
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.existsByEmail("nuevo@example.com")).thenReturn(false);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "nuevo@example.com", "123456789", "wrongPassword");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });
        assertEquals("La contraseña actual es incorrecta", exception.getMessage());
    }
}