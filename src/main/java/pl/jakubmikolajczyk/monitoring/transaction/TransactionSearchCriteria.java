package pl.jakubmikolajczyk.monitoring.transaction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/// Search filters from REQ-06: the business context is mandatory, everything else
/// narrows it down. A record with a compact constructor keeps the invariants
/// (required businessId, ordered date range) right next to the data they protect.
public record TransactionSearchCriteria(
        String businessId,
        UUID customerId,
        Instant dateFrom,
        Instant dateTo) {

    public TransactionSearchCriteria {
        if (businessId == null || businessId.isBlank()) {
            throw new IllegalArgumentException("businessId filter is required");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("dateFrom must not be after dateTo");
        }
    }

    /// Single dynamic query instead of 8 derived-query permutations. The mandatory
    /// businessId predicate matches the (business_id, transaction_date) index.
    Specification<Transaction> toSpecification() {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.equal(root.get("businessId"), businessId));
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), customerId));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), dateTo));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
