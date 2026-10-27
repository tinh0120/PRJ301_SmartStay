<%@ tag pageEncoding="UTF-8" body-content="empty" %>
<%@ attribute name="detail" required="true" type="java.lang.Object" %>
<%@ attribute name="amount" required="true" type="java.lang.Object" %>
<%@ attribute name="roomNumber" required="true" %>
<%@ attribute name="bookingCode" required="true" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%-- Checkout "Booking reservation" summary (Task R5): room + code, ONE stay line, slot names
     as plain text, a small receipt and the 48-hour cancellation line. --%>
<div class="ss-reservation">
    <div class="ss-reservation__head">
        <div>
            <p class="ss-reservation__room">${e:esc(e:roomName(roomNumber))}</p>
            <p class="ss-reservation__room-code">Room ${e:esc(roomNumber)}</p>
        </div>
        <span class="ss-reservation__code">${e:esc(bookingCode)}</span>
    </div>
    <t:if test="${not empty detail && not empty detail.booking}">
        <p class="ss-reservation__when">${e:esc(e:stayLine(detail.booking.checkinAt, detail.booking.checkoutAt))}</p>
        <p class="ss-reservation__slots">${e:esc(e:slotNames(detail.slots))}</p>
    </t:if>
    <dl class="ss-reservation__receipt">
        <t:if test="${not empty detail && not empty detail.booking && detail.booking.discountAmount > 0}">
            <div class="ss-reservation__line">
                <dt>Base price</dt>
                <dd>${e:money(detail.booking.subtotal)}</dd>
            </div>
            <div class="ss-reservation__line">
                <dt>Discount (&minus;${detail.booking.discountPercent}%)</dt>
                <dd>&minus;${e:money(detail.booking.discountAmount)}</dd>
            </div>
        </t:if>
        <div class="ss-reservation__line ss-reservation__line--total">
            <dt>Total due</dt>
            <dd>${e:money(amount)}</dd>
        </div>
    </dl>
    <t:if test="${not empty detail}">
        <t:cancelLine booking="${detail}" cssClass="mb-0"/>
    </t:if>
</div>
