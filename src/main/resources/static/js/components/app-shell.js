// Application shell: top navigation + hash router. Views are plain custom
// elements swapped into the outlet; the alert-details route carries the id as an
// attribute (ADR-0010). Styles live in /styles/app-shell.css and /styles/components.css.

const template = document.createElement('template');
template.innerHTML = `
    <link rel="stylesheet" href="/styles/components.css">
    <link rel="stylesheet" href="/styles/app-shell.css">
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
