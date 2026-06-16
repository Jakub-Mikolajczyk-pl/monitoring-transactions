package pl.jakubmikolajczyk.monitoring.customer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    // Listing uses the inherited findAll(Pageable); sort is supplied by the controller.
}
