document.addEventListener('DOMContentLoaded', () => {

    // ==========================================
    // 1. Калькулятор стоимости + Промокоды + Конфиг с сервера
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

    // Конфиг цен по умолчанию (переопределяется с бэкенда)
    let serverPricing = {
        rates: { 1000: 1000, 1500: 1500, 2000: 2000 },
        addons: { express: 1000, studio: 1500 },
        promoCodes: { START10: { type: 'percent', value: 10 }, FOTO500: { type: 'fixed', value: 500 } }
    };

    async function loadPricingConfig() {
        try {
            const res = await fetch(`${API_BASE}/api/pricing/config`);
            if (res.ok) {
                const cfg = await res.json();
                if (cfg) {
                    serverPricing = cfg;
                    calculate();
                }
            }
        } catch (e) {
            console.warn('Серверный конфигуратор цен недоступен, используются резервные значения.');
        }
    }
    loadPricingConfig();

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
            summaryPrice.textContent = formatCurrency(currentCalculatedPrice);
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
        totalPriceEl.textContent = formatCurrency(finalTotal);
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
                notice.textContent = `✓ Выбран расчет: ${detailsText} — ${formatCurrency(currentCalculatedPrice)} ₽`;
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
            navbarNav.classList.toggle('open');
        });

        navbarNav.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                navbarNav.classList.remove('open');
            });
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
    // 6. Отзывы клиентов (Защита от Stored XSS)
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

                    const header = document.createElement('div');
                    header.className = 'review-header';

                    const author = document.createElement('span');
                    author.className = 'review-author';
                    author.textContent = r.authorName || 'Гость';

                    const starsEl = document.createElement('span');
                    starsEl.className = 'review-stars';
                    starsEl.textContent = stars;

                    header.appendChild(author);
                    header.appendChild(starsEl);

                    const text = document.createElement('p');
                    text.className = 'review-text';
                    text.textContent = r.comment || '';

                    const date = document.createElement('div');
                    date.className = 'review-date';
                    date.textContent = r.createdAt || '';

                    card.appendChild(header);
                    card.appendChild(text);
                    card.appendChild(date);

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
    // 7. Отправка заявки на съемку с серверной валидацией цены
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
                details = `Съемка на ${bookingDate} в ${bookingTime}`;
            }

            bookingAlert.textContent = 'Отправка заявки...';
            bookingAlert.classList.remove('d-none');
            bookingAlert.style.color = '#e6cc98';

            const rateVal = shootType ? parseInt(shootType.value, 10) : 1000;
            const hoursVal = hoursRange ? parseInt(hoursRange.value, 10) : 2;
            const addonExp = document.getElementById('addonExpress') ? document.getElementById('addonExpress').checked : false;
            const addonStd = document.getElementById('addonStudio') ? document.getElementById('addonStudio').checked : false;

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
                        shootTypeRate: rateVal,
                        hours: hoursVal,
                        addonExpress: addonExp,
                        addonStudio: addonStd,
                        promoCode: appliedPromoCode || null
                    })
                });

                if (response.ok) {
                    const data = await response.json();
                    bookingAlert.innerHTML = `
                        <div style="color: #75b798; font-weight: bold; margin-bottom: 8px;">✓ Заявка успешно отправлена! Номер: #${escapeHtml(data.orderId)}</div>
                        <div style="background: #202020; border: 1px solid #444; border-radius: 4px; padding: 12px; margin-bottom: 6px; text-align: center;">
                            <div style="font-size: 12px; color: #aaa; margin-bottom: 4px;">Ваш персональный код отслеживания:</div>
                            <div style="font-size: 20px; font-weight: bold; color: #e6cc98; letter-spacing: 2px;">${escapeHtml(data.trackingToken)}</div>
                        </div>
                        <div style="font-size: 12px; color: #888;">Сохраните этот код — по нему можно в 1 клик проверить статус заказа и скачать готовые фото в блоке ниже!</div>
                    `;
                    bookingForm.reset();
                    if (bookingDate) loadAvailableTimeSlots(bookingDate);
                    updateBookingSummary();
                    const notice = document.getElementById('selectedCalcNotice');
                    if (notice) notice.style.display = 'none';

                    // Автоматически подставляем код в форму проверки для удобства
                    const statusTokenInput = document.getElementById('statusToken');
                    if (statusTokenInput && data.trackingToken) {
                        statusTokenInput.value = data.trackingToken;
                    }
                } else if (response.status === 409) {
                    const errorJson = await response.json().catch(() => null);
                    const conflictMsg = (errorJson && errorJson.error) ? errorJson.error : 'Окно уже занято';
                    bookingAlert.textContent = conflictMsg;
                    bookingAlert.style.color = '#ea868f';
                    if (bookingDate) loadAvailableTimeSlots(bookingDate);
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
    // 8. Проверка статуса заказа (По коду отслеживания или ID + телефон)
    // ==========================================
    const statusForm = document.getElementById('statusForm');
    const statusResult = document.getElementById('statusResult');
    const altToggle = document.getElementById('altStatusToggle');
    const altFields = document.getElementById('altStatusFields');

    if (altToggle && altFields) {
        altToggle.addEventListener('click', () => {
            if (altFields.style.display === 'none') {
                altFields.style.display = 'block';
                altToggle.textContent = '▲ Скрыть альтернативные поля';
            } else {
                altFields.style.display = 'none';
                altToggle.textContent = 'Потеряли код? Проверить по номеру заявки и телефону';
            }
        });
    }

    if (statusForm) {
        statusForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const tokenInput = document.getElementById('statusToken');
            const token = tokenInput ? tokenInput.value.trim().toUpperCase() : '';
            const phone = document.getElementById('statusPhone') ? document.getElementById('statusPhone').value.trim() : '';
            const orderIdInp = document.getElementById('statusOrderId');
            const orderId = orderIdInp ? orderIdInp.value.trim() : '';

            let queryUrl = '';
            if (token) {
                queryUrl = `${API_BASE}/api/status?token=${encodeURIComponent(token)}`;
            } else if (orderId && phone) {
                queryUrl = `${API_BASE}/api/status?orderId=${encodeURIComponent(orderId)}&phone=${encodeURIComponent(phone)}`;
            } else {
                alert('Пожалуйста, введите код отслеживания или номер заявки с телефоном!');
                return;
            }

            statusResult.innerHTML = '<span style="color:#aaa;">Поиск в базе данных...</span>';
            statusResult.classList.remove('d-none');

            try {
                const response = await fetch(queryUrl);

                if (response.ok) {
                    const data = await response.json();
                    statusResult.innerHTML = '';

                    const orderNumP = document.createElement('p');
                    orderNumP.style.marginBottom = '6px';
                    orderNumP.textContent = `Заявка #${data.id || ''}`;
                    if (data.trackingToken) {
                        orderNumP.innerHTML += ` <span style="font-size:12px; color:#e6cc98;">(${escapeHtml(data.trackingToken)})</span>`;
                    }
                    statusResult.appendChild(orderNumP);

                    const clientP = document.createElement('p');
                    clientP.style.marginBottom = '6px';
                    clientP.innerHTML = `<strong>Клиент:</strong> <span>${escapeHtml(data.clientName || 'Гость')}</span>`;
                    statusResult.appendChild(clientP);

                    const statusP = document.createElement('p');
                    statusP.style.marginBottom = '6px';
                    statusP.innerHTML = `<strong>Статус заказа:</strong> <span style="color: #e6cc98; font-weight: bold;">${escapeHtml(data.status)}</span>`;
                    statusResult.appendChild(statusP);

                    if (data.bookingDate) {
                        const dateP = document.createElement('p');
                        dateP.style.cssText = 'margin-bottom: 6px; font-size: 14px; color: #888;';
                        dateP.textContent = `📅 Дата съемки: ${data.bookingDate} ${data.bookingTime || ''}`;
                        statusResult.appendChild(dateP);
                    }

                    if (data.downloadUrl && (data.downloadUrl.startsWith('https://') || data.downloadUrl.startsWith('http://'))) {
                        const dlDiv = document.createElement('div');
                        dlDiv.style.marginTop = '15px';
                        const dlLink = document.createElement('a');
                        dlLink.href = data.downloadUrl;
                        dlLink.target = '_blank';
                        dlLink.rel = 'noopener noreferrer';
                        dlLink.className = 'btn-primary';
                        dlLink.style.cssText = 'display: inline-block; padding: 12px 20px; text-decoration: none; font-size: 13px; border-radius: 4px;';
                        dlLink.textContent = '📥 Скачать готовые фотографии';
                        dlDiv.appendChild(dlLink);
                        statusResult.appendChild(dlDiv);
                    }
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

});
