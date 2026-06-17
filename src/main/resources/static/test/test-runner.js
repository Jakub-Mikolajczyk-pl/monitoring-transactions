// A tiny zero-dependency test harness for the Web Components. It runs in any
// browser - no Node, npm or bundler - which keeps the frontend toolchain-free
// (ADR-0010). Open /test/index.html to run it, or drive it headlessly and read
// window.__TEST_RESULTS__. In a production pipeline these same specs would run
// under Playwright/@web-test-runner in CI; here they stay framework-free.

const tests = [];
let currentSuite = null;

export function describe(name, fn) {
    currentSuite = name;
    fn();
    currentSuite = null;
}

export function it(name, fn) {
    tests.push({ suite: currentSuite, name, fn });
}

export function assert(condition, message) {
    if (!condition) {
        throw new Error(message || 'assertion failed');
    }
}

export function assertEquals(actual, expected, message) {
    if (actual !== expected) {
        throw new Error(`${message || 'not equal'}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
    }
}

// --- helpers for component tests -------------------------------------------

/** Replaces window.fetch with a handler; returns a restore function. */
export function stubFetch(handler) {
    const original = globalThis.fetch;
    globalThis.fetch = async (url, options) => handler(url, options);
    return () => {
        globalThis.fetch = original; };
}

/** Builds a minimal fetch Response stand-in carrying a JSON body. */
export function jsonResponse(status, body) {
    return { ok: status >= 200 && status < 300, status, json: async () => body };
}

export function tick(ms = 0) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

export function mount(tag, attributes = {}) {
    const element = document.createElement(tag);
    for (const [name, value] of Object.entries(attributes)) {
        element.setAttribute(name, value);
    }
    document.body.appendChild(element);
    return element;
}

// --- runner -----------------------------------------------------------------

export async function run(rootElement) {
    const results = [];
    for (const test of tests) {
        try {
            await test.fn();
            results.push({ ...test, ok: true });
        } catch (error) {
            results.push({ ...test, ok: false, error: error.message });
        }
    }
    const passed = results.filter((r) => r.ok).length;
    globalThis.__TEST_RESULTS__ = { passed, total: results.length, results };
    if (rootElement) {
        render(rootElement, results, passed);
    }
    return globalThis.__TEST_RESULTS__;
}

// Test names contain markup such as "<customer-form>"; escaping keeps the report
// as text instead of instantiating those tags as live elements.
function escapeHtml(value) {
    return String(value ?? '').replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
    }[c]));
}

function render(rootElement, results, passed) {
    const allGreen = passed === results.length;
    const rows = results.map((r) => `
        <li class="${r.ok ? 'ok' : 'fail'}">
            ${r.ok ? '✓' : '✗'} <strong>${escapeHtml(r.suite)}</strong> — ${escapeHtml(r.name)}
            ${r.ok ? '' : `<div class="error">${escapeHtml(r.error)}</div>`}
        </li>
    `).join('');
    rootElement.innerHTML = `
        <p class="summary ${allGreen ? 'ok' : 'fail'}" data-summary>
            ${passed}/${results.length} testów przeszło
        </p>
        <ul class="test-list">${rows}</ul>
    `;
}
