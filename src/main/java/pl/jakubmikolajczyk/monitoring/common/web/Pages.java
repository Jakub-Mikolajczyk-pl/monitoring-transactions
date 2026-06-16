package pl.jakubmikolajczyk.monitoring.common.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/// Builds a [Pageable] from raw request parameters, clamping the page size so a
/// client can never ask for an unbounded result set. Without this cap a single
/// `?size=1000000` request could try to load an entire table into memory - exactly
/// the "fetch all" risk a paginated API exists to remove.
public final class Pages {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private Pages() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(safePage, safeSize, sort);
    }
}
