/**
 * Tini SmartStay - Weekly Block Calendar Picker (Image 3 implementation)
 * Enables selecting a continuous 7-day week block with instant row highlight,
 * week numbers (Week), and warm boutique styling.
 */
(function(window, document) {
    'use strict';

    function pad2(n) {
        return (n < 10 ? '0' : '') + n;
    }

    function formatDate(d) {
        return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
    }

    function parseDate(s) {
        if (!s) return new Date();
        var parts = s.split('-');
        if (parts.length === 3) {
            return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
        }
        return new Date();
    }

    // Get ISO-8601 week number
    function getWeekNumber(date) {
        var d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
        var dayNum = d.getUTCDay() || 7;
        d.setUTCDate(d.getUTCDate() + 4 - dayNum);
        var yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
        return Math.ceil((((d - yearStart) / 86400000) + 1) / 7);
    }

    // Get Monday (or start) of the week for given date
    function getMonday(date) {
        var d = new Date(date);
        var day = d.getDay();
        var diff = d.getDate() - day + (day === 0 ? -6 : 1); // adjust when day is sunday
        return new Date(d.setDate(diff));
    }

    var MONTH_NAMES = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    var DAY_HEADERS = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

    function WeeklyCalendar(options) {
        this.options = options || {};
        this.trigger = typeof options.trigger === 'string' ? document.querySelector(options.trigger) : options.trigger;
        this.input = typeof options.input === 'string' ? document.querySelector(options.input) : options.input;
        this.weekStartInput = typeof options.weekStartInput === 'string' ? document.querySelector(options.weekStartInput) : options.weekStartInput;
        this.onSelect = options.onSelect || null;

        var initialDate = this.input && this.input.value ? parseDate(this.input.value) : new Date();
        this.selectedDate = initialDate;
        this.viewMonth = initialDate.getMonth();
        this.viewYear = initialDate.getFullYear();

        this.init();
    }

    WeeklyCalendar.prototype.init = function() {
        if (!this.trigger && !this.input) return;

        this.createPopup();
        this.bindEvents();
    };

    WeeklyCalendar.prototype.createPopup = function() {
        var self = this;
        var popup = document.createElement('div');
        popup.className = 'weekly-calendar-popover shadow-lg';
        popup.style.display = 'none';

        popup.innerHTML =
            '<div class="weekly-cal-header d-flex align-items-center justify-content-between p-3">' +
                '<button type="button" class="btn btn-sm btn-cal-nav prev-month-btn" aria-label="Previous month">&lsaquo;</button>' +
                '<div class="fw-bold cal-month-title text-espresso"></div>' +
                '<button type="button" class="btn btn-sm btn-cal-nav next-month-btn" aria-label="Next month">&rsaquo;</button>' +
            '</div>' +
            '<div class="weekly-cal-body px-3 pb-2">' +
                '<table class="weekly-cal-table w-100">' +
                    '<thead>' +
                        '<tr>' +
                            '<th class="th-week-num" title="Week of Year">Wk</th>' +
                            '<th>Mon</th><th>Tue</th><th>Wed</th><th>Thu</th><th>Fri</th><th>Sat</th><th>Sun</th>' +
                        '</tr>' +
                    '</thead>' +
                    '<tbody class="cal-days-tbody"></tbody>' +
                '</table>' +
            '</div>' +
            '<div class="weekly-cal-footer d-flex align-items-center justify-content-between px-3 py-2 border-top">' +
                '<button type="button" class="btn btn-sm btn-link text-decoration-none text-muted p-0 btn-cal-clear">Today</button>' +
                '<button type="button" class="btn btn-sm btn-boutique-pill btn-cal-this-week">This Week</button>' +
            '</div>';

        document.body.appendChild(popup);
        this.popup = popup;
        this.monthTitle = popup.querySelector('.cal-month-title');
        this.tbody = popup.querySelector('.cal-days-tbody');
    };

    WeeklyCalendar.prototype.render = function() {
        var self = this;
        var year = this.viewYear;
        var month = this.viewMonth;

        this.monthTitle.textContent = MONTH_NAMES[month] + ' ' + year;

        // Clear existing rows
        this.tbody.innerHTML = '';

        var firstDayOfMonth = new Date(year, month, 1);
        var lastDayOfMonth = new Date(year, month + 1, 0);

        // Find Monday of first week
        var currentMonday = getMonday(firstDayOfMonth);
        var today = new Date();
        today.setHours(0, 0, 0, 0);

        var selectedMonday = getMonday(this.selectedDate);
        var selectedMondayStr = formatDate(selectedMonday);

        // Render rows until we pass the last day of month
        var loopDate = new Date(currentMonday);
        var maxLoops = 6;
        var loopCount = 0;

        while (loopCount < maxLoops) {
            loopCount++;
            var weekNum = getWeekNumber(loopDate);
            var tr = document.createElement('tr');
            tr.className = 'week-row';

            var weekStartStr = formatDate(loopDate);
            tr.setAttribute('data-week-start', weekStartStr);

            var isSelectedWeek = (weekStartStr === selectedMondayStr);
            if (isSelectedWeek) {
                tr.classList.add('week-row-selected');
            }

            // Week Number Cell
            var tdWeek = document.createElement('td');
            tdWeek.className = 'td-week-num';
            tdWeek.textContent = weekNum;
            tr.appendChild(tdWeek);

            // 7 Days
            var hasAnyDayInCurrentMonth = false;
            for (var d = 0; d < 7; d++) {
                var cellDate = new Date(loopDate);
                var isCurrentMonth = (cellDate.getMonth() === month);
                if (isCurrentMonth) hasAnyDayInCurrentMonth = true;

                var isToday = (cellDate.getTime() === today.getTime());
                var isPast = (cellDate.getTime() < today.getTime());

                var tdDay = document.createElement('td');
                tdDay.className = 'td-day-cell';
                if (!isCurrentMonth) tdDay.classList.add('day-other-month');
                if (isToday) tdDay.classList.add('day-today');
                if (isPast) tdDay.classList.add('day-past');

                tdDay.textContent = cellDate.getDate();
                tdDay.setAttribute('data-date', formatDate(cellDate));

                tr.appendChild(tdDay);

                // Advance loopDate by 1 day
                loopDate.setDate(loopDate.getDate() + 1);
            }

            // Click row to select entire 7-day week
            (function(wsStr) {
                tr.addEventListener('click', function(e) {
                    self.selectWeek(wsStr);
                });

                // Row hover effect across all 7 days
                tr.addEventListener('mouseenter', function() {
                    tr.classList.add('week-row-hover');
                });
                tr.addEventListener('mouseleave', function() {
                    tr.classList.remove('week-row-hover');
                });
            })(weekStartStr);

            self.tbody.appendChild(tr);

            if (loopDate.getMonth() !== month && loopDate.getDate() > 7) {
                break;
            }
        }
    };

    WeeklyCalendar.prototype.selectWeek = function(weekStartStr) {
        var weekStartDate = parseDate(weekStartStr);
        this.selectedDate = weekStartDate;

        if (this.input) {
            this.input.value = weekStartStr;
            // Dispatch change event
            var event = new Event('change', { bubbles: true });
            this.input.dispatchEvent(event);
        }

        if (this.weekStartInput) {
            this.weekStartInput.value = weekStartStr;
        }

        if (typeof this.onSelect === 'function') {
            this.onSelect(weekStartStr, weekStartDate);
        }

        this.hide();
    };

    WeeklyCalendar.prototype.show = function() {
        var triggerEl = this.trigger || this.input;
        if (!triggerEl) return;

        // Position popup under trigger
        var rect = triggerEl.getBoundingClientRect();
        var top = rect.bottom + window.scrollY + 6;
        var left = rect.left + window.scrollX;

        // Prevent off-screen right
        var popWidth = 320;
        if (left + popWidth > window.innerWidth - 16) {
            left = window.innerWidth - popWidth - 16;
        }
        if (left < 16) left = 16;

        this.popup.style.top = top + 'px';
        this.popup.style.left = left + 'px';
        this.popup.style.display = 'block';

        this.render();
    };

    WeeklyCalendar.prototype.hide = function() {
        if (this.popup) {
            this.popup.style.display = 'none';
        }
    };

    WeeklyCalendar.prototype.bindEvents = function() {
        var self = this;

        var triggerEl = this.trigger || this.input;
        if (triggerEl) {
            triggerEl.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                if (self.popup.style.display === 'block') {
                    self.hide();
                } else {
                    self.show();
                }
            });
        }

        if (this.input && this.trigger !== this.input) {
            this.input.addEventListener('click', function(e) {
                e.preventDefault();
                e.stopPropagation();
                self.show();
            });
        }

        // Prev Month
        this.popup.querySelector('.prev-month-btn').addEventListener('click', function(e) {
            e.stopPropagation();
            self.viewMonth--;
            if (self.viewMonth < 0) {
                self.viewMonth = 11;
                self.viewYear--;
            }
            self.render();
        });

        // Next Month
        this.popup.querySelector('.next-month-btn').addEventListener('click', function(e) {
            e.stopPropagation();
            self.viewMonth++;
            if (self.viewMonth > 11) {
                self.viewMonth = 0;
                self.viewYear++;
            }
            self.render();
        });

        // Today / Clear button
        this.popup.querySelector('.btn-cal-clear').addEventListener('click', function(e) {
            e.stopPropagation();
            var today = new Date();
            self.viewMonth = today.getMonth();
            self.viewYear = today.getFullYear();
            self.selectWeek(formatDate(today));
        });

        // This Week button
        this.popup.querySelector('.btn-cal-this-week').addEventListener('click', function(e) {
            e.stopPropagation();
            var today = new Date();
            var mon = getMonday(today);
            self.viewMonth = mon.getMonth();
            self.viewYear = mon.getFullYear();
            self.selectWeek(formatDate(mon));
        });

        // Close on outside click
        document.addEventListener('click', function(e) {
            if (self.popup.style.display === 'block' &&
                !self.popup.contains(e.target) &&
                e.target !== triggerEl &&
                !triggerEl.contains(e.target)) {
                self.hide();
            }
        });

        // Close on Esc key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && self.popup.style.display === 'block') {
                self.hide();
            }
        });
    };

    window.WeeklyCalendar = WeeklyCalendar;

    // Auto-init for elements with data-toggle="weekly-calendar"
    function autoInitWeeklyCalendars() {
        var triggers = document.querySelectorAll('[data-toggle="weekly-calendar"]');
        triggers.forEach(function(el) {
            if (el._weeklyCalInitialized) return;
            el._weeklyCalInitialized = true;

            var targetInput = el.getAttribute('data-target-input');
            var weekStartInput = el.getAttribute('data-target-weekstart');
            var onSelectStr = el.getAttribute('data-onselect');

            new WeeklyCalendar({
                trigger: el,
                input: targetInput ? document.querySelector(targetInput) : (el.tagName === 'INPUT' ? el : null),
                weekStartInput: weekStartInput ? document.querySelector(weekStartInput) : null,
                onSelect: function(weekStartStr, weekStartDate) {
                    if (onSelectStr) {
                        try {
                            var fn = new Function('weekStartStr', 'weekStartDate', onSelectStr);
                            fn.call(el, weekStartStr, weekStartDate);
                        } catch(err) {
                            console.error('Error executing onSelect:', err);
                        }
                    }
                }
            });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', autoInitWeeklyCalendars);
    } else {
        autoInitWeeklyCalendars();
    }

})(window, document);
