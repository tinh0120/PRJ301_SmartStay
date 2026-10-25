<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Devices" active="devices">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Devices</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h2 mb-1 text-espresso">Device management</h1>
            <p class="text-muted small mb-0">Configure smart locks and security cameras</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/devices/edit" class="btn btn-primary btn-sm">
                + New device
            </a>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Mobile Filter Toggle Button (< 768px) -->
    <div class="d-md-none mb-3">
        <button class="btn btn-outline-secondary btn-sm w-100 d-flex justify-content-between align-items-center" type="button" data-bs-toggle="collapse" data-bs-target="#deviceFilterCollapse" aria-expanded="false" aria-controls="deviceFilterCollapse">
            <span><i class="bi bi-funnel me-1"></i> Filters &amp; Search</span>
            <i class="bi bi-chevron-down"></i>
        </button>
    </div>

    <div class="card mb-4 shadow-sm collapse d-md-block" id="deviceFilterCollapse">
        <div class="card-body">
            <form action="${pageContext.request.contextPath}/admin/devices" method="get" class="row g-2 align-items-end">
                <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
                <input type="hidden" name="dir" value="${e:esc(pr.dir)}">

                <div class="col-md-3">
                    <label for="keyword" class="form-label small fw-semibold">Search</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="ID, name, or zone..." maxlength="100"
                           value="${e:esc(pr.keyword)}">
                </div>

                <div class="col-md-2">
                    <label for="type" class="form-label small fw-semibold">Type</label>
                    <select class="form-select form-select-sm ${not empty errors['type'] ? 'is-invalid' : ''}" id="type" name="type">
                        <option value="">All types</option>
                        <option value="SMART_LOCK" ${pr.getFilter('type') == 'SMART_LOCK' ? 'selected' : ''}>Smart Lock</option>
                        <option value="SECURITY_CAMERA" ${pr.getFilter('type') == 'SECURITY_CAMERA' ? 'selected' : ''}>Security Camera</option>
                    </select>
                    <t:if test="${not empty errors['type']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['type'])}</div>
                    </t:if>
                </div>

                <div class="col-md-2">
                    <label for="status" class="form-label small fw-semibold">Status</label>
                    <select class="form-select form-select-sm ${not empty errors['status'] ? 'is-invalid' : ''}" id="status" name="status">
                        <option value="">All statuses</option>
                        <option value="ONLINE" ${pr.getFilter('status') == 'ONLINE' ? 'selected' : ''}>Online</option>
                        <option value="OFFLINE" ${pr.getFilter('status') == 'OFFLINE' ? 'selected' : ''}>Offline</option>
                        <option value="FAULT" ${pr.getFilter('status') == 'FAULT' ? 'selected' : ''}>Fault</option>
                    </select>
                    <t:if test="${not empty errors['status']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['status'])}</div>
                    </t:if>
                </div>

                <div class="col-md-2">
                    <label for="active" class="form-label small fw-semibold">Active</label>
                    <select class="form-select form-select-sm ${not empty errors['active'] ? 'is-invalid' : ''}" id="active" name="active">
                        <option value="">All</option>
                        <option value="1" ${pr.getFilter('active') == '1' ? 'selected' : ''}>Active only</option>
                        <option value="0" ${pr.getFilter('active') == '0' ? 'selected' : ''}>Inactive only</option>
                    </select>
                    <t:if test="${not empty errors['active']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['active'])}</div>
                    </t:if>
                </div>

                <div class="col-md-3 d-flex gap-2">
                    <button type="submit" class="btn btn-primary btn-sm flex-grow-1">Filter</button>
                    <a href="${pageContext.request.contextPath}/admin/devices" class="btn btn-outline-secondary btn-sm">Reset</a>
                </div>
            </form>
        </div>
    </div>

    <!-- Note & Scope Explanation -->
    <div class="d-flex flex-wrap align-items-center gap-3 small text-muted mb-2 px-1">
        <span><i class="bi bi-info-circle me-1 text-espresso"></i><strong>Hardware Scope:</strong> Devices with Room <code>- (Shared)</code> belong to common areas (Main Entrance &amp; Lobby) accessible to all valid guest PINs; assigned rooms are private room locks.</span>
    </div>

    <div class="card shadow-sm mb-4">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover table-striped align-middle mb-0">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-3"><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/devices" column="id" label="Device ID"/></th>
                            <th><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/devices" column="name" label="Name"/></th>
                            <th><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/devices" column="type" label="Type"/></th>
                            <th>Zone</th>
                            <th>
                                Room
                                <i class="bi bi-info-circle ms-1 text-muted" data-bs-toggle="tooltip" title="'-' indicates a shared device (main entrance/lobby) accessible to all active guests, whereas room numbers indicate private door locks"></i>
                            </th>
                            <th><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/devices" column="status" label="Status"/></th>
                            <th>Active</th>
                            <th><t:sortLink result="${result}" baseUrl="${pageContext.request.contextPath}/admin/devices" column="seen" label="Last Seen"/></th>
                            <th class="pe-3 text-end">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:if test="${empty result.items}">
                            <tr>
                                <td colspan="9" class="text-center py-4 text-muted">
                                    No devices found matching your criteria.
                                </td>
                            </tr>
                        </t:if>
                        <t:forEach items="${result.items}" var="d">
                            <tr>
                                <td class="ps-3"><code class="fw-bold">${e:esc(d.deviceId)}</code></td>
                                <td>${e:esc(d.deviceName)}</td>
                                <td>
                                    <t:if test="${d.deviceType == 'SMART_LOCK'}">
                                        <span class="badge bg-info text-dark">Lock</span>
                                    </t:if>
                                    <t:if test="${d.deviceType == 'SECURITY_CAMERA'}">
                                        <span class="badge bg-secondary">Camera</span>
                                    </t:if>
                                </td>
                                <td>${e:esc(d.zoneId)}</td>
                                <td>
                                    <t:if test="${not empty d.roomNumber}">
                                        <span class="badge bg-light text-dark border">Room ${e:esc(d.roomNumber)}</span>
                                    </t:if>
                                    <t:if test="${empty d.roomNumber}">
                                        <span class="badge bg-secondary-subtle text-secondary" data-bs-toggle="tooltip" title="Shared device (main entrance / lobby) &ndash; accessible to all guests">- (Shared)</span>
                                    </t:if>
                                </td>
                                <td>
                                    <span class="badge-status status-${e:esc(d.status)}">${e:esc(d.status)}</span>
                                </td>
                                <td>
                                    <t:if test="${d.active}">
                                        <span class="badge bg-success">Yes</span>
                                    </t:if>
                                    <t:if test="${not d.active}">
                                        <span class="badge bg-secondary">No</span>
                                    </t:if>
                                </td>
                                <td class="small">
                                    <t:if test="${not empty d.lastSeenAt}">
                                        <span class="font-monospace text-dark" data-bs-toggle="tooltip" title="${e:datetime(d.lastSeenAt)}">
                                            ${e:timeAgo(d.lastSeenAt)}
                                        </span>
                                    </t:if>
                                    <t:if test="${empty d.lastSeenAt}">
                                        <span class="text-muted">-</span>
                                    </t:if>
                                </td>
                                <td class="pe-3 text-end">
                                    <div class="d-inline-flex gap-1 align-items-center">
                                        <a href="${pageContext.request.contextPath}/admin/devices/edit?deviceId=${e:url(d.deviceId)}"
                                           class="btn btn-outline-primary btn-sm py-0 px-2" title="Edit device details">
                                            Edit
                                        </a>

                                        <button type="button" class="btn btn-outline-warning btn-sm py-0 px-2 btn-api-key-modal"
                                                data-device-id="${e:esc(d.deviceId)}" title="Regenerate API key">
                                            API key
                                        </button>

                                        <button type="button" class="btn btn-outline-danger btn-sm py-0 px-2 btn-delete-device-modal"
                                                data-device-id="${e:esc(d.deviceId)}" data-device-name="${e:esc(d.deviceName)}"
                                                title="Delete device">
                                            Delete
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </div>
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
                        <input type="text" class="form-control font-monospace bg-light fw-bold" id="revealApiKeyInput" readonly>
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

    <script>
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
