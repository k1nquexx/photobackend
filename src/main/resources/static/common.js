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
