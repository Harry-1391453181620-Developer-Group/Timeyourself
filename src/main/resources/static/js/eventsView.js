/* TimeYourself - Events 页：全部事件，列表 only */
(function () {
    'use strict';

    const main = document.getElementById('main-display-area');
    if (!main) return;

    let events = [];

    function fail(error) {
        console.error(error);
        alert('请求失败：' + error.message);
    }

    function nowLocal(minutesToAdd) {
        const d = new Date(Date.now() + (minutesToAdd || 0) * 60000);
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
            'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function clearRows() {
        main.querySelectorAll('.main-display-bar-event-entry, .main-display-bar-task-entry')
            .forEach(function (row) { row.remove(); });
    }

    async function load() {
        try {
            const result = await Promise.all([
                API.get('/api/events'),
                API.get('/api/summary')
            ]);

            events = result[0] || [];
            clearRows();
            events.forEach(function (event) { main.appendChild(Render.eventRow(event)); });
            Render.footer(result[1]);
        } catch (error) {
            fail(error);
        }
    }

    function addEvent() {
        const name = RowActions.makeInput('text', '');
        const description = RowActions.makeTextarea('');
        const start = RowActions.makeInput('datetime-local', nowLocal(0));
        const end = RowActions.makeInput('datetime-local', nowLocal(60));

        RowActions.openModal('Add Event', [
            RowActions.makeField('Event name', name),
            RowActions.makeField('Description', description),
            RowActions.makeField('Start', start),
            RowActions.makeField('End', end)
        ], async function () {
            const value = name.value.trim();
            if (!value) { alert('Event name is required'); return true; }
            if (start.value && end.value && end.value < start.value) {
                alert('End cannot be before start');
                return true;
            }

            await API.post('/api/events', {
                name: value,
                description: description.value.trim(),
                startAt: start.value || null,
                endAt: end.value || null
            });

            await load();
            return false;
        }, 'Create');
    }

    const addButton = document.querySelector('.add-event-button');
    if (addButton) addButton.addEventListener('click', addEvent);

    RowActions.bind(main, {
        getTasks: function () { return []; },
        getEvents: function () { return events; },
        reload: load
    });

    load();
})();