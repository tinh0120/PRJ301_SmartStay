<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${isEdit ? 'Edit Device' : 'New Device'}" active="devices">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <nav aria-label="breadcrumb" class="mb-3">
                <ol class="breadcrumb boutique-breadcrumb m-0">
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                            <i class="bi bi-house-door me-1"></i>Dashboard
                        </a>
                    </li>
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/admin/devices" class="boutique-breadcrumb-link">
                            Devices
                        </a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">${isEdit ? 'Edit Device' : 'New Device'}</li>
                </ol>
            </nav>

            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <h1 class="h2 mb-1">${isEdit ? 'Edit device' : 'Register new device'}</h1>
                    <p class="text-muted small mb-0">
                        ${isEdit ? 'Update hardware attributes and room mapping' : 'Add a new smart lock or security camera to the property'}
                    </p>
                </div>
                <div>
                    <a href="${pageContext.request.contextPath}/admin/devices" class="btn btn-outline-secondary btn-sm">
                        &larr; Back to devices
                    </a>
                </div>
            </div>

            <t:if test="${not empty errors['general']}">
                <div class="alert alert-danger" role="alert">${e:esc(errors['general'])}</div>
            </t:if>

            <div class="card shadow-sm mb-4">
                <div class="card-body">
                    <form action="${pageContext.request.contextPath}/admin/devices/edit" method="post" class="needs-validation" novalidate>
                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                        <input type="hidden" name="isEdit" value="${isEdit ? 'true' : 'false'}">

                        <div class="mb-3">
                            <label for="deviceId" class="form-label fw-semibold">
                                Device ID <span class="text-danger">*</span>
                            </label>
                            <t:if test="${isEdit}">
                                <input type="text" class="form-control font-monospace bg-light" id="deviceId" name="deviceId"
                                       value="${e:esc(device.deviceId)}" readonly>
                                <div class="form-text small">Device ID cannot be modified after registration.</div>
                            </t:if>
                            <t:if test="${not isEdit}">
                                <input type="text" class="form-control font-monospace ${not empty errors['deviceId'] ? 'is-invalid' : ''}"
                                       id="deviceId" name="deviceId" value="${e:esc(device.deviceId)}" required
                                       maxlength="50" pattern="^[A-Z0-9_]{3,50}$" placeholder="e.g. MAIN_DOOR_LOCK_01">
                                <div class="form-text small">3 to 50 uppercase alphanumeric characters or underscores.</div>
                                <t:if test="${not empty errors['deviceId']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['deviceId'])}</div>
                                </t:if>
                            </t:if>
                        </div>

                        <div class="mb-3">
                            <label for="deviceName" class="form-label fw-semibold">
                                Device name <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control ${not empty errors['deviceName'] ? 'is-invalid' : ''}"
                                   id="deviceName" name="deviceName" value="${e:esc(device.deviceName)}" required
                                   maxlength="100" placeholder="e.g. Main Entrance Smart Lock">
                            <t:if test="${not empty errors['deviceName']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['deviceName'])}</div>
                            </t:if>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="deviceType" class="form-label fw-semibold">
                                    Device type <span class="text-danger">*</span>
                                </label>
                                <select class="form-select ${not empty errors['deviceType'] ? 'is-invalid' : ''}"
                                        id="deviceType" name="deviceType" required>
                                    <option value="SMART_LOCK" ${device.deviceType == 'SMART_LOCK' ? 'selected' : ''}>Smart Lock</option>
                                    <option value="SECURITY_CAMERA" ${device.deviceType == 'SECURITY_CAMERA' ? 'selected' : ''}>Security Camera</option>
                                </select>
                                <t:if test="${not empty errors['deviceType']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['deviceType'])}</div>
                                </t:if>
                            </div>

                            <div class="col-md-6">
                                <label for="zoneId" class="form-label fw-semibold">
                                    Zone <span class="text-danger">*</span>
                                </label>
                                <select class="form-select ${not empty errors['zoneId'] ? 'is-invalid' : ''}"
                                        id="zoneId" name="zoneId" required>
                                    <option value="MAIN_ENTRANCE" ${device.zoneId == 'MAIN_ENTRANCE' ? 'selected' : ''}>Main Entrance</option>
                                    <option value="LOBBY" ${device.zoneId == 'LOBBY' ? 'selected' : ''}>Lobby</option>
                                </select>
                                <t:if test="${not empty errors['zoneId']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['zoneId'])}</div>
                                </t:if>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="roomId" class="form-label fw-semibold">Room assignment</label>
                                <select class="form-select ${not empty errors['roomId'] ? 'is-invalid' : ''}"
                                        id="roomId" name="roomId">
                                    <option value="">- Shared Device (Main Entrance / Lobby) -</option>
                                    <t:forEach items="${rooms}" var="r">
                                        <option value="${r.roomId}" ${device.roomId == r.roomId ? 'selected' : ''}>
                                            Room ${e:esc(r.roomNumber)} (${e:esc(r.status)})
                                        </option>
                                    </t:forEach>
                                </select>
                                <div class="form-text small">Select a specific room for private door hardware, or leave as shared for entrance/lobby.</div>
                                <t:if test="${not empty errors['roomId']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['roomId'])}</div>
                                </t:if>
                            </div>

                            <div class="col-md-6">
                                <label for="status" class="form-label fw-semibold">
                                    Operational status <span class="text-danger">*</span>
                                </label>
                                <select class="form-select ${not empty errors['status'] ? 'is-invalid' : ''}"
                                        id="status" name="status" required>
                                    <option value="ONLINE" ${device.status == 'ONLINE' ? 'selected' : ''}>Online</option>
                                    <option value="OFFLINE" ${device.status == 'OFFLINE' ? 'selected' : ''}>Offline</option>
                                    <option value="FAULT" ${device.status == 'FAULT' ? 'selected' : ''}>Fault</option>
                                </select>
                                <t:if test="${not empty errors['status']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['status'])}</div>
                                </t:if>
                            </div>
                        </div>

                        <t:if test="${isEdit}">
                            <div class="mb-3">
                                <label class="form-label fw-semibold">Last seen heartbeat</label>
                                <input type="text" class="form-control bg-light" readonly
                                       value="${not empty device.lastSeenAt ? e:datetime(device.lastSeenAt) : 'Never'}">
                                <div class="form-text small">Recorded automatically when telemetry is ingested.</div>
                            </div>
                        </t:if>

                        <div class="mb-4 form-check form-switch theme-switch">
                            <input class="form-check-input" type="checkbox" id="active" name="active" value="1"
                                   ${device.active ? 'checked' : ''}>
                            <label class="form-check-label fw-semibold" for="active">Active device</label>
                            <div class="form-text small">Deactivated devices are ignored during door unlock and monitoring operations.</div>
                        </div>

                        <div class="d-flex justify-content-between align-items-center">
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary">
                                    ${isEdit ? 'Update device' : 'Register device'}
                                </button>
                                <a href="${pageContext.request.contextPath}/admin/devices" class="btn btn-outline-secondary">
                                    Cancel
                                </a>
                            </div>
                        </div>
                    </form>
                </div>
            </div>

            <t:if test="${isEdit}">
                <div class="card border-warning shadow-sm mb-4">
                    <div class="card-header bg-warning bg-opacity-10 fw-semibold text-warning-emphasis">
                        <i class="bi bi-key-fill me-1"></i>API Key Management
                    </div>
                    <div class="card-body">
                        <p class="small text-muted mb-3">
                            Generate a new 32-character API key for this device. The raw key is revealed once with a copy button and never stored in plain text.
                        </p>
                        <button type="button" class="btn btn-outline-warning btn-sm" id="btnOpenApiKeyModal">
                            <i class="bi bi-arrow-repeat me-1"></i>Regenerate API key
                        </button>
                    </div>
                </div>

                <!-- API Key Confirmation Modal -->
                <div class="modal fade" id="apiKeyConfirmModal" tabindex="-1" aria-labelledby="apiKeyConfirmModalLabel" aria-hidden="true">
                    <div class="modal-dialog">
                        <div class="modal-content">
                            <form action="${pageContext.request.contextPath}/admin/devices/api-key" method="post" id="apiKeyForm">
                                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                <input type="hidden" name="deviceId" value="${e:esc(device.deviceId)}">
                                <input type="hidden" name="returnUrl" value="${pageContext.request.contextPath}/admin/devices/edit?deviceId=${e:url(device.deviceId)}">
                                <div class="modal-header bg-warning bg-opacity-25 border-bottom border-warning">
                                    <h5 class="modal-title text-warning-emphasis" id="apiKeyConfirmModalLabel"><i class="bi bi-key-fill me-2"></i>Regenerate API Key</h5>
                                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                </div>
                                <div class="modal-body">
                                    <p>Regenerate a new API key for device <strong class="font-monospace text-espresso">${e:esc(device.deviceId)}</strong>?</p>
                                    <div class="alert alert-warning small mb-0" role="alert">
                                        <i class="bi bi-exclamation-triangle-fill me-1"></i>
                                        <strong>Warning:</strong> Regenerating will immediately invalidate the previous key! Connected hardware will fail authentication until updated with the new key.
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
                                <p class="small mb-2">New 32-character API key for <strong class="font-monospace text-espresso">${e:esc(device.deviceId)}</strong>:</p>
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
                </t:if>

                <script>
                    document.addEventListener('DOMContentLoaded', function() {
                        var apiKeyConfirmModalEl = document.getElementById('apiKeyConfirmModal');
                        var apiKeyConfirmModal = apiKeyConfirmModalEl ? new bootstrap.Modal(apiKeyConfirmModalEl) : null;
                        var apiKeyRevealModalEl = document.getElementById('apiKeyRevealModal');
                        var apiKeyRevealModal = apiKeyRevealModalEl ? new bootstrap.Modal(apiKeyRevealModalEl) : null;

                        var btnOpen = document.getElementById('btnOpenApiKeyModal');
                        if (btnOpen && apiKeyConfirmModal) {
                            btnOpen.addEventListener('click', function() {
                                apiKeyConfirmModal.show();
                            });
                        }

                        var apiKeyForm = document.getElementById('apiKeyForm');
                        var revealApiKeyInput = document.getElementById('revealApiKeyInput');
                        var copyApiKeyBtn = document.getElementById('copyApiKeyBtn');
                        var copyBtnText = document.getElementById('copyBtnText');

                        if (apiKeyForm) {
                            apiKeyForm.addEventListener('submit', function(e) {
                                e.preventDefault();
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
                                    if (revealApiKeyInput) revealApiKeyInput.value = data.rawKey || '';
                                    if (copyBtnText) copyBtnText.textContent = 'Copy';
                                    if (apiKeyRevealModal) apiKeyRevealModal.show();
                                })
                                .catch(function() {
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

                        var autoKey = document.getElementById('autoOpenApiKey');
                        if (autoKey && autoKey.value && apiKeyRevealModal) {
                            if (revealApiKeyInput) revealApiKeyInput.value = autoKey.value;
                            apiKeyRevealModal.show();
                        }
                    });
                </script>
            </t:if>
        </div>
    </div>
</t:layout>
