
(function () {
    'use strict';

    const kind = document.body.dataset.pick;
    if (!kind) return;

    const idParam = kind === 'category' ? 'categoryId' : 'planId';
    const path = kind === 'category' ? '/api/categories' : '/api/plans';
    const label = kind === 'category' ? 'Category' : 'Plan';

    const main = document.getElementById('main-display-area');
    if (!main) return;

    const title = main.querySelector('.main-display-bar-title p');

    let items = [];
    let tasks = [];
    let selected = null;

    function fail(error) {
        console.error(error);
        alert('请求失败：' + error.message);
    }

    function setTitle(text) {
        if (title) title.textContent = text;
    }

    function clearRows() {
        main.querySelectorAll('.main-display-bar-task-entry, .main-display-bar-event-entry')
            .forEach(function (row) { row.remove(); });
    }

    async function loadTasks() {
        if (!selected) return;

        try {
            const result = await Promise.all([
                API.get('/api/tasks?' + idParam + '=' + encodeURIComponent(selected.id)),
                API.get('/api/summary')
            ]);

            tasks = result[0] || [];
            clearRows();
            tasks.forEach(function (task) { main.appendChild(Render.taskRow(task)); });

            setTitle(label + ': ' + selected.name + ' (' + tasks.length + ')');

            const finished = tasks.filter(function (task) { return task.completed; }).length;
            Render.footer(result[1], { finished: finished, remaining: tasks.length - finished });
            if (window.ChooseBars) {
                window.ChooseBars.reload();
            }
        } catch (error) {
            fail(error);
        }
    }

    async function select(item) {
        selected = item;
        setTitle(label + ': ' + item.name);
        await loadTasks();
    }


    document.addEventListener('choose', function (e) {
        if (!e.detail || e.detail.kind !== kind) return;
        select(e.detail.item).catch(fail);
    });


    document.addEventListener('choose-renamed', function (e) {
        if (!e.detail || e.detail.kind !== kind) return;

        if (selected && selected.id === e.detail.item.id) {
            selected.name = e.detail.item.name;
            setTitle(label + ': ' + selected.name + ' (' + tasks.length + ')');
        }

        if (selected) {
            loadTasks().catch(fail);
        }
    });


    document.addEventListener('choose-removed', function (e) {
        if (!e.detail || e.detail.kind !== kind) return;

        const removedCurrent = e.detail.item
            ? (selected && selected.id === e.detail.item.id)
            : true;

        if (removedCurrent) {
            selected = null;
            tasks = [];
            clearRows();
            setTitle('Select a ' + label.toLowerCase());
            return;
        }

        loadTasks().catch(fail);
    });


    async function init() {
        items = (await API.get(path)) || [];

        const params = new URLSearchParams(window.location.search);
        const preset = params.get(idParam);
        if (!preset) return;

        const hit = items.find(function (item) { return String(item.id) === String(preset); });
        if (hit) await select(hit);
    }

    RowActions.bind(main, {
        getTasks: function () { return tasks; },
        getEvents: function () { return []; },
        reload: loadTasks
    });

    init().catch(fail);
})();