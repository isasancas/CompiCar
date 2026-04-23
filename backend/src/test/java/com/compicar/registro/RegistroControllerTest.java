package com.compicar.registro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.compicar.autenticacion.registro.Registro;
import com.compicar.autenticacion.registro.RegistroController;
import com.compicar.autenticacion.registro.RegistroService;
import com.compicar.persona.Persona;

@ExtendWith(MockitoExtension.class)
class RegistroControllerTest {

    @InjectMocks
    private RegistroController registroController;

    @Mock
    private RegistroService registroService;

    @Test
    void testRegister() {
        // Arrange
        Registro registro = new Registro();
        registro.setNombre("Ana");
        registro.setPrimerApellido("López");
        registro.setSegundoApellido("Martínez");
        registro.setEmail("ana@example.com");
        registro.setContrasena("claveSegura");
        registro.setNumTelefono("+34987654321");

        Persona personaMock = new Persona();
        when(registroService.registrarPersona(eq(registro))).thenReturn(personaMock);

        RegistroController controller = new RegistroController(registroService);

        // Act
        ResponseEntity<String> response = controller.register(registro);

        // Assert
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Usuario registrado", response.getBody());
        verify(registroService).registrarPersona(registro);
    }
}
