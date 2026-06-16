package pl.jakubmikolajczyk.monitoring.alert;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    boolean existsByTransactionId(UUID transactionId);

    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);
}
