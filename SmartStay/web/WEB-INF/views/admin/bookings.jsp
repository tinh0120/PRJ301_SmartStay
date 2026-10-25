<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Bookings" active="admin-bookings">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Dashboard
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Bookings</li>
        </ol>
    </nav>

    <div class="d-flex justify-content-between align-items-center mb-3">
        <div>
            <h1 class="h3 fw-bold mb-1">Booking management</h1>
            <p class="text-muted small mb-0">Manage guest reservations, walk-in bookings, check-in, check-out, and no-shows.</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/admin/bookings/edit" class="btn btn-primary btn-sm">
                + Walk-in booking
            </a>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger py-2" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <!-- Tabbed Booking Filter Pills (Zero-Scroll Table Views) -->
    <ul class="nav nav-pills mb-3 gap-2 flex-wrap" id="bookingTabPills" role="tablist">
        <li class="nav-item">
            <a class="nav-link ${empty pr.getFilter('status') && empty pr.getFilter('flag') ? 'active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings">
                <i class="bi bi-collection me-1"></i> All Bookings
                <span class="badge rounded-pill ms-1 ${empty pr.getFilter('status') && empty pr.getFilter('flag') ? 'bg-cream text-espresso' : 'bg-light text-muted border'}">${not empty statusCounts['ALL'] ? statusCounts['ALL'] : 0}</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${pr.getFilter('status') == 'PENDING_PAYMENT' ? 'active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?status=PENDING_PAYMENT">
                <i class="bi bi-clock-history me-1"></i> Pending Payment
                <span class="badge rounded-pill ms-1 ${pr.getFilter('status') == 'PENDING_PAYMENT' ? 'bg-cream text-espresso' : 'bg-light text-muted border'}">${not empty statusCounts['PENDING_PAYMENT'] ? statusCounts['PENDING_PAYMENT'] : 0}</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${pr.getFilter('status') == 'CONFIRMED' ? 'active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?status=CONFIRMED">
                <i class="bi bi-check-circle me-1"></i> Confirmed
                <span class="badge rounded-pill ms-1 ${pr.getFilter('status') == 'CONFIRMED' ? 'bg-cream text-espresso' : 'bg-light text-muted border'}">${not empty statusCounts['CONFIRMED'] ? statusCounts['CONFIRMED'] : 0}</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${pr.getFilter('status') == 'CHECKED_IN' ? 'active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?status=CHECKED_IN">
                <i class="bi bi-door-open-fill me-1"></i> Checked In
                <span class="badge rounded-pill ms-1 ${pr.getFilter('status') == 'CHECKED_IN' ? 'bg-cream text-espresso' : 'bg-light text-muted border'}">${not empty statusCounts['CHECKED_IN'] ? statusCounts['CHECKED_IN'] : 0}</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${pr.getFilter('status') == 'CHECKED_OUT' ? 'active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?status=CHECKED_OUT">
                <i class="bi bi-check2-all me-1"></i> Checked Out
                <span class="badge rounded-pill ms-1 ${pr.getFilter('status') == 'CHECKED_OUT' ? 'bg-cream text-espresso' : 'bg-light text-muted border'}">${not empty statusCounts['CHECKED_OUT'] ? statusCounts['CHECKED_OUT'] : 0}</span>
            </a>
        </li>
        <li class="nav-item">
            <a class="nav-link ${pr.getFilter('flag') == 'noshow_held' ? 'active' : ''}"
               href="${pageContext.request.contextPath}/admin/bookings?flag=noshow_held">
                <i class="bi bi-exclamation-triangle me-1"></i> No-Show / Expired
                <span class="badge rounded-pill ms-1 ${pr.getFilter('flag') == 'noshow_held' ? 'bg-cream text-espresso' : 'bg-light text-muted border'}">${not empty statusCounts['NOSHOW_HELD'] ? statusCounts['NOSHOW_HELD'] : 0}</span>
            </a>
        </li>
    </ul>

    <!-- Mobile Filter Toggle Button (< 768px) -->
    <div class="d-md-none mb-3">
        <button class="btn btn-outline-secondary btn-sm w-100 d-flex justify-content-between align-items-center" type="button" data-bs-toggle="collapse" data-bs-target="#bookingFilterCollapse" aria-expanded="false" aria-controls="bookingFilterCollapse">
            <span><i class="bi bi-funnel me-1"></i> Filters &amp; Search</span>
            <i class="bi bi-chevron-down"></i>
        </button>
    </div>

    <!-- Filters Card -->
    <div class="card shadow-sm border-0 mb-4 bg-light collapse d-md-block" id="bookingFilterCollapse">
        <div class="card-body p-3">
            <form action="${pageContext.request.contextPath}/admin/bookings" method="get" class="row g-2 align-items-end">
                <input type="hidden" name="sort" value="${e:esc(pr.sort)}">
                <input type="hidden" name="dir" value="${e:esc(pr.dir)}">

                <input type="hidden" name="status" value="${e:esc(pr.getFilter('status'))}">

                <div class="col-md-3">
                    <label for="keyword" class="form-label small fw-semibold">Search</label>
                    <input type="text" class="form-control form-control-sm" id="keyword" name="keyword"
                           placeholder="Code, guest name, email..." maxlength="100"
                           value="${e:esc(pr.keyword)}">
                </div>

                <div class="col-md-3">
                    <label for="payment_status" class="form-label small fw-semibold">Payment Status</label>
                    <select class="form-select form-select-sm" id="payment_status" name="payment_status">
                        <option value="">All payment statuses</option>
                        <option value="PAID" ${pr.getFilter('payment_status') == 'PAID' ? 'selected' : ''}>PAID</option>
                        <option value="PENDING" ${pr.getFilter('payment_status') == 'PENDING' ? 'selected' : ''}>PENDING</option>
                        <option value="REVIEW_REQUIRED" ${pr.getFilter('payment_status') == 'REVIEW_REQUIRED' ? 'selected' : ''}>REVIEW_REQUIRED</option>
                        <option value="REFUND_PENDING" ${pr.getFilter('payment_status') == 'REFUND_PENDING' ? 'selected' : ''}>REFUND_PENDING</option>
                        <option value="REFUNDED" ${pr.getFilter('payment_status') == 'REFUNDED' ? 'selected' : ''}>REFUNDED</option>
                        <option value="FAILED" ${pr.getFilter('payment_status') == 'FAILED' ? 'selected' : ''}>FAILED</option>
                        <option value="EXPIRED" ${pr.getFilter('payment_status') == 'EXPIRED' ? 'selected' : ''}>EXPIRED</option>
                    </select>
                </div>

                <div class="col-md-3">
                    <label for="room" class="form-label small fw-semibold">Room</label>
                    <select class="form-select form-select-sm" id="room" name="room">
                        <option value="">All rooms</option>
                        <t:forEach items="${rooms}" var="rm">
                            <option value="${rm.roomId}" ${pr.getFilter('room') == rm.roomId or pr.getFilter('room') == rm.roomNumber ? 'selected' : ''}>Room ${e:esc(rm.roomNumber)}</option>
                        </t:forEach>
                    </select>
                </div>

                <div class="col-md-3">
                    <div class="row g-1">
                        <div class="col-6">
                            <label for="from" class="form-label small fw-semibold">From Date</label>
                            <input type="date" class="form-control form-control-sm" id="from" name="from" data-datepicker
                                   value="${e:esc(pr.getFilter('from'))}">
                        </div>
                        <div class="col-6">
                            <label for="to" class="form-label small fw-semibold">To Date</label>
                            <input type="date" class="form-control form-control-sm" id="to" name="to" data-datepicker
                                   value="${e:esc(pr.getFilter('to'))}">
                        </div>
                    </div>
                </div>

                <div class="col-12 d-flex justify-content-between align-items-center mt-2 pt-2 border-top">
                    <div class="form-check form-check-inline">
                        <input class="form-check-input" type="checkbox" id="noshow_held" name="noshow_held" value="1"
                               ${pr.getFilter('noshow_held') == '1' or pr.getFilter('noshow_held') == 'true' ? 'checked' : ''}>
                        <label class="form-check-label small fw-bold text-danger" for="noshow_held">
                            No-show with held slots only
                        </label>
                    </div>

                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary btn-sm px-3">Filter</button>
                        <a href="${pageContext.request.contextPath}/admin/bookings" class="btn btn-outline-secondary btn-sm px-3">Clear</a>
                    </div>
                </div>
            </form>
        </div>
    </div>

    <!-- Summary & Tooltip Legend -->
    <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">
        <span class="text-muted small">${e:esc(pageResult.summary)}</span>
        <div class="d-flex flex-wrap align-items-center gap-3 small text-muted">
            <span><i class="bi bi-info-circle me-1 text-espresso"></i><strong>Legend:</strong></span>
            <span>
                <span class="badge bg-warning text-dark py-1 px-2" style="font-size: 0.7rem;" data-bs-toggle="tooltip" title="Slot remains reserved on hardware after guest no-show until staff intervention">Held slots</span>
                <span class="text-secondary ms-1">Reserved slot lock state</span>
            </span>
            <span>
                <span class="badge border bg-light text-dark py-1 px-2" style="font-size: 0.7rem;" data-bs-toggle="tooltip" title="Early Check-In: Allows guest to enter room before slot start time">
                    <i class="bi bi-toggle-off me-1"></i>Early CI
                </span>
                <span class="text-secondary ms-1">Access before check-in time</span>
            </span>
        </div>
    </div>

    <!-- Bookings Table -->
    <div class="card shadow-sm border-0 mb-3">
        <div class="table-responsive">
            <table class="table table-sm table-hover align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th class="ps-3" style="width: 140px;">
                            <t:sortLink column="date" label="Booking Code" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                        </th>
                        <th>
                            <t:sortLink column="guest" label="Guest" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                        </th>
                        <th>
                            <t:sortLink column="room" label="Room" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                        </th>
                        <th>Date &amp; Slots</th>
                        <th>
                            <t:sortLink column="total" label="Total & Payment" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                        </th>
                        <th>
                            <t:sortLink column="status" label="Status" result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>
                        </th>
                        <th class="pe-3 text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    <t:if test="${empty pageResult.items}">
                        <tr>
                            <td colspan="7" class="text-center py-5 text-muted">
                                <div class="empty-state py-2">
                                    <i class="bi bi-calendar-x text-muted mb-2" style="font-size: 2.2rem; opacity: 0.6;"></i>
                                    <div class="fw-semibold text-espresso fs-6">No bookings yet</div>
                                    <div class="small text-muted">No reservations match the selected criteria.</div>
                                </div>
                            </td>
                        </tr>
                    </t:if>

                    <t:forEach items="${pageResult.items}" var="b">
                        <tr>
                            <td class="ps-3">
                                <span class="badge border bg-white text-espresso font-monospace px-2 py-1 shadow-2xs" style="border-color: #D9B991 !important;">
                                    <a href="${pageContext.request.contextPath}/admin/bookings/detail?id=${b.bookingId}" class="text-decoration-none text-espresso fw-bold">
                                        ${e:esc(b.bookingCode)}
                                    </a>
                                </span>
                                <t:if test="${b.hasHeldSlots}">
                                    <span class="badge bg-warning text-dark d-block mt-1" style="font-size: 0.68rem;" title="Room slots remain locked after no-show until manually released">Held slots</span>
                                </t:if>
                            </td>
                            <td>
                                <div class="fw-semibold text-espresso">${e:esc(b.guestName)}</div>
                                <div class="small text-muted">${e:esc(b.guestEmail)}</div>
                            </td>
                            <td>
                                <span class="badge bg-light text-dark border">Room ${e:esc(b.roomNumber)}</span>
                                <div class="small text-muted text-truncate" style="max-width: 130px;">${e:esc(b.roomTitle)}</div>
                            </td>
                            <td>
                                <div class="fw-semibold text-dark">${e:date(b.slotDate)}</div>
                                <div class="small text-muted">${e:esc(b.slotsSummary)}</div>
                            </td>
                            <td>
                                <div class="fw-bold text-dark">${e:money(not empty b.totalAmount ? b.totalAmount : b.total)}</div>
                                <t:if test="${b.discountPercent > 0}">
                                    <span class="badge bg-success-subtle text-success" style="font-size: 0.68rem;">-${b.discountPercent}%</span>
                                </t:if>
                                <div class="mt-1">
                                    <span class="badge-status status-${b.paymentStatus}" style="font-size: 0.68rem;">${b.paymentStatus}</span>
                                </div>
                            </td>
                            <td>
                                <div>
                                    <span class="badge-status status-${b.status}">${b.status}</span>
                                    <t:if test="${b.status == 'CONFIRMED'}">
                                        <form method="post" action="${pageContext.request.contextPath}/admin/bookings/early-checkin" class="d-inline ms-1">
                                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                                            <input type="hidden" name="bookingId" value="${b.bookingId}">
                                            <input type="hidden" name="allow" value="${b.allowEarlyCheckin ? 'false' : 'true'}">
                                            <button type="submit" class="btn btn-xs ${b.allowEarlyCheckin ? 'btn-warning text-dark' : 'btn-outline-secondary'} py-0 px-1 rounded-pill" style="font-size: 0.68rem;" title="${b.allowEarlyCheckin ? 'Early check-in enabled: Click to revoke permission' : 'Early check-in disabled: Click to grant permission'}">
                                                <i class="bi ${b.allowEarlyCheckin ? 'bi-check2-circle' : 'bi-toggle-off'}"></i>
                                                ${b.allowEarlyCheckin ? 'Early CI: ON' : 'Early CI: OFF'}
                                            </button>
                                        </form>
                                    </t:if>
                                    <t:if test="${b.status == 'CHECKED_IN' && b.earlyCheckin}">
                                        <div class="mt-1">
                                            <span class="badge bg-espresso text-cream" style="font-size: 0.68rem;" title="Checked in early by ${b.earlyCheckinMinutes} minutes">
                                                <i class="bi bi-clock-history me-1"></i>Early CI (${b.earlyCheckinMinutes}m)
                                            </span>
                                        </div>
                                    </t:if>
                                    <t:if test="${b.status == 'EXPIRED' && not empty b.expireReason}">
                                        <div class="small text-danger mt-1">${e:esc(b.expireReason)}</div>
                                    </t:if>
                                </div>
                            </td>
                            <td class="pe-3 text-end">
                                <div class="d-inline-flex gap-1 align-items-center">
                                    <a href="${pageContext.request.contextPath}/admin/bookings/detail?id=${b.bookingId}"
                                       class="btn btn-outline-soft btn-sm py-0 px-2" title="View details">
                                        Detail
                                    </a>
                                    <t:if test="${b.status == 'PENDING_PAYMENT' or b.status == 'CONFIRMED'}">
                                        <a href="${pageContext.request.contextPath}/admin/bookings/edit?id=${b.bookingId}"
                                           class="btn btn-outline-primary btn-sm py-0 px-2" title="Edit booking">
                                            Edit
                                        </a>
                                    </t:if>
                                    <t:if test="${b.status == 'CANCELLED' && b.paymentStatus == 'REFUND_PENDING'}">
                                        <button type="button" class="btn btn-outline-warning btn-sm py-0 px-2 btn-refund-modal"
                                                data-booking-id="${b.bookingId}" data-booking-code="${e:esc(b.bookingCode)}"
                                                title="Mark payment as refunded">
                                            Mark refunded
                                        </button>
                                    </t:if>
                                </div>
                            </td>
                        </tr>
                    </t:forEach>
                </tbody>
            </table>
        </div>
    </div>

    <t:pagination result="${pageResult}" baseUrl="${pageContext.request.contextPath}/admin/bookings"/>

    <!-- Refund Confirmation Modal -->
    <div class="modal fade" id="refundModal" tabindex="-1" aria-labelledby="refundModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <form action="${pageContext.request.contextPath}/admin/bookings/refund" method="post">
                    <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                    <input type="hidden" name="bookingId" id="refundBookingId">
                    <div class="modal-header">
                        <h5 class="modal-title" id="refundModalLabel"><i class="bi bi-arrow-counterclockwise me-2 text-warning"></i>Mark Payment as Refunded</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p>Are you sure you want to mark payment as <strong>REFUNDED</strong> for cancelled booking <strong id="refundBookingCode" class="text-espresso font-monospace"></strong>?</p>
                        <div class="mb-3">
                            <label for="refundNote" class="form-label small fw-semibold">Refund resolution note / transfer reference:</label>
                            <input type="text" class="form-control form-control-sm" id="refundNote" name="note" maxlength="255" placeholder="e.g. Bank transfer ref #123456 completed">
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-warning btn-sm">Confirm Refunded</button>
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
        });
    </script>
</t:layout>
