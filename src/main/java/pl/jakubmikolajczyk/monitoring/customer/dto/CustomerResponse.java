package pl.jakubmikolajczyk.monitoring.customer.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.jakubmikolajczyk.monitoring.customer.Customer;

@Schema(description = "Klient zarejestrowany w systemie.")
public record CustomerResponse(
        @Schema(description = "Techniczny identyfikator klienta (UUIDv7).", example = "0190abcd-1234-7000-8000-000000000001")
        UUID id,
        @Schema(description = "Identyfikator kontekstu biznesowego, np. BANK_A.", example = "BANK_A")
        String businessId,
        @Schema(description = "Imię klienta.", example = "Jan")
        String firstName,
        @Schema(description = "Nazwisko klienta.", example = "Kowalski")
        String lastName,
        @Schema(description = "Czas rejestracji klienta w systemie.", example = "2026-06-11T10:15:30Z")
        Instant createdAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getBusinessId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getCreatedAt());
    }
}
