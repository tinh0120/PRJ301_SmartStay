<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Access Denied">
    <div class="text-center py-5">
        <h1 class="display-1 fw-bold text-danger">403</h1>
        <h2 class="h3 fw-semibold mb-3">Access denied</h2>
        <p class="text-muted mb-4">You do not have permission to view this page.</p>
        <a href="${pageContext.request.contextPath}/" class="btn btn-primary px-4">Back to home</a>
    </div>
</t:layout>
