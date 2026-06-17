import { api, ApiError } from '../api.js';
import { esc, fmtDateTime, fmtMoney } from '../format.js';
import { renderPager, PAGE_SIZE } from '../pagination.js';

// Transaction search (businessId required - mirrors the API contract, REQ-06) and
// registration. Backend problem+json errors are rendered verbatim: the server is
// the source of truth for messages. Styles: /styles/components.css.

const CURRENCIES = ['PLN', 'EUR', 'USD', 'GBP', 'CHF'];
const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const AMOUNT_PATTERN = /^\d+([.,]\d{1,2})?$/;

function parseLocalDateTime(value) {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
}

function addError(errors, field, message) {
    errors.push({ field, message });
}

class TransactionsView extends HTMLElement {

    // The active search criteria are remembered so the pager can re-run the same
    // query for a different page rather than re-reading the form.
    #criteria = null;

    constructor() {
        super();
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <link rel="stylesheet" href="/styles/components.css">
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
                            <span class="field-error" data-error-for="businessId"></span>
                        </label>
                        <label>customerId <span class="optional">(opcjonalnie)</span>
                            <input name="customerId" placeholder="UUID klienta">
                            <span class="field-error" data-error-for="customerId"></span>
                        </label>
                        <label>Od <span class="optional">(opcjonalnie)</span>
                            <input name="dateFrom" type="datetime-local">
                            <span class="field-error" data-error-for="dateFrom"></span>
                        </label>
                        <label>Do <span class="optional">(opcjonalnie)</span>
                            <input name="dateTo" type="datetime-local">
                            <span class="field-error" data-error-for="dateTo"></span>
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
                <div class="pager" id="pager"></div>
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

    #onSearch = (event) => {
        event.preventDefault();
        const form = event.target;
        const banner = this.shadowRoot.getElementById('search-banner');
        this.#clearErrors(form);
        banner.hidden = true;
        const raw = Object.fromEntries(new FormData(form));
        const { criteria, errors } = this.#validateSearch(raw);
        if (errors.length > 0) {
            this.#showValidationErrors(form, banner, errors, 'Popraw kryteria wyszukiwania.');
            return;
        }
        this.#criteria = criteria;
        this.#runSearch(0); // a fresh search always starts on the first page
    };

    async #runSearch(page) {
        const banner = this.shadowRoot.getElementById('search-banner');
        const results = this.shadowRoot.getElementById('results');
        try {
            const pageData = await api.transactions.search({ ...this.#criteria, page, size: PAGE_SIZE });
            this.#renderResults(pageData);
        } catch (error) {
            if (error instanceof ApiError) {
                this.#showApiError(this.shadowRoot.getElementById('search-form'), banner, error);
                results.innerHTML = '<div class="empty">Popraw kryteria wyszukiwania.</div>';
                this.shadowRoot.getElementById('pager').innerHTML = '';
            } else {
                throw error;
            }
        }
    }

    #onRegister = async (event) => {
        event.preventDefault();
        const form = event.target;
        const banner = this.shadowRoot.getElementById('register-banner');
        banner.hidden = true;
        this.#clearErrors(form);
        const raw = Object.fromEntries(new FormData(form));
        const { data, errors } = this.#validateRegistration(raw);
        if (errors.length > 0) {
            this.#showValidationErrors(form, banner, errors, 'Popraw dane transakcji.');
            return;
        }
        try {
            const transaction = await api.transactions.create(data);
            banner.textContent =
                `Zarejestrowano transakcję ${fmtMoney(transaction.amount, transaction.currency)} (id: ${transaction.id}). Analiza AML działa w tle.`;
            banner.className = 'banner banner-info';
            banner.hidden = false;
            form.querySelector('[name="amount"]').value = '';
        } catch (error) {
            if (error instanceof ApiError) {
                this.#showApiError(form, banner, error);
            } else {
                throw error;
            }
        }
    };

    #validateSearch(raw) {
        const errors = [];
        const businessId = raw.businessId.trim();
        const customerId = raw.customerId.trim();
        const dateFrom = parseLocalDateTime(raw.dateFrom);
        const dateTo = parseLocalDateTime(raw.dateTo);
        if (!businessId) {
            addError(errors, 'businessId', 'businessId jest wymagany.');
        }
        if (customerId && !UUID_PATTERN.test(customerId)) {
            addError(errors, 'customerId', 'Podaj pełny UUID klienta.');
        }
        if (raw.dateFrom && !dateFrom) {
            addError(errors, 'dateFrom', 'Data początkowa ma niepoprawny format.');
        }
        if (raw.dateTo && !dateTo) {
            addError(errors, 'dateTo', 'Data końcowa ma niepoprawny format.');
        }
        if (dateFrom && dateTo && dateFrom > dateTo) {
            addError(errors, 'dateTo', 'Data końcowa nie może być wcześniejsza niż początkowa.');
        }
        const criteria = { businessId };
        if (customerId) criteria.customerId = customerId;
        if (dateFrom) criteria.dateFrom = dateFrom.toISOString();
        if (dateTo) criteria.dateTo = dateTo.toISOString();
        return { criteria, errors };
    }

    #validateRegistration(raw) {
        const errors = [];
        const businessId = raw.businessId.trim();
        const customerId = raw.customerId.trim();
        const amountText = raw.amount.trim();
        const transactionDate = parseLocalDateTime(raw.transactionDate);
        let amount = null;

        if (!businessId) {
            addError(errors, 'businessId', 'businessId jest wymagany.');
        }
        if (!customerId || !UUID_PATTERN.test(customerId)) {
            addError(errors, 'customerId', 'Podaj pełny UUID klienta.');
        }
        if (!amountText || !AMOUNT_PATTERN.test(amountText)) {
            addError(errors, 'amount', 'Podaj dodatnią kwotę z maksymalnie 2 miejscami po przecinku.');
        } else {
            amount = Number(amountText.replace(',', '.'));
            if (amount <= 0) {
                addError(errors, 'amount', 'Podaj dodatnią kwotę z maksymalnie 2 miejscami po przecinku.');
            }
        }
        if (!raw.transactionDate) {
            addError(errors, 'transactionDate', 'Data transakcji jest wymagana.');
        } else if (!transactionDate) {
            addError(errors, 'transactionDate', 'Data transakcji ma niepoprawny format.');
        } else if (transactionDate.getTime() > Date.now()) {
            addError(errors, 'transactionDate', 'Data transakcji nie może być z przyszłości.');
        }

        return {
            data: {
                businessId,
                customerId,
                amount,
                currency: raw.currency,
                transactionDate: transactionDate?.toISOString() ?? null,
            },
            errors,
        };
    }

    #clearErrors(form) {
        form.querySelectorAll('.field-error').forEach((el) => (el.textContent = ''));
    }

    #showValidationErrors(form, banner, errors, message) {
        this.#showFieldErrors(form, errors);
        banner.textContent = message;
        banner.className = 'banner banner-warn';
        banner.hidden = false;
    }

    #showApiError(form, banner, error) {
        this.#showFieldErrors(form, error.fieldErrors);
        banner.textContent = error.message;
        banner.className = 'banner banner-error';
        banner.hidden = false;
    }

    #showFieldErrors(form, errors) {
        errors.forEach(({ field, message }) => {
            const slot = form.querySelector(`[data-error-for="${field}"]`);
            if (slot) slot.textContent = message;
        });
    }

    #renderResults(pageData) {
        const container = this.shadowRoot.getElementById('results');
        const pager = this.shadowRoot.getElementById('pager');
        if (pageData.content.length === 0) {
            container.innerHTML = '<div class="empty">Brak transakcji dla podanych kryteriów.</div>';
            pager.innerHTML = '';
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
                    ${pageData.content.map((t) => `
                        <tr>
                            <td>${fmtDateTime(t.transactionDate)}</td>
                            <td class="right"><strong>${fmtMoney(t.amount, t.currency)}</strong></td>
                            <td class="mono">${esc(t.businessId)}</td>
                            <td class="mono muted">${esc(t.customerId)}</td>
                            <td class="mono muted">${esc(t.id)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        renderPager(pager, pageData, (p) => this.#runSearch(p));
    }
}

customElements.define('transactions-view', TransactionsView);
