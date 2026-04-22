package com.compicar.vehiculo.validaciones;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Year;

public class AnioNoFuturoValidator implements ConstraintValidator<AnioNoFuturo, Integer> {

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int anioActual = Year.now().getValue();
        return value <= anioActual;
    }
}
