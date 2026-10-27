<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="booking" required="true" type="java.lang.Object" %>
<%@ attribute name="cssClass" required="false" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%-- 48-hour cancellation line (Task R3): "Free cancellation until dd/MM/yyyy HH:mm" or a
     "Non-refundable" badge. Uses the virtual clock through e:cancellationInfo. --%>
<%
    util.CancellationPolicy.Info ci = util.ElFunctions.cancellationInfo(jspContext.getAttribute("booking"));
    jspContext.setAttribute("ci", ci);
    if (ci.isKnown()) {
        if (ci.isRefundable()) {
%>
<p class="ss-cancel-line ss-cancel-line--free ${e:esc(cssClass)}">
    <i class="bi bi-calendar-check ss-cancel-line__icon" aria-hidden="true"></i>
    <span>Free cancellation until <span class="ss-cancel-line__time">${e:datetime(ci.deadline)}</span></span>
</p>
<%
        } else {
%>
<p class="ss-cancel-line ss-cancel-line--locked ${e:esc(cssClass)}">
    <span class="ss-badge ss-badge--warning">Non-refundable</span>
    <span>Check-in is less than ${ci.freeHours} hours away, so this booking cannot be refunded.</span>
</p>
<%
        }
    }
%>
