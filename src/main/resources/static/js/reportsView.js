/* TimeYourself - Reports 页：4 个周期 × 7 个指标 */
(function () {
    'use strict';

    const container = document.querySelector('.reports-container');
    if (!container) return;

    const PERIODS = [
        { key: 'daily', title: 'Daily' },
        { key: 'weekly', title: 'Weekly' },
        { key: 'monthly', title: 'Monthly' },
        { key: 'yearly', title: 'Yearly' }
    ];

    function makeEntry(label, value) {
        const row = document.createElement('div');
        row.className = 'reports-group-entry';
        const text = document.createElement('p');
        text.textContent = label + ': ' + value;
        row.appendChild(text);
        return row;
    }

    function makeGroup(title) {
        const group = document.createElement('div');
        group.className = 'reports-group';

        const head = document.createElement('div');
        head.className = 'reports-group-title';
        const headText = document.createElement('p');
        headText.textContent = title;
        head.appendChild(headText);

        group.appendChild(head);
        container.appendChild(group);
        return group;
    }

    function round(value) {
        return Math.round(value * 100) / 100;
    }

    function hoursText(value) {
        const hours = round(value || 0);
        return hours + ' hours';
    }

    function render(reports) {
        Render.clear(container);

        reports.forEach(function (report, index) {
            const group = makeGroup(PERIODS[index].title);
            const unit = report.unit === 'hour' ? 'Hour' : 'Day';

            const rows = [
                ['Number of Tasks Done', report.tasksDone],
                ['Number of Events Past', report.eventsPast],
                ['Number of Task Failed', report.tasksFailed],
                ['Task completion Rate', Math.round(report.completionRate) + '%'],
                ['Mean Task Duration', hoursText(report.meanTaskHours)],
                ['Number of Tasks Not Done', report.tasksNotDone],
                ['Average Task per ' + unit, round(report.averageTasksPerUnit)]
            ];

            rows.forEach(function (pair) {
                group.appendChild(makeEntry(pair[0], pair[1]));
            });
        });
    }

    async function load() {
        try {
            const result = await Promise.all([
                Promise.all(PERIODS.map(function (period) {
                    return API.get('/api/reports?period=' + period.key);
                })),
                API.get('/api/summary')
            ]);

            render(result[0]);
            Render.footer(result[1]);
        } catch (error) {
            console.error(error);
            alert('请求失败：' + error.message);
        }
    }

    load();
})();