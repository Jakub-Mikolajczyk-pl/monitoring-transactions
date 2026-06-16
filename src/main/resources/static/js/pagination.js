// Renders a previous/next pager into a host element from a backend page envelope.
// The host stays empty for single-page results, so views can always call it.

export const PAGE_SIZE = 20;

export function renderPager(host, pageData, onGo) {
    if (!host) {
        return;
    }
    const { page, totalPages, totalElements } = pageData;
    if (!totalPages || totalPages <= 1) {
        host.innerHTML = '';
        return;
    }
    host.innerHTML = `
        <button class="btn" data-go="prev" ${page <= 0 ? 'disabled' : ''}>← Poprzednia</button>
        <span class="pager-info">Strona ${page + 1} z ${totalPages} · ${totalElements} rekordów</span>
        <button class="btn" data-go="next" ${page >= totalPages - 1 ? 'disabled' : ''}>Następna →</button>
    `;
    host.querySelector('[data-go="prev"]')?.addEventListener('click', () => onGo(page - 1));
    host.querySelector('[data-go="next"]')?.addEventListener('click', () => onGo(page + 1));
}
