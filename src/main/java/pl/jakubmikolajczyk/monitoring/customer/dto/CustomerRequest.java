package pl.jakubmikolajczyk.monitoring.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

/// API input modelled as a record: immutable, compact constructor-validated by
/// Bean Validation, no accidental identity (Java 16+).
@Schema(description = "Dane wymagane do rejestracji klienta.")
public record CustomerRequest(
        @Schema(description = "Identyfikator kontekstu biznesowego, np. BANK_A.", example = "BANK_A", maxLength = 64)
        @NotBlank @Size(max = 64) String businessId,
        @Schema(description = "Imię klienta.", example = "Jan", maxLength = 100)
        @NotBlank @Size(max = 100) String firstName,
        @Schema(description = "Nazwisko klienta.", example = "Kowalski", maxLength = 100)
        @NotBlank @Size(max = 100) String lastName) {
}
