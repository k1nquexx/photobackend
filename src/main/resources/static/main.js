document.addEventListener('DOMContentLoaded', () => {

    const API_BASE = (window.location.protocol === 'file:' || (window.location.port && window.location.port !== '8080'))
        ? 'http://localhost:8080'
        : '';

    // ==========================================
    // 1. Калькулятор стоимости + Промокоды
    // ==========================================
    const shootType = document.getElementById('shootType');
    const hoursRange = document.getElementById('hoursRange');
    const hoursValue = document.getElementById('hoursValue');
    const addons = document.querySelectorAll('.calc-addon');
    const totalPriceEl = document.getElementById('totalPrice');
    const promoInput = document.getElementById('promoInput');
    const applyPromoBtn = document.getElementById('applyPromoBtn');
    const promoMsg = document.getElementById('promoMsg');
    const bookCalcBtn = document.getElementById('bookCalcBtn');

    const bookingDateInput = document.getElementById('bookingDate');
    const bookingTimeInput = document.getElementById('bookingTime');
    const slotsButtonsContainer = document.getElementById('slotsButtonsContainer');
    const noSlotsMsg = document.getElementById('noSlotsMsg');

    let appliedPromoCode = '';
    let currentCalculatedPrice = 2000;

    function updateBookingSummary() {
        const summaryDate = document.getElementById('summaryDate');
        const summaryTime = document.getElementById('summaryTime');
        const summaryPrice = document.getElementById('summaryPrice');
        const dateVal = bookingDateInput ? bookingDateInput.value : '';
        const timeVal = bookingTimeInput ? bookingTimeInput.value : '';

        if (summaryDate) {
            summaryDate.textContent = dateVal ? dateVal : 'Дата не выбрана';
        }
        if (summaryTime) {
            summaryTime.textContent = timeVal ? `в ${timeVal}` : '(окно не выбрано)';
        }
        if (summaryPrice) {
            summaryPrice.textContent = currentCalculatedPrice.toLocaleString('ru-RU');
        }
    }

    function calculate() {
        if (!shootType || !hoursRange || !totalPriceEl) return;

        const rate = parseInt(shootType.value, 10);
        const hours = parseInt(hoursRange.value, 10);

        if (hoursValue) hoursValue.textContent = hours;

        let baseTotal = rate * hours;

        addons.forEach(addon => {
            if (addon.checked) {
                baseTotal += parseInt(addon.value, 10);
            }
        });

        // Применяем скидку по промокоду
        let finalTotal = baseTotal;
        if (appliedPromoCode === 'START10') {
            finalTotal = Math.round(baseTotal * 0.9); // Скидка 10%
        } else if (appliedPromoCode === 'FOTO500') {
            finalTotal = Math.max(0, baseTotal - 500); // Скидка 500 руб
        }

        currentCalculatedPrice = finalTotal;
        totalPriceEl.textContent = finalTotal.toLocaleString('ru-RU');
        updateBookingSummary();
    }

    if (shootType) shootType.addEventListener('change', calculate);
    if (hoursRange) hoursRange.addEventListener('input', calculate);
    addons.forEach(a => a.addEventListener('change', calculate));

    // ==========================================
    // 2. Система свободных окон записи
    // ==========================================
    async function loadAvailableTimeSlots(date) {
        if (!slotsButtonsContainer) return;
        slotsButtonsContainer.innerHTML = '<span style="color: #aaa; font-size:14px;">Загрузка свободных окон...</span>';
        if (noSlotsMsg) noSlotsMsg.style.display = 'none';
        if (bookingTimeInput) bookingTimeInput.value = '';

        if (!date) {
            slotsButtonsContainer.innerHTML = '<span style="color: #888; font-size:14px;">Выберите дату в календаре выше...</span>';
            updateBookingSummary();
            return;
        }

        try {
            const res = await fetch(`${API_BASE}/api/slots?date=${encodeURIComponent(date)}`);
            if (res.ok) {
                const slots = await res.json();
                slotsButtonsContainer.innerHTML = '';

                if (slots.length === 0) {
                    if (noSlotsMsg) noSlotsMsg.style.display = 'block';
                } else {
                    if (noSlotsMsg) noSlotsMsg.style.display = 'none';
                    slots.forEach((s, idx) => {
                        const btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'slot-select-btn';
                        btn.textContent = s.slotTime;

                        // Автоматически выбираем первое окно для удобства
                        if (idx === 0) {
                            btn.classList.add('active');
                            if (bookingTimeInput) bookingTimeInput.value = s.slotTime;
                        }

                        btn.addEventListener('click', () => {
                            document.querySelectorAll('.slot-select-btn').forEach(b => b.classList.remove('active'));
                            btn.classList.add('active');
                            if (bookingTimeInput) bookingTimeInput.value = s.slotTime;
                            updateBookingSummary();
                        });

                        slotsButtonsContainer.appendChild(btn);
                    });
                }
            } else {
                slotsButtonsContainer.innerHTML = '<span style="color: #ea868f;">Ошибка загрузки окон с сервера.</span>';
            }
        } catch (e) {
            slotsButtonsContainer.innerHTML = '<span style="color: #ea868f;">Нет связи с сервером (запустите Java).</span>';
        }
        updateBookingSummary();
    }

    // Инициализация сегодняшней даты по умолчанию
    if (bookingDateInput) {
        const todayStr = new Date().toISOString().split('T')[0];
        bookingDateInput.value = todayStr;
        bookingDateInput.min = todayStr; // Нельзя выбирать прошедшие даты
        loadAvailableTimeSlots(todayStr);

        bookingDateInput.addEventListener('change', (e) => {
            loadAvailableTimeSlots(e.target.value);
        });
    }

    // Проверка промокода
    if (applyPromoBtn && promoInput) {
        applyPromoBtn.addEventListener('click', () => {
            const code = promoInput.value.trim().toUpperCase();
            if (code === 'START10') {
                appliedPromoCode = 'START10';
                promoMsg.textContent = '✓ Промокод применен: скидка 10%';
                promoMsg.className = 'promo-msg promo-success';
            } else if (code === 'FOTO500') {
                appliedPromoCode = 'FOTO500';
                promoMsg.textContent = '✓ Промокод применен: скидка 500 ₽';
                promoMsg.className = 'promo-msg promo-success';
            } else if (code === '') {
                appliedPromoCode = '';
                promoMsg.textContent = '';
            } else {
                appliedPromoCode = '';
                promoMsg.textContent = '✕ Неверный промокод';
                promoMsg.className = 'promo-msg promo-error';
            }
            calculate();
        });
    }

    // Кнопка «Забронировать этот расчет»
    if (bookCalcBtn) {
        bookCalcBtn.addEventListener('click', () => {
            const typeText = shootType.options[shootType.selectedIndex].text.split('(')[0].trim();
            const hours = hoursRange.value;
            const selectedAddons = [];
            addons.forEach(a => {
                if (a.checked) selectedAddons.push(a.parentElement.textContent.trim());
            });

            let detailsText = `Расчет: ${typeText}, ${hours} ч.`;
            if (selectedAddons.length > 0) {
                detailsText += ` | Опции: ${selectedAddons.join(', ')}`;
            }
            if (appliedPromoCode) {
                detailsText += ` | Промокод: ${appliedPromoCode}`;
            }

            const notice = document.getElementById('selectedCalcNotice');
            if (notice) {
                notice.style.display = 'block';
                notice.innerHTML = `✓ <strong>Выбран расчет:</strong> ${detailsText} — <strong>${currentCalculatedPrice.toLocaleString('ru-RU')} ₽</strong>`;
            }

            const detailsField = document.getElementById('bookingDetails');
            if (detailsField && !detailsField.value.trim()) {
                detailsField.value = detailsText;
            }

            updateBookingSummary();

            const bookingSection = document.getElementById('booking');
            if (bookingSection) {
                bookingSection.scrollIntoView({ behavior: 'smooth' });
            }
        });
    }

    calculate();


    // ==========================================
    // 3. Мобильное меню
    // ==========================================
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const navbarNav = document.getElementById('navbarNav');

    if (mobileMenuBtn && navbarNav) {
        mobileMenuBtn.addEventListener('click', () => {
            if (navbarNav.style.display === 'flex') {
                navbarNav.style.display = 'none';
            } else {
                navbarNav.style.display = 'flex';
                navbarNav.style.flexDirection = 'column';
                navbarNav.style.position = 'absolute';
                navbarNav.style.top = '70px';
                navbarNav.style.left = '0';
                navbarNav.style.width = '100%';
                navbarNav.style.background = '#050505';
                navbarNav.style.padding = '20px';
                navbarNav.style.borderBottom = '1px solid #222222';
            }
        });
    }


    // ==========================================
    // 4. Маска для телефона (+7 ...)
    // ==========================================
    const phoneInputs = document.querySelectorAll('input[type="tel"]');
    phoneInputs.forEach(input => {
        input.addEventListener('input', function () {
            let val = this.value.replace(/\D/g, '');
            if (val.length === 0) {
                this.value = '';
                return;
            }
            if (val[0] === '8' || val[0] === '7') {
                val = val.substring(1);
            }
            let res = '+7 ';
            if (val.length > 0) res += '(' + val.substring(0, 3);
            if (val.length >= 4) res += ') ' + val.substring(3, 6);
            if (val.length >= 7) res += '-' + val.substring(6, 8);
            if (val.length >= 9) res += '-' + val.substring(8, 10);
            this.value = res;
        });
    });


    // ==========================================
    // 5. Аккордеон FAQ
    // ==========================================
    const faqQuestions = document.querySelectorAll('.faq-question');
    faqQuestions.forEach(q => {
        q.addEventListener('click', () => {
            const item = q.parentElement;
            item.classList.toggle('active');
        });
    });


    // ==========================================
    // 6. Отзывы клиентов (Загрузка и Отправка)
    // ==========================================
    const reviewsContainer = document.getElementById('reviewsContainer');
    const reviewForm = document.getElementById('reviewForm');
    const reviewAlert = document.getElementById('reviewAlert');

    async function loadReviews() {
        if (!reviewsContainer) return;
        try {
            const res = await fetch(`${API_BASE}/api/reviews`);
            if (res.ok) {
                const reviews = await res.json();
                if (reviews.length === 0) {
                    reviewsContainer.innerHTML = '<p style="color:#888; text-align:center; grid-column:1/-1;">Отзывов пока нет. Будьте первыми!</p>';
                    return;
                }
                reviewsContainer.innerHTML = '';
                reviews.forEach(r => {
                    const stars = '⭐'.repeat(r.rating || 5);
                    const card = document.createElement('div');
                    card.className = 'review-card';
                    card.innerHTML = `
                        <div class="review-header">
                            <span class="review-author">${escapeHtml(r.authorName)}</span>
                            <span class="review-stars">${stars}</span>
                        </div>
                        <p class="review-text">${escapeHtml(r.comment)}</p>
                        <div class="review-date">${escapeHtml(r.createdAt || '')}</div>
                    `;
                    reviewsContainer.appendChild(card);
                });
            }
        } catch (e) {
            reviewsContainer.innerHTML = '<p style="color:#888; text-align:center; grid-column:1/-1;">Не удалось загрузить отзывы (проверьте, запущен ли сервер).</p>';
        }
    }

    if (reviewForm) {
        reviewForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('reviewAuthor').value;
            const rating = parseInt(document.getElementById('reviewRating').value, 10);
            const comment = document.getElementById('reviewComment').value;

            try {
                const res = await fetch(`${API_BASE}/api/reviews`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ authorName: name, rating: rating, comment: comment })
                });

                if (res.ok) {
                    reviewAlert.textContent = '✓ Спасибо! Отзыв отправлен на проверку и появится после подтверждения администратором.';
                    reviewAlert.style.color = '#75b798';
                    reviewAlert.style.display = 'block';
                    reviewForm.reset();
                } else {
                    reviewAlert.textContent = 'Ошибка при отправке отзыва.';
                    reviewAlert.style.color = '#ea868f';
                    reviewAlert.style.display = 'block';
                }
            } catch (err) {
                reviewAlert.textContent = 'Нет связи с сервером.';
                reviewAlert.style.color = '#ea868f';
                reviewAlert.style.display = 'block';
            }
        });
    }

    loadReviews();


    // ==========================================
    // 7. Отправка заявки на съемку
    // ==========================================
    const bookingForm = document.getElementById('bookingForm');
    const bookingAlert = document.getElementById('bookingAlert');

    if (bookingForm) {
        bookingForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const name = document.getElementById('clientName').value;
            const phone = document.getElementById('clientPhone').value;
            let details = document.getElementById('bookingDetails').value;
            const bookingDate = document.getElementById('bookingDate').value;
            const bookingTime = document.getElementById('bookingTime').value;

            if (!bookingTime || bookingTime.trim() === '') {
                alert('Пожалуйста, выберите доступное окно по времени!');
                return;
            }

            if (!details || !details.trim()) {
                details = `Съемка на ${bookingDate} в ${bookingTime} | Стоимость: ${currentCalculatedPrice} ₽`;
            }

            bookingAlert.textContent = 'Отправка заявки...';
            bookingAlert.classList.remove('d-none');
            bookingAlert.style.color = '#e6cc98';

            try {
                const response = await fetch(`${API_BASE}/api/booking`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        clientName: name,
                        clientPhone: phone,
                        details: details,
                        bookingDate: bookingDate,
                        bookingTime: bookingTime,
                        totalPrice: currentCalculatedPrice,
                        promoCode: appliedPromoCode || null
                    })
                });

                if (response.ok) {
                    bookingAlert.textContent = await response.text();
                    bookingAlert.style.color = '#75b798';
                    bookingForm.reset();
                    // Перезагружаем окна на выбранную дату (забронированное окно исчезнет)
                    if (bookingDate) loadAvailableTimeSlots(bookingDate);
                    updateBookingSummary();
                    const notice = document.getElementById('selectedCalcNotice');
                    if (notice) notice.style.display = 'none';
                } else {
                    bookingAlert.textContent = 'Ошибка сервера при сохранении заявки.';
                    bookingAlert.style.color = '#ea868f';
                }
            } catch (error) {
                bookingAlert.textContent = 'Нет связи с сервером. Запустите Java в IDEA (порт 8080)!';
                bookingAlert.style.color = '#ea868f';
            }
        });
    }


    // ==========================================
    // 8. Проверка статуса заказа
    // ==========================================
    const statusForm = document.getElementById('statusForm');
    const statusResult = document.getElementById('statusResult');

    if (statusForm) {
        statusForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const phone = document.getElementById('statusPhone').value;

            statusResult.innerHTML = 'Поиск в базе данных...';
            statusResult.classList.remove('d-none');

            try {
                const response = await fetch(`${API_BASE}/api/status?phone=` + encodeURIComponent(phone));

                if (response.ok) {
                    const data = await response.json();
                    let html = `<p style="margin-bottom: 6px;"><strong>Клиент:</strong> ${escapeHtml(data.clientName || 'Гость')}</p>`;
                    html += `<p style="margin-bottom: 6px;"><strong>Статус заказа:</strong> <span style="color: #e6cc98; font-weight: bold;">${escapeHtml(data.status)}</span></p>`;
                    if (data.bookingDate) {
                        html += `<p style="margin-bottom: 6px; font-size: 14px; color: #888;">📅 Дата съемки: ${escapeHtml(data.bookingDate)} ${escapeHtml(data.bookingTime || '')}</p>`;
                    }
                    if (data.totalPrice) {
                        html += `<p style="margin-bottom: 6px; font-size: 14px; color: #888;">💰 Сумма: <strong>${data.totalPrice.toLocaleString('ru-RU')} ₽</strong></p>`;
                    }

                    if (data.downloadUrl && data.downloadUrl.trim() !== '') {
                        html += `
                            <div style="margin-top: 15px;">
                                <a href="${escapeHtml(data.downloadUrl)}" target="_blank" class="btn-primary" style="display: inline-block; padding: 12px 20px; text-decoration: none; font-size: 13px; border-radius: 4px;">
                                    📥 Скачать готовые фотографии
                                </a>
                            </div>
                        `;
                    }
                    statusResult.innerHTML = html;
                } else if (response.status === 404) {
                    const errorMsg = await response.text();
                    statusResult.innerHTML = `<span style="color: #ea868f;">${escapeHtml(errorMsg)}</span>`;
                } else {
                    statusResult.innerHTML = '<span style="color: #ea868f;">Ошибка при поиске.</span>';
                }
            } catch (error) {
                statusResult.innerHTML = '<span style="color: #ea868f;">Нет связи с сервером.</span>';
            }
        });
    }

    function escapeHtml(str) {
        return (str || '').toString().replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
    }

});
