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

    <!-- Boutique Filter Card -->
    <div class="card search-boutique-card mb-4 border-0">
        <div class="card-body p-4">
            <div class="d-flex align-items-center gap-2 mb-3">
                <span class="search-card-icon" style="width: 32px; height: 32px; font-size: 0.95rem;">
                    <i class="bi bi-sliders"></i>
                </span>
                <span class="fw-bold text-cocoa small text-uppercase" style="letter-spacing: 0.5px;">Filter Reservations</span>
            </div>
            <form action="${pageContext.request.contextPath}/guest/bookings" method="get" class="row g-3 align-items-end needs-validation" novalidate>
                <input type="hidden" name="sort" value="${e:esc(result.request.sort)}">
                <input type="hidden" name="dir" value="${e:esc(result.request.dir)}">

                <div class="col-12 col-sm-6 col-md-2">
                    <label for="status" class="form-label small fw-semibold text-cocoa">Status</label>
                    <select class="form-select form-control-pill ${errors['status'] != null ? 'is-invalid' : ''}" id="status" name="status">
                        <option value="">All Statuses</option>
                        <t:forEach items="${statuses}" var="st">
                            <option value="${e:esc(st)}" ${result.request.getFilter('status') == st ? 'selected' : ''}>${e:esc(st)}</option>
                        </t:forEach>
                    </select>
                    <t:if test="${not empty errors['status']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['status'])}</div>
                    </t:if>
                </div>

                <div class="col-12 col-sm-6 col-md-2">
                    <label for="room" class="form-label small fw-semibold text-cocoa">Room</label>
                    <input type="text" class="form-control form-control-pill" id="room" name="room"
                           placeholder="e.g. 101"
                           value="${e:esc(result.request.getFilter('room'))}">
                </div>

                <div class="col-12 col-sm-6 col-md-2">
                    <label for="from" class="form-label small fw-semibold text-cocoa">From Date</label>
                    <input type="date" class="form-control form-control-pill ${errors['from'] != null ? 'is-invalid' : ''}"
                           id="from" name="from" data-datepicker
                           value="${e:esc(result.request.getFilter('from'))}">
                    <t:if test="${not empty errors['from']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['from'])}</div>
                    </t:if>
                </div>

                <div class="col-12 col-sm-6 col-md-2">
                    <label for="to" class="form-label small fw-semibold text-cocoa">To Date</label>
                    <input type="date" class="form-control form-control-pill ${errors['to'] != null ? 'is-invalid' : ''}"
                           id="to" name="to" data-datepicker
                           value="${e:esc(result.request.getFilter('to'))}">
                    <t:if test="${not empty errors['to']}">
                        <div class="invalid-feedback d-block">${e:esc(errors['to'])}</div>
                    </t:if>
                </div>

                <div class="col-12 col-sm-6 col-md-2">
                    <label for="keyword" class="form-label small fw-semibold text-cocoa">Booking Code</label>
                    <input type="text" class="form-control form-control-pill" id="keyword" name="keyword"
                           placeholder="e.g. BK..." maxlength="50"
                           value="${e:esc(result.request.keyword)}">
                </div>

                <div class="col-12 col-sm-6 col-md-2 d-flex gap-2">
                    <button type="submit" class="btn btn-primary rounded-pill px-3 py-2 fw-semibold flex-fill d-inline-flex align-items-center justify-content-center gap-1 shadow-sm">
                        <i class="bi bi-search"></i> Search
                    </button>
                    <a href="${pageContext.request.contextPath}/guest/bookings" class="btn btn-outline-secondary rounded-pill px-3 py-2 fw-semibold flex-fill d-inline-flex align-items-center justify-content-center gap-1">
                        <i class="bi bi-arrow-counterclockwise"></i> Reset
                    </a>
                </div>
            </form>
        </div>
    </div>

    <!-- Summary Badge Bar -->
    <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
        <span class="badge rounded-pill bg-sand text-cocoa px-3 py-2 fw-semibold border border-latte">
            <i class="bi bi-collection me-1 text-caramel"></i>${e:esc(result.summary)}
        </span>
    </div>

    <!-- Bookings Table Card -->
    <div class="card bookings-table-card border-0 mb-4">
        <div class="table-responsive">
            <table class="table bookings-table align-middle mb-0">
                <thead>
                    <tr>
                        <th scope="col" style="width: 160px;">
                            <t:sortLink column="date" label="Booking Code" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                        </th>
                        <th scope="col" style="width: 180px;">
                            <t:sortLink column="room" label="Room" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                        </th>
                        <th scope="col" style="width: 140px;">
                            <t:sortLink column="date" label="Date" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                        </th>
                        <th scope="col" style="width: 120px;">Slots</th>
                        <th scope="col" style="width: 140px;">
                            <t:sortLink column="total" label="Total" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                        </th>
                        <th scope="col" style="width: 140px;">
                            <t:sortLink column="status" label="Status" result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
                        </th>
                        <th scope="col" style="width: 130px;">Payment</th>
                        <th scope="col" style="width: 80px;" class="text-end pe-4">Detail</th>
                    </tr>
                </thead>
                <tbody>
                    <t:forEach items="${result.items}" var="b">
                        <tr class="booking-row">
                            <td>
                                <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${b.bookingId}" class="booking-code-pill text-decoration-none">
                                    <i class="bi bi-ticket-perforated"></i>
                                    <span>${e:esc(b.bookingCode)}</span>
                                </a>
                            </td>
                            <td>
                                <div class="fw-bold text-espresso">${e:esc(b.roomTitle)}</div>
                                <div class="text-muted small d-flex align-items-center gap-1 mt-1">
                                    <span class="room-ref-badge">${e:esc(b.roomNumber)}</span>
                                </div>
                            </td>
                            <td>
                                <div class="booking-date-cell">
                                    <i class="bi bi-calendar-event me-1 text-caramel"></i>
                                    <span>${e:date(b.slotDate)}</span>
                                </div>
                            </td>
                            <td>
                                <span class="booking-slot-pill">${e:esc(b.slotsSummary)}</span>
                            </td>
                            <td>
                                <span class="booking-total-price">${e:money(b.total)}</span>
                            </td>
                            <td>
                                <t:if test="${b.status == 'CHECKED_IN' && b.earlyCheckin}">
                                    <span class="badge-status status-CHECKED_IN" title="Early check-in by ${b.earlyCheckinMinutes} minutes">CHECKED_IN &middot; Early (${b.earlyCheckinMinutes}m)</span>
                                </t:if>
                                <t:if test="${!(b.status == 'CHECKED_IN' && b.earlyCheckin)}">
                                    <span class="badge-status status-${b.status}">${e:esc(b.status)}</span>
                                </t:if>
                            </td>
                            <td>
                                <t:if test="${b.paymentStatus == 'NONE' || empty b.paymentStatus}">
                                    <span class="text-muted small">&mdash;</span>
                                </t:if>
                                <t:if test="${b.paymentStatus != 'NONE' && not empty b.paymentStatus}">
                                    <span class="badge-status status-${b.paymentStatus}">${e:esc(b.paymentStatus)}</span>
                                </t:if>
                            </td>
                            <td class="text-end pe-4">
                                <a href="${pageContext.request.contextPath}/guest/bookings/detail?id=${b.bookingId}"
                                   class="btn btn-sm btn-boutique-view rounded-pill"
                                   title="View details">
                                    <i class="bi bi-chevron-right"></i>
                                </a>
                            </td>
                        </tr>
                    </t:forEach>
                    <t:if test="${empty result.items}">
                        <tr>
                            <td colspan="8" class="text-center py-5">
                                <div class="empty-state py-4">
                                    <div class="empty-icon mb-3">
                                        <i class="bi bi-calendar-x text-muted" style="font-size: 2.8rem; opacity: 0.6;"></i>
                                    </div>
                                    <h5 class="fw-bold text-espresso mb-1">No bookings yet</h5>
                                    <p class="text-muted small mb-3">You don't have any bookings matching this filter. Explore available rooms and reserve your stay.</p>
                                    <a href="${pageContext.request.contextPath}/rooms" class="btn btn-primary rounded-pill px-4 btn-sm fw-semibold">
                                        Explore Rooms
                                    </a>
                                </div>
                            </td>
                        </tr>
                    </t:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Pagination -->
    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/guest/bookings"/>
</t:layout>
