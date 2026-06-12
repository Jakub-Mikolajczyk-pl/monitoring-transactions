package pl.jakubmikolajczyk.monitoring.alert;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pl.jakubmikolajczyk.monitoring.alert.dto.AlertResponse;

@RestController
@RequestMapping("/api/alerts")
class AlertController {

    private final AlertService service;

    AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    List<AlertResponse> list(@RequestParam(required = false) AlertStatus status) {
        return service.findAll(status).stream().map(AlertResponse::from).toList();
    }
}
