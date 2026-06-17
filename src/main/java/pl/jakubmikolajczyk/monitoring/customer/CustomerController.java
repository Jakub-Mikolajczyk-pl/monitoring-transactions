package pl.jakubmikolajczyk.monitoring.customer;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
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
import pl.jakubmikolajczyk.monitoring.customer.dto.CustomerRequest;
import pl.jakubmikolajczyk.monitoring.customer.dto.CustomerResponse;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Klienci", description = "Rejestr klientów w kontekście biznesowym, np. BANK_A.")
class CustomerController {

    private final CustomerService service;

    CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
            summary = "Rejestruje klienta",
            description = """
                    Dodaje klienta do wskazanego kontekstu biznesowego. `businessId` nie jest
                    unikalnym identyfikatorem rekordu, tylko partycją/kontekstem danych.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Klient zapisany; `Location` wskazuje nowy zasób."),
            @ApiResponse(responseCode = "400", description = "Niepoprawne dane wejściowe (`ProblemDetail` z błędami pól).")
    })
    ResponseEntity<CustomerResponse> register(@Valid @RequestBody CustomerRequest request) {
        var customer = service.register(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customer.getId())
                .toUri();
        return ResponseEntity.created(location).body(CustomerResponse.from(customer));
    }

    @GetMapping
    @Operation(
            summary = "Listuje klientów",
            description = "Zwraca klientów posortowanych od najnowszych, w stabilnej kopercie paginacji.")
    @ApiResponse(responseCode = "200", description = "Strona klientów.")
    PageResponse<CustomerResponse> list(
            @Parameter(description = "Numer strony liczony od 0.", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Rozmiar strony; wartości powyżej 100 są obcinane.", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(service.findAll(pageable).map(CustomerResponse::from));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Pobiera klienta", description = "Zwraca pojedynczego klienta po technicznym identyfikatorze UUIDv7.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Klient znaleziony."),
            @ApiResponse(responseCode = "404", description = "Klient nie istnieje.")
    })
    CustomerResponse byId(
            @Parameter(description = "UUIDv7 klienta.", example = "0190abcd-1234-7000-8000-000000000001")
            @PathVariable UUID id) {
        return CustomerResponse.from(service.findById(id));
    }
}
