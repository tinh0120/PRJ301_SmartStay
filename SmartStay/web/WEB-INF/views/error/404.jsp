<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Page Not Found">
    <div class="text-center py-5">
        <h1 class="display-1 fw-bold text-secondary">404</h1>
        <h2 class="h3 fw-semibold mb-3">Page not found</h2>
        <p class="text-muted mb-4">The page you requested does not exist.</p>
        <a href="${pageContext.request.contextPath}/" class="btn btn-primary px-4">Back to home</a>
    </div>
</t:layout>
