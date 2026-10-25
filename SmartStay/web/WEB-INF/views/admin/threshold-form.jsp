<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${threshold.thresholdId > 0 ? 'Edit Threshold' : 'New Threshold'}" active="thresholds">
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
                        <a href="${pageContext.request.contextPath}/admin/thresholds" class="boutique-breadcrumb-link">
                            Thresholds
                        </a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">${threshold.thresholdId > 0 ? 'Edit Threshold' : 'New Threshold'}</li>
                </ol>
            </nav>

            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <h1 class="h2 mb-1">${threshold.thresholdId > 0 ? 'Edit alert threshold' : 'New alert threshold'}</h1>
                    <p class="text-muted small mb-0">
                        ${threshold.thresholdId > 0 ? 'Update trigger evaluation parameters and alert classification' : 'Define metric trigger conditions to automatically generate operational alerts'}
                    </p>
                </div>
                <div>
                    <a href="${pageContext.request.contextPath}/admin/thresholds" class="btn btn-outline-secondary btn-sm">
                        &larr; Back to thresholds
                    </a>
                </div>
            </div>

            <t:if test="${not empty errors['general']}">
                <div class="alert alert-danger" role="alert">${e:esc(errors['general'])}</div>
            </t:if>

            <div class="card shadow-sm mb-4">
                <div class="card-body">
                    <form action="${pageContext.request.contextPath}/admin/thresholds/edit" method="post" class="needs-validation" novalidate>
                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                        <input type="hidden" name="thresholdId" value="${threshold.thresholdId}">

                        <div class="mb-3">
                            <label for="deviceId" class="form-label fw-semibold">Device scope</label>
                            <select class="form-select ${not empty errors['deviceId'] ? 'is-invalid' : ''}"
                                    id="deviceId" name="deviceId">
                                <option value="">All devices (Global rule)</option>
                                <t:forEach items="${devices}" var="dev">
                                    <option value="${dev.deviceId}" ${threshold.deviceId == dev.deviceId ? 'selected' : ''}>
                                        ${e:esc(dev.deviceId)} - ${e:esc(dev.deviceName)}
                                    </option>
                                </t:forEach>
                            </select>
                            <div class="form-text small">Leave blank to apply this rule to all reporting IoT devices.</div>
                            <t:if test="${not empty errors['deviceId']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['deviceId'])}</div>
                            </t:if>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-4">
                                <label for="metricType" class="form-label fw-semibold">
                                    Metric type <span class="text-danger">*</span>
                                </label>
                                <select class="form-select ${not empty errors['metricType'] ? 'is-invalid' : ''}"
                                        id="metricType" name="metricType" required>
                                    <option value="MOTION" ${threshold.metricType == 'MOTION' || threshold.metricType == 'PIR_TRIGGER' ? 'selected' : ''}>Motion Trigger (MOTION)</option>
                                    <option value="DISTANCE_CM" ${threshold.metricType == 'DISTANCE_CM' ? 'selected' : ''}>Distance (DISTANCE_CM)</option>
                                    <option value="DOOR_OPEN_SECONDS" ${threshold.metricType == 'DOOR_OPEN_SECONDS' ? 'selected' : ''}>Door Open (DOOR_OPEN_SECONDS)</option>
                                </select>
                                <t:if test="${not empty errors['metricType']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['metricType'])}</div>
                                </t:if>
                            </div>

                            <div class="col-md-4">
                                <label for="operator" class="form-label fw-semibold">
                                    Operator <span class="text-danger">*</span>
                                </label>
                                <select class="form-select ${not empty errors['operator'] ? 'is-invalid' : ''}"
                                        id="operator" name="operator" required>
                                    <option value="GT" ${threshold.operator == 'GT' ? 'selected' : ''}>Greater than (&gt;)</option>
                                    <option value="GTE" ${threshold.operator == 'GTE' ? 'selected' : ''}>Greater than or equal (&gt;=)</option>
                                    <option value="LT" ${threshold.operator == 'LT' ? 'selected' : ''}>Less than (&lt;)</option>
                                    <option value="LTE" ${threshold.operator == 'LTE' ? 'selected' : ''}>Less than or equal (&lt;=)</option>
                                </select>
                                <t:if test="${not empty errors['operator']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['operator'])}</div>
                                </t:if>
                            </div>

                            <div class="col-md-4">
                                <label for="thresholdValue" class="form-label fw-semibold">
                                    Threshold value <span class="text-danger">*</span>
                                </label>
                                <input type="number" step="any" min="0"
                                       class="form-control ${not empty errors['thresholdValue'] ? 'is-invalid' : ''}"
                                       id="thresholdValue" name="thresholdValue"
                                       value="${threshold.thresholdValue}" required>
                                <t:if test="${not empty errors['thresholdValue']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['thresholdValue'])}</div>
                                </t:if>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="alertType" class="form-label fw-semibold">
                                    Alert type <span class="text-danger">*</span>
                                </label>
                                <select class="form-select ${not empty errors['alertType'] ? 'is-invalid' : ''}"
                                        id="alertType" name="alertType" required>
                                    <option value="PIN_BRUTE_FORCE" ${threshold.alertType == 'PIN_BRUTE_FORCE' ? 'selected' : ''}>PIN_BRUTE_FORCE</option>
                                    <option value="DOOR_OPEN_ABNORMAL" ${threshold.alertType == 'DOOR_OPEN_ABNORMAL' ? 'selected' : ''}>DOOR_OPEN_ABNORMAL</option>
                                    <option value="DOOR_OPEN_TOO_LONG" ${threshold.alertType == 'DOOR_OPEN_TOO_LONG' ? 'selected' : ''}>DOOR_OPEN_TOO_LONG</option>
                                    <option value="LOBBY_PROXIMITY" ${threshold.alertType == 'LOBBY_PROXIMITY' ? 'selected' : ''}>LOBBY_PROXIMITY</option>
                                    <option value="SUSPICIOUS_MOTION" ${threshold.alertType == 'SUSPICIOUS_MOTION' ? 'selected' : ''}>SUSPICIOUS_MOTION</option>
                                    <option value="CLEANING_SESSION_EXPIRED" ${threshold.alertType == 'CLEANING_SESSION_EXPIRED' ? 'selected' : ''}>CLEANING_SESSION_EXPIRED</option>
                                    <option value="PAYMENT_REVIEW_REQUIRED" ${threshold.alertType == 'PAYMENT_REVIEW_REQUIRED' ? 'selected' : ''}>PAYMENT_REVIEW_REQUIRED</option>
                                    <option value="BOOKING_NO_SHOW" ${threshold.alertType == 'BOOKING_NO_SHOW' ? 'selected' : ''}>BOOKING_NO_SHOW</option>
                                    <option value="DEVICE_OFFLINE" ${threshold.alertType == 'DEVICE_OFFLINE' ? 'selected' : ''}>DEVICE_OFFLINE</option>
                                    <option value="DEVICE_FAULT" ${threshold.alertType == 'DEVICE_FAULT' ? 'selected' : ''}>DEVICE_FAULT</option>
                                </select>
                                <t:if test="${not empty errors['alertType']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['alertType'])}</div>
                                </t:if>
                            </div>

                            <div class="col-md-6">
                                <label for="severity" class="form-label fw-semibold">
                                    Severity <span class="text-danger">*</span>
                                </label>
                                <select class="form-select ${not empty errors['severity'] ? 'is-invalid' : ''}"
                                        id="severity" name="severity" required>
                                    <option value="LOW" ${threshold.severity == 'LOW' ? 'selected' : ''}>LOW</option>
                                    <option value="MEDIUM" ${threshold.severity == 'MEDIUM' ? 'selected' : ''}>MEDIUM</option>
                                    <option value="HIGH" ${threshold.severity == 'HIGH' ? 'selected' : ''}>HIGH</option>
                                    <option value="CRITICAL" ${threshold.severity == 'CRITICAL' ? 'selected' : ''}>CRITICAL</option>
                                </select>
                                <t:if test="${not empty errors['severity']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['severity'])}</div>
                                </t:if>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label for="description" class="form-label fw-semibold">Description</label>
                            <input type="text" class="form-control ${not empty errors['description'] ? 'is-invalid' : ''}"
                                   id="description" name="description" value="${e:esc(threshold.description)}"
                                   maxlength="255" placeholder="e.g. Proximity alert when object is closer than 50cm">
                            <div class="form-text small">Optional; helpful summary for operators.</div>
                            <t:if test="${not empty errors['description']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['description'])}</div>
                            </t:if>
                        </div>

                        <div class="mb-4 form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="enabled" name="enabled" value="1"
                                   ${threshold.enabled ? 'checked' : ''}>
                            <label class="form-check-label fw-semibold" for="enabled">Rule enabled</label>
                            <div class="form-text small">Disabled rules will not trigger any alerts upon incoming telemetry.</div>
                        </div>

                        <div class="d-flex justify-content-between align-items-center">
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary">
                                    ${threshold.thresholdId > 0 ? 'Update threshold' : 'Create threshold'}
                                </button>
                                <a href="${pageContext.request.contextPath}/admin/thresholds" class="btn btn-outline-secondary">
                                    Cancel
                                </a>
                            </div>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</t:layout>
