package pl.jakubmikolajczyk.monitoring.customer;

import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                clock);
        return repository.save(customer);
    }

    public List<Customer> findAll() {
        return repository.findAllByOrderByCreatedAtDesc();
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
