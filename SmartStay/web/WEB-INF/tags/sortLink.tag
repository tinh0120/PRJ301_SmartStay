<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="result" required="true" type="util.PageResult" %>
<%@ attribute name="baseUrl" required="true" %>
<%@ attribute name="column" required="true" %>
<%@ attribute name="label" required="true" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ tag import="util.PageResult, util.PageResult.PageRequest, util.ElFunctions" %>
<%
    PageResult<?> pr = (PageResult<?>) jspContext.getAttribute("result");
    PageRequest req = (pr != null) ? pr.getRequest() : null;

    String currentSort = (req != null) ? req.getSort() : null;
    String currentDir = (req != null) ? req.getDir() : "ASC";
    boolean isActive = (currentSort != null && currentSort.equalsIgnoreCase(column));

    String nextDir = "ASC";
    if (isActive) {
        nextDir = "ASC".equalsIgnoreCase(currentDir) ? "DESC" : "ASC";
    }

    String baseQs = (req != null) ? req.toQueryString("page", "sort", "dir") : "";
    String separator = baseQs.isEmpty() ? "?" : "?" + baseQs + "&";
    String targetUrl = baseUrl + separator + "sort=" + column + "&dir=" + nextDir + "&page=1";
    String displayLabel = (label != null) ? label.replace("&amp;", "&") : "";
%>
<a href="<%= ElFunctions.esc(targetUrl) %>" class="sort-pill-btn text-decoration-none <%= isActive ? "active" : "" %> fw-semibold d-inline-flex align-items-center gap-1">
    <span><%= ElFunctions.esc(displayLabel) %></span>
    <% if (isActive) { %>
        <% if ("ASC".equalsIgnoreCase(currentDir)) { %>
            <span aria-hidden="true">&#9650;</span>
            <span class="visually-hidden">(sorted ascending)</span>
        <% } else { %>
            <span aria-hidden="true">&#9660;</span>
            <span class="visually-hidden">(sorted descending)</span>
        <% } %>
    <% } %>
</a>
