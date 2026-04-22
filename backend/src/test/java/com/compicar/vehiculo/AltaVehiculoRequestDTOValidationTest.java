package com.compicar.vehiculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.compicar.vehiculo.dto.AltaVehiculoRequestDTO;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class AltaVehiculoRequestDTOValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void dtoValido_noTieneViolaciones() {
        AltaVehiculoRequestDTO dto = crearDtoValido();

        Set<ConstraintViolation<AltaVehiculoRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
    }

    @Test
    void anioFuturo_daViolacionEnAnio() {
        AltaVehiculoRequestDTO dto = crearDtoValido();
        dto.setAnio(Year.now().getValue() + 1);

        Set<ConstraintViolation<AltaVehiculoRequestDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        ConstraintViolation<AltaVehiculoRequestDTO> v = violations.iterator().next();
        assertEquals("anio", v.getPropertyPath().toString());
        assertEquals("El anio no puede ser mayor al actual", v.getMessage());
    }

    @Test
    void anioPorDebajoDelMinimo_daViolacionMin() {
        AltaVehiculoRequestDTO dto = crearDtoValido();
        dto.setAnio(1949);

        Set<ConstraintViolation<AltaVehiculoRequestDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        ConstraintViolation<AltaVehiculoRequestDTO> v = violations.iterator().next();
        assertEquals("anio", v.getPropertyPath().toString());
        assertEquals("El anio no es valido", v.getMessage());
    }

    @Test
    void anioNull_daViolacionNotNull() {
        AltaVehiculoRequestDTO dto = crearDtoValido();
        dto.setAnio(null);

        Set<ConstraintViolation<AltaVehiculoRequestDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        ConstraintViolation<AltaVehiculoRequestDTO> v = violations.iterator().next();
        assertEquals("anio", v.getPropertyPath().toString());
        assertEquals("El anio es obligatorio", v.getMessage());
    }

    private AltaVehiculoRequestDTO crearDtoValido() {
        AltaVehiculoRequestDTO dto = new AltaVehiculoRequestDTO();
        dto.setMatricula("1234ABC");
        dto.setMarca("Seat");
        dto.setModelo("Ibiza");
        dto.setPlazas(4);
        dto.setConsumo(5.2);
        dto.setAnio(Year.now().getValue());
        dto.setTipo(TipoVehiculo.COCHE);
        return dto;
    }
}
