package pl.jakubmikolajczyk.monitoring.alert.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import pl.jakubmikolajczyk.monitoring.alert.Decision;

/// `comment` is mandatory: an audit decision without justification is worthless.
/// `alertVersion` echoes the version the analyst saw when deciding - the server
/// compares it against the current one to detect lost updates (ADR-0008).
@Schema(description = "Decyzja analityka dopisywana do historii alertu.")
public record DecisionRequest(
        @Schema(description = "Werdykt analityka dla alertu.", example = "APPROVE")
        @NotNull Decision decision,
        @Schema(description = "Uzasadnienie decyzji, przechowywane w historii audytowej.", example = "Zweryfikowano z klientem.", maxLength = 1000)
        @NotBlank @Size(max = 1000) String comment,
        @Schema(description = "Wersja alertu widziana przez analityka w momencie decyzji.", example = "0")
        @NotNull @PositiveOrZero Long alertVersion) {
}
