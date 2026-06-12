package pl.jakubmikolajczyk.monitoring.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/// Validates that a String is an ISO 4217 currency code known to the JDK (ADR-0004).
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CurrencyValidator.class)
public @interface ValidCurrency {

    String message() default "must be an ISO 4217 currency code (e.g. PLN)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
