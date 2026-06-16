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

import pl.jakubmikolajczyk.monitoring.common.web.PageResponse;
import pl.jakubmikolajczyk.monitoring.common.web.Pages;
import pl.jakubmikolajczyk.monitoring.customer.dto.CustomerRequest;
import pl.jakubmikolajczyk.monitoring.customer.dto.CustomerResponse;

@RestController
@RequestMapping("/api/customers")
class CustomerController {

    private final CustomerService service;

    CustomerController(CustomerService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<CustomerResponse> register(@Valid @RequestBody CustomerRequest request) {
        var customer = service.register(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(customer.getId())
                .toUri();
        return ResponseEntity.created(location).body(CustomerResponse.from(customer));
    }

    @GetMapping
    PageResponse<CustomerResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(service.findAll(pageable).map(CustomerResponse::from));
    }

    @GetMapping("/{id}")
    CustomerResponse byId(@PathVariable UUID id) {
        return CustomerResponse.from(service.findById(id));
    }
}
