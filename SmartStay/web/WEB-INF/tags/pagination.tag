<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="result" required="true" type="util.PageResult" %>
<%@ attribute name="baseUrl" required="true" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ tag import="util.PageResult, util.ElFunctions" %>
<%
    PageResult<?> pr = (PageResult<?>) jspContext.getAttribute("result");
    if (pr != null) {
%>
    <div class="d-flex flex-column flex-md-row justify-content-between align-items-md-center my-3 gap-2">
        <p class="text-muted small mb-0"><%= ElFunctions.esc(pr.getSummary()) %></p>
        <%
            if (pr.getTotalPages() > 1) {
                int cur = pr.getPage();
                int totalPages = pr.getTotalPages();
                String qs = pr.getRequest().toQueryString("page");
                String separator = qs.isEmpty() ? "?" : "?" + qs + "&";
        %>
            <nav aria-label="Pagination">
                <ul class="pagination pagination-sm flex-wrap mb-0">
                    <%-- Previous link --%>
                    <% if (pr.isHasPrevious()) { %>
                        <li class="page-item">
                            <a class="page-link" href="<%= ElFunctions.esc(baseUrl + separator + "page=" + (cur - 1)) %>" aria-label="Previous">&laquo;</a>
                        </li>
                    <% } else { %>
                        <li class="page-item disabled">
                            <span class="page-link">&laquo;</span>
                        </li>
                    <% } %>

                    <%
                        int start = Math.max(1, cur - 2);
                        int end = Math.min(totalPages, cur + 2);

                        if (start > 1) {
                    %>
                        <li class="page-item <%= (cur == 1) ? "active" : "" %>">
                            <a class="page-link" href="<%= ElFunctions.esc(baseUrl + separator + "page=1") %>">1</a>
                        </li>
                        <% if (start > 2) { %>
                            <li class="page-item disabled"><span class="page-link">&hellip;</span></li>
                        <% } %>
                    <%
                        }

                        for (int p = start; p <= end; p++) {
                            if (p == cur) {
                    %>
                        <li class="page-item active" aria-current="page">
                            <span class="page-link"><%= p %></span>
                        </li>
                    <%      } else { %>
                        <li class="page-item">
                            <a class="page-link" href="<%= ElFunctions.esc(baseUrl + separator + "page=" + p) %>"><%= p %></a>
                        </li>
                    <%      }
                        }

                        if (end < totalPages) {
                            if (end < totalPages - 1) {
                    %>
                            <li class="page-item disabled"><span class="page-link">&hellip;</span></li>
                        <%  } %>
                        <li class="page-item <%= (cur == totalPages) ? "active" : "" %>">
                            <a class="page-link" href="<%= ElFunctions.esc(baseUrl + separator + "page=" + totalPages) %>"><%= totalPages %></a>
                        </li>
                    <%  } %>

                    <%-- Next link --%>
                    <% if (pr.isHasNext()) { %>
                        <li class="page-item">
                            <a class="page-link" href="<%= ElFunctions.esc(baseUrl + separator + "page=" + (cur + 1)) %>" aria-label="Next">&raquo;</a>
                        </li>
                    <% } else { %>
                        <li class="page-item disabled">
                            <span class="page-link">&raquo;</span>
                        </li>
                    <% } %>
                </ul>
            </nav>
        <%  } %>
    </div>
<% } %>
