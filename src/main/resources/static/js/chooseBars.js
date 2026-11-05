/* TimeYourself - choose bar：分类 / 计划两列，交替填充 */
const ChooseBars = (() => {
    'use strict';

    const state = { category: [], plan: [] };
    const PATH = { category: '/api/categories', plan: '/api/plans' };
    const LABEL = { category: 'Category', plan: 'Plan' };
    const ID_PARAM = { category: 'categoryId', plan: 'planId' };
    const PAGE = { category: 'categories.html', plan: 'plans.html' };

    function parts(kind) {
        const bar = document.querySelector(kind === 'category' ? '#for-categories' : '#for-plans');
        if (!bar) return null;
        const columns = bar.querySelectorAll('.entry-column');
        return { bar: bar, left: columns[0], right: columns[1] || null };
    }

    /* 交替：0、2、4… 进左列；1、3、5… 进右列 */
    function render(kind) {
        const spot = parts(kind);
        if (!spot || !spot.left) return;

        Render.clear(spot.left);
        if (spot.right) Render.clear(spot.right);

        state[kind].forEach(function (item, index) {
            const target = (index % 2 === 0) ? spot.left : (spot.right || spot.left);
            target.appendChild(Render.chooseEntry(item, {
                select: function (selected) { select(kind, selected); },
                rename: function (selected) { rename(kind, selected); },
                remove: function (selected) { remove(kind, selected); }
            }));
        });
    }

    function select(kind, item) {
        if (document.body.dataset.pick === kind) {
            document.dispatchEvent(new CustomEvent('choose', { detail: { kind: kind, item: item } }));
            return;
        }
        /* 视图页里点分类/计划：跳到对应页面并带上选中项 */
        window.location.href = PAGE[kind] + '?' + ID_PARAM[kind] + '=' + encodeURIComponent(item.id);
    }

    async function loadAll() {
        const result = await Promise.all([API.get(PATH.category), API.get(PATH.plan)]);
        state.category = result[0] || [];
        state.plan = result[1] || [];
        render('category');
        render('plan');
    }

    function add(kind) {
        const name = RowActions.makeInput('text', '');
        const description = RowActions.makeTextarea('');
        const fields = [RowActions.makeField(LABEL[kind] + ' name', name)];
        if (kind === 'plan') fields.push(RowActions.makeField('Description', description));

        RowActions.openModal('Add ' + LABEL[kind], fields, async function () {
            const value = name.value.trim();
            if (!value) { alert(LABEL[kind] + ' name is required'); return true; }

            const payload = kind === 'plan'
                ? { name: value, description: description.value.trim() }
                : { name: value };

            await API.post(PATH[kind], payload);
            await loadAll();
        }, 'Create');
    }

    function rename(kind, item) {
        const name = RowActions.makeInput('text', item.name);
        const fields = [RowActions.makeField(LABEL[kind] + ' name', name)];

        RowActions.openModal('Rename ' + LABEL[kind], fields, async function () {
            const value = name.value.trim();
            if (!value) { alert(LABEL[kind] + ' name is required'); return true; }

            const payload = kind === 'plan'
                ? { name: value, description: item.description || '' }
                : { name: value };

            await API.put(PATH[kind] + '/' + item.id, payload);
            await loadAll();
            document.dispatchEvent(new CustomEvent('choose-renamed', {
                detail: { kind: kind, item: { id: item.id, name: value } }
            }));
        });
    }

    function remove(kind, item) {
        const note = document.createElement('p');
        note.className = 'ts-modal-note';
        note.textContent = 'Delete ' + LABEL[kind].toLowerCase() + ' "' + item.name +
            '"? Its tasks stay, but become uncategorized.';

        RowActions.openModal('Delete ' + LABEL[kind], [note], async function () {
            await API.del(PATH[kind] + '/' + item.id);
            await loadAll();
            document.dispatchEvent(new CustomEvent('choose-removed', {
                detail: { kind: kind, item: item }
            }));
        }, 'Delete');
    }

    function removeAll(kind) {
        const note = document.createElement('p');
        note.className = 'ts-modal-note';
        note.textContent = 'Delete all ' + LABEL[kind].toLowerCase() + 's?';

        RowActions.openModal('Delete All ' + LABEL[kind] + 's', [note], async function () {
            await API.del(PATH[kind]);
            await loadAll();
            document.dispatchEvent(new CustomEvent('choose-removed', {
                detail: { kind: kind, item: null }
            }));
        }, 'Delete');
    }

    function bindButtons() {
        ['category', 'plan'].forEach(function (kind) {
            const spot = parts(kind);
            if (!spot) return;

            const addButton = spot.bar.querySelector('.add-button-on-choose-bars');
            const clearButton = spot.bar.querySelector('.delete-all-button-on-choose-bars');

            if (addButton) addButton.addEventListener('click', function () { add(kind); });
            if (clearButton) clearButton.addEventListener('click', function () { removeAll(kind); });
        });
    }

    bindButtons();
    loadAll().catch(function (error) {
        console.error(error);
    });

    return { reload: loadAll };
})();

window.ChooseBars = ChooseBars;
