<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="${e:esc(room.displayTitle)} slots and pricing" active="admin-rooms">
    <%-- R8: the ONLY place to edit S1–S4 prices, the room status (Occupied is read-only) and each
         slot's status. Field names (slotId, slotCode, slotName_Sx, startTime_Sx, endTime_Sx, price_Sx,
         status_Sx, active_Sx, roomStatus) and CSRF are unchanged. --%>
    <div class="ss-page-header ss-rm-form__header">
        <div>
            <a href="${pageContext.request.contextPath}/admin/rooms" class="ss-rm-form__back">
                <i class="bi bi-arrow-left" aria-hidden="true"></i>Rooms
            </a>
            <h1 class="ss-page-header__title">${e:esc(room.displayTitle)} <span class="ss-rm-form__code">${e:esc(room.roomNumber)}</span></h1>
        </div>
    </div>

    <t:roomTabs roomId="${room.roomId}" active="slots"/>

    <t:if test="${not empty errorMessage}">
        <div class="ss-toast ss-toast--error" role="alert">
            <i class="bi bi-x-circle" aria-hidden="true"></i><span>${e:esc(errorMessage)}</span>
        </div>
    </t:if>

    <form action="${pageContext.request.contextPath}/admin/rooms/slots" method="post" id="slotsForm" class="ss-rm-form">
        <input type="hidden" name="_csrf" value="${e:esc(not empty _csrf ? _csrf : sessionScope._csrf)}">
        <input type="hidden" name="roomId" value="${room.roomId}">

        <!-- Room status -->
        <section class="ss-rm-section" aria-labelledby="secStatus">
            <h2 class="ss-rm-section__title" id="secStatus">Room status</h2>
            <t:if test="${room.status == 'OCCUPIED'}">
                <p class="ss-rm-status-ro">
                    <span class="ss-badge ss-badge--neutral">Occupied</span>
                    <span>A guest is checked in. The status changes by itself at check-out.</span>
                </p>
            </t:if>
            <t:if test="${room.status != 'OCCUPIED'}">
                <div class="ss-rm-status">
                    <label for="roomStatus" class="form-label fw-semibold mb-0">Status</label>
                    <select class="form-select ss-rm-status__select" id="roomStatus" name="roomStatus" data-initial="${e:esc(room.status)}">
                        <option value="AVAILABLE" ${room.status == 'AVAILABLE' ? 'selected' : ''}>Available</option>
                        <option value="NEEDS_CLEANING" ${room.status == 'NEEDS_CLEANING' ? 'selected' : ''}>Needs cleaning</option>
                        <option value="MAINTENANCE" ${room.status == 'MAINTENANCE' ? 'selected' : ''}>Maintenance</option>
                    </select>
                </div>
                <p class="ss-rm-section__hint">Occupied is set automatically by check-in and check-out.</p>
            </t:if>
            <div class="ss-rm-maint ${room.status == 'MAINTENANCE' ? '' : 'd-none'}" id="maintNotice" role="status">
                <i class="bi bi-cone-striped" aria-hidden="true"></i>
                <span>While the room is in maintenance, all four slots are blocked and guests cannot book it, whatever the slot status below says.</span>
            </div>
            <t:if test="${!room.active}">
                <p class="ss-rm-section__hint">This room is hidden from guests (Details tab, Visibility).</p>
            </t:if>
        </section>

        <!-- Slots & prices -->
        <section class="ss-rm-section" aria-labelledby="secSlots">
            <h2 class="ss-rm-section__title" id="secSlots">Slots &amp; prices</h2>
            <div class="table-responsive">
                <table class="table align-middle mb-0 ss-table ss-rm-slots">
                    <thead>
                        <tr>
                            <th scope="col">Slot</th>
                            <th scope="col">Time window</th>
                            <th scope="col">Price</th>
                            <th scope="col">Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        <t:forEach items="${slots}" var="s">
                            <tr class="ss-rm-slots__row" data-slot-row>
                                <td>
                                    <input type="hidden" name="slotId" value="${s.roomSlotId}">
                                    <input type="hidden" name="slotCode" value="${e:esc(s.slotCode)}">
                                    <input type="hidden" name="slotId_${s.slotCode}" value="${s.roomSlotId}">
                                    <label class="visually-hidden" for="slotName_${s.slotCode}">${e:esc(s.slotCode)} name</label>
                                    <input type="text" class="form-control form-control-sm ss-rm-slots__name" id="slotName_${s.slotCode}"
                                           name="slotName_${s.slotCode}" value="${e:esc(s.slotName)}" maxlength="50" required>
                                    <span class="ss-rm-slots__code">${e:esc(s.slotCode)}</span>
                                </td>
                                <td>
                                    <div class="ss-rm-slots__times">
                                        <label class="visually-hidden" for="startTime_${s.slotCode}">${e:esc(s.slotName)} start</label>
                                        <input type="time" class="form-control form-control-sm" id="startTime_${s.slotCode}"
                                               name="startTime_${s.slotCode}" value="${s.startTime}" required>
                                        <span aria-hidden="true">&rarr;</span>
                                        <label class="visually-hidden" for="endTime_${s.slotCode}">${e:esc(s.slotName)} end</label>
                                        <input type="time" class="form-control form-control-sm" id="endTime_${s.slotCode}"
                                               name="endTime_${s.slotCode}" value="${s.endTime}" required>
                                    </div>
                                    <t:if test="${s.crossesMidnight}"><span class="ss-rm-slots__note">Ends the next day</span></t:if>
                                </td>
                                <td>
                                    <div class="ss-rm-slots__price-box">
                                        <div class="input-group input-group-sm ss-rm-slots__price">
                                            <label class="visually-hidden" for="price_${s.slotCode}">${e:esc(s.slotName)} price</label>
                                            <input type="number" class="form-control" id="price_${s.slotCode}" name="price_${s.slotCode}"
                                                   value="${s.listPrice}" min="1000" step="1000" required>
                                            <span class="input-group-text">VND</span>
                                        </div>
                                        <span class="ss-rm-slots__price-hint">${e:money(s.listPrice)}</span>
                                    </div>
                                </td>
                                <td>
                                    <div class="ss-rm-slots__status">
                                        <label class="visually-hidden" for="status_${s.slotCode}">${e:esc(s.slotName)} status</label>
                                        <select class="form-select form-select-sm" id="status_${s.slotCode}" name="status_${s.slotCode}">
                                            <option value="AVAILABLE" ${s.status == 'AVAILABLE' ? 'selected' : ''}>Available</option>
                                            <option value="NEEDS_CLEANING" ${s.status == 'NEEDS_CLEANING' ? 'selected' : ''}>Needs cleaning</option>
                                            <option value="MAINTENANCE" ${s.status == 'MAINTENANCE' ? 'selected' : ''}>Maintenance</option>
                                            <t:if test="${s.status == 'BOOKED'}">
                                                <option value="BOOKED" selected>Booked</option>
                                            </t:if>
                                        </select>
                                        <div class="form-check form-switch ss-rm-slots__offered">
                                            <input class="form-check-input" type="checkbox" id="active_${s.slotCode}" name="active_${s.slotCode}" ${s.active ? 'checked' : ''}>
                                            <label class="form-check-label" for="active_${s.slotCode}">Offered</label>
                                        </div>
                                    </div>
                                    <t:if test="${not empty s.todayDifference}">
                                        <span class="ss-rm-slots__today">${e:esc(s.todayDifference)}</span>
                                    </t:if>
                                </td>
                            </tr>
                        </t:forEach>
                    </tbody>
                </table>
            </div>
        </section>

        <div class="ss-rm-actionbar">
            <button type="submit" class="btn btn-primary px-4" id="slotsSaveBtn">Save changes</button>
            <a href="${pageContext.request.contextPath}/admin/rooms" class="btn btn-outline-soft px-4">Cancel</a>
            <span class="ss-rm-actionbar__note" id="slotsChangedNote" aria-live="polite"></span>
        </div>
    </form>

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            var form = document.getElementById('slotsForm');
            if (!form) return;

            // Remember the loaded values, then highlight rows that differ before saving
            function value(el) { return el.type === 'checkbox' ? String(el.checked) : el.value; }
            var fields = form.querySelectorAll('[data-slot-row] input:not([type="hidden"]), [data-slot-row] select');
            fields.forEach(function(el) { el.setAttribute('data-initial-value', value(el)); });

            var note = document.getElementById('slotsChangedNote');
            var saveBtn = document.getElementById('slotsSaveBtn');
            var statusSelect = document.getElementById('roomStatus');
            var maintNotice = document.getElementById('maintNotice');

            function updatePriceHints() {
                form.querySelectorAll('[data-slot-row]').forEach(function(row) {
                    var priceInput = row.querySelector('input[type="number"][name^="price_"]');
                    var hint = row.querySelector('.ss-rm-slots__price-hint');
                    if (priceInput && hint) {
                        var n = parseInt(priceInput.value, 10);
                        if (!isNaN(n) && n > 0) {
                            hint.textContent = n.toLocaleString('en-US') + ' VND';
                        }
                    }
                });
            }

            function refresh() {
                updatePriceHints();
                var changedRows = 0;
                form.querySelectorAll('[data-slot-row]').forEach(function(row) {
                    var changed = false;
                    row.querySelectorAll('input:not([type="hidden"]), select').forEach(function(el) {
                        if (value(el) !== el.getAttribute('data-initial-value')) { changed = true; }
                    });
                    row.classList.toggle('ss-rm-slots__row--changed', changed);
                    if (changed) { changedRows++; }
                });
                var statusChanged = statusSelect && statusSelect.value !== statusSelect.getAttribute('data-initial');
                if (note) {
                    var parts = [];
                    if (changedRows) { parts.push(changedRows + (changedRows === 1 ? ' slot changed' : ' slots changed')); }
                    if (statusChanged) { parts.push('room status changed'); }
                    note.textContent = parts.length ? parts.join(', ') + '. Not saved yet.' : '';
                }
                if (maintNotice && statusSelect) {
                    maintNotice.classList.toggle('d-none', statusSelect.value !== 'MAINTENANCE');
                }
            }

            form.addEventListener('input', refresh);
            form.addEventListener('change', refresh);
            form.addEventListener('submit', function() {
                if (saveBtn) { saveBtn.disabled = true; saveBtn.textContent = 'Saving...'; }
            });
        });
    </script>
</t:layout>
