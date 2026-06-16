package pl.jakubmikolajczyk.monitoring.alert;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pl.jakubmikolajczyk.monitoring.alert.dto.AlertDetailsResponse;
import pl.jakubmikolajczyk.monitoring.alert.dto.AlertResponse;
import pl.jakubmikolajczyk.monitoring.alert.dto.DecisionRequest;
import pl.jakubmikolajczyk.monitoring.alert.dto.DecisionResponse;
import pl.jakubmikolajczyk.monitoring.common.web.PageResponse;
import pl.jakubmikolajczyk.monitoring.common.web.Pages;
import pl.jakubmikolajczyk.monitoring.customer.CustomerService;
import pl.jakubmikolajczyk.monitoring.customer.dto.CustomerResponse;
import pl.jakubmikolajczyk.monitoring.transaction.TransactionService;
import pl.jakubmikolajczyk.monitoring.transaction.dto.TransactionResponse;

@RestController
@RequestMapping("/api/alerts")
class AlertController {

    private final AlertService service;
    private final TransactionService transactions;
    private final CustomerService customers;

    AlertController(AlertService service, TransactionService transactions, CustomerService customers) {
        this.service = service;
        this.transactions = transactions;
        this.customers = customers;
    }

    @GetMapping
    PageResponse<AlertResponse> list(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(service.findAll(status, pageable).map(AlertResponse::from));
    }

    /// Composition endpoint for the analyst's detail view (REQ-04): the alert, the
    /// transaction that triggered it, the customer behind it and the full decision
    /// history - one request, everything the drawer needs.
    @GetMapping("/{id}")
    AlertDetailsResponse details(@PathVariable UUID id) {
        var alert = service.findById(id);
        var transaction = transactions.findById(alert.getTransactionId());
        var customer = customers.findById(transaction.getCustomerId());
        var history = service.decisionHistory(id).stream().map(DecisionResponse::from).toList();
        return AlertDetailsResponse.of(alert,
                TransactionResponse.from(transaction),
                CustomerResponse.from(customer),
                history);
    }

    @PostMapping("/{id}/decisions")
    @ResponseStatus(HttpStatus.CREATED)
    DecisionResponse decide(@PathVariable UUID id, @Valid @RequestBody DecisionRequest request) {
        return DecisionResponse.from(service.decide(id, request));
    }
}
