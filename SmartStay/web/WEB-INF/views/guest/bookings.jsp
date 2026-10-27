<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<%
    if (request.getAttribute("result") == null && request.getAttribute("pageResult") != null) {
        request.setAttribute("result", request.getAttribute("pageResult"));
    }
    if (request.getAttribute("statuses") == null && request.getAttribute("statusOptions") != null) {
        request.setAttribute("statuses", request.getAttribute("statusOptions"));
    }
%>
<t:layout title="My bookings" active="bookings">
    <!-- Boutique Breadcrumb -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Home
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">My bookings</li>
        </ol>
    </nav>

    <!-- Header Section -->
    <div class="bookings-header mb-4">
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
            <div>
                <div class="d-flex align-items-center gap-2 mb-1">
                    <span class="bookings-header-icon">
                        <i class="bi bi-journal-bookmark-fill"></i>
                    </span>
                    <h1 class="h3 fw-bold mb-0 text-espresso">My bookings</h1>
                </div>
                <p class="text-muted small mb-0">View and manage your room reservations, access credentials, and payment status.</p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/rooms" class="btn btn-primary rounded-pill px-4 py-2 fw-semibold d-inline-flex align-items-center gap-2 shadow-sm">
                    <i class="bi bi-plus-circle"></i>Book another room
                </a>
            </div>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger py-2 rounded-4 mb-4" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <%-- Round 3, item 4: four tabs replace the five-field filter form. Unpaid holds that expired
         are listed only under "All". --%>
    <nav class="ss-tabs ss-bookings-tabs" aria-label="My bookings">
        <a class="ss-tabs__link ${pr.getFilter('view') == 'upcoming' ? 'ss-tabs__link--active' : ''}"
           href="${pageContext.request.contextPath}/guest/bookings?view=upcoming" ${pr.getFilter('view') == 'upcoming' ? 'aria-current="page"' : ''}>
            Upcoming <span class="ss-bk-tabs__count">${empty viewCounts ? 0 : viewCounts['upcoming']}</span>
        </a>
        <a class="ss-tabs__link ${pr.getFilter('view') == 'past' ? 'ss-tabs__link--active' : ''}"
           href="${pageContext.request.contextPath}/guest/bookings?view=past" ${pr.getFilter('view') == 'past' ? 'aria-current="page"' : ''}>
            Past stays <span class="ss-bk-tabs__count">${empty viewCounts ? 0 : viewCounts['past']}</span>
        </a>
        <a class="ss-tabs__link ${pr.getFilter('view') == 'cancelled' ? 'ss-tabs__link--active' : ''}"
           href="${pageContext.request.contextPath}/guest/bookings?view=cancelled" ${pr.getFilter('view') == 'cancelled' ? 'aria-current="page"' : ''}>
            Cancelled <span class="ss-bk-tabs__count">${empty viewCounts ? 0 : viewCounts['cancelled']}</span>
        </a>
        <a class="ss-tabs__link ${pr.getFilter('view') == 'all' ? 'ss-tabs__link--active' : ''}"
           href="${pageContext.request.contextPath}/guest/bookings?view=all" ${pr.getFilter('view') == 'all' ? 'aria-current="page"' : ''}>
            All <span class="ss-bk-tabs__count">${empty viewCounts ? 0 : viewCounts['all']}</span>
        </a>
    </nav>

    <%-- R5: one friendly state per booking, one action per row, cards on mobile. The pagination
         below already shows "Showing x-y of z", so the duplicate summary badge is gone. --%>

    <t:if test="${empty result.items}">
        <div class="ss-empty ss-bookings-empty" role="status">
            <i class="bi bi-calendar-x ss-empty__icon" aria-hidden="true"></i>
            <p class="ss-empty__title">${pr.getFilter('view') == 'upcoming' ? 'No upcoming stays' : (pr.getFilter('view') == 'past' ? 'No past stays yet' : (pr.getFilter('view') == 'cancelled' ? 'No cancelled bookings' : 'No bookings yet'))}</p>
            <p class="ss-empty__desc">Pick a room and a time slot to reserve your next stay.</p>
            <a href="${pageContext.request.contextPath}/rooms" class="btn btn-primary rounded-pill px-4 btn-sm fw-semibold">Explore rooms</a>
        </div>
    </t:if>

    <t:if test="${not empty result.items}">
        <!-- Desktop / tablet table (>= 768px) -->
        <div class="card bookings-table-card ss-bookings-table border-0 mb-4 d-none d-md-block">
            <div class="table-responsive">
                <table class="table bookings-table align-middle mb-0">
                    <thead>
                        <tr>
                            <th scope="col">
                                <t:sortLink column="date" label="Booking" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                            </th>
                            <th scope="col">
                                <t:sortLink column="room" label="Room" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                            </th>
                            <th scope="col">
                                <t:sortLink column="date" label="Date &amp; time" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                            </th>
                            <th scope="col" class="text-end">
                                <t:sortLink column="total" label="Total" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                            </th>
                            <th scope="col">
                                <t:sortLink column="status" label="Status" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                            </th>
                            <th scope="col" class="text-end pe-4"><span class="visually-hidden">Action</span></th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:forEach items="${result.items}" var="b">
                            <tr class="booking-row">
                                <td>
                                    <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${b.bookingId}" class="ss-bookings__code">${e:esc(b.bookingCode)}</a>
                                </td>
                                <td>
                                    <div class="fw-semibold text-espresso">${e:esc(b.roomTitle)}</div>
                                    <div class="ss-bookings__muted">Room ${e:esc(b.roomNumber)}</div>
                                </td>
                                <td>
                                    <div class="ss-bookings__date">${e:date(b.slotDate)}</div>
                                    <div class="ss-bookings__slot">${e:esc(e:slotNames(b.slotsSummary))} &middot; ${e:esc(e:stayTimes(b.checkinAt, b.checkoutAt))}</div>
                                </td>
                                <td class="text-end">
                                    <span class="ss-bookings__total">${e:money(b.total)}</span>
                                </td>
                                <td>
                                    <span class="ss-badge ss-badge--${e:guestState(b).tone}"
                                          ${b.status == 'CHECKED_IN' && b.earlyCheckin ? 'title="Checked in early"' : ''}>${e:esc(e:guestState(b).label)}</span>
                                </td>
                                <td class="text-end pe-4">
                                    <t:if test="${e:guestState(b).payAction}">
                                        <form action="${pageContext.request.contextPath}/guest/bookings/pay" method="post" class="d-inline m-0">
                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                            <input type="hidden" name="id" value="${b.bookingId}">
                                            <button type="submit" class="btn btn-sm btn-primary rounded-pill px-3 ss-bookings__action">${e:esc(e:guestState(b).actionLabel)}</button>
                                        </form>
                                    </t:if>
                                    <t:if test="${e:guestState(b).pinAction}">
                                        <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${b.bookingId}#booking-pin"
                                           class="btn btn-sm btn-outline-soft rounded-pill px-3 ss-bookings__action">${e:esc(e:guestState(b).actionLabel)}</a>
                                    </t:if>
                                    <t:if test="${!e:guestState(b).payAction && !e:guestState(b).pinAction}">
                                        <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${b.bookingId}"
                                           class="btn btn-sm btn-outline-soft rounded-pill px-3 ss-bookings__action">${e:esc(e:guestState(b).actionLabel)}</a>
                                    </t:if>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- Mobile cards (< 768px) -->
        <ul class="ss-bookings-cards d-md-none">
            <t:forEach items="${result.items}" var="b">
                <li class="ss-bookings-card">
                    <div class="ss-bookings-card__top">
                        <div>
                            <div class="ss-bookings-card__room">${e:esc(b.roomTitle)}</div>
                            <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${b.bookingId}" class="ss-bookings__code">${e:esc(b.bookingCode)}</a>
                        </div>
                        <span class="ss-badge ss-badge--${e:guestState(b).tone}">${e:esc(e:guestState(b).label)}</span>
                    </div>
                    <div class="ss-bookings-card__when">
                        <span class="ss-bookings__date">${e:date(b.slotDate)}</span>
                        <span class="ss-bookings__slot">${e:esc(e:slotNames(b.slotsSummary))} &middot; ${e:esc(e:stayTimes(b.checkinAt, b.checkoutAt))}</span>
                    </div>
                    <div class="ss-bookings-card__bottom">
                        <span class="ss-bookings__total">${e:money(b.total)}</span>
                        <t:if test="${e:guestState(b).payAction}">
                            <form action="${pageContext.request.contextPath}/guest/bookings/pay" method="post" class="m-0">
                                <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                <input type="hidden" name="id" value="${b.bookingId}">
                                <button type="submit" class="btn btn-primary rounded-pill px-4 ss-bookings__action">${e:esc(e:guestState(b).actionLabel)}</button>
                            </form>
                        </t:if>
                        <t:if test="${e:guestState(b).pinAction}">
                            <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${b.bookingId}#booking-pin"
                               class="btn btn-outline-soft rounded-pill px-4 ss-bookings__action">${e:esc(e:guestState(b).actionLabel)}</a>
                        </t:if>
                        <t:if test="${!e:guestState(b).payAction && !e:guestState(b).pinAction}">
                            <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${b.bookingId}"
                               class="btn btn-outline-soft rounded-pill px-4 ss-bookings__action">${e:esc(e:guestState(b).actionLabel)}</a>
                        </t:if>
                    </div>
                </li>
            </t:forEach>
        </ul>
    </t:if>

    <!-- Pagination (hidden when the tab is empty: the empty state already says so) -->
    <t:if test="${not empty result.items}">
        <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
    </t:if>
</t:layout>
