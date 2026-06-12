import { sharedStyles } from '../shared-styles.js';

// Application shell: top navigation + hash router. Views are plain custom
// elements swapped into the outlet; the alert-details route carries the id as an
// attribute (ADR-0010).

const shellStyles = new CSSStyleSheet();
shellStyles.replaceSync(`
    :host { display: block; min-height: 100vh; }
    header {
        background: var(--color-surface);
        border-bottom: 1px solid var(--color-border);
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 24px;
        padding: 12px 28px;
        position: sticky;
        top: 0;
        z-index: 10;
    }
    .brand { display: flex; align-items: center; gap: 12px; }
    .brand-mark {
        width: 38px; height: 38px;
        display: grid; place-items: center;
        background: var(--color-primary);
        color: #fff;
        font-weight: 800;
        border-radius: 8px;
        letter-spacing: 0.02em;
    }
    .brand strong { display: block; line-height: 1.2; }
    .brand .muted { font-size: 0.8rem; }
    nav { display: flex; align-items: center; gap: 4px; }
    nav a {
        text-decoration: none;
        color: var(--color-text);
        font-weight: 600;
        padding: 8px 14px;
        border-radius: 6px;
    }
    nav a:hover { background: var(--color-bg); }
    nav a.active { background: var(--color-primary); color: #fff; }
    nav a.api-link { color: var(--color-text-muted); font-weight: 400; }
    main { max-width: 1100px; margin: 0 auto; padding: 28px 20px 60px; }
`);

const template = document.createElement('template');
template.innerHTML = `
    <header>
        <div class="brand">
            <span class="brand-mark">MT</span>
            <div>
                <strong>Monitoring Transakcji</strong>
                <span class="muted">moduł AML</span>
            </div>
        </div>
        <nav>
            <a href="#/alerts" data-route="alerts">Alerty</a>
            <a href="#/transactions" data-route="transactions">Transakcje</a>
            <a href="#/customers" data-route="customers">Klienci</a>
            <a class="api-link" href="/swagger-ui.html" target="_blank" rel="noopener">API ↗</a>
        </nav>
    </header>
    <main id="outlet"></main>
`;

const routes = {
    customers: () => document.createElement('customers-view'),
    transactions: () => document.createElement('transactions-view'),
    alerts: () => document.createElement('alerts-view'),
};

class AppShell extends HTMLElement {
    #outlet;

    constructor() {
        super();
        const shadow = this.attachShadow({ mode: 'open' });
        shadow.adoptedStyleSheets = [sharedStyles, shellStyles];
        shadow.append(template.content.cloneNode(true));
        this.#outlet = shadow.getElementById('outlet');
    }

    connectedCallback() {
        window.addEventListener('hashchange', this.#onRouteChange);
        this.#onRouteChange();
    }

    disconnectedCallback() {
        window.removeEventListener('hashchange', this.#onRouteChange);
    }

    // '#/alerts/<id>' -> details view; default route: alerts (the analyst's home).
    #onRouteChange = () => {
        const [name = 'alerts', id] = window.location.hash.replace(/^#\//, '').split('/');
        let view;
        if (name === 'alerts' && id) {
            view = document.createElement('alert-details');
            view.setAttribute('alert-id', id);
        } else {
            view = (routes[name] ?? routes.alerts)();
        }
        this.#outlet.replaceChildren(view);
        this.shadowRoot.querySelectorAll('nav a[data-route]').forEach((link) =>
            link.classList.toggle('active', link.dataset.route === name));
    };
}

customElements.define('app-shell', AppShell);
