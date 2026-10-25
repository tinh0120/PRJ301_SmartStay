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

                    var firstInvalid = this.querySelector(':invalid');
                    if (firstInvalid && typeof firstInvalid.focus === 'function') {
                        firstInvalid.focus();
                    }
                }
            }, false);
        }
    });
})();
