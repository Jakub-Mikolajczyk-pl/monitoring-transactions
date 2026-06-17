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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alerty", description = "Kolejka alertów AML i decyzje analityka z historią audytową.")
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
    @Operation(
            summary = "Listuje alerty",
            description = "Zwraca kolejkę alertów AML, opcjonalnie zawężoną po statusie.")
    @ApiResponse(responseCode = "200", description = "Strona alertów.")
    PageResponse<AlertResponse> list(
            @Parameter(description = "Opcjonalny status alertu.", example = "OPEN")
            @RequestParam(required = false) AlertStatus status,
            @Parameter(description = "Numer strony liczony od 0.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Rozmiar strony; wartości powyżej 100 są obcinane.", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(service.findAll(status, pageable).map(AlertResponse::from));
    }

    /// Composition endpoint for the analyst's detail view (REQ-04): the alert, the
    /// transaction that triggered it, the customer behind it and the full decision
    /// history - one request, everything the drawer needs.
    @GetMapping("/{id}")
    @Operation(
            summary = "Pobiera szczegóły alertu",
            description = "Komponuje alert, transakcję źródłową, klienta i pełną historię decyzji w jednej odpowiedzi.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Szczegóły alertu."),
            @ApiResponse(responseCode = "404", description = "Alert, transakcja albo klient nie istnieje.")
    })
    AlertDetailsResponse details(
            @Parameter(description = "UUIDv7 alertu.", example = "0190abcd-1234-7000-8000-000000000020")
            @PathVariable UUID id) {
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
    @Operation(
            summary = "Dodaje decyzję analityka",
            description = """
                    Dopisuje decyzję do historii alertu i aktualizuje status alertu. Klient musi odesłać
                    `alertVersion`, którą widział w UI; nieaktualna wersja kończy się `409`.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Decyzja zapisana, status alertu zaktualizowany."),
            @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe (`ProblemDetail` z błędami pól)."),
            @ApiResponse(responseCode = "404", description = "Alert nie istnieje."),
            @ApiResponse(responseCode = "409", description = "Alert został zmieniony przez innego analityka.")
    })
    DecisionResponse decide(
            @Parameter(description = "UUIDv7 alertu.", example = "0190abcd-1234-7000-8000-000000000020")
            @PathVariable UUID id,
            @Valid @RequestBody DecisionRequest request) {
        return DecisionResponse.from(service.decide(id, request));
    }
}
