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
        <p class="text-muted mb-0">Browse ${result.totalItems} ${result.totalItems == 1 ? 'room' : 'rooms'} &middot; select your date &amp; preferred time slots</p>
    </div>

    <!-- 1. Agoda-style Search First Card -->
    <div class="card border-0 shadow-sm rounded-4 p-3 p-md-4 mb-4 agoda-search-box">
        <form action="${pageContext.request.contextPath}/rooms" method="get" class="search-form-grid search-grid-rooms">
            <input type="hidden" name="floor" value="${e:esc(currentFloor)}">
            <input type="hidden" name="minCapacity" value="${e:esc(currentCap)}">
            <input type="hidden" name="sort" value="${e:esc(currentSort)}">
            <input type="hidden" name="dir" value="${e:esc(currentDir)}">
            <input type="hidden" id="roomsHiddenWeekStart" name="weekStart" value="${weekStart}">

            <!-- 1. Check-in Date -->
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

            <!-- 2. Required Time Slots (4 equal buttons in 1 row) -->
            <div class="search-field">
                <label class="search-label">
                    <i class="bi bi-clock-history me-1 text-caramel"></i>Required Time Slots (Multi-select)
                </label>
                <div class="slot-picker-grid search-control">
                    <label class="slot-pill-check">
                        <input type="checkbox" name="slots" value="S1" ${selectedSlots.contains('S1') ? 'checked' : ''}>
                        <span class="slot-pill-btn">
                            <span class="slot-badge-code">S1</span>
                            <span class="slot-time-label">06:00 &ndash; 12:00</span>
                        </span>
                    </label>
                    <label class="slot-pill-check">
                        <input type="checkbox" name="slots" value="S2" ${selectedSlots.contains('S2') ? 'checked' : ''}>
                        <span class="slot-pill-btn">
                            <span class="slot-badge-code">S2</span>
                            <span class="slot-time-label">12:00 &ndash; 18:00</span>
                        </span>
                    </label>
                    <label class="slot-pill-check">
                        <input type="checkbox" name="slots" value="S3" ${selectedSlots.contains('S3') ? 'checked' : ''}>
                        <span class="slot-pill-btn">
                            <span class="slot-badge-code">S3</span>
                            <span class="slot-time-label">18:00 &ndash; 23:00</span>
                        </span>
                    </label>
                    <label class="slot-pill-check">
                        <input type="checkbox" name="slots" value="S4" ${selectedSlots.contains('S4') ? 'checked' : ''}>
                        <span class="slot-pill-btn">
                            <i class="bi bi-moon-stars-fill slot-moon-icon"></i>
                            <span class="slot-badge-code">S4</span>
                            <span class="slot-time-label">Overnight</span>
                        </span>
                    </label>
                </div>
            </div>

            <!-- 3. Submit Button -->
            <div class="search-field">
                <button type="submit" class="btn agoda-search-btn rounded-pill px-4 py-2 w-100 fw-bold d-flex align-items-center justify-content-center gap-2 search-control">
                    <i class="bi bi-search"></i> Search Rooms
                </button>
            </div>
        </form>
    </div>

    <!-- 2. Single 7-Day Navigation Strip & Summary Bar -->
    <div class="card border-0 shadow-sm rounded-4 p-3 mb-4 date-strip-card">
        <div class="d-flex align-items-center justify-content-between gap-2">
            <!-- Prev 7 Days Button -->
            <a href="${pageContext.request.contextPath}/rooms?date=${prevWeekStart}&weekStart=${prevWeekStart}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? currentCap : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? currentFloor : ''}&sort=${currentSort}&dir=${currentDir}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
               class="btn btn-sm btn-outline-soft rounded-pill px-2 py-1 flex-shrink-0 ${!canGoPrev ? 'disabled opacity-50' : ''}"
               title="Previous 7 Days">
                <i class="bi bi-chevron-left"></i>
                <span class="d-none d-sm-inline ms-1">Prev</span>
            </a>

            <!-- 7 Day Pills -->
            <div class="week-days-strip d-flex align-items-center gap-2 flex-grow-1 overflow-auto py-1">
                <t:forEach items="${weekDays}" var="wd">
                    <a href="${pageContext.request.contextPath}/rooms?date=${wd.date}&weekStart=${weekStart}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? currentCap : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? currentFloor : ''}&sort=${currentSort}&dir=${currentDir}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                       class="week-day-pill rounded-4 p-2 text-center flex-shrink-0 ${wd.active ? 'active-week-pill' : 'inactive-week-pill'}"
                       style="min-width: 82px;">
                        <div class="small fw-bold text-uppercase ${wd.active ? 'text-white' : 'text-caramel'}" style="font-size: 0.72rem;">${wd.dayName}</div>
                        <div class="fw-bold ${wd.active ? 'text-white' : 'text-espresso'}" style="font-size: 0.92rem;">${wd.shortDate}</div>
                    </a>
                </t:forEach>
            </div>

            <!-- Next 7 Days Button -->
            <a href="${pageContext.request.contextPath}/rooms?date=${nextWeekStart}&weekStart=${nextWeekStart}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? currentCap : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? currentFloor : ''}&sort=${currentSort}&dir=${currentDir}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
               class="btn btn-sm btn-outline-soft rounded-pill px-2 py-1 flex-shrink-0 ${!canGoNext ? 'disabled opacity-50' : ''}"
               title="Next 7 Days">
                <span class="d-none d-sm-inline me-1">Next</span>
                <i class="bi bi-chevron-right"></i>
            </a>

            <!-- Small Calendar Icon Button (Native Date Picker) -->
            <button type="button" class="btn btn-calendar-trigger" id="btnOpenDatePicker" title="Pick a specific date"
                    onclick="var el=document.getElementById('nativeDateJump'); if(el.showPicker){el.showPicker();}else{el.click();}">
                <i class="bi bi-calendar-event"></i>
            </button>
            <input type="date" id="nativeDateJump" data-datepicker class="visually-hidden" min="${today}" max="${maxDate}" value="${date}"
                   onchange="if(this.value){ window.location.href='${pageContext.request.contextPath}/rooms?date=' + this.value + '&weekStart=' + this.value + '${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? currentCap : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? currentFloor : ''}&sort=${currentSort}&dir=${currentDir}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}'; }">
        </div>

        <!-- Visual Summary Strip -->
        <hr class="my-2 border-latte opacity-50">
        <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 pt-1">
            <div class="d-flex align-items-center gap-2">
                <span class="badge rounded-pill px-3 py-2 ${availableRoomCount > 0 ? 'bg-success' : 'bg-danger'}" style="font-size: 0.85rem;">
                    <i class="bi ${availableRoomCount > 0 ? 'bi-check-circle' : 'bi-x-circle'} me-1"></i>
                    ${availableRoomCount}/${totalRoomCount} rooms available
                </span>
                <span class="text-secondary small fw-medium">
                    for <strong>${e:date(date)}</strong>
                    <t:if test="${not empty selectedSlotsStr}">
                        &middot; Slots: <span class="badge bg-light text-espresso border">${selectedSlotsStr}</span>
                    </t:if>
                </span>
            </div>
            <div class="small text-muted">
                <i class="bi bi-shield-lock text-caramel me-1"></i> Smart Lock &middot; 24/7 Automated Self Check-in
            </div>
        </div>
    </div>

    <t:if test="${not empty dbError}">
        <div class="alert alert-danger py-2 rounded-3" role="alert">${e:esc(dbError)}</div>
    </t:if>

    <t:if test="${not empty dateNotice}">
        <div class="alert alert-warning py-2 rounded-3" role="alert">${e:esc(dateNotice)}</div>
    </t:if>

    <!-- Segmented Filter & Sort Controls Row -->
    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
        <!-- Filter Segmented Control Groups -->
        <div class="d-flex align-items-center gap-3 flex-wrap">
            <!-- Capacity Group: [ Capacity: All | 2 | 3+ ] -->
            <div class="segmented-control-group d-inline-flex align-items-center rounded-pill p-1">
                <span class="segmented-label px-2"><i class="bi bi-people me-1"></i>Capacity:</span>
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                   class="segmented-btn ${empty currentCap ? 'active' : ''}">All</a>
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}&minCapacity=2${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                   class="segmented-btn ${currentCap == '2' ? 'active' : ''}">2</a>
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}&minCapacity=3${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                   class="segmented-btn ${currentCap == '3' ? 'active' : ''}">3+</a>
            </div>

            <!-- Floor Group: [ Floor: All | 1 | 2 ] -->
            <div class="segmented-control-group d-inline-flex align-items-center rounded-pill p-1">
                <span class="segmented-label px-2"><i class="bi bi-layers me-1"></i>Floor:</span>
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                   class="segmented-btn ${empty currentFloor ? 'active' : ''}">All</a>
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}&floor=1&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                   class="segmented-btn ${currentFloor == '1' ? 'active' : ''}">1</a>
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}&floor=2&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                   class="segmented-btn ${currentFloor == '2' ? 'active' : ''}">2</a>
            </div>
        </div>

        <!-- Sort By Dropdown: Sort by: [ Room A–Z ▾ ] -->
        <div class="d-inline-flex align-items-center gap-2">
            <span class="sort-by-label text-muted small fw-semibold"><i class="bi bi-sliders me-1 text-caramel"></i>Sort by:</span>
            <div class="dropdown">
                <button class="btn btn-sort-dropdown dropdown-toggle rounded-pill px-3 py-1 fw-semibold d-inline-flex align-items-center gap-2"
                        type="button" id="sortDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                    <span>${activeSortLabel}</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow-sm border rounded-3 p-1" aria-labelledby="sortDropdown">
                    <li>
                        <a class="dropdown-item rounded-2 py-1 small ${currentSort == 'room' && currentDir == 'ASC' ? 'active fw-bold' : ''}"
                           href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=room&dir=ASC${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}">
                            Room A&ndash;Z
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item rounded-2 py-1 small ${currentSort == 'room' && currentDir == 'DESC' ? 'active fw-bold' : ''}"
                           href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=room&dir=DESC${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}">
                            Room Z&ndash;A
                        </a>
                    </li>
                    <li><hr class="dropdown-divider my-1"></li>
                    <li>
                        <a class="dropdown-item rounded-2 py-1 small ${currentSort == 'price' && currentDir == 'ASC' ? 'active fw-bold' : ''}"
                           href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=price&dir=ASC${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}">
                            Price (Lowest first)
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item rounded-2 py-1 small ${currentSort == 'price' && currentDir == 'DESC' ? 'active fw-bold' : ''}"
                           href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=price&dir=DESC${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}">
                            Price (Highest first)
                        </a>
                    </li>
                    <li><hr class="dropdown-divider my-1"></li>
                    <li>
                        <a class="dropdown-item rounded-2 py-1 small ${currentSort == 'floor' && currentDir == 'ASC' ? 'active fw-bold' : ''}"
                           href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=floor&dir=ASC${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}">
                            Floor (1 &rarr; 2)
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item rounded-2 py-1 small ${currentSort == 'floor' && currentDir == 'DESC' ? 'active fw-bold' : ''}"
                           href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=floor&dir=DESC${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}">
                            Floor (2 &rarr; 1)
                        </a>
                    </li>
                    <li><hr class="dropdown-divider my-1"></li>
                    <li>
                        <a class="dropdown-item rounded-2 py-1 small ${currentSort == 'capacity' && currentDir == 'ASC' ? 'active fw-bold' : ''}"
                           href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=capacity&dir=ASC${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}">
                            Capacity (Lowest first)
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item rounded-2 py-1 small ${currentSort == 'capacity' && currentDir == 'DESC' ? 'active fw-bold' : ''}"
                           href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=capacity&dir=DESC${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}">
                            Capacity (Highest first)
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>

    <!-- Active Filters & Results Count Row -->
    <div class="d-flex align-items-center gap-2 flex-wrap mb-4 active-filters-bar">
        <span class="rooms-count-label fw-bold text-dark me-1">${result.totalItems} ${result.totalItems == 1 ? 'room' : 'rooms'}</span>

        <t:if test="${not empty currentCap}">
            <span class="active-filter-chip">
                <span>
                    <t:if test="${currentCap == '2'}">2 Guests</t:if>
                    <t:if test="${currentCap == '3'}">3+ Guests</t:if>
                    <t:if test="${currentCap != '2' && currentCap != '3'}">${e:esc(currentCap)}+ Guests</t:if>
                </span>
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentFloor ? '&floor=' : ''}${not empty currentFloor ? e:esc(currentFloor) : ''}&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                   class="chip-remove-btn" aria-label="Remove guest capacity filter">&times;</a>
            </span>
        </t:if>

        <t:if test="${not empty currentFloor}">
            <span class="active-filter-chip">
                <span>Floor ${e:esc(currentFloor)}</span>
                <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}${not empty currentCap ? '&minCapacity=' : ''}${not empty currentCap ? e:esc(currentCap) : ''}&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
                   class="chip-remove-btn" aria-label="Remove floor filter">&times;</a>
            </span>
        </t:if>

        <t:if test="${not empty currentCap || not empty currentFloor}">
            <a href="${pageContext.request.contextPath}/rooms?date=${e:esc(date)}&sort=${e:esc(currentSort)}&dir=${e:esc(currentDir)}${not empty selectedSlotsStr ? '&slots=' : ''}${not empty selectedSlotsStr ? selectedSlotsStr : ''}"
               class="clear-all-filters-btn ms-1">
                Clear all
            </a>
        </t:if>
    </div>

    <!-- Rooms Cards List -->
    <t:if test="${empty result.items}">
        <div class="alert alert-info py-4 text-center my-4 rounded-4 shadow-sm" role="status">
            <div class="h5 fw-bold mb-1">No rooms match your filters</div>
            <p class="text-muted small mb-0">Try clearing some filter criteria or selecting another date.</p>
        </div>
    </t:if>

    <div class="room-boutique-list mb-4">
        <t:forEach items="${result.items}" var="card">
            <div class="room-boutique-card mb-4 shadow-sm">
                <div class="row g-0 align-items-stretch">
                    <!-- Left Column: Photo, Badges, Collapsible Time Slots -->
                    <div class="col-lg-5 col-md-5 d-flex flex-column p-3 boutique-card-media-col">
                        <div class="boutique-card-img-wrap position-relative">
                            <img src="${pageContext.request.contextPath}/${card.room.resolvedImageUrl}"
                                 alt="${e:esc(card.room.displayTitle)} room interior"
                                 class="boutique-card-img"
                                 onerror="this.onerror=null; this.src='${pageContext.request.contextPath}/assets/images/branding/hero_img2.jpg';">
                            <div class="room-card-placeholder" style="display: none;">
                                <span>${e:esc(card.room.displayTitle)}</span>
                            </div>
                            <!-- Cute Badges on Image -->
                            <div class="boutique-badge-top-left">
                                <span class="badge-tag-cute">SHORT STAY</span>
                            </div>
                            <div class="boutique-badge-top-right">
                                <span class="badge-ref-cute">${e:esc(card.room.roomNumber)}</span>
                            </div>
                            <t:if test="${!card.bookable}">
                                <div class="boutique-badge-status">
                                    <span class="badge bg-danger">MAINTENANCE</span>
                                </div>
                            </t:if>
                        </div>

                        <!-- Collapsible Time Slots Toggle Button below photo -->
                        <button class="btn boutique-slot-toggle w-100 rounded-pill mt-3 py-2 small fw-semibold d-flex align-items-center justify-content-between px-3"
                                type="button"
                                data-bs-toggle="collapse"
                                data-bs-target="#slots-${card.room.roomId}"
                                aria-expanded="false"
                                aria-controls="slots-${card.room.roomId}">
                            <span><i class="bi bi-clock-history me-1 text-caramel"></i> View Time Slots (4)</span>
                            <i class="bi bi-chevron-down toggle-caret"></i>
                        </button>
                        <div class="collapse mt-2" id="slots-${card.room.roomId}">
                            <div class="slot-grid-mini p-2 rounded-3">
                                <t:forEach items="${card.slots}" var="sa">
                                    <div class="slot-mini-item ${sa.cssClass} p-2 rounded-2 mb-1">
                                        <div class="d-flex justify-content-between align-items-center">
                                            <span class="fw-bold small slot-mini-title">${e:esc(sa.slot.slotCode)} &middot; ${e:esc(sa.slot.slotName)}</span>
                                            <span class="badge ${sa.cssClass == 'slot-available' ? 'bg-success' : 'bg-secondary'}" style="font-size:0.65rem;">${e:esc(sa.label)}</span>
                                        </div>
                                        <div class="d-flex justify-content-between align-items-center mt-1">
                                            <span class="text-muted small" style="font-size:0.75rem;"><i class="bi bi-clock me-1"></i>${sa.slot.timeRange}</span>
                                            <span class="fw-semibold text-primary" style="font-size:0.82rem;">${e:money(sa.slot.listPrice)}</span>
                                        </div>
                                    </div>
                                </t:forEach>
                            </div>
                        </div>
                    </div>

                    <!-- Right Column: Title, Overnight Price, Description, Amenities, Action -->
                    <div class="col-lg-7 col-md-7 p-4 d-flex flex-column justify-content-between boutique-card-content-col">
                        <div>
                            <!-- Top Row: Title + Overnight Price -->
                            <div class="d-flex justify-content-between align-items-start gap-3">
                                <div>
                                    <h2 class="boutique-room-title mb-1">${e:esc(card.room.displayTitle)}</h2>
                                    <div class="boutique-room-meta text-muted small">
                                        <span><i class="bi bi-people me-1 text-caramel"></i>${card.room.capacity} Guests</span>
                                        <t:if test="${not empty card.room.locationDescription}">
                                            <span class="mx-1">&middot;</span>
                                            <span><i class="bi bi-layers me-1 text-caramel"></i>${e:esc(card.room.locationDescription)}</span>
                                        </t:if>
                                        <t:if test="${empty card.room.locationDescription}">
                                            <span class="mx-1">&middot;</span>
                                            <span><i class="bi bi-layers me-1 text-caramel"></i>Floor ${card.room.floorNumber}</span>
                                        </t:if>
                                    </div>
                                </div>
                                <div class="text-end flex-shrink-0">
                                    <div class="pricing-rate-3h justify-content-end">
                                        <span class="rate-value">${e:money(card.minPrice)}</span>
                                        <span class="rate-unit">/ 3 hours</span>
                                    </div>
                                    <div class="pricing-rate-overnight justify-content-end">
                                        <span class="rate-value-sub">
                                            <t:if test="${card.room.overnightPrice > 0}">
                                                ${e:money(card.room.overnightPrice)}
                                            </t:if>
                                            <t:if test="${card.room.overnightPrice <= 0}">
                                                ${e:money(card.minPrice + 100000)}
                                            </t:if>
                                        </span>
                                        <span class="rate-unit-sub">/ overnight</span>
                                    </div>
                                </div>
                            </div>

                            <!-- Room Description -->
                            <t:if test="${not empty card.room.caption}">
                                <p class="boutique-room-desc mt-3 mb-3 text-secondary">
                                    ${e:esc(card.room.caption)}
                                </p>
                            </t:if>

                            <!-- Cute Rounded Amenities Pills -->
                            <t:if test="${not empty card.room.amenities}">
                                <div class="boutique-amenities-row mb-3">
                                    <t:forEach items="${e:compactAmenities(card.room.amenities, 4)}" var="am">
                                        <span class="amenity-pill-cute">
                                            <t:if test="${am.hasIcon}">
                                                <img src="${pageContext.request.contextPath}/assets/images/amenities/${am.iconFile}" alt="" class="amenity-pill-icon">
                                            </t:if>
                                            ${e:esc(am.label)}
                                        </span>
                                    </t:forEach>
                                    <span class="amenity-pill-cute text-muted">+ more</span>
                                </div>
                            </t:if>
                        </div>

                        <!-- Bottom Row: Divider + Deposit Note + CTA Button -->
                        <div>
                            <hr class="boutique-card-divider my-3">
                            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                                <div class="boutique-note small text-muted">
                                    <i class="bi bi-shield-check text-success me-1"></i>
                                    <span>Keyless 24/7 smart lock &middot; Instant confirmation</span>
                                </div>
                                <a href="${pageContext.request.contextPath}/rooms/detail?id=${card.room.roomId}&amp;date=${date}"
                                   class="btn btn-primary rounded-pill px-4 boutique-btn-view">
                                    View Room &amp; Availability &rarr;
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </t:forEach>
    </div>

    <!-- Pagination -->
    <t:pagination result="${result}" baseUrl="${pageContext.request.contextPath}/rooms" />
</t:layout>
