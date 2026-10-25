<%@ tag pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="test" required="true" type="java.lang.Boolean" %>
<%-- JSTL-free conditional block: renders the body only when test is true. --%>
<%
    Boolean cond = (Boolean) jspContext.getAttribute("test");
    if (cond != null && cond.booleanValue()) {
%>
<jsp:doBody/>
<%
    }
%>
