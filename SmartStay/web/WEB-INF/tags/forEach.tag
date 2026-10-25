<%@ tag pageEncoding="UTF-8" body-content="scriptless" %>
<%@ attribute name="items" required="true" type="java.lang.Object" %>
<%@ attribute name="var" required="true" type="java.lang.String" rtexprvalue="false" %>
<%@ variable name-from-attribute="var" alias="item" scope="NESTED" %>
<%@ variable name-given="loop" scope="NESTED" %>
<%
    Object collection = jspContext.getAttribute("items");
    if (collection instanceof java.lang.Iterable) {
        int index = 0;
        java.util.Iterator<?> it = ((java.lang.Iterable<?>) collection).iterator();
        while (it.hasNext()) {
            Object elem = it.next();
            jspContext.setAttribute("item", elem);
            java.util.Map<String, Object> loopMap = new java.util.HashMap<String, Object>();
            loopMap.put("index", index);
            loopMap.put("count", index + 1);
            loopMap.put("first", index == 0);
            loopMap.put("last", !it.hasNext());
            jspContext.setAttribute("loop", loopMap);
            index++;
%>
<jsp:doBody/>
<%
        }
    }
%>
