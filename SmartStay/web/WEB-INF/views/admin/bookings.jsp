<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Bookings" active="admin-bookings">
    <%-- R7: tabs with real counts, toolbar (search + chips + More filters), one badge and one visible
         action per row, secondary actions in the ⋯ menu. --%>
    <div class="ss-page-header">
        <div>
            <h1 class="ss-page-header__title">Bookings</h1>
            <p class="ss-page-header__subtitle">Reservations, payments and stays.</p>
        </div>
        <a href="${pageContext.request.contextPath}/admin/bookings/edit" class="btn btn-primary btn-sm rounded-pill px-3">
            <i class="bi bi-plus-lg me-1" aria-hidden="true"></i>Walk-in booking
        </a>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger py-2" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Status tabs: counts come from the database, independent of the search below -->
    <nav class="ss-tabs ss-bk-tabs" aria-label="Booking status">
        <a class="ss-tabs__link ${empty pr.getFilter('status') ? 'ss-tabs__link--active' : ''}"
           href="${pageContext.request.contextPath}/admin/bookings?${tabBaseQs}" ${empty pr.getFilter('status') ? 'aria-current="page"' : ''}>
            All <span class="ss-bk-tabs__count">${statusCounts['ALL']}</span>
        </a>
        <t:if test="${statusCounts['PENDING_PAYMENT'] > 0 || pr.getFilter('status') == 'PENDING_PAYMENT'}">
            <a class="ss-tabs__link ${pr.getFilter('status') == 'PENDING_PAYMENT' ? 'ss-tabs__link--active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?${tabBaseQs}&amp;status=PENDING_PAYMENT">
                Awaiting payment <span class="ss-bk-tabs__count">${statusCounts['PENDING_PAYMENT']}</span>
            </a>
        </t:if>
        <t:if test="${statusCounts['CONFIRMED'] > 0 || pr.getFilter('status') == 'CONFIRMED'}">
            <a class="ss-tabs__link ${pr.getFilter('status') == 'CONFIRMED' ? 'ss-tabs__link--active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?${tabBaseQs}&amp;status=CONFIRMED">
                Confirmed <span class="ss-bk-tabs__count">${statusCounts['CONFIRMED']}</span>
            </a>
        </t:if>
        <t:if test="${statusCounts['CHECKED_IN'] > 0 || pr.getFilter('status') == 'CHECKED_IN'}">
            <a class="ss-tabs__link ${pr.getFilter('status') == 'CHECKED_IN' ? 'ss-tabs__link--active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?${tabBaseQs}&amp;status=CHECKED_IN">
                Checked in <span class="ss-bk-tabs__count">${statusCounts['CHECKED_IN']}</span>
            </a>
        </t:if>
        <t:if test="${statusCounts['CHECKED_OUT'] > 0 || pr.getFilter('status') == 'CHECKED_OUT'}">
            <a class="ss-tabs__link ${pr.getFilter('status') == 'CHECKED_OUT' ? 'ss-tabs__link--active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?${tabBaseQs}&amp;status=CHECKED_OUT">
                Completed <span class="ss-bk-tabs__count">${statusCounts['CHECKED_OUT']}</span>
            </a>
        </t:if>
        <t:if test="${statusCounts['CANCELLED'] > 0 || pr.getFilter('status') == 'CANCELLED'}">
            <a class="ss-tabs__link ${pr.getFilter('status') == 'CANCELLED' ? 'ss-tabs__link--active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?${tabBaseQs}&amp;status=CANCELLED">
                Cancelled <span class="ss-bk-tabs__count">${statusCounts['CANCELLED']}</span>
            </a>
        </t:if>
        <t:if test="${statusCounts['EXPIRED'] > 0 || pr.getFilter('status') == 'EXPIRED'}">
            <a class="ss-tabs__link ${pr.getFilter('status') == 'EXPIRED' ? 'ss-tabs__link--active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?${tabBaseQs}&amp;status=EXPIRED">
                Expired <span class="ss-bk-tabs__count">${statusCounts['EXPIRED']}</span>
            </a>
        </t:if>
    </nav>

    <!-- Toolbar: search + quick chips + More filters (collapsed) -->
    <form action="${pageContext.request.contextPath}/admin/bookings" method="get" class="ss-toolbar" id="bkToolbar">
        <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
        <input type="hidden" name="dir" value="${e:esc(pr.dir)}">
        <input type="hidden" name="status" value="${e:esc(pr.getFilter('status'))}">
        <input type="hidden" name="chip" value="${e:esc(activeChip)}">

        <div class="ss-toolbar__main">
            <div class="ss-toolbar__search">
                <i class="bi bi-search ss-toolbar__search-icon" aria-hidden="true"></i>
                <label for="keyword" class="visually-hidden">Search bookings</label>
                <input type="search" class="ss-toolbar__search-input" id="keyword" name="keyword"
                       placeholder="Code, guest name, email or room" maxlength="100" value="${e:esc(pr.keyword)}">
            </div>
            <div class="ss-toolbar__chips" role="group" aria-label="Quick filters">
                <a class="ss-chip ${activeChip == 'today' ? 'ss-chip--active' : ''}" ${activeChip == 'today' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/bookings?${chipBaseQs}${activeChip == 'today' ? '' : '&chip=today'}">Today</a>
                <a class="ss-chip ${activeChip == 'upcoming' ? 'ss-chip--active' : ''}" ${activeChip == 'upcoming' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/bookings?${chipBaseQs}${activeChip == 'upcoming' ? '' : '&chip=upcoming'}">Upcoming</a>
                <a class="ss-chip ${activeChip == 'refund' ? 'ss-chip--active' : ''}" ${activeChip == 'refund' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/bookings?${chipBaseQs}${activeChip == 'refund' ? '' : '&chip=refund'}">Needs refund</a>
                <a class="ss-chip ${activeChip == 'noshow' ? 'ss-chip--active' : ''}" ${activeChip == 'noshow' ? 'aria-current="true"' : ''}
                   href="${pageContext.request.contextPath}/admin/bookings?${chipBaseQs}${activeChip == 'noshow' ? '' : '&chip=noshow'}">No-show</a>
                <t:if test="${activeChip == 'checkins_today' || activeChip == 'checkouts_today'}">
                    <a class="ss-chip ss-chip--active" aria-current="true" href="${pageContext.request.contextPath}/admin/bookings?${chipBaseQs}">
                        ${activeChip == 'checkins_today' ? 'Check-ins today' : 'Check-outs today'}
                        <i class="bi bi-x" aria-hidden="true"></i><span class="visually-hidden">Remove filter</span>
                    </a>
                </t:if>
            </div>
            <button class="ss-toolbar__more-toggle" type="button" data-bs-toggle="collapse" data-bs-target="#bkMoreFilters"
                    aria-expanded="${moreFiltersOpen ? 'true' : 'false'}" aria-controls="bkMoreFilters">
                <i class="bi bi-sliders" aria-hidden="true"></i>More filters
            </button>
        </div>

        <div class="collapse ${moreFiltersOpen ? 'show' : ''}" id="bkMoreFilters">
            <div class="ss-toolbar__collapse ss-bk-more">
                <div class="ss-bk-more__field">
                    <label for="payment_status" class="ss-bk-more__label">Payment</label>
                    <select class="form-select form-select-sm" id="payment_status" name="payment_status">
                        <option value="">Any payment</option>
                        <option value="PAID" ${pr.getFilter('payment_status') == 'PAID' ? 'selected' : ''}>Paid</option>
                        <option value="PENDING" ${pr.getFilter('payment_status') == 'PENDING' ? 'selected' : ''}>Waiting for transfer</option>
                        <option value="REVIEW_REQUIRED" ${pr.getFilter('payment_status') == 'REVIEW_REQUIRED' ? 'selected' : ''}>Needs review</option>
                        <option value="REFUND_PENDING" ${pr.getFilter('payment_status') == 'REFUND_PENDING' ? 'selected' : ''}>Refund pending</option>
                        <option value="REFUNDED" ${pr.getFilter('payment_status') == 'REFUNDED' ? 'selected' : ''}>Refunded</option>
                        <option value="FAILED" ${pr.getFilter('payment_status') == 'FAILED' ? 'selected' : ''}>Failed</option>
                        <option value="EXPIRED" ${pr.getFilter('payment_status') == 'EXPIRED' ? 'selected' : ''}>Link expired</option>
                    </select>
                </div>
                <div class="ss-bk-more__field">
                    <label for="room" class="ss-bk-more__label">Room</label>
                    <select class="form-select form-select-sm" id="room" name="room">
                        <option value="">Any room</option>
                        <t:forEach items="${rooms}" var="rm">
                            <option value="${rm.roomId}" ${pr.getFilter('room') == rm.roomId or pr.getFilter('room') == rm.roomNumber ? 'selected' : ''}>Room ${e:esc(rm.roomNumber)}</option>
                        </t:forEach>
                    </select>
                </div>
                <div class="ss-bk-more__field">
                    <label for="from" class="ss-bk-more__label">From</label>
                    <input type="date" class="form-control form-control-sm" id="from" name="from" data-datepicker value="${e:esc(pr.getFilter('from'))}">
                </div>
                <div class="ss-bk-more__field">
                    <label for="to" class="ss-bk-more__label">To</label>
                    <input type="date" class="form-control form-control-sm" id="to" name="to" data-datepicker value="${e:esc(pr.getFilter('to'))}">
                </div>
                <div class="form-check ss-bk-more__check">
                    <input class="form-check-input" type="checkbox" id="noshow_held" name="noshow_held" value="1"
                           ${pr.getFilter('noshow_held') == '1' or pr.getFilter('noshow_held') == 'true' ? 'checked' : ''}>
                    <label class="form-check-label" for="noshow_held">No-show still holding slots</label>
                </div>
                <div class="ss-bk-more__actions">
                    <button type="submit" class="btn btn-primary btn-sm px-3">Apply</button>
                    <a href="${pageContext.request.contextPath}/admin/bookings" class="btn btn-outline-soft btn-sm px-3">Clear all</a>
                </div>
            </div>
        </div>
    </form>

    <!-- Bookings table -->
    <div class="ss-bk-table" id="bkTable">
        <t:if test="${empty pageResult.items && empty dbError}">
            <div class="ss-empty">
                <i class="bi bi-calendar-x ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No bookings match</p>
                <p class="ss-empty__desc">Try another tab, remove a quick filter or clear the search.</p>
                <a href="${pageContext.request.contextPath}/admin/bookings" class="btn btn-outline-soft btn-sm rounded-pill px-3">Clear all filters</a>
            </div>
        </t:if>

        <t:if test="${not empty pageResult.items}">
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table">
                    <thead>
                        <tr>
                            <th scope="col" class="ps-3">
                                <t:sortLink column="guest" label="Booking" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                            </th>
                            <th scope="col">
                                <t:sortLink column="room" label="Room" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                            </th>
                            <th scope="col">
                                <t:sortLink column="date" label="When" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                            </th>
                            <th scope="col" class="text-end">
                                <t:sortLink column="total" label="Total" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                            </th>
                            <th scope="col">
                                <t:sortLink column="status" label="Status" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                            </th>
                            <th scope="col" class="pe-3 text-end"><span class="visually-hidden">Actions</span></th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:forEach items="${pageResult.items}" var="b">
                            <%-- V3: click / Enter on the row opens the shared detail drawer (activity.js) --%>
                            <tr class="ss-act__row" tabindex="0" data-act-row
                                data-d-state="${e:esc(e:adminState(b).label)}"
                                data-d-guest="${e:esc(b.guestName)}"
                                data-d-email="${e:esc(b.guestEmail)}"
                                data-d-room="Room ${e:esc(b.roomNumber)} ${e:esc(b.roomTitle)}"
                                data-d-when="${e:esc(e:slotLine(b.checkinAt, b.checkoutAt, b.slotsSummary))}"
                                data-d-total="${e:money(not empty b.totalAmount ? b.totalAmount : b.total)}${b.discountPercent > 0 ? ' (combo -' += b.discountPercent += '%)' : ''}"
                                data-d-payment="${e:esc(e:humanStatus(b.paymentStatus))}"
                                data-d-code="${e:esc(b.bookingCode)}"
                                data-d-created="${empty b.createdAt ? '' : e:datetime(b.createdAt)}"
                                data-d-href="${pageContext.request.contextPath}/admin/bookings/detail?id=${b.bookingId}">
                                <%-- Item 3: one fact per cell; email, slot names and discount are in the drawer --%>
                                <td class="ps-3">
                                    <div class="ss-bk__guest" title="${e:esc(b.guestEmail)}">${e:esc(b.guestName)}</div>
                                    <div class="ss-bk__sub">
                                        <span class="ss-bk__code">${e:esc(b.bookingCode)}</span>
                                        <%-- V4: paid / checked-out bookings keep HELD slot rows too; the lock is only for no-shows --%>
                                        <t:if test="${b.hasHeldSlots && b.status == 'EXPIRED' && b.expireReason == 'NO_SHOW'}">
                                            <i class="bi bi-lock-fill ss-bk__lock" title="Slots are still held after a no-show" aria-hidden="true"></i>
                                            <span class="visually-hidden">Slots are still held after a no-show</span>
                                        </t:if>
                                    </div>
                                </td>
                                <td class="text-nowrap">
                                    <span class="ss-bk__room">${e:esc(b.roomNumber)}</span>
                                    <span class="ss-bk__sub">${e:esc(b.roomTitle)}</span>
                                </td>
                                <td class="text-nowrap">
                                    <span class="ss-bk__when" title="${e:esc(e:slotNames(b.slotsSummary))}">${e:esc(e:stayLine(b.checkinAt, b.checkoutAt))}</span>
                                </td>
                                <td class="text-end text-nowrap">
                                    <span class="ss-bk__total">${e:money(not empty b.totalAmount ? b.totalAmount : b.total)}</span>
                                </td>
                                <td>
                                    <span class="ss-badge ss-badge--${e:adminState(b).tone}">${e:esc(e:adminState(b).label)}</span>
                                    <t:if test="${b.status == 'CONFIRMED' && b.allowEarlyCheckin}">
                                        <div class="ss-bk__flag" title="The guest may enter before the first slot starts">
                                            <i class="bi bi-door-open" aria-hidden="true"></i>Early check-in on
                                        </div>
                                    </t:if>
                                </td>
                                <td class="pe-3 text-end text-nowrap">
                                    <%-- Item 2: the row opens the drawer (with Open booking); no separate Detail button --%>
                                    <div class="dropdown d-inline-block ss-row-menu">
                                        <button class="btn-trigger" type="button" data-bs-toggle="dropdown" aria-expanded="false"
                                                aria-label="More actions for ${e:esc(b.bookingCode)}">
                                            <i class="bi bi-three-dots" aria-hidden="true"></i>
                                        </button>
                                        <ul class="dropdown-menu dropdown-menu-end">
                                            <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/bookings/detail?id=${b.bookingId}">Open booking</a></li>
                                            <t:if test="${b.status == 'PENDING_PAYMENT' or b.status == 'CONFIRMED'}">
                                                <li><a class="dropdown-item" href="${pageContext.request.contextPath}/admin/bookings/edit?id=${b.bookingId}">Edit</a></li>
                                            </t:if>
                                            <t:if test="${b.status == 'CONFIRMED'}">
                                                <li>
                                                    <form method="post" action="${pageContext.request.contextPath}/admin/bookings/early-checkin" class="m-0">
                                                        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                                        <input type="hidden" name="bookingId" value="${b.bookingId}">
                                                        <input type="hidden" name="allow" value="${b.allowEarlyCheckin ? 'false' : 'true'}">
                                                        <button type="submit" class="dropdown-item">${b.allowEarlyCheckin ? 'Turn off early check-in' : 'Allow early check-in'}</button>
                                                    </form>
                                                </li>
                                            </t:if>
                                            <t:if test="${b.status == 'CANCELLED' && b.paymentStatus == 'REFUND_PENDING'}">
                                                <li>
                                                    <button type="button" class="dropdown-item btn-refund-modal"
                                                            data-booking-id="${b.bookingId}" data-booking-code="${e:esc(b.bookingCode)}">Mark refunded</button>
                                                </li>
                                            </t:if>
                                            <t:if test="${b.status == 'PENDING_PAYMENT' or b.status == 'CONFIRMED'}">
                                                <li><hr class="dropdown-divider"></li>
                                                <li><a class="dropdown-item dropdown-item--destructive" href="${pageContext.request.contextPath}/admin/bookings/detail?id=${b.bookingId}&amp;cancel=1">Cancel booking</a></li>
                                            </t:if>
                                            <t:if test="${!(b.status == 'PENDING_PAYMENT' or b.status == 'CONFIRMED' or (b.status == 'CANCELLED' && b.paymentStatus == 'REFUND_PENDING'))}">
                                                <li><span class="dropdown-item-text ss-bk__none">No other actions</span></li>
                                            </t:if>
                                        </ul>
                                    </div>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </t:if>
    </div>

    <t:pagination result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>

    <!-- Refund Confirmation Modal (unchanged hooks: #refundModal, .btn-refund-modal) -->
    <div class="modal fade" id="refundModal" tabindex="-1" aria-labelledby="refundModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/bookings/refund" method="post">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="bookingId" id="refundBookingId">
                    <div class="modal-header">
                        <h5 class="modal-title" id="refundModalLabel">Mark payment as refunded</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p>Mark the payment of cancelled booking <strong id="refundBookingCode" class="font-monospace"></strong> as refunded?</p>
                        <div class="mb-3">
                            <label for="refundNote" class="form-label small fw-semibold">Refund note or transfer reference</label>
                            <input type="text" class="form-control form-control-sm" id="refundNote" name="note" maxlength="255" placeholder="e.g. Bank transfer ref #123456">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-soft btn-sm" data-bs-dismiss="modal">Keep pending</button>
                        <button type="submit" class="btn btn-primary btn-sm">Mark refunded</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            var refundModalEl = document.getElementById('refundModal');
            var refundModal = refundModalEl ? new bootstrap.Modal(refundModalEl) : null;
            document.querySelectorAll('.btn-refund-modal').forEach(function(btn) {
                btn.addEventListener('click', function() {
                    var bid = this.getAttribute('data-booking-id');
                    var bcode = this.getAttribute('data-booking-code');
                    document.getElementById('refundBookingId').value = bid;
                    document.getElementById('refundBookingCode').textContent = bcode;
                    document.getElementById('refundNote').value = 'Bank refund completed for ' + bcode;
                    if (refundModal) refundModal.show();
                });
            });

            // Loading state while the next page of results is requested
            var table = document.getElementById('bkTable');
            function busy() { if (table) { table.classList.add('ss-bk-table--loading'); table.setAttribute('aria-busy', 'true'); } }
            var toolbar = document.getElementById('bkToolbar');
            if (toolbar) {
                toolbar.addEventListener('submit', function() {
                    busy();
                    var btn = toolbar.querySelector('button[type="submit"]');
                    if (btn) { btn.disabled = true; btn.textContent = 'Applying...'; }
                });
            }
            document.querySelectorAll('.ss-bk-tabs a, .ss-toolbar__chips a').forEach(function(a) {
                a.addEventListener('click', busy);
            });
        });
    </script>

    <!-- V3: shared detail drawer (same pattern as Alerts and Activity logs) -->
    <aside class="ss-drawer" id="actDrawer" role="dialog" aria-modal="false" aria-labelledby="actDrawerTitle" aria-hidden="true">
        <div class="ss-drawer__header">
            <h2 class="ss-drawer__title" id="actDrawerTitle" tabindex="-1">Booking</h2>
            <button type="button" class="ss-drawer__close" data-act-close aria-label="Close detail"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
        </div>
        <div class="ss-drawer__body">
            <dl class="ss-act-dl" id="actDrawerList" data-tab="booking"></dl>
        </div>
        <div class="ss-drawer__footer">
            <a class="btn btn-primary btn-sm" id="actDrawerOpen" href="#" hidden>Open booking</a>
        </div>
    </aside>
    <script src="${pageContext.request.contextPath}/assets/js/activity.js"></script>
</t:layout>
