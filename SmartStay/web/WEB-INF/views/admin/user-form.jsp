<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${empty targetUser.userId ? 'New user' : 'Edit user'}" active="admin-users">
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
                        <a href="${pageContext.request.contextPath}/admin/users" class="boutique-breadcrumb-link">
                            Users
                        </a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">${empty targetUser.userId ? 'New User' : 'Edit User'}</li>
                </ol>
            </nav>

            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <h1 class="h2 mb-1">${empty targetUser.userId ? 'Create new user' : 'Edit user'}</h1>
                    <p class="text-muted small mb-0">Configure account credentials, system role, and staff employment details</p>
                </div>
                <div>
                    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary btn-sm">
                        &larr; Back to users
                    </a>
                </div>
            </div>

            <t:if test="${not empty globalError}">
                <div class="alert alert-danger" role="alert">${e:esc(globalError)}</div>
            </t:if>

            <div class="card shadow-sm mb-4">
                <div class="card-body p-4">
                    <form action="${pageContext.request.contextPath}/admin/users/edit" method="post" id="userForm" novalidate>
                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                        <input type="hidden" name="userId" value="${e:esc(targetUser.userId)}">

                        <h5 class="card-title mb-3 border-bottom pb-2">Account information</h5>

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="fullName" class="form-label small fw-semibold">Full name <span class="text-danger">*</span></label>
                                <input type="text" class="form-control ${not empty fieldErrors.fullName ? 'is-invalid' : ''}"
                                       id="fullName" name="fullName" maxlength="100" required
                                       value="${e:esc(targetUser.fullName)}" placeholder="e.g. Nguyen Van A">
                                <t:if test="${not empty fieldErrors.fullName}">
                                    <div class="invalid-feedback">${e:esc(fieldErrors.fullName)}</div>
                                </t:if>
                            </div>

                            <div class="col-md-6">
                                <label for="email" class="form-label small fw-semibold">Email address <span class="text-danger">*</span></label>
                                <input type="email" class="form-control ${not empty fieldErrors.email ? 'is-invalid' : ''}"
                                       id="email" name="email" maxlength="255" required
                                       value="${e:esc(targetUser.email)}" placeholder="name@example.com">
                                <t:if test="${not empty fieldErrors.email}">
                                    <div class="invalid-feedback">${e:esc(fieldErrors.email)}</div>
                                </t:if>
                            </div>
                        </div>

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="phone" class="form-label small fw-semibold">Phone number <span class="text-danger">*</span></label>
                                <input type="tel" class="form-control ${not empty fieldErrors.phone ? 'is-invalid' : ''}"
                                       id="phone" name="phone" pattern="^(0|\+84)\d{9,10}$" required
                                       value="${e:esc(targetUser.phone)}" placeholder="0912345678 or +84912345678">
                                <t:if test="${not empty fieldErrors.phone}">
                                    <div class="invalid-feedback">${e:esc(fieldErrors.phone)}</div>
                                </t:if>
                            </div>

                            <div class="col-md-3">
                                <label for="role" class="form-label small fw-semibold">Role <span class="text-danger">*</span></label>
                                <select class="form-select ${not empty fieldErrors.role ? 'is-invalid' : ''}"
                                        id="role" name="role" required onchange="updateStaffVisibility(this.value)">
                                    <t:forEach items="${availableRoles}" var="r">
                                        <option value="${r}" ${targetUser.roleName == r ? 'selected' : ''}>${r}</option>
                                    </t:forEach>
                                </select>
                                <t:if test="${not empty fieldErrors.role}">
                                    <div class="invalid-feedback">${e:esc(fieldErrors.role)}</div>
                                </t:if>
                            </div>

                            <div class="col-md-3">
                                <label for="status" class="form-label small fw-semibold">Status <span class="text-danger">*</span></label>
                                <select class="form-select ${not empty fieldErrors.status ? 'is-invalid' : ''}"
                                        id="status" name="status" required>
                                    <option value="ACTIVE" ${targetUser.status == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                    <option value="LOCKED" ${targetUser.status == 'LOCKED' ? 'selected' : ''}>Locked</option>
                                    <option value="INACTIVE" ${targetUser.status == 'INACTIVE' ? 'selected' : ''}>Inactive</option>
                                </select>
                                <t:if test="${not empty fieldErrors.status}">
                                    <div class="invalid-feedback">${e:esc(fieldErrors.status)}</div>
                                </t:if>
                            </div>
                        </div>

                        <h5 class="card-title mt-4 mb-3 border-bottom pb-2">Password credentials</h5>

                        <div class="row g-3 mb-3">
                            <div class="col-md-6">
                                <label for="password" class="form-label small fw-semibold">
                                    Password
                                    <t:if test="${empty targetUser.userId}"><span class="text-danger">*</span></t:if>
                                </label>
                                <div class="input-group has-validation">
                                    <input type="password" class="form-control ${not empty fieldErrors.password ? 'is-invalid' : ''}"
                                           id="password" name="password" minlength="8"
                                           ${empty targetUser.userId ? 'required' : ''}
                                           placeholder="${empty targetUser.userId ? 'At least 8 chars, letters and digits' : 'Leave blank to keep current password'}">
                                    <button class="btn btn-outline-secondary toggle-password-btn" type="button"
                                            tabindex="-1" aria-label="Show password" title="Show password">
                                        <svg class="eye-icon eye-open" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                            <circle cx="12" cy="12" r="3"></circle>
                                        </svg>
                                        <svg class="eye-icon eye-closed d-none" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                            <line x1="1" y1="1" x2="23" y2="23"></line>
                                        </svg>
                                    </button>
                                    <t:if test="${not empty fieldErrors.password}">
                                        <div class="invalid-feedback">${e:esc(fieldErrors.password)}</div>
                                    </t:if>
                                </div>
                                <t:if test="${not empty targetUser.userId}">
                                    <div class="form-text small">Leave blank to keep the current password.</div>
                                </t:if>
                            </div>

                            <div class="col-md-6">
                                <label for="confirmPassword" class="form-label small fw-semibold">
                                    Confirm password
                                    <t:if test="${empty targetUser.userId}"><span class="text-danger">*</span></t:if>
                                </label>
                                <div class="input-group has-validation">
                                    <input type="password" class="form-control ${not empty fieldErrors.confirmPassword ? 'is-invalid' : ''}"
                                           id="confirmPassword" name="confirmPassword"
                                           ${empty targetUser.userId ? 'required' : ''}
                                           placeholder="Re-enter password to confirm">
                                    <button class="btn btn-outline-secondary toggle-password-btn" type="button"
                                            tabindex="-1" aria-label="Show password" title="Show password">
                                        <svg class="eye-icon eye-open" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8z"></path>
                                            <circle cx="12" cy="12" r="3"></circle>
                                        </svg>
                                        <svg class="eye-icon eye-closed d-none" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                            <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                            <line x1="1" y1="1" x2="23" y2="23"></line>
                                        </svg>
                                    </button>
                                    <t:if test="${not empty fieldErrors.confirmPassword}">
                                        <div class="invalid-feedback">${e:esc(fieldErrors.confirmPassword)}</div>
                                    </t:if>
                                </div>
                            </div>
                        </div>

                        <!-- Staff profile fields for STAFF or ADMIN -->
                        <div id="staffSection" style="${targetUser.roleName == 'STAFF' or targetUser.roleName == 'ADMIN' ? 'display: block;' : 'display: none;'}">
                            <h5 class="card-title mt-4 mb-3 border-bottom pb-2">Staff employment profile</h5>

                            <div class="row g-3 mb-3">
                                <div class="col-md-5">
                                    <label for="position" class="form-label small fw-semibold">Position <span class="text-danger">*</span></label>
                                    <input type="text" class="form-control ${not empty fieldErrors.position ? 'is-invalid' : ''}"
                                           id="position" name="position" maxlength="80"
                                           value="${e:esc(targetUser.position)}" placeholder="e.g. Housekeeper, Receptionist">
                                    <t:if test="${not empty fieldErrors.position}">
                                        <div class="invalid-feedback">${e:esc(fieldErrors.position)}</div>
                                    </t:if>
                                </div>

                                <div class="col-md-4">
                                    <label for="employmentStatus" class="form-label small fw-semibold">Employment status <span class="text-danger">*</span></label>
                                    <select class="form-select ${not empty fieldErrors.employmentStatus ? 'is-invalid' : ''}"
                                            id="employmentStatus" name="employmentStatus">
                                        <option value="ACTIVE" ${targetUser.employmentStatus == 'ACTIVE' ? 'selected' : ''}>Active</option>
                                        <option value="ON_LEAVE" ${targetUser.employmentStatus == 'ON_LEAVE' ? 'selected' : ''}>On leave</option>
                                        <option value="RESIGNED" ${targetUser.employmentStatus == 'RESIGNED' ? 'selected' : ''}>Resigned</option>
                                    </select>
                                    <t:if test="${not empty fieldErrors.employmentStatus}">
                                        <div class="invalid-feedback">${e:esc(fieldErrors.employmentStatus)}</div>
                                    </t:if>
                                </div>

                                <div class="col-md-3">
                                    <label for="hireDate" class="form-label small fw-semibold">Hire date</label>
                                    <input type="date" class="form-control ${not empty fieldErrors.hireDate ? 'is-invalid' : ''}"
                                           id="hireDate" name="hireDate"
                                           value="${e:esc(targetUser.hireDate)}" data-datepicker>
                                    <t:if test="${not empty fieldErrors.hireDate}">
                                        <div class="invalid-feedback">${e:esc(fieldErrors.hireDate)}</div>
                                    </t:if>
                                </div>
                            </div>
                        </div>

                        <div class="d-flex justify-content-end gap-2 mt-4 pt-3 border-top">
                            <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline-secondary">Cancel</a>
                            <button type="submit" class="btn btn-primary">
                                ${empty targetUser.userId ? 'Create user' : 'Save changes'}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>

    <script>
        function updateStaffVisibility(role) {
            var staffSection = document.getElementById('staffSection');
            if (staffSection) {
                if (role === 'STAFF' || role === 'ADMIN') {
                    staffSection.style.display = 'block';
                } else {
                    staffSection.style.display = 'none';
                }
            }
        }
    </script>
</t:layout>
