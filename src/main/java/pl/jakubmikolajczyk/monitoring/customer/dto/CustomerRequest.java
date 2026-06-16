package pl.jakubmikolajczyk.monitoring.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// API input modelled as a record: immutable, compact constructor-validated by
/// Bean Validation, no accidental identity (Java 16+).
public record CustomerRequest(
        @NotBlank @Size(max = 64) String businessId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName) {
}
