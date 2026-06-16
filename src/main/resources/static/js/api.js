// The only gateway to the backend. Understands RFC 9457 application/problem+json
// and turns failures into ApiError instances the views can render directly.

export class ApiError extends Error {
    constructor(problem, status) {
        super(problem.detail || problem.title || `HTTP ${status}`);
        this.status = status;
        this.problem = problem;
    }

    /** Field-level validation errors: [{field, message}] */
    get fieldErrors() {
        return this.problem.errors ?? [];
    }
}

/** Builds a query string from an object, dropping null/undefined/empty values. */
function query(params = {}) {
    const search = new URLSearchParams();
    for (const [key, value] of Object.entries(params)) {
        if (value !== null && value !== undefined && value !== '') {
            search.set(key, value);
        }
    }
    const serialized = search.toString();
    return serialized ? `?${serialized}` : '';
}

async function request(path, options = {}) {
    const response = await fetch(path, {
        headers: { 'Content-Type': 'application/json' },
        ...options,
    });
    if (response.status === 204) {
        return null;
    }
    const body = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new ApiError(body, response.status);
    }
    return body;
}

// List endpoints return a pagination envelope: { content, page, size, totalElements, totalPages }.
export const api = {
    customers: {
        list: (params) => request(`/api/customers${query(params)}`),
        create: (data) => request('/api/customers', { method: 'POST', body: JSON.stringify(data) }),
    },
    transactions: {
        search: (params) => request(`/api/transactions${query(params)}`),
        create: (data) => request('/api/transactions', { method: 'POST', body: JSON.stringify(data) }),
    },
    alerts: {
        list: (params) => request(`/api/alerts${query(params)}`),
        details: (id) => request(`/api/alerts/${id}`),
        decide: (id, data) => request(`/api/alerts/${id}/decisions`, {
            method: 'POST',
            body: JSON.stringify(data),
        }),
    },
};
