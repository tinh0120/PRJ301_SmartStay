<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="e" uri="/WEB-INF/tld/smartstay.tld" %>
<%@ taglib prefix="t" tagdir="/WEB-INF/tags" %>
<t:layout title="Rooms" active="rooms">
    <!-- Boutique Breadcrumb Navigation -->
    <nav aria-label="breadcrumb" class="mb-3">
        <ol class="breadcrumb boutique-breadcrumb m-0">
            <li class="breadcrumb-item">
                <a href="${pageContext.request.contextPath}/" class="boutique-breadcrumb-link">
                    <i class="bi bi-house-door me-1"></i>Home
                </a>
            </li>
            <li class="breadcrumb-item active" aria-current="page">Rooms</li>
        </ol>
    </nav>

    <div class="rooms-page-header mb-3">
        <h1 class="room-page-title mb-1">Find Your Perfect Room</h1>
        <p class="text-muted mb-0">Select your date and preferred time slots</p>
    </div>

    <!-- 1. Search & filters (R4): every change re-queries after 300 ms; the button still submits without JS -->
    <div class="card border-0 shadow-sm rounded-4 p-3 p-md-4 mb-4 agoda-search-box">
        <form action="${pageContext.request.contextPath}/rooms" method="get" id="roomsFilterForm" class="ss-rooms-filter">
            <input type="hidden" id="roomsHiddenWeekStart" name="weekStart" value="${weekStart}">

            <div class="search-form-grid search-grid-rooms">
                <!-- Check-in Date -->
                <div class="search-field">
                    <label for="roomsAgodaDateInput" class="search-label">
                        <i class="bi bi-calendar-event me-1 text-caramel"></i>Check-in Date
                    </label>
                    <div class="position-relative search-control">
                        <input type="text" id="roomsAgodaDateInput" name="date" class="form-control rounded-pill px-3 py-2 fw-semibold agoda-input"
                               value="${date}" readonly style="cursor: pointer;" required
                               data-toggle="weekly-calendar" data-target-input="#roomsAgodaDateInput" data-target-weekstart="#roomsHiddenWeekStart">
                        <i class="bi bi-calendar3 position-absolute end-0 top-50 translate-middle-y me-3 text-caramel pe-none"></i>
                    </div>
                </div>

                <!-- Time slots: names only (hours differ per room); adjacent slots only -->
                <div class="search-field">
                    <span class="search-label" id="roomsSlotsLabel">
                        <i class="bi bi-clock-history me-1 text-caramel"></i>Time Slots
                    </span>
                    <div class="slot-picker-grid search-control" role="group" aria-labelledby="roomsSlotsLabel" data-slot-group>
                        <label class="slot-pill-check">
                            <input type="checkbox" name="slots" value="S1" ${selectedSlots.contains('S1') ? 'checked' : ''}>
                            <span class="slot-pill-btn"><span class="slot-time-label">Morning</span></span>
                        </label>
                        <label class="slot-pill-check">
                            <input type="checkbox" name="slots" value="S2" ${selectedSlots.contains('S2') ? 'checked' : ''}>
                            <span class="slot-pill-btn"><span class="slot-time-label">Afternoon</span></span>
                        </label>
                        <label class="slot-pill-check">
                            <input type="checkbox" name="slots" value="S3" ${selectedSlots.contains('S3') ? 'checked' : ''}>
                            <span class="slot-pill-btn"><span class="slot-time-label">Evening</span></span>
                        </label>
                        <label class="slot-pill-check">
                            <input type="checkbox" name="slots" value="S4" ${selectedSlots.contains('S4') ? 'checked' : ''}>
                            <span class="slot-pill-btn">
                                <i class="bi bi-moon-stars-fill slot-moon-icon" aria-hidden="true"></i>
                                <span class="slot-time-label">Overnight</span>
                            </span>
                        </label>
                    </div>
                </div>

                <!-- Submit (still works without JS; no longer required with JS) -->
                <div class="search-field">
                    <button type="submit" class="btn agoda-search-btn rounded-pill px-4 py-2 w-100 fw-bold d-flex align-items-center justify-content-center gap-2 search-control">
                        <i class="bi bi-search"></i> Search Rooms
                    </button>
                </div>
            </div>
            <p class="ss-rooms-filter__hint" data-slot-hint>Pick one slot or several slots next to each other. 2 slots save ${rates.twoSlots}%, 3 slots ${rates.threeSlots}%, 4 slots ${rates.fourSlots}%.</p>

            <!-- Secondary filters: options come from the rooms in the database -->
            <div class="ss-rooms-filter__row">
                <div class="ss-rooms-filter__field">
                    <label for="roomsCapacitySelect" class="ss-rooms-filter__label">Guests</label>
                    <select id="roomsCapacitySelect" name="minCapacity" class="form-select form-select-sm ss-rooms-filter__select">
                        <option value="">Any size</option>
                        <t:forEach items="${capacityOptions}" var="cap">
                            <option value="${cap}" ${currentCap == cap.toString() ? 'selected' : ''}>${cap}+ guests</option>
                        </t:forEach>
                    </select>
                </div>
                <div class="ss-rooms-filter__field">
                    <label for="roomsFloorSelect" class="ss-rooms-filter__label">Floor</label>
                    <select id="roomsFloorSelect" name="floor" class="form-select form-select-sm ss-rooms-filter__select">
                        <option value="">Any floor</option>
                        <t:forEach items="${floorOptions}" var="fl">
                            <option value="${fl}" ${currentFloor == fl.toString() ? 'selected' : ''}>Floor ${fl}</option>
                        </t:forEach>
                    </select>
                </div>
                <div class="ss-rooms-filter__field">
                    <label for="roomsSortSelect" class="ss-rooms-filter__label">Sort by</label>
                    <select id="roomsSortSelect" name="sort" class="form-select form-select-sm ss-rooms-filter__select">
                        <option value="room" ${currentSort == 'room' ? 'selected' : ''}>Room A to Z</option>
                        <option value="price" ${currentSort == 'price' ? 'selected' : ''}>Price low to high</option>
                        <option value="price_desc" ${currentSort == 'price_desc' ? 'selected' : ''}>Price high to low</option>
                        <option value="capacity" ${currentSort == 'capacity' ? 'selected' : ''}>Capacity</option>
                    </select>
                </div>
                <div class="form-check form-switch ss-rooms-filter__switch">
                    <input class="form-check-input" type="checkbox" role="switch" id="roomsAvailableOnly" name="available" value="1" ${availableOnly ? 'checked' : ''}>
                    <label class="form-check-label" for="roomsAvailableOnly">Available only</label>
                </div>
            </div>
        </form>
    </div>

    <p id="roomsLive" class="visually-hidden" aria-live="polite"></p>

    <!-- 2. Results (replaced in place by room-search.js) -->
    <div id="roomsResults" class="ss-rooms-results" data-status-line="${e:esc(statusLine)}">

        <!-- 7-day strip -->
        <div class="card border-0 shadow-sm rounded-4 p-3 mb-3 date-strip-card">
            <div class="d-flex align-items-center justify-content-between gap-2">
                <t:if test="${canGoPrev}">
                    <a href="${e:esc(prevWeekUrl)}" data-rooms-nav class="btn btn-sm btn-outline-soft rounded-pill px-2 py-1 flex-shrink-0" title="Previous 7 days">
                        <i class="bi bi-chevron-left" aria-hidden="true"></i><span class="d-none d-sm-inline ms-1">Prev</span>
                    </a>
                </t:if>
                <t:if test="${!canGoPrev}">
                    <span class="btn btn-sm btn-outline-soft rounded-pill px-2 py-1 flex-shrink-0 disabled opacity-50" aria-disabled="true">
                        <i class="bi bi-chevron-left" aria-hidden="true"></i><span class="d-none d-sm-inline ms-1">Prev</span>
                    </span>
                </t:if>

                <div class="week-days-strip d-flex align-items-center gap-2 flex-grow-1 overflow-auto py-1">
                    <t:forEach items="${weekDays}" var="wd">
                        <a href="${e:esc(wd.url)}" data-rooms-nav
                           class="week-day-pill rounded-4 p-2 text-center flex-shrink-0 ${wd.active ? 'active-week-pill' : 'inactive-week-pill'}"
                           ${wd.active ? 'aria-current="date"' : ''} style="min-width: 82px;">
                            <div class="small fw-bold ${wd.active ? 'text-white' : 'text-caramel'}" style="font-size: 0.72rem;">${wd.dayName}</div>
                            <div class="fw-bold ${wd.active ? 'text-white' : 'text-espresso'}" style="font-size: 0.92rem;">${wd.shortDate}</div>
                        </a>
                    </t:forEach>
                </div>

                <t:if test="${canGoNext}">
                    <a href="${e:esc(nextWeekUrl)}" data-rooms-nav class="btn btn-sm btn-outline-soft rounded-pill px-2 py-1 flex-shrink-0" title="Next 7 days">
                        <span class="d-none d-sm-inline me-1">Next</span><i class="bi bi-chevron-right" aria-hidden="true"></i>
                    </a>
                </t:if>
                <t:if test="${!canGoNext}">
                    <span class="btn btn-sm btn-outline-soft rounded-pill px-2 py-1 flex-shrink-0 disabled opacity-50" aria-disabled="true">
                        <span class="d-none d-sm-inline me-1">Next</span><i class="bi bi-chevron-right" aria-hidden="true"></i>
                    </span>
                </t:if>
            </div>
        </div>

        <!-- Status line + active filter chips -->
        <div class="ss-rooms-results__bar">
            <p class="ss-rooms-results__status">${e:esc(statusLine)}</p>
            <t:if test="${not empty chips}">
                <div class="ss-rooms-results__chips">
                    <t:forEach items="${chips}" var="chip">
                        <span class="ss-chip ss-chip--active ss-rooms-chip">
                            <span>${e:esc(chip.label)}</span>
                            <a href="${e:esc(chip.removeUrl)}" data-rooms-nav class="ss-chip__remove ss-rooms-chip__remove" aria-label="${e:esc(chip.removeLabel)}">
                                <i class="bi bi-x" aria-hidden="true"></i>
                            </a>
                        </span>
                    </t:forEach>
                    <a href="${e:esc(clearAllUrl)}" data-rooms-nav class="ss-rooms-results__clear">Clear all</a>
                </div>
            </t:if>
        </div>

        <t:if test="${not empty slotNotice}">
            <div class="alert alert-warning py-2 rounded-3" role="alert">${e:esc(slotNotice)}</div>
        </t:if>
        <t:if test="${not empty dbError}">
            <div class="alert alert-danger py-2 rounded-3" role="alert">${e:esc(dbError)}</div>
        </t:if>
        <t:if test="${not empty dateNotice}">
            <div class="alert alert-warning py-2 rounded-3" role="alert">${e:esc(dateNotice)}</div>
        </t:if>

        <!-- Empty state -->
        <t:if test="${empty rooms && empty dbError}">
            <div class="ss-empty ss-rooms-empty" role="status">
                <i class="bi bi-door-closed ss-empty__icon" aria-hidden="true"></i>
                <p class="ss-empty__title">No rooms match your filters</p>
                <p class="ss-empty__desc">Remove a filter or choose another date to see more rooms.</p>
                <a href="${e:esc(clearAllUrl)}" data-rooms-nav class="btn btn-outline-soft rounded-pill px-4">Clear filters</a>
            </div>
        </t:if>

        <!-- Rooms (unavailable ones stay visible, dimmed and last) -->
        <div class="room-boutique-list mb-4">
            <t:forEach items="${rooms}" var="item">
                <div class="room-boutique-card mb-4 shadow-sm ss-rooms-card ${item.available ? '' : 'ss-rooms-card--unavailable'}">
                    <div class="row g-0 align-items-stretch">
                        <!-- Left Column: Photo, Badges, Collapsible Time Slots -->
                        <div class="col-lg-5 col-md-5 d-flex flex-column p-3 boutique-card-media-col">
                            <div class="boutique-card-img-wrap position-relative">
                                <img src="${pageContext.request.contextPath}/${item.card.room.resolvedImageUrl}"
                                     alt="${e:esc(item.card.room.displayTitle)} room interior"
                                     class="boutique-card-img" width="768" height="576"
                                     onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                                <%-- V4: the identical "Short stay" tag on every photo was removed --%>
                                <div class="boutique-badge-top-right">
                                    <span class="badge-ref-cute">${e:esc(item.card.room.roomNumber)}</span>
                                </div>
                                <t:if test="${!item.card.bookable}">
                                    <div class="boutique-badge-status">
                                        <span class="ss-badge ss-badge--danger">Maintenance</span>
                                    </div>
                                </t:if>
                            </div>

                            <%-- Round 3, item 4: the per-card slot list was removed; the room page has the full slot picker --%>
                        </div>

                        <!-- Right Column: Title, price for the selection, description, amenities, action -->
                        <div class="col-lg-7 col-md-7 p-4 d-flex flex-column justify-content-between boutique-card-content-col">
                            <div>
                                <div class="d-flex justify-content-between align-items-start gap-3">
                                    <div>
                                        <h2 class="boutique-room-title mb-1">${e:esc(item.card.room.displayTitle)}</h2>
                                        <div class="boutique-room-meta text-muted small">
                                            <span><i class="bi bi-people me-1 text-caramel"></i>${item.card.room.capacity} Guests</span>
                                            <span class="mx-1">&middot;</span>
                                            <span><i class="bi bi-layers me-1 text-caramel"></i>Floor ${item.card.room.floorNumber}</span>
                                        </div>
                                        <t:if test="${not empty item.timeText}">
                                            <div class="ss-rooms-card__times">
                                                <i class="bi bi-clock me-1" aria-hidden="true"></i>${e:esc(item.timeText)}
                                            </div>
                                        </t:if>
                                    </div>
                                    <div class="text-end flex-shrink-0">
                                        <t:if test="${item.hasQuote}">
                                            <div class="ss-rooms-card__price">${e:money(item.quoteTotal)}</div>
                                            <div class="ss-rooms-card__price-meta">
                                                ${item.slotCount} ${item.slotCount == 1 ? 'slot' : 'slots'}<t:if test="${item.discountPercent > 0}"> &middot; &minus;${item.discountPercent}%</t:if>
                                            </div>
                                        </t:if>
                                        <t:if test="${!item.hasQuote}">
                                            <div class="pricing-rate-3h justify-content-end">
                                                <span class="rate-value">${e:money(item.card.minPrice)}</span>
                                                <span class="rate-unit">/ 3 hours</span>
                                            </div>
                                            <div class="pricing-rate-overnight justify-content-end">
                                                <span class="rate-value-sub">
                                                    <t:if test="${item.card.room.overnightPrice > 0}">${e:money(item.card.room.overnightPrice)}</t:if>
                                                    <t:if test="${item.card.room.overnightPrice <= 0}">${e:money(item.card.minPrice + 100000)}</t:if>
                                                </span>
                                                <span class="rate-unit-sub">/ overnight</span>
                                            </div>
                                        </t:if>
                                    </div>
                                </div>

                                <t:if test="${not empty item.card.room.caption}">
                                    <p class="boutique-room-desc mt-3 mb-3 text-secondary">${e:esc(item.card.room.caption)}</p>
                                </t:if>

                                <t:if test="${not empty item.card.room.amenities}">
                                    <div class="boutique-amenities-row mb-3">
                                        <t:forEach items="${e:compactAmenities(item.card.room.amenities, 4)}" var="am">
                                            <span class="amenity-pill-cute">
                                                <t:if test="${am.hasIcon}">
                                                    <img src="${pageContext.request.contextPath}/assets/images/amenities/sm/${am.iconFile}" alt="" class="amenity-pill-icon" width="14" height="14">
                                                </t:if>
                                                ${e:esc(am.label)}
                                            </span>
                                        </t:forEach>
                                        <span class="amenity-pill-cute text-muted">+ more</span>
                                    </div>
                                </t:if>
                            </div>

                            <div>
                                <%-- Round 3, item 4: the same "Keyless 24/7" line on every card was removed --%>
                                <div class="d-flex justify-content-end align-items-center flex-wrap gap-2 mt-3">
                                    <t:if test="${item.available}">
                                        <a href="${e:esc(item.detailUrl)}" class="btn btn-primary rounded-pill px-4 boutique-btn-view">
                                            View Room &amp; Availability
                                        </a>
                                    </t:if>
                                    <t:if test="${!item.available}">
                                        <div class="ss-rooms-card__reason">
                                            <i class="bi bi-calendar-x me-1" aria-hidden="true"></i>${e:esc(item.reason)}
                                        </div>
                                        <span class="btn btn-outline-soft rounded-pill px-4 disabled ss-rooms-card__cta-off" aria-disabled="true">
                                            ${empty selectedSlots ? 'Try another date' : 'Try other slots'}
                                        </span>
                                    </t:if>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </t:forEach>
        </div>
    </div>

    <script src="${pageContext.request.contextPath}/assets/js/room-search.js"></script>
</t:layout>
