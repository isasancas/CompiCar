package com.compicar.vehiculo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Year;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.compicar.vehiculo.validaciones.AnioNoFuturoValidator;

class AnioNoFuturoValidatorTest {

    private AnioNoFuturoValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AnioNoFuturoValidator();
    }

    @Test
    void isValid_null_devuelveTrue() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void isValid_anioPasado_devuelveTrue() {
        int anioPasado = Year.now().getValue() - 1;
        assertTrue(validator.isValid(anioPasado, null));
    }

    @Test
    void isValid_anioActual_devuelveTrue() {
        int anioActual = Year.now().getValue();
        assertTrue(validator.isValid(anioActual, null));
    }

    @Test
    void isValid_anioFuturo_devuelveFalse() {
        int anioFuturo = Year.now().getValue() + 1;
        assertFalse(validator.isValid(anioFuturo, null));
    }
}
