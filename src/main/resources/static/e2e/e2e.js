// Light end-to-end walkthrough. Unlike the component unit tests (which stub fetch),
// this drives the REAL running backend - HTTP -> service -> rules -> async analysis
// -> DB -> API - and shows every case ticking green live. Open /e2e/ to watch it.
//
// Each run uses a unique businessId (E2E-<timestamp>) so repeated runs and the demo
// seed never collide. State is threaded through `ctx` between steps.

const steps = [];
const ctx = {};
let RUN = '';

function step(title, fn) {
    steps.push({ title, fn });
}

function assert(condition, message) {
    if (!condition) {
        throw new Error(message || 'assertion failed');
    }
}

async function http(method, path, body) {
    const response = await fetch(path, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    let parsed = null;
    try {
        parsed = await response.json();
    } catch (ignored) {
        // some responses (e.g. 404 with empty body) carry no JSON
    }
    return { status: response.status, body: parsed };
}

/// Polls the alert list until one references the given transaction, or times out.
async function pollAlert(transactionId, timeoutMs = 6000) {
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
        const { body } = await http('GET', '/api/alerts?size=100');
        const found = (body.content || []).find((a) => a.transactionId === transactionId);
        if (found) {
            return found;
        }
        await new Promise((resolve) => setTimeout(resolve, 200));
    }
    return null;
}

function registerCustomer(firstName, lastName) {
    return http('POST', '/api/customers', { businessId: ctx.biz, firstName, lastName })
        .then((r) => {
            assert(r.status === 201, `rejestracja klienta: oczekiwano 201, było ${r.status}`);
            return r.body.id;
        });
}

function registerTransaction(customerId, amount, dateIso, currency = 'PLN', businessId = ctx.biz) {
    return http('POST', '/api/transactions', {
        businessId, customerId, amount, currency, transactionDate: dateIso,
    });
}

// --- the scenario ----------------------------------------------------------

step('Klient — rejestracja (201 + Location)', async () => {
    const r = await http('POST', '/api/customers', { businessId: ctx.biz, firstName: 'Jan', lastName: 'E2E' });
    assert(r.status === 201, `oczekiwano 201, było ${r.status}`);
    assert(r.body.id, 'brak id w odpowiedzi');
    assert(r.body.businessId === ctx.biz, 'businessId nie zgadza się');
    ctx.customer = r.body.id;
    return `id=${r.body.id.slice(0, 8)}…, businessId=${ctx.biz}`;
});

step('Walidacja klienta — puste pole → 400 z błędem pola', async () => {
    const r = await http('POST', '/api/customers', { businessId: ctx.biz, firstName: '   ', lastName: 'X' });
    assert(r.status === 400, `oczekiwano 400, było ${r.status}`);
    assert(r.body.errors?.[0]?.field === 'firstName', 'oczekiwano błędu pola firstName');
    return `400, errors[0].field=firstName`;
});

step('Transakcja zwykła (201)', async () => {
    const r = await registerTransaction(ctx.customer, 100.00, '2026-05-20T08:00:00Z');
    assert(r.status === 201, `oczekiwano 201, było ${r.status}`);
    ctx.cleanTx = r.body.id;
    return `id=${r.body.id.slice(0, 8)}…, kwota=100,00 PLN`;
});

step('Transakcja podejrzana kwotowo > 2000 (201)', async () => {
    const r = await registerTransaction(ctx.customer, 2500.50, '2026-05-20T09:00:00Z');
    assert(r.status === 201, `oczekiwano 201, było ${r.status}`);
    ctx.suspTx = r.body.id;
    return `id=${r.body.id.slice(0, 8)}…, kwota=2500,50 PLN`;
});

step('Alert SUSPICIOUS_AMOUNT powstaje asynchronicznie', async () => {
    const alert = await pollAlert(ctx.suspTx);
    assert(alert, 'alert nie pojawił się w czasie 6 s');
    assert(alert.reason === 'SUSPICIOUS_AMOUNT', `powód=${alert.reason}`);
    assert(alert.status === 'OPEN', `status=${alert.status}`);
    ctx.alert = alert.id;
    return `alert ${alert.id.slice(0, 8)}…, reason=SUSPICIOUS_AMOUNT, status=OPEN`;
});

step('Transakcja zwykła NIE generuje alertu', async () => {
    // The suspicious alert above is the synchronisation point: both transactions are
    // committed and analysed, so the clean one having no alert is now deterministic.
    const alert = await pollAlert(ctx.cleanTx, 1200);
    assert(alert === null, 'nieoczekiwany alert dla zwykłej transakcji');
    return 'brak alertu (zgodnie z oczekiwaniem)';
});

step('Reguła HIGH_FREQUENCY — 6 transakcji w 1 h', async () => {
    const customer = await registerCustomer('Anna', 'Czesta');
    let sixth = null;
    for (let minute = 0; minute < 6; minute++) {
        const r = await registerTransaction(customer, 30.00, `2026-05-21T10:0${minute}:00Z`);
        assert(r.status === 201, `transakcja ${minute + 1}: ${r.status}`);
        sixth = r.body.id;
    }
    const alert = await pollAlert(sixth);
    assert(alert, 'alert HIGH_FREQUENCY nie powstał');
    assert(alert.reason === 'HIGH_FREQUENCY', `powód=${alert.reason}`);
    return `6. transakcja → alert HIGH_FREQUENCY`;
});

step('Scalone powody — kwota + częstotliwość w jednym alercie', async () => {
    const customer = await registerCustomer('Zofia', 'Mieszana');
    let sixth = null;
    for (let minute = 0; minute < 5; minute++) {
        await registerTransaction(customer, 40.00, `2026-05-22T11:0${minute}:00Z`);
    }
    const big = await registerTransaction(customer, 9999.00, '2026-05-22T11:05:00Z');
    sixth = big.body.id;
    const alert = await pollAlert(sixth);
    assert(alert, 'alert nie powstał');
    const reasons = alert.reason.split(',');
    assert(reasons.includes('SUSPICIOUS_AMOUNT') && reasons.includes('HIGH_FREQUENCY'),
        `oczekiwano obu powodów, było: ${alert.reason}`);
    return `reason=${alert.reason}`;
});

step('Walidacja transakcji — zła waluta → 400', async () => {
    const r = await registerTransaction(ctx.customer, 10.00, '2026-05-20T08:30:00Z', 'ZZZ');
    assert(r.status === 400, `oczekiwano 400, było ${r.status}`);
    assert(r.body.errors?.some((e) => e.field === 'currency'), 'oczekiwano błędu pola currency');
    return '400, errors[].field=currency';
});

step('Spójność biznesowa — businessId ≠ klient → 422', async () => {
    const r = await registerTransaction(ctx.customer, 10.00, '2026-05-20T08:31:00Z', 'PLN', ctx.biz + '-INNY');
    assert(r.status === 422, `oczekiwano 422, było ${r.status}`);
    assert(r.body.title === 'Business rule violated', `title=${r.body.title}`);
    return '422, title="Business rule violated"';
});

step('Wyszukiwanie — businessId wymagany → 400', async () => {
    const r = await http('GET', '/api/transactions');
    assert(r.status === 400, `oczekiwano 400, było ${r.status}`);
    return '400 (brak wymaganego businessId)';
});

step('Paginacja + twardy limit rozmiaru strony', async () => {
    const firstPage = await http('GET', `/api/transactions?businessId=${ctx.biz}&page=0&size=1`);
    assert(firstPage.status === 200, `oczekiwano 200, było ${firstPage.status}`);
    assert(firstPage.body.size === 1, `size=${firstPage.body.size}`);
    assert(firstPage.body.content.length === 1, `content.length=${firstPage.body.content.length}`);
    assert(firstPage.body.totalElements >= 2, `totalElements=${firstPage.body.totalElements}`);
    const capped = await http('GET', `/api/transactions?businessId=${ctx.biz}&size=1000000`);
    assert(capped.body.size === 100, `size po obcięciu=${capped.body.size}`);
    return `page0 size=1 z ${firstPage.body.totalElements} rekordów; size=1000000 → 100`;
});

step('Szczegóły alertu — kompozycja transakcja + klient + historia', async () => {
    const r = await http('GET', `/api/alerts/${ctx.alert}`);
    assert(r.status === 200, `oczekiwano 200, było ${r.status}`);
    assert(r.body.transaction?.amount === 2500.50, `transaction.amount=${r.body.transaction?.amount}`);
    assert(r.body.customer?.firstName === 'Jan', `customer.firstName=${r.body.customer?.firstName}`);
    assert(Array.isArray(r.body.decisions), 'brak tablicy decisions');
    ctx.alertVersion = r.body.version;
    return `transakcja 2500,50 + klient Jan + ${r.body.decisions.length} decyzji, version=${r.body.version}`;
});

step('Decyzja APPROVE (201) → status APPROVED', async () => {
    const r = await http('POST', `/api/alerts/${ctx.alert}/decisions`, {
        decision: 'APPROVE', comment: 'E2E: zweryfikowano z klientem', alertVersion: ctx.alertVersion,
    });
    assert(r.status === 201, `oczekiwano 201, było ${r.status}`);
    const details = await http('GET', `/api/alerts/${ctx.alert}`);
    assert(details.body.status === 'APPROVED', `status=${details.body.status}`);
    assert(details.body.version === ctx.alertVersion + 1, `version=${details.body.version}`);
    assert(details.body.decisions.length === 1, `decisions=${details.body.decisions.length}`);
    return `status=APPROVED, version=${details.body.version}, 1 wpis w historii`;
});

step('Konflikt wersji — nieaktualna decyzja → 409', async () => {
    const r = await http('POST', `/api/alerts/${ctx.alert}/decisions`, {
        decision: 'REJECT', comment: 'E2E: praca na nieaktualnym widoku', alertVersion: ctx.alertVersion,
    });
    assert(r.status === 409, `oczekiwano 409, było ${r.status}`);
    assert(r.body.title === 'Concurrent modification', `title=${r.body.title}`);
    return '409, title="Concurrent modification"';
});

step('Nieznany alert — decyzja → 404', async () => {
    const r = await http('POST', '/api/alerts/00000000-0000-7000-8000-000000000000/decisions', {
        decision: 'APPROVE', comment: 'E2E: duch', alertVersion: 0,
    });
    assert(r.status === 404, `oczekiwano 404, było ${r.status}`);
    return '404 (zasób nie istnieje)';
});

// --- runner -----------------------------------------------------------------

const listEl = document.getElementById('steps');
const summaryEl = document.getElementById('summary');

function addRow(title) {
    const li = document.createElement('li');
    li.className = 'running';
    li.innerHTML = `<span class="mark">▶</span> <span class="title"></span> <span class="detail"></span>`;
    li.querySelector('.title').textContent = title;
    listEl.appendChild(li);
    return li;
}

function finishRow(li, ok, text) {
    li.className = ok ? 'ok' : 'fail';
    li.querySelector('.mark').textContent = ok ? '✓' : '✗';
    li.querySelector('.detail').textContent = text ? `— ${text}` : '';
}

export async function run() {
    listEl.innerHTML = '';
    summaryEl.className = 'summary running';
    summaryEl.textContent = 'Trwa przebieg E2E…';
    RUN = String(Date.now());
    ctx.biz = `E2E-${RUN}`;

    let passed = 0;
    for (const s of steps) {
        const li = addRow(s.title);
        try {
            const detail = await s.fn();
            finishRow(li, true, detail);
            passed += 1;
        } catch (error) {
            finishRow(li, false, error.message);
        }
    }

    const allGreen = passed === steps.length;
    summaryEl.className = `summary ${allGreen ? 'ok' : 'fail'}`;
    summaryEl.textContent = `${passed}/${steps.length} przypadków przeszło — businessId przebiegu: ${ctx.biz}`;
    window.__E2E_RESULTS__ = { passed, total: steps.length, businessId: ctx.biz };
    return window.__E2E_RESULTS__;
}

document.getElementById('rerun').addEventListener('click', run);
run();
