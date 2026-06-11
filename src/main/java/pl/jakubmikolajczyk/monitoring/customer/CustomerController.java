package pl.jakubmikolajczyk.monitoring.customer;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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
    List<CustomerResponse> list() {
        return service.findAll().stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{id}")
    CustomerResponse byId(@PathVariable UUID id) {
        return CustomerResponse.from(service.findById(id));
    }
}
