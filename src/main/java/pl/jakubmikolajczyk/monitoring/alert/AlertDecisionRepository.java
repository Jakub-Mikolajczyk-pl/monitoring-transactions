package pl.jakubmikolajczyk.monitoring.alert;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertDecisionRepository extends JpaRepository<AlertDecision, UUID> {

    List<AlertDecision> findAllByAlertIdOrderByCreatedAtDesc(UUID alertId);
}
