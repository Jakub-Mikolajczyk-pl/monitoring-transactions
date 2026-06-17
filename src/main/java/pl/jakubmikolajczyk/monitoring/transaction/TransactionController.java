package pl.jakubmikolajczyk.monitoring.transaction;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import pl.jakubmikolajczyk.monitoring.common.web.PageResponse;
import pl.jakubmikolajczyk.monitoring.common.web.Pages;
import pl.jakubmikolajczyk.monitoring.transaction.dto.TransactionRequest;
import pl.jakubmikolajczyk.monitoring.transaction.dto.TransactionResponse;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transakcje", description = "Niemutowalne fakty finansowe klienta i ich wyszukiwanie.")
class TransactionController {

    private final TransactionService service;

    TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Rejestruje transakcję",
            description = """
                    Zapisuje transakcję jako niemutowalny fakt. Odpowiedź `201 Created` oznacza,
                    że sama transakcja jest już utrwalona; analiza AML i ewentualny alert powstają asynchronicznie.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transakcja zapisana; `Location` wskazuje nowy zasób."),
            @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe (`ProblemDetail` z błędami pól)."),
            @ApiResponse(responseCode = "404", description = "Klient nie istnieje."),
            @ApiResponse(responseCode = "422", description = "`businessId` transakcji nie zgadza się z klientem.")
    })
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
    @Operation(
            summary = "Wyszukuje transakcje",
            description = """
                    Wyszukuje transakcje w wymaganym kontekście biznesowym. `customerId` i zakres dat
                    zawężają wynik, a paginacja chroni API przed nieograniczonymi listami.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strona transakcji."),
            @ApiResponse(responseCode = "400", description = "Brak wymaganego `businessId` lub niepoprawny parametr.")
    })
    PageResponse<TransactionResponse> search(
            @Parameter(description = "Identyfikator kontekstu biznesowego, np. BANK_A.", example = "BANK_A", required = true)
            @RequestParam String businessId,
            @Parameter(description = "Opcjonalny filtr po technicznym UUIDv7 klienta.")
            @RequestParam(required = false) UUID customerId,
            @Parameter(description = "Początek zakresu czasu biznesowego transakcji, ISO-8601.", example = "2026-06-11T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
            @Parameter(description = "Koniec zakresu czasu biznesowego transakcji, ISO-8601.", example = "2026-06-11T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
            @Parameter(description = "Numer strony liczony od 0.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Rozmiar strony; wartości powyżej 100 są obcinane.", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        var criteria = new TransactionSearchCriteria(businessId, customerId, dateFrom, dateTo);
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        return PageResponse.from(service.search(criteria, pageable).map(TransactionResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pobiera transakcję", description = "Zwraca pojedynczą transakcję po technicznym identyfikatorze UUIDv7.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transakcja znaleziona."),
            @ApiResponse(responseCode = "404", description = "Transakcja nie istnieje.")
    })
    TransactionResponse byId(
            @Parameter(description = "UUIDv7 transakcji.", example = "0190abcd-1234-7000-8000-000000000010")
            @PathVariable UUID id) {
        return TransactionResponse.from(service.findById(id));
    }
}
