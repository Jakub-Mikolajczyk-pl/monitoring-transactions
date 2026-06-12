package pl.jakubmikolajczyk.monitoring.transaction;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import pl.jakubmikolajczyk.monitoring.transaction.dto.TransactionRequest;
import pl.jakubmikolajczyk.monitoring.transaction.dto.TransactionResponse;

@RestController
@RequestMapping("/api/transactions")
class TransactionController {

    private final TransactionService service;

    TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<TransactionResponse> register(@Valid @RequestBody TransactionRequest request) {
        var transaction = service.register(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(transaction.getId())
                .toUri();
        // 201, not 202: the transaction itself is durably stored when we respond;
        // only the AML analysis (a different resource - the alert) is async (ADR-0009).
        return ResponseEntity.created(location).body(TransactionResponse.from(transaction));
    }

    @GetMapping
    List<TransactionResponse> search(
            @RequestParam String businessId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo) {
        var criteria = new TransactionSearchCriteria(businessId, customerId, dateFrom, dateTo);
        return service.search(criteria).stream().map(TransactionResponse::from).toList();
    }

    @GetMapping("/{id}")
    TransactionResponse byId(@PathVariable UUID id) {
        return TransactionResponse.from(service.findById(id));
    }
}
