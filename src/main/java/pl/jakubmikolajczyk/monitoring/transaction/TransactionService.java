package pl.jakubmikolajczyk.monitoring.transaction;

import java.time.InstantSource;
import java.util.List;
import java.util.UUID;

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

    TransactionService(TransactionRepository repository, CustomerService customers, InstantSource clock) {
        this.repository = repository;
        this.customers = customers;
        this.clock = clock;
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
        return repository.save(Transaction.register(
                businessId,
                request.customerId(),
                request.amount(),
                request.currency(),
                request.transactionDate(),
                clock));
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
