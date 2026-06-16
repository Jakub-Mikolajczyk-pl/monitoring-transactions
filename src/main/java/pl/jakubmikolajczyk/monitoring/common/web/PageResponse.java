package pl.jakubmikolajczyk.monitoring.common.web;

import java.util.List;

import org.springframework.data.domain.Page;

/// Stable pagination envelope for list endpoints. We expose this instead of
/// serializing Spring Data's `Page` directly: `PageImpl`'s JSON shape is an
/// implementation detail Spring itself warns about, and a record pins the contract
/// (content + the few numbers a client actually needs to render a pager).
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
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
