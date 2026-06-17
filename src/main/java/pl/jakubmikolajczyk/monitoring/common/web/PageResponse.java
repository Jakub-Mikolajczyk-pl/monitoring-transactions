package pl.jakubmikolajczyk.monitoring.common.web;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

/// Stable pagination envelope for list endpoints. We expose this instead of
/// serializing Spring Data's `Page` directly: `PageImpl`'s JSON shape is an
/// implementation detail Spring itself warns about, and a record pins the contract
/// (content + the few numbers a client actually needs to render a pager).
@Schema(description = "Stabilna koperta paginacji dla endpointów listujących.")
public record PageResponse<T>(
        @Schema(description = "Elementy bieżącej strony.")
        List<T> content,
        @Schema(description = "Numer strony liczony od 0.", example = "0")
        int page,
        @Schema(description = "Rozmiar strony po zastosowaniu limitu bezpieczeństwa.", example = "20")
        int size,
        @Schema(description = "Łączna liczba elementów pasujących do zapytania.", example = "42")
        long totalElements,
        @Schema(description = "Łączna liczba stron.", example = "3")
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
