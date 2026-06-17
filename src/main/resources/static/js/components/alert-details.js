import { api, ApiError } from '../api.js';
import { esc, fmtDateTime, fmtMoney } from '../format.js';

// Alert details: the triggering transaction, the customer, the decision history
// (newest first) and the decision form. The form carries the alert version it was
// rendered with; a 409 from the backend means another analyst decided in the
// meantime - the view explains it and reloads fresh data (ADR-0008).
// Styles: /styles/components.css + /styles/alert-details.css.

const STYLE_LINKS = `
    <link rel="stylesheet" href="/styles/components.css">
    <link rel="stylesheet" href="/styles/alert-details.css">
`;

class AlertDetails extends HTMLElement {

    #details;

    constructor() {
        super();
        this.attachShadow({ mode: 'open' });
    }

    connectedCallback() {
        this.#load();
    }

    async #load() {
        try {
            this.#details = await api.alerts.details(this.getAttribute('alert-id'));
            this.#render();
        } catch (error) {
            this.shadowRoot.innerHTML = `${STYLE_LINKS}<div class="card"><div class="empty">${esc(error.message)}</div></div>`;
        }
    }

    #render() {
        const alert = this.#details;
        this.shadowRoot.innerHTML = `
            ${STYLE_LINKS}
            <div class="view">
                <div class="view-header">
                    <a class="back" href="#/alerts">← Wróć do listy alertów</a>
                    <h2>
                        Alert
                        <span class="badge badge-${alert.status.toLowerCase()}">${esc(alert.status)}</span>
                    </h2>
                    <p class="lead">
                        ${alert.reason.split(',').map((r) => `<span class="chip">${esc(r)}</span>`).join('')}
                        <span class="muted">utworzony ${fmtDateTime(alert.createdAt)} · wersja ${alert.version}</span>
                    </p>
                </div>

                <p class="banner" id="banner" hidden></p>

                <div class="columns">
                    <section class="card">
                        <h3>Transakcja</h3>
                        <p class="amount">${fmtMoney(alert.transaction.amount, alert.transaction.currency)}</p>
                        <dl>
                            <dt>Data transakcji</dt><dd>${fmtDateTime(alert.transaction.transactionDate)}</dd>
                            <dt>businessId</dt><dd class="mono">${esc(alert.businessId)}</dd>
                            <dt>Id transakcji</dt><dd class="mono">${esc(alert.transactionId)}</dd>
                            <dt>Zarejestrowano</dt><dd>${fmtDateTime(alert.transaction.createdAt)}</dd>
                        </dl>
                    </section>

                    <section class="card">
                        <h3>Klient</h3>
                        <p><strong>${esc(alert.customer.firstName)} ${esc(alert.customer.lastName)}</strong></p>
                        <dl>
                            <dt>businessId</dt><dd class="mono">${esc(alert.customer.businessId)}</dd>
                            <dt>Id klienta</dt><dd class="mono">${esc(alert.customer.id)}</dd>
                            <dt>W systemie od</dt><dd>${fmtDateTime(alert.customer.createdAt)}</dd>
                        </dl>
                    </section>
                </div>

                <section class="card">
                    <h3>Historia decyzji</h3>
                    ${this.#renderHistory(alert.decisions)}
                </section>

                <form class="card" id="decision-form" novalidate>
                    <h3>Podejmij decyzję</h3>
                    <div class="form-grid">
                        <div class="decision-options">
                            <label><input type="radio" name="decision" value="APPROVE" checked> Zatwierdź</label>
                            <label><input type="radio" name="decision" value="REJECT"> Odrzuć</label>
                        </div>
                    </div>
                    <label>Komentarz (wymagany — uzasadnienie trafia do historii audytowej)
                        <textarea name="comment" required maxlength="1000"></textarea>
                        <span class="field-error" data-error-for="comment"></span>
                    </label>
                    <p style="margin-top:12px">
                        <button class="btn btn-primary" type="submit">Zapisz decyzję</button>
                    </p>
                </form>
            </div>
        `;
        this.shadowRoot.getElementById('decision-form').addEventListener('submit', this.#onDecide);
    }

    #renderHistory(decisions) {
        if (decisions.length === 0) {
            return '<div class="empty">Brak decyzji — alert czeka na analizę.</div>';
        }
        return `
            <ol class="timeline">
                ${decisions.map((d) => `
                    <li>
                        <span class="badge badge-${d.decision === 'APPROVE' ? 'approved' : 'rejected'}">
                            ${d.decision === 'APPROVE' ? 'ZATWIERDZONO' : 'ODRZUCONO'}
                        </span>
                        <span class="when">${fmtDateTime(d.createdAt)}</span>
                        <p>${esc(d.comment)}</p>
                    </li>
                `).join('')}
            </ol>
        `;
    }

    #onDecide = async (event) => {
        event.preventDefault();
        const form = event.target;
        const banner = this.shadowRoot.getElementById('banner');
        banner.hidden = true;
        const data = Object.fromEntries(new FormData(form));
        try {
            await api.alerts.decide(this.#details.id, {
                decision: data.decision,
                comment: data.comment,
                alertVersion: this.#details.version,
            });
            await this.#load();
            const refreshedBanner = this.shadowRoot.getElementById('banner');
            refreshedBanner.textContent = 'Decyzja zapisana i dopisana do historii.';
            refreshedBanner.className = 'banner banner-info';
            refreshedBanner.hidden = false;
        } catch (error) {
            if (error instanceof ApiError && error.status === 409) {
                // Optimistic-locking conflict: somebody decided while we were looking.
                await this.#load();
                const refreshedBanner = this.shadowRoot.getElementById('banner');
                refreshedBanner.textContent =
                    'Inny analityk podjął decyzję w międzyczasie. Dane zostały odświeżone — sprawdź historię i zdecyduj ponownie.';
                refreshedBanner.className = 'banner banner-warn';
                refreshedBanner.hidden = false;
            } else if (error instanceof ApiError) {
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
}

customElements.define('alert-details', AlertDetails);
