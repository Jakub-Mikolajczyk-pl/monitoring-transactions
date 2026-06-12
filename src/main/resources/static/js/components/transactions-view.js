import { api, ApiError } from '../api.js';
import { esc, fmtDateTime, fmtMoney, shortId } from '../format.js';
import { sharedStyles } from '../shared-styles.js';

// Transaction search (businessId required - mirrors the API contract, REQ-06) and
// registration. Backend problem+json errors are rendered verbatim: the server is
// the source of truth for messages.

const CURRENCIES = ['PLN', 'EUR', 'USD', 'GBP', 'CHF'];

class TransactionsView extends HTMLElement {

    constructor() {
        super();
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.adoptedStyleSheets = [sharedStyles];
        shadow.innerHTML = `
            <div class="view">
                <div class="view-header">
                    <h2>Transakcje</h2>
                    <p class="lead">Wyszukiwanie w ramach kontekstu biznesowego oraz rejestracja nowych transakcji.</p>
                </div>

                <form class="card" id="search-form" novalidate>
                    <h3>Wyszukiwanie</h3>
                    <div class="form-grid">
                        <label>businessId
                            <input name="businessId" required maxlength="64" placeholder="np. BANK_A">
                        </label>
                        <label>customerId <span class="optional">(opcjonalnie)</span>
                            <input name="customerId" placeholder="UUID klienta">
                        </label>
                        <label>Od <span class="optional">(opcjonalnie)</span>
                            <input name="dateFrom" type="datetime-local">
                        </label>
                        <label>Do <span class="optional">(opcjonalnie)</span>
                            <input name="dateTo" type="datetime-local">
                        </label>
                        <button class="btn btn-primary" type="submit">Szukaj</button>
                    </div>
                    <p class="banner" id="search-banner" hidden></p>
                </form>

                <form class="card" id="register-form" novalidate>
                    <h3>Nowa transakcja</h3>
                    <div class="form-grid">
                        <label>businessId
                            <input name="businessId" required maxlength="64">
                            <span class="field-error" data-error-for="businessId"></span>
                        </label>
                        <label>customerId
                            <input name="customerId" required placeholder="UUID klienta">
                            <span class="field-error" data-error-for="customerId"></span>
                        </label>
                        <label>Kwota
                            <input name="amount" required inputmode="decimal" placeholder="1500.75">
                            <span class="field-error" data-error-for="amount"></span>
                        </label>
                        <label>Waluta
                            <select name="currency">
                                ${CURRENCIES.map((c) => `<option>${c}</option>`).join('')}
                            </select>
                            <span class="field-error" data-error-for="currency"></span>
                        </label>
                        <label>Data transakcji
                            <input name="transactionDate" type="datetime-local" required>
                            <span class="field-error" data-error-for="transactionDate"></span>
                        </label>
                        <button class="btn btn-primary" type="submit">Zarejestruj</button>
                    </div>
                    <p class="banner" id="register-banner" hidden></p>
                </form>

                <div class="card" id="results">
                    <div class="empty">Podaj businessId i kliknij „Szukaj”.</div>
                </div>
            </div>
        `;
        shadow.getElementById('search-form').addEventListener('submit', this.#onSearch);
        shadow.getElementById('register-form').addEventListener('submit', this.#onRegister);
    }

    connectedCallback() {
        // sensible default for manual testing: now, in local time
        const input = this.shadowRoot.querySelector('#register-form [name="transactionDate"]');
        input.value = new Date(Date.now() - 60_000).toISOString().slice(0, 16);
    }

    #onSearch = async (event) => {
        event.preventDefault();
        const banner = this.shadowRoot.getElementById('search-banner');
        const results = this.shadowRoot.getElementById('results');
        banner.hidden = true;
        const raw = Object.fromEntries(new FormData(event.target));
        if (!raw.businessId.trim()) {
            banner.textContent = 'businessId jest wymagany do wyszukiwania.';
            banner.className = 'banner banner-warn';
            banner.hidden = false;
            return;
        }
        const params = { businessId: raw.businessId.trim() };
        if (raw.customerId.trim()) params.customerId = raw.customerId.trim();
        if (raw.dateFrom) params.dateFrom = new Date(raw.dateFrom).toISOString();
        if (raw.dateTo) params.dateTo = new Date(raw.dateTo).toISOString();
        try {
            this.#renderResults(await api.transactions.search(params));
        } catch (error) {
            if (error instanceof ApiError) {
                banner.textContent = error.message;
                banner.className = 'banner banner-error';
                banner.hidden = false;
                results.innerHTML = '<div class="empty">Popraw kryteria wyszukiwania.</div>';
            } else {
                throw error;
            }
        }
    };

    #onRegister = async (event) => {
        event.preventDefault();
        const form = event.target;
        const banner = this.shadowRoot.getElementById('register-banner');
        banner.hidden = true;
        this.shadowRoot.querySelectorAll('#register-form .field-error')
            .forEach((el) => (el.textContent = ''));
        const raw = Object.fromEntries(new FormData(form));
        const data = {
            businessId: raw.businessId.trim(),
            customerId: raw.customerId.trim(),
            amount: raw.amount === '' ? null : Number(raw.amount.replace(',', '.')),
            currency: raw.currency,
            transactionDate: raw.transactionDate ? new Date(raw.transactionDate).toISOString() : null,
        };
        try {
            const transaction = await api.transactions.create(data);
            banner.textContent = `Zarejestrowano transakcję ${fmtMoney(transaction.amount, transaction.currency)}. Analiza AML działa w tle.`;
            banner.className = 'banner banner-info';
            banner.hidden = false;
            form.querySelector('[name="amount"]').value = '';
        } catch (error) {
            if (error instanceof ApiError) {
                error.fieldErrors.forEach(({ field, message }) => {
                    const slot = this.shadowRoot.querySelector(`#register-form [data-error-for="${field}"]`);
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

    #renderResults(transactions) {
        const container = this.shadowRoot.getElementById('results');
        if (transactions.length === 0) {
            container.innerHTML = '<div class="empty">Brak transakcji dla podanych kryteriów.</div>';
            return;
        }
        container.innerHTML = `
            <table>
                <thead>
                    <tr>
                        <th>Data transakcji</th><th class="right">Kwota</th>
                        <th>businessId</th><th>Klient</th><th>Id</th>
                    </tr>
                </thead>
                <tbody>
                    ${transactions.map((t) => `
                        <tr>
                            <td>${fmtDateTime(t.transactionDate)}</td>
                            <td class="right"><strong>${fmtMoney(t.amount, t.currency)}</strong></td>
                            <td class="mono">${esc(t.businessId)}</td>
                            <td class="mono muted" title="${esc(t.customerId)}">${shortId(t.customerId)}</td>
                            <td class="mono muted" title="${esc(t.id)}">${shortId(t.id)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    }
}

customElements.define('transactions-view', TransactionsView);
