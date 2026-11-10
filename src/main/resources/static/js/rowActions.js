
const RowActions = (() => {
    'use strict';

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
        confirm.textContent = submitLabel || 'Save';
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
                    console.error(error);
                    if (activeModalBackdrop === backdrop) {
                        showModalError('请求失败：' + error.message);
                    } else {
                        alert('请求失败：' + error.message);
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

    function bind(container, options) {
        const getTasks = options.getTasks || function () { return []; };
        const getEvents = options.getEvents || function () { return []; };
        const reload = options.reload || function () {};

        function findTask(id) {
            return getTasks().find(function (task) { return task.id === id; });
        }

        function findEvent(id) {
            return getEvents().find(function (event) { return event.id === id; });
        }

        async function applyTask(task, changes) {
            await API.put('/api/tasks/' + task.id, Object.assign(taskPayload(task), changes));
            await reload();
        }

        async function applyEvent(event, changes) {
            await API.put('/api/events/' + event.id, Object.assign(eventPayload(event), changes));
            await reload();
        }

        function renameTask(task) {
            const name = makeInput('text', task.name);
            openModal('Rename Task', [makeField('Task name', name)], async function () {
                const value = name.value.trim();
                if (!value) { showModalError('Task name is required'); return true; }
                await applyTask(task, { name: value });
            });
        }

        function reclarifyTask(task) {
            const description = makeTextarea(task.description || '');
            openModal('Reclarify Task', [makeField('Description', description)], async function () {
                await applyTask(task, { description: description.value.trim() });
            });
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
                await reload();
            }, 'Delete');
        }

        function renameEvent(event) {
            const name = makeInput('text', event.name);
            openModal('Rename Event', [makeField('Event name', name)], async function () {
                const value = name.value.trim();
                if (!value) { showModalError('Event name is required'); return true; }
                await applyEvent(event, { name: value });
            });
        }

        function reclarifyEvent(event) {
            const description = makeTextarea(event.description || '');
            openModal('Reclarify Event', [makeField('Description', description)], async function () {
                await applyEvent(event, { description: description.value.trim() });
            });
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
                await reload();
            }, 'Delete');
        }

        container.addEventListener('click', function (e) {
            const taskRow = e.target.closest('.main-display-bar-task-entry');
            if (taskRow) {
                const task = findTask(Number(taskRow.dataset.id));
                if (!task) return;

                if (e.target.closest('.checkbox')) {
                    API.patch('/api/tasks/' + task.id + '/completion', { completed: !task.completed })
                        .then(reload)
                        .catch(function (error) { alert('请求失败：' + error.message); });
                    return;
                }

                const action = e.target.closest('[data-action]');
                if (!action) return;

                const kind = action.dataset.action;
                if (kind === 'rename') renameTask(task);
                else if (kind === 'reclarify') reclarifyTask(task);
                else if (kind === 'redate') redateTask(task);
                else if (kind === 'delete') deleteTask(task);
                return;
            }

            const eventRow = e.target.closest('.main-display-bar-event-entry');
            if (!eventRow) return;

            if (e.target.closest('.checkbox')) return;

            const event = findEvent(Number(eventRow.dataset.id));
            if (!event) return;

            const action = e.target.closest('[data-action]');
            if (!action) return;

            const kind = action.dataset.action;
            if (kind === 'rename') renameEvent(event);
            else if (kind === 'reclarify') reclarifyEvent(event);
            else if (kind === 'redate') redateEvent(event);
            else if (kind === 'delete') deleteEvent(event);
        });
    }

    return {
        bind: bind,
        openModal: openModal,
        makeInput: makeInput,
        makeTextarea: makeTextarea,
        makeField: makeField
    };
})();

window.RowActions = RowActions;
