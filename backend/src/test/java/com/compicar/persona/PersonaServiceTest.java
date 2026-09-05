package com.compicar.persona;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.compicar.autenticacion.registro.Registro;
import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Transfer;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.TransferCreateParams;

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

        ReflectionTestUtils.setField(personaService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void testCrearPersonaDesdeRegistro_Success_ConSlugColisionYTelefono() {
        Registro registro = new Registro();
        registro.setNombre("Ana");
        registro.setPrimerApellido("Lopez");
        registro.setEmail("ana@example.com");
        registro.setNumTelefono("+34612345678");
        registro.setContrasena("password123");

        when(personaRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(personaRepository.existsByTelefono("+34612345678")).thenReturn(false);
        when(personaRepository.existsBySlug("ana-lopez")).thenReturn(true);
        when(personaRepository.existsBySlug("ana-lopez-2")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Persona result = personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);

        assertNotNull(result);
        assertEquals("ana@example.com", result.getEmail());
        assertEquals("ana-lopez-2", result.getSlug());
        assertEquals("encoded_pass", result.getContrasena());
        verify(personaRepository).save(any(Persona.class));
    }

    @Test
    void testCrearPersonaDesdeRegistro_TelefonoNullOVacio() {
        Registro registro = new Registro();
        registro.setNombre("Ana");
        registro.setPrimerApellido("Lopez");
        registro.setEmail("ana@example.com");
        registro.setNumTelefono("");
        registro.setContrasena("password123");

        when(personaRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(personaRepository.existsBySlug("ana-lopez")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_pass");
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Persona result = personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);

        assertNotNull(result);
        assertEquals("", result.getTelefono());
        verify(personaRepository, never()).existsByTelefono(anyString());
    }

    @Test
    void testCrearPersonaDesdeRegistro_EmailYaExiste() {
        Registro registro = new Registro();
        registro.setEmail(persona.getEmail());

        when(personaRepository.existsByEmail(persona.getEmail())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);
        });

        assertEquals("El email ya está registrado", exception.getMessage());
        verify(personaRepository, never()).save(any());
    }

    @Test
    void testCrearPersonaDesdeRegistro_TelefonoYaExiste() {
        Registro registro = new Registro();
        registro.setEmail("nuevo_email@example.com");
        registro.setNumTelefono(persona.getTelefono());

        when(personaRepository.existsByEmail("nuevo_email@example.com")).thenReturn(false);
        when(personaRepository.existsByTelefono(persona.getTelefono())).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);
        });

        assertEquals("El teléfono ya está registrado", exception.getMessage());
    }

    @Test
    void testCrearPersonaDesdeRegistro_TelefonoFormatoInvalido() {
        Registro registro = new Registro();
        registro.setEmail("test@example.com");
        registro.setNumTelefono("123");

        when(personaRepository.existsByEmail(anyString())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.crearPersonaDesdeRegistro(registro, passwordEncoder);
        });

        assertEquals("El formato del teléfono es inválido", exception.getMessage());
        verify(personaRepository, never()).save(any(Persona.class));
    }

    @Test
    void testObtenerPerfil_ok() {
        when(personaRepository.findById(1L)).thenReturn(Optional.of(persona));

        PerfilPersonaDTO result = personaService.obtenerPerfil(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Juan", result.getNombre());
        assertEquals("Perez", result.getPrimerApellido());
    }

    @Test
    void testObtenerPerfil_PersonaNoEncontrada() {
        when(personaRepository.findById(1L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.obtenerPerfil(1L);
        });
        assertEquals("Persona no encontrada", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_ok_MismoEmailYPreferencias() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "juan@example.com", "123456789", null);
        dto.setPreferenciasViaje(List.of("MÚSICA", "MASCOTAS"));

        ActualizarPerfilDTO result = personaService.actualizarPerfil(1L, dto);

        assertNotNull(result);
        assertEquals("Juan", result.getNombre());
        assertEquals(2, persona.getPreferenciasViaje().size());
        verify(personaRepository).save(persona);
    }

    @Test
    void testActualizarPerfil_ok_CambioEmailCorrecto() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.existsByEmail("nuevo@example.com")).thenReturn(false);
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "nuevo@example.com", "123456789", "password123");

        ActualizarPerfilDTO result = personaService.actualizarPerfil(1L, dto);

        assertNotNull(result);
        assertEquals("nuevo@example.com", result.getEmail());
    }

    @Test
    void testActualizarPerfil_CambioEmail_ContrasenaNullOBlanca() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.existsByEmail("nuevo@example.com")).thenReturn(false);

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "nuevo@example.com", "123456789", "  ");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });

        assertEquals("Debes introducir tu contraseña actual para cambiar el email", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_UsuarioNoAutenticado() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.empty());

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "juan@example.com", "123456789", "password123");

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_NoPuedeModificarOtroUsuario() {
        Persona otroUsuario = new Persona();
        ReflectionTestUtils.setField(otroUsuario, "id", 2L);
        otroUsuario.setEmail("otro@example.com");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("otro@example.com");
        when(personaRepository.findByEmail("otro@example.com")).thenReturn(Optional.of(otroUsuario));

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "juan@example.com", "123456789", "password123");

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });
        assertEquals("No puedes modificar el perfil de otro usuario", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_EmailYaRegistrado() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.existsByEmail("nuevo@example.com")).thenReturn(true);

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "nuevo@example.com", "123456789", "password123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });
        assertEquals("El email ya está registrado", exception.getMessage());
    }

    @Test
    void testActualizarPerfil_ContrasenaIncorrecta() {
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("juan@example.com");
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.existsByEmail("nuevo@example.com")).thenReturn(false);
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        ActualizarPerfilDTO dto = new ActualizarPerfilDTO("Juan", "Perez", "Garcia", "nuevo@example.com", "123456789", "wrongPassword");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.actualizarPerfil(1L, dto);
        });
        assertEquals("La contraseña actual es incorrecta", exception.getMessage());
    }

    @Test
    void testObtenerPersonaPorNombrePersona_ok() {
        when(personaRepository.findByNombre("Juan")).thenReturn(persona);

        Persona result = personaService.obtenerPersonaPorNombrePersona("Juan");

        assertNotNull(result);
        assertEquals("Juan", result.getNombre());
    }

    @Test
    void testObtenerPersonaPorNombrePersona_NoEncontrado() {
        when(personaRepository.findByNombre("NoExiste")).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            personaService.obtenerPersonaPorNombrePersona("NoExiste");
        });
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void testObtenerPersonaPorEmail_ok() {
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));

        Persona result = personaService.obtenerPersonaPorEmail("juan@example.com");

        assertNotNull(result);
        assertEquals("juan@example.com", result.getEmail());
    }

    @Test
    void testObtenerPersonaPorEmail_NoEncontrado() {
        when(personaRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            personaService.obtenerPersonaPorEmail("noexiste@example.com");
        });
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void testObtenerPerfilPorSlug_ok() {
        persona.setSlug("juan-perez");
        when(personaRepository.findBySlug("juan-perez")).thenReturn(Optional.of(persona));

        PerfilPersonaDTO result = personaService.obtenerPerfilPorSlug("juan-perez");

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testObtenerPerfilPorSlug_NoEncontrado() {
        when(personaRepository.findBySlug("inexistente")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            personaService.obtenerPerfilPorSlug("inexistente");
        });
        assertEquals("Persona no encontrada", exception.getMessage());
    }

    @Test
    void testSubirFoto_ok() {
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.save(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        personaService.subirFoto("juan@example.com", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAE...");

        assertEquals("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAE...", persona.getFoto());
        verify(personaRepository).save(persona);
    }

    @Test
    void testSubirFoto_UsuarioNoEncontrado() {
        when(personaRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            personaService.subirFoto("noexiste@example.com", "foto");
        });
        assertEquals("Usuario no encontrado", exception.getMessage());
    }

    @Test
    void testSubirFoto_TamanoExcedido() {
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));

        String fotoGrande = "a".repeat(5 * 1024 * 1024 + 1);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            personaService.subirFoto("juan@example.com", fotoGrande);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Foto demasiado grande", exception.getReason());
    }

    @Test
    void testRetirarFondos_SaldoInsuficiente() {
        persona.setFondosActuales(new BigDecimal("5.00"));
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            personaService.retirarFondos("juan@example.com");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Se requiere un saldo mínimo de 10.00€"));
    }

    @Test
    void testRetirarFondos_FondosNull_SaldoInsuficiente() {
        persona.setFondosActuales(null);
        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            personaService.retirarFondos("juan@example.com");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testRetirarFondos_UsuarioNoEncontrado() {
        when(personaRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            personaService.retirarFondos("noexiste@example.com");
        });

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Usuario no encontrado", exception.getReason());
    }

    @Test
    void testRetirarFondos_ok_CreaCuentaYTransfiere() {
        persona.setFondosActuales(new BigDecimal("50.00"));
        persona.setStripeConductorId(null);

        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.save(any(Persona.class))).thenReturn(persona);

        try (MockedStatic<Account> mockedAccount = mockStatic(Account.class);
             MockedStatic<AccountLink> mockedAccountLink = mockStatic(AccountLink.class)) {

            Account mockAccount = mock(Account.class);
            when(mockAccount.getId()).thenReturn("acct_123");
            mockedAccount.when(() -> Account.create(any(AccountCreateParams.class)))
                          .thenReturn(mockAccount);

            AccountLink mockLink = mock(AccountLink.class);
            when(mockLink.getUrl()).thenReturn("https://connect.stripe.com/setup/s/xyz");
            mockedAccountLink.when(() -> AccountLink.create(any(AccountLinkCreateParams.class)))
                             .thenReturn(mockLink);

            Map<String, Object> resultado = personaService.retirarFondos("juan@example.com");

            assertNotNull(resultado);
            assertEquals("REQUIRES_ONBOARDING", resultado.get("status"));
            assertEquals("https://connect.stripe.com/setup/s/xyz", resultado.get("url"));
            verify(personaRepository).save(persona);
        }
    }

    @Test
    void testRetirarFondos_CuentaExistente_Incompleta_RequiresOnboarding() {
        persona.setFondosActuales(new BigDecimal("25.00"));
        persona.setStripeConductorId("acct_existente");

        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));

        try (MockedStatic<Account> mockedAccount = mockStatic(Account.class);
             MockedStatic<AccountLink> mockedAccountLink = mockStatic(AccountLink.class)) {

            Account mockAccount = mock(Account.class);
            when(mockAccount.getId()).thenReturn("acct_existente");
            when(mockAccount.getDetailsSubmitted()).thenReturn(false);

            mockedAccount.when(() -> Account.retrieve("acct_existente")).thenReturn(mockAccount);

            AccountLink mockLink = mock(AccountLink.class);
            when(mockLink.getUrl()).thenReturn("http://localhost:3000/perfil?stripe=refresh");
            mockedAccountLink.when(() -> AccountLink.create(any(AccountLinkCreateParams.class)))
                             .thenReturn(mockLink);

            Map<String, Object> resultado = personaService.retirarFondos("juan@example.com");

            assertNotNull(resultado);
            assertEquals("REQUIRES_ONBOARDING", resultado.get("status"));
            assertEquals("http://localhost:3000/perfil?stripe=refresh", resultado.get("url"));
        }
    }

    @Test
    void testRetirarFondos_Success_TransfiereExitosamente() {
        persona.setFondosActuales(new BigDecimal("50.00"));
        persona.setStripeConductorId("acct_completa");

        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));
        when(personaRepository.save(any(Persona.class))).thenReturn(persona);

        try (MockedStatic<Account> mockedAccount = mockStatic(Account.class);
             MockedStatic<Transfer> mockedTransfer = mockStatic(Transfer.class)) {

            Account mockAccount = mock(Account.class);
            when(mockAccount.getDetailsSubmitted()).thenReturn(true);
            mockedAccount.when(() -> Account.retrieve("acct_completa")).thenReturn(mockAccount);

            Transfer mockTransfer = mock(Transfer.class);
            when(mockTransfer.getId()).thenReturn("tr_9999");
            mockedTransfer.when(() -> Transfer.create(any(TransferCreateParams.class))).thenReturn(mockTransfer);

            Map<String, Object> resultado = personaService.retirarFondos("juan@example.com");

            assertNotNull(resultado);
            assertEquals("SUCCESS", resultado.get("status"));
            assertEquals("tr_9999", resultado.get("transferId"));
            assertEquals(BigDecimal.ZERO, persona.getFondosActuales());
            verify(personaRepository).save(persona);
        }
    }

    @Test
    void testRetirarFondos_StripeException_LanzaResponseStatusException() {
        persona.setFondosActuales(new BigDecimal("50.00"));
        persona.setStripeConductorId("acct_error");

        when(personaRepository.findByEmail("juan@example.com")).thenReturn(Optional.of(persona));

        try (MockedStatic<Account> mockedAccount = mockStatic(Account.class)) {
            mockedAccount.when(() -> Account.retrieve("acct_error")).thenThrow(mock(StripeException.class));

            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                personaService.retirarFondos("juan@example.com");
            });

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
            assertTrue(exception.getReason().contains("Error al comunicarse con Stripe"));
        }
    }

    @Test
    void testObtenerTopConductores_ok() {
        Persona conductorTop = new Persona();
        ReflectionTestUtils.setField(conductorTop, "id", 2L);
        conductorTop.setNombre("Carlos");

        Object[] fila = new Object[]{ conductorTop, 4.766 };
        List<Object[]> resultados = java.util.Collections.singletonList(fila);

        when(personaRepository.findTopConductoresConReputacion(any(Pageable.class))).thenReturn(resultados);

        List<PerfilPersonaDTO> dtos = personaService.obtenerTopConductores();

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals("Carlos", dtos.get(0).getNombre());
        assertEquals(4.8, dtos.get(0).getReputacion());
    }
}