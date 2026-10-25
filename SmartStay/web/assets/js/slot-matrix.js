/**
 * SmartStay - 7-Day Matrix Calendar Slot Picker & Sticky Summary Bar
 * Clean English UI with VND formatting, responsive mobile cards, and terms consent.
 */
(function() {
    'use strict';

    function initMatrix() {
        var container = document.getElementById('matrix-calendar');
        if (!container) return;

        var twoSlotsRate = parseFloat(container.getAttribute('data-rate-two')) || 10;
        var threeSlotsRate = parseFloat(container.getAttribute('data-rate-three')) || 20;
        var fourSlotsRate = parseFloat(container.getAttribute('data-rate-four')) || 50;

        var slotButtons = container.querySelectorAll('.matrix-slot-available');
        var form = document.getElementById('booking-form');
        var btnContinue = document.getElementById('btn-continue');
        var btnLogin = document.getElementById('btn-login-to-book');
        var slotDateInput = document.getElementById('bookingSlotDate');
        var slotCodesContainer = document.getElementById('bookingSlotCodes');

        // Sticky Summary Bar elements (Desktop)
        var emptyStateEl = document.getElementById('sticky-empty-state');
        var activeStateEl = document.getElementById('sticky-active-state');
        var dateSlotsEl = document.getElementById('sticky-date-slots');
        var slotBadgesEl = document.getElementById('sticky-slot-badges');
        var timeRangesEl = document.getElementById('sticky-time-ranges');
        var subtotalEl = document.getElementById('sticky-subtotal');
        var discountEl = document.getElementById('sticky-discount-badge');
        var totalEl = document.getElementById('sticky-total');
        var suggestionEl = document.getElementById('sticky-suggestion');
        var termsConsent = document.getElementById('termsConsent');
        var errorEl = document.getElementById('matrix-error');

        // Mobile Agoda-Style Bottom Bar & Drawer elements (< 992px)
        var mobileBar = document.getElementById('mobile-booking-bar');
        var mobileSlotsCount = document.getElementById('mobile-bar-slots-count');
        var mobileDiscount = document.getElementById('mobile-bar-discount');
        var mobilePrice = document.getElementById('mobile-bar-price');
        var mobileBarBookBtn = document.getElementById('mobileBarBookBtn');
        var mobileBarToggleBtn = document.getElementById('mobileBarToggleBtn');
        var mobileDrawer = document.getElementById('mobile-booking-drawer');
        var mobileBackdrop = document.getElementById('mobile-drawer-backdrop');
        var mobileDrawerClose = document.getElementById('mobileDrawerCloseBtn');
        var mobileDrawerDateSlots = document.getElementById('mobile-drawer-date-slots');
        var mobileDrawerSlotBadges = document.getElementById('mobile-drawer-slot-badges');
        var mobileDrawerChips = document.getElementById('mobile-drawer-chips');
        var mobileDrawerTimeRanges = document.getElementById('mobile-drawer-time-ranges');
        var mobileDrawerSubtotal = document.getElementById('mobile-drawer-subtotal');
        var mobileDrawerDiscount = document.getElementById('mobile-drawer-discount-badge');
        var mobileDrawerDiscountRule = document.getElementById('mobile-drawer-discount-rule');
        var mobileDrawerTotal = document.getElementById('mobile-drawer-total');
        var mobileDrawerSuggestion = document.getElementById('mobile-drawer-suggestion');
        var mobileTermsConsent = document.getElementById('mobileTermsConsent');
        var btnMobileContinue = document.getElementById('btn-mobile-continue');

        // Mobile day switcher pills
        var dayPills = container.querySelectorAll('.mobile-day-pill');
        var dayGrids = container.querySelectorAll('.mobile-day-grid');

        var activeDate = null;
        var activeDateFormatted = '';
        var selectedSlots = []; // array of { date, dateFormatted, code, name, time, sort, price, priceFormatted, btn }

        function formatMoney(num) {
            return (num || 0).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',') + ' VND';
        }

        function syncButtonsVisuals(date, code, isSelected) {
            var matchingButtons = container.querySelectorAll('.matrix-slot-available[data-date="' + date + '"][data-code="' + code + '"]');
            matchingButtons.forEach(function(btn) {
                if (isSelected) {
                    btn.classList.add('matrix-slot-selected');
                    // Desktop cell
                    if (!btn.classList.contains('mobile-slot-card')) {
                        var selSpan = btn.querySelector('.matrix-slot-status-selected');
                        if (!selSpan) {
                            selSpan = document.createElement('span');
                            selSpan.className = 'matrix-slot-status-selected';
                            selSpan.innerHTML = '<i class="bi bi-check-circle-fill"></i> Selected';
                            btn.appendChild(selSpan);
                        }
                    } else {
                        // Mobile card
                        var statusBadge = btn.querySelector('.card-slot-badge-status');
                        if (statusBadge) {
                            statusBadge.className = 'card-slot-badge-status badge-status-selected';
                            statusBadge.innerHTML = '<i class="bi bi-check-circle-fill me-1"></i>Selected';
                        }
                    }
                } else {
                    btn.classList.remove('matrix-slot-selected');
                    var selSpan = btn.querySelector('.matrix-slot-status-selected');
                    if (selSpan) selSpan.remove();
                    var statusBadge = btn.querySelector('.card-slot-badge-status');
                    if (statusBadge) {
                        statusBadge.className = 'card-slot-badge-status badge-status-avail';
                        statusBadge.textContent = 'Available';
                    }
                }
            });
        }

        function clearAllSelections() {
            var allButtons = container.querySelectorAll('.matrix-slot-available');
            allButtons.forEach(function(btn) {
                btn.classList.remove('matrix-slot-selected');
                var selSpan = btn.querySelector('.matrix-slot-status-selected');
                if (selSpan) selSpan.remove();
                var statusBadge = btn.querySelector('.card-slot-badge-status');
                if (statusBadge) {
                    statusBadge.className = 'card-slot-badge-status badge-status-avail';
                    statusBadge.textContent = 'Available';
                }
            });
            selectedSlots = [];
            activeDate = null;
            activeDateFormatted = '';
        }

        function areConsecutive(slots) {
            if (slots.length <= 1) return true;
            var sorted = slots.slice().sort(function(a, b) {
                return a.sort - b.sort;
            });
            for (var i = 1; i < sorted.length; i++) {
                if (sorted[i].sort !== sorted[i - 1].sort + 1) {
                    return false;
                }
            }
            return true;
        }

        function isSelectionValid() {
            return selectedSlots.length > 0 && areConsecutive(selectedSlots);
        }

        function keepFirstContiguousGroup() {
            if (selectedSlots.length <= 1) return;
            selectedSlots.sort(function(a, b) { return a.sort - b.sort; });
            var kept = [selectedSlots[0]];
            for (var i = 1; i < selectedSlots.length; i++) {
                if (selectedSlots[i].sort === kept[kept.length - 1].sort + 1) {
                    kept.push(selectedSlots[i]);
                } else {
                    break;
                }
            }
            // All slots not in kept must be unselected on UI
            selectedSlots.forEach(function(s) {
                var inKept = false;
                for (var j = 0; j < kept.length; j++) {
                    if (kept[j].code === s.code) { inKept = true; break; }
                }
                if (!inKept) {
                    syncButtonsVisuals(s.date || activeDate, s.code, false);
                }
            });
            selectedSlots = kept;
            updateUI();
        }

        function showMatrixToast(title, message, options) {
            options = options || {};
            var container = document.getElementById('matrix-toast-container');
            if (!container) {
                container = document.createElement('div');
                container.id = 'matrix-toast-container';
                container.className = 'matrix-toast-container';
                document.body.appendChild(container);
            }

            // Clean up any existing toast to prevent clutter
            container.innerHTML = '';

            var toast = document.createElement('div');
            toast.className = 'matrix-toast';
            toast.setAttribute('role', 'alert');

            var iconHtml = options.icon || '<i class="bi bi-exclamation-triangle-fill text-warning"></i>';

            var actionsHtml = '';
            if (options.showFixBtn) {
                actionsHtml += '<button type="button" class="matrix-toast-btn-fix"><i class="bi bi-scissors me-1"></i>Keep Adjacent Slots</button>';
            }
            actionsHtml += '<button type="button" class="matrix-toast-btn-dismiss">Got it</button>';

            toast.innerHTML =
                '<div class="matrix-toast-icon-wrap">' + iconHtml + '</div>' +
                '<div class="matrix-toast-content">' +
                    '<div class="matrix-toast-title">' + title + '</div>' +
                    '<div class="matrix-toast-message">' + message + '</div>' +
                    '<div class="matrix-toast-actions">' + actionsHtml + '</div>' +
                '</div>' +
                '<button type="button" class="matrix-toast-close" title="Close">&times;</button>';

            container.appendChild(toast);

            var timer = null;

            function dismiss() {
                if (timer) clearTimeout(timer);
                toast.classList.add('toast-hiding');
                setTimeout(function() {
                    if (toast && toast.parentNode) {
                        toast.parentNode.removeChild(toast);
                    }
                }, 260);
            }

            var closeBtn = toast.querySelector('.matrix-toast-close');
            if (closeBtn) closeBtn.addEventListener('click', dismiss);

            var dismissBtn = toast.querySelector('.matrix-toast-btn-dismiss');
            if (dismissBtn) dismissBtn.addEventListener('click', dismiss);

            var fixBtn = toast.querySelector('.matrix-toast-btn-fix');
            if (fixBtn) {
                fixBtn.addEventListener('click', function(e) {
                    e.preventDefault();
                    dismiss();
                    keepFirstContiguousGroup();
                });
            }

            timer = setTimeout(dismiss, 5000);
        }

        function updateButtonState() {
            var consecutive = areConsecutive(selectedSlots);

            if (btnContinue) {
                if (selectedSlots.length === 0) {
                    btnContinue.disabled = true;
                    btnContinue.classList.remove('btn-invalid-slots');
                    btnContinue.innerHTML = '<span>Continue to Payment</span> <i class="bi bi-arrow-right"></i>';
                } else if (!consecutive) {
                    btnContinue.disabled = true;
                    btnContinue.classList.add('btn-invalid-slots');
                    btnContinue.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-1"></i><span>Non-Consecutive Slots</span>';
                } else {
                    btnContinue.classList.remove('btn-invalid-slots');
                    btnContinue.innerHTML = '<span>Continue to Payment</span> <i class="bi bi-arrow-right"></i>';
                    if (termsConsent) {
                        btnContinue.disabled = !termsConsent.checked;
                    } else {
                        btnContinue.disabled = false;
                    }
                }
            }

            if (btnMobileContinue) {
                if (selectedSlots.length === 0) {
                    btnMobileContinue.disabled = true;
                    btnMobileContinue.classList.remove('btn-invalid-slots');
                    btnMobileContinue.innerHTML = '<span>Continue to Payment</span> <i class="bi bi-arrow-right"></i>';
                } else if (!consecutive) {
                    btnMobileContinue.disabled = true;
                    btnMobileContinue.classList.add('btn-invalid-slots');
                    btnMobileContinue.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-1"></i><span>Non-Consecutive Slots</span>';
                } else {
                    btnMobileContinue.classList.remove('btn-invalid-slots');
                    btnMobileContinue.innerHTML = '<span>Continue to Payment</span> <i class="bi bi-arrow-right"></i>';
                    var isConsentChecked = (mobileTermsConsent && mobileTermsConsent.checked) || (termsConsent && termsConsent.checked);
                    btnMobileContinue.disabled = !isConsentChecked;
                }
            }

            if (btnLogin) {
                if (selectedSlots.length > 0 && !consecutive) {
                    btnLogin.classList.add('disabled', 'btn-invalid-slots');
                    btnLogin.setAttribute('aria-disabled', 'true');
                    btnLogin.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-1"></i><span>Non-Consecutive Slots</span>';
                    btnLogin.style.pointerEvents = 'none';
                } else {
                    btnLogin.classList.remove('disabled', 'btn-invalid-slots');
                    btnLogin.removeAttribute('aria-disabled');
                    btnLogin.innerHTML = '<span>Login to Book</span> <i class="bi bi-arrow-right"></i>';
                    btnLogin.style.pointerEvents = 'auto';
                }
            }
        }

        function updateUI() {
            if (selectedSlots.length === 0) {
                activeDate = null;
                activeDateFormatted = '';
                if (emptyStateEl) emptyStateEl.classList.remove('d-none');
                if (activeStateEl) activeStateEl.classList.add('d-none');
                if (mobileBar) mobileBar.classList.add('d-none');
                if (mobileDrawer) {
                    mobileDrawer.classList.remove('is-open');
                    if (mobileBackdrop) mobileBackdrop.classList.remove('is-open');
                    document.body.style.overflow = '';
                }
                if (slotCodesContainer) slotCodesContainer.innerHTML = '';
                if (errorEl) {
                    errorEl.classList.add('d-none');
                    errorEl.textContent = '';
                }
                updateButtonState();
                return;
            }

            if (emptyStateEl) emptyStateEl.classList.add('d-none');
            if (activeStateEl) activeStateEl.classList.remove('d-none');
            if (mobileBar) mobileBar.classList.remove('d-none');

            // Calculate totals
            var subtotal = 0;
            var slotCodes = [];
            var timeRanges = [];
            selectedSlots.sort(function(a, b) { return a.sort - b.sort; });

            selectedSlots.forEach(function(s) {
                subtotal += s.price;
                slotCodes.push(s.code);
                timeRanges.push(s.time);
            });

            // Update Left: Date & Slots
            if (dateSlotsEl) {
                if (slotBadgesEl) {
                    dateSlotsEl.textContent = activeDateFormatted;
                    slotBadgesEl.textContent = slotCodes.join(' + ');
                } else {
                    dateSlotsEl.textContent = activeDateFormatted + ' \u00B7 ' + slotCodes.join(' + ');
                }
            }
            if (timeRangesEl) {
                var formattedTimes = timeRanges.map(function(t) {
                    return t;
                });
                timeRangesEl.innerHTML = '<i class="bi bi-clock-history me-1 text-caramel"></i>' + formattedTimes.join(' &middot; ');
            }

            // Sync form hidden inputs
            if (slotDateInput) {
                slotDateInput.value = activeDate;
            }
            if (slotCodesContainer) {
                var inputsHtml = '';
                selectedSlots.forEach(function(s) {
                    inputsHtml += '<input type="hidden" name="slotCodes" value="' + s.code + '">';
                });
                slotCodesContainer.innerHTML = inputsHtml;
            }

            var consecutive = areConsecutive(selectedSlots);

            if (!consecutive) {
                // Non-consecutive slots selected: NO DISCOUNT & NO PAYMENT ALLOWED!
                if (subtotalEl) subtotalEl.classList.add('d-none');
                if (discountEl) discountEl.classList.add('d-none');
                if (totalEl) totalEl.textContent = formatMoney(subtotal);

                if (mobilePrice) mobilePrice.textContent = formatMoney(subtotal);
                if (mobileSlotsCount) mobileSlotsCount.textContent = selectedSlots.length + (selectedSlots.length === 1 ? ' Slot' : ' Slots');
                if (mobileDiscount) mobileDiscount.classList.add('d-none');
                if (mobileDrawerTotal) mobileDrawerTotal.textContent = formatMoney(subtotal);
                if (mobileDrawerSubtotal) mobileDrawerSubtotal.classList.add('d-none');
                if (mobileDrawerDiscount) mobileDrawerDiscount.classList.add('d-none');

                if (suggestionEl) {
                    suggestionEl.innerHTML = '<div class="text-danger fw-bold"><i class="bi bi-exclamation-triangle-fill me-1"></i>Time slots must be consecutive!</div>' +
                        '<div class="mt-1" style="font-size:0.73rem;">' +
                        '<a href="javascript:void(0)" class="text-danger fw-bold text-decoration-underline btn-fix-consecutive">' +
                        '<i class="bi bi-scissors me-1"></i>Remove disconnected slots' +
                        '</a></div>';
                    if (mobileDrawerSuggestion) mobileDrawerSuggestion.innerHTML = suggestionEl.innerHTML;
                }

                if (errorEl) {
                    errorEl.className = 'alert alert-danger small py-2 my-3';
                    errorEl.innerHTML = '<div class="d-flex align-items-center justify-content-between flex-wrap gap-2">' +
                        '<span><i class="bi bi-exclamation-triangle-fill me-2"></i><strong>Booking Rule:</strong> Selected time slots are not consecutive on the same day. Please select adjacent slots to continue booking.</span>' +
                        '<button type="button" class="btn btn-outline-danger btn-sm py-0 px-2 rounded-pill btn-fix-consecutive" style="font-size:0.75rem;"><i class="bi bi-scissors me-1"></i>Keep adjacent slots only</button>' +
                        '</div>';
                    errorEl.classList.remove('d-none');
                }

                document.querySelectorAll('.btn-fix-consecutive').forEach(function(b) {
                    b.addEventListener('click', function(ev) {
                        ev.preventDefault();
                        ev.stopPropagation();
                        keepFirstContiguousGroup();
                    });
                });

                updateButtonState();
                return;
            }

            // Normal valid consecutive slots
            if (errorEl) {
                errorEl.classList.add('d-none');
                errorEl.textContent = '';
            }

            var discountPercent = 0;
            if (selectedSlots.length === 2) discountPercent = twoSlotsRate;
            else if (selectedSlots.length === 3) discountPercent = threeSlotsRate;
            else if (selectedSlots.length >= 4) discountPercent = fourSlotsRate;

            var discountAmount = Math.round((subtotal * discountPercent) / 100);
            var total = subtotal - discountAmount;

            // Update Middle: Prices
            if (subtotalEl) {
                if (discountPercent > 0) {
                    subtotalEl.classList.remove('d-none');
                    subtotalEl.textContent = formatMoney(subtotal);
                } else {
                    subtotalEl.classList.add('d-none');
                }
            }
            if (discountEl) {
                if (discountPercent > 0) {
                    discountEl.classList.remove('d-none');
                    discountEl.textContent = '-' + discountPercent + '%';
                } else {
                    discountEl.classList.add('d-none');
                }
            }
            if (totalEl) {
                totalEl.textContent = formatMoney(total);
            }

            // Sync Mobile Bottom Bar
            if (mobilePrice) mobilePrice.textContent = formatMoney(total);
            if (mobileSlotsCount) mobileSlotsCount.textContent = selectedSlots.length + (selectedSlots.length === 1 ? ' slot' : ' slots');
            if (mobileDiscount) {
                if (discountPercent > 0) {
                    mobileDiscount.classList.remove('d-none');
                    mobileDiscount.textContent = '-' + discountPercent + '%';
                } else {
                    mobileDiscount.classList.add('d-none');
                }
            }

            // Sync Mobile Drawer Breakdown
            if (mobileDrawerDateSlots) mobileDrawerDateSlots.textContent = activeDateFormatted;
            if (mobileDrawerSlotBadges) mobileDrawerSlotBadges.textContent = slotCodes.join(' + ');
            if (mobileDrawerChips) {
                mobileDrawerChips.innerHTML = selectedSlots.map(function(s) {
                    return '<span class="badge rounded-pill bg-sand text-espresso border border-latte py-1 px-2" style="font-size:0.75rem;">' +
                           (s.dateFormatted || activeDateFormatted) + ' &middot; ' + s.code +
                           '</span>';
                }).join('');
            }
            if (mobileDrawerTimeRanges) mobileDrawerTimeRanges.innerHTML = '<i class="bi bi-clock-history me-1 text-caramel"></i>' + timeRanges.join(' &middot; ');
            if (mobileDrawerSubtotal) {
                if (discountPercent > 0) {
                    mobileDrawerSubtotal.classList.remove('d-none');
                    mobileDrawerSubtotal.textContent = formatMoney(subtotal);
                } else {
                    mobileDrawerSubtotal.classList.add('d-none');
                }
            }
            if (mobileDrawerDiscount) {
                if (discountPercent > 0) {
                    mobileDrawerDiscount.classList.remove('d-none');
                    mobileDrawerDiscount.textContent = '-' + discountPercent + '%';
                } else {
                    mobileDrawerDiscount.classList.add('d-none');
                }
            }
            if (mobileDrawerDiscountRule) {
                if (discountPercent > 0) {
                    var ruleText = '';
                    if (selectedSlots.length === 2) ruleText = '2 slots: ' + twoSlotsRate + '% off';
                    else if (selectedSlots.length === 3) ruleText = '3 slots: ' + threeSlotsRate + '% off';
                    else if (selectedSlots.length >= 4) ruleText = 'Full day: ' + fourSlotsRate + '% off';
                    mobileDrawerDiscountRule.textContent = '(' + ruleText + ')';
                    mobileDrawerDiscountRule.classList.remove('d-none');
                } else {
                    mobileDrawerDiscountRule.classList.add('d-none');
                }
            }
            if (mobileDrawerTotal) mobileDrawerTotal.textContent = formatMoney(total);

            // Update Dynamic Suggestion / Upsell Line
            if (suggestionEl) {
                if (selectedSlots.length === 1) {
                    suggestionEl.innerHTML = '<i class="bi bi-lightning-charge-fill me-1"></i>Select 1 more consecutive slot for ' + twoSlotsRate + '% off';
                } else if (selectedSlots.length === 2) {
                    suggestionEl.innerHTML = '<i class="bi bi-lightning-charge-fill me-1"></i>Select 1 more consecutive slot for ' + threeSlotsRate + '% off';
                } else if (selectedSlots.length === 3) {
                    suggestionEl.innerHTML = '<i class="bi bi-lightning-charge-fill me-1"></i>Select 1 more slot (full 24h) for ' + fourSlotsRate + '% off';
                } else if (selectedSlots.length >= 4) {
                    suggestionEl.innerHTML = '<i class="bi bi-trophy-fill text-warning me-1"></i>Maximum ' + fourSlotsRate + '% discount applied for full day!';
                }
                if (mobileDrawerSuggestion) {
                    mobileDrawerSuggestion.innerHTML = suggestionEl.innerHTML;
                }
            }

            updateButtonState();
        }

        // Mobile Drawer Controller functions
        function openMobileDrawer() {
            if (mobileDrawer) {
                mobileDrawer.classList.add('is-open');
                if (mobileBackdrop) mobileBackdrop.classList.add('is-open');
                document.body.style.overflow = 'hidden';
            }
        }

        function closeMobileDrawer() {
            if (mobileDrawer) {
                mobileDrawer.classList.remove('is-open');
                if (mobileBackdrop) mobileBackdrop.classList.remove('is-open');
                document.body.style.overflow = '';
            }
        }

        if (mobileBarToggleBtn) {
            mobileBarToggleBtn.addEventListener('click', function(e) {
                e.preventDefault();
                if (mobileDrawer && mobileDrawer.classList.contains('is-open')) {
                    closeMobileDrawer();
                } else {
                    openMobileDrawer();
                }
            });
        }

        if (mobileBarBookBtn) {
            mobileBarBookBtn.addEventListener('click', function(e) {
                e.preventDefault();
                openMobileDrawer();
            });
        }

        if (mobileDrawerClose) {
            mobileDrawerClose.addEventListener('click', closeMobileDrawer);
        }

        if (mobileBackdrop) {
            mobileBackdrop.addEventListener('click', closeMobileDrawer);
        }

        // Terms consent synchronization
        if (termsConsent && mobileTermsConsent) {
            termsConsent.addEventListener('change', function() {
                mobileTermsConsent.checked = termsConsent.checked;
                updateButtonState();
            });
            mobileTermsConsent.addEventListener('change', function() {
                termsConsent.checked = mobileTermsConsent.checked;
                updateButtonState();
            });
        } else if (termsConsent) {
            termsConsent.addEventListener('change', updateButtonState);
        } else if (mobileTermsConsent) {
            mobileTermsConsent.addEventListener('change', updateButtonState);
        }

        if (btnContinue) {
            btnContinue.addEventListener('click', function(e) {
                if (!isSelectionValid()) {
                    e.preventDefault();
                    showMatrixToast(
                        'Cannot Proceed',
                        'You have selected non-consecutive time slots. Please select adjacent slots to continue.',
                        { showFixBtn: true, icon: '<i class="bi bi-x-circle-fill text-danger"></i>' }
                    );
                    return;
                }
                if (btnContinue) {
                    btnContinue.disabled = true;
                    btnContinue.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span><span>Processing booking...</span>';
                }
                if (form) form.submit();
            });
        }

        if (btnMobileContinue) {
            btnMobileContinue.addEventListener('click', function(e) {
                if (!isSelectionValid()) {
                    e.preventDefault();
                    showMatrixToast(
                        'Cannot Proceed',
                        'You have selected non-consecutive time slots. Please select adjacent slots to continue.',
                        { showFixBtn: true, icon: '<i class="bi bi-x-circle-fill text-danger"></i>' }
                    );
                    return;
                }
                if (btnMobileContinue) {
                    btnMobileContinue.disabled = true;
                    btnMobileContinue.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span><span>Processing...</span>';
                }
                if (form) form.submit();
            });
        }

        if (form) {
            form.addEventListener('submit', function(e) {
                if (!isSelectionValid()) {
                    e.preventDefault();
                    showMatrixToast(
                        'Booking Error',
                        'Selected time slots must be consecutive on the same day. Please select adjacent slots.',
                        { showFixBtn: true, icon: '<i class="bi bi-exclamation-triangle-fill text-danger"></i>' }
                    );
                    if (errorEl) {
                        errorEl.className = 'alert alert-danger small py-2 mb-3';
                        errorEl.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-2"></i><strong>Booking Error:</strong> Selected time slots must be consecutive on the same day. Please select adjacent slots.';
                        errorEl.classList.remove('d-none');
                        errorEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }
                    return false;
                }
            });
        }

        // Mobile day pill switcher
        if (dayPills && dayPills.length > 0) {
            dayPills.forEach(function(pill) {
                pill.addEventListener('click', function(e) {
                    e.preventDefault();
                    var targetDate = pill.getAttribute('data-target-date');
                    dayPills.forEach(function(p) { p.classList.remove('active'); });
                    pill.classList.add('active');
                    if (dayGrids) {
                        dayGrids.forEach(function(grid) {
                            if (grid.id === 'mobile-day-' + targetDate) {
                                grid.classList.remove('d-none');
                            } else {
                                grid.classList.add('d-none');
                            }
                        });
                    }
                });
            });
        }

        slotButtons.forEach(function(btn) {
            btn.addEventListener('click', function(e) {
                e.preventDefault();
                var date = btn.getAttribute('data-date');
                var dateFormatted = btn.getAttribute('data-date-formatted');
                var code = btn.getAttribute('data-code');
                var name = btn.getAttribute('data-name');
                var time = btn.getAttribute('data-time');
                var sort = parseInt(btn.getAttribute('data-sort'), 10) || 1;
                var price = parseFloat(btn.getAttribute('data-price')) || 0;
                var priceFormatted = btn.getAttribute('data-price-formatted');

                // If clicking a slot on a different day, reset and start on this day
                if (activeDate !== date) {
                    clearAllSelections();
                    activeDate = date;
                    activeDateFormatted = dateFormatted;
                }

                // Switch mobile day pill & grid if needed
                if (dayPills && dayPills.length > 0) {
                    dayPills.forEach(function(p) {
                        if (p.getAttribute('data-target-date') === date) {
                            p.classList.add('active');
                            p.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
                        } else {
                            p.classList.remove('active');
                        }
                    });
                    if (dayGrids) {
                        dayGrids.forEach(function(grid) {
                            if (grid.id === 'mobile-day-' + date) {
                                grid.classList.remove('d-none');
                            } else {
                                grid.classList.add('d-none');
                            }
                        });
                    }
                }

                // Check if already selected
                var existingIndex = -1;
                for (var i = 0; i < selectedSlots.length; i++) {
                    if (selectedSlots[i].code === code) {
                        existingIndex = i;
                        break;
                    }
                }

                if (existingIndex >= 0) {
                    // Deselect
                    selectedSlots.splice(existingIndex, 1);
                    syncButtonsVisuals(date, code, false);

                    // If deselecting left a non-consecutive gap
                    if (!areConsecutive(selectedSlots) && selectedSlots.length > 1) {
                        showMatrixToast(
                            'Non-Consecutive Slots',
                            'You deselected a middle slot, breaking the consecutive sequence.<br>' +
                            'Please select <strong>adjacent slots</strong> to proceed to payment.',
                            {
                                showFixBtn: true,
                                icon: '<i class="bi bi-exclamation-octagon-fill text-danger"></i>'
                            }
                        );
                    }
                } else {
                    // Tentatively add
                    var candidate = {
                        date: date,
                        dateFormatted: dateFormatted,
                        code: code,
                        name: name,
                        time: time,
                        sort: sort,
                        price: price,
                        priceFormatted: priceFormatted,
                        btn: btn
                    };
                    var testList = selectedSlots.concat([candidate]);

                    if (areConsecutive(testList)) {
                        selectedSlots.push(candidate);
                        syncButtonsVisuals(date, code, true);
                    } else {
                        // Not consecutive: replace selection with this single slot
                        clearAllSelections();
                        selectedSlots = [candidate];
                        syncButtonsVisuals(date, code, true);

                        updateUI();

                        // Immediate visual feedback right on the clicked button
                        btn.classList.add('slot-shake-effect');
                        setTimeout(function() {
                            btn.classList.remove('slot-shake-effect');
                        }, 450);

                        // Pop up the top-center floating notification right in front of user
                        showMatrixToast(
                            'Booking Policy',
                            'Only <strong>consecutive time slots</strong> on the same day can be booked.<br>' +
                            'Switched selection to: <strong>' + name + '</strong> (' + time + ').',
                            {
                                icon: '<i class="bi bi-exclamation-triangle-fill text-warning"></i>'
                            }
                        );

                        if (errorEl) {
                            errorEl.className = 'alert alert-warning small py-2 mb-3';
                            errorEl.innerHTML = '<i class="bi bi-info-circle-fill me-2"></i>Only consecutive time slots can be booked. Switched to <strong>' + name + '</strong>.';
                            errorEl.classList.remove('d-none');
                        }
                        return;
                    }
                }

                updateUI();
            });
        });

        // Initialize Bootstrap Popovers for special offers
        try {
            if (window.bootstrap && typeof window.bootstrap.Popover === 'function') {
                var popovers = document.querySelectorAll('.btn-discount-popover');
                popovers.forEach(function(el) {
                    new window.bootstrap.Popover(el, {
                        html: true,
                        trigger: 'focus'
                    });
                });
            }
        } catch (err) {
            console.error('Error initializing popovers', err);
        }

        // Reviews Toggle 'View all'
        var btnToggle = document.getElementById('btnToggleReviews');
        var extraRev = document.getElementById('extraReviews');
        if (btnToggle && extraRev) {
            var count = btnToggle.getAttribute('data-count') || '';
            btnToggle.addEventListener('click', function() {
                if (extraRev.classList.contains('d-none')) {
                    extraRev.classList.remove('d-none');
                    btnToggle.innerHTML = '<i class="bi bi-chevron-up me-1"></i>Show less';
                } else {
                    extraRev.classList.add('d-none');
                    btnToggle.innerHTML = 'View all (' + count + ') &rarr;';
                }
            });
        }

        // Desktop Booking Summary is cleanly placed in right column (position: sticky)
        // Mobile Booking Bar is fixed at bottom (position: fixed; bottom: 0)
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initMatrix);
    } else {
        initMatrix();
    }
})();
