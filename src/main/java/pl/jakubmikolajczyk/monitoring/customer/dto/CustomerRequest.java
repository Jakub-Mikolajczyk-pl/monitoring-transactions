package pl.jakubmikolajczyk.monitoring.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// API input modelled as a record: immutable, compact constructor-validated by
/// Bean Validation, no accidental identity (Java 16+).
///
/// `email` is optional (nullable). When present it must be a syntactically valid
/// address. The default `@Email` is intentionally lenient (it would accept
/// `a@b`), so the `regexp` tightens it to require a dotted domain. Note that the
/// only authoritative proof an address exists is a confirmation message - syntax
/// validation just rejects obvious typos early.
public record CustomerRequest(
        @NotBlank @Size(max = 64) String businessId,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") @Size(max = 254) String email) {
}
