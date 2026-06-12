// Small formatting/escaping helpers shared by all views.

export const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (c) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;',
}[c]));

export const fmtDateTime = (iso) => iso
    ? new Date(iso).toLocaleString('pl-PL', { dateStyle: 'medium', timeStyle: 'short' })
    : '—';

export const fmtMoney = (amount, currency) =>
    new Intl.NumberFormat('pl-PL', { style: 'currency', currency }).format(amount);

export const shortId = (id) => (id ? `${String(id).slice(0, 8)}…` : '—');
