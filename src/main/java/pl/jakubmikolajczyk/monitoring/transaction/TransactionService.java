package pl.jakubmikolajczyk.monitoring.transaction;

import java.time.InstantSource;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.jakubmikolajczyk.monitoring.common.web.BusinessRuleViolationException;
import pl.jakubmikolajczyk.monitoring.common.web.ResourceNotFoundException;
import pl.jakubmikolajczyk.monitoring.customer.CustomerService;
import pl.jakubmikolajczyk.monitoring.transaction.dto.TransactionRequest;

@Service
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository repository;
    private final CustomerService customers;
    private final InstantSource clock;
    private final ApplicationEventPublisher events;

    TransactionService(TransactionRepository repository, CustomerService customers,
            InstantSource clock, ApplicationEventPublisher events) {
        this.repository = repository;
        this.customers = customers;
        this.clock = clock;
        this.events = events;
    }

    @Transactional
    public Transaction register(TransactionRequest request) {
        var businessId = request.businessId().strip();
        var customer = customers.lookup(request.customerId())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Customer %s does not exist".formatted(request.customerId())));
        if (!customer.getBusinessId().equals(businessId)) {
            // ADR-0003: a transaction always happens inside its customer's business
            // context; a mismatch means corrupt input, not a new context.
            throw new BusinessRuleViolationException(
                    "Transaction businessId '%s' does not match customer businessId '%s'"
                            .formatted(businessId, customer.getBusinessId()));
        }
        var transaction = repository.save(Transaction.register(
                businessId,
                request.customerId(),
                request.amount(),
                request.currency(),
                request.transactionDate(),
                clock));
        // Published inside the ongoing transaction; AFTER_COMMIT listeners only run
        // once this data is durable, so analysis can never observe uncommitted state
        // and a rollback never triggers a phantom analysis (ADR-0006).
        events.publishEvent(TransactionRegisteredEvent.of(transaction));
        return transaction;
    }

    public List<Transaction> search(TransactionSearchCriteria criteria) {
        return repository.findAll(criteria.toSpecification(),
                Sort.by(Sort.Direction.DESC, "transactionDate"));
    }

    public Transaction findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", id));
    }
}
