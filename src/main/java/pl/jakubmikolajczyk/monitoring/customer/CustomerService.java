package pl.jakubmikolajczyk.monitoring.customer;

import java.time.InstantSource;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.jakubmikolajczyk.monitoring.common.web.ResourceNotFoundException;
import pl.jakubmikolajczyk.monitoring.customer.dto.CustomerRequest;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository repository;
    private final InstantSource clock;

    CustomerService(CustomerRepository repository, InstantSource clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Customer register(CustomerRequest request) {
        var customer = Customer.register(
                request.businessId().strip(),
                request.firstName().strip(),
                request.lastName().strip(),
                normalizeEmail(request.email()),
                clock);
        return repository.save(customer);
    }

    /// Blank and missing e-mail collapse to null: a half-empty string in the
    /// database is a future bug waiting to happen.
    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.strip();
    }

    public Page<Customer> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Customer findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    /// Cross-feature lookup that lets callers decide their own error semantics
    /// (e.g. transaction registration maps a missing customer to 422, not 404).
    public Optional<Customer> lookup(UUID id) {
        return repository.findById(id);
    }
}
