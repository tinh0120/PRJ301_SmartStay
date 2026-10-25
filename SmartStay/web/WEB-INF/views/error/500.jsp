<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Server Error">
    <div class="text-center py-5">
        <h1 class="display-1 fw-bold text-danger">500</h1>
        <h2 class="h3 fw-semibold mb-3">Something went wrong</h2>
        <p class="text-muted mb-4">An unexpected error occurred. Please try again later.</p>
        <a href="${pageContext.request.contextPath}/" class="btn btn-primary px-4">Back to home</a>
    </div>
</t:layout>
