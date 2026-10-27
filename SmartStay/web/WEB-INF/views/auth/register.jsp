<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Register" active="register">
    <div class="auth-page-wrapper">
        <div class="auth-card-container-lg">
            <div class="card auth-card">
                <div class="card-body p-4 p-md-5">
                    <div class="text-center mb-3">
                        <span class="auth-icon-badge">
                            <i class="bi bi-person-plus-fill"></i>
                        </span>
                    </div>
                    <h1 class="ss-serif auth-title text-center mb-2">Create an account</h1>
                    <p class="auth-subtitle text-center mb-4">Register as a guest to book rooms and receive digital door PINs.</p>

                    <t:if test="${not empty globalError}">
                        <div class="alert alert-danger py-2 px-3 rounded-3 mb-3" role="alert">${e:esc(globalError)}</div>
                    </t:if>

                    <form action="${pageContext.request.contextPath}/register" method="post" class="needs-validation" novalidate>
                        <input type="hidden" name="_csrf" value="${e:esc(_csrf)}">

                        <div class="mb-3">
                            <label for="fullName" class="form-label auth-label">Full name</label>
                            <input type="text" class="form-control auth-input ${errors['fullName'] != null ? 'is-invalid' : ''}"
                                   id="fullName" name="fullName" required maxlength="100" autocomplete="name"
                                   placeholder="e.g. John Doe"
                                   value="${e:esc(form['fullName'])}">
                            <t:if test="${not empty errors['fullName']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['fullName'])}</div>
                            </t:if>
                            <t:if test="${empty errors['fullName']}">
                                <div class="invalid-feedback">Please enter your full name (max 100 characters).</div>
                            </t:if>
                        </div>

                        <div class="mb-3">
                            <label for="email" class="form-label auth-label">Email address</label>
                            <input type="email" class="form-control auth-input ${errors['email'] != null ? 'is-invalid' : ''}"
                                   id="email" name="email" required maxlength="255" autocomplete="email"
                                   placeholder="name@example.com"
                                   value="${e:esc(form['email'])}">
                            <t:if test="${not empty errors['email']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['email'])}</div>
                            </t:if>
                            <t:if test="${empty errors['email']}">
                                <div class="invalid-feedback">Please enter a valid email address.</div>
                            </t:if>
                        </div>

                        <div class="mb-3">
                            <label for="phone" class="form-label auth-label">Phone number</label>
                            <input type="tel" class="form-control auth-input ${errors['phone'] != null ? 'is-invalid' : ''}"
                                   id="phone" name="phone" required pattern="^(0|\+84)[0-9]{9,10}$"
                                   title="Vietnamese phone format: 0xxxxxxxxx or +84xxxxxxxxx (10-11 digits)"
                                   autocomplete="tel" placeholder="0901234567"
                                   value="${e:esc(form['phone'])}">
                            <t:if test="${not empty errors['phone']}">
                                <div class="invalid-feedback d-block">${e:esc(errors['phone'])}</div>
                            </t:if>
                            <t:if test="${empty errors['phone']}">
                                <div class="invalid-feedback">Please enter a valid phone number (0xxxxxxxxx or +84xxxxxxxxx).</div>
                            </t:if>
                        </div>

                        <div class="mb-3">
                            <label for="password" class="form-label auth-label">Password</label>
                            <div class="input-group auth-input-group has-validation">
                                <input type="password" class="form-control auth-input ${errors['password'] != null ? 'is-invalid' : ''}"
                                       id="password" name="password" required minlength="8" autocomplete="new-password"
                                       placeholder="Minimum 8 characters">
                                <button class="btn btn-outline-secondary toggle-password-btn" type="button"
                                        tabindex="-1" aria-label="Show password" title="Show password">
                                    <svg class="eye-icon eye-open" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                    <svg class="eye-icon eye-closed d-none" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                        <line x1="1" y1="1" x2="23" y2="23"></line>
                                    </svg>
                                </button>
                                <t:if test="${not empty errors['password']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['password'])}</div>
                                </t:if>
                                <t:if test="${empty errors['password']}">
                                    <div class="invalid-feedback">Password must be at least 8 characters and include both letters and digits.</div>
                                </t:if>
                            </div>
                            <div class="form-text small">At least 8 characters with letters and numbers.</div>
                        </div>

                        <div class="mb-4">
                            <label for="confirm" class="form-label auth-label">Confirm password</label>
                            <div class="input-group auth-input-group has-validation">
                                <input type="password" class="form-control auth-input ${errors['confirm'] != null ? 'is-invalid' : ''}"
                                       id="confirm" name="confirm" required autocomplete="new-password"
                                       data-match="password" data-match-message="Passwords do not match."
                                       placeholder="Re-enter your password">
                                <button class="btn btn-outline-secondary toggle-password-btn" type="button"
                                        tabindex="-1" aria-label="Show password" title="Show password">
                                    <svg class="eye-icon eye-open" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                        <circle cx="12" cy="12" r="3"></circle>
                                    </svg>
                                    <svg class="eye-icon eye-closed d-none" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                                        <line x1="1" y1="1" x2="23" y2="23"></line>
                                    </svg>
                                </button>
                                <t:if test="${not empty errors['confirm']}">
                                    <div class="invalid-feedback d-block">${e:esc(errors['confirm'])}</div>
                                </t:if>
                                <t:if test="${empty errors['confirm']}">
                                    <div class="invalid-feedback">Passwords do not match.</div>
                                </t:if>
                            </div>
                        </div>

                        <div class="d-grid mb-4">
                            <button type="submit" class="btn btn-auth-submit">
                                <span>Create account</span>
                                <i class="bi bi-arrow-right"></i>
                            </button>
                        </div>

                        <div class="text-center auth-footer-prompt">
                            Already have an account?
                            <a href="${pageContext.request.contextPath}/login" class="auth-footer-link">Log in</a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</t:layout>
