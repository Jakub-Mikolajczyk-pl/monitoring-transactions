import { describe, it, assert, assertEquals } from './test-runner.js';
import { esc, fmtMoney, shortId } from '../js/format.js';

describe('format.esc', () => {
    it('escapes HTML-significant characters', () => {
        assertEquals(esc('<a>&"\''), '&lt;a&gt;&amp;&quot;&#39;');
    });
    it('renders null as empty string', () => {
        assertEquals(esc(null), '');
    });
});

describe('format.shortId', () => {
    it('truncates to 8 chars with an ellipsis', () => {
        assertEquals(shortId('0190abcd-1234-7000'), '0190abcd…');
    });
    it('renders a dash for a missing id', () => {
        assertEquals(shortId(null), '—');
    });
});

describe('format.fmtMoney', () => {
    it('formats an amount with its currency', () => {
        const formatted = fmtMoney(1500.5, 'PLN').replace(/\s/g, '');
        assert(formatted.includes('1500,50'), `unexpected money format: ${formatted}`);
    });
});
