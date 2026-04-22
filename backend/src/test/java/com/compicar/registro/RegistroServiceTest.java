package com.compicar.registro;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.compicar.autenticacion.registro.Registro;
import com.compicar.autenticacion.registro.RegistroService;
import com.compicar.persona.Persona;
import com.compicar.persona.PersonaService;

@ExtendWith(MockitoExtension.class)
class RegistroServiceTest {

    @Mock
    private PersonaService personaService;
    
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistroService registroService;

    @Test
    void testRegistrarPersona() {
        // Arrange
        Registro registro = new Registro();
        registro.setNombre("Juan");
        registro.setPrimerApellido("Pérez");
        registro.setSegundoApellido("García");
        registro.setEmail("juan@example.com");
        registro.setContrasena("password123");
        registro.setNumTelefono("+34123456789");

        Persona persona = new Persona();
        when(personaService.crearPersonaDesdeRegistro(eq(registro), eq(passwordEncoder)))
                .thenReturn(persona);

        // Act
        Persona personaResult = registroService.registrarPersona(registro);

        // Assert
        assertNotNull(personaResult);
        verify(personaService).crearPersonaDesdeRegistro(registro, passwordEncoder);
    }

}