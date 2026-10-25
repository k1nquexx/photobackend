// schedule.js - Логика управления расписанием и окнами (Защищенная административная зона)

document.addEventListener('DOMContentLoaded', () => {
    initSchedule();
});

function initSchedule() {
    const today = new Date().toISOString().split('T')[0];
    const dateInput = document.getElementById('selectedDate');
    if (dateInput) {
        dateInput.value = today;
    }
    loadSlotsForDate();
}

async function loadSlotsForDate() {
    const dateInput = document.getElementById('selectedDate');
    const date = dateInput ? dateInput.value : '';
    if (!date) return;

    const titleEl = document.getElementById('currentDateTitle');
    if (titleEl) titleEl.textContent = date;

    const container = document.getElementById('slotsContainer');
    container.replaceChildren();
    const loadP = document.createElement('p');
    loadP.className = 'slots-msg-loading';
    loadP.textContent = 'Загрузка окон...';
    container.appendChild(loadP);

    try {
        const res = await fetch(`${API_BASE}/api/slots/all?date=${encodeURIComponent(date)}`, {
            headers: getAuthHeaders(),
            credentials: 'same-origin'
        });

        if (res.status === 401) {
            alert('Сессия истекла. Войдите снова.');
            location.reload();
            return;
        }

        if (res.ok) {
            const slots = await res.json();
            container.replaceChildren();

            if (slots.length === 0) {
                const emptyP = document.createElement('p');
                emptyP.className = 'slots-msg-empty';
                emptyP.textContent = 'На этот день еще нет окон. Нажмите на кнопки выше, чтобы добавить окна для клиентов.';
                container.appendChild(emptyP);
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
        container.replaceChildren();
        const errP = document.createElement('p');
        errP.className = 'slots-msg-error';
        errP.textContent = 'Ошибка загрузки окон.';
        container.appendChild(errP);
    }
}

async function addSingleSlot(timeStr) {
    const dateInput = document.getElementById('selectedDate');
    const date = dateInput ? dateInput.value : '';
    if (!date) {
        alert('Пожалуйста, выберите дату!');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/slots`, {
            method: 'POST',
            headers: getAuthHeaders(),
            credentials: 'same-origin',
            body: JSON.stringify({ slotDate: date, slotTime: timeStr })
        });

        if (res.status === 401) {
            alert('Требуется авторизация');
            location.reload();
            return;
        }

        if (!res.ok) {
            const err = await res.text();
            alert(err || 'Ошибка при добавлении окна');
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
            headers: getAuthHeaders(),
            credentials: 'same-origin'
        });

        if (res.status === 401) {
            alert('Требуется авторизация');
            location.reload();
            return;
        }

        if (res.ok) {
            loadSlotsForDate();
        }
    } catch (e) {
        alert('Ошибка сети при удалении');
    }
}
