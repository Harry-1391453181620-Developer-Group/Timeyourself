(function () {
    'use strict';

    const scope = document.body.dataset.scope || 'day';
    const main = document.getElementById('main-display-area');
    if (!main) return;

    const grid = main.querySelector('.grid-for-view-in-line');
    let tasks = [];
    let events = [];

    function toInput(value) {
        return value ? String(value).replace('T', ' ').slice(0, 16) : '';
    }

    function fromInput(value) {
        const text = (value || '').trim();
        return text ? text.replace(' ', 'T') : null;
    }

    function taskPayload(task) {
        return {
            name: task.name,
            description: task.description || '',
            startAt: task.startAt,
            dueAt: task.dueAt,
            categoryId: task.categoryId,
            planId: task.planId
        };
    }

    function eventPayload(event) {
        return {
            name: event.name,
            description: event.description || '',
            startAt: event.startAt,
            endAt: event.endAt
        };
    }

    function fail(error) {
        console.error(error);
        alert('请求失败：' + error.message);
    }

    /* ---------- 弹窗基础 ---------- */
    function nowLocal(minutesToAdd) {
        const d = new Date(Date.now() + (minutesToAdd || 0) * 60000);
        const pad = function (n) { return String(n).padStart(2, '0'); };
        return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
            'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    function makeInput(type, value) {
        const node = document.createElement('input');
        node.type = type;
        if (value !== undefined) node.value = value;
        return node;
    }

    function makeTextarea(value) {
        const node = document.createElement('textarea');
        if (value !== undefined) node.value = value;
        return node;
    }

    function toLocalInput(value) {
        return value ? String(value).replace(' ', 'T').slice(0, 16) : '';
    }

    function makeField(labelText, control) {
        const wrap = document.createElement('label');
        wrap.appendChild(document.createTextNode(labelText));
        wrap.appendChild(control);
        return wrap;
    }

    function openModal(title, fields, onSubmit, submitLabel) {
        const backdrop = document.createElement('div');
        backdrop.className = 'ts-modal-backdrop';

        const box = document.createElement('div');
        box.className = 'ts-modal';

        const heading = document.createElement('h2');
        heading.textContent = title;
        box.appendChild(heading);

        fields.forEach(function (node) { box.appendChild(node); });

        const actions = document.createElement('div');
        actions.className = 'ts-modal-actions';

        const cancel = document.createElement('button');
        cancel.type = 'button';
        cancel.textContent = 'Cancel';
        cancel.addEventListener('click', function () { backdrop.remove(); });

        const confirm = document.createElement('button');
        confirm.type = 'button';
        confirm.textContent = submitLabel || 'Create';
        confirm.addEventListener('click', function () {
            Promise.resolve(onSubmit())
                .then(function (keepOpen) {
                    if (keepOpen !== true) backdrop.remove();
                })
                .catch(fail);
        });

        actions.appendChild(cancel);
        actions.appendChild(confirm);
        box.appendChild(actions);
        backdrop.appendChild(box);
        document.body.appendChild(backdrop);

        const first = box.querySelector('input, textarea');
        if (first) first.focus();

        return backdrop;
    }

    /* 名字不存在就新建，返回 id；名字为空返回 null */
    async function findOrCreate(path, rawName, extra) {
        const name = (rawName || '').trim();
        if (!name) return null;

        const list = await API.get(path);
        const hit = (list || []).find(function (item) {
            return String(item.name).toLowerCase() === name.toLowerCase();
        });
        if (hit) return hit.id;

        const created = await API.post(path, Object.assign({ name: name }, extra || {}));
        return created.id;
    }

    /* ---------- Add Task ---------- */
    function openTaskDialog() {
        const name = makeInput('text', '');
        const description = makeTextarea('');
        const start = makeInput('datetime-local', nowLocal(0));
        const end = makeInput('datetime-local', nowLocal(60));
        const category = makeInput('text', '');
        const plan = makeInput('text', '');

        openModal('Add Task', [
            makeField('Task name', name),
            makeField('Description', description),
            makeField('Start', start),
            makeField('End', end),
            makeField('Category (optional — a new one will be created if it does not exist)', category),
            makeField('Plan (optional — a new one will be created if it does not exist)', plan)
        ], async function () {
            const taskName = name.value.trim();
            if (!taskName) {
                alert('Task name is required');
                return true;
            }
            if (start.value && end.value && end.value < start.value) {
                alert('End cannot be before start');
                return true;
            }

            const categoryId = await findOrCreate('/api/categories', category.value);
            const planId = await findOrCreate('/api/plans', plan.value, { description: '' });

            await API.post('/api/tasks', {
                name: taskName,
                description: description.value.trim(),
                startAt: start.value || null,
                dueAt: end.value || null,
                categoryId: categoryId,
                planId: planId
            });

            await load();
            return false;
        });
    }

    /* ---------- Add Event ---------- */
    function openEventDialog() {
        const name = makeInput('text', '');
        const description = makeTextarea('');
        const start = makeInput('datetime-local', nowLocal(0));
        const end = makeInput('datetime-local', nowLocal(60));

        openModal('Add Event', [
            makeField('Event name', name),
            makeField('Description', description),
            makeField('Start', start),
            makeField('End', end)
        ], async function () {
            const eventName = name.value.trim();
            if (!eventName) {
                alert('Event name is required');
                return true;
            }
            if (start.value && end.value && end.value < start.value) {
                alert('End cannot be before start');
                return true;
            }

            await API.post('/api/events', {
                name: eventName,
                description: description.value.trim(),
                startAt: start.value || null,
                endAt: end.value || null
            });

            await load();
            return false;
        });
    }

    async function applyTask(task, changes) {
        await API.put('/api/tasks/' + task.id, Object.assign(taskPayload(task), changes));
        await load();
    }

    function renameTask(task) {
        const name = makeInput('text', task.name);
        openModal('Rename Task', [makeField('Task name', name)], async function () {
            const value = name.value.trim();
            if (!value) { alert('Task name is required'); return true; }
            await applyTask(task, { name: value });
        }, 'Save');
    }

    function reclarifyTask(task) {
        const description = makeTextarea(task.description || '');
        openModal('Reclarify Task', [makeField('Description', description)], async function () {
            await applyTask(task, { description: description.value.trim() });
        }, 'Save');
    }

    function redateTask(task) {
        const start = makeInput('datetime-local', toLocalInput(task.startAt));
        const end = makeInput('datetime-local', toLocalInput(task.dueAt));
        openModal('Redate Task', [
            makeField('Start', start),
            makeField('End', end)
        ], async function () {
            if (start.value && end.value && end.value < start.value) {
                alert('End cannot be before start');
                return true;
            }
            await applyTask(task, {
                startAt: start.value || null,
                dueAt: end.value || null
            });
        }, 'Save');
    }

    function deleteTask(task) {
        const note = document.createElement('p');
        note.className = 'ts-modal-note';
        note.textContent = 'Delete task "' + task.name + '"?';
        openModal('Delete Task', [note], async function () {
            await API.del('/api/tasks/' + task.id);
            await load();
        }, 'Delete');
    }

    async function applyEvent(event, changes) {
        await API.put('/api/events/' + event.id, Object.assign(eventPayload(event), changes));
        await load();
    }

    function renameEvent(event) {
        const name = makeInput('text', event.name);
        openModal('Rename Event', [makeField('Event name', name)], async function () {
            const value = name.value.trim();
            if (!value) { alert('Event name is required'); return true; }
            await applyEvent(event, { name: value });
        }, 'Save');
    }

    function reclarifyEvent(event) {
        const description = makeTextarea(event.description || '');
        openModal('Reclarify Event', [makeField('Description', description)], async function () {
            await applyEvent(event, { description: description.value.trim() });
        }, 'Save');
    }

    function redateEvent(event) {
        const start = makeInput('datetime-local', toLocalInput(event.startAt));
        const end = makeInput('datetime-local', toLocalInput(event.endAt));
        openModal('Redate Event', [
            makeField('Start', start),
            makeField('End', end)
        ], async function () {
            if (start.value && end.value && end.value < start.value) {
                alert('End cannot be before start');
                return true;
            }
            await applyEvent(event, {
                startAt: start.value || null,
                endAt: end.value || null
            });
        }, 'Save');
    }

    function deleteEvent(event) {
        const note = document.createElement('p');
        note.className = 'ts-modal-note';
        note.textContent = 'Delete event "' + event.name + '"?';
        openModal('Delete Event', [note], async function () {
            await API.del('/api/events/' + event.id);
            await load();
        }, 'Delete');
    }

    function deleteAllDone() {
        const note = document.createElement('p');
        note.className = 'ts-modal-note';
        note.textContent = 'Delete all completed tasks and all past events? This cannot be undone.';

        openModal('Delete All Done', [note], async function () {
            await API.del('/api/tasks/completed');
            await API.del('/api/events/past');
            await load();
        }, 'Delete');
    }

    /* ---------- 渲染 ---------- */
    function removeRows() {
        main.querySelectorAll('.main-display-bar-task-entry, .main-display-bar-event-entry')
            .forEach(function (row) { row.remove(); });
    }

    function isLineView() {
        return grid !== null && grid.style.display === 'grid';
    }

    function appendRow(row) {
        if (grid) {
            main.insertBefore(row, grid);
        } else {
            main.appendChild(row);
        }
        if (isLineView()) row.style.display = 'none';
    }

    function render() {
        removeRows();
        tasks.forEach(function (task) { appendRow(Render.taskRow(task)); });
        events.forEach(function (event) { appendRow(Render.eventRow(event)); });
    }

    /* ---------- 加载 ---------- */
    async function load() {
        try {
            const result = await Promise.all([
                API.get('/api/tasks?scope=' + encodeURIComponent(scope)),
                API.get('/api/events?scope=' + encodeURIComponent(scope)),
                API.get('/api/summary')
            ]);
            tasks = result[0] || [];
            events = result[1] || [];
            render();

            const finished = tasks.filter(function (task) { return task.completed; }).length;
            Render.footer(result[2], {
                finished: finished,
                remaining: tasks.length - finished
            });

            if (window.LineView) {
                window.LineView.setTasks(tasks);
            }
            if (window.ChooseBars) {
                window.ChooseBars.reload();
            }
        } catch (error) {
            fail(error);
        }
    }

    /* ---------- 交互 ---------- */
    function findTask(id) {
        return tasks.find(function (task) { return task.id === id; });
    }

    function findEvent(id) {
        return events.find(function (event) { return event.id === id; });
    }

    async function saveTask(id, payload) {
        try {
            await API.put('/api/tasks/' + id, payload);
            await load();
        } catch (error) {
            fail(error);
        }
    }

    async function saveEvent(id, payload) {
        try {
            await API.put('/api/events/' + id, payload);
            await load();
        } catch (error) {
            fail(error);
        }
    }

    function handleTask(row, e) {
        const task = findTask(Number(row.dataset.id));
        if (!task) return;

        if (e.target.closest('.checkbox')) {
            API.patch('/api/tasks/' + task.id + '/completion', { completed: !task.completed })
                .then(load)
                .catch(fail);
            return;
        }

        const action = e.target.closest('[data-action]');
        if (!action) return;

        const kind = action.dataset.action;

        if (kind === 'rename') renameTask(task);
        else if (kind === 'reclarify') reclarifyTask(task);
        else if (kind === 'redate') redateTask(task);
        else if (kind === 'delete') deleteTask(task);
    }

    function handleEvent(row, e) {
        const event = findEvent(Number(row.dataset.id));
        if (!event) return;

        if (e.target.closest('.checkbox')) return;

        const action = e.target.closest('[data-action]');
        if (!action) return;

        const kind = action.dataset.action;

        if (kind === 'rename') renameEvent(event);
        else if (kind === 'reclarify') reclarifyEvent(event);
        else if (kind === 'redate') redateEvent(event);
        else if (kind === 'delete') deleteEvent(event);
    }

    main.addEventListener('click', function (e) {
        const taskRow = e.target.closest('.main-display-bar-task-entry');
        if (taskRow) {
            handleTask(taskRow, e);
            return;
        }
        const eventRow = e.target.closest('.main-display-bar-event-entry');
        if (eventRow) {
            handleEvent(eventRow, e);
        }
    });

    const addTaskButton = document.querySelector('.add-task-button');
    if (addTaskButton) addTaskButton.addEventListener('click', openTaskDialog);

    const addEventButton = document.querySelector('.add-event-button');
    if (addEventButton) addEventButton.addEventListener('click', openEventDialog);

    const deleteAllDoneButton = document.querySelector('.delete-all-done-button');
    if (deleteAllDoneButton) deleteAllDoneButton.addEventListener('click', deleteAllDone);

    /* choose bar 改名/删除后，My Day 等页面的行里 Category/Plan 文本要重取 */
    document.addEventListener('choose-renamed', function () {
        load();
    });

    document.addEventListener('choose-removed', function () {
        load();
    });

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', load);
    } else {
        load();
    }
})();