package pl.jakubmikolajczyk.monitoring.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import pl.jakubmikolajczyk.monitoring.alert.Decision;

/// `comment` is mandatory: an audit decision without justification is worthless.
/// `alertVersion` echoes the version the analyst saw when deciding - the server
/// compares it against the current one to detect lost updates (ADR-0008).
public record DecisionRequest(
        @NotNull Decision decision,
        @NotBlank @Size(max = 1000) String comment,
        @NotNull @PositiveOrZero Long alertVersion) {
}
