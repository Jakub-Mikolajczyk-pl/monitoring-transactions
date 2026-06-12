package pl.jakubmikolajczyk.monitoring.alert;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    boolean existsByTransactionId(UUID transactionId);

    List<Alert> findAllByOrderByCreatedAtDesc();

    List<Alert> findAllByStatusOrderByCreatedAtDesc(AlertStatus status);
}
