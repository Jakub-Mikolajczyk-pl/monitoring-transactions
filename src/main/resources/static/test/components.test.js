import { describe, it, assert, assertEquals, stubFetch, jsonResponse, mount, tick } from './test-runner.js';
import '../js/components/customers-view.js';
import '../js/components/transactions-view.js';
import '../js/components/alerts-view.js';
import '../js/components/alert-details.js';

function fillCustomerForm(shadow, { businessId = 'BANK', firstName = 'Jan', lastName = 'Kowalski' } = {}) {
    shadow.querySelector('[name="businessId"]').value = businessId;
    shadow.querySelector('[name="firstName"]').value = firstName;
    shadow.querySelector('[name="lastName"]').value = lastName;
}

describe('<customer-form>', () => {
    it('emits a customer-registered event after a successful POST', async () => {
        const restore = stubFetch(() => jsonResponse(201, { id: 'x', firstName: 'Jan', lastName: 'Kowalski' }));
        const element = mount('customer-form');
        let detail = null;
        element.addEventListener('customer-registered', (event) => { detail = event.detail; });

        fillCustomerForm(element.shadowRoot, {});
        element.shadowRoot.querySelector('form').requestSubmit();
        await tick(50);
        restore();

        assert(detail !== null, 'event fired');
        assertEquals(detail.firstName, 'Jan');
        element.remove();
    });

    it('renders the backend field error next to the offending input', async () => {
        const restore = stubFetch(() => jsonResponse(400, {
            title: 'Bad request',
            errors: [{ field: 'lastName', message: 'nazwisko jest wymagane' }],
        }));
        const element = mount('customer-form');

        fillCustomerForm(element.shadowRoot, { lastName: '' });
        element.shadowRoot.querySelector('form').requestSubmit();
        await tick(50);
        restore();

        const errorText = element.shadowRoot.querySelector('[data-error-for="lastName"]').textContent;
        assertEquals(errorText, 'nazwisko jest wymagane');
        element.remove();
    });
});

describe('<customers-view>', () => {
    it('renders the full customer id so it can be copied into transaction forms', async () => {
        const customerId = '0190abcd-1234-7000-8000-000000000001';
        const restore = stubFetch(() => jsonResponse(200, {
            content: [
                {
                    id: customerId,
                    businessId: 'BANK',
                    firstName: 'Jan',
                    lastName: 'Kowalski',
                    createdAt: '2026-06-15T09:00:00Z',
                },
            ],
            page: 0, size: 20, totalElements: 1, totalPages: 1,
        }));
        const element = mount('customers-view');
        await tick(80);
        restore();

        assert(element.shadowRoot.textContent.includes(customerId), 'full customer id is visible');
        assert(!element.shadowRoot.textContent.includes('0190abcd…'), 'customer id is not abbreviated');
        element.remove();
    });
});

function fillTransactionSearch(shadow, {
    businessId = 'BANK',
    customerId = '',
    dateFrom = '',
    dateTo = '',
} = {}) {
    const form = shadow.getElementById('search-form');
    form.querySelector('[name="businessId"]').value = businessId;
    form.querySelector('[name="customerId"]').value = customerId;
    form.querySelector('[name="dateFrom"]').value = dateFrom;
    form.querySelector('[name="dateTo"]').value = dateTo;
}

function fillTransactionRegistration(shadow, {
    businessId = 'BANK',
    customerId = '0190abcd-1234-7000-8000-000000000001',
    amount = '10.00',
    transactionDate = '2026-06-15T09:00',
} = {}) {
    const form = shadow.getElementById('register-form');
    form.querySelector('[name="businessId"]').value = businessId;
    form.querySelector('[name="customerId"]').value = customerId;
    form.querySelector('[name="amount"]').value = amount;
    form.querySelector('[name="transactionDate"]').value = transactionDate;
}

describe('<transactions-view>', () => {
    it('renders the full transaction id so it can be copied', async () => {
        const transactionId = '0190abcd-1234-7000-8000-000000000010';
        const restore = stubFetch(() => jsonResponse(200, {
            content: [
                {
                    id: transactionId,
                    businessId: 'BANK',
                    customerId: '0190abcd-1234-7000-8000-000000000001',
                    amount: 2500.50,
                    currency: 'PLN',
                    transactionDate: '2026-06-15T09:00:00Z',
                    createdAt: '2026-06-15T09:00:01Z',
                },
            ],
            page: 0, size: 20, totalElements: 1, totalPages: 1,
        }));
        const element = mount('transactions-view');
        fillTransactionSearch(element.shadowRoot);
        element.shadowRoot.getElementById('search-form').requestSubmit();
        await tick(80);
        restore();

        assert(element.shadowRoot.textContent.includes(transactionId), 'full transaction id is visible');
        assert(!element.shadowRoot.textContent.includes('0190abcd…'), 'transaction id is not abbreviated');
        element.remove();
    });

    it('shows local registration errors instead of sending malformed transaction data', async () => {
        let calls = 0;
        const restore = stubFetch(() => {
            calls += 1;
            return jsonResponse(500, {});
        });
        const element = mount('transactions-view');
        fillTransactionRegistration(element.shadowRoot, {
            businessId: '',
            customerId: 'not-a-uuid',
            amount: 'abc',
            transactionDate: '',
        });
        element.shadowRoot.getElementById('register-form').requestSubmit();
        await tick(50);
        restore();

        assertEquals(calls, 0, 'no request is sent');
        assertEquals(element.shadowRoot.querySelector('#register-form [data-error-for="businessId"]').textContent,
            'businessId jest wymagany.');
        assertEquals(element.shadowRoot.querySelector('#register-form [data-error-for="customerId"]').textContent,
            'Podaj pełny UUID klienta.');
        assertEquals(element.shadowRoot.querySelector('#register-form [data-error-for="amount"]').textContent,
            'Podaj dodatnią kwotę z maksymalnie 2 miejscami po przecinku.');
        assertEquals(element.shadowRoot.querySelector('#register-form [data-error-for="transactionDate"]').textContent,
            'Data transakcji jest wymagana.');
        element.remove();
    });

    it('shows local search errors for malformed filters and inverted dates', async () => {
        let calls = 0;
        const restore = stubFetch(() => {
            calls += 1;
            return jsonResponse(500, {});
        });
        const element = mount('transactions-view');
        fillTransactionSearch(element.shadowRoot, {
            businessId: '',
            customerId: 'bad-id',
            dateFrom: '2026-06-16T10:00',
            dateTo: '2026-06-15T10:00',
        });
        element.shadowRoot.getElementById('search-form').requestSubmit();
        await tick(50);
        restore();

        assertEquals(calls, 0, 'no request is sent');
        assertEquals(element.shadowRoot.querySelector('#search-form [data-error-for="businessId"]').textContent,
            'businessId jest wymagany.');
        assertEquals(element.shadowRoot.querySelector('#search-form [data-error-for="customerId"]').textContent,
            'Podaj pełny UUID klienta.');
        assertEquals(element.shadowRoot.querySelector('#search-form [data-error-for="dateTo"]').textContent,
            'Data końcowa nie może być wcześniejsza niż początkowa.');
        element.remove();
    });
});

describe('<alerts-view>', () => {
    it('renders one row per alert from the page envelope', async () => {
        const transactionId = '0190abcd-1234-7000-8000-000000000011';
        const restore = stubFetch(() => jsonResponse(200, {
            content: [
                { id: 'a1', status: 'OPEN', reason: 'SUSPICIOUS_AMOUNT', businessId: 'BANK', createdAt: '2026-06-15T09:00:00Z', transactionId },
                { id: 'a2', status: 'APPROVED', reason: 'SUSPICIOUS_AMOUNT,HIGH_FREQUENCY', businessId: 'BANK', createdAt: '2026-06-15T10:00:00Z', transactionId: '0190abcd-1234-7000-8000-000000000012' },
            ],
            page: 0, size: 20, totalElements: 2, totalPages: 1,
        }));
        const element = mount('alerts-view');
        await tick(80);
        restore();

        const rows = element.shadowRoot.querySelectorAll('tbody tr');
        assertEquals(rows.length, 2);
        // the merged-reason alert renders one chip per reason
        const chips = rows[1].querySelectorAll('.chip').length;
        assertEquals(chips, 2);
        assert(element.shadowRoot.textContent.includes(transactionId), 'full transaction id is visible');
        element.remove();
    });
});

describe('<alert-details>', () => {
    it('renders the full transaction id in the detail view', async () => {
        const transactionId = '0190abcd-1234-7000-8000-000000000013';
        const restore = stubFetch(() => jsonResponse(200, {
            id: 'alert-1',
            businessId: 'BANK',
            transactionId,
            status: 'OPEN',
            reason: 'SUSPICIOUS_AMOUNT',
            createdAt: '2026-06-15T09:00:00Z',
            version: 0,
            transaction: {
                id: transactionId,
                businessId: 'BANK',
                customerId: '0190abcd-1234-7000-8000-000000000001',
                amount: 2500.50,
                currency: 'PLN',
                transactionDate: '2026-06-15T09:00:00Z',
                createdAt: '2026-06-15T09:00:01Z',
            },
            customer: {
                id: '0190abcd-1234-7000-8000-000000000001',
                businessId: 'BANK',
                firstName: 'Jan',
                lastName: 'Kowalski',
                createdAt: '2026-06-14T09:00:00Z',
            },
            decisions: [],
        }));
        const element = mount('alert-details', { 'alert-id': 'alert-1' });
        await tick(80);
        restore();

        assert(element.shadowRoot.textContent.includes(transactionId), 'full transaction id is visible');
        assert(!element.shadowRoot.textContent.includes('0190abcd…'), 'transaction id is not abbreviated');
        element.remove();
    });
});
