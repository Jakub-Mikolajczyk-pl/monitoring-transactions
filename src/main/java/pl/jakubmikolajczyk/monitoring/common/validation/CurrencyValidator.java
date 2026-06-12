package pl.jakubmikolajczyk.monitoring.common.validation;

import java.util.Currency;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class CurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // presence is @NotNull's responsibility
        }
        try {
            Currency.getInstance(value);
            return true;
        } catch (IllegalArgumentException notACurrencyCode) {
            return false;
        }
    }
}
