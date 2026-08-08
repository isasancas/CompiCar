package com.compicar.persona;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import com.compicar.persona.dto.ActualizarPerfilDTO;
import com.compicar.persona.dto.PerfilPersonaDTO;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Transfer;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import com.stripe.param.TransferCreateParams;
import com.compicar.autenticacion.registro.Registro;
import com.compicar.config.SlugUtils;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class PersonaServiceImpl implements PersonaService {

    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PersonaServiceImpl(PersonaRepository personaRepository, PasswordEncoder passwordEncoder) {
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    @Override
    public Persona crearPersonaDesdeRegistro(Registro registro, PasswordEncoder passwordEncoder) {
        if (personaRepository.existsByEmail(registro.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        String tel = registro.getNumTelefono();
        if (tel != null && !tel.isEmpty()) {
            if (!tel.matches("^\\+?[0-9]{7,15}$")) {
                throw new IllegalArgumentException("El formato del teléfono es inválido");
            }
            
            if (personaRepository.existsByTelefono(tel)) {
                throw new IllegalArgumentException("El teléfono ya está registrado");
            }
        }

        Persona persona = new Persona();
        persona.setNombre(registro.getNombre());
        persona.setPrimerApellido(registro.getPrimerApellido());
        persona.setSegundoApellido(registro.getSegundoApellido());
        persona.setEmail(registro.getEmail());
        persona.setTelefono(registro.getNumTelefono());

        String contrasenaEncriptada = passwordEncoder.encode(registro.getContrasena());
        persona.setContrasena(contrasenaEncriptada);

        String baseSlug = SlugUtils.toSlug(
            registro.getNombre() + "-" + registro.getPrimerApellido()
        );
        persona.setSlug(generarSlugUnico(baseSlug));

        return personaRepository.save(persona);
    }

    @Override
    public PerfilPersonaDTO obtenerPerfil(Long personaId) {
        Persona persona = personaRepository.findById(personaId)
                .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        return new PerfilPersonaDTO(persona);
    }

    @Override
    public ActualizarPerfilDTO actualizarPerfil(Long personaId, ActualizarPerfilDTO perfilActualizado) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Persona personaAutenticada = personaRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!personaAutenticada.getId().equals(personaId)) {
            throw new AccessDeniedException("No puedes modificar el perfil de otro usuario");
        }
        
        personaAutenticada.setNombre(perfilActualizado.getNombre());
        personaAutenticada.setPrimerApellido(perfilActualizado.getPrimerApellido());
        personaAutenticada.setSegundoApellido(perfilActualizado.getSegundoApellido());
        personaAutenticada.setTelefono(perfilActualizado.getTelefono());
        personaAutenticada.getPreferenciasViaje().clear();
        if (perfilActualizado.getPreferenciasViaje() != null) {
            personaAutenticada.getPreferenciasViaje().addAll(perfilActualizado.getPreferenciasViaje());
        }

        if (!personaAutenticada.getEmail().equals(perfilActualizado.getEmail())) {
            if (personaRepository.existsByEmail(perfilActualizado.getEmail())) {
                throw new IllegalArgumentException("El email ya está registrado");
            }
            if (perfilActualizado.getContrasenaActual() == null || perfilActualizado.getContrasenaActual().isBlank()) {
                throw new IllegalArgumentException("Debes introducir tu contraseña actual para cambiar el email");
            }
            if (!passwordEncoder.matches(perfilActualizado.getContrasenaActual(), personaAutenticada.getContrasena())) {
                throw new IllegalArgumentException("La contraseña actual es incorrecta");
            } else {
                personaAutenticada.setEmail(perfilActualizado.getEmail());
            }
        }
        
        Persona personaActualizada = personaRepository.save(personaAutenticada);
        return new ActualizarPerfilDTO(personaActualizada);
    }

    @Override
    public Persona obtenerPersonaPorNombrePersona(String username) {
       Persona persona = personaRepository.findByNombre(username);
         if (persona == null) {
              throw new RuntimeException("Usuario no encontrado");
         }
        return persona;
    }

    @Override
    public Persona obtenerPersonaPorEmail(String email) {
        Persona persona = personaRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return persona;
    }

    @Override
    public PerfilPersonaDTO obtenerPerfilPorSlug(String slug) {
        Persona persona = personaRepository.findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("Persona no encontrada"));
        return new PerfilPersonaDTO(persona);
    }

    @Override
    public void subirFoto(String email, String fotoBase64) {
        Persona persona = personaRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Validar tamaño máximo (5MB en Base64 = ~3.75MB original)
        if (fotoBase64.length() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Foto demasiado grande");
        }
        
        persona.setFoto(fotoBase64);
        personaRepository.save(persona);
    }

    /**
     * Permite al usuario retirar sus fondos acumulados.
     * Restricción: Saldo mínimo de 10.00€.
     */
    @Override
    @Transactional
    public Map<String, Object> retirarFondos(String email) {
        Persona persona = personaRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        BigDecimal fondosActuales = persona.getFondosActuales() != null 
                ? persona.getFondosActuales() 
                : BigDecimal.ZERO;

        BigDecimal minimoRetirada = new BigDecimal("10.00");

        // 1. Validar que tenga al menos 10€
        if (fondosActuales.compareTo(minimoRetirada) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, 
                    "Se requiere un saldo mínimo de 10.00€ para realizar la retirada. Saldo actual: " + fondosActuales + "€"
            );
        }

        try {
            boolean necesitaOnboarding = false;
            Account account = null;

            // 1. Si no tiene ID en BD, hay que crearlo
            if (persona.getStripeConductorId() == null || persona.getStripeConductorId().isBlank()) {
                AccountCreateParams accountParams = AccountCreateParams.builder()
                        .setType(AccountCreateParams.Type.EXPRESS)
                        .setCountry("ES")
                        .setEmail(persona.getEmail())
                        .setCapabilities(AccountCreateParams.Capabilities.builder()
                                .setTransfers(AccountCreateParams.Capabilities.Transfers.builder()
                                        .setRequested(true)
                                        .build())
                                .build())
                        .build();

                account = Account.create(accountParams);
                persona.setStripeConductorId(account.getId());
                personaRepository.save(persona);
                necesitaOnboarding = true;
            } else {
                // 2. Si ya tiene ID, consultamos a Stripe si completó todos sus datos (IBAN, etc.)
                account = Account.retrieve(persona.getStripeConductorId());
                if (!Boolean.TRUE.equals(account.getDetailsSubmitted())) {
                    necesitaOnboarding = true;
                }
            }

            // Si la cuenta es nueva O si dejó el onboarding a mitad de camino:
            if (necesitaOnboarding) {
                AccountLinkCreateParams linkParams = AccountLinkCreateParams.builder()
                        .setAccount(account.getId())
                        .setRefreshUrl("http://localhost:5173/perfil?stripe=refresh")
                        .setReturnUrl("http://localhost:5173/perfil?stripe=success")
                        .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                        .build();

                AccountLink accountLink = AccountLink.create(linkParams);

                return Map.of(
                    "status", "REQUIRES_ONBOARDING",
                    "url", accountLink.getUrl()
                );
            }

            // 3. Si llegó aquí, los datos están completados y la capacidad activa -> Ejecutar transferencia
            long amountInCents = fondosActuales.multiply(new BigDecimal("100")).longValue();

            TransferCreateParams transferParams = TransferCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("eur")
                    .setDestination(persona.getStripeConductorId())
                    .setDescription("Retiro de saldo de " + persona.getNombre())
                    .build();

            Transfer transfer = Transfer.create(transferParams);

            persona.setFondosActuales(BigDecimal.ZERO);
            personaRepository.save(persona);

            return Map.of(
                "status", "SUCCESS",
                "mensaje", "Retiro completado con éxito",
                "transferId", transfer.getId()
            );

        } catch (StripeException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al comunicarse con Stripe: " + e.getMessage()
            );
        }
    }

    private String generarSlugUnico(String baseSlug) {
        String candidato = baseSlug;
        int sufijo = 2;
        while (personaRepository.existsBySlug(candidato)) {
            candidato = baseSlug + "-" + sufijo;
            sufijo++;
        }
        return candidato;
    }
}
