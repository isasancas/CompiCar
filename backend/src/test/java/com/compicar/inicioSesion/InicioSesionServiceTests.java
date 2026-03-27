package com.compicar.inicioSesion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.compicar.autenticacion.inicioSesion.InicioSesion;
import com.compicar.autenticacion.inicioSesion.InicioSesionService;
import com.compicar.autenticacion.inicioSesion.JwtUtil;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaService;

@ExtendWith(MockitoExtension.class)
class InicioSesionServiceTests {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PersonaService personaService;

    @InjectMocks
    private InicioSesionService inicioSesionService;

    private Persona persona;

    @BeforeEach
    void setUp() {
        persona = new Persona();
        persona.setNombre("Juan");
        persona.setPrimerApellido("Pérez");
        persona.setSegundoApellido("García");
        persona.setEmail("juan.perez@gmail.com");
        persona.setContrasena("123456");
        persona.setTelefono("+34123456789");
    }

    @Test
    void testIniciarSesion_devuelve200() {
        // Arrange
        InicioSesion request = new InicioSesion();
        request.setEmail(persona.getEmail());
        request.setContrasena(persona.getContrasena());

        when(personaService.obtenerPersonaPorEmail(persona.getEmail())).thenReturn(persona);
        when(passwordEncoder.matches(request.getContrasena(), persona.getContrasena())).thenReturn(true);
        when(jwtUtil.generateToken(persona.getEmail())).thenReturn("token123");
        
        //Act
        String token = inicioSesionService.iniciarSesion(request);
        
        // Assert
        assertNotNull(token);
        assertEquals("token123", token);
    }

    @Test
    void testIniciarSesion_usuarioNoEncontrado() {
        // Arrange
        InicioSesion request = new InicioSesion();
        request.setEmail(persona.getEmail());
        request.setContrasena(persona.getContrasena());

        when(personaService.obtenerPersonaPorEmail(persona.getEmail())).thenReturn(null);
        
        // Act & Assert
        assertThatThrownBy(() -> inicioSesionService.iniciarSesion(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Usuario no encontrado");
    }

    @Test
    void testIniciarSesion_contrasenaIncorrecta() {
        // Arrange
        InicioSesion request = new InicioSesion();
        request.setEmail(persona.getEmail());
        request.setContrasena(persona.getContrasena());

        when(personaService.obtenerPersonaPorEmail(persona.getEmail())).thenReturn(persona);
        when(passwordEncoder.matches(request.getContrasena(), persona.getContrasena())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> inicioSesionService.iniciarSesion(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Contraseña incorrecta");
    }
    
}
