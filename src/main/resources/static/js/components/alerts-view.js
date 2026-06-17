import { api } from '../api.js';
import { esc, fmtDateTime } from '../format.js';
import { renderPager, PAGE_SIZE } from '../pagination.js';

// Analyst's queue: alerts filtered by status, newest first. Row click navigates
// to the details route - state lives in the URL hash, so views stay stateless.
// Styles: /styles/components.css + /styles/alerts-view.css.

const STATUS_TABS = [
    { value: '', label: 'Wszystkie' },
    { value: 'OPEN', label: 'Otwarte' },
    { value: 'APPROVED', label: 'Zatwierdzone' },
    { value: 'REJECTED', label: 'Odrzucone' },
];

class AlertsView extends HTMLElement {

    #status = 'OPEN';

    constructor() {
        super();
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.innerHTML = `
            <link rel="stylesheet" href="/styles/components.css">
            <link rel="stylesheet" href="/styles/alerts-view.css">
            <div class="view">
                <div class="view-header">
                    <h2>Alerty AML</h2>
                    <p class="lead">Podejrzane transakcje wykryte przez silnik reguł — kolejka pracy analityka.</p>
                </div>
                <div class="tabs">
                    ${STATUS_TABS.map(({ value, label }) => `
                        <button class="btn" data-status="${value}">${label}</button>
                    `).join('')}
                </div>
                <div class="card" id="list"><div class="empty">Ładowanie…</div></div>
                <div class="pager" id="pager"></div>
            </div>
        `;
        shadow.querySelector('.tabs').addEventListener('click', (event) => {
            const button = event.target.closest('[data-status]');
            if (!button) return;
            this.#status = button.dataset.status;
            this.#load(0); // switching filter restarts at the first page
        });
    }

    connectedCallback() {
        this.#load(0);
    }

    async #load(page) {
        this.shadowRoot.querySelectorAll('.tabs .btn').forEach((b) =>
            b.classList.toggle('active', b.dataset.status === this.#status));
        const container = this.shadowRoot.getElementById('list');
        const result = await api.alerts.list({ status: this.#status, page, size: PAGE_SIZE });
        if (result.content.length === 0) {
            container.innerHTML = '<div class="empty">Brak alertów w tym widoku. 🎉</div>';
            this.shadowRoot.getElementById('pager').innerHTML = '';
            return;
        }
        container.innerHTML = `
            <table class="clickable">
                <thead>
                    <tr><th>Status</th><th>Powody</th><th>businessId</th><th>Utworzono</th><th>Transakcja</th><th></th></tr>
                </thead>
                <tbody>
                    ${result.content.map((a) => `
                        <tr data-id="${esc(a.id)}">
                            <td><span class="badge badge-${a.status.toLowerCase()}">${esc(a.status)}</span></td>
                            <td>${a.reason.split(',').map((r) => `<span class="chip">${esc(r)}</span>`).join('')}</td>
                            <td class="mono">${esc(a.businessId)}</td>
                            <td class="muted">${fmtDateTime(a.createdAt)}</td>
                            <td class="mono muted">${esc(a.transactionId)}</td>
                            <td class="right"><a href="#/alerts/${esc(a.id)}">Szczegóły →</a></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        container.querySelectorAll('tbody tr').forEach((row) =>
            row.addEventListener('click', () => (globalThis.location.hash = `#/alerts/${row.dataset.id}`)));
        renderPager(this.shadowRoot.getElementById('pager'), result, (p) => this.#load(p));
    }
}

customElements.define('alerts-view', AlertsView);
