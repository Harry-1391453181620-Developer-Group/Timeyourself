
const DateTimeField = (() => {
    'use strict';

    const PARTS = [
        { key: 'year', digits: 4, placeholder: 'YYYY', label: 'Year' },
        { key: 'month', digits: 2, placeholder: 'MM', label: 'Month' },
        { key: 'day', digits: 2, placeholder: 'DD', label: 'Day' },
        { key: 'hour', digits: 2, placeholder: 'HH', label: 'Hour' },
        { key: 'minute', digits: 2, placeholder: 'MM', label: 'Minute' }
    ];

    const WEEKDAYS = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];



    const CALENDAR_ICON = '<img class="ts-dt-calendar-icon" src="/images/icons/calendar-32.png" alt="" aria-hidden="true" draggable="false">';

    function pad2(value) {
        return String(value).padStart(2, '0');
    }

    function partsFromIso(raw) {
        if (!raw) return null;
        const match = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})/.exec(String(raw).trim());
        if (!match) return null;
        return {
            year: match[1],
            month: match[2],
            day: match[3],
            hour: match[4],
            minute: match[5]
        };
    }

    function daysInMonth(year, month) {
        return new Date(year, month, 0).getDate();
    }

    function validParts(parts) {
        if (!parts) return false;
        const year = Number(parts.year);
        const month = Number(parts.month);
        const day = Number(parts.day);
        const hour = Number(parts.hour);
        const minute = Number(parts.minute);
        if (!(year >= 1 && year <= 9999)) return false;
        if (!(month >= 1 && month <= 12)) return false;
        if (!(day >= 1 && day <= daysInMonth(year, month))) return false;
        if (!(hour >= 0 && hour <= 23)) return false;
        if (!(minute >= 0 && minute <= 59)) return false;
        return true;
    }

    function isoOf(parts) {
        return parts.year + '-' + parts.month + '-' + parts.day +
            'T' + parts.hour + ':' + parts.minute;
    }

    function todayParts() {
        const now = new Date();
        return {
            year: String(now.getFullYear()),
            month: pad2(now.getMonth() + 1),
            day: pad2(now.getDate())
        };
    }

    function create(initialValue) {
        const root = document.createElement('div');
        root.className = 'ts-datetime';

        const inputs = {};
        const order = [];

        PARTS.forEach(function (part, index) {
            if (index > 0) {
                const separator = document.createElement('span');
                separator.className = 'ts-dt-sep';
                separator.textContent = index === 3 ? ':' : '-';
                root.appendChild(separator);
            }

            const input = document.createElement('input');
            input.type = 'text';
            input.inputMode = 'numeric';
            input.autocomplete = 'off';
            input.spellcheck = false;
            input.maxLength = part.digits;
            input.placeholder = part.placeholder;
            input.className = 'ts-dt-part ts-dt-' + part.key;
            input.setAttribute('aria-label', part.label);
            input.dataset.part = part.key;
            root.appendChild(input);
            inputs[part.key] = input;
            order.push(input);
        });

        const calendarButton = document.createElement('button');
        calendarButton.type = 'button';
        calendarButton.className = 'ts-dt-calendar';
        calendarButton.setAttribute('aria-label', 'Choose a date from the calendar');
        calendarButton.setAttribute('aria-expanded', 'false');
        calendarButton.innerHTML = CALENDAR_ICON;
        root.appendChild(calendarButton);

        let popover = null;
        let viewYear = 0;
        let viewMonth = 0;
        let outsideHandler = null;
        let keyHandler = null;
        let scrollHandler = null;

        function readParts() {
            const parts = {};
            order.forEach(function (input, index) {
                parts[PARTS[index].key] = input.value;
            });
            return parts;
        }

        function blank() {
            return order.every(function (input) { return input.value === ''; });
        }

        function complete() {
            return validParts(readParts());
        }

        function value() {
            return complete() ? isoOf(readParts()) : null;
        }

        function fill(parts) {
            PARTS.forEach(function (part, index) {
                order[index].value = parts && parts[part.key] ? parts[part.key] : '';
            });
        }

        function setValue(raw) {
            fill(partsFromIso(raw));
        }

        function focus() {
            for (let index = 0; index < order.length; index += 1) {
                if (order[index].value.length < PARTS[index].digits) {
                    order[index].focus();
                    return;
                }
            }
            order[0].focus();
        }

        function moveTo(index) {
            const target = order[index];
            if (!target) return;
            target.focus();
            target.select();
        }

        function sanitize(input, part) {
            const digits = input.value.replace(/\D/g, '').slice(0, part.digits);
            if (digits !== input.value) input.value = digits;
        }

        function jumpBackToPrevious(input) {
            const index = order.indexOf(input);
            if (index <= 0) return false;
            const previous = order[index - 1];
            previous.focus();
            previous.value = previous.value.slice(0, -1);
            const end = previous.value.length;
            previous.setSelectionRange(end, end);
            return true;
        }

        order.forEach(function (input, index) {
            const part = PARTS[index];

            input.addEventListener('focus', function () {
                input.select();
            });

            input.addEventListener('input', function () {
                sanitize(input, part);
                if (input.value.length === part.digits) {
                    moveTo(index + 1);
                }
            });

            input.addEventListener('keydown', function (e) {
                if (e.ctrlKey || e.metaKey || e.altKey) return;

                if (e.key === 'Backspace' || e.key === 'Delete') {
                    if (input.value.length === 0 && jumpBackToPrevious(input)) {
                        e.preventDefault();
                    }
                    return;
                }

                if (e.key === 'ArrowLeft') {
                    if (input.selectionStart === 0 && index > 0) {
                        e.preventDefault();
                        moveTo(index - 1);
                    }
                    return;
                }

                if (e.key === 'ArrowRight') {
                    if (input.selectionStart === input.value.length && index < order.length - 1) {
                        e.preventDefault();
                        moveTo(index + 1);
                    }
                    return;
                }

                if (e.key === 'Escape') {
                    closeCalendar();
                    return;
                }

                if (e.key.length === 1 && !/^\d$/.test(e.key)) {
                    e.preventDefault();
                }
            });
        });

        function closeCalendar() {
            calendarButton.setAttribute('aria-expanded', 'false');
            if (popover) {
                popover.remove();
                popover = null;
            }
            if (outsideHandler) {
                document.removeEventListener('mousedown', outsideHandler, true);
                outsideHandler = null;
            }
            if (keyHandler) {
                document.removeEventListener('keydown', keyHandler, true);
                keyHandler = null;
            }
            if (scrollHandler) {
                window.removeEventListener('scroll', scrollHandler, true);
                scrollHandler = null;
            }
        }

        function placePopover() {
            if (!popover) return;
            const rect = root.getBoundingClientRect();
            const width = popover.offsetWidth || 232;
            const height = popover.offsetHeight || 260;
            const margin = 8;

            let left = rect.right - width;
            left = Math.max(margin, Math.min(left, window.innerWidth - width - margin));

            let top = rect.bottom + 6;
            if (top + height > window.innerHeight - margin && rect.top - height - 6 > margin) {
                top = rect.top - height - 6;
            }

            popover.style.left = left + 'px';
            popover.style.top = top + 'px';
        }

        function renderCalendar() {
            if (!popover) return;

            const title = popover.querySelector('.ts-calendar-title');
            const body = popover.querySelector('.ts-calendar-body');
            title.textContent = viewYear + '-' + pad2(viewMonth);

            while (body.firstChild) body.removeChild(body.firstChild);

            const firstDay = new Date(viewYear, viewMonth - 1, 1);
            const lead = (firstDay.getDay() + 6) % 7;
            const total = daysInMonth(viewYear, viewMonth);
            const selected = readParts();
            const today = todayParts();

            for (let blankCell = 0; blankCell < lead; blankCell += 1) {
                const empty = document.createElement('button');
                empty.type = 'button';
                empty.className = 'ts-calendar-day is-blank';
                empty.disabled = true;
                empty.tabIndex = -1;
                body.appendChild(empty);
            }

            for (let day = 1; day <= total; day += 1) {
                const cell = document.createElement('button');
                cell.type = 'button';
                cell.className = 'ts-calendar-day';
                cell.textContent = String(day);
                cell.dataset.day = String(day);

                const isSelected = Number(selected.year) === viewYear &&
                    Number(selected.month) === viewMonth &&
                    Number(selected.day) === day;
                if (isSelected) cell.classList.add('is-selected');

                const isToday = Number(today.year) === viewYear &&
                    Number(today.month) === viewMonth &&
                    Number(today.day) === day;
                if (isToday) cell.classList.add('is-today');

                body.appendChild(cell);
            }
        }

        function openCalendar() {
            closeCalendar();

            popover = document.createElement('div');
            popover.className = 'ts-calendar';
            popover.setAttribute('role', 'dialog');
            popover.setAttribute('aria-label', 'Choose date');

            const head = document.createElement('div');
            head.className = 'ts-calendar-head';

            const previous = document.createElement('button');
            previous.type = 'button';
            previous.className = 'ts-calendar-nav';
            previous.setAttribute('aria-label', 'Previous month');
            previous.textContent = '‹';

            const title = document.createElement('span');
            title.className = 'ts-calendar-title';

            const next = document.createElement('button');
            next.type = 'button';
            next.className = 'ts-calendar-nav';
            next.setAttribute('aria-label', 'Next month');
            next.textContent = '›';

            head.appendChild(previous);
            head.appendChild(title);
            head.appendChild(next);

            const weekdays = document.createElement('div');
            weekdays.className = 'ts-calendar-weekdays';
            WEEKDAYS.forEach(function (name) {
                const cell = document.createElement('span');
                cell.textContent = name;
                weekdays.appendChild(cell);
            });

            const body = document.createElement('div');
            body.className = 'ts-calendar-body ts-calendar-grid';

            popover.appendChild(head);
            popover.appendChild(weekdays);
            popover.appendChild(body);
            root.appendChild(popover);

            const parts = readParts();
            const today = todayParts();
            if (/^\d{4}$/.test(parts.year) && /^\d{1,2}$/.test(parts.month)) {
                viewYear = Number(parts.year);
                viewMonth = Math.min(12, Math.max(1, Number(parts.month)));
            } else {
                viewYear = Number(today.year);
                viewMonth = Number(today.month);
            }

            renderCalendar();
            placePopover();
            calendarButton.setAttribute('aria-expanded', 'true');

            previous.addEventListener('click', function () {
                viewMonth -= 1;
                if (viewMonth < 1) {
                    viewMonth = 12;
                    viewYear -= 1;
                }
                renderCalendar();
                placePopover();
            });

            next.addEventListener('click', function () {
                viewMonth += 1;
                if (viewMonth > 12) {
                    viewMonth = 1;
                    viewYear += 1;
                }
                renderCalendar();
                placePopover();
            });

            body.addEventListener('click', function (e) {
                const cell = e.target.closest('.ts-calendar-day');
                if (!cell || cell.disabled || !cell.dataset.day) return;

                inputs.year.value = String(viewYear).padStart(4, '0');
                inputs.month.value = pad2(viewMonth);
                inputs.day.value = pad2(Number(cell.dataset.day));
                closeCalendar();

                if (!inputs.hour.value) inputs.hour.focus();
            });

            outsideHandler = function (e) {
                if (root.contains(e.target)) return;
                closeCalendar();
            };
            document.addEventListener('mousedown', outsideHandler, true);

            keyHandler = function (e) {
                if (e.key !== 'Escape') return;
                e.stopPropagation();
                closeCalendar();
            };
            document.addEventListener('keydown', keyHandler, true);

            scrollHandler = function () { closeCalendar(); };
            window.addEventListener('scroll', scrollHandler, true);
        }

        calendarButton.addEventListener('click', function () {
            if (popover) closeCalendar();
            else openCalendar();
        });

        if (initialValue) setValue(initialValue);

        return {
            root: root,
            getValue: value,
            isBlank: blank,
            isComplete: complete,
            setValue: setValue,
            focus: focus,
            validate: function () {
                if (blank()) return null;
                if (!complete()) {
                    return 'Please complete the date and time in the format YYYY-MM-DD HH:MM.';
                }
                return null;
            }
        };
    }

    function validateRange(startField, endField) {
        const startError = startField.validate();
        if (startError) return 'Start: ' + startError;

        const endError = endField.validate();
        if (endError) return 'End: ' + endError;

        const start = startField.getValue();
        const end = endField.getValue();
        if (start && end && end < start) {
            return 'End cannot be before start.';
        }
        return null;
    }

    return {
        create: create,
        validateRange: validateRange
    };
})();

window.DateTimeField = DateTimeField;
