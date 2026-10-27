/**
 * SmartStay Realtime WebSocket Client & Auto-Sync Layer
 * Connects to /ws/realtime with 3s auto-reconnect.
 */
(function () {
    'use strict';

    // V4: one socket per page even if the script is included twice
    if (window.__ssRealtimeLoaded) {
        return;
    }
    window.__ssRealtimeLoaded = true;

    let ws = null;
    let reconnectTimer = null;
    const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
    const host = window.location.host;
    // Derive context path from first segment or default
    const pathParts = window.location.pathname.split('/');
    const contextPath = pathParts.length > 1 && pathParts[1] ? '/' + pathParts[1] : '';
    const wsUrl = protocol + host + contextPath + '/ws/realtime';

    function showRealtimeToast(title, message, isSuccess = true) {
        let toastContainer = document.getElementById('smartstay-realtime-toast-container');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'smartstay-realtime-toast-container';
            toastContainer.setAttribute('role', 'status');
            toastContainer.setAttribute('aria-live', 'polite');
            toastContainer.style.cssText = 'position:fixed;bottom:24px;right:24px;z-index:9999;display:flex;flex-direction:column;gap:8px;max-width:360px;pointer-events:none;';
            document.body.appendChild(toastContainer);
        }

        const toast = document.createElement('div');
        toast.style.cssText = 'background:linear-gradient(135deg, #2B1B12 0%, #4A3326 100%);color:#FFFDF9;padding:12px 16px;border-radius:12px;border:1px solid #D9B991;box-shadow:0 8px 24px rgba(43,27,18,0.25);font-size:0.85rem;pointer-events:auto;transition:all 0.3s ease;transform:translateY(10px);opacity:0;';
        toast.innerHTML = `
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">
                <span style="color:#F5C542;"><i class="bi ${isSuccess ? 'bi-check-circle-fill' : 'bi-info-circle-fill'}"></i></span>
                <strong style="color:#FAF5EC;font-size:0.88rem;">${title}</strong>
            </div>
            <div style="color:#F0E4D3;font-size:0.8rem;line-height:1.4;">${message}</div>
        `;
        toastContainer.appendChild(toast);

        requestAnimationFrame(() => {
            toast.style.transform = 'translateY(0)';
            toast.style.opacity = '1';
        });

        setTimeout(() => {
            toast.style.transform = 'translateY(-10px)';
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 4000);
    }

    function connect() {
        try {
            ws = new WebSocket(wsUrl);

            ws.onopen = function () {
                console.log('[WebSocket] Connected to SmartStay Realtime Layer:', wsUrl);
                if (reconnectTimer) {
                    clearTimeout(reconnectTimer);
                    reconnectTimer = null;
                }
            };

            ws.onmessage = function (event) {
                try {
                    const data = JSON.parse(event.data);
                    console.log('[WebSocket] Received realtime event:', data);
                    handleRealtimeEvent(data);
                } catch (e) {
                    console.warn('[WebSocket] Malformed payload:', event.data);
                }
            };

            ws.onclose = function () {
                console.log('[WebSocket] Connection closed. Auto-reconnecting in 3s...');
                scheduleReconnect();
            };

            ws.onerror = function (err) {
                console.warn('[WebSocket] Connection error:', err);
                try { ws.close(); } catch (e) {}
            };
        } catch (e) {
            console.error('[WebSocket] Initialization failed:', e);
            scheduleReconnect();
        }
    }

    function scheduleReconnect() {
        if (!reconnectTimer) {
            reconnectTimer = setTimeout(connect, 3000);
        }
    }

    function handleRealtimeEvent(data) {
        if (!data || !data.event) return;

        // Dispatch custom DOM events for page observers (such as mockpay.js)
        try {
            window.dispatchEvent(new CustomEvent('smartstay:realtime', { detail: data }));
            if (data.event === 'PAYMENT_CONFIRMED' || data.event === 'PAYMENT_SUCCESS') {
                window.dispatchEvent(new CustomEvent('smartstay:PAYMENT_CONFIRMED', { detail: data }));
            } else if (data.event === 'PAYMENT_FAILED') {
                window.dispatchEvent(new CustomEvent('smartstay:PAYMENT_FAILED', { detail: data }));
            }
        } catch (evErr) {
            console.warn('[WebSocket] Event dispatch error:', evErr);
        }

        switch (data.event) {
            case 'BOOKING_CHECKED_IN':
            case 'BOOKING_UPDATED':
                const code = data.bookingCode || 'Booking';
                const rawStatus = data.status || 'CHECKED_IN';
                const label = formatBookingStatus(rawStatus);
                showRealtimeToast('Booking Status Updated', `${escapeHtml(code)} transitioned to: <strong>${escapeHtml(label)}</strong>`);
                
                // Only touch status badges belonging to this specific booking
                if (data.bookingCode && data.status) {
                    document.querySelectorAll('[data-status][data-booking-code]').forEach(el => {
                        if (el.getAttribute('data-booking-code') === data.bookingCode) {
                            el.textContent = label;
                            el.setAttribute('data-status', data.status);
                        }
                    });
                }
                break;

            case 'ROOM_STATUS_CHANGED':
                const roomNum = data.roomNumber || data.roomId;
                const newStatus = data.newStatus || 'AVAILABLE';
                showRealtimeToast('Room Status Changed', `Room ${roomNum} is now: <strong>${newStatus}</strong>`);
                break;

            case 'PAYMENT_CONFIRMED':
            case 'PAYMENT_SUCCESS':
                showRealtimeToast('Payment Confirmed', 'Payment received and door access credentials generated.');
                break;

            case 'PAYMENT_FAILED':
                showRealtimeToast('Payment Failed', 'Payment transaction failed or was cancelled.', false);
                break;

            case 'SYNC_COMPLETED':
                showRealtimeToast('Sync Completed', 'All background workers completed data synchronization.');
                break;

            case 'ALERT_UPDATED':
            case 'ALERT_CREATED':
                // R10: plain-words title from the server (util.AlertTitles), never the raw code
                const aTitle = escapeHtml(data.title || 'Security alert');
                const aStatusText = { NEW: 'open', ACKNOWLEDGED: 'acknowledged', RESOLVED: 'resolved' }[data.status] || 'updated';
                if (data.event === 'ALERT_CREATED') {
                    showRealtimeToast('New alert', `<strong>${aTitle}</strong>`, false);
                } else {
                    showRealtimeToast('Alert updated', `${aTitle} is now <strong>${aStatusText}</strong>`);
                }

                if (data.newAlertCount !== undefined) {
                    document.querySelectorAll('[data-alert-count]').forEach(function (b) {
                        b.textContent = data.newAlertCount;
                        b.style.display = data.newAlertCount > 0 ? '' : 'none';
                    });
                    const navBadge = document.getElementById('navAlertBadge');
                    if (navBadge) {
                        if (data.newAlertCount > 0) {
                            navBadge.textContent = data.newAlertCount;
                            navBadge.style.display = '';
                        } else {
                            navBadge.style.display = 'none';
                        }
                    }
                    const tabBadge = document.getElementById('tabAlertBadge');
                    if (tabBadge) {
                        if (data.newAlertCount > 0) {
                            tabBadge.textContent = data.newAlertCount + ' NEW';
                            tabBadge.style.display = '';
                        } else {
                            tabBadge.style.display = 'none';
                        }
                    }
                }
                break;
        }
    }

    function formatBookingStatus(raw) {
        if (!raw) return '';
        const map = {
            'PENDING_PAYMENT': 'Awaiting payment',
            'CONFIRMED': 'Confirmed',
            'CHECKED_IN': 'Checked in',
            'CHECKED_OUT': 'Completed',
            'CANCELLED': 'Cancelled',
            'EXPIRED': 'Expired'
        };
        const upper = String(raw).trim().toUpperCase();
        if (map[upper]) {
            return map[upper];
        }
        const words = upper.replace(/_/g, ' ').toLowerCase();
        return words.charAt(0).toUpperCase() + words.slice(1);
    }

    function escapeHtml(s) {
        return String(s).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }

    // R10: lets page scripts (alerts.js) reuse the same toast; pass plain text, it is escaped here
    window.ssRealtimeToast = function (title, text, isSuccess) {
        showRealtimeToast(escapeHtml(title), escapeHtml(text), isSuccess !== false);
    };

    // Expose manual sync trigger helper
    window.triggerManualSync = function (btn) {
        if (btn) {
            btn.disabled = true;
            const icon = btn.querySelector('.bi-arrow-repeat');
            if (icon) icon.classList.add('spin-animation');
            const text = btn.querySelector('#syncText');
            if (text) text.textContent = 'Syncing...';
        }

        fetch(contextPath + '/admin/sync-now', {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest',
                'Accept': 'application/json'
            }
        })
        .then(res => res.json())
        .then(data => {
            showRealtimeToast('Sync Triggered', data.message || 'Background workers are executing.');
        })
        .catch(err => {
            console.error('Manual sync failed:', err);
            showRealtimeToast('Notification', 'Sync request dispatched to server.');
        })
        .finally(() => {
            setTimeout(() => {
                if (btn) {
                    btn.disabled = false;
                    const icon = btn.querySelector('.bi-arrow-repeat');
                    if (icon) icon.classList.remove('spin-animation');
                    const text = btn.querySelector('#syncText');
                    if (text) text.textContent = 'Sync Now';
                }
            }, 2000);
        });
    };

    // Auto-init on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', connect);
    } else {
        connect();
    }
})();
