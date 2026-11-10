
(function () {
    'use strict';

    const main = document.getElementById('main-display-area');
    if (!main) return;

    let events = [];

    function fail(error) {
        console.error(error);
        alert('请求失败：' + error.message);
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
        const start = DateTimeField.create();
        const end = DateTimeField.create();

        RowActions.openModal('Add Event', [
            RowActions.makeField('Event name', name),
            RowActions.makeField('Description', description),
            RowActions.makeField('Start', start.root),
            RowActions.makeField('End', end.root)
        ], async function () {
            const value = name.value.trim();
            if (!value) { alert('Event name is required'); return true; }

            const rangeError = DateTimeField.validateRange(start, end);
            if (rangeError) {
                alert(rangeError);
                return true;
            }

            await API.post('/api/events', {
                name: value,
                description: description.value.trim(),
                startAt: start.getValue(),
                endAt: end.getValue()
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
