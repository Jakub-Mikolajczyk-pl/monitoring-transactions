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

export const api = {
    customers: {
        list: () => request('/api/customers'),
        create: (data) => request('/api/customers', { method: 'POST', body: JSON.stringify(data) }),
    },
    transactions: {
        search: (params) => request(`/api/transactions?${new URLSearchParams(params)}`),
        create: (data) => request('/api/transactions', { method: 'POST', body: JSON.stringify(data) }),
    },
    alerts: {
        list: (status) => request(`/api/alerts${status ? `?status=${status}` : ''}`),
        details: (id) => request(`/api/alerts/${id}`),
        decide: (id, data) => request(`/api/alerts/${id}/decisions`, {
            method: 'POST',
            body: JSON.stringify(data),
        }),
    },
};
