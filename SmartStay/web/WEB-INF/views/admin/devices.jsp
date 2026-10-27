<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Devices" active="devices">
    <%-- R9: summary line, rows grouped by zone, status dot + text, one visible action (Edit);
         API key and Delete live in the ⋯ menu and still open the existing confirm modals. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Devices</h1>
            <p class="ss-page-header__subtitle ss-dev-summary">
                <span class="${deviceCounts['FAULT'] > 0 ? 'ss-dev-summary__bad' : ''}">${deviceCounts['FAULT']} fault</span>
                <span aria-hidden="true">&middot;</span>
                <span class="${deviceCounts['OFFLINE'] > 0 ? 'ss-dev-summary__warn' : ''}">${deviceCounts['OFFLINE']} offline</span>
                <span aria-hidden="true">&middot;</span>
                <span>${deviceCounts['ONLINE']} online</span>
            </p>
            <%-- Confirmation pushed by the lock (realtime DEVICE_ACK) after a remote Unlock / Silence --%>
            <p class="ss-dev-ack" id="deviceAck" role="status" aria-live="polite" hidden></p>
        </div>
        <a href="${pageContext.request.contextPath}/admin/devices/edit" class="btn btn-primary btn-sm rounded-pill px-3">
            <i class="bi bi-plus-lg me-1" aria-hidden="true"></i>New device
        </a>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Toolbar: search + status chips -->
    <form action="${pageContext.request.contextPath}/admin/devices" method="get" class="ss-toolbar">
        <input type="hidden" name="status" value="${e:esc(pr.getFilter('status'))}">
        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search devices</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword"
                       placeholder="Device name, ID or zone" maxlength="100" value="${e:esc(pr.keyword)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Device status">
                <a class="ss-chip ${empty pr.getFilter('status') ? 'ss-chip--active' : ''}" href="${pageContext.request.contextPath}/admin/devices">All</a>
                <a class="ss-chip ${pr.getFilter('status') == 'FAULT' ? 'ss-chip--active' : ''}" href="${pageContext.request.contextPath}/admin/devices?status=FAULT">Fault</a>
                <a class="ss-chip ${pr.getFilter('status') == 'OFFLINE' ? 'ss-chip--active' : ''}" href="${pageContext.request.contextPath}/admin/devices?status=OFFLINE">Offline</a>
                <a class="ss-chip ${pr.getFilter('status') == 'ONLINE' ? 'ss-chip--active' : ''}" href="${pageContext.request.contextPath}/admin/devices?status=ONLINE">Online</a>
            </div>
        </div>
    </form>

    <div class="ss-dev-table">
        <t:if test="${empty result.items && empty dbError}">
            <div class="ss-empty">
                <i class="bi bi-cpu ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No devices match</p>
                <p class="ss-empty__desc">Choose another status or clear the search.</p>
                <a href="${pageContext.request.contextPath}/admin/devices" class="btn btn-outline-soft btn-sm rounded-pill px-3">Show all devices</a>
            </div>
        </t:if>
        <t:if test="${not empty result.items}">
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3">Name</th>
                            <th scope="col">Type</th>
                            <th scope="col">Status</th>
                            <th scope="col">Last seen</th>
                            <th scope="col">
                                Room
                                <i class="bi bi-info-circle ms-1" tabindex="0" role="img" aria-label="Shared devices serve the main entrance and lobby and accept every valid guest PIN. A room code means a private room lock."
                                   data-bs-toggle="tooltip" title="Shared devices serve the main entrance and lobby and accept every valid guest PIN. A room code means a private room lock."></i>
                            </th>
                            <th scope="col" class="pe-3 text-end"><span class="visually-hidden">Actions</span></th>
                        </tr>
                    </thead>
                    <t:forEach items="${deviceGroups}" var="grp">
                        <tbody>
                            <tr class="ss-dev-group">
                                <th scope="rowgroup" colspan="6" class="ps-3">${e:esc(grp.label)}</th>
                            </tr>
                            <t:forEach items="${grp.items}" var="d">
                                <tr class="${!d.active ? 'ss-dev-row--off' : ''}">
                                    <td class="ps-3">
                                        <div class="ss-dev__name"><a class="ss-row-link" href="${pageContext.request.contextPath}/admin/devices/edit?deviceId=${e:url(d.deviceId)}">${e:esc(d.deviceName)}</a></div>
                                        <div class="ss-dev__id">${e:esc(d.deviceId)}</div>
                                    </td>
                                    <td>
                                        <t:if test="${d.deviceType == 'SMART_LOCK'}"><i class="bi bi-lock ss-dev__type" aria-hidden="true"></i>Lock</t:if>
                                        <t:if test="${d.deviceType == 'SECURITY_CAMERA'}"><i class="bi bi-camera-video ss-dev__type" aria-hidden="true"></i>Camera</t:if>
                                        <t:if test="${d.deviceType != 'SMART_LOCK' && d.deviceType != 'SECURITY_CAMERA'}"><i class="bi bi-cpu ss-dev__type" aria-hidden="true"></i>Sensor</t:if>
                                    </td>
                                    <td>
                                        <span class="ss-dev__status">
                                            <t:if test="${!d.active}"><span class="ss-status-dot ss-status-dot--muted" aria-hidden="true"></span>Disabled</t:if>
                                            <t:if test="${d.active && d.status == 'ONLINE'}"><span class="ss-status-dot ss-status-dot--ok" aria-hidden="true"></span>Online</t:if>
                                            <t:if test="${d.active && d.status == 'OFFLINE'}"><span class="ss-status-dot ss-status-dot--warn" aria-hidden="true"></span><span class="ss-dev__warn">Offline</span></t:if>
                                            <t:if test="${d.active && d.status == 'FAULT'}"><span class="ss-status-dot ss-status-dot--danger" aria-hidden="true"></span><span class="ss-dev__bad">Fault</span></t:if>
                                        </span>
                                    </td>
                                    <td>
                                        <t:if test="${not empty d.lastSeenAt}">
                                            <span data-bs-toggle="tooltip" title="${e:datetime(d.lastSeenAt)}">${e:timeAgo(d.lastSeenAt)}</span>
                                        </t:if>
                                        <t:if test="${empty d.lastSeenAt}"><span class="ss-dev__id">Never</span></t:if>
                                    </td>
                                    <td>
                                        <t:if test="${not empty d.roomNumber}">${e:esc(d.roomNumber)}</t:if>
                                        <t:if test="${empty d.roomNumber}"><span class="ss-dev__id">Shared</span></t:if>
                                    </td>
                                    <td class="pe-3 text-end text-nowrap">
                                        <t:if test="${d.deviceType == 'SMART_LOCK' && d.active}">
                                            <%-- Remote door commands (MQTT). Unlock needs a second tap within 3 s. --%>
                                            <div class="ss-dev__cmds">
                                                <form action="${pageContext.request.contextPath}/admin/devices/command" method="post" class="ss-dev__cmd-form">
                                                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                    <input type="hidden" name="deviceId" value="${e:esc(d.deviceId)}">
                                                    <input type="hidden" name="command" value="UNLOCK">
                                                    <button type="submit" class="btn btn-outline-soft btn-sm rounded-pill ss-dev__cmd ss-tap-confirm" data-tap-confirm
                                                            aria-live="polite">
                                                        <i class="bi bi-unlock" aria-hidden="true"></i><span class="ss-tap-confirm__text">Unlock</span><span class="visually-hidden"> ${e:esc(d.deviceName)}</span>
                                                        <span class="ss-tap-confirm__bar" aria-hidden="true"></span>
                                                    </button>
                                                </form>
                                                <form action="${pageContext.request.contextPath}/admin/devices/command" method="post" class="ss-dev__cmd-form">
                                                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                    <input type="hidden" name="deviceId" value="${e:esc(d.deviceId)}">
                                                    <input type="hidden" name="command" value="SILENCE">
                                                    <button type="submit" class="btn btn-outline-soft btn-sm rounded-pill ss-dev__cmd"
                                                            aria-label="Silence the alarm on ${e:esc(d.deviceName)}">
                                                        <i class="bi bi-volume-mute" aria-hidden="true"></i><span>Silence</span>
                                                    </button>
                                                </form>
                                            </div>
                                        </t:if>
                                        <div class="dropdown d-inline-block ss-row-menu">
                                            <button class="btn-trigger" type="button" data-bs-toggle="dropdown" aria-expanded="false"
                                                    aria-label="More actions for ${e:esc(d.deviceName)}">
                                                <i class="bi bi-three-dots" aria-hidden="true"></i>
                                            </button>
                                            <ul class="dropdown-menu dropdown-menu-end">
                                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/devices/edit?deviceId=${e:url(d.deviceId)}">Edit</a></li>
                                                <li>
                                                    <button type="button" class="dropdown-item btn-api-key-modal" data-device-id="${e:esc(d.deviceId)}">New API key</button>
                                                </li>
                                                <li><hr class="dropdown-divider"></li>
                                                <li>
                                                    <button type="button" class="dropdown-item dropdown-item--destructive btn-delete-device-modal"
                                                            data-device-id="${e:esc(d.deviceId)}" data-device-name="${e:esc(d.deviceName)}">Delete device</button>
                                                </li>
                                            </ul>
                                        </div>
                                    </td>
                                </tr>
                            </t:forEach>
                        </tbody>
                    </t:forEach>
                </table>
            </div>
        </t:if>
    </div>

    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/admin/devices"/>

    <!-- Delete Device Confirmation Modal (Requires Device ID typed/confirmed) -->
    <div class="modal fade" id="deleteDeviceModal" tabindex="-1" aria-labelledby="deleteDeviceModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/devices/delete" method="post" id="deleteDeviceForm">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="deviceId" id="modalDeleteDeviceIdHidden">
                    <div class="modal-header bg-danger text-white">
                        <h5 class="modal-title" id="deleteDeviceModalLabel"><i class="bi bi-exclamation-triangle-fill me-2"></i>Delete Device</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p>Are you sure you want to permanently delete device <strong id="modalDeleteDeviceIdText" class="font-monospace text-danger"></strong> (<span id="modalDeleteDeviceNameText"></span>)?</p>
                        <p class="small text-muted mb-3">This action cannot be undone. To prevent accidental deletion, please type the device ID below to confirm:</p>
                        <div class="mb-3">
                            <label for="confirmDeleteDeviceIdInput" class="form-label small fw-semibold">Type Device ID:</label>
                            <input type="text" class="form-control font-monospace" id="confirmDeleteDeviceIdInput" name="confirmDeviceId" autocomplete="off" placeholder="Type device ID here">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-danger btn-sm" id="confirmDeleteBtn" disabled>Delete Device</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- API Key Confirmation Modal (Warning before regeneration) -->
    <div class="modal fade" id="apiKeyConfirmModal" tabindex="-1" aria-labelledby="apiKeyConfirmModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/devices/api-key" method="post" id="apiKeyForm">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="deviceId" id="modalApiKeyDeviceIdHidden">
                    <input type="hidden" name="returnUrl" value="${pageContext.request.contextPath}/admin/devices">
                    <div class="modal-header bg-warning bg-opacity-25 border-bottom border-warning">
                        <h5 class="modal-title text-warning-emphasis" id="apiKeyConfirmModalLabel"><i class="bi bi-key-fill me-2"></i>Regenerate API Key</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p>Generate a new API key for device <strong id="modalApiKeyDeviceIdText" class="font-monospace text-espresso"></strong>?</p>
                        <div class="alert alert-warning small mb-0" role="alert">
                            <i class="bi bi-exclamation-triangle-fill me-1"></i>
                            <strong>Warning:</strong> Regenerating will immediately invalidate the previous API key! Any hardware using the old key will fail to authenticate until updated.
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-warning btn-sm" id="confirmRegenApiKeyBtn">Confirm &amp; Generate Key</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <!-- API Key One-Time Reveal Modal -->
    <div class="modal fade" id="apiKeyRevealModal" tabindex="-1" aria-labelledby="apiKeyRevealModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header bg-success bg-opacity-10 border-bottom border-success">
                    <h5 class="modal-title text-success fw-bold" id="apiKeyRevealModalLabel"><i class="bi bi-shield-check me-2"></i>New API Key Generated</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <p class="small mb-2">New 32-character API key for <strong id="revealDeviceIdText" class="font-monospace text-espresso"></strong>:</p>
                    <div class="input-group mb-3">
                        <input type="text" class="form-control font-monospace bg-light fw-bold" id="revealApiKeyInput" aria-label="New API key" readonly>
                        <button class="btn btn-primary" type="button" id="copyApiKeyBtn">
                            <i class="bi bi-clipboard me-1"></i><span id="copyBtnText">Copy</span>
                        </button>
                    </div>
                    <div class="alert alert-danger small mb-0" role="alert">
                        <i class="bi bi-shield-lock-fill me-1"></i>
                        <strong>Important:</strong> This API key is revealed once and will never be shown again. Store it securely in your device firmware configuration.
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary btn-sm" data-bs-dismiss="modal">Done</button>
                </div>
            </div>
        </div>
    </div>

    <t:if test="${not empty newApiKeyRevealed}">
        <input type="hidden" id="autoOpenApiKey" value="${e:esc(newApiKeyRevealed)}">
        <input type="hidden" id="autoOpenDeviceId" value="${e:esc(newApiKeyDeviceId)}">
    </t:if>

    <script src="${pageContext.request.contextPath}/assets/js/tap-confirm.js"></script>
    <script>
        // Remote command confirmation from the lock (sent through /api/iot/heartbeat -> DEVICE_ACK)
        window.addEventListener('smartstay:realtime', function (ev) {
            var data = ev.detail || {};
            var box = document.getElementById('deviceAck');
            if (data.event !== 'DEVICE_ACK' || !box) {
                return;
            }
            var what = data.command === 'UNLOCK' ? 'Door unlocked'
                     : data.command === 'SILENCE' ? 'Alarm silenced' : String(data.command || 'Command done');
            var now = new Date();
            var hh = ('0' + now.getHours()).slice(-2) + ':' + ('0' + now.getMinutes()).slice(-2) + ':' + ('0' + now.getSeconds()).slice(-2);
            box.textContent = what + ' · ' + (data.deviceId || '') + ' · ' + hh;
            box.hidden = false;
        });

        document.addEventListener('DOMContentLoaded', function() {
            // Delete Modal Logic
            var deleteModalEl = document.getElementById('deleteDeviceModal');
            var deleteModal = deleteModalEl ? new bootstrap.Modal(deleteModalEl) : null;
            var deleteIdHidden = document.getElementById('modalDeleteDeviceIdHidden');
            var deleteIdText = document.getElementById('modalDeleteDeviceIdText');
            var deleteNameText = document.getElementById('modalDeleteDeviceNameText');
            var confirmInput = document.getElementById('confirmDeleteDeviceIdInput');
            var confirmBtn = document.getElementById('confirmDeleteBtn');
            var targetDeviceId = '';

            document.querySelectorAll('.btn-delete-device-modal').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    targetDeviceId = this.getAttribute('data-device-id') || '';
                    var dName = this.getAttribute('data-device-name') || '';
                    if (deleteIdHidden) deleteIdHidden.value = targetDeviceId;
                    if (deleteIdText) deleteIdText.textContent = targetDeviceId;
                    if (deleteNameText) deleteNameText.textContent = dName;
                    if (confirmInput) {
                        confirmInput.value = '';
                        confirmInput.placeholder = 'Type ' + targetDeviceId + ' to confirm';
                    }
                    if (confirmBtn) confirmBtn.disabled = true;
                    if (deleteModal) deleteModal.show();
                });
            });

            if (confirmInput) {
                confirmInput.addEventListener('input', function() {
                    if (confirmBtn) {
                        confirmBtn.disabled = (this.value.trim() !== targetDeviceId);
                    }
                });
            }

            // API Key Confirm Modal & Reveal Logic
            var apiKeyConfirmModalEl = document.getElementById('apiKeyConfirmModal');
            var apiKeyConfirmModal = apiKeyConfirmModalEl ? new bootstrap.Modal(apiKeyConfirmModalEl) : null;
            var apiKeyRevealModalEl = document.getElementById('apiKeyRevealModal');
            var apiKeyRevealModal = apiKeyRevealModalEl ? new bootstrap.Modal(apiKeyRevealModalEl) : null;

            var apiKeyForm = document.getElementById('apiKeyForm');
            var modalApiKeyDeviceIdHidden = document.getElementById('modalApiKeyDeviceIdHidden');
            var modalApiKeyDeviceIdText = document.getElementById('modalApiKeyDeviceIdText');
            var revealDeviceIdText = document.getElementById('revealDeviceIdText');
            var revealApiKeyInput = document.getElementById('revealApiKeyInput');
            var copyApiKeyBtn = document.getElementById('copyApiKeyBtn');
            var copyBtnText = document.getElementById('copyBtnText');

            document.querySelectorAll('.btn-api-key-modal').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    var dId = this.getAttribute('data-device-id') || '';
                    if (modalApiKeyDeviceIdHidden) modalApiKeyDeviceIdHidden.value = dId;
                    if (modalApiKeyDeviceIdText) modalApiKeyDeviceIdText.textContent = dId;
                    if (apiKeyConfirmModal) apiKeyConfirmModal.show();
                });
            });

            if (apiKeyForm) {
                apiKeyForm.addEventListener('submit', function(e) {
                    // Try fetch for seamless in-modal reveal
                    e.preventDefault();
                    var dId = modalApiKeyDeviceIdHidden ? modalApiKeyDeviceIdHidden.value : '';
                    var formData = new FormData(apiKeyForm);
                    formData.append('ajax', '1');

                    fetch(apiKeyForm.action, {
                        method: 'POST',
                        body: new URLSearchParams(formData),
                        headers: {
                            'Accept': 'application/json',
                            'X-Requested-With': 'XMLHttpRequest'
                        }
                    })
                    .then(function(res) {
                        if (res.ok) return res.json();
                        throw new Error('Failed to generate key');
                    })
                    .then(function(data) {
                        if (apiKeyConfirmModal) apiKeyConfirmModal.hide();
                        if (revealDeviceIdText) revealDeviceIdText.textContent = data.deviceId || dId;
                        if (revealApiKeyInput) revealApiKeyInput.value = data.rawKey || '';
                        if (copyBtnText) copyBtnText.textContent = 'Copy';
                        if (apiKeyRevealModal) apiKeyRevealModal.show();
                    })
                    .catch(function(err) {
                        // Fallback to standard submit
                        apiKeyForm.submit();
                    });
                });
            }

            if (copyApiKeyBtn && revealApiKeyInput) {
                copyApiKeyBtn.addEventListener('click', function() {
                    revealApiKeyInput.select();
                    navigator.clipboard.writeText(revealApiKeyInput.value).then(function() {
                        if (copyBtnText) copyBtnText.textContent = 'Copied!';
                        setTimeout(function() {
                            if (copyBtnText) copyBtnText.textContent = 'Copy';
                        }, 2000);
                    });
                });
            }

            // Auto-open reveal modal if redirected from standard POST
            var autoKey = document.getElementById('autoOpenApiKey');
            var autoId = document.getElementById('autoOpenDeviceId');
            if (autoKey && autoKey.value && apiKeyRevealModal) {
                if (revealDeviceIdText && autoId) revealDeviceIdText.textContent = autoId.value;
                if (revealApiKeyInput) revealApiKeyInput.value = autoKey.value;
                apiKeyRevealModal.show();
            }
        });
    </script>
</t:layout>
