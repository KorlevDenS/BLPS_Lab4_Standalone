package org.korolev.dens.blps_lab4_standalone.security;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ClientPassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext constraintValidatorContext) {
        if (password.length() < 8 || password.length() > 20) return false;
        return password.matches("^[a-zA-Z0-9]+$");
    }

}
