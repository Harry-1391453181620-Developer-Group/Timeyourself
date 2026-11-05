/* TimeYourself - Categories / Plans 页：choose bar 选中后加载任务（只有 list） */
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

    /* choose bar 里点了一条 */
    document.addEventListener('choose', function (e) {
        if (!e.detail || e.detail.kind !== kind) return;
        select(e.detail.item).catch(fail);
    });

    /* 分类/计划改名：标题 + 每一行的 "Category: xxx" 都要更新 */
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

    /* 分类/计划被删：删的是当前选中项就复位，否则刷新行里的分类/计划名 */
    document.addEventListener('choose-removed', function (e) {
        if (!e.detail || e.detail.kind !== kind) return;

        const removedCurrent = e.detail.item
            ? (selected && selected.id === e.detail.item.id)
            : true;   /* item 为 null 表示 Del A 清空全部 */

        if (removedCurrent) {
            selected = null;
            tasks = [];
            clearRows();
            setTitle('Select a ' + label.toLowerCase());
            return;
        }

        loadTasks().catch(fail);
    });

    /* URL 里带 ?categoryId=3 / ?planId=7 时自动选中 */
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