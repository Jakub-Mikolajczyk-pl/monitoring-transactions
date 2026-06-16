package pl.jakubmikolajczyk.monitoring.customer;

import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import pl.jakubmikolajczyk.monitoring.common.id.Uuids;

/// A customer registered within a business context. `businessId` identifies that
/// context (e.g. a bank unit) and is deliberately not unique — many customers and
/// their transactions share it (ADR-0003).
///
/// JPA entities stay mutable classes by design (proxies, lazy loading); immutable
/// record-style modelling is reserved for DTOs and events.
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    private UUID id;

    @Column(name = "business_id", nullable = false, updatable = false, length = 64)
    private String businessId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Customer() {
        // JPA only
    }

    private Customer(UUID id, String businessId, String firstName, String lastName, Instant createdAt) {
        this.id = id;
        this.businessId = businessId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
    }

    public static Customer register(String businessId, String firstName, String lastName, InstantSource clock) {
        return new Customer(Uuids.v7(), businessId, firstName, lastName, clock.instant());
    }

    public UUID getId() {
        return id;
    }

    public String getBusinessId() {
        return businessId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
