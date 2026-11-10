const Render = (() => {
    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined && text !== null) node.textContent = text;
        return node;
    }

    function fmt(value) {
        if (!value) return '--';
        const s = String(value).replace('T', ' ');
        const date = s.slice(0, 10).split('-').join('/');
        const time = s.slice(11, 16);
        return time ? date + ' ' + time : date;
    }

    const STATE_CLASS = {
        COMPLETED: 'is-completed',
        ONGOING: 'is-ongoing',
        FAILED: 'is-failed'
    };

    function dateArea(startAt, endAt) {
        const area = el('div', 'date-area');
        const start = el('div', 'start-date');
        start.appendChild(el('p', null, 'St: ' + fmt(startAt)));
        const end = el('div', 'end-date');
        end.appendChild(el('p', null, 'Ed: ' + fmt(endAt)));
        area.appendChild(start);
        area.appendChild(end);
        return area;
    }

    function checkbox(completed, id) {
        const box = el('div', 'checkbox' + (completed ? ' is-checked' : ''));
        box.dataset.id = id;
        box.setAttribute('role', 'checkbox');
        box.setAttribute('aria-checked', String(completed));
        return box;
    }

    function buttonArea() {
        const area = el('div', 'button-area');
        const kinds = ['rename', 'reclarify', 'redate', 'delete'];
        kinds.forEach((kind) => {
            const button = el('div', kind + '-button');
            button.dataset.action = kind;
            button.appendChild(el('p', null, kind.charAt(0).toUpperCase() + kind.slice(1)));
            area.appendChild(button);
        });
        return area;
    }


    function lineText(text) {
        const p = el('p');
        const span = el('span', 'entry-line');
        span.textContent = text;
        p.appendChild(span);
        return p;
    }

    function taskRow(task) {
        const row = el('div', 'main-display-bar-task-entry ' + (STATE_CLASS[task.status] || ''));
        row.dataset.id = task.id;
        row.dataset.status = task.status || '';

        const name = el('div', 'name-area');
        name.appendChild(lineText(task.name || ''));

        const sort = el('div', 'sort-area');
        const category = el('div', 'category-part');
        category.appendChild(lineText('Category: ' + (task.categoryName || '-')));
        const plan = el('div', 'plan-part');
        plan.appendChild(lineText('Plan: ' + (task.planName || '-')));
        sort.appendChild(category);
        sort.appendChild(plan);

        const description = el('div', 'description-area');
        description.appendChild(lineText('Description: ' + (task.description || '')));

        row.appendChild(checkbox(task.completed, task.id));
        row.appendChild(name);
        row.appendChild(dateArea(task.startAt, task.dueAt));
        row.appendChild(sort);
        row.appendChild(description);
        row.appendChild(buttonArea());
        return row;
    }

    function eventRow(event) {
        const row = el('div', 'main-display-bar-event-entry');
        row.dataset.id = event.id;
        row.dataset.state = event.state || '';

        const name = el('div', 'name-area');
        name.appendChild(lineText(event.name || ''));

        const description = el('div', 'description-area');
        description.appendChild(lineText('Description: ' + (event.description || '')));

        row.appendChild(checkbox(event.completed, event.id));
        row.appendChild(name);
        row.appendChild(dateArea(event.startAt, event.endAt));
        row.appendChild(description);
        row.appendChild(buttonArea());
        return row;
    }

    function chooseEntry(item, handlers) {
        const entry = el('div', 'entry');
        entry.dataset.id = item.id;

        const name = el('div', 'name');
        name.appendChild(el('p', null, item.name + ' (' + (item.taskCount || 0) + ')'));

        const buttons = el('div', 'buttons');
        const rename = el('div', 'rename-button-for-choose-bars', 'R');
        rename.dataset.action = 'rename';
        const remove = el('div', 'delete-button-for-choose-bars', 'D');
        remove.dataset.action = 'delete';
        buttons.appendChild(rename);
        buttons.appendChild(remove);

        entry.appendChild(name);
        entry.appendChild(buttons);

        entry.addEventListener('click', (e) => {
            const action = e.target.dataset ? e.target.dataset.action : null;
            if (action === 'rename') handlers.rename(item);
            else if (action === 'delete') handlers.remove(item);
            else handlers.select(item);
        });
        return entry;
    }

    function footer(summary, counts) {
        const status = document.querySelector('.foot-status-bar p');
        const sentence = document.querySelector('.foot-sentence-bar p');
        const progress = document.querySelector('.foot-progress-bar p');

        const finished = counts ? counts.finished : summary.finishedTaskNumber;
        const remaining = counts ? counts.remaining : summary.remainingTaskNumber;

        if (status) {
            status.textContent = 'Remainder: Task: ' + (summary.ongoingTaskName || '...') +
                ' is ongoing, Event: ' + (summary.ongoingEventName || '...') + ' is ongoing';
        }
        if (sentence) {
            sentence.textContent = 'One Day One Sentence: ' + (summary.sentence || '');
        }
        if (progress) {
            progress.textContent = 'Finished Task: ' + finished +
                ' Remaining Task: ' + remaining;
        }
    }

    function clear(node) {
        while (node.firstChild) node.removeChild(node.firstChild);
    }

    return {
        el, fmt, taskRow, eventRow, chooseEntry, footer, clear, STATE_CLASS
    };
})();

window.Render = Render;