package pl.jakubmikolajczyk.monitoring.customer.dto;

import java.time.Instant;
import java.util.UUID;

import pl.jakubmikolajczyk.monitoring.customer.Customer;

public record CustomerResponse(
        UUID id,
        String businessId,
        String firstName,
        String lastName,
        String email,
        Instant createdAt) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getBusinessId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getCreatedAt());
    }
}
