import { api, ApiError } from '../api.js';
import { esc, fmtDateTime, shortId } from '../format.js';
import { renderPager, PAGE_SIZE } from '../pagination.js';

// Two cooperating components: <customer-form> emits a composed 'customer-registered'
// CustomEvent, <customers-view> listens and refreshes the table - upward
// communication via events, downward via attributes/properties (ADR-0010).
// Styles: /styles/components.css.

class CustomerForm extends HTMLElement {

    constructor() {
        super();
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <link rel="stylesheet" href="/styles/components.css">
            <form class="card" novalidate>
                <h3>Nowy klient</h3>
                <div class="form-grid">
                    <label>Kontekst biznesowy (businessId)
                        <input name="businessId" required maxlength="64" placeholder="np. BANK_A">
                        <span class="field-error" data-error-for="businessId"></span>
                    </label>
                    <label>Imię
                        <input name="firstName" required maxlength="100" placeholder="Jan">
                        <span class="field-error" data-error-for="firstName"></span>
                    </label>
                    <label>Nazwisko
                        <input name="lastName" required maxlength="100" placeholder="Kowalski">
                        <span class="field-error" data-error-for="lastName"></span>
                    </label>
                    <button class="btn btn-primary" type="submit">Dodaj klienta</button>
                </div>
                <p class="banner" hidden></p>
            </form>
        `;
        shadow.querySelector('form').addEventListener('submit', this.#onSubmit);
    }

    #onSubmit = async (event) => {
        event.preventDefault();
        const form = event.target;
        const banner = this.shadowRoot.querySelector('.banner');
        this.#clearErrors();
        banner.hidden = true;
        try {
            const data = Object.fromEntries(new FormData(form));
            const customer = await api.customers.create(data);
            form.reset();
            banner.textContent = `Dodano klienta ${customer.firstName} ${customer.lastName}.`;
            banner.className = 'banner banner-info';
            banner.hidden = false;
            this.dispatchEvent(new CustomEvent('customer-registered', {
                bubbles: true,
                composed: true,
                detail: customer,
            }));
        } catch (error) {
            if (error instanceof ApiError) {
                error.fieldErrors.forEach(({ field, message }) => {
                    const slot = this.shadowRoot.querySelector(`[data-error-for="${field}"]`);
                    if (slot) slot.textContent = message;
                });
                banner.textContent = error.message;
                banner.className = 'banner banner-error';
                banner.hidden = false;
            } else {
                throw error;
            }
        }
    };

    #clearErrors() {
        this.shadowRoot.querySelectorAll('.field-error').forEach((el) => (el.textContent = ''));
    }
}

class CustomersView extends HTMLElement {

    #page = 0;

    constructor() {
        super();
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <link rel="stylesheet" href="/styles/components.css">
            <div class="view">
                <div class="view-header">
                    <h2>Klienci</h2>
                    <p class="lead">Rejestr klientów w ramach kontekstów biznesowych.</p>
                </div>
                <customer-form></customer-form>
                <div class="card" id="list"><div class="empty">Ładowanie…</div></div>
                <div class="pager" id="pager"></div>
            </div>
        `;
        // A new registration jumps back to the first page so the newest row is visible.
        shadow.addEventListener('customer-registered', () => this.#load(0));
    }

    connectedCallback() {
        this.#load(0);
    }

    async #load(page) {
        this.#page = page;
        const container = this.shadowRoot.getElementById('list');
        const result = await api.customers.list({ page, size: PAGE_SIZE });
        if (result.content.length === 0) {
            container.innerHTML = '<div class="empty">Brak klientów — dodaj pierwszego powyżej.</div>';
            this.shadowRoot.getElementById('pager').innerHTML = '';
            return;
        }
        container.innerHTML = `
            <table>
                <thead>
                    <tr><th>Klient</th><th>businessId</th><th>Utworzono</th><th>Id</th></tr>
                </thead>
                <tbody>
                    ${result.content.map((c) => `
                        <tr>
                            <td><strong>${esc(c.firstName)} ${esc(c.lastName)}</strong></td>
                            <td class="mono">${esc(c.businessId)}</td>
                            <td class="muted">${fmtDateTime(c.createdAt)}</td>
                            <td class="mono muted" title="${esc(c.id)}">${shortId(c.id)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        renderPager(this.shadowRoot.getElementById('pager'), result, (p) => this.#load(p));
    }
}

customElements.define('customer-form', CustomerForm);
customElements.define('customers-view', CustomersView);
