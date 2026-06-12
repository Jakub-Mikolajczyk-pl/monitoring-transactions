package pl.jakubmikolajczyk.monitoring.alert;

import java.time.InstantSource;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pl.jakubmikolajczyk.monitoring.common.web.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository repository;
    private final InstantSource clock;

    AlertService(AlertRepository repository, InstantSource clock) {
        this.repository = repository;
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

    /// Optional status filter keeps the analyst's queue views (OPEN first) cheap.
    public List<Alert> findAll(AlertStatus status) {
        return status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findAllByStatusOrderByCreatedAtDesc(status);
    }

    public Alert findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
    }
}
