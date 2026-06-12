package pl.jakubmikolajczyk.monitoring.transaction;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository
        extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {

    /// Counts one customer's transactions inside a half-open business-time window
    /// (windowStart, windowEnd]. Rides the (customer_id, transaction_date) index
    /// from migration V2 (REQ-10).
    @Query("""
            select count(t) from Transaction t
            where t.customerId = :customerId
              and t.transactionDate > :windowStart
              and t.transactionDate <= :windowEnd
            """)
    long countInWindow(UUID customerId, Instant windowStart, Instant windowEnd);
}
