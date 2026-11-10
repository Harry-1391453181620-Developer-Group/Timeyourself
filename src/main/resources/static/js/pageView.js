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

    function makeField(labelText, control) {
        const wrap = document.createElement('label');
        wrap.appendChild(document.createTextNode(labelText));
        wrap.appendChild(control);
        return wrap;
    }

    let activeModalBackdrop = null;

    function showModalError(message) {
        if (!activeModalBackdrop) {
            alert(message);
            return;
        }
        let box = activeModalBackdrop.querySelector('.ts-modal-error');
        if (!box) {
            box = document.createElement('p');
            box.className = 'ts-modal-error';
            const actions = activeModalBackdrop.querySelector('.ts-modal-actions');
            if (actions && actions.parentNode) {
                actions.parentNode.insertBefore(box, actions);
            } else {
                activeModalBackdrop.querySelector('.ts-modal').appendChild(box);
            }
        }
        box.textContent = message;
        box.scrollIntoView({ block: 'nearest' });
    }

    function clearModalError() {
        if (!activeModalBackdrop) return;
        const box = activeModalBackdrop.querySelector('.ts-modal-error');
        if (box) box.textContent = '';
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
        cancel.addEventListener('click', function () { activeModalBackdrop = null; backdrop.remove(); });

        const confirm = document.createElement('button');
        confirm.type = 'button';
        confirm.textContent = submitLabel || 'Create';
        confirm.addEventListener('click', function () {
            clearModalError();
            Promise.resolve(onSubmit())
                .then(function (keepOpen) {
                    if (keepOpen !== true) {
                        if (activeModalBackdrop === backdrop) activeModalBackdrop = null;
                        backdrop.remove();
                    }
                })
                .catch(function (error) {
                    if (activeModalBackdrop === backdrop) {
                        showModalError('请求失败：' + error.message);
                    } else {
                        fail(error);
                    }
                });
        });

        actions.appendChild(cancel);
        actions.appendChild(confirm);
        box.appendChild(actions);
        backdrop.appendChild(box);
        document.body.appendChild(backdrop);
        activeModalBackdrop = backdrop;

        const first = box.querySelector('input, textarea');
        if (first) first.focus();

        return backdrop;
    }


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


    function openTaskDialog() {
        const name = makeInput('text', '');
        const description = makeTextarea('');
        const start = DateTimeField.create();
        const end = DateTimeField.create();
        const category = makeInput('text', '');
        const plan = makeInput('text', '');

        openModal('Add Task', [
            makeField('Task name', name),
            makeField('Description', description),
            makeField('Start', start.root),
            makeField('End', end.root),
            makeField('Category (optional — a new one will be created if it does not exist)', category),
            makeField('Plan (optional — a new one will be created if it does not exist)', plan)
        ], async function () {
            const taskName = name.value.trim();
            if (!taskName) {
                showModalError('Task name is required');
                return true;
            }
            const rangeError = DateTimeField.validateRange(start, end);
            if (rangeError) {
                showModalError(rangeError);
                return true;
            }

            const categoryId = await findOrCreate('/api/categories', category.value);
            const planId = await findOrCreate('/api/plans', plan.value, { description: '' });

            await API.post('/api/tasks', {
                name: taskName,
                description: description.value.trim(),
                startAt: start.getValue(),
                dueAt: end.getValue(),
                categoryId: categoryId,
                planId: planId
            });

            await load();
            return false;
        });
    }


    function openEventDialog() {
        const name = makeInput('text', '');
        const description = makeTextarea('');
        const start = DateTimeField.create();
        const end = DateTimeField.create();

        openModal('Add Event', [
            makeField('Event name', name),
            makeField('Description', description),
            makeField('Start', start.root),
            makeField('End', end.root)
        ], async function () {
            const eventName = name.value.trim();
            if (!eventName) {
                showModalError('Event name is required');
                return true;
            }
            const rangeError = DateTimeField.validateRange(start, end);
            if (rangeError) {
                showModalError(rangeError);
                return true;
            }

            await API.post('/api/events', {
                name: eventName,
                description: description.value.trim(),
                startAt: start.getValue(),
                endAt: end.getValue()
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
            if (!value) { showModalError('Task name is required'); return true; }
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
        const start = DateTimeField.create(task.startAt);
        const end = DateTimeField.create(task.dueAt);
        openModal('Redate Task', [
            makeField('Start', start.root),
            makeField('End', end.root)
        ], async function () {
            const rangeError = DateTimeField.validateRange(start, end);
            if (rangeError) {
                showModalError(rangeError);
                return true;
            }
            await applyTask(task, {
                startAt: start.getValue(),
                dueAt: end.getValue()
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
            if (!value) { showModalError('Event name is required'); return true; }
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
        const start = DateTimeField.create(event.startAt);
        const end = DateTimeField.create(event.endAt);
        openModal('Redate Event', [
            makeField('Start', start.root),
            makeField('End', end.root)
        ], async function () {
            const rangeError = DateTimeField.validateRange(start, end);
            if (rangeError) {
                showModalError(rangeError);
                return true;
            }
            await applyEvent(event, {
                startAt: start.getValue(),
                endAt: end.getValue()
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