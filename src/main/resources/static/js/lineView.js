
(function () {
    'use strict';

    const scope = document.body.dataset.scope || 'day';
    const main = document.getElementById('main-display-area');
    if (!main) return;

    const grid = main.querySelector('.grid-for-view-in-line');
    if (!grid) return;

    const HEAT = ['#caf0f8', '#ade8f4', '#90e0ef', '#48cae4', '#00b4d8', '#0096c7', '#0077b6'];

    const STATUS_CLASS = {
        COMPLETED: 'is-completed',
        ONGOING: 'is-ongoing',
        FAILED: 'is-failed'
    };

    const CONFIG = {
        day:   { slots: 24, columns: 8 },
        week:  { slots: 7,  columns: 3 },
        month: { slots: 31, columns: 8 },
        year:  { slots: 12, columns: 4 }
    };
    const cfg = CONFIG[scope] || CONFIG.day;


    const MIN_MS = {
        day: 0,
        week: 24 * 60 * 60 * 1000,
        month: 24 * 60 * 60 * 1000,
        year: null
    }[scope];

    const COLOR = (function () {
        const root = window.getComputedStyle(document.documentElement);
        return {
            COMPLETED: root.getPropertyValue('--state-completed').trim(),
            ONGOING: root.getPropertyValue('--state-ongoing-line').trim(),
            FAILED: root.getPropertyValue('--state-failed').trim()
        };
    })();

    let tasks = [];
    let entries = [];
    let blocks = [];
    let connectors = null;
    let geom = null;


    function startOfScope() {
        const now = new Date();
        const day = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        if (scope === 'day') return day;
        if (scope === 'week') {
            const shift = (day.getDay() + 6) % 7;
            day.setDate(day.getDate() - shift);
            return day;
        }
        if (scope === 'month') return new Date(now.getFullYear(), now.getMonth(), 1);
        return new Date(now.getFullYear(), 0, 1);
    }

    const origin = startOfScope();

    function slotOf(date) {
        if (scope === 'year') {
            return (date.getFullYear() - origin.getFullYear()) * 12 + (date.getMonth() - origin.getMonth());
        }
        const ms = scope === 'day' ? 3600000 : 86400000;
        return Math.floor((date.getTime() - origin.getTime()) / ms);
    }

    function rangeOf(task) {
        const startText = task.startAt || task.dueAt;
        const endText = task.dueAt || task.startAt;
        if (!startText || !endText) return null;

        const startDate = new Date(startText);
        const endDate = new Date(endText);
        if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) return null;

        let a = slotOf(startDate);
        let b = slotOf(new Date(endDate.getTime() - 1));
        if (b < 0 || a > cfg.slots - 1) return null;

        a = Math.max(a, 0);
        b = Math.min(b, cfg.slots - 1);
        if (b < a) b = a;
        return [a, b];
    }


    function exceedsOneMonth(task) {
        const start = new Date(task.startAt);
        const end = new Date(task.dueAt);
        const limit = new Date(start.getTime());
        limit.setMonth(limit.getMonth() + 1);
        return end.getTime() > limit.getTime();
    }

    function passes(task) {
        if (scope === 'day') return true;
        if (!task.startAt || !task.dueAt) return false;
        if (scope === 'year') return exceedsOneMonth(task);
        return new Date(task.dueAt).getTime() - new Date(task.startAt).getTime() >= MIN_MS;
    }

    function visible() {
        return tasks.filter(passes);
    }


    function computeEntries(list) {
        const sorted = list.slice().sort(function (a, b) {
            const byStart = String(a.startAt || '').localeCompare(String(b.startAt || ''));
            return byStart !== 0 ? byStart : (a.id - b.id);
        });

        const laneRanges = [];
        const result = [];

        sorted.forEach(function (task) {
            const range = rangeOf(task);
            if (!range) return;

            let lane = 0;
            while (true) {
                const used = (laneRanges[lane] || []).some(function (iv) {
                    return !(range[1] < iv[0] || range[0] > iv[1]);
                });
                if (!used) break;
                lane += 1;
            }

            if (!laneRanges[lane]) laneRanges[lane] = [];
            laneRanges[lane].push(range);

            result.push({ task: task, range: range, lane: lane });
        });

        return result;
    }


    function buildBlocks(counts) {
        Render.clear(grid);
        connectors = null;
        blocks = [];

        for (let i = 0; i < cfg.slots; i++) {
            const block = document.createElement('div');
            block.className = 'single-view-in-line-block';
            block.dataset.slot = String(i);
            block.dataset.count = String(counts[i]);
            block.style.backgroundColor = HEAT[Math.min(counts[i], 6)];
            if (counts[i] >= 6) block.classList.add('is-dark');

            const more = document.createElement('img');
            more.className = 'expand-button';
            more.src = '../images/more.png';
            more.alt = 'show more';
            more.addEventListener('mouseenter', function () { openFrom(block); });
            block.appendChild(more);

            block.addEventListener('mouseleave', function (e) {
                if (!block.classList.contains('is-open')) return;

                const next = e.relatedTarget;
                if (next && next.closest && next.closest('.single-view-in-line-block.is-open')) return;

                collapseAll();
            });
            grid.appendChild(block);
            blocks.push(block);
        }
    }

    function buildPills() {
        entries.forEach(function (entry) {

            const segments = [];
            for (let slot = entry.range[0]; slot <= entry.range[1]; slot++) {
                const row = Math.floor(slot / cfg.columns);
                const last = segments[segments.length - 1];
                if (last && last.row === row) {
                    last.end = slot;
                } else {
                    segments.push({ row: row, start: slot, end: slot });
                }
            }

            segments.forEach(function (seg) {
                for (let slot = seg.start; slot <= seg.end; slot++) {
                    const pill = document.createElement('div');
                    pill.className = 'view-in-grid-entries ' + (STATUS_CLASS[entry.task.status] || '');
                    pill.dataset.taskId = String(entry.task.id);
                    pill.dataset.slot = String(slot);
                    pill.dataset.lane = String(entry.lane);

                    if (slot > entry.range[0]) pill.classList.add('flush-left');
                    if (slot < entry.range[1]) pill.classList.add('flush-right');

                    blocks[slot].appendChild(pill);
                }
            });
        });
    }


    function buildLabels() {
        const layer = document.createElement('div');
        layer.className = 'line-labels';
        grid.appendChild(layer);

        entries.forEach(function (entry) {
            const segments = [];
            for (let slot = entry.range[0]; slot <= entry.range[1]; slot++) {
                const row = Math.floor(slot / cfg.columns);
                const last = segments[segments.length - 1];
                if (last && last.row === row) {
                    last.end = slot;
                } else {
                    segments.push({ row: row, start: slot, end: slot });
                }
            }

            segments.forEach(function (seg) {
                const label = document.createElement('span');
                label.className = 'line-label';
                label.dataset.taskId = String(entry.task.id);
                label.dataset.segStart = String(seg.start);
                label.dataset.segEnd = String(seg.end);
                label.textContent = entry.task.name || '';
                layer.appendChild(label);
            });
        });
    }

    function positionLabels() {
        const layer = grid.querySelector('.line-labels');
        if (!layer || !geom || !blocks.length) return;

        const overlay = layer.getBoundingClientRect();
        const rects = blocks.map(function (block) { return block.getBoundingClientRect(); });

        layer.querySelectorAll('.line-label').forEach(function (label) {
            const taskId = Number(label.dataset.taskId);
            const segStart = Number(label.dataset.segStart);
            const segEnd = Number(label.dataset.segEnd);


            const pill = blocks[segStart]
                .querySelector('.view-in-grid-entries[data-task-id="' + taskId + '"]');

            if (!pill || pill.style.display === 'none') {
                label.style.display = 'none';
                return;
            }


            let onTop = false;
            for (let slot = segStart; slot <= segEnd; slot++) {
                if (blocks[slot] && blocks[slot].classList.contains('is-open')) {
                    onTop = true;
                    break;
                }
            }
            label.classList.toggle('is-on-top', onTop);

            const lane = Number(pill.dataset.lane);
            const left = rects[segStart].left;
            const right = rects[segEnd].right;

            label.style.display = '';
            label.style.left = (left - overlay.left) + 'px';
            label.style.width = Math.max(0, right - left) + 'px';
            label.style.top = (rects[segStart].top + geom.padTop + lane * (geom.pillH + geom.gap) - overlay.top) + 'px';
            label.style.height = geom.pillH + 'px';
        });
    }




    function positionPills(block, laneLimit) {
        let hidden = false;

        block.querySelectorAll('.view-in-grid-entries').forEach(function (pill) {
            const lane = Number(pill.dataset.lane);

            if (lane >= laneLimit) {
                hidden = true;
                pill.style.display = 'none';
                return;
            }

            const flushLeft = pill.classList.contains('flush-left');
            const flushRight = pill.classList.contains('flush-right');

            pill.style.display = '';
            pill.style.top = (geom.padTop + lane * (geom.pillH + geom.gap)) + 'px';
            pill.style.height = geom.pillH + 'px';
            pill.style.left = (flushLeft ? 0 : geom.padLeft) + 'px';
            pill.style.width = (geom.box.width
                - (flushLeft ? 0 : geom.padLeft)
                - (flushRight ? 0 : geom.padRight)) + 'px';
        });

        return hidden;
    }

    function layout() {
        if (!blocks.length) return;

        const box = blocks[0].getBoundingClientRect();
        if (box.width === 0 || box.height === 0) return;

        const style = window.getComputedStyle(blocks[0]);
        const padTop = parseFloat(style.paddingTop) || 0;
        const padBottom = parseFloat(style.paddingBottom) || 0;
        const padLeft = parseFloat(style.paddingLeft) || 0;
        const padRight = parseFloat(style.paddingRight) || 0;

        const moreH = Math.max(12, box.height * 0.08);
        const pillH = Math.max(14, box.height * 0.13);
        const gap = Math.max(4, pillH * 0.3);

        const usable = box.height - padTop - padBottom - moreH;
        const lanesFit = Math.max(1, Math.floor((usable + gap) / (pillH + gap)));

        geom = {
            box: box,
            padTop: padTop,
            padBottom: padBottom,
            padLeft: padLeft,
            padRight: padRight,
            moreH: moreH,
            pillH: pillH,
            gap: gap,
            lanesFit: lanesFit
        };

        blocks.forEach(function (block) {
            const hidden = positionPills(block, geom.lanesFit);
            if (!block.classList.contains('is-open')) {
                block.classList.toggle('overflowing', hidden);
            }
        });
        positionLabels();
    }


    function drawConnectors() {
        if (connectors) connectors.remove();
        connectors = document.createElement('div');
        connectors.className = 'line-connectors';
        grid.appendChild(connectors);

        if (!geom || grid.style.display !== 'grid') return;

        const overlay = connectors.getBoundingClientRect();

        entries.forEach(function (entry) {
            for (let slot = entry.range[0]; slot < entry.range[1]; slot++) {
                if (Math.floor(slot / cfg.columns) !== Math.floor((slot + 1) / cfg.columns)) continue;

                const leftBlock = blocks[slot];
                const rightBlock = blocks[slot + 1];
                if (!leftBlock || !rightBlock) continue;

                const leftPill = leftBlock
                    .querySelector('.view-in-grid-entries[data-task-id="' + entry.task.id + '"]');
                const rightPill = rightBlock
                    .querySelector('.view-in-grid-entries[data-task-id="' + entry.task.id + '"]');


                if (!leftPill || !rightPill) continue;
                if (leftPill.style.display === 'none' || rightPill.style.display === 'none') continue;

                const lb = leftBlock.getBoundingClientRect();
                const rb = rightBlock.getBoundingClientRect();
                const top = parseFloat(leftPill.style.top) || 0;

                const piece = document.createElement('div');
                piece.className = 'line-connector';
                piece.style.left = (lb.right - overlay.left) + 'px';
                piece.style.width = Math.max(0, rb.left - lb.right) + 'px';
                piece.style.top = (lb.top + top - overlay.top) + 'px';
                piece.style.height = geom.pillH + 'px';
                piece.style.backgroundColor = COLOR[entry.task.status] || COLOR.ONGOING;

                connectors.appendChild(piece);
            }
        });
    }


    function openBlock(block) {
        if (!geom) return;

        block.classList.add('is-open');
        positionPills(block, Infinity);

        let maxLane = 0;
        block.querySelectorAll('.view-in-grid-entries').forEach(function (pill) {
            maxLane = Math.max(maxLane, Number(pill.dataset.lane));
        });

        const needed = geom.padTop + (maxLane + 1) * (geom.pillH + geom.gap) - geom.gap +
            geom.padBottom + geom.moreH;

        block.style.height = Math.max(geom.box.height, needed) + 'px';
    }

    function openFrom(block) {
        blocks.forEach(function (item) {
            item.classList.remove('is-open');
            item.style.height = '';
        });
        layout();

        const ids = new Set();
        block.querySelectorAll('.view-in-grid-entries').forEach(function (pill) {
            ids.add(Number(pill.dataset.taskId));
        });

        if (!ids.size) {
            openBlock(block);
        } else {
            blocks.forEach(function (candidate) {
                const hit = Array.prototype.some.call(
                    candidate.querySelectorAll('.view-in-grid-entries'),
                    function (pill) { return ids.has(Number(pill.dataset.taskId)); }
                );
                if (hit) openBlock(candidate);
            });
        }


        positionLabels();
        window.requestAnimationFrame(drawConnectors);
    }

    function collapseAll() {
        let changed = false;

        blocks.forEach(function (block) {
            if (block.classList.contains('is-open')) changed = true;
            block.classList.remove('is-open');
            block.style.height = '';
        });

        if (!changed) return;

        window.requestAnimationFrame(function () {
            layout();
            drawConnectors();
        });
    }


    function build() {
        const list = visible();
        entries = computeEntries(list);

        const counts = new Array(cfg.slots).fill(0);
        entries.forEach(function (entry) {
            for (let i = entry.range[0]; i <= entry.range[1]; i++) counts[i] += 1;
        });

        buildBlocks(counts);
        buildPills();
        buildLabels();

        window.requestAnimationFrame(function () {
            layout();
            drawConnectors();
        });
    }

    function setTasks(list) {
        tasks = Array.isArray(list) ? list : [];
        build();
    }

    window.LineView = { setTasks: setTasks, renderBars: build };


    build();

    const lineButton = document.querySelector('.view-as-line-button');
    if (lineButton) {
        lineButton.addEventListener('click', function () {
            window.setTimeout(build, 0);
        });
    }

    grid.addEventListener('mouseleave', collapseAll);

    let resizeTimer = null;
    window.addEventListener('resize', function () {
        if (grid.style.display !== 'grid') return;
        window.clearTimeout(resizeTimer);
        resizeTimer = window.setTimeout(function () { collapseAll(); build(); }, 150);
    });
})();