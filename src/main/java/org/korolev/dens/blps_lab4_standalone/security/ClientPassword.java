package org.korolev.dens.blps_lab4_standalone.security;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordValidator.class)
public @interface ClientPassword {

    String message() default "Invalid client Password";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
