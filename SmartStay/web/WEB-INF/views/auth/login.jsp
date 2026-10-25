<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Log in" active="login">
    <div class="auth-page-wrapper">
        <div class="auth-card-container">
            <div class="card auth-card">
                <div class="card-body p-4 p-md-5">
                    <div class="text-center mb-3">
                        <span class="auth-icon-badge">
                            <i class="bi bi-door-open-fill"></i>
                        </span>
                    </div>
                    <h1 class="auth-title text-center mb-2">Welcome back</h1>
                    <p class="auth-subtitle text-center mb-4">Log in to your <strong>Tini SmartStay</strong> account.</p>

                    <t:if test="${not empty globalError}">
                        <div class="alert alert-danger py-2 px-3 rounded-3 mb-3" role="alert">${e:esc(globalError)}</div>
                    </t:if>

                    <form action="${pageContext.request.contextPath}/login" method="post" class="needs-validation" novalidate>
                        <input type="hidden" name="_csrf" value="${e:esc(_csrf)}">
                        <t:if test="${not empty next}">
                            <input type="hidden" name="next" value="${e:esc(next)}">
                        </t:if>

                        <div class="mb-3">
                            <label for="email" class="form-label auth-label">Email address</label>
                            <input type="email" class="form-control auth-input"
                                   id="email" name="email" required maxlength="255" autocomplete="email"
                                   placeholder="name@example.com"
                                   value="${e:esc(form['email'])}">
                            <div class="invalid-feedback">Please enter your email address.</div>
                        </div>

                        <div class="mb-4">
                            <label for="password" class="form-label auth-label">Password</label>
                            <div class="input-group auth-input-group has-validation">
                                <input type="password" class="form-control auth-input"
                                       id="password" name="password" required autocomplete="current-password"
                                       placeholder="Enter your password">
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
                                <div class="invalid-feedback">Please enter your password.</div>
                            </div>
                        </div>

                        <div class="d-grid mb-4">
                            <button type="submit" class="btn btn-auth-submit">
                                <span>Log in</span>
                                <i class="bi bi-arrow-right"></i>
                            </button>
                        </div>

                        <div class="text-center auth-footer-prompt">
                            Don't have an account?
                            <a href="${pageContext.request.contextPath}/register" class="auth-footer-link">Register</a>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</t:layout>
