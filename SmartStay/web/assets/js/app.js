/**
 * SmartStay Global Application Helpers (ES5)
 */
(function () {
    'use strict';

    var metaCtx = document.querySelector('meta[name="ctx"]');
    var contextPath = metaCtx ? metaCtx.getAttribute('content') : '';
    if (contextPath === null || contextPath === undefined) {
        contextPath = '';
    }

    window.SmartStay = {
        ctx: contextPath,
        url: function (path) {
            if (!path) {
                return contextPath;
            }
            if (path.charAt(0) !== '/') {
                path = '/' + path;
            }
            return contextPath + path;
        }
    };

    // Delegated click confirmation handler
    document.addEventListener('click', function (event) {
        var target = event.target;
        while (target && target !== document) {
            if (target.hasAttribute && target.hasAttribute('data-confirm')) {
                var message = target.getAttribute('data-confirm');
                if (message && !window.confirm(message)) {
                    event.preventDefault();
                    event.stopPropagation();
                    return;
                }
            }
            target = target.parentNode;
        }
    }, false);

    // Delegated password visibility toggle handler
    document.addEventListener('click', function (event) {
        var btn = event.target.closest ? event.target.closest('.toggle-password-btn, [data-toggle="password"]') : null;
        if (!btn) {
            return;
        }
        event.preventDefault();

        var input = null;
        var targetSelector = btn.getAttribute('data-target');
        if (targetSelector) {
            input = document.querySelector(targetSelector);
        }
        if (!input) {
            var group = btn.closest('.input-group');
            if (group) {
                input = group.querySelector('input');
            }
        }
        if (!input) {
            input = btn.previousElementSibling;
        }

        if (input) {
            var isPassword = input.type === 'password';
            input.type = isPassword ? 'text' : 'password';

            var eyeOpen = btn.querySelector('.eye-open');
            var eyeClosed = btn.querySelector('.eye-closed');
            if (eyeOpen && eyeClosed) {
                if (isPassword) {
                    eyeOpen.classList.add('d-none');
                    eyeClosed.classList.remove('d-none');
                    btn.setAttribute('aria-label', 'Hide password');
                    btn.setAttribute('title', 'Hide password');
                } else {
                    eyeOpen.classList.remove('d-none');
                    eyeClosed.classList.add('d-none');
                    btn.setAttribute('aria-label', 'Show password');
                    btn.setAttribute('title', 'Show password');
                }
            }
            if (typeof input.focus === 'function') {
                input.focus();
            }
        }
    }, false);

    // Initialize Bootstrap Tooltips
    function initTooltips() {
        if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
            var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.forEach(function (el) {
                if (!bootstrap.Tooltip.getInstance(el)) {
                    new bootstrap.Tooltip(el);
                }
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initTooltips);
    } else {
        initTooltips();
    }

    // Auto-dismiss flash alerts after 4.5 seconds
    setTimeout(function () {
        var alerts = document.querySelectorAll('.flash-alert');
        alerts.forEach(function (alertEl) {
            if (typeof bootstrap !== 'undefined' && bootstrap.Alert) {
                var bsAlert = bootstrap.Alert.getInstance(alertEl) || new bootstrap.Alert(alertEl);
                bsAlert.close();
            }
        });
    }, 4500);

    // Global submit loading & disabled state handler
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!form || form.tagName !== 'FORM') return;

        // Skip client-side invalid forms
        if (form.classList.contains('needs-validation') && form.checkValidity && !form.checkValidity()) {
            return;
        }

        // Find primary submit button
        var submitBtn = form.querySelector('button[type="submit"]:not([disabled])');
        if (!submitBtn) return;

        // Do not disable if form explicitly opts out with data-no-disable
        if (form.hasAttribute('data-no-disable') || submitBtn.hasAttribute('data-no-disable')) return;

        // Add spinner and disable after microtask so browser sends form data reliably
        setTimeout(function () {
            submitBtn.disabled = true;
            submitBtn.classList.add('disabled');
            var customLoadingText = submitBtn.getAttribute('data-loading-text');
            if (customLoadingText) {
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>' + customLoadingText;
            } else if (!submitBtn.querySelector('.spinner-border')) {
                var spinner = document.createElement('span');
                spinner.className = 'spinner-border spinner-border-sm me-1';
                spinner.setAttribute('role', 'status');
                spinner.setAttribute('aria-hidden', 'true');
                submitBtn.insertBefore(spinner, submitBtn.firstChild);
            }
        }, 10);
    }, false);

    // Re-enable disabled submit buttons on bfcache restore
    window.addEventListener('pageshow', function (event) {
        if (event.persisted) {
            document.querySelectorAll('button[type="submit"].disabled, button[type="submit"]:disabled').forEach(function (btn) {
                btn.disabled = false;
                btn.classList.remove('disabled');
                var sp = btn.querySelector('.spinner-border');
                if (sp) sp.remove();
            });
        }
    });
})();
