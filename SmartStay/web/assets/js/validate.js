/**
 * Client-side checks are a convenience only. The server validates everything again.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        // Support data-match matching validation
        var matchingInputs = document.querySelectorAll('input[data-match]');
        for (var i = 0; i < matchingInputs.length; i++) {
            (function (input) {
                var targetId = input.getAttribute('data-match');
                var targetInput = document.getElementById(targetId);
                var message = input.getAttribute('data-match-message') || 'Values do not match.';

                function validateMatch() {
                    if (!targetInput) {
                        return;
                    }
                    if (input.value && targetInput.value && input.value !== targetInput.value) {
                        input.setCustomValidity(message);
                    } else {
                        input.setCustomValidity('');
                    }
                }

                input.addEventListener('input', validateMatch, false);
                if (targetInput) {
                    targetInput.addEventListener('input', validateMatch, false);
                }
            })(matchingInputs[i]);
        }

        // Bootstrap form validation for .needs-validation forms
        var forms = document.querySelectorAll('form.needs-validation');
        for (var j = 0; j < forms.length; j++) {
            forms[j].addEventListener('submit', function (event) {
                if (!this.checkValidity()) {
                    event.preventDefault();
                    event.stopPropagation();
                    this.classList.add('was-validated');

                    var invalidFields = this.querySelectorAll('input:invalid, select:invalid, textarea:invalid');
                    for (var f = 0; f < invalidFields.length; f++) {
                        invalidFields[f].setAttribute('aria-invalid', 'true');
                    }

                    var firstInvalid = this.querySelector(':invalid');
                    if (firstInvalid && typeof firstInvalid.focus === 'function') {
                        firstInvalid.focus();
                    }
                }
            }, false);
        }

        // V4: each error text is read together with its field (server errors use .is-invalid)
        var feedbacks = document.querySelectorAll('.invalid-feedback');
        for (var k = 0; k < feedbacks.length; k++) {
            var fb = feedbacks[k];
            var field = fb.previousElementSibling;
            while (field && !field.matches('input, select, textarea') && !field.querySelector('input, select, textarea')) {
                field = field.previousElementSibling;
            }
            if (field && !field.matches('input, select, textarea')) {
                field = field.querySelector('input, select, textarea');
            }
            if (!field || field.type === 'hidden') {
                continue;
            }
            if (!fb.id) {
                fb.id = (field.id || ('ssField' + k)) + 'Error';
            }
            var described = field.getAttribute('aria-describedby') || '';
            if ((' ' + described + ' ').indexOf(' ' + fb.id + ' ') < 0) {
                field.setAttribute('aria-describedby', (described + ' ' + fb.id).trim());
            }
            if (field.classList.contains('is-invalid')) {
                field.setAttribute('aria-invalid', 'true');
            }
        }

        // Clear the flag once the browser considers the value valid again
        document.addEventListener('input', function (event) {
            var el = event.target;
            if (el && el.getAttribute && el.getAttribute('aria-invalid') === 'true'
                    && el.validity && el.validity.valid && !el.classList.contains('is-invalid')) {
                el.removeAttribute('aria-invalid');
            }
        }, false);
    });
})();
