// schedule.js - Логика управления расписанием и окнами

// Авторизация по сессионному токену
    if (sessionStorage.getItem('adminToken')) {
        showSchedule();
    }

    async function checkAdminPassword() {
        const pass = document.getElementById('adminPassInput').value;
        const errDiv = document.getElementById('loginError');

        try {
            const res = await fetch(`${API_BASE}/api/admin/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(pass)
            });

            if (res.ok) {
                const data = await res.json();
                sessionStorage.setItem('adminToken', data.token);
                errDiv.style.display = 'none';
                showSchedule();
            } else {
                errDiv.style.display = 'block';
            }
        } catch (e) {
            errDiv.style.display = 'block';
        }
    }

    document.getElementById('adminPassInput').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') checkAdminPassword();
    });

    function showSchedule() {
        document.getElementById('loginBox').classList.add('hidden');
        document.getElementById('scheduleApp').classList.remove('hidden');

        // Устанавливаем сегодняшнюю дату по умолчанию
        const today = new Date().toISOString().split('T')[0];
        const dateInput = document.getElementById('selectedDate');
        dateInput.value = today;
        loadSlotsForDate();
    }

    async function loadSlotsForDate() {
        const date = document.getElementById('selectedDate').value;
        if (!date) return;

        document.getElementById('currentDateTitle').textContent = date;
        const container = document.getElementById('slotsContainer');
        container.innerHTML = '<p class="slots-msg-loading">Загрузка окон...</p>';

        try {
            const res = await fetch(`${API_BASE}/api/slots/all?date=${encodeURIComponent(date)}`, {
                headers: getAuthHeaders()
            });

            if (res.status === 401) {
                alert('Сессия истекла. Войдите снова.');
                sessionStorage.removeItem('adminToken');
                location.reload();
                return;
            }

            if (res.ok) {
                const slots = await res.json();
                container.innerHTML = '';

                if (slots.length === 0) {
                    container.innerHTML = '<p class="slots-msg-empty">На этот день еще нет окон. Нажмите на кнопки выше, чтобы добавить окна для клиентов.</p>';
                    return;
                }

                slots.forEach(s => {
                    const pill = document.createElement('div');
                    pill.className = `slot-pill ${s.booked ? 'booked' : ''}`;

                    const infoDiv = document.createElement('div');
                    const timeDiv = document.createElement('div');
                    timeDiv.className = 'slot-time';
                    timeDiv.textContent = s.slotTime || '';
                    infoDiv.appendChild(timeDiv);

                    const badge = document.createElement('span');
                    badge.className = `slot-badge ${s.booked ? 'badge-busy' : 'badge-free'}`;
                    badge.textContent = s.booked ? 'Занято' : 'Свободно';
                    infoDiv.appendChild(badge);

                    const delBtn = document.createElement('button');
                    delBtn.className = 'btn-del-slot';
                    delBtn.textContent = '✕';
                    delBtn.title = 'Удалить окно';
                    delBtn.onclick = () => deleteSlot(s.id);

                    pill.appendChild(infoDiv);
                    pill.appendChild(delBtn);
                    container.appendChild(pill);
                });
            }
        } catch (e) {
            container.innerHTML = '<p class="slots-msg-error">Ошибка загрузки окон.</p>';
        }
    }

    async function addSingleSlot(timeStr) {
        const date = document.getElementById('selectedDate').value;
        if (!date) {
            alert('Пожалуйста, выберите дату!');
            return;
        }

        try {
            const res = await fetch(`${API_BASE}/api/slots`, {
                method: 'POST',
                headers: getAuthHeaders(),
                body: JSON.stringify({ slotDate: date, slotTime: timeStr })
            });

            if (res.status === 401) {
                alert('Требуется авторизация');
                return;
            }

            loadSlotsForDate();
        } catch (e) {
            alert('Ошибка при добавлении окна');
        }
    }

    async function addCustomSlot() {
        const timeVal = document.getElementById('customTime').value;
        if (!timeVal) {
            alert('Укажите время!');
            return;
        }
        await addSingleSlot(timeVal);
        document.getElementById('customTime').value = '';
    }

    async function fillStandardDay() {
        const times = ['11:00', '14:00', '16:30', '19:00'];
        for (const t of times) {
            await addSingleSlot(t);
        }
    }

    async function deleteSlot(id) {
        if (!confirm('Удалить это окно записи?')) return;
        try {
            const res = await fetch(`${API_BASE}/api/slots/${id}`, {
                method: 'DELETE',
                headers: getAuthHeaders()
            });

            if (res.status === 401) {
                alert('Требуется авторизация');
                return;
            }

            if (res.ok) {
                loadSlotsForDate();
            }
        } catch (e) {
            alert('Ошибка сети при удалении');
        }
    }
