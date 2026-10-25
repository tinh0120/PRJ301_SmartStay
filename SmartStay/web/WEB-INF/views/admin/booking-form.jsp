<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${mode == 'create' ? 'Create walk-in booking' : 'Edit booking'}" active="admin-bookings">
    <div class="row justify-content-center">
        <div class="col-lg-8">
            <nav aria-label="breadcrumb" class="mb-3">
                <ol class="breadcrumb boutique-breadcrumb m-0">
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/admin/dashboard" class="boutique-breadcrumb-link">
                            <i class="bi bi-house-door me-1"></i>Dashboard
                        </a>
                    </li>
                    <li class="breadcrumb-item">
                        <a href="${pageContext.request.contextPath}/admin/bookings" class="boutique-breadcrumb-link">
                            Bookings
                        </a>
                    </li>
                    <li class="breadcrumb-item active" aria-current="page">
                        <t:if test="${mode == 'create'}">Create walk-in</t:if>
                        <t:if test="${mode != 'create'}">Edit ${booking.bookingCode}</t:if>
                    </li>
                </ol>
            </nav>

            <div class="card shadow-sm border-0 mb-4">
                <div class="card-header bg-white py-3">
                    <h1 class="h4 fw-bold mb-0">
                        <t:if test="${mode == 'create'}">Create walk-in booking</t:if>
                        <t:if test="${mode != 'create'}">Edit booking ${booking.bookingCode}</t:if>
                    </h1>
                    <p class="text-muted small mb-0 mt-1">
                        <t:if test="${mode == 'create'}">
                            Create a reservation on behalf of an active guest with walk-in privileges.
                        </t:if>
                        <t:if test="${mode != 'create'}">
                            <t:if test="${booking.status == 'PENDING_PAYMENT'}">
                                Update room, date, slots, and notes. Amounts and payment deadline will be recalculated.
                            </t:if>
                            <t:if test="${booking.status == 'CONFIRMED'}">
                                Confirmed bookings only allow updating the administrative note.
                            </t:if>
                            <t:if test="${booking.status != 'PENDING_PAYMENT' and booking.status != 'CONFIRMED'}">
                                This booking is in <strong>${e:esc(booking.status)}</strong> status and cannot be modified.
                            </t:if>
                        </t:if>
                    </p>
                </div>

                <div class="card-body p-4">
                    <t:if test="${mode != 'create' and booking.status != 'PENDING_PAYMENT' and booking.status != 'CONFIRMED'}">
                        <div class="alert alert-warning" role="alert">
                            Booking <strong>${e:esc(booking.bookingCode)}</strong> is currently ${e:esc(booking.status)}. Only PENDING_PAYMENT (full edit) and CONFIRMED (note only) bookings can be updated.
                        </div>
                        <div class="mt-3">
                            <a href="${pageContext.request.contextPath}/admin/bookings/detail?id=${booking.bookingId}" class="btn btn-outline-secondary">
                                &larr; Return to detail
                            </a>
                        </div>
                    </t:if>

                    <t:if test="${mode == 'create' or booking.status == 'PENDING_PAYMENT' or booking.status == 'CONFIRMED'}">
                        <form action="${pageContext.request.contextPath}/admin/bookings/edit" method="post" class="needs-validation" novalidate>
                            <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
                            <t:if test="${mode == 'edit'}">
                                <input type="hidden" name="bookingId" value="${booking.bookingId}">
                            </t:if>

                            <!-- Guest Information -->
                            <t:if test="${mode == 'create'}">
                                <div class="mb-3">
                                    <label for="guestEmail" class="form-label fw-semibold">Guest Email <span class="text-danger">*</span></label>
                                    <input type="email" class="form-control" id="guestEmail" name="guestEmail"
                                           placeholder="guest@example.com" value="${e:esc(guestEmail)}" required>
                                    <div class="form-text">Must be an existing, active user with GUEST role.</div>
                                </div>
                            </t:if>
                            <t:if test="${mode == 'edit'}">
                                <div class="row g-2 mb-3 bg-light p-3 rounded">
                                    <div class="col-sm-4">
                                        <span class="text-muted small d-block">Guest</span>
                                        <strong>${e:esc(booking.guestName)}</strong>
                                    </div>
                                    <div class="col-sm-4">
                                        <span class="text-muted small d-block">Email</span>
                                        <span>${e:esc(booking.guestEmail)}</span>
                                    </div>
                                    <div class="col-sm-4">
                                        <span class="text-muted small d-block">Status</span>
                                        <span class="badge ${booking.status == 'CONFIRMED' ? 'bg-success' : 'bg-warning text-dark'}">${e:esc(booking.status)}</span>
                                    </div>
                                </div>
                            </t:if>

                            <!-- Room & Date Selection -->
                            <t:if test="${mode == 'create' or booking.status == 'PENDING_PAYMENT'}">
                                <div class="row g-3 mb-3">
                                    <div class="col-md-6">
                                        <label for="roomId" class="form-label fw-semibold">Room <span class="text-danger">*</span></label>
                                        <select class="form-select" id="roomId" name="roomId" required onchange="reloadBookingForm()">
                                            <t:forEach items="${rooms}" var="rm">
                                                <option value="${rm.roomId}" ${selectedRoomId == rm.roomId ? 'selected' : ''}>
                                                    Room ${e:esc(rm.roomNumber)} &ndash; Floor ${rm.floorNumber} (Cap: ${rm.capacity})
                                                </option>
                                            </t:forEach>
                                        </select>
                                    </div>

                                    <div class="col-md-6">
                                        <label for="slotDate" class="form-label fw-semibold">Stay Date <span class="text-danger">*</span></label>
                                        <input type="date" class="form-control" id="slotDate" name="slotDate"
                                               value="${selectedDate}" required onchange="reloadBookingForm()" data-datepicker>
                                    </div>
                                </div>

                                <!-- Slot Selection -->
                                <div class="mb-3">
                                    <label class="form-label fw-semibold d-block">Select Time Slots <span class="text-danger">*</span></label>
                                    <div class="form-text mb-2">Select 1 to 4 slots. Slots must be contiguous in sequence.</div>

                                    <div class="row g-2">
                                        <t:if test="${empty slots}">
                                            <div class="col-12 text-muted small p-2 border rounded">
                                                No time slots configured for this room.
                                            </div>
                                        </t:if>
                                        <t:forEach items="${slots}" var="sl">
                                            <div class="col-sm-6">
                                                <div class="card h-100 border p-2">
                                                    <div class="form-check">
                                                        <input class="form-check-input" type="checkbox" name="slots"
                                                               value="${sl.slotCode}" id="slot_${sl.slotCode}">
                                                        <label class="form-check-label w-100" for="slot_${sl.slotCode}">
                                                            <div class="fw-bold">${e:esc(sl.slotCode)} &ndash; ${e:esc(sl.slotName)}</div>
                                                            <div class="small text-muted">${sl.startTime} &ndash; ${sl.endTime}</div>
                                                            <div class="small text-primary fw-semibold">${e:money(sl.listPrice)}</div>
                                                        </label>
                                                    </div>
                                                </div>
                                            </div>
                                        </t:forEach>
                                    </div>
                                </div>
                            </t:if>

                            <t:if test="${mode == 'edit' and booking.status == 'CONFIRMED'}">
                                <div class="row g-3 mb-3 bg-light p-3 rounded">
                                    <div class="col-md-6">
                                        <span class="text-muted small d-block">Room</span>
                                        <strong>Room ${e:esc(booking.roomNumber)}</strong>
                                    </div>
                                    <div class="col-md-6">
                                        <span class="text-muted small d-block">Stay Date &amp; Slots</span>
                                        <strong>${e:date(booking.slotDate)} (${e:esc(booking.slotsSummary)})</strong>
                                    </div>
                                </div>
                            </t:if>

                            <!-- Administrative Note -->
                            <div class="mb-4">
                                <label for="note" class="form-label fw-semibold">
                                    Note <t:if test="${mode == 'create'}"><span class="text-danger">*</span></t:if>
                                </label>
                                <textarea class="form-control" id="note" name="note" rows="3" maxlength="500"
                                          placeholder="${mode == 'create' ? 'Reason for walk-in booking (required, max 500 characters)' : 'Administrative remarks (max 500 characters)'}"
                                          ${mode == 'create' ? 'required' : ''}>${e:esc(mode == 'edit' ? booking.note : '')}</textarea>
                                <div class="form-text">Max 500 characters.</div>
                            </div>

                            <div class="d-flex justify-content-between">
                                <t:if test="${mode == 'edit'}">
                                    <a href="${pageContext.request.contextPath}/admin/bookings/detail?id=${booking.bookingId}"
                                       class="btn btn-outline-secondary">
                                        Cancel
                                    </a>
                                </t:if>
                                <t:if test="${mode != 'edit'}">
                                    <a href="${pageContext.request.contextPath}/admin/bookings"
                                       class="btn btn-outline-secondary">
                                        Cancel
                                    </a>
                                </t:if>
                                <button type="submit" class="btn btn-primary px-4">
                                    ${mode == 'create' ? 'Create walk-in booking' : (booking.status == 'CONFIRMED' ? 'Update note' : 'Save changes')}
                                </button>
                            </div>
                        </form>
                    </t:if>
                </div>
            </div>
        </div>
    </div>

    <script>
        function reloadBookingForm() {
            var rmEl = document.getElementById('roomId');
            var dtEl = document.getElementById('slotDate');
            if (!rmEl || !dtEl) return;
            var url = '${pageContext.request.contextPath}/admin/bookings/edit?roomId=' + encodeURIComponent(rmEl.value) + '&date=' + encodeURIComponent(dtEl.value);
            <t:if test="${mode == 'edit'}">
            url += '&id=${booking.bookingId}';
            </t:if>
            <t:if test="${mode == 'create' and not empty guestEmail}">
            url += '&guestEmail=${e:url(guestEmail)}';
            </t:if>
            window.location.href = url;
        }
    </script>
</t:layout>
