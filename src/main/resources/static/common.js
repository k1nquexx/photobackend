// common.js - Общие утилиты и безопасность для фото-сайта

const API_BASE = (window.location.protocol === 'file:' || (window.location.port && window.location.port !== '8080'))
    ? 'http://localhost:8080'
    : '';

function getAuthHeaders() {
    return {
        'Content-Type': 'application/json',
        'X-Admin-Token': sessionStorage.getItem('adminToken') || ''
    };
}

// Защита от XSS (экранирование спецсимволов)
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

// Форматирование чисел для цен (2000 -> "2 000")
function formatCurrency(num) {
    return (num || 0).toLocaleString('ru-RU');
}

// ==========================================
// Плавный переход между страницами
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    function getCleanPath(url) {
        if (!url) return '';
        const path = url.split('#')[0].split('?')[0].split('/').pop();
        return (!path || path === '') ? 'index.html' : path;
    }

    const currentPath = getCleanPath(window.location.pathname);

    document.querySelectorAll('a[href]').forEach(link => {
        link.addEventListener('click', (e) => {
            const href = link.getAttribute('href');
            if (!href) return;

            // Пропускаем внешние ссылки, якоря, спец-протоколы и новые вкладки
            if (
                link.target === '_blank' ||
                href.startsWith('#') ||
                href.startsWith('tel:') ||
                href.startsWith('mailto:') ||
                href.startsWith('javascript:') ||
                link.hasAttribute('download') ||
                e.ctrlKey || e.metaKey || e.shiftKey || e.altKey
            ) {
                return;
            }

            const targetPath = getCleanPath(href);

            // Если кликнули на якорь текущей страницы (например: "index.html#booking" находясь на index.html)
            if (href.includes('#') && targetPath === currentPath) {
                return;
            }

            // Если включен режим уменьшения движения у пользователя
            if (window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
                return;
            }

            e.preventDefault();
            document.body.classList.add('page-leaving');

            setTimeout(() => {
                window.location.href = href;
            }, 200);
        });
    });
});

// Сброс класса при возврате по кнопке "Назад" (bfcache)
window.addEventListener('pageshow', () => {
    document.body.classList.remove('page-leaving');
});
