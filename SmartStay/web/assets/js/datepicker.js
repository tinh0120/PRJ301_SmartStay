/**
 * SmartStay - Flatpickr Datepicker Initialization (Offline 100%)
 * Enhances every <input type="date" data-datepicker> or <input data-datepicker>.
 * 
 * Rules:
 * 1. disableMobile: true (ensures consistent boutique calendar popup on all devices).
 * 2. altInput: true, altFormat: "d/m/Y" (visible input displays dd/MM/yyyy).
 * 3. dateFormat: "Y-m-d" (underlying submitted value stays yyyy-MM-dd so servlets remain unchanged).
 * 4. Preserves all existing input IDs, names, and event handlers.
 */
(function() {
    'use strict';

    function initFlatpickrInputs() {
        if (typeof flatpickr !== 'function') {
            return;
        }

        var elements = document.querySelectorAll('input[data-datepicker]');
        for (var i = 0; i < elements.length; i++) {
            var el = elements[i];
            if (el._flatpickr) {
                continue;
            }

            var config = {
                disableMobile: true,
                altInput: true,
                altFormat: 'd/m/Y',
                dateFormat: 'Y-m-d',
                allowInput: true,
                altInputClass: el.className + ' flatpickr-alt-input'
            };

            // Honor min and max constraints if present
            if (el.hasAttribute('min') && el.getAttribute('min')) {
                config.minDate = el.getAttribute('min');
            }
            if (el.hasAttribute('max') && el.getAttribute('max')) {
                config.maxDate = el.getAttribute('max');
            }

            // Honor initial value
            if (el.value) {
                config.defaultDate = el.value;
            }

            // Trigger change event on original input when date is chosen so existing listeners (e.g. auto-submit) fire
            config.onChange = function(selectedDates, dateStr, instance) {
                var event;
                if (typeof Event === 'function') {
                    event = new Event('change', { bubbles: true });
                } else {
                    event = document.createEvent('Event');
                    event.initEvent('change', true, true);
                }
                instance.input.dispatchEvent(event);
                if (typeof instance.input.onchange === 'function') {
                    instance.input.onchange();
                }
            };

            flatpickr(el, config);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initFlatpickrInputs);
    } else {
        initFlatpickrInputs();
    }

    window.SmartStayDatePicker = {
        init: initFlatpickrInputs
    };
})();
