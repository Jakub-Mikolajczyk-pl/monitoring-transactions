package pl.jakubmikolajczyk.monitoring.alert;

import java.time.InstantSource;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.jakubmikolajczyk.monitoring.alert.dto.DecisionRequest;
import pl.jakubmikolajczyk.monitoring.common.web.ConflictException;
import pl.jakubmikolajczyk.monitoring.common.web.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository repository;
    private final AlertDecisionRepository decisions;
    private final InstantSource clock;

    AlertService(AlertRepository repository, AlertDecisionRepository decisions, InstantSource clock) {
        this.repository = repository;
        this.decisions = decisions;
        this.clock = clock;
    }

    /// Raises one alert per transaction. The exists-check is a cheap fast path;
    /// the real guarantee is the unique constraint on transaction_id - a concurrent
    /// duplicate surfaces as DataIntegrityViolationException at the caller (ADR-0007).
    @Transactional
    public void raise(String businessId, UUID transactionId, List<AlertReason> reasons) {
        if (repository.existsByTransactionId(transactionId)) {
            log.info("Alert for transaction {} already exists, skipping", transactionId);
            return;
        }
        var alert = repository.save(Alert.raise(businessId, transactionId, reasons, clock));
        log.info("Alert {} raised for transaction {} with reasons {}",
                alert.getId(), transactionId, alert.getReason());
    }

    /// Records the analyst's verdict (append-only) and moves the alert's status.
    /// The client echoes the version it saw; a mismatch means somebody else decided
    /// in the meantime -> 409, reload, re-decide (ADR-0008). The @Version bump at
    /// flush additionally guards the race window between this check and the commit.
    @Transactional
    public AlertDecision decide(UUID alertId, DecisionRequest request) {
        var alert = findById(alertId);
        if (alert.getVersion() != request.alertVersion()) {
            throw new ConflictException(
                    "Alert %s changed since it was loaded (expected version %d, current %d); reload and decide again"
                            .formatted(alertId, request.alertVersion(), alert.getVersion()));
        }
        alert.applyDecision(request.decision());
        var decision = decisions.save(AlertDecision.of(
                alertId, request.decision(), request.comment().strip(), clock));
        log.info("Decision {} recorded for alert {} -> status {}",
                request.decision(), alertId, alert.getStatus());
        return decision;
    }

    public List<AlertDecision> decisionHistory(UUID alertId) {
        return decisions.findAllByAlertIdOrderByCreatedAtDesc(alertId);
    }

    /// Optional status filter keeps the analyst's queue views cheap; sort and page
    /// size come from the controller via the Pageable.
    public Page<Alert> findAll(AlertStatus status, Pageable pageable) {
        return status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
    }

    public Alert findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
    }
}
