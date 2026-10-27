<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="roomId" required="true" type="java.lang.Long" %>
<%@ attribute name="active" required="true" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%-- Page-level tabs of a room (Task R8): "Details" | "Slots & Pricing". In create mode (roomId <= 0)
     the Slots tab is disabled: the room must be saved first. --%>
<nav class="ss-tabs ss-rm-tabs" aria-label="Room sections">
    <t:if test="${roomId > 0}">
        <a class="ss-tabs__link ${active == 'details' ? 'ss-tabs__link--active' : ''}" ${active == 'details' ? 'aria-current="page"' : ''}
           href="${pageContext.request.contextPath}/admin/rooms/edit?roomId=${roomId}">Details</a>
        <t:if test="${sessionScope.permissions.contains('ROOM_SLOT_MANAGE')}">
            <a class="ss-tabs__link ${active == 'slots' ? 'ss-tabs__link--active' : ''}" ${active == 'slots' ? 'aria-current="page"' : ''}
               href="${pageContext.request.contextPath}/admin/rooms/slots?roomId=${roomId}">Slots &amp; Pricing</a>
        </t:if>
    </t:if>
    <t:if test="${roomId <= 0}">
        <span class="ss-tabs__link ss-tabs__link--active" aria-current="page">Details</span>
        <span class="ss-tabs__link ss-rm-tabs__off" aria-disabled="true" title="Save the room first"
              data-bs-toggle="tooltip">Slots &amp; Pricing</span>
    </t:if>
</nav>
