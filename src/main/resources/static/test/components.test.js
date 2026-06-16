import { describe, it, assert, assertEquals, stubFetch, jsonResponse, mount, tick } from './test-runner.js';
import '../js/components/customers-view.js';
import '../js/components/alerts-view.js';

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

describe('<alerts-view>', () => {
    it('renders one row per alert from the page envelope', async () => {
        const restore = stubFetch(() => jsonResponse(200, {
            content: [
                { id: 'a1', status: 'OPEN', reason: 'SUSPICIOUS_AMOUNT', businessId: 'BANK', createdAt: '2026-06-15T09:00:00Z', transactionId: 't1' },
                { id: 'a2', status: 'APPROVED', reason: 'SUSPICIOUS_AMOUNT,HIGH_FREQUENCY', businessId: 'BANK', createdAt: '2026-06-15T10:00:00Z', transactionId: 't2' },
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
        element.remove();
    });
});
